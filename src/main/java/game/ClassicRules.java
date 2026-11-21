package game;

import dataStructures.DynamicArray;
import dataStructures.MonotoneStack;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;

public class ClassicRules implements RuleSet {
    private final RuleManager ruleManager = new RuleManager();

    @Override public String getName() { return "classic"; }

    @Override
    public DynamicArray<Card> buildCards() {
        DynamicArray<Card> cards = new DynamicArray<>();
        Color[] colors = {Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW};
        for (Color color : colors) {
            cards.add(new Card(color, CardType.NUMBER, 0));
            for (int value = 1; value <= 9; value++) {
                cards.add(new Card(color, CardType.NUMBER, value));
                cards.add(new Card(color, CardType.NUMBER, value));
            }
            for (int i = 0; i < 2; i++) {
                cards.add(new Card(color, CardType.SKIP, 0));
                cards.add(new Card(color, CardType.REVERSE, 0));
                cards.add(new Card(color, CardType.DRAW_TWO, 0));
            }
        }
        for (int i = 0; i < 4; i++) {
            cards.add(new Card(Color.NONE, CardType.WILD, 0));
            cards.add(new Card(Color.NONE, CardType.WILD_DRAW_FOUR, 0));
        }
        return cards;
    }

    @Override
    public boolean isValidPlay(Card card, Card topCard, Color activeColor, SinglyLinkedList<Card> hand, MonotoneStack penaltyStack) {
        return ruleManager.isValidPlay(card, topCard, activeColor, hand, penaltyStack);
    }

    @Override public String penaltyHint() { return "A penalty is pending: stack a +2/+4 or draw."; }
    @Override public boolean needsColorChoice(Card card) { return card.getCardType().isWildFamily(); }
    @Override public boolean needsTarget(Card card) { return false; }
    @Override public boolean drawUntilPlayable() { return false; }
    @Override public int eliminationLimit() { return 0; }
    @Override public boolean strictPenaltyStack() { return false; }

    @Override
    public void applyEffect(Round round, Player player, Card card, Color chosenColor, Player target) {
        int penalty = card.getCardType().penaltyValue();
        if (penalty > 0) {
            round.addPenalty(penalty, card.getCardType());
            round.advance(1);
            return;
        }
        switch (card.getCardType()) {
            case SKIP:
                round.skipNext();
                break;
            case REVERSE:
                round.reverse();
                break;
            default:
                round.advance(1);
                break;
        }
    }
}
