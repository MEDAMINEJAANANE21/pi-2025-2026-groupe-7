package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.MSeq;

import java.util.Arrays;

/**
 * Conversions entre un chromosome Jenetics et une tournée (permutation d'indices de livraisons).
 */
final class RouteGenes {

    private RouteGenes() {
    }

    static int[] toRoute(Genotype<IntegerGene> genotype) {
        IntegerChromosome chromosome = (IntegerChromosome) genotype.chromosome();
        int[] route = new int[chromosome.length()];
        for (int i = 0; i < chromosome.length(); i++) {
            route[i] = chromosome.get(i).intValue();
        }
        return route;
    }

    static Genotype<IntegerGene> toGenotype(int[] route) {
        int min = Arrays.stream(route).min().orElse(0);
        int max = Arrays.stream(route).max().orElse(min);
        MSeq<IntegerGene> genes = MSeq.ofLength(route.length);
        for (int i = 0; i < route.length; i++) {
            genes.set(i, IntegerGene.of(route[i], min, max));
        }
        return Genotype.of(IntegerChromosome.of(genes.toISeq()));
    }

    static Phenotype<IntegerGene, Double> toPhenotype(int[] route, long generation) {
        return Phenotype.of(toGenotype(route), generation);
    }
}
