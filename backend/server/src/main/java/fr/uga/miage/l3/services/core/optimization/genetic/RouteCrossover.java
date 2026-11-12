package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Alterer;
import io.jenetics.AltererResult;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.MSeq;
import io.jenetics.util.Seq;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Croisement qui garde une permutation valide : l'enfant reprend une case sur deux d'un parent
 * et complète les trous avec les livraisons manquantes, dans l'ordre de l'autre parent.
 */
public class RouteCrossover implements Alterer<IntegerGene, Double> {

    private static final int EMPTY = -1;

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
                int[] child = crossoverRoutes(
                    RouteGenes.toRoute(pop.get(i).genotype()),
                    RouteGenes.toRoute(pop.get(i + 1).genotype()),
                    random
                );
                pop.set(i, RouteGenes.toPhenotype(child, generation));
                alterations++;
            }
        }
        return new AltererResult<>(pop.toISeq(), alterations);
    }

    static int[] crossoverRoutes(int[] parent1, int[] parent2, Random random) {
        if (parent1.length != parent2.length) {
            throw new IllegalArgumentException("Parents must have the same length");
        }
        boolean swapParents = random.nextBoolean();
        int[] kept = swapParents ? parent2.clone() : parent1.clone();
        int[] filler = swapParents ? parent1.clone() : parent2.clone();
        boolean keepOddPositions = random.nextBoolean();

        int[] child = new int[kept.length];
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < kept.length; i++) {
            boolean keep = (i % 2 == 1) == keepOddPositions;
            child[i] = keep ? kept[i] : EMPTY;
            if (keep) {
                used.add(kept[i]);
            }
        }
        for (int i = 0; i < child.length; i++) {
            if (child[i] == EMPTY) {
                child[i] = takeFirstUnused(filler, used);
                used.add(child[i]);
            }
        }
        if (used.size() != child.length) {
            throw new IllegalStateException("Child route is not a permutation of parents");
        }
        return child;
    }

    private static int takeFirstUnused(int[] parent, Set<Integer> used) {
        for (int i = 0; i < parent.length; i++) {
            int value = parent[i];
            if (value >= 0 && !used.contains(value)) {
                parent[i] = EMPTY;
                return value;
            }
        }
        throw new IllegalStateException("No new gene found");
    }
}
