package model;

/**
 * Read-only view of the round given to a player when it must decide.
 * playable[i] tells whether the i-th card of the player's hand (list order) is a legal play right now.
 */
public final class RoundState {
    private final Card topCard;
    private final Color activeColor;
    private final int pendingPenalty;
    private final boolean[] playable;
    private final Card drawnCard;   // non-null only right after a draw that produced a playable card

    public RoundState(Card topCard, Color activeColor, int pendingPenalty, boolean[] playable, Card drawnCard) {
        this.topCard = topCard;
        this.activeColor = activeColor;
        this.pendingPenalty = pendingPenalty;
        this.playable = playable;
        this.drawnCard = drawnCard;
    }

    public Card getTopCard() { return topCard; }
    public Color getActiveColor() { return activeColor; }
    public int getPendingPenalty() { return pendingPenalty; }
    public Card getDrawnCard() { return drawnCard; }
    public boolean isPlayable(int handIndex) { return handIndex >= 0 && handIndex < playable.length && playable[handIndex]; }
}
