package game;

import dataStructures.DynamicArray;
import dataStructures.MonotoneStack;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.Color;
import model.Player;

public interface RuleSet {
    String getName();
    DynamicArray<Card> buildCards();

    boolean isValidPlay(Card card, Card topCard, Color activeColor, SinglyLinkedList<Card> hand, MonotoneStack penaltyStack);

    String penaltyHint();

    boolean needsColorChoice(Card card);

    boolean needsTarget(Card card);

    boolean drawUntilPlayable();

    int eliminationLimit();

    boolean strictPenaltyStack();
    void applyEffect(Round round, Player player, Card card, Color chosenColor, Player target);
}
