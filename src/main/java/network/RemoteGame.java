package network;

import controller.GameActions;
import model.Color;

import java.io.IOException;

public final class RemoteGame implements GameActions {
    private final NetClient net;

    RemoteGame(NetClient net) { this.net = net; }

    private static JsonObject cmd(String name) { return new JsonObject().put("cmd", name); }

    @Override public void play(int cardIndex, Color chosenColor) { play(cardIndex, chosenColor, -1); }

    @Override
    public void play(int cardIndex, Color chosenColor, int targetSeat) {
        net.send(cmd("play").put("index", cardIndex).put("color", chosenColor == null ? null : chosenColor.name()).put("target", targetSeat));
    }

    @Override public void draw() { net.send(cmd("draw")); }
    @Override public void drawMachine() { net.send(cmd("draw_machine")); }
    @Override public void playDrawn(Color chosenColor) { net.send(cmd("play_drawn").put("color", chosenColor == null ? null : chosenColor.name())); }
    @Override public void pass() { net.send(cmd("pass")); }
    @Override public void nextRound() { net.send(cmd("next_round")); }
    @Override public void cheat(String rawCommand) { net.send(cmd("cheat").put("text", rawCommand)); }

    @Override
    public void quit() {
        net.send(cmd("quit"));
        net.setView(null);
    }

    @Override
    public String scoreboardText() {
        try {
            JsonObject r = net.request(cmd("scoreboard"));
            return r.getBool("ok", false) ? r.getString("text") : String.valueOf(r.getString("error"));
        } catch (IOException e) {
            return "Server connection problem: " + e.getMessage();
        }
    }
}
