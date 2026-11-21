package game;

import dataStructures.DynamicArray;
import model.Player;

public class Game {
    public enum Mode { SCORE, ROUNDS }

    private final DynamicArray<Player> players;
    private final ScoreManager scoreManager;
    private final Mode mode;
    private final int limit;
    private final RuleSet rules;
    private Round currentRound;
    private int roundNumber;
    private int roundsPlayed;
    private boolean roundRecorded;
    private boolean finalized;

    public Game(DynamicArray<Player> players, ScoreManager scoreManager, Mode mode, int limit) {
        this(players, scoreManager, mode, limit, new ClassicRules());
    }

    public Game(DynamicArray<Player> players, ScoreManager scoreManager, Mode mode, int limit, RuleSet rules) {
        this.rules = rules;
        this.players = players;
        this.scoreManager = scoreManager;
        this.mode = mode;
        this.limit = limit;
    }

    public void startRound() {
        roundNumber++;
        roundRecorded = false;
        currentRound = new Round(rules);
        currentRound.start(players);
    }

    public int finishRound() {
        if (currentRound == null || !currentRound.isFinished() || roundRecorded) return 0;
        roundRecorded = true;
        roundsPlayed++;
        return scoreManager.recordRound(players, currentRound.getWinner(), currentRound.getEliminationBonus());
    }

    public boolean isGameFinished() {
        if (mode == Mode.ROUNDS) return roundsPlayed >= limit;
        for (int i = 0; i < players.getSize(); i++) {
            if (players.get(i).getCurrentGameScore() >= limit) return true;
        }
        return false;
    }

    public Player getChampion() {
        Player best = null;
        for (int i = 0; i < players.getSize(); i++) {
            Player candidate = players.get(i);
            if (best == null || candidate.getCurrentGameScore() > best.getCurrentGameScore()) best = candidate;
        }
        return best;
    }

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
    public RuleSet getRules() { return rules; }
    public int getLimit() { return limit; }
    public int getRoundNumber() { return roundNumber; }
    public int getRoundsPlayed() { return roundsPlayed; }
}
