package game;

import dataStructures.DynamicArray;
import model.Player;

/** A whole game: consecutive rounds until the win condition (score target or number of rounds) is met. */
public class Game {
    public enum Mode { SCORE, ROUNDS }

    private final DynamicArray<Player> players;
    private final ScoreManager scoreManager;
    private final Mode mode;
    private final int limit;
    private Round currentRound;
    private int roundNumber;
    private int roundsPlayed;
    private boolean roundRecorded;
    private boolean finalized;

    public Game(DynamicArray<Player> players, ScoreManager scoreManager, Mode mode, int limit) {
        this.players = players;
        this.scoreManager = scoreManager;
        this.mode = mode;
        this.limit = limit;
    }

    public void startRound() {
        roundNumber++;
        roundRecorded = false;
        currentRound = new Round();
        currentRound.start(players);
    }

    /** Scores the finished round (once). Returns the points the winner earned, or 0 if the round is not over. */
    public int finishRound() {
        if (currentRound == null || !currentRound.isFinished() || roundRecorded) return 0;
        roundRecorded = true;
        roundsPlayed++;
        return scoreManager.recordRound(players, currentRound.getWinner());
    }

    public boolean isGameFinished() {
        if (mode == Mode.ROUNDS) return roundsPlayed >= limit;
        for (int i = 0; i < players.getSize(); i++) {
            if (players.get(i).getCurrentGameScore() >= limit) return true;
        }
        return false;
    }

    /** Highest running total (first player wins ties). */
    public Player getChampion() {
        Player best = null;
        for (int i = 0; i < players.getSize(); i++) {
            Player candidate = players.get(i);
            if (best == null || candidate.getCurrentGameScore() > best.getCurrentGameScore()) best = candidate;
        }
        return best;
    }

    /** Writes the game result to every account (once) and refreshes the leaderboard. */
    public void finalizeGame() {
        if (finalized) return;
        finalized = true;
        Player champion = getChampion();
        for (int i = 0; i < players.getSize(); i++) {
            Player p = players.get(i);
            p.getAccount().addGameResult(p.getCurrentGameScore(), p == champion);
        }
        scoreManager.getLeaderboard().sortByScore();
    }

    public Round getCurrentRound() { return currentRound; }
    public DynamicArray<Player> getPlayers() { return players; }
    public ScoreManager getScoreManager() { return scoreManager; }
    public Mode getMode() { return mode; }
    public int getLimit() { return limit; }
    public int getRoundNumber() { return roundNumber; }
    public int getRoundsPlayed() { return roundsPlayed; }
}
