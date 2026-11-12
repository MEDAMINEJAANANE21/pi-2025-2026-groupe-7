package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.services.core.separation.SeparationStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Choisit la stratégie de séparation demandée et garantit un résultat exploitable.
 */
@Component
public class GroupSeparator {

    private final Map<Separation, SeparationStrategy> strategies = new EnumMap<>(Separation.class);

    public GroupSeparator(List<SeparationStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
    }

    public List<List<Integer>> separate(Separation separation, OptimisationProblem problem, int nbGroups) {
        SeparationStrategy fallback = strategies.get(Separation.NONE);
        SeparationStrategy strategy = strategies.getOrDefault(separation, fallback);
        List<List<Integer>> groups = strategy.separate(problem.locations(), problem.distances(), Math.max(1, nbGroups));
        if (groups == null || groups.isEmpty()) {
            return fallback.separate(problem.locations(), problem.distances(), 1);
        }
        return groups;
    }
}
