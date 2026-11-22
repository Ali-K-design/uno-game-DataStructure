package controller;

import dataStructures.DynamicArray;
import game.AccountManager;
import game.ClassicRules;
import game.Game;
import game.Leaderboard;
import game.NoMercyRules;
import game.RuleSet;
import game.ScoreManager;
import model.Account;
import model.BotPlayer;
import model.HumanPlayer;
import model.Player;
import view.GameView;

public class UnoController implements AppActions {
    private final Leaderboard leaderboard;
    private final AccountManager accounts;
    private final Scheduler scheduler;
    private final long botDelayMillis;
    private Account current;

    public UnoController(Scheduler scheduler, long botDelayMillis) {
        this(new Leaderboard(), scheduler, botDelayMillis);
    }

    public UnoController(Leaderboard leaderboard, Scheduler scheduler, long botDelayMillis) {
        this(leaderboard, new AccountManager(leaderboard), scheduler, botDelayMillis);
    }

    public UnoController(Leaderboard leaderboard, AccountManager accounts, Scheduler scheduler, long botDelayMillis) {
        this.leaderboard = leaderboard;
        this.accounts = accounts;
        this.scheduler = scheduler;
        this.botDelayMillis = botDelayMillis;
    }

    @Override
    public String register(String username, String password, String displayName) {
        return accounts.register(username, password, displayName);
    }

    @Override
    public String login(String username, String password) {
        Account account = accounts.authenticate(username, password);
        if (account == null) return "Wrong username or password.";
        current = account;
        return null;
    }

    @Override
    public void logout() { current = null; }

    @Override
    public String currentDisplayName() { return current == null ? "Guest" : current.getDisplayName(); }

    public Account getCurrentAccount() { return current; }

    @Override
    public LeaderboardRow[] leaderboard() {
        leaderboard.sortByScore();
        LeaderboardRow[] rows = new LeaderboardRow[leaderboard.size()];
        for (int i = 0; i < rows.length; i++) {
            Account a = leaderboard.getRankedAccount(i);
            rows[i] = new LeaderboardRow(i + 1, a.getDisplayName(), a.getUsername(), a.getTotalScore(), a.getGamesPlayed(), a.getWins());
        }
        return rows;
    }

    @Override
    public GameActions startGame(int humans, int bots, String mode, int limit, String[] otherHumans, String variant, GameView view) {
        if (current == null) throw new IllegalArgumentException("Please log in first.");
        if (humans < 1) throw new IllegalArgumentException("There must be at least one human player.");
        if (otherHumans == null || otherHumans.length != humans - 1) {
            throw new IllegalArgumentException("Provide the usernames of the other " + (humans - 1) + " human player(s).");
        }
        Account[] humanAccounts = new Account[humans];
        humanAccounts[0] = current;
        for (int i = 0; i < otherHumans.length; i++) {
            Account account = accounts.find(otherHumans[i] == null ? "" : otherHumans[i].trim());
            if (account == null) throw new IllegalArgumentException("No registered user named '" + otherHumans[i] + "'.");
            humanAccounts[i + 1] = account;
        }
        return createSession(humanAccounts, bots, mode, limit, variant, view, scheduler, botDelayMillis, true);
    }

    public GameSession createSession(Account[] humanAccounts, int bots, String mode, int limit, String variant,
                                     GameView view, Scheduler sched, long delayMillis, boolean startNow) {
        int humans = humanAccounts.length;
        if (humans < 1) throw new IllegalArgumentException("There must be at least one human player.");
        if (bots < 0 || humans + bots < 2 || humans + bots > 10) {
            throw new IllegalArgumentException("Total players must be between 2 and 10.");
        }
        if (limit < 1) throw new IllegalArgumentException("The limit must be a positive number.");
        Game.Mode gameMode;
        if ("score".equalsIgnoreCase(mode)) gameMode = Game.Mode.SCORE;
        else if ("rounds".equalsIgnoreCase(mode)) gameMode = Game.Mode.ROUNDS;
        else throw new IllegalArgumentException("Mode must be 'score' or 'rounds'.");
        RuleSet rules;
        if (variant == null || variant.isEmpty() || "classic".equalsIgnoreCase(variant)) rules = new ClassicRules();
        else if ("nomercy".equalsIgnoreCase(variant)) rules = new NoMercyRules();
        else throw new IllegalArgumentException("Variant must be 'classic' or 'nomercy'.");

        DynamicArray<Player> players = new DynamicArray<>();
        for (int i = 0; i < humans; i++) {
            Account account = humanAccounts[i];
            for (int j = 0; j < i; j++) {
                if (humanAccounts[j] == account) throw new IllegalArgumentException("'" + account.getUsername() + "' is already in this game.");
            }
            players.add(new HumanPlayer(account.getDisplayName(), account));
        }
        for (int i = 1; i <= bots; i++) {
            String name = "Bot_" + i;
            players.add(new BotPlayer(name, new Account(name, "", name)));
        }
        Game game = new Game(players, new ScoreManager(leaderboard), gameMode, limit, rules);
        return new GameSession(game, view, sched, delayMillis, startNow);
    }
}
