package game;
import dataStructures.CustomStack;
import model.Card;

public class DiscardPile {
    private CustomStack<Card> playedStack ;

    DiscardPile(){
        playedStack = new CustomStack<>();
    }

    public void addCard (Card card){
        playedStack.push(card);
    }

    public Card removeTopCard(){
        return playedStack.pop();
    }

    public Card getTopCard(){
        return playedStack.peek();
    }

    public int size(){
        return playedStack.getSize();
    }

    public boolean isEmpty(){
        return playedStack.isEmpty();
    }

}
