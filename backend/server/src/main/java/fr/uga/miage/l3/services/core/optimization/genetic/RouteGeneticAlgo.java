package fr.uga.miage.l3.services.core.optimization.genetic;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.Factory;

public class RouteGeneticAlgo {

    private final double[][] matrix;   // matrice des distances
    private final int parkingIdx;      // index parking dans la matrice
    private final int[] idxs;          // indices des clients dans la matrice

    public RouteGeneticAlgo(int[] idxs, int parkingIdx, double[][] matrix) {
        this.idxs = idxs;
        this.parkingIdx = parkingIdx;
        this.matrix = matrix;
    }

    /**
     * Equivalent à searchForFastestPath(idxs, parkingIdx, matrix) en TS.
     */
    public int[] searchForFastestPath() {
        // Fabrique de Genotype: permutation d'indices clients
        Factory<Genotype<IntegerGene>> genotypeFactory =
                () -> RouteGenotypeFactory.factory(idxs);

        Engine<IntegerGene, Double> engine = Engine
                .builder(this::fitness, genotypeFactory)
                .populationSize(100)                      // populationSize
                .selector(new TournamentSelector<>(2))    // RandomSelection(2) analogue
                .alterers(
                        new RouteCrossover(0.5),          // crossoverRate = 0.5
                        new RouteMutation(0.1)             // swap-only mutation to keep permutations
                )
                .build();

        EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

        Phenotype<IntegerGene, Double> best = engine.stream()
                .limit(1000)                              // generationsCount = 1000
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());

        System.out.println(statistics);

        // Décodage du meilleur individu -> route d'indices clients
        int[] bestRoute = decodeRoute(best.genotype());
        return bestRoute;
    }

    /**
     * Fitness = 1 / (distance + 0.001) comme en TypeScript.
     */
    private Double fitness(Genotype<IntegerGene> gt) {
        int[] route = decodeRoute(gt);
        double distance = 0.0;

        // parking -> premier
        distance += matrix[parkingIdx][route[0]];

        // route[i] -> route[i+1]
        for (int i = 0; i < route.length - 1; i++) {
            distance += matrix[route[i]][route[i + 1]];
        }

        // dernier -> parking
        distance += matrix[route[route.length - 1]][parkingIdx];

        return 1.0 / (distance + 0.001);
    }

    /**
     * Décode le Genotype: on lit directement les valeurs des gènes
     * comme indices clients (chaque gène = index de client).
     */
    private int[] decodeRoute(Genotype<IntegerGene> gt) {
        IntegerChromosome chromosome = (IntegerChromosome) gt.chromosome();
        int length = chromosome.length();
        int[] route = new int[length];

        for (int i = 0; i < length; i++) {
            route[i] = chromosome.get(i).intValue();
        }
        return route;
    }
}
