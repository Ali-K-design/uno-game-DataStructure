package controller;

import model.Card;

public final class CardText {
    private CardText() { }

    public static String of(Card card) {
        switch (card.getCardType()) {
            case NUMBER: return card.getColor() + " " + card.getValue();
            case SKIP: return card.getColor() + " SKIP";
            case REVERSE: return card.getColor() + " REVERSE";
            case DRAW_TWO: return card.getColor() + " DRAW TWO (+2)";
            case WILD: return "WILD";
            case SKIP_EVERYONE: return card.getColor() + " SKIP EVERYONE";
            case DISCARD_ALL: return card.getColor() + " DISCARD ALL";
            case WILD_DRAW_SIX: return "WILD DRAW SIX (+6)";
            case WILD_DRAW_TEN: return "WILD DRAW TEN (+10)";
            case COLOR_ROULETTE: return "WILD COLOR ROULETTE";
            default: return "WILD DRAW FOUR (+4)";
        }
    }
}
