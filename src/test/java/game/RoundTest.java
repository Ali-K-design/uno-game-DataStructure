package game;

import dataStructures.DynamicArray;
import model.*;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RoundTest {
    private DynamicArray<Player> players;
    private Player p0, p1, p2;

    /** Starts a round whose first card has no effect (a plain number), so tests begin from a known state. */
    private Round plainRound(int playerCount) {
        for (long seed = 0; ; seed++) {
            players = new DynamicArray<>();
            for (int i = 0; i < playerCount; i++) players.add(new HumanPlayer("P" + i, new Account("p" + i, "x", "P" + i)));
            Round r = new Round(new Random(seed));
            r.start(players);
            if (r.getTopCard().getCardType() == CardType.NUMBER && r.getCurrentPlayer() == players.get(0)) {
                p0 = players.get(0);
                p1 = players.get(1);
                p2 = playerCount > 2 ? players.get(2) : null;
                return r;
            }
        }
    }

    private Card c(Color color, CardType type, int value) { return new Card(color, type, value); }

    @Test
    void everyPlayerStartsWithSevenCards() {
        Round r = plainRound(4);
        for (int i = 0; i < 4; i++) assertEquals(7, players.get(i).getHand().getSize());
        assertEquals(108 - 28 - 1, r.getDeckSize());
    }

    @Test
    void penaltiesStackAndAreDrawnInOneGo() {
        Round r = plainRound(3);
        Color active = r.getActiveColor();
        Card two = c(active, CardType.DRAW_TWO, 0);
        r.cheatGive(p0, two);
        assertNull(r.tryPlay(p0, two, null));
        assertEquals(2, r.getPendingPenalty());
        assertSame(p1, r.getCurrentPlayer());

        Card four = c(Color.NONE, CardType.WILD_DRAW_FOUR, 0);
        r.cheatGive(p1, four);
        assertNull(r.tryPlay(p1, four, Color.BLUE));            // allowed while stacking, even with active color in hand
        assertEquals(6, r.getPendingPenalty());
        assertEquals(Color.BLUE, r.getActiveColor());

        Card number = c(Color.BLUE, CardType.NUMBER, 3);
        r.cheatGive(p2, number);
        assertNotNull(r.tryPlay(p2, number, null));             // normal cards are refused under a penalty
        int before = p2.getHand().getSize();
        assertEquals(6, r.takePenalty(p2));
        assertEquals(before + 6, p2.getHand().getSize());
        assertEquals(0, r.getPendingPenalty());
        assertSame(p0, r.getCurrentPlayer());
    }

    @Test
    void reverseActsLikeSkipInTwoPlayerGames() {
        Round r = plainRound(2);
        Card reverse = c(r.getActiveColor(), CardType.REVERSE, 0);
        r.cheatGive(p0, reverse);
        assertNull(r.tryPlay(p0, reverse, null));
        assertSame(p0, r.getCurrentPlayer());
    }

    @Test
    void reverseChangesDirectionWithThreePlayers() {
        Round r = plainRound(3);
        Card reverse = c(r.getActiveColor(), CardType.REVERSE, 0);
        r.cheatGive(p0, reverse);
        assertNull(r.tryPlay(p0, reverse, null));
        assertFalse(r.isClockwise());
        assertSame(p2, r.getCurrentPlayer());
    }

    @Test
    void skipJumpsOverTheNextPlayer() {
        Round r = plainRound(3);
        Card skip = c(r.getActiveColor(), CardType.SKIP, 0);
        r.cheatGive(p0, skip);
        assertNull(r.tryPlay(p0, skip, null));
        assertSame(p2, r.getCurrentPlayer());
    }

    @Test
    void wildDrawFourIsRefusedWhileHoldingTheActiveColor() {
        Round r = plainRound(3);
        p0.clearHand();
        r.cheatGive(p0, c(r.getActiveColor(), CardType.NUMBER, 4));
        Card four = c(Color.NONE, CardType.WILD_DRAW_FOUR, 0);
        r.cheatGive(p0, four);
        assertNotNull(r.tryPlay(p0, four, Color.RED));
        p0.clearHand();
        r.cheatGive(p0, four);
        r.cheatGive(p0, c(Color.NONE, CardType.WILD, 0));
        assertNull(r.tryPlay(p0, four, Color.RED));
    }

    @Test
    void wildNeedsAChosenColor() {
        Round r = plainRound(3);
        Card wild = c(Color.NONE, CardType.WILD, 0);
        r.cheatGive(p0, wild);
        assertNotNull(r.tryPlay(p0, wild, null));
        assertNotNull(r.tryPlay(p0, wild, Color.NONE));
        assertNull(r.tryPlay(p0, wild, Color.GREEN));
        assertEquals(Color.GREEN, r.getActiveColor());
    }

    @Test
    void playingOutOfTurnOrNonMatchingIsRefusedAndDoesNotConsumeTheTurn() {
        Round r = plainRound(3);
        assertNotNull(r.tryPlay(p1, p1.getHand().get(0), Color.RED));
        Color other = r.getActiveColor() == Color.RED ? Color.BLUE : Color.RED;
        Card bad = c(other, CardType.NUMBER, (r.getTopCard().getValue() + 1) % 10);
        r.cheatGive(p0, bad);
        assertNotNull(r.tryPlay(p0, bad, null));
        assertSame(p0, r.getCurrentPlayer());
    }

    @Test
    void lastCardEndsTheRound() {
        Round r = plainRound(3);
        p0.clearHand();
        Card last = c(r.getActiveColor(), CardType.NUMBER, 1);
        r.cheatGive(p0, last);
        assertNull(r.tryPlay(p0, last, null));
        assertTrue(r.isFinished());
        assertSame(p0, r.getWinner());
    }

    @Test
    void drawnPlayableCardKeepsTheTurnUntilPlayedOrPassed() {
        boolean exercised = false;
        for (long seed = 0; seed < 300 && !exercised; seed++) {
            players = new DynamicArray<>();
            for (int i = 0; i < 3; i++) players.add(new HumanPlayer("P" + i, new Account("p" + i, "x", "P" + i)));
            Round r = new Round(new Random(seed));
            r.start(players);
            if (r.getTopCard().getCardType() != CardType.NUMBER || r.getCurrentPlayer() != players.get(0)) continue;
            Player first = players.get(0);
            Card drawn = r.drawOne(first);
            if (r.getDrawnCard() == null) {
                assertSame(players.get(1), r.getCurrentPlayer());         // not playable: turn passed on its own
                continue;
            }
            exercised = true;
            assertSame(drawn, r.getDrawnCard());
            assertSame(first, r.getCurrentPlayer());                      // turn still open
            Card other = first.getHand().get(0) == drawn ? first.getHand().get(1) : first.getHand().get(0);
            assertNotNull(r.tryPlay(first, other, Color.RED));            // only the drawn card may be played
            r.pass();
            assertSame(players.get(1), r.getCurrentPlayer());
            assertNull(r.getDrawnCard());
        }
        assertTrue(exercised);
    }

    @Test
    void cheatWinRoundLeavesOneCardAndEndsTheRound() {
        Round r = plainRound(3);
        r.cheatWinRound(p0);
        assertTrue(r.isFinished());
        assertEquals(1, p0.getHand().getSize());
        assertSame(p0, r.getWinner());
    }

    @Test
    void scoringIsRecursiveAndCorrect() {
        Leaderboard lb = new Leaderboard();
        ScoreManager sm = new ScoreManager(lb);
        Round r = plainRound(3);
        p1.clearHand(); p2.clearHand();
        r.cheatGive(p1, c(Color.RED, CardType.NUMBER, 7));
        r.cheatGive(p1, c(Color.RED, CardType.SKIP, 0));          // 7 + 20
        r.cheatGive(p2, c(Color.NONE, CardType.WILD, 0));
        r.cheatGive(p2, c(Color.NONE, CardType.WILD_DRAW_FOUR, 0)); // 100
        assertEquals(27, sm.calculateHandPoints(p1.getHand()));
        assertEquals(100, sm.calculateHandPoints(p2.getHand()));
        assertEquals(0, sm.calculateHandPoints(new dataStructures.SinglyLinkedList<>()));
        assertEquals(127, sm.recordRound(players, p0));
        assertEquals(127, p0.getCurrentGameScore());
        assertEquals(0, p1.getCurrentGameScore());
    }

    @Test
    void botFollowsTheSpecPriorities() {
        Account acc = new Account("b", "", "b");
        BotPlayer bot = new BotPlayer("Bot_1", acc);
        // penalty pending: first +2/+4 in hand order
        bot.addCard(new Card(Color.RED, CardType.NUMBER, 1));
        bot.addCard(new Card(Color.NONE, CardType.WILD_DRAW_FOUR, 0));
        bot.addCard(new Card(Color.BLUE, CardType.DRAW_TWO, 0));
        Action a = bot.chooseAction(new RoundState(new Card(Color.RED, CardType.DRAW_TWO, 0), Color.RED, 2, new boolean[]{false, true, true}, null));
        assertEquals(CardType.WILD_DRAW_FOUR, a.getCard().getCardType());
        // penalty pending but nothing to stack: draw
        Action d = bot.chooseAction(new RoundState(new Card(Color.RED, CardType.DRAW_TWO, 0), Color.RED, 2, new boolean[]{false, false, false}, null));
        assertEquals(Action.Type.DRAW, d.getType());
        // normal turn: coloured beats wild
        Action n = bot.chooseAction(new RoundState(new Card(Color.RED, CardType.NUMBER, 9), Color.RED, 0, new boolean[]{true, false, false}, null));
        assertEquals(CardType.NUMBER, n.getCard().getCardType());
    }
}
