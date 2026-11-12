package fr.uga.miage.l3.services.statistics;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Calcule distance, temps de trajet, temps de montage et carburant d'une tournée.
 */
@Component
@RequiredArgsConstructor
public class TourneeStatisticsCalculator {

    private final FinanceCalculator financeCalculator;

    public Statistic fromProblem(List<Integer> route, OptimisationProblem problem) {
        long distance = 0;
        Duration travel = Duration.ZERO;
        int previous = problem.parkingIndex();
        for (int index : route) {
            distance += Math.round(problem.distances()[previous][index]);
            travel = travel.plus(problem.durations()[previous][index]);
            previous = index;
        }
        if (!route.isEmpty()) {
            distance += Math.round(problem.distances()[previous][problem.parkingIndex()]);
            travel = travel.plus(problem.durations()[previous][problem.parkingIndex()]);
        }
        List<LivraisonEntity> livraisons = route.stream().map(problem::livraisonAt).toList();
        return fromTotals(distance, travel, livraisons, null);
    }

    public Statistic fromTotals(long distanceMeters, Duration travel, List<LivraisonEntity> livraisons, VehiculeEntity vehicule) {
        return Statistic.builder()
            .distance(distanceMeters)
            .tempsTrajet(travel)
            .tempsMontage(montageOf(livraisons))
            .carburant(financeCalculator.fuelMilliliters(distanceMeters, consumptionOf(vehicule)))
            .build();
    }

    public Statistic withVehicule(Statistic statistic, VehiculeEntity vehicule) {
        if (statistic == null || statistic.getDistance() == null) {
            return statistic;
        }
        return Statistic.builder()
            .distance(statistic.getDistance())
            .tempsTrajet(statistic.getTempsTrajet())
            .tempsMontage(statistic.getTempsMontage())
            .carburant(financeCalculator.fuelMilliliters(statistic.getDistance(), consumptionOf(vehicule)))
            .build();
    }

    private static Duration montageOf(List<LivraisonEntity> livraisons) {
        return livraisons.stream()
            .map(LivraisonEntity::getTempsMontage)
            .filter(Objects::nonNull)
            .reduce(Duration.ZERO, Duration::plus);
    }

    private static Double consumptionOf(VehiculeEntity vehicule) {
        return vehicule == null ? null : vehicule.getConsommationLitresAux100Km();
    }
}
