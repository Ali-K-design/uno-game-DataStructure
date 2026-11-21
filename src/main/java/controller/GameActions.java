package controller;

import model.Color;

public interface GameActions {
    void play(int cardIndex, Color chosenColor);
    void play(int cardIndex, Color chosenColor, int targetSeat);
    void draw();
    void drawMachine();
    void playDrawn(Color chosenColor);
    void pass();
    void nextRound();
    void quit();
    void cheat(String rawCommand);
    String scoreboardText();
}
