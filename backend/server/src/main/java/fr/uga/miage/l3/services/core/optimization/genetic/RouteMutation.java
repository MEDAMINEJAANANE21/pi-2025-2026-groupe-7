package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Alterer;
import io.jenetics.AltererResult;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.MSeq;
import io.jenetics.util.Seq;

import java.util.Random;

/**
 * Mutation par échanges : on permute deux livraisons au hasard, ce qui garde une permutation valide.
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
                int[] route = RouteGenes.toRoute(pop.get(i).genotype());
                shuffleBySwaps(route);
                pop.set(i, RouteGenes.toPhenotype(route, generation));
                alterations++;
            }
        }
        return new AltererResult<>(pop.toISeq(), alterations);
    }

    void shuffleBySwaps(int[] route) {
        for (int swap = 0; swap < route.length / 2; swap++) {
            int a = random.nextInt(route.length);
            int b = random.nextInt(route.length);
            int tmp = route[a];
            route[a] = route[b];
            route[b] = tmp;
        }
    }
}
