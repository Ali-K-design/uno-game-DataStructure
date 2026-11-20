package view;

import controller.TableSnapshot;

/** Implemented by ConsoleView (CLI) and TableScreen (GUI). Always call on the JavaFX thread for the GUI. */
public interface GameView {
    void render(TableSnapshot snapshot);
    void toast(String message);
}
