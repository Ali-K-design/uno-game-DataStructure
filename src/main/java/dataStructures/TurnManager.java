package dataStructures;
import model.Player;
import dataStructures.DoublyNode;
import dataStructures.DoublyCircularLinkedList;

public class TurnManager {
    private DoublyCircularLinkedList<Player> players;
    private DoublyNode<Player> current;
    private boolean isClockwise;

    public TurnManager() {
        players = new DoublyCircularLinkedList<>();
        current = null;
        isClockwise = true;
    }

    public void addPlayer(Player p) {
        players.add(p);
        if (current == null) {
            current = players.getHead();
        }
    }

    public void handleReverseCard() {
        if (players.getSize() == 2) {
            advanceTurn(2);
        } else {
            reverseDirection();
            advanceTurn(1);
        }
    }

    public void skipTurn() {
        advanceTurn(2);
    }

    public Player getCurrentPlayer() {
        if (current == null) {
            return null;
        }
        return current.getData();
    }

    public Player peekNextPlayer() {
        if (current == null) {
            return null;
        }
        if (isClockwise) {
            return current.getNext().getData();
        }
        return current.getPrev().getData();
    }

    public void advanceTurn(int steps) {
        if (current == null || steps < 0) {
            return;
        }
        for (int i = 0; i < steps; i++) {
            if (isClockwise) {
                current = current.getNext();
            } else {
                current = current.getPrev();
            }
        }
    }

    public void reverseDirection() {
        isClockwise = !isClockwise;
    }

    public boolean isClockwise() {
        return isClockwise;
    }
}

