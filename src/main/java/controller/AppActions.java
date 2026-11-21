package controller;

import view.GameView;

public interface AppActions {
    String register(String username, String password, String displayName);
    String login(String username, String password);
    void logout();
    String currentDisplayName();
    LeaderboardRow[] leaderboard();

    GameActions startGame(int humans, int bots, String mode, int limit, String[] otherHumanUsernames,
                          String variant, GameView view);

    default GameActions startGame(int humans, int bots, String mode, int limit, String[] otherHumanUsernames, GameView view) {
        return startGame(humans, bots, mode, limit, otherHumanUsernames, "classic", view);
    }

    record LeaderboardRow(int rank, String displayName, String username, int totalScore, int games, int wins) { }
}
