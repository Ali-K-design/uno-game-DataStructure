package network;

import controller.AppActions;
import controller.GameSession;
import controller.UnoController;
import model.Account;
import model.Color;
import model.Player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ClientHandler extends Thread {
    private final UnoServer server;
    private final Socket socket;
    private final BufferedReader in;
    private final OutboundQueue out = new OutboundQueue();
    private final UnoController app;

    private Room room;
    private Player player;
    private String loggedUser;
    private volatile boolean closed;
    private boolean disconnected;
    private int badMessages;

    ClientHandler(UnoServer server, Socket socket) throws IOException {
        super("uno-client-" + socket.getRemoteSocketAddress());
        setDaemon(true);
        this.server = server;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.app = server.newController();
    }

    Account getAccount() { return app.getCurrentAccount(); }
    Player getPlayer() { return player; }
    void setPlayer(Player player) { this.player = player; }

    @Override
    public void run() {
        Thread writer = new Thread(this::writeLoop, getName() + "-writer");
        writer.setDaemon(true);
        writer.start();
        send(Protocol.push("hello").put("protocol", Protocol.VERSION).put("server", "UNO"));
        try {
            String line;
            while (!closed && (line = readLine()) != null) handleLine(line);
        } catch (IOException e) {
            System.out.println("IO Exception dropped");
        } finally {
            disconnect();
        }
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return sb.toString();
            if (c != '\r') sb.append((char) c);
            if (sb.length() > Protocol.MAX_LINE_CHARS) throw new IOException("Message too long");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void writeLoop() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = out.take()) != null) {
                writer.write(line);
                writer.write('\n');
                writer.flush();
            }
        } catch (IOException | InterruptedException e) {
            closeSocket();
        }
    }

    void send(JsonObject message) {
        if (!out.put(Json.stringify(message))) closeSocket();
    }

    void closeSocket() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            System.out.println("IOException dropped");
        }
    }

    private void disconnect() {
        synchronized (server.lock) {
            if (disconnected) return;
            disconnected = true;
            closed = true;
            leaveRoom();
            if (loggedUser != null) server.markLoggedOut(loggedUser);
            loggedUser = null;
            server.removeClient(this);
        }
        out.close();
        closeSocket();
    }

    private void handleLine(String line) {
        if (line.isBlank()) return;
        JsonObject request;
        try {
            request = Json.parseObject(line);
        } catch (JsonException e) {
            if (++badMessages > 5) closeSocket();
            reply(null, false, "Malformed message: " + e.getMessage(), null);
            return;
        }
        badMessages = 0;
        Object id = request.get("id");
        synchronized (server.lock) {
            try {
                dispatch(request.getString("cmd"), request, id);
            } catch (IllegalArgumentException e) {
                reply(id, false, e.getMessage(), null);
            } catch (RuntimeException e) {
                server.log("handler error: " + e);
                reply(id, false, "Internal server error.", null);
            }
        }
    }

    private void reply(Object id, boolean ok, String error, JsonObject extra) {
        JsonObject o = new JsonObject();
        if (id != null) o.put("id", id);
        o.put("ok", ok);
        if (error != null) o.put("error", error);
        if (extra != null) {
            for (int i = 0; i < extra.size(); i++) o.put(extra.keyAt(i), extra.valueAt(i));
        }
        send(o);
    }

    private void requireLogin() {
        if (app.getCurrentAccount() == null) throw new IllegalArgumentException("Please log in first.");
    }

    private void dispatch(String cmd, JsonObject req, Object id) {
        if (cmd == null) throw new IllegalArgumentException("Missing 'cmd'.");
        switch (cmd) {
            case "ping":
                reply(id, true, null, null);
                return;
            case "register": {
                String err = app.register(req.getString("username"), req.getString("password"), req.getString("displayName"));
                reply(id, err == null, err, null);
                return;
            }
            case "login": {
                if (loggedUser != null) throw new IllegalArgumentException("You are already logged in.");
                String user = req.getString("username");
                if (user != null && server.isLoggedIn(user)) throw new IllegalArgumentException("That account is already logged in elsewhere.");
                String err = app.login(user, req.getString("password"));
                if (err != null) {
                    reply(id, false, err, null);
                    return;
                }
                loggedUser = user;
                server.markLoggedIn(user);
                reply(id, true, null, new JsonObject().put("displayName", app.currentDisplayName()));
                return;
            }
            case "logout":
                leaveRoom();
                if (loggedUser != null) server.markLoggedOut(loggedUser);
                loggedUser = null;
                app.logout();
                reply(id, true, null, null);
                return;
            case "leaderboard": {
                JsonArray rows = new JsonArray();
                for (AppActions.LeaderboardRow r : app.leaderboard()) rows.add(Protocol.encodeRow(r));
                reply(id, true, null, new JsonObject().put("rows", rows));
                return;
            }
            case "list_rooms": {
                requireLogin();
                JsonArray rooms = new JsonArray();
                for (AppActions.RoomInfo r : server.waitingRooms()) rooms.add(Protocol.encodeRoom(r));
                reply(id, true, null, new JsonObject().put("rooms", rooms));
                return;
            }
            case "host_room": {
                requireLogin();
                if (room != null) throw new IllegalArgumentException("You are already in a room.");
                Room created = server.createRoom(this, req.getInt("humans", 0), req.getInt("bots", 0), req.getString("mode"),
                        req.getInt("limit", 0), req.getString("variant"));
                room = created;
                reply(id, true, null, new JsonObject().put("roomId", created.id()));
                created.join(this);
                return;
            }
            case "join_room": {
                requireLogin();
                if (room != null) throw new IllegalArgumentException("You are already in a room.");
                Room target = server.findRoom(req.getInt("roomId", -1));
                if (target == null) throw new IllegalArgumentException("No such room.");
                String err = target.join(this);
                if (err != null) throw new IllegalArgumentException(err);
                room = target;
                reply(id, true, null, new JsonObject().put("roomId", target.id()));
                return;
            }
            case "leave_room":
            case "quit":
                leaveRoom();
                if (id != null) reply(id, true, null, null);
                return;
            default:
                inGame(cmd, req, id);
        }
    }

    private void inGame(String cmd, JsonObject req, Object id) {
        Room r = room;
        if (r == null || !r.isStarted()) {
            if (!isGameCommand(cmd)) throw new IllegalArgumentException("Unknown command '" + cmd + "'.");
            send(Protocol.toast("You are not in a running game."));
            return;
        }
        GameSession session = r.session();
        switch (cmd) {
            case "next_round":
                r.perform(this, session::nextRound);
                return;
            case "scoreboard":
                reply(id, true, null, new JsonObject().put("text", session.scoreboardText()));
                return;
            case "play": case "draw": case "draw_machine": case "play_drawn": case "pass": case "cheat":
                break;
            default:
                throw new IllegalArgumentException("Unknown command '" + cmd + "'.");
        }
        if (session.getCurrentPlayer() != player) {
            send(Protocol.toast("It is not your turn."));
            return;
        }
        Color color = parseColor(req.getString("color"));
        switch (cmd) {
            case "play": {
                int index = req.getInt("index", -1), target = req.getInt("target", -1);
                r.perform(this, () -> session.play(index, color, target));
                return;
            }
            case "draw": r.perform(this, session::draw); return;
            case "draw_machine": r.perform(this, session::drawMachine); return;
            case "play_drawn": r.perform(this, () -> session.playDrawn(color)); return;
            case "pass": r.perform(this, session::pass); return;
            default: {                                              // cheat
                if (!server.cheatsAllowed()) {
                    send(Protocol.toast("Cheats are disabled on this server."));
                    return;
                }
                String text = req.getString("text");
                r.perform(this, () -> session.cheat(text));
            }
        }
    }

    private static boolean isGameCommand(String cmd) {
        switch (cmd) {
            case "play": case "draw": case "draw_machine": case "play_drawn": case "pass":
            case "cheat": case "next_round": case "scoreboard":
                return true;
            default:
                return false;
        }
    }

    private static Color parseColor(String text) {
        if (text == null) return null;
        try {
            return Color.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void leaveRoom() {
        if (room == null) return;
        Room r = room;
        room = null;
        r.leave(this);
        player = null;
    }
}
