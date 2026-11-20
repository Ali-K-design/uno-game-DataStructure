package model;

import dataStructures.Node;

/** Deterministic bot following section 3.6 of the project spec. */
public class BotPlayer extends Player {
    private static final Color[] COLOR_PRIORITY = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};

    public BotPlayer(String name, Account account) {
        super(name, account);
    }

    @Override
    public boolean isBot() { return true; }

    @Override
    public Action chooseAction(RoundState state) {
        // Just drew a playable card: play it right away.
        Card drawn = state.getDrawnCard();
        if (drawn != null) return Action.play(drawn, colorFor(drawn));

        Card colored = null, wild = null, wildFour = null;
        int index = 0;
        for (Node<Card> n = getHand().getHead(); n != null; n = n.getNext(), index++) {
            if (!state.isPlayable(index)) continue;
            Card card = n.getData();
            if (state.getPendingPenalty() > 0) {
                return Action.play(card, colorFor(card));      // first +2 / +4 found
            }
            if (card.getCardType() == CardType.WILD) {
                if (wild == null) wild = card;
            } else if (card.getCardType() == CardType.WILD_DRAW_FOUR) {
                if (wildFour == null) wildFour = card;
            } else if (colored == null) {
                colored = card;
            }
        }
        if (colored != null) return Action.play(colored, Color.NONE);
        if (wild != null) return Action.play(wild, bestColor());
        if (wildFour != null) return Action.play(wildFour, bestColor());
        return Action.draw();
    }

    private Color colorFor(Card card) {
        boolean isWild = card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR;
        return isWild ? bestColor() : Color.NONE;
    }

    /** Most frequent colour in hand; ties and empty hands follow RED, BLUE, GREEN, YELLOW. */
    private Color bestColor() {
        int[] counts = new int[COLOR_PRIORITY.length];
        for (Node<Card> n = getHand().getHead(); n != null; n = n.getNext()) {
            for (int i = 0; i < COLOR_PRIORITY.length; i++) {
                if (n.getData().getColor() == COLOR_PRIORITY[i]) counts[i]++;
            }
        }
        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) best = i;
        }
        return COLOR_PRIORITY[best];
    }
}
