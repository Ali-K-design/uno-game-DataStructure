package controller;

import view.GameView;

/** Auth / menu level actions. Methods returning String give back an error message, or null on success. */
public interface AppActions {
    String register(String username, String password, String displayName);
    String login(String username, String password);
    void logout();
    String currentDisplayName();
    LeaderboardRow[] leaderboard();

    /** mode = "score" or "rounds". Throws IllegalArgumentException(message) on invalid config. */
    GameActions startGame(int humans, int bots, String mode, int limit, String[] otherHumanUsernames, GameView view);

    record LeaderboardRow(int rank, String displayName, String username, int totalScore, int games, int wins) { }
}
