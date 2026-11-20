package game;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WeightedRandomTest {
    @Test
    void prefixSumsAreCumulative() {
        int[] p = WeightedRandom.buildPrefixSums(new int[]{1, 2, 2});
        assertEquals(1, p[0]);
        assertEquals(3, p[1]);
        assertEquals(5, p[2]);
    }

    @Test
    void zeroWeightIsNeverChosen() {
        Random random = new Random(7);
        for (int i = 0; i < 5000; i++) {
            assertTrue(WeightedRandom.pick(new int[]{0, 3, 0, 1}, random) != 0);
            assertTrue(WeightedRandom.pick(new int[]{0, 3, 0, 1}, random) != 2);
        }
    }

    @Test
    void invalidWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> WeightedRandom.buildPrefixSums(new int[]{0, 0}));
        assertThrows(IllegalArgumentException.class, () -> WeightedRandom.buildPrefixSums(new int[]{2, -1}));
        assertThrows(IllegalArgumentException.class, () -> WeightedRandom.buildPrefixSums(new int[0]));
    }

    @Test
    void boundaryValuesMapToTheRightOption() {
        int[] p = WeightedRandom.buildPrefixSums(new int[]{1, 2, 2});
        assertEquals(0, WeightedRandom.indexFor(p, 0));
        assertEquals(1, WeightedRandom.indexFor(p, 1));
        assertEquals(1, WeightedRandom.indexFor(p, 2));
        assertEquals(2, WeightedRandom.indexFor(p, 3));
        assertEquals(2, WeightedRandom.indexFor(p, 4));
    }

    @Test
    void distributionIsCloseToTheWeights() {
        Random random = new Random(11);
        int[] hits = new int[3];
        int trials = 100000;
        for (int i = 0; i < trials; i++) hits[WeightedRandom.pick(new int[]{1, 2, 2}, random)]++;
        assertTrue(Math.abs(hits[0] / (double) trials - 0.2) < 0.01);
        assertTrue(Math.abs(hits[1] / (double) trials - 0.4) < 0.01);
        assertTrue(Math.abs(hits[2] / (double) trials - 0.4) < 0.01);
    }

    @Test
    void drawMachineOnlyProducesTableValues() {
        DrawMachine machine = new DrawMachine(new Random(3));
        for (int i = 0; i < 2000; i++) {
            int two = machine.roll(false);
            int four = machine.roll(true);
            assertTrue(two == 0 || two == 1 || two == 2 || two == 4);
            assertTrue(four == 0 || four == 2 || four == 4 || four == 6);
        }
    }
}
