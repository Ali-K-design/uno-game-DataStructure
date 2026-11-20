package game;

import java.util.Random;

/** Weighted Random Selection with a hand-built prefix-sum array (no collections). */
public final class WeightedRandom {
    private WeightedRandom() { }

    /** Builds cumulative sums; rejects null/empty input, negative weights and an all-zero total. */
    public static int[] buildPrefixSums(int[] weights) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("Weights must not be empty.");
        }
        int[] prefix = new int[weights.length];
        int running = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0) {
                throw new IllegalArgumentException("Negative weight at index " + i);
            }
            running += weights[i];
            prefix[i] = running;
        }
        if (running == 0) {
            throw new IllegalArgumentException("Total weight must be greater than zero.");
        }
        return prefix;
    }

    /** First index whose prefix sum is strictly greater than r (so zero-weight options can never win). */
    public static int indexFor(int[] prefix, int r) {
        for (int i = 0; i < prefix.length; i++) {
            if (r < prefix[i]) return i;
        }
        throw new IllegalArgumentException("r is outside the total weight range.");
    }

    public static int pick(int[] weights, Random random) {
        int[] prefix = buildPrefixSums(weights);
        return indexFor(prefix, random.nextInt(prefix[prefix.length - 1]));
    }
}
