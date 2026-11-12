package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Genotype;
import io.jenetics.util.MSeq;

import java.util.Arrays;
import java.util.Collections;

public final class RouteGenotypeFactory {
    /**
     * Crée un Genotype où le chromosome est une permutation des indices clients.
     * idxs = {2,5,7,...} sont les indices dans la matrice de distances.
     */
    public static Genotype<IntegerGene> newGenotype(int[] idxs) {
        Integer[] perm = Arrays.stream(idxs).boxed().toArray(Integer[]::new);
        Collections.shuffle(Arrays.asList(perm));

        int min = Arrays.stream(idxs).min().orElse(0);
        int max = Arrays.stream(idxs).max().orElse(min);

        MSeq<IntegerGene> genes = MSeq.ofLength(perm.length);
        for (int i = 0; i < perm.length; i++) {
            genes.set(i, IntegerGene.of(perm[i], min, max));
        }

        IntegerChromosome chromosome = IntegerChromosome.of(genes.toISeq());
        return Genotype.of(chromosome);
    }

    /**
     * Fabrique pour Jenetics : génome aléatoire basé sur idxs.
     */
    public static Genotype<IntegerGene> factory(int[] idxs) {
        return newGenotype(idxs);
    }
}
