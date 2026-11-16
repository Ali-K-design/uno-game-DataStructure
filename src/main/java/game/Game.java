package game;

import dataStructures.DynamicArray;
import model.Account;
import model.Player;

public class Game {

    private DynamicArray<Player> players;
    private ScoreManager scoreManager;
    private Round currentRound;
    private int targetLimit;
    public Game(DynamicArray<Player> players, ScoreManager scoreManager, int targetLimit) {

        this.players = players;
        this.scoreManager = scoreManager;
        this.targetLimit = targetLimit;
    }

    public void startRound() {

        currentRound = new Round();
        currentRound.start(players);
    }

    public void finishRound() {

        if (!currentRound.isFinished()) {
            return;
        }
        scoreManager.recordRound(players, currentRound.getWinner());
        scoreManager.getLeaderboard().sortByScore();
    }

    public boolean isGameFinished() {

        for (int i = 0; i < players.getSize(); i++) {
            Account account = players.get(i).getAccount();
            if (account.getTotalScore() >= targetLimit) {
                return true;
            }
        }
        return false;
    }

    public Account getWinner() {

        Account winner = null;
        for (int i = 0; i < players.getSize(); i++) {
            Account account = players.get(i).getAccount();
            if (winner == null || account.getTotalScore() > winner.getTotalScore()) {
                winner = account;
            }
        }
        return winner;
    }

    public Round getCurrentRound() {

        return currentRound;
    }
}
