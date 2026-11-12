package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.*;
import io.jenetics.util.MSeq;
import io.jenetics.util.Seq;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RouteCrossover implements Alterer<IntegerGene, Double> {

    private final double probability;
    private final Random random;

    public RouteCrossover(double probability) {
        this(probability, new Random());
    }

    RouteCrossover(double probability, Random random) {
        this.probability = probability;
        this.random = random;
    }

    @Override
    public AltererResult<IntegerGene, Double> alter(Seq<Phenotype<IntegerGene, Double>> population, long generation) {
        MSeq<Phenotype<IntegerGene, Double>> pop = population.asMSeq();
        int alterations = 0;

        for (int i = 0; i + 1 < pop.size(); i += 2) {
            if (random.nextDouble() < probability) {
                Phenotype<IntegerGene, Double> p1 = pop.get(i);
                Phenotype<IntegerGene, Double> p2 = pop.get(i + 1);

                int[] childRoute = crossoverRoutes(
                    toRoute(p1.genotype()),
                    toRoute(p2.genotype()),
                    random
                );

                pop.set(i, phenotypeFromRoute(childRoute, generation));
                alterations++;
            }
        }

        return new AltererResult<>(pop.toISeq(), alterations);
    }

    static int[] crossoverRoutes(int[] parent1, int[] parent2, Random random) {
        int length = parent1.length;
        if (length != parent2.length) {
            throw new IllegalArgumentException("Parents must have the same length");
        }

        int[] p1 = parent1.clone();
        int[] p2 = parent2.clone();

        // Shuffle parent order like the TS implementation
        if (random.nextBoolean()) {
            int[] tmp = p1;
            p1 = p2;
            p2 = tmp;
        }

        boolean restOdd = random.nextBoolean();
        int[] child = new int[length];
        Set<Integer> used = new HashSet<>();

        for (int i = 0; i < length; i++) {
            if ((i % 2 == 1) == restOdd) {
                child[i] = p1[i];
                used.add(child[i]);
            } else {
                child[i] = -1;
            }
        }

        for (int i = 0; i < length; i++) {
            if (child[i] == -1) {
                child[i] = takeFirstNewGene(p2, used);
                used.add(child[i]);
            }
        }

        if (used.size() != length) {
            throw new IllegalStateException("Child route is not a permutation of parents");
        }

        return child;
    }

    private static int[] toRoute(Genotype<IntegerGene> genotype) {
        IntegerChromosome chromosome = (IntegerChromosome) genotype.chromosome();
        int[] route = new int[chromosome.length()];
        for (int i = 0; i < chromosome.length(); i++) {
            route[i] = chromosome.get(i).intValue();
        }
        return route;
    }

    private Phenotype<IntegerGene, Double> phenotypeFromRoute(int[] route, long generation) {
        int min = route.length == 0 ? 0 : route[0];
        int max = min;
        for (int value : route) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        MSeq<IntegerGene> genes = MSeq.ofLength(route.length);
        for (int i = 0; i < route.length; i++) {
            genes.set(i, IntegerGene.of(route[i], min, max));
        }
        IntegerChromosome childChromosome = IntegerChromosome.of(genes.toISeq());
        return Phenotype.of(Genotype.of(childChromosome), generation);
    }

    private static int takeFirstNewGene(int[] parentValues, Set<Integer> used) {
        for (int i = 0; i < parentValues.length; i++) {
            int value = parentValues[i];
            if (value >= 0 && !used.contains(value)) {
                parentValues[i] = -1;
                return value;
            }
        }
        throw new IllegalStateException("No new gene found");
    }
}
