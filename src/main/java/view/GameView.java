package view;

import controller.TableSnapshot;

public interface GameView {
    void render(TableSnapshot snapshot);
    void toast(String message);

    default void lobby(String roomId, String[] names, int needed) {
        toast("Waiting for players (" + names.length + "/" + needed + ") in room " + roomId);
    }
}
