package network;

import controller.TableSnapshot;
import view.GameView;

final class BroadcastView implements GameView {
    private final Room room;
    private ClientHandler actor;
    private boolean rendered;

    BroadcastView(Room room) { this.room = room; }

    void beginAction(ClientHandler who) {
        actor = who;
        rendered = false;
    }

    void endAction() { actor = null; }

    @Override
    public void render(TableSnapshot ignored) {
        rendered = true;
        for (int i = 0; i < room.slots(); i++) {
            ClientHandler client = room.member(i);
            if (client == null) continue;
            TableSnapshot own = room.session().snapshotFor(room.session().getPlayer(i));
            client.send(Protocol.push("snapshot").put("state", Protocol.encodeSnapshot(own)));
        }
    }

    @Override
    public void toast(String message) {
        if (actor != null && !rendered) {
            actor.send(Protocol.toast(message));
            return;
        }
        for (int i = 0; i < room.slots(); i++) {
            ClientHandler client = room.member(i);
            if (client != null) client.send(Protocol.toast(message));
        }
    }
}
