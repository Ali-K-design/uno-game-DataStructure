package network;

import controller.AppActions;
import controller.GameActions;
import view.GameView;

import java.io.IOException;
import java.util.function.Consumer;

public final class RemoteApp implements AppActions {
    private final NetClient net;
    private String displayName = "Guest";

    public RemoteApp(NetClient net) { this.net = net; }

    public static RemoteApp connect(String host, int port, Consumer<Runnable> dispatcher) throws IOException {
        return new RemoteApp(NetClient.connect(host, port, dispatcher));
    }

    public NetClient getClient() { return net; }

    private String call(JsonObject request, JsonObject[] responseOut) {
        try {
            JsonObject r = net.request(request);
            if (responseOut != null) responseOut[0] = r;
            if (r.getBool("ok", false)) return null;
            String error = r.getString("error");
            return error == null ? "Request failed." : error;
        } catch (IOException e) {
            return "Server connection problem: " + e.getMessage();
        }
    }

    @Override
    public String register(String username, String password, String displayName) {
        return call(new JsonObject().put("cmd", "register").put("username", username).put("password", password)
                .put("displayName", displayName), null);
    }

    @Override
    public String login(String username, String password) {
        JsonObject[] response = new JsonObject[1];
        String error = call(new JsonObject().put("cmd", "login").put("username", username).put("password", password), response);
        if (error == null && response[0] != null && response[0].getString("displayName") != null) {
            displayName = response[0].getString("displayName");
        }
        return error;
    }

    @Override
    public void logout() {
        call(new JsonObject().put("cmd", "logout"), null);
        displayName = "Guest";
    }

    @Override public String currentDisplayName() { return displayName; }

    @Override
    public LeaderboardRow[] leaderboard() {
        JsonObject[] response = new JsonObject[1];
        if (call(new JsonObject().put("cmd", "leaderboard"), response) != null) return new LeaderboardRow[0];
        JsonArray rows = response[0].getArray("rows");
        if (rows == null) return new LeaderboardRow[0];
        LeaderboardRow[] out = new LeaderboardRow[rows.size()];
        for (int i = 0; i < out.length; i++) out[i] = Protocol.decodeRow(rows.getObject(i));
        return out;
    }

    @Override public boolean isOnline() { return true; }

    @Override
    public GameActions startGame(int humans, int bots, String mode, int limit, String[] otherHumanUsernames, String variant, GameView view) {
        throw new IllegalArgumentException("Online: host a room or join one (other players connect themselves).");
    }

    @Override
    public GameActions hostRoom(int humans, int bots, String mode, int limit, String variant, GameView view) {
        GameView previous = net.getView();
        net.setView(view);
        String error = call(new JsonObject().put("cmd", "host_room").put("humans", humans).put("bots", bots)
                .put("mode", mode).put("limit", limit).put("variant", variant), null);
        if (error != null) {
            net.setView(previous);
            throw new IllegalArgumentException(error);
        }
        return new RemoteGame(net);
    }

    @Override
    public RoomInfo[] listRooms() {
        JsonObject[] response = new JsonObject[1];
        String error = call(new JsonObject().put("cmd", "list_rooms"), response);
        if (error != null) throw new IllegalArgumentException(error);
        JsonArray rooms = response[0].getArray("rooms");
        if (rooms == null) return new RoomInfo[0];
        RoomInfo[] out = new RoomInfo[rooms.size()];
        for (int i = 0; i < out.length; i++) out[i] = Protocol.decodeRoom(rooms.getObject(i));
        return out;
    }

    @Override
    public GameActions joinRoom(int roomId, GameView view) {
        GameView previous = net.getView();
        net.setView(view);
        String error = call(new JsonObject().put("cmd", "join_room").put("roomId", roomId), null);
        if (error != null) {
            net.setView(previous);
            throw new IllegalArgumentException(error);
        }
        return new RemoteGame(net);
    }
}
