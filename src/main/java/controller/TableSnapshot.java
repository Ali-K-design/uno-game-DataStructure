package controller;

import model.Card;
import model.Color;

public record TableSnapshot(
        String roundTitle, String winCondition, boolean clockwise,
        Seat[] seats, int currentSeat,
        int deckCount, Card topCard, Color activeColor, int pendingPenalty,
        Card[] hand, boolean[] playable, boolean drawnCardPending,
        String[] log, RoundSummary summary,
        String variant, int mercyLimit) {

    public record Seat(String name, int cards, int score, boolean bot, boolean eliminated) { }

    public record RoundSummary(String winner, String[] names, String[] leftovers, int[] points,
                               int gained, boolean gameOver, String champion) { }

    public boolean myTurn() { return currentSeat == 0 && !seats[0].eliminated(); }

    public boolean sevensNeedTarget() { return "nomercy".equals(variant); }
}
