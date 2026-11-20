package model;

/** Persistent user account: identity plus lifetime statistics (used by the Leaderboard). */
public class Account {
    private String username;
    private String password;
    private String displayName;
    private int totalScore;
    private int gamesPlayed;
    private int wins;

    public Account(String username, String password, String displayName) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    /** Called once when a game ends: adds that game's score, counts the game and (maybe) a win. */
    public void addGameResult(int gameScore, boolean won) {
        totalScore += gameScore;
        gamesPlayed++;
        if (won) wins++;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
}
