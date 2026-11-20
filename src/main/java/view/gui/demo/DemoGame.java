package view.gui.demo;

import controller.GameActions;
import controller.TableSnapshot;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import model.Card;
import model.CardType;
import model.Color;
import view.GameView;

import java.util.ArrayList;
import java.util.Random;

/** THROWAWAY simulation of a round (simplified rules) just to exercise every UI state. */
final class DemoGame implements GameActions {
    private static final Color[] COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
    private final GameView view;
    private final String winText;
    private final String[] names;
    private final int[] counts;
    private final boolean[] bots;
    private final ArrayList<Card> hand = new ArrayList<>();
    private final ArrayList<String> log = new ArrayList<>();
    private final Random rnd = new Random();
    private int turn, pending, round = 1, n;
    private boolean clockwise = true, drawnPending;
    private Card top;
    private Color active;
    private TableSnapshot.RoundSummary summary;

    DemoGame(String me, int total, int humans, String winText, GameView view) {
        this.view = view; this.winText = winText; this.n = total;
        names = new String[total]; counts = new int[total]; bots = new boolean[total];
        for (int i = 0; i < total; i++) { names[i] = i == 0 ? me : (i < humans ? "Player " + (i + 1) : "Bot_" + (i - humans + 1)); bots[i] = i >= humans; }
        resetRound();
    }

    private void resetRound() {
        hand.clear(); log.clear(); pending = 0; turn = 0; drawnPending = false; summary = null; clockwise = true;
        for (int i = 0; i < n; i++) counts[i] = 7;
        for (int i = 0; i < 7; i++) hand.add(randomCard());
        top = new Card(COLORS[rnd.nextInt(4)], CardType.NUMBER, rnd.nextInt(10));
        active = top.getColor();
        say("Round started");
        view.render(snapshot());
    }

    private Card randomCard() {
        int t = rnd.nextInt(20);
        Color c = COLORS[rnd.nextInt(4)];
        if (t < 12) return new Card(c, CardType.NUMBER, rnd.nextInt(10));
        if (t < 14) return new Card(c, CardType.SKIP, 0);
        if (t < 16) return new Card(c, CardType.REVERSE, 0);
        if (t < 18) return new Card(c, CardType.DRAW_TWO, 0);
        return new Card(Color.NONE, t == 18 ? CardType.WILD : CardType.WILD_DRAW_FOUR, 0);
    }

    private boolean isWild(Card c) { return c.getCardType() == CardType.WILD || c.getCardType() == CardType.WILD_DRAW_FOUR; }

    private boolean valid(Card c) {
        if (pending > 0) return c.getCardType() == CardType.DRAW_TWO || c.getCardType() == CardType.WILD_DRAW_FOUR;
        if (isWild(c) || c.getColor() == active) return true;
        return c.getCardType() == top.getCardType() && (c.getCardType() != CardType.NUMBER || c.getValue() == top.getValue());
    }

    private void say(String s) { log.add(s); }

    private TableSnapshot snapshot() {
        TableSnapshot.Seat[] seats = new TableSnapshot.Seat[n];
        for (int i = 0; i < n; i++) seats[i] = new TableSnapshot.Seat(names[i], counts[i], 0, bots[i]);
        Card[] h = hand.toArray(new Card[0]);
        boolean[] ok = new boolean[h.length];
        for (int i = 0; i < h.length; i++) ok[i] = valid(h[i]) && (!drawnPending || i == h.length - 1);
        return new TableSnapshot("ROUND #" + round, winText, clockwise, seats, turn, 60, top, active, pending,
                h, ok, drawnPending, log.toArray(new String[0]), summary);
    }

    private void advance(int steps) {
        turn = (((turn + (clockwise ? steps : -steps)) % n) + n) % n;
        view.render(snapshot());
        if (turn != 0) {
            PauseTransition p = new PauseTransition(Duration.millis(1100));
            p.setOnFinished(e -> botTurn());
            p.play();
        }
    }

    private void botTurn() {
        int b = turn;
        if (pending > 0 && rnd.nextInt(4) != 0) {
            counts[b] += pending; say(names[b] + " draws " + pending); view.toast(names[b] + " draws " + pending); pending = 0; advance(1); return;
        }
        if (pending == 0 && rnd.nextInt(5) == 0) { counts[b]++; say(names[b] + " draws a card"); advance(1); return; }
        Card c = pending > 0 ? new Card(COLORS[rnd.nextInt(4)], CardType.DRAW_TWO, 0) : randomCard();
        top = c; active = isWild(c) ? COLORS[rnd.nextInt(4)] : c.getColor(); counts[b]--;
        say(names[b] + " played " + c.getColor() + " " + c.getCardType());
        if (counts[b] == 0) { finish(b); return; }
        applyEffect(c);
    }

    private void applyEffect(Card c) {
        switch (c.getCardType()) {
            case SKIP: view.toast("Skip!"); advance(2); break;
            case REVERSE: clockwise = !clockwise; view.toast("Reverse!"); advance(n == 2 ? 2 : 1); break;
            case DRAW_TWO: pending += 2; view.toast("+2 penalty"); advance(1); break;
            case WILD_DRAW_FOUR: pending += 4; view.toast("+4 \u2192 " + active); advance(1); break;
            case WILD: view.toast("Color: " + active); advance(1); break;
            default: advance(1);
        }
    }

    private void finish(int winner) {
        String[] nm = new String[n]; String[] left = new String[n]; int[] pts = new int[n]; int sum = 0;
        for (int i = 0; i < n; i++) { nm[i] = names[i]; left[i] = counts[i] + " cards"; pts[i] = i == winner ? 0 : counts[i] * 7; sum += pts[i]; }
        summary = new TableSnapshot.RoundSummary(names[winner], nm, left, pts, sum, false, null);
        view.render(snapshot());
    }

    // ---- GameActions
    @Override public void play(int i, Color chosen) {
        if (turn != 0 || i < 0 || i >= hand.size()) return;
        Card c = hand.get(i);
        if (!valid(c) || (drawnPending && i != hand.size() - 1)) { view.toast("Illegal move"); return; }
        hand.remove(i); counts[0]--; drawnPending = false;
        top = c; active = isWild(c) ? chosen : c.getColor();
        say("You played " + c.getColor() + " " + c.getCardType());
        if (counts[0] == 0) { finish(0); return; }
        applyEffect(c);
    }

    @Override public void draw() {
        if (turn != 0) return;
        if (pending > 0) {
            for (int i = 0; i < pending; i++) hand.add(randomCard());
            counts[0] += pending; say("You took " + pending); pending = 0; advance(1); return;
        }
        Card c = randomCard(); hand.add(c); counts[0]++; say("You drew a card");
        drawnPending = valid(c);
        if (drawnPending) view.render(snapshot()); else advance(1);
    }

    @Override public void playDrawn(Color chosen) { play(hand.size() - 1, chosen); }
    @Override public void pass() { drawnPending = false; advance(1); }
    @Override public void nextRound() { round++; resetRound(); }
    @Override public void quit() { }
    @Override public void drawMachine() { if (turn == 0 && pending > 0) { int k = rnd.nextInt(pending + 1); for (int i = 0; i < k; i++) hand.add(randomCard()); counts[0] += k; pending = 0; say("Lucky draw: " + k); advance(1); } }
    @Override public String scoreboardText() { return "(demo) no history"; }
    @Override public void cheat(String raw) { say("cheat: " + raw); view.toast("cheat accepted (demo)"); view.render(snapshot()); }
}
