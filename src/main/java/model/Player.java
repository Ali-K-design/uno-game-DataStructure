package model;
import dataStructures.Node;
import dataStructures.SinglyLinkedList;

public  abstract class Player {
    Account account;
    String name ;
    int currentGameScore ;
    SinglyLinkedList<Card> hand ;
    SinglyLinkedList<Integer> roundScores ;

    public Player(String name, Account account) {
        this.name = name;
        this.account = account;
        this.hand = new SinglyLinkedList<>();
    }

    public void addCard(Card card) {
        hand.addLast(card);
    }

    public String getName() {
        return name;
    }

    public SinglyLinkedList<Card> getHand() {
        return hand;
    }

    public Account getAccount() {
        return account;
    }

    public boolean removeCard(Card card) {
        Node<Card> current = hand.getHead();
        int index = 0;
        while (current != null) {
            if (current.getData() == card) {
                hand.removeAt(index);
                return true;
            }
            current = current.getNext();
            index++;
        }
        return false;
    }
}
