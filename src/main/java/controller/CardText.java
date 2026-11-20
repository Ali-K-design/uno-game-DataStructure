package controller;

import model.Card;

/** Human-readable card names shared by the log, the console and tests. */
public final class CardText {
    private CardText() { }

    public static String of(Card card) {
        switch (card.getCardType()) {
            case NUMBER: return card.getColor() + " " + card.getValue();
            case SKIP: return card.getColor() + " SKIP";
            case REVERSE: return card.getColor() + " REVERSE";
            case DRAW_TWO: return card.getColor() + " DRAW TWO (+2)";
            case WILD: return "WILD";
            default: return "WILD DRAW FOUR (+4)";
        }
    }
}
