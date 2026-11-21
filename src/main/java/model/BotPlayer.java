package model;

import dataStructures.Node;

public class BotPlayer extends Player {
    private static final Color[] COLOR_PRIORITY = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};

    public BotPlayer(String name, Account account) {
        super(name, account);
    }

    @Override
    public boolean isBot() { return true; }

    @Override
    public Action chooseAction(RoundState state) {
        Card drawn = state.getDrawnCard();
        if (drawn != null) return play(drawn, state);

        Card colored = null, weakSeven = null, wild = null, otherWild = null;
        int index = 0;
        for (Node<Card> n = getHand().getHead(); n != null; n = n.getNext(), index++) {
            if (!state.isPlayable(index)) continue;
            Card card = n.getData();
            if (state.getPendingPenalty() > 0) {
                return play(card, state);
            }
            if (card.getCardType() == CardType.WILD) {
                if (wild == null) wild = card;
            } else if (card.getCardType().isWildFamily()) {
                if (otherWild == null) otherWild = card;
            } else if (isSwapSeven(card, state) && !swapHelps(state)) {
                if (weakSeven == null) weakSeven = card;
            } else if (colored == null) {
                colored = card;
            }
        }
        if (colored != null) return play(colored, state);
        if (wild != null) return play(wild, state);
        if (otherWild != null) return play(otherWild, state);
        if (weakSeven != null) return play(weakSeven, state);
        return Action.draw();
    }

    private Action play(Card card, RoundState state) {
        Color color = card.getCardType().isWildFamily() ? bestColor() : Color.NONE;
        Player target = isSwapSeven(card, state) ? bestSwapTarget(state) : null;
        return Action.play(card, color, target);
    }

    private boolean isSwapSeven(Card card, RoundState state) {
        return state.isSevenSwaps() && card.getCardType() == CardType.NUMBER && card.getValue() == 7;
    }

    private Player bestSwapTarget(RoundState state) {
        Player best = null;
        for (Player p : state.getOpponents()) {
            if (best == null || p.getHand().getSize() < best.getHand().getSize()) best = p;
        }
        return best;
    }

    private boolean swapHelps(RoundState state) {
        Player target = bestSwapTarget(state);
        return target != null && target.getHand().getSize() < getHand().getSize() - 1;
    }

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
