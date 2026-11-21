package model;

import dataStructures.Node;
import dataStructures.SinglyLinkedList;

public abstract class Player {
    private final Account account;
    private final String name;
    private int currentGameScore;
    private boolean eliminated;
    private SinglyLinkedList<Card> hand;
    private final SinglyLinkedList<Integer> roundScores;

    public Player(String name, Account account) {
        this.name = name;
        this.account = account;
        this.hand = new SinglyLinkedList<>();
        this.roundScores = new SinglyLinkedList<>();
    }

    public abstract Action chooseAction(RoundState state);

    public abstract boolean isBot();

    public void addCard(Card card) { hand.addLast(card); }

    public void clearHand() { hand = new SinglyLinkedList<>(); }

    public SinglyLinkedList<Card> releaseHand() {
        SinglyLinkedList<Card> old = hand;
        hand = new SinglyLinkedList<>();
        return old;
    }

    public void swapHandWith(Player other) {
        SinglyLinkedList<Card> mine = this.hand;
        this.hand = other.hand;
        other.hand = mine;
    }

    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

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
