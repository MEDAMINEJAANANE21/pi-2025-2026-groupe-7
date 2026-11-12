package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.ISeq;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RouteCrossoverTest {

    @Test
    void crossoverRoutesAlternatesAndFillsFromSecondParent() {
        int[] parent1 = {0, 1, 2, 3};
        int[] parent2 = {3, 2, 1, 0};

        StubRandom rnd = new StubRandom(new double[0], new boolean[]{false, true}, new int[0]);

        int[] child = RouteCrossover.crossoverRoutes(parent1, parent2, rnd);

        assertArrayEquals(new int[]{2, 1, 0, 3}, child);
        assertEquals(parent1.length, Arrays.stream(child).distinct().count());
    }

    @Test
    void crossoverRoutesRespectsParentSwapAndEvenPositions() {
        int[] parent1 = {0, 1, 2, 3};
        int[] parent2 = {3, 2, 1, 0};

        StubRandom rnd = new StubRandom(new double[0], new boolean[]{true, false}, new int[0]);

        int[] child = RouteCrossover.crossoverRoutes(parent1, parent2, rnd);

        assertArrayEquals(new int[]{3, 0, 1, 2}, child);
        assertEquals(parent1.length, Arrays.stream(child).distinct().count());
    }

    @Test
    void crossoverRoutesRejectsDifferentLengths() {
        int[] parent1 = {0, 1, 2};
        int[] parent2 = {0, 1};

        assertThrows(IllegalArgumentException.class, () -> RouteCrossover.crossoverRoutes(parent1, parent2, new Random()));
    }

    @Test
    void routeMutationSwapsWithoutChangingPermutation() {
        int[] initialRoute = {0, 1, 2, 3};
        Phenotype<IntegerGene, Double> phenotype = phenotypeFromRoute(initialRoute, 0);

        StubRandom rnd = new StubRandom(
            new double[]{0.0}, // always mutate
            new boolean[0],
            new int[]{0, 3, 1, 2} // swap 0<->3 then 1<->2
        );

        RouteMutation mutation = new RouteMutation(1.0, rnd);
        var result = mutation.alter(ISeq.of(phenotype), 1);

        int[] mutated = decodeRoute(result.population().get(0));

        assertArrayEquals(new int[]{3, 2, 1, 0}, mutated);
        assertEquals(initialRoute.length, Arrays.stream(mutated).distinct().count());
        assertTrue(Arrays.stream(mutated).boxed().allMatch(v -> Arrays.stream(initialRoute).anyMatch(i -> i == v)));
    }

    private static Phenotype<IntegerGene, Double> phenotypeFromRoute(int[] route, long generation) {
        int min = Arrays.stream(route).min().orElse(0);
        int max = Arrays.stream(route).max().orElse(min);

        ISeq<IntegerGene> genes = ISeq.of(Arrays.stream(route)
            .mapToObj(v -> IntegerGene.of(v, min, max))
            .toList());
        IntegerChromosome chromosome = IntegerChromosome.of(genes);
        return Phenotype.of(Genotype.of(chromosome), generation);
    }

    private static int[] decodeRoute(Phenotype<IntegerGene, Double> phenotype) {
        IntegerChromosome chromosome = (IntegerChromosome) phenotype.genotype().chromosome();
        int[] route = new int[chromosome.length()];
        for (int i = 0; i < chromosome.length(); i++) {
            route[i] = chromosome.get(i).intValue();
        }
        return route;
    }

    /**
     * Minimal deterministic RNG for the crossover/mutation tests.
     */
    static final class StubRandom extends Random {
        private final double[] doubles;
        private int di;
        private final boolean[] bools;
        private int bi;
        private final int[] ints;
        private int ii;

        StubRandom(double[] doubles, boolean[] bools, int[] ints) {
            this.doubles = doubles;
            this.bools = bools;
            this.ints = ints;
        }

        @Override
        public double nextDouble() {
            if (doubles.length == 0) {
                return super.nextDouble();
            }
            return doubles[di++ % doubles.length];
        }

        @Override
        public boolean nextBoolean() {
            if (bools.length == 0) {
                return super.nextBoolean();
            }
            return bools[bi++ % bools.length];
        }

        @Override
        public int nextInt(int bound) {
            if (ints.length == 0) {
                return super.nextInt(bound);
            }
            return Math.floorMod(ints[ii++ % ints.length], bound);
        }
    }
}

