package game;

import model.Account;

/** Registration and authentication rules (spec 3.7). */
public class AccountManager {
    private final Leaderboard leaderboard;

    public AccountManager(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    /** Returns null on success, otherwise the reason registration failed. */
    public String register(String username, String password, String displayName) {
        if (username == null || username.isEmpty() || username.chars().anyMatch(Character::isWhitespace)) {
            return "Username must not be empty or contain spaces.";
        }
        if (password == null || password.length() < 4) return "Password must be at least 4 characters.";
        if (displayName == null || displayName.isBlank()) return "Display name must not be empty.";
        if (leaderboard.findByUsername(username) != null) return "That username is already taken.";
        leaderboard.addAccount(new Account(username, password, displayName.trim()));
        return null;
    }

    /** Null when the credentials are wrong. */
    public Account authenticate(String username, String password) {
        Account account = leaderboard.findByUsername(username);
        if (account == null || !account.getPassword().equals(password)) return null;
        return account;
    }

    public Account find(String username) { return leaderboard.findByUsername(username); }
}
