package game;

import java.util.Random;

/**
 * The "lucky draw" a player may use instead of taking a pending penalty (spec 3.8).
 * Outcome = how many cards are drawn; it REPLACES the whole pending penalty.
 * The table used depends on the card that created the top of the penalty chain.
 */
public class DrawMachine {
    private static final int[] PLUS_TWO_CARDS = {0, 1, 2, 4};
    private static final int[] PLUS_TWO_WEIGHTS = {1, 2, 2, 1};
    private static final int[] PLUS_FOUR_CARDS = {0, 2, 4, 6};
    private static final int[] PLUS_FOUR_WEIGHTS = {1, 2, 2, 1};

    private final Random random;

    public DrawMachine() { this(new Random()); }

    public DrawMachine(Random random) { this.random = random; }

    public int roll(boolean plusFour) {
        int[] cards = plusFour ? PLUS_FOUR_CARDS : PLUS_TWO_CARDS;
        int[] weights = plusFour ? PLUS_FOUR_WEIGHTS : PLUS_TWO_WEIGHTS;
        return cards[WeightedRandom.pick(weights, random)];
    }
}
