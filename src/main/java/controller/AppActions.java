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

    default boolean isOnline() { return false; }

    default GameActions hostRoom(int humans, int bots, String mode, int limit, String variant, GameView view) {
        throw new IllegalArgumentException("Not connected to a server.");
    }

    default RoomInfo[] listRooms() { return new RoomInfo[0]; }

    default GameActions joinRoom(int roomId, GameView view) {
        throw new IllegalArgumentException("Not connected to a server.");
    }

    record RoomInfo(int id, String host, int humans, int joined, int bots, String mode, int limit, String variant) { }

    record LeaderboardRow(int rank, String displayName, String username, int totalScore, int games, int wins) { }
}
