package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Genotype;
import io.jenetics.IntegerGene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Crée des individus aléatoires : chaque chromosome est une permutation des livraisons.
 */
public final class RouteGenotypeFactory {

    private RouteGenotypeFactory() {
    }

    public static Genotype<IntegerGene> newGenotype(int[] deliveryIndexes) {
        List<Integer> permutation = new ArrayList<>(Arrays.stream(deliveryIndexes).boxed().toList());
        Collections.shuffle(permutation);
        return RouteGenes.toGenotype(permutation.stream().mapToInt(Integer::intValue).toArray());
    }
}
