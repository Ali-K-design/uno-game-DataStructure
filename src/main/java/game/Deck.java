package game;

import dataStructures.CustomQueue;
import dataStructures.DynamicArray;
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

    /** Builds the standard 108-card deck, shuffles it (Fisher-Yates) and loads it into the queue. */
    public void createDeck() {
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

    /** Draws one card, rebuilding the deck from the discard pile first if it ran out. Null = nothing left at all. */
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

    /** Spec 3.4: keep the table card, reset Wild colours, shuffle the rest back into the queue. */
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

    /** Returns a card to the deck and reshuffles everything (used when the first table card is a +4). */
    public void putBackAndShuffle(Card card) {
        DynamicArray<Card> cards = new DynamicArray<>();
        while (!drawQueue.isEmpty()) {
            cards.add(drawQueue.dequeue());
        }
        cards.add(card);
        shuffle(cards);
        enqueueAll(cards);
    }

    /** Cheat support: drop cards until only one is left. */
    public void reduceToOne() {
        while (drawQueue.getSize() > 1) {
            drawQueue.dequeue();
        }
    }

    public int getSize() { return drawQueue.getSize(); }
    public boolean isEmpty() { return drawQueue.isEmpty(); }
}
