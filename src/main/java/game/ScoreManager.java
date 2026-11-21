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

    public int calculateHandPoints(SinglyLinkedList<Card> hand) {
        return handPoints(hand);
    }

    public static int handPoints(SinglyLinkedList<Card> hand) {
        return sumFrom(hand.getHead());
    }

    private static int sumFrom(Node<Card> node) {
        if (node == null) return 0;
        return pointsOf(node.getData()) + sumFrom(node.getNext());
    }

    public int cardPoints(Card card) { return pointsOf(card); }

    public static int pointsOf(Card card) {
        switch (card.getCardType()) {
            case NUMBER: return card.getValue();
            case SKIP:
            case REVERSE:
            case DRAW_TWO: return 20;
            case SKIP_EVERYONE:
            case DISCARD_ALL: return 30;
            case WILD_DRAW_SIX: return 60;
            case WILD_DRAW_TEN: return 80;
            default: return 50;
        }
    }

    public int recordRound(DynamicArray<Player> players, Player winner) {
        return recordRound(players, winner, 0);
    }

    public int recordRound(DynamicArray<Player> players, Player winner, int bonus) {
        int winnerScore = bonus;
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
