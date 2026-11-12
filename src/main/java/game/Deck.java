package game;
import dataStructures.CustomQueue;
import dataStructures.DynamicArray;
import model.Card;
import model.CardType;
import model.Color;
import model.Player;
import java.util.Random;

public class Deck {
     private CustomQueue<Card> drawQueue ;
     private Random random ;

     public Deck(){
         drawQueue = new CustomQueue<>();
         random = new Random();
     }

     public void createDeck(){
         DynamicArray<Card> cards = new DynamicArray<>();
         Color[] colors = {Color.BLUE,Color.GREEN,Color.RED,Color.YELLOW} ;
         for (Color color : colors){
             cards.add(new Card(color , CardType.NUMBER , 0));

             for (int i = 0; i < 9; i++) {
                 cards.add(new Card(color , CardType.NUMBER , i));
                 cards.add(new Card(color , CardType.NUMBER , i));
             }

             for (int i = 0; i < 2; i++) {
                 cards.add(new Card(color , CardType.SKIP ,0));
                 cards.add(new Card(color , CardType.REVERSE ,0 ));
                 cards.add(new Card(color , CardType.DRAW_TWO ,0));
             }
             for (int i = 0; i < 4; i++) {
                 cards.add(new Card(color , CardType.WILD , 0));
                 cards.add(new Card(color , CardType.WILD_DRAW_FOUR , 0));
             }
             shuffle(cards);

             for (int i = 0; i < cards.getSize(); i++) {
                 drawQueue.enqueue(cards.get(i));
             }
         }
     }

    private void shuffle(DynamicArray<Card> cards) {
        for (int i = cards.getSize() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            cards.swap(i, j);
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
                    players.get(i).addCard(card);
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
            cards.add(discardPile.removeTopCard());
        }
        shuffle(cards);
        for (int i = 0; i < cards.getSize(); i++) {
            drawQueue.enqueue(cards.get(i));
        }
        discardPile.addCard(topCard);
        return true;
    }

    public int getSize() {
        return drawQueue.getSize();
    }

    public boolean isEmpty() {
        return drawQueue.isEmpty();
    }

}
