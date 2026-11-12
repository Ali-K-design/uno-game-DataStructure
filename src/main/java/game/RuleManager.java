package game;
import dataStructures.CustomStack;
import dataStructures.Node;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.CardType;
import model.Color;

public class RuleManager {
    public boolean isValidPlay(Card card, Card topCard, Color activeColor, SinglyLinkedList<Card> hand, CustomStack<Integer> penaltyStack) {
        if (card == null || topCard == null) {
            return false;
        }

        if (!penaltyStack.isEmpty()) {
            return card.getCardType() == CardType.DRAW_TWO || card.getCardType() == CardType.WILD_DRAW_FOUR;
        }

        if (card.getCardType() == CardType.WILD) {
            return true;
        }

        if (card.getCardType() == CardType.WILD_DRAW_FOUR) {
            return !hasActiveColor(hand, activeColor);
        }

        if (card.getColor() == activeColor) {
            return true;
        }

        if (card.getCardType() == topCard.getCardType()) {
            if (card.getCardType() == CardType.NUMBER) {
                return card.getValue() == topCard.getValue();
            }
            return true;
        }
        return false;
    }

    private boolean hasActiveColor(SinglyLinkedList<Card> hand,Color activeColor) {
        Node<Card> current = hand.getHead();
        while (current != null) {
            Card card = current.getData();
            if (card.getColor()== activeColor) {
                return true;
            }
            current = current.getNext();
        }
        return false;
    }
}
