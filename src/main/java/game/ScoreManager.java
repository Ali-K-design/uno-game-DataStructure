package game;

import dataStructures.DynamicArray;
import dataStructures.Node;
import dataStructures.SinglyLinkedList;
import model.Account;
import model.Card;
import model.Player;

import static model.CardType.*;

public class ScoreManager {
    private Leaderboard leaderboard;
    public ScoreManager(Leaderboard leaderboard) {

        this.leaderboard = leaderboard;
    }

    public int calculateHandPoints(SinglyLinkedList<Card> hand) {

        int total = 0;
        Node<Card> current = hand.getHead();
        while (current != null) {
            Card card = current.getData();
            switch (card.getCardType()) {
                case NUMBER:
                    total += card.getValue();
                    break;
                case SKIP:
                case REVERSE:
                case DRAW_TWO:
                    total += 20;
                    break;
                case WILD:
                case WILD_DRAW_FOUR:
                    total += 50;
                    break;
            }
            current = current.getNext();
        }
        return total;
    }

    public void recordRound(DynamicArray<Player> players, Player winner) {

        int winnerScore = 0;
        for (int i = 0; i < players.getSize(); i++) {
            Player player = players.get(i);
            if (player != winner) {
                winnerScore += calculateHandPoints(player.getHand());
            }
        }
        for (int i = 0; i < players.getSize(); i++) {
            Player player = players.get(i);
            if (player == winner) {
                player.getAccount().addRoundScore(winnerScore);
            } else {
                player.getAccount().addRoundScore(0);
            }
        }
    }

    public boolean hasReachedTarget(Account account, int targetLimit) {

        return account.getTotalScore() >= targetLimit;
    }

    public Leaderboard getLeaderboard() {

        return leaderboard;
    }
}