package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.TournamentSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;

/**
 * Cherche l'ordre de passage le plus court avec un algorithme génétique (bibliothèque Jenetics).
 *
 * <p>Un individu est une permutation des livraisons ; sa note est l'inverse de la distance
 * totale dépôt → livraisons → dépôt.
 */
public class RouteGeneticAlgo {

    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 1000;
    private static final double CROSSOVER_PROBABILITY = 0.5;
    private static final double MUTATION_PROBABILITY = 0.1;

    private final int[] deliveryIndexes;
    private final int parkingIndex;
    private final double[][] distances;

    public RouteGeneticAlgo(int[] deliveryIndexes, int parkingIndex, double[][] distances) {
        this.deliveryIndexes = deliveryIndexes;
        this.parkingIndex = parkingIndex;
        this.distances = distances;
    }

    public int[] searchForFastestPath() {
        Engine<IntegerGene, Double> engine = Engine
            .builder(this::fitness, () -> RouteGenotypeFactory.newGenotype(deliveryIndexes))
            .populationSize(POPULATION_SIZE)
            .selector(new TournamentSelector<>(2))
            .alterers(new RouteCrossover(CROSSOVER_PROBABILITY), new RouteMutation(MUTATION_PROBABILITY))
            .build();

        Phenotype<IntegerGene, Double> best = engine.stream()
            .limit(GENERATIONS)
            .collect(EvolutionResult.toBestPhenotype());
        return RouteGenes.toRoute(best.genotype());
    }

    double totalDistance(int[] route) {
        double distance = distances[parkingIndex][route[0]];
        for (int i = 0; i < route.length - 1; i++) {
            distance += distances[route[i]][route[i + 1]];
        }
        return distance + distances[route[route.length - 1]][parkingIndex];
    }

    private Double fitness(Genotype<IntegerGene> genotype) {
        // Le epsilon évite une division par zéro quand toutes les livraisons sont au même endroit.
        return 1.0 / (totalDistance(RouteGenes.toRoute(genotype)) + 0.001);
    }
}
