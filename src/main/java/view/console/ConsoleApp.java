package view.console;

import controller.AppActions;
import controller.GameActions;
import model.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

public class ConsoleApp {
    private final AppActions app;
    private final BufferedReader in;
    private final PrintStream out;
    private final ConsoleView view;
    private GameActions game;
    private boolean loggedIn;
    private boolean running = true;

    public ConsoleApp(AppActions app, BufferedReader in, PrintStream out) {
        this.app = app;
        this.in = in;
        this.out = out;
        this.view = new ConsoleView(out);
        this.view.setOnline(app.isOnline());
    }

    public ConsoleView getView() { return view; }

    public void run() throws IOException {
        out.println("Welcome to UNO.  Commands: register -u U -p P -n NAME | login -u U -p P | exit");
        while (running) {
            out.print(game != null ? "Enter Command > " : (loggedIn ? "uno (menu) > " : "uno (login) > "));
            out.flush();
            String line = in.readLine();
            if (line == null) return;
            line = line.trim();
            if (line.isEmpty()) continue;
            if (game != null) handleGame(line);
            else if (loggedIn) handleMenu(line);
            else handleAuth(line);
        }
    }

    private void handleAuth(String line) {
        String[] t = line.split("\\s+");
        switch (t[0].toLowerCase()) {
            case "register": {
                String err = app.register(flag(t, "-u"), flag(t, "-p"), rest(t, "-n"));
                out.println(err == null ? "Account created. You can log in now." : "Error: " + err);
                break;
            }
            case "login": {
                String err = app.login(flag(t, "-u"), flag(t, "-p"));
                if (err == null) { loggedIn = true; out.println("Welcome, " + app.currentDisplayName() + "!  Commands: leaderboard | rooms | host ... | join <id> | start_game -p H -b B -mode score|rounds -limit N [-variant nomercy] | logout"); }
                else out.println("Error: " + err);
                break;
            }
            case "exit": running = false; break;
            default: out.println("Unknown command. Available here: register, login, exit.");
        }
    }

    private void handleMenu(String line) throws IOException {
        String[] t = line.split("\\s+");
        switch (t[0].toLowerCase()) {
            case "leaderboard": printLeaderboard(); break;
            case "start_game": startGame(t); break;
            case "rooms": if (app.isOnline()) printRooms(); else out.println("Not connected to a server."); break;
            case "host": if (app.isOnline()) hostRoom(t); else out.println("Not connected to a server."); break;
            case "join": if (app.isOnline()) joinRoom(t); else out.println("Not connected to a server."); break;
            case "logout": case "exit": app.logout(); loggedIn = false; break;
            default: out.println("Unknown command. Available here: leaderboard, start_game, logout.");
        }
    }

    private void printLeaderboard() {
        out.printf("%-5s %-16s %-14s %7s %6s %5s%n", "Rank", "Name", "Username", "Score", "Games", "Wins");
        for (AppActions.LeaderboardRow r : app.leaderboard()) {
            out.printf("%-5d %-16s %-14s %7d %6d %5d%n", r.rank(), r.displayName(), r.username(), r.totalScore(), r.games(), r.wins());
        }
    }

    private void printRooms() {
        AppActions.RoomInfo[] rooms;
        try {
            rooms = app.listRooms();
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return;
        }
        if (rooms.length == 0) out.println("No open rooms. Create one with: host -p <humans> -b <bots> -mode score|rounds -limit N");
        for (AppActions.RoomInfo r : rooms) {
            out.printf("Room %d | host %s | humans %d/%d | bots %d | %s %d | %s%n", r.id(), r.host(), r.joined(), r.humans(),
                    r.bots(), r.mode(), r.limit(), r.variant());
        }
    }

