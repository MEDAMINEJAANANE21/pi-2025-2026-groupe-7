package fr.uga.miage.l3.services.optimisation.routing;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.services.core.optimization.genetic.RouteGeneticAlgo;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Ordonne un groupe avec un algorithme génétique : un seul véhicule par groupe.
 */
@Component
public class GeneticRouteOptimizer implements RouteOptimizer {

    @Override
    public Optimisation type() {
        return Optimisation.GENETIC;
    }

    @Override
    public List<List<Integer>> optimize(List<Integer> group, int nbVehicules, RouteConstraints constraints, OptimisationProblem problem) {
        if (group.size() <= 1) {
            return group.isEmpty() ? List.of() : List.of(List.copyOf(group));
        }
        int[] indexes = group.stream().mapToInt(Integer::intValue).toArray();
        int[] bestRoute = new RouteGeneticAlgo(indexes, problem.parkingIndex(), problem.distances()).searchForFastestPath();
        return List.of(Arrays.stream(bestRoute).boxed().toList());
    }
}
