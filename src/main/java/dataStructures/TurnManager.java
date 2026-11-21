package dataStructures;
import model.Player;
import dataStructures.DoublyNode;
import dataStructures.DynamicArray;
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
        int size = players.getSize();
        if (size == 0) {
            return;
        }
        int effective = steps % size;
        for (int i = 0; i < effective; i++) {
            if (isClockwise) {
                current = current.getNext();
            } else {
                current = current.getPrev();
            }
        }
    }

    public int getPlayerCount() {
        return players.getSize();
    }

    public void removePlayer(Player player) {
        DoublyNode<Player> node = players.findNode(player);
        if (node == null) {
            return;
        }
        if (node == current) {
            current = players.getSize() == 1 ? null : (isClockwise ? node.getNext() : node.getPrev());
        }
        players.remove(node);
    }

    public DynamicArray<Player> getPlayersInOrder() {
        DynamicArray<Player> order = new DynamicArray<>();
        DoublyNode<Player> node = current;
        for (int i = 0; i < players.getSize(); i++) {
            order.add(node.getData());
            node = isClockwise ? node.getNext() : node.getPrev();
        }
        return order;
    }

    public void reverseDirection() {
        isClockwise = !isClockwise;
    }

    public boolean isClockwise() {
        return isClockwise;
    }
}

