package view.console;

import controller.CardText;
import controller.TableSnapshot;
import model.Card;
import view.GameView;

import java.io.PrintStream;

/** Prints the table in the format required by the spec. Used by the CLI. */
public class ConsoleView implements GameView {
    private static final String BAR = "==================================================================";
    private static final String DASH = "------------------------------------------------------------------";
    private final PrintStream out;
    private int printedLogLines;
    private boolean summaryPending;
    private boolean gameOver;

    public ConsoleView(PrintStream out) { this.out = out; }

    /** True once after a round summary was printed; the app then asks how to continue. */
    public boolean takeSummaryPending() {
        boolean was = summaryPending;
        summaryPending = false;
        return was;
    }

    public boolean isGameOver() { return gameOver; }

    @Override
    public void render(TableSnapshot s) {
        if (s.log().length < printedLogLines) printedLogLines = 0;     // a new round started
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
    public void toast(String message) { out.println("  >> " + message); }

    private void printBoard(TableSnapshot s) {
        out.println(BAR);
        out.println(s.roundTitle() + " | Direction: " + (s.clockwise() ? "CLOCKWISE (--->)" : "COUNTER-CLOCKWISE (<---)")
                + " | " + s.winCondition());
        StringBuilder players = new StringBuilder("Players: ");
        int n = s.seats().length;
        for (int k = 0; k < n; k++) {
            int idx = s.clockwise() ? k : (k == 0 ? 0 : n - k);       // show them in the order play will travel
            TableSnapshot.Seat seat = s.seats()[idx];
            String name = idx == 0 ? "*" + seat.name() + " (You)*" : seat.name();
            players.append("[").append(name).append(": ").append(seat.cards()).append(" cards]");
            if (k < n - 1) players.append(s.clockwise() ? " --> " : " <-- ");
        }
        out.println(players);
        out.println(DASH);
        out.println("Deck Remaining: " + s.deckCount() + " cards | Pending Draw Penalty: " + s.pendingPenalty());
        out.println("Top Card on Table: [" + CardText.of(s.topCard()) + "] | Active Color: " + s.activeColor());
        out.println(DASH);
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
        out.println(BAR);
    }
}
