package controller;

import model.Card;
import model.Color;

/**
 * Immutable picture of the table that the View renders.
 * seats[0] is ALWAYS the player whose hand is shown; the rest follow in clockwise order.
 * Plain arrays are used on purpose (no java.util collections in game-facing code).
 */
public record TableSnapshot(
        String roundTitle, String winCondition, boolean clockwise,
        Seat[] seats, int currentSeat,
        int deckCount, Card topCard, Color activeColor, int pendingPenalty,
        Card[] hand, boolean[] playable, boolean drawnCardPending,
        String[] log, RoundSummary summary) {

    public record Seat(String name, int cards, int score, boolean bot) { }

    /** Non-null only when the round just ended. */
    public record RoundSummary(String winner, String[] names, String[] leftovers, int[] points,
                               int gained, boolean gameOver, String champion) { }

    public boolean myTurn() { return currentSeat == 0; }
}
