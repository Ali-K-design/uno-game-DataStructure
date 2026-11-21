package model;

public final class Action {
    public enum Type { PLAY, DRAW, AWAIT_INPUT }

    private final Type type;
    private final Card card;
    private final Color color;
    private final Player target;

    private Action(Type type, Card card, Color color, Player target) {
        this.type = type;
        this.card = card;
        this.color = color;
        this.target = target;
    }

    public static Action play(Card card, Color chosenColor) { return new Action(Type.PLAY, card, chosenColor, null); }
    public static Action play(Card card, Color chosenColor, Player target) { return new Action(Type.PLAY, card, chosenColor, target); }
    public static Action draw() { return new Action(Type.DRAW, null, Color.NONE, null); }
    public static Action awaitInput() { return new Action(Type.AWAIT_INPUT, null, Color.NONE, null); }

    public Type getType() { return type; }
    public Card getCard() { return card; }
    public Color getColor() { return color; }
    public Player getTarget() { return target; }
}
