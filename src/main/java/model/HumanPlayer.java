package model;

public class HumanPlayer extends Player {
    private boolean autopilot;

    public HumanPlayer(String name, Account account) {
        super(name, account);
    }

    public void setAutopilot(boolean autopilot) { this.autopilot = autopilot; }

    @Override
    public Action chooseAction(RoundState state) {
        return autopilot ? BotPlayer.decide(this, state) : Action.awaitInput();
    }

    @Override
    public boolean isBot() { return autopilot; }
}
