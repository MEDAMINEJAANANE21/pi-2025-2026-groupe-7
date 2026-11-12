package fr.uga.miage.l3.services.optimisation.routing;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.services.core.optimization.greedy.GreedyCommand;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ordonne un groupe avec l'heuristique du plus proche voisin : un seul véhicule par groupe.
 */
@Component
public class GreedyRouteOptimizer implements RouteOptimizer {

    private final GreedyCommand greedy = new GreedyCommand();

    @Override
    public Optimisation type() {
        return Optimisation.GREEDY;
    }

    @Override
    public List<List<Integer>> optimize(List<Integer> group, int nbVehicules, RouteConstraints constraints, OptimisationProblem problem) {
        if (group.isEmpty()) {
            return List.of();
        }
        return List.of(greedy.searchForFastestPath(group, problem.parkingIndex(), problem.distances()));
    }
}