    private void hostRoom(String[] t) {
        try {
            String variant = flag(t, "-variant");
            game = app.hostRoom(Integer.parseInt(flag(t, "-p")), Integer.parseInt(flag(t, "-b")), String.valueOf(flag(t, "-mode")),
                    Integer.parseInt(flag(t, "-limit")), variant == null ? "classic" : variant, view);
            out.println("Room created. Waiting for the other players...");
        } catch (NumberFormatException e) {
            out.println("Usage: host -p <humans incl. you> -b <bots> -mode <score|rounds> -limit <N> [-variant classic|nomercy]");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void joinRoom(String[] t) {
        try {
            game = app.joinRoom(Integer.parseInt(t[1]), view);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            out.println("Usage: join <room_id>   (see: rooms)");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void startGame(String[] t) throws IOException {
        int humans, bots, limit;
        try {
            humans = Integer.parseInt(flag(t, "-p"));
            bots = Integer.parseInt(flag(t, "-b"));
            limit = Integer.parseInt(flag(t, "-limit"));
        } catch (NumberFormatException e) {
            out.println("Usage: start_game -p <humans> -b <bots> -mode <score|rounds> -limit <N> [-variant classic|nomercy]");
            return;
        }
        String[] others = new String[Math.max(0, humans - 1)];
        for (int i = 0; i < others.length; i++) {
            out.print("Enter username of human player " + (i + 2) + ": ");
            out.flush();
            String name = in.readLine();
            others[i] = name == null ? "" : name.trim();
        }
        try {
            String variant = flag(t, "-variant");
            game = app.startGame(humans, bots, String.valueOf(flag(t, "-mode")), limit, others, variant == null ? "classic" : variant, view);
            afterGameCommand();
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void handleGame(String line) throws IOException {
        String[] t = line.split("\\s+");
        switch (t[0].toLowerCase()) {
            case "play": {
                try {
                    int target = -1;
                    String targetText = flag(t, "-t");
                    if (targetText != null) {
                        target = view.seatAt(Integer.parseInt(targetText));
                        if (target < 1) { out.println("  >> -t must be the number of ANOTHER player from the Players line."); break; }
                    }
                    game.play(Integer.parseInt(t[1]), colorOrNull(flag(t, "-c")), target);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    out.println("Usage: play <card_index> [-t <player_index>] [-c RED|YELLOW|GREEN|BLUE]");
                }
                break;
            }
            case "draw": game.draw(); break;
            case "draw_machine": game.drawMachine(); break;
            case "play_drawn": game.playDrawn(colorOrNull(flag(t, "-c"))); break;
            case "pass": game.pass(); break;
            case "scores": out.print(game.scoreboardText()); break;
            case "next": game.nextRound(); break;
            case "quit_game": game.quit(); game = null; return;
            case "cheat_give": case "cheat_top": case "cheat_empty_deck": case "cheat_win_round": game.cheat(line); break;
            default: out.println("Unknown command. Use: play, draw, draw_machine, play_drawn, pass, scores, next, quit_game.");
        }
        if (!app.isOnline()) afterGameCommand();
    }

    private void afterGameCommand() throws IOException {
        while (game != null && view.takeSummaryPending()) {
            out.print(game.scoreboardText());
            if (view.isGameOver()) { game.quit(); game = null; return; }
            out.print("Press Enter for the next round (or type quit_game) > ");
            out.flush();
            String answer = in.readLine();
            if (answer == null || answer.trim().equalsIgnoreCase("quit_game")) { game.quit(); game = null; return; }
            game.nextRound();
        }
    }

    private static String flag(String[] t, String name) {
        for (int i = 1; i < t.length - 1; i++) if (t[i].equalsIgnoreCase(name)) return t[i + 1];
        return null;
    }


    private static String rest(String[] t, String name) {
        for (int i = 1; i < t.length - 1; i++) {
            if (t[i].equalsIgnoreCase(name)) {
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < t.length && !t[j].startsWith("-"); j++) sb.append(j > i + 1 ? " " : "").append(t[j]);
                return sb.toString();
            }
        }
        return null;
    }

    private Color colorOrNull(String text) {
        if (text == null) return null;
        try { return Color.valueOf(text.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }
}
