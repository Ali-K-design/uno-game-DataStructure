package network;

import controller.AppActions;
import controller.Scheduler;
import controller.UnoController;
import dataStructures.DynamicArray;
import game.AccountManager;
import game.Leaderboard;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class UnoServer {
    private static final int MAX_CLIENTS = 200;

    final Object lock = new Object();
    private final Leaderboard leaderboard = new Leaderboard();
    private final AccountManager accounts = new AccountManager(leaderboard);
    private final long botDelayMillis;
    private final ServerSocket serverSocket;
    private final Scheduler scheduler;
    private final UnoController factory;

    private DynamicArray<Room> rooms = new DynamicArray<>();
    private DynamicArray<ClientHandler> clients = new DynamicArray<>();
    private DynamicArray<String> loggedIn = new DynamicArray<>();
    private int nextRoomId = 1;
    private volatile boolean running;
    private boolean cheatsAllowed = true;
    private boolean verbose;

    public UnoServer(int port, long botDelayMillis) throws IOException {
        this.botDelayMillis = botDelayMillis;
        this.serverSocket = new ServerSocket(port);
        this.scheduler = (delay, task) -> {
            Thread t = new Thread(() -> {
                try {
                    if (delay > 0) Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
                synchronized (lock) {
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        log("bot task failed: " + e);
                    }
                }
            }, "uno-bot");
            t.setDaemon(true);
            t.start();
        };
        this.factory = new UnoController(leaderboard, accounts, scheduler, botDelayMillis);
    }

    public int getPort() { return serverSocket.getLocalPort(); }
    public void setCheatsAllowed(boolean allowed) { synchronized (lock) { cheatsAllowed = allowed; } }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    public void start() {
        running = true;
        Thread accept = new Thread(this::acceptLoop, "uno-accept");
        accept.setDaemon(true);
        accept.start();
        log("listening on port " + getPort());
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            System.out.println("IOException dropped");
        }
        synchronized (lock) {
            for (int i = 0; i < clients.getSize(); i++) clients.get(i).closeSocket();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                synchronized (lock) {
                    if (clients.getSize() >= MAX_CLIENTS) {
                        socket.close();
                        continue;
                    }
                    ClientHandler handler = new ClientHandler(this, socket);
                    clients.add(handler);
                    handler.start();
                }
            } catch (IOException e) {
                if (!running) return;
                log("accept failed: " + e);
            }
        }
    }

    UnoController newController() { return new UnoController(leaderboard, accounts, scheduler, botDelayMillis); }
    UnoController factory() { return factory; }
    Scheduler scheduler() { return scheduler; }
    long botDelayMillis() { return botDelayMillis; }
    boolean cheatsAllowed() { return cheatsAllowed; }

    void log(String message) {
        if (verbose) System.err.println("[uno-server] " + message);
    }

    boolean isLoggedIn(String username) {
        for (int i = 0; i < loggedIn.getSize(); i++) if (loggedIn.get(i).equals(username)) return true;
        return false;
    }

    void markLoggedIn(String username) { loggedIn.add(username); }

    void markLoggedOut(String username) {
        DynamicArray<String> kept = new DynamicArray<>();
        for (int i = 0; i < loggedIn.getSize(); i++) if (!loggedIn.get(i).equals(username)) kept.add(loggedIn.get(i));
        loggedIn = kept;
    }

    void removeClient(ClientHandler handler) {
        DynamicArray<ClientHandler> kept = new DynamicArray<>();
        for (int i = 0; i < clients.getSize(); i++) if (clients.get(i) != handler) kept.add(clients.get(i));
        clients = kept;
    }

    Room createRoom(ClientHandler host, int humans, int bots, String mode, int limit, String variant) {
        if (humans < 2 || humans > 10) throw new IllegalArgumentException("An online game needs 2 to 10 human players.");
        if (bots < 0 || humans + bots > 10) throw new IllegalArgumentException("Total players must be at most 10.");
        if (!"score".equals(mode) && !"rounds".equals(mode)) throw new IllegalArgumentException("Mode must be 'score' or 'rounds'.");
        if (limit < 1) throw new IllegalArgumentException("The limit must be a positive number.");
        if (variant == null || !("classic".equals(variant) || "nomercy".equals(variant))) {
            throw new IllegalArgumentException("Variant must be 'classic' or 'nomercy'.");
        }
        Room room = new Room(this, nextRoomId++, host, humans, bots, mode, limit, variant);
        rooms.add(room);
        log("room " + room.id() + " created by " + host.getAccount().getUsername());
        return room;
    }

    Room findRoom(int id) {
        for (int i = 0; i < rooms.getSize(); i++) if (rooms.get(i).id() == id) return rooms.get(i);
        return null;
    }

    void removeRoom(Room room) {
        DynamicArray<Room> kept = new DynamicArray<>();
        for (int i = 0; i < rooms.getSize(); i++) if (rooms.get(i) != room) kept.add(rooms.get(i));
        rooms = kept;
        log("room " + room.id() + " closed");
    }

    AppActions.RoomInfo[] waitingRooms() {
        int count = 0;
        for (int i = 0; i < rooms.getSize(); i++) if (!rooms.get(i).isStarted()) count++;
        AppActions.RoomInfo[] out = new AppActions.RoomInfo[count];
        int k = 0;
        for (int i = 0; i < rooms.getSize(); i++) if (!rooms.get(i).isStarted()) out[k++] = rooms.get(i).info();
        return out;
    }
}
