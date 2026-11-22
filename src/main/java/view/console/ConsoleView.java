package view.console;

import controller.CardText;
import controller.TableSnapshot;
import model.Card;
import view.GameView;

import java.io.PrintStream;

public class ConsoleView implements GameView {
    private static final String BAR = "==================================================================";
    private static final String DASH = "------------------------------------------------------------------";
    private final PrintStream out;
    private int printedLogLines;
    private boolean summaryPending;
    private boolean gameOver;
    private TableSnapshot last;
    private boolean online;

    public ConsoleView(PrintStream out) { this.out = out; }

    public synchronized boolean takeSummaryPending() {
        boolean was = summaryPending;
        summaryPending = false;
        return was;
    }

    public synchronized boolean isGameOver() { return gameOver; }

    public synchronized void setOnline(boolean online) { this.online = online; }

    public synchronized int seatAt(int displayIndex) {
        if (last == null) return -1;
        int n = last.seats().length;
        if (displayIndex < 0 || displayIndex >= n) return -1;
        return last.clockwise() ? displayIndex : (displayIndex == 0 ? 0 : n - displayIndex);
    }

    @Override
    public synchronized void render(TableSnapshot s) {
        last = s;
        if (s.log().length < printedLogLines) printedLogLines = 0;
        for (int i = printedLogLines; i < s.log().length; i++) out.println("  * " + s.log()[i]);
        printedLogLines = s.log().length;

        if (s.summary() != null) {
            printSummary(s.summary());
            summaryPending = true;
            gameOver = s.summary().gameOver();
            return;
        }
        gameOver = false;
        if (s.myTurn()) printBoard(s);
    }

    @Override
    public synchronized void toast(String message) { out.println("  >> " + message); }

    @Override
    public synchronized void lobby(String roomId, String[] names, int needed) {
        out.println("  >> Room " + roomId + ": waiting for players (" + names.length + "/" + needed + "): " + String.join(", ", names));
    }

    private void printBoard(TableSnapshot s) {
        out.println(BAR);
        out.println(s.roundTitle() + " | Direction: " + (s.clockwise() ? "CLOCKWISE (--->)" : "COUNTER-CLOCKWISE (<---)")
                + " | " + s.winCondition());
        if (s.mercyLimit() > 0) out.println("Variant: NO MERCY (" + s.mercyLimit() + " cards = eliminated; 7 swaps hands, 0 rotates them)");
        StringBuilder players = new StringBuilder("Players: ");
        int n = s.seats().length;
        for (int k = 0; k < n; k++) {
            int idx = s.clockwise() ? k : (k == 0 ? 0 : n - k);
            TableSnapshot.Seat seat = s.seats()[idx];
            String name = idx == 0 ? "*" + seat.name() + " (You)*" : seat.name();
            String label = s.mercyLimit() > 0 ? k + ":" + name : name;
            players.append("[").append(label).append(": ")
                   .append(seat.eliminated() ? "OUT" : seat.cards() + " cards").append("]");
            if (k < n - 1) players.append(s.clockwise() ? " --> " : " <-- ");
        }
        out.println(players);
        out.println(DASH);
        out.println("Deck Remaining: " + s.deckCount() + " cards | Pending Draw Penalty: " + s.pendingPenalty());
        out.println("Top Card on Table: [" + CardText.of(s.topCard()) + "] | Active Color: " + s.activeColor());
        out.println(DASH);
        if (s.mercyLimit() > 0) out.println("Your cards: " + s.hand().length + "/" + s.mercyLimit());
        out.println("Your Hand (" + s.seats()[0].name() + "):" + (s.drawnCardPending() ? "   (last card = the one you just drew)" : ""));
        for (int i = 0; i < s.hand().length; i++) {
            Card c = s.hand()[i];
            out.println("[" + i + "] " + CardText.of(c) + (s.playable()[i] ? "" : "   (not playable)"));
        }
        out.println(BAR);
    }

    private void printSummary(TableSnapshot.RoundSummary r) {
        out.println(BAR);
        out.println("ROUND OVER - Winner: " + r.winner());
        for (int i = 0; i < r.names().length; i++) {
            if (r.names()[i].equals(r.winner())) continue;
            out.println("  " + r.names()[i] + ": " + r.leftovers()[i] + "  => " + r.points()[i] + " pts");
        }
        out.println("Points added to " + r.winner() + ": " + r.gained());
        if (r.gameOver()) out.println("GAME OVER - Champion: " + r.champion());
        if (online) out.println(r.gameOver() ? "Type quit_game to go back to the menu." : "Type next for the next round (or quit_game to leave).");
        out.println(BAR);
    }
}
