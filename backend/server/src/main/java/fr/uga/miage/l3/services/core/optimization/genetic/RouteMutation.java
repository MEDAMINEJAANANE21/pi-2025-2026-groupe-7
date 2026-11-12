package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Alterer;
import io.jenetics.AltererResult;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.MSeq;
import io.jenetics.util.Seq;

import java.util.Random;

/**
 * Swap-only mutation to mirror the TypeScript implementation: for each selected
 * genome we perform len/2 random swaps, keeping alleles as a permutation.
 */
public class RouteMutation implements Alterer<IntegerGene, Double> {

    private final double probability;
    private final Random random;

    public RouteMutation(double probability) {
        this(probability, new Random());
    }

    RouteMutation(double probability, Random random) {
        this.probability = probability;
        this.random = random;
    }

    @Override
    public AltererResult<IntegerGene, Double> alter(Seq<Phenotype<IntegerGene, Double>> population, long generation) {
        MSeq<Phenotype<IntegerGene, Double>> pop = population.asMSeq();
        int alterations = 0;

        for (int i = 0; i < pop.size(); i++) {
            if (random.nextDouble() < probability) {
                int[] route = toRoute(pop.get(i).genotype());

                int swaps = route.length / 2; // mirrors TS loop
                for (int s = 0; s < swaps; s++) {
                    int a = random.nextInt(route.length);
                    int b = random.nextInt(route.length);
                    swap(route, a, b);
                }

                pop.set(i, phenotypeFromRoute(route, generation));
                alterations++;
            }
        }

        return new AltererResult<>(pop.toISeq(), alterations);
    }

    private static void swap(int[] route, int i, int j) {
        int tmp = route[i];
        route[i] = route[j];
        route[j] = tmp;
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
}
