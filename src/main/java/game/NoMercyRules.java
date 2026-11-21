package game;

import dataStructures.DynamicArray;
import dataStructures.MonotoneStack;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;

public class NoMercyRules extends ClassicRules {
    public static final int ELIMINATION_LIMIT = 25;

    @Override public String getName() { return "nomercy"; }

    @Override
    public DynamicArray<Card> buildCards() {
        DynamicArray<Card> cards = super.buildCards();                 // the 108 classic cards
        Color[] colors = {Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW};
        for (Color color : colors) {
            for (int i = 0; i < 2; i++) {
                cards.add(new Card(color, CardType.SKIP_EVERYONE, 0));
                cards.add(new Card(color, CardType.DISCARD_ALL, 0));
            }
        }
        for (int i = 0; i < 4; i++) {
            cards.add(new Card(Color.NONE, CardType.WILD_DRAW_SIX, 0));
            cards.add(new Card(Color.NONE, CardType.WILD_DRAW_TEN, 0));
            cards.add(new Card(Color.NONE, CardType.COLOR_ROULETTE, 0));
        }
        return cards;
    }

    @Override
    public boolean isValidPlay(Card card, Card topCard, Color activeColor, SinglyLinkedList<Card> hand, MonotoneStack penaltyStack) {
        if (card == null || topCard == null) return false;
        if (!penaltyStack.isEmpty()) {
            int value = card.getCardType().penaltyValue();
            return value > 0 && penaltyStack.canPush(value);
        }
        CardType type = card.getCardType();
        if (type == CardType.WILD_DRAW_SIX || type == CardType.WILD_DRAW_TEN || type == CardType.COLOR_ROULETTE) {
            return true;
        }
        return super.isValidPlay(card, topCard, activeColor, hand, penaltyStack);
    }

    @Override public String penaltyHint() { return "A penalty is pending: stack a penalty card worth at least as much as the top one, or draw."; }
    @Override public boolean needsTarget(Card card) { return card.getCardType() == CardType.NUMBER && card.getValue() == 7; }
    @Override public boolean drawUntilPlayable() { return true; }
    @Override public int eliminationLimit() { return ELIMINATION_LIMIT; }
    @Override public boolean strictPenaltyStack() { return true; }

    @Override
    public void applyEffect(Round round, Player player, Card card, Color chosenColor, Player target) {
        switch (card.getCardType()) {
            case SKIP_EVERYONE:
                round.event(player.getName() + " skips everyone and plays again.");
                round.advance(round.activePlayerCount());
                return;
            case DISCARD_ALL: {
                final Color color = card.getColor();
                SinglyLinkedList<Card> dumped = player.getHand().removeIf(c -> c.getColor() == color);
                round.slipUnderTop(dumped);
                round.event(player.getName() + " dumped " + dumped.getSize() + " more " + color + " card(s).");
                if (player.getHand().isEmpty()) {
                    round.declareWinner(player);
                } else {
                    round.advance(1);
                }
                return;
            }
            case COLOR_ROULETTE: {
                Player victim = round.peekNextPlayer();
                int drew = round.drawUntilColor(victim, chosenColor);
                round.event(victim.getName() + " drew " + drew + " card(s) looking for " + chosenColor + " and loses the turn.");
                round.enforceMercy();
                round.advance(victim.isEliminated() ? 1 : 2);
                return;
            }
            case NUMBER:
                if (card.getValue() == 7) {
                    round.swapHands(player, target);
                    advanceAfterHandChange(round);
                    return;
                }
                if (card.getValue() == 0) {
                    round.rotateHands();
                    advanceAfterHandChange(round);
                    return;
                }
                break;
            default:
                break;
        }
        super.applyEffect(round, player, card, chosenColor, target);
    }

    private void advanceAfterHandChange(Round round) {
        boolean currentGone = round.enforceMercy();
        if (!currentGone && !round.isFinished()) round.advance(1);
    }
}
