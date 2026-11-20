package model;

import dataStructures.Node;
import dataStructures.SinglyLinkedList;

public abstract class Player {
    private final Account account;
    private final String name;
    private int currentGameScore;
    private SinglyLinkedList<Card> hand;
    private final SinglyLinkedList<Integer> roundScores;

    public Player(String name, Account account) {
        this.name = name;
        this.account = account;
        this.hand = new SinglyLinkedList<>();
        this.roundScores = new SinglyLinkedList<>();
    }

    /** Polymorphic decision point: bots decide, humans wait for console/GUI input. */
    public abstract Action chooseAction(RoundState state);

    public abstract boolean isBot();

    public void addCard(Card card) { hand.addLast(card); }

    public void clearHand() { hand = new SinglyLinkedList<>(); }

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

    /** Records the points this player earned in one round and updates the running game total. */
    public void addRoundScore(int points) {
        roundScores.addLast(points);
        currentGameScore += points;
    }

    public String getName() { return name; }
    public Account getAccount() { return account; }
    public SinglyLinkedList<Card> getHand() { return hand; }
    public int getCurrentGameScore() { return currentGameScore; }
    public SinglyLinkedList<Integer> getRoundScores() { return roundScores; }
}
