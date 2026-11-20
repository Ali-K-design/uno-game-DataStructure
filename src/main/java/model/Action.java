package model;

/** A decision returned by Player.chooseAction(RoundState). */
public final class Action {
    public enum Type { PLAY, DRAW, AWAIT_INPUT }

    private final Type type;
    private final Card card;
    private final Color color;

    private Action(Type type, Card card, Color color) {
        this.type = type;
        this.card = card;
        this.color = color;
    }

    public static Action play(Card card, Color chosenColor) { return new Action(Type.PLAY, card, chosenColor); }
    public static Action draw() { return new Action(Type.DRAW, null, Color.NONE); }
    public static Action awaitInput() { return new Action(Type.AWAIT_INPUT, null, Color.NONE); }

    public Type getType() { return type; }
    public Card getCard() { return card; }
    public Color getColor() { return color; }
}
