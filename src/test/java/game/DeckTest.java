package game;

import model.Card;
import model.CardType;
import model.Color;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {
    @Test
    void standardDeckHas108CorrectCards() {
        Deck deck = new Deck(new Random(1));
        deck.createDeck();
        assertEquals(108, deck.getSize());
        DiscardPile pile = new DiscardPile();
        int[] byType = new int[CardType.values().length];
        int zeros = 0, nines = 0, coloredWild = 0;
        while (!deck.isEmpty()) {
            Card c = deck.drawCard(pile);
            byType[c.getCardType().ordinal()]++;
            if (c.getCardType() == CardType.NUMBER && c.getValue() == 0) zeros++;
            if (c.getCardType() == CardType.NUMBER && c.getValue() == 9) nines++;
            boolean wild = c.getCardType() == CardType.WILD || c.getCardType() == CardType.WILD_DRAW_FOUR;
            if (wild && c.getColor() != Color.NONE) coloredWild++;
        }
        assertEquals(76, byType[CardType.NUMBER.ordinal()]);
        assertEquals(8, byType[CardType.SKIP.ordinal()]);
        assertEquals(8, byType[CardType.REVERSE.ordinal()]);
        assertEquals(8, byType[CardType.DRAW_TWO.ordinal()]);
        assertEquals(4, byType[CardType.WILD.ordinal()]);
        assertEquals(4, byType[CardType.WILD_DRAW_FOUR.ordinal()]);
        assertEquals(4, zeros);
        assertEquals(8, nines);
        assertEquals(0, coloredWild);
    }

    @Test
    void emptyDeckIsRebuiltFromDiscardKeepingTheTopCard() {
        Deck deck = new Deck(new Random(2));
        deck.createDeck();
        DiscardPile pile = new DiscardPile();
        for (int i = 0; i < 10; i++) pile.addCard(deck.drawCard(pile));
        Card top = pile.getTopCard();
        deck.reduceToOne();
        deck.drawCard(pile);                       // takes the last card
        assertTrue(deck.isEmpty());
        assertNotNull(deck.drawCard(pile));        // triggers the refill
        assertEquals(8, deck.getSize());           // 9 recycled cards - 1 just drawn
        assertSame(top, pile.getTopCard());
        assertEquals(1, pile.size());
    }
}
