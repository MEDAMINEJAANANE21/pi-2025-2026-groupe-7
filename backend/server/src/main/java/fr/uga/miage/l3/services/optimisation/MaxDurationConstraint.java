package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applique la durée maximale par véhicule après l'optimisation.
 *
 * <p>Tant qu'une tournée dépasse le budget, on retire la commande la plus récente :
 * les clients qui attendent depuis le plus longtemps restent prioritaires.
 */
@Component
public class MaxDurationConstraint {

    public List<List<Integer>> apply(List<List<Integer>> routes, Duration maxDuration, OptimisationProblem problem) {
        if (maxDuration == null || maxDuration.isNegative() || maxDuration.isZero()) {
            return routes;
        }
        return routes.stream()
            .map(route -> trim(new ArrayList<>(route), maxDuration, problem))
            .filter(route -> !route.isEmpty())
            .toList();
    }

    Duration durationOf(List<Integer> route, OptimisationProblem problem) {
        Duration total = Duration.ZERO;
        int previous = problem.parkingIndex();
        for (int index : route) {
            total = total.plus(problem.durations()[previous][index]).plus(montageOf(problem.livraisonAt(index)));
            previous = index;
        }
        return route.isEmpty() ? total : total.plus(problem.durations()[previous][problem.parkingIndex()]);
    }

    private List<Integer> trim(List<Integer> route, Duration maxDuration, OptimisationProblem problem) {
        while (!route.isEmpty() && durationOf(route, problem).compareTo(maxDuration) > 0) {
            route.remove(mostRecentOrder(route, problem));
        }
        return route;
    }

    private static Integer mostRecentOrder(List<Integer> route, OptimisationProblem problem) {
        return route.stream()
            .max(Comparator.comparing(index -> orderDate(problem.livraisonAt(index))))
            .orElseThrow();
    }

    private static LocalDate orderDate(LivraisonEntity livraison) {
        return livraison.getDateCommande() == null ? LocalDate.MIN : livraison.getDateCommande();
    }

    private static Duration montageOf(LivraisonEntity livraison) {
        return livraison.getTempsMontage() == null ? Duration.ZERO : livraison.getTempsMontage();
    }
}
