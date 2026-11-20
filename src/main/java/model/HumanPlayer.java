package model;

public class HumanPlayer extends Player {
    public HumanPlayer(String name, Account account) {
        super(name, account);
    }

    @Override
    public Action chooseAction(RoundState state) {
        return Action.awaitInput();   // the View (console / GUI) supplies the move
    }

    @Override
    public boolean isBot() { return false; }
}
