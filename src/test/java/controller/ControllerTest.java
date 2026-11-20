package controller;

import game.Round;
import model.Card;
import model.Color;
import org.junit.jupiter.api.Test;
import view.GameView;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    /** Records the latest snapshot and toast. */
    static final class Recorder implements GameView {
        TableSnapshot last;
        String lastToast;
        @Override public void render(TableSnapshot s) { last = s; }
        @Override public void toast(String m) { lastToast = m; }
    }

    private UnoController controllerWithUser() {
        UnoController c = new UnoController(Scheduler.immediate(), 0);
        assertNull(c.register("erfan", "1234", "Erfan"));
        assertNull(c.register("sara", "1234", "Sara"));
        assertNull(c.login("erfan", "1234"));
        return c;
    }

    @Test
    void authRules() {
        UnoController c = new UnoController(Scheduler.immediate(), 0);
        assertNotNull(c.login("ghost", "1234"));
        assertNull(c.register("erfan", "1234", "Erfan"));
        assertNotNull(c.register("erfan", "1234", "Again"));
        assertNull(c.login("erfan", "1234"));
        assertEquals("Erfan", c.currentDisplayName());
        c.logout();
        assertEquals("Guest", c.currentDisplayName());
    }

    @Test
    void startGameValidatesTheConfiguration() {
        UnoController c = new UnoController(Scheduler.immediate(), 0);
        Recorder v = new Recorder();
        assertThrows(IllegalArgumentException.class, () -> c.startGame(1, 3, "score", 100, new String[0], v));   // not logged in
        UnoController u = controllerWithUser();
        assertThrows(IllegalArgumentException.class, () -> u.startGame(1, 0, "score", 100, new String[0], v));   // 1 player
        assertThrows(IllegalArgumentException.class, () -> u.startGame(1, 10, "score", 100, new String[0], v));  // 11 players
        assertThrows(IllegalArgumentException.class, () -> u.startGame(1, 3, "weird", 100, new String[0], v));
        assertThrows(IllegalArgumentException.class, () -> u.startGame(2, 1, "score", 100, new String[]{"ghost"}, v));
        assertThrows(IllegalArgumentException.class, () -> u.startGame(2, 1, "score", 100, new String[]{"erfan"}, v));
        assertNotNull(u.startGame(2, 1, "score", 100, new String[]{"sara"}, v));
        assertEquals(3, v.last.seats().length);
    }

    @Test
    void firstSnapshotIsConsistent() {
        UnoController c = controllerWithUser();
        Recorder v = new Recorder();
        c.startGame(1, 3, "score", 500, new String[0], v);
        TableSnapshot s = v.last;
        assertTrue(s.myTurn());
        assertEquals("Erfan", s.seats()[0].name());
        assertEquals(7, s.hand().length);
        assertEquals(s.hand().length, s.playable().length);
        assertEquals("Win Condition: 500 Pts", s.winCondition());
    }

    @Test
    void cheatWinRoundProducesASummaryWhoseNumbersAddUp() {
        UnoController c = controllerWithUser();
        Recorder v = new Recorder();
        GameActions g = c.startGame(1, 3, "rounds", 2, new String[0], v);
        g.cheat("cheat_win_round");
        TableSnapshot.RoundSummary r = v.last.summary();
        assertNotNull(r);
        assertEquals("Erfan", r.winner());
        int losers = 0;
        for (int i = 0; i < r.names().length; i++) if (!r.names()[i].equals("Erfan")) losers += r.points()[i];
        assertEquals(losers, r.gained());
        assertFalse(r.gameOver());
        g.nextRound();
        assertNull(v.last.summary());
        assertEquals(7, v.last.hand().length);              // fresh hands
        g.cheat("cheat_win_round");
        assertTrue(v.last.summary().gameOver());            // 2 rounds played
        assertEquals(1, c.leaderboard()[0].games());
    }

    @Test
    void cheatsGiveAndReplaceCardsWithoutPassingTheTurn() {
        UnoController c = controllerWithUser();
        Recorder v = new Recorder();
        GameActions g = c.startGame(1, 2, "score", 500, new String[0], v);
        g.cheat("cheat_give -t NUMBER -c RED -v 7");
        assertEquals(8, v.last.hand().length);
        assertEquals("RED 7", CardText.of(v.last.hand()[7]));
        g.cheat("cheat_give -t WILD_DRAW_FOUR -c NONE");
        assertEquals(9, v.last.hand().length);
        g.cheat("cheat_top -t SKIP -c BLUE");
        assertEquals("BLUE SKIP", CardText.of(v.last.topCard()));
        assertTrue(v.last.myTurn());
        g.cheat("cheat_empty_deck");
        assertEquals(1, v.last.deckCount());
        g.cheat("cheat_give -t NUMBER -c NONE -v 1");
        assertTrue(v.lastToast.contains("real color"));
        g.cheat("cheat_give -t NUMBER -c RED -v 12");
        assertTrue(v.lastToast.contains("0 and 9"));
    }

    @Test
    void invalidMovesDoNotConsumeTheTurn() {
        UnoController c = controllerWithUser();
        Recorder v = new Recorder();
        GameActions g = c.startGame(1, 2, "score", 500, new String[0], v);
        g.play(99, null);
        assertTrue(v.lastToast.contains("no card"));
        g.cheat("cheat_give -t WILD -c NONE");
        g.play(v.last.hand().length - 1, null);             // wild without a colour
        assertTrue(v.lastToast.toLowerCase().contains("color"));
        assertTrue(v.last.myTurn());
    }

    /** Plays many complete games with a simple human strategy and checks the invariants after every move. */
    @Test
    void fullGamesFinishAndAlwaysKeep108Cards() {
        java.util.Random choice = new java.util.Random(99);
        int[] playerCounts = {2, 3, 4, 6, 10};
        for (int game = 0; game < 120; game++) {
            int total = playerCounts[game % playerCounts.length];
            UnoController c = new UnoController(Scheduler.immediate(), 0);
            c.register("h", "1234", "Human");
            c.login("h", "1234");
            Recorder v = new Recorder();
            boolean scoreMode = game % 2 == 0;
            GameSession s = (GameSession) c.startGame(1, total - 1, scoreMode ? "score" : "rounds", scoreMode ? 60 : 3, new String[0], v);
            int moves = 0;
            while (!(v.last.summary() != null && v.last.summary().gameOver())) {
                assertTrue(++moves < 20000, "game does not terminate");
                Round r = s.getGame().getCurrentRound();
                TableSnapshot t = v.last;
                if (t.summary() != null) { s.nextRound(); continue; }
                int cards = 0;
                for (int i = 0; i < t.seats().length; i++) cards += t.seats()[i].cards();
                assertEquals(108, cards + t.deckCount() + r.getDiscardSize(), "card conservation");
                assertTrue(t.myTurn());
                if (t.pendingPenalty() > 0 && choice.nextInt(4) == 0) { s.drawMachine(); continue; }
                int pick = -1;
                for (int i = 0; i < t.playable().length; i++) if (t.playable()[i]) { pick = i; break; }
                if (pick >= 0) {
                    Card card = t.hand()[pick];
                    boolean wild = card.getColor() == Color.NONE;
                    if (t.drawnCardPending()) s.playDrawn(wild ? Color.GREEN : null);
                    else s.play(pick, wild ? Color.values()[choice.nextInt(4)] : null);
                } else if (t.drawnCardPending()) {
                    s.pass();
                } else {
                    s.draw();
                }
            }
            assertNotNull(v.last.summary().champion());
            assertEquals(1, c.leaderboard().length);
            assertEquals(1, c.leaderboard()[0].games());
        }
    }
}
