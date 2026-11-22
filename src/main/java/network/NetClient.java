package network;

import controller.TableSnapshot;
import dataStructures.DynamicArray;
import view.GameView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class NetClient implements Closeable {
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final long REQUEST_TIMEOUT_MS = 8000;

    private static final class Pending {
        final int id;
        JsonObject response;
        boolean done;
        Pending(int id) { this.id = id; }
    }

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final Consumer<Runnable> dispatcher;
    private final Object writeLock = new Object();
    private final Object pendingLock = new Object();
    private DynamicArray<Pending> pending = new DynamicArray<>();
    private int nextId = 1;
    private volatile GameView view;
    private volatile Runnable onDisconnect;
    private volatile boolean closed;

    private NetClient(Socket socket, Consumer<Runnable> dispatcher) throws IOException {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public static NetClient connect(String host, int port, Consumer<Runnable> dispatcher) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            NetClient client = new NetClient(socket, dispatcher);
            String hello = client.in.readLine();
            JsonObject message;
            try {
                message = Json.parseObject(hello);
            } catch (JsonException e) {
                throw new IOException("That server does not speak the UNO protocol.");
            }
            if (!"hello".equals(message.getString("push")) || message.getInt("protocol", -1) != Protocol.VERSION) {
                throw new IOException("Incompatible server (protocol version).");
            }
            socket.setSoTimeout(0);
            Thread reader = new Thread(client::readLoop, "uno-net-reader");
            reader.setDaemon(true);
            reader.start();
            return client;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    public void setView(GameView view) { this.view = view; }
    public GameView getView() { return view; }
    public void setOnDisconnect(Runnable onDisconnect) { this.onDisconnect = onDisconnect; }
    public boolean isClosed() { return closed; }

    public JsonObject request(JsonObject request) throws IOException {
        Pending p;
        synchronized (pendingLock) {
            if (closed) throw new IOException("Not connected.");
            p = new Pending(nextId++);
            pending.add(p);
        }
        request.put("id", p.id);
        try {
            write(request);
        } catch (IOException e) {
            forget(p);
            throw e;
        }
        long deadline = System.currentTimeMillis() + REQUEST_TIMEOUT_MS;
        synchronized (p) {
            while (!p.done) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    forget(p);
                    throw new IOException("The server did not answer in time.");
                }
                try {
                    p.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted.");
                }
            }
        }
        if (p.response == null) throw new IOException("Connection lost.");
        return p.response;
    }

    public void send(JsonObject message) {
        try {
            write(message);
        } catch (IOException e) {
            closeQuietly();
        }
    }

    private void write(JsonObject message) throws IOException {
        synchronized (writeLock) {
            if (closed) throw new IOException("Not connected.");
            out.write(Json.stringify(message));
            out.write('\n');
            out.flush();
        }
    }

    private void forget(Pending p) {
        synchronized (pendingLock) {
            DynamicArray<Pending> kept = new DynamicArray<>();
            for (int i = 0; i < pending.getSize(); i++) if (pending.get(i) != p) kept.add(pending.get(i));
            pending = kept;
        }
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonObject message;
                try {
                    message = Json.parseObject(line);
                } catch (JsonException e) {
                    continue;
                }
                if (message.has("push")) handlePush(message);
                else complete(message);
            }
        } catch (IOException e) {
            System.out.println("IOException dropped");
        } finally {
            connectionLost();
        }
    }

    private void complete(JsonObject response) {
        int id = response.getInt("id", -1);
        Pending found = null;
        synchronized (pendingLock) {
            for (int i = 0; i < pending.getSize(); i++) if (pending.get(i).id == id) found = pending.get(i);
        }
        if (found == null) return;
        forget(found);
        synchronized (found) {
            found.response = response;
            found.done = true;
            found.notifyAll();
        }
    }

    private void handlePush(JsonObject message) {
        GameView v = view;
        String type = message.getString("push");
        if (v == null || type == null) return;
        switch (type) {
            case "snapshot": {
                JsonObject state = message.getObject("state");
                try {
                    TableSnapshot snapshot = Protocol.decodeSnapshot(state == null ? new JsonObject() : state);
                    dispatcher.accept(() -> v.render(snapshot));
                } catch (JsonException e) {
                    dispatcher.accept(() -> v.toast("The server sent unreadable game data."));
                }
                break;
            }
            case "toast": {
                String text = message.getString("text");
                if (text != null) dispatcher.accept(() -> v.toast(text));
                break;
            }
            case "lobby": {
                JsonArray names = message.getArray("names");
                if (names == null) return;
                try {
                    String[] list = names.toStringArray();
                    String roomId = message.getString("roomId");
                    int needed = message.getInt("needed", list.length);
                    dispatcher.accept(() -> v.lobby(roomId, list, needed));
                } catch (JsonException e) {
                    System.out.println("IOException dropped");
                }
                break;
            }
            default:
                break;
        }
    }

    private void connectionLost() {
        boolean first;
        synchronized (pendingLock) {
            first = !closed;
            closed = true;
            for (int i = 0; i < pending.getSize(); i++) {
                Pending p = pending.get(i);
                synchronized (p) {
                    p.done = true;
                    p.notifyAll();
                }
            }
            pending = new DynamicArray<>();
        }
        if (!first) return;
        GameView v = view;
        Runnable cb = onDisconnect;
        dispatcher.accept(() -> {
            if (v != null) v.toast("Connection to the server was lost.");
            if (cb != null) cb.run();
        });
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
            System.out.println("IOException dropped");
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        socket.close();
    }
}
