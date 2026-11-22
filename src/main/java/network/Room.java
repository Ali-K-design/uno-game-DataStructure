package network;

import controller.AppActions;
import controller.GameSession;
import model.Account;

final class Room {
    private final UnoServer server;
    private final int id;
    private final int humans;
    private final int bots;
    private final String mode;
    private final int limit;
    private final String variant;
    private final String hostName;
    private final ClientHandler[] members;
    private int joined;
    private boolean started;
    private GameSession session;
    private BroadcastView view;

    Room(UnoServer server, int id, ClientHandler host, int humans, int bots, String mode, int limit, String variant) {
        this.server = server;
        this.id = id;
        this.humans = humans;
        this.bots = bots;
        this.mode = mode;
        this.limit = limit;
        this.variant = variant;
        this.hostName = host.getAccount().getDisplayName();
        this.members = new ClientHandler[humans];
    }

    int id() { return id; }
    boolean isStarted() { return started; }
    GameSession session() { return session; }
    int slots() { return members.length; }
    ClientHandler member(int i) { return members[i]; }

    AppActions.RoomInfo info() {
        return new AppActions.RoomInfo(id, hostName, humans, joined, bots, mode, limit, variant);
    }

    String join(ClientHandler client) {
        if (started) return "That game has already started.";
        if (joined >= humans) return "That room is full.";
        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                members[i] = client;
                break;
            }
        }
        joined++;
        if (joined == humans) start();
        else pushLobby();
        return null;
    }

    private void start() {
        try {
            Account[] accounts = new Account[humans];
            for (int i = 0; i < humans; i++) accounts[i] = members[i].getAccount();
            view = new BroadcastView(this);
            session = server.factory().createSession(accounts, bots, mode, limit, variant, view,
                    server.scheduler(), server.botDelayMillis(), false);
            for (int i = 0; i < humans; i++) members[i].setPlayer(session.getPlayer(i));
            started = true;
            session.begin();
        } catch (IllegalArgumentException e) {
            for (int i = 0; i < humans; i++) {
                if (members[i] != null) members[i].send(Protocol.toast("Could not start the game: " + e.getMessage()));
            }
            server.removeRoom(this);
        }
    }

    private void pushLobby() {
        String[] names = new String[joined];
        int k = 0;
        for (int i = 0; i < members.length; i++) {
            if (members[i] != null) names[k++] = members[i].getAccount().getDisplayName();
        }
        JsonArray array = JsonArray.ofStrings(names);
        for (int i = 0; i < members.length; i++) {
            if (members[i] != null) {
                members[i].send(Protocol.push("lobby").put("roomId", String.valueOf(id)).put("names", array).put("needed", humans));
            }
        }
    }

    void leave(ClientHandler client) {
        int slot = -1;
        for (int i = 0; i < members.length; i++) if (members[i] == client) slot = i;
        if (slot < 0) return;
        members[slot] = null;
        joined--;
        if (!started) {
            if (joined == 0) server.removeRoom(this);
            else pushLobby();
            return;
        }
        session.autopilot(client.getPlayer());
        if (joined == 0) {
            session.quit();
            server.removeRoom(this);
        }
    }

    void perform(ClientHandler who, Runnable action) {
        view.beginAction(who);
        try {
            action.run();
        } finally {
            view.endAction();
        }
    }
}
