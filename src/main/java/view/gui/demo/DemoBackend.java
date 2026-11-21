package view.gui.demo;

import controller.AppActions;
import controller.GameActions;
import view.GameView;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DemoBackend implements AppActions {
    private final Map<String, String[]> users = new LinkedHashMap<>();   // username -> {password, displayName}
    private String current;

    public DemoBackend() {
        users.put("demo", new String[]{"1234", "Demo Player"});
        users.put("sara", new String[]{"1234", "Sara"});
        users.put("ali", new String[]{"1234", "Ali"});
    }

    @Override public String register(String u, String p, String n) {
        if (u == null || u.isBlank() || u.contains(" ")) return "Username must be non-empty and contain no spaces.";
        if (p == null || p.length() < 4) return "Password must be at least 4 characters.";
        if (users.containsKey(u)) return "That username is already taken.";
        users.put(u, new String[]{p, n == null || n.isBlank() ? u : n});
        current = u;
        return null;
    }

    @Override public String login(String u, String p) {
        String[] rec = users.get(u);
        if (rec == null || !rec[0].equals(p)) return "Wrong username or password.";
        current = u;
        return null;
    }

    @Override public void logout() { current = null; }
    @Override public String currentDisplayName() { return current == null ? "Guest" : users.get(current)[1]; }

    @Override public LeaderboardRow[] leaderboard() {
        LeaderboardRow[] rows = new LeaderboardRow[users.size()];
        int i = 0;
        for (Map.Entry<String, String[]> e : users.entrySet()) {
            rows[i] = new LeaderboardRow(i + 1, e.getValue()[1], e.getKey(), 1200 - i * 310, 12 - i * 3, 7 - i * 2);
            i++;
        }
        return rows;
    }

    @Override public GameActions startGame(int humans, int bots, String mode, int limit, String[] others, String variant, GameView view) {
        if (humans + bots < 2 || humans + bots > 10) throw new IllegalArgumentException("Total players must be between 2 and 10.");
        String win = mode.equals("score") ? "Win Condition: " + limit + " Pts" : "Win Condition: " + limit + " rounds";
        return new DemoGame(currentDisplayName(), humans + bots, humans, win, view);
    }
}
