package game;

import dataStructures.DynamicArray;
import dataStructures.Node;
import dataStructures.SinglyLinkedList;
import model.Card;
import model.Player;

public class ScoreManager {
    private final Leaderboard leaderboard;

    public ScoreManager(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    /** Total value of a hand, computed recursively over the linked list. */
    public int calculateHandPoints(SinglyLinkedList<Card> hand) {
        return calcHandScore(hand.getHead());
    }

    private int calcHandScore(Node<Card> node) {
        if (node == null) return 0;
        return cardPoints(node.getData()) + calcHandScore(node.getNext());
    }

    public int cardPoints(Card card) {
        switch (card.getCardType()) {
            case NUMBER: return card.getValue();
            case SKIP:
            case REVERSE:
            case DRAW_TWO: return 20;
            default: return 50;      // WILD, WILD_DRAW_FOUR
        }
    }

    /** Gives every loser 0 and the winner the sum of all losers' hands. Returns that sum. */
    public int recordRound(DynamicArray<Player> players, Player winner) {
        int winnerScore = 0;
        for (int i = 0; i < players.getSize(); i++) {
            Player player = players.get(i);
            if (player != winner) winnerScore += calculateHandPoints(player.getHand());
        }
        for (int i = 0; i < players.getSize(); i++) {
            Player player = players.get(i);
            player.addRoundScore(player == winner ? winnerScore : 0);
        }
        return winnerScore;
    }

    public Leaderboard getLeaderboard() { return leaderboard; }
}
