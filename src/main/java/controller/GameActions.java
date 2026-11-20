package controller;

import model.Color;

/** What a View may ask the game controller to do during a game. */
public interface GameActions {
    void play(int cardIndex, Color chosenColor);   // chosenColor only for WILD / +4, otherwise null
    void draw();                                    // draw 1 card, or take the whole pending penalty
    void drawMachine();                             // lucky draw instead of taking a pending penalty
    void playDrawn(Color chosenColor);
    void pass();
    void nextRound();
    void quit();
    void cheat(String rawCommand);                  // cheat_give / cheat_top / cheat_empty_deck / cheat_win_round
    String scoreboardText();                        // per-round scores + running totals
}
