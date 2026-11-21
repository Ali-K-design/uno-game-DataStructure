package model;

public enum CardType {
    NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR,
    SKIP_EVERYONE, DISCARD_ALL, WILD_DRAW_SIX, WILD_DRAW_TEN, COLOR_ROULETTE;

    public boolean isWildFamily() {
        return this == WILD || this == WILD_DRAW_FOUR || this == WILD_DRAW_SIX
                || this == WILD_DRAW_TEN || this == COLOR_ROULETTE;
    }

    public int penaltyValue() {
        switch (this) {
            case DRAW_TWO: return 2;
            case WILD_DRAW_FOUR: return 4;
            case WILD_DRAW_SIX: return 6;
            case WILD_DRAW_TEN: return 10;
            default: return 0;
        }
    }
}
