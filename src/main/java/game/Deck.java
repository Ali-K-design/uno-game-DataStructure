package game;

import dataStructures.CustomQueue;
import dataStructures.DynamicArray;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;

import java.util.Random;

public class Deck {
    private final CustomQueue<Card> drawQueue = new CustomQueue<>();
    private final Random random;

    public Deck() { this(new Random()); }

    public Deck(Random random) { this.random = random; }

    public void createDeck() {
        createDeck(new ClassicRules().buildCards());
    }

    public void createDeck(DynamicArray<Card> cards) {
        shuffle(cards);
        enqueueAll(cards);
    }

    private void shuffle(DynamicArray<Card> cards) {
        for (int i = cards.getSize() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            cards.swap(i, j);
        }
    }

    private void enqueueAll(DynamicArray<Card> cards) {
        for (int i = 0; i < cards.getSize(); i++) {
            drawQueue.enqueue(cards.get(i));
        }
    }

    public Card drawCard(DiscardPile discardPile) {
        if (drawQueue.isEmpty()) {
            refillDeck(discardPile);
        }
        if (drawQueue.isEmpty()) {
            return null;
        }
        return drawQueue.dequeue();
    }

    public void dealCards(DynamicArray<Player> players, int cardsPerPlayer, DiscardPile discardPile) {
        for (int i = 0; i < cardsPerPlayer; i++) {
            for (int j = 0; j < players.getSize(); j++) {
                Card card = drawCard(discardPile);
                if (card != null) {
                    players.get(j).addCard(card);
                }
            }
        }
    }

    private boolean refillDeck(DiscardPile discardPile) {
        if (discardPile.size() <= 1) {
            return false;
        }
        Card topCard = discardPile.removeTopCard();
        DynamicArray<Card> cards = new DynamicArray<>();
        while (!discardPile.isEmpty()) {
            Card card = discardPile.removeTopCard();
            if (card.getCardType() == CardType.WILD || card.getCardType() == CardType.WILD_DRAW_FOUR) {
                card.setColor(Color.NONE);
            }
            cards.add(card);
        }
        shuffle(cards);
        enqueueAll(cards);
        discardPile.addCard(topCard);
        return true;
    }

    public void putBackAndShuffle(Card card) {
        DynamicArray<Card> cards = new DynamicArray<>();
        while (!drawQueue.isEmpty()) {
            cards.add(drawQueue.dequeue());
        }
        cards.add(card);
        shuffle(cards);
        enqueueAll(cards);
    }

    public void appendToEnd(SinglyLinkedList<Card> cards) {
        drawQueue.appendList(cards);
    }

    public void reduceToOne() {
        while (drawQueue.getSize() > 1) {
            drawQueue.dequeue();
        }
    }

    public int getSize() { return drawQueue.getSize(); }
    public boolean isEmpty() { return drawQueue.isEmpty(); }
}
