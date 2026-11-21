package model;

public final class RoundState {
    private final Card topCard;
    private final Color activeColor;
    private final int pendingPenalty;
    private final boolean[] playable;
    private final Card drawnCard;
    private final Player[] opponents;
    private final boolean sevenSwaps;

    public RoundState(Card topCard, Color activeColor, int pendingPenalty, boolean[] playable, Card drawnCard) {
        this(topCard, activeColor, pendingPenalty, playable, drawnCard, new Player[0], false);
    }

    public RoundState(Card topCard, Color activeColor, int pendingPenalty, boolean[] playable, Card drawnCard,
                      Player[] opponents, boolean sevenSwaps) {
        this.topCard = topCard;
        this.activeColor = activeColor;
        this.pendingPenalty = pendingPenalty;
        this.playable = playable;
        this.drawnCard = drawnCard;
        this.opponents = opponents;
        this.sevenSwaps = sevenSwaps;
    }

    public Card getTopCard() { return topCard; }
    public Color getActiveColor() { return activeColor; }
    public int getPendingPenalty() { return pendingPenalty; }
    public Card getDrawnCard() { return drawnCard; }
    public Player[] getOpponents() { return opponents; }
    public boolean isSevenSwaps() { return sevenSwaps; }
    public boolean isPlayable(int handIndex) { return handIndex >= 0 && handIndex < playable.length && playable[handIndex]; }
}
