package fr.uga.miage.l3.services.optimisation.routing;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.OptimizationResult;
import fr.uga.miage.l3.models.core.RouteStep;
import fr.uga.miage.l3.models.core.VehicleRoute;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Délègue le calcul au solveur VROOM exposé par OpenRouteService (endpoint /optimization).
 */
@Component
@RequiredArgsConstructor
public class VroomRouteOptimizer implements RouteOptimizer {

    private static final String PROFILE = "driving-car";

    private final OpenRouteServiceClient orsClient;

    @Override
    public Optimisation type() {
        return Optimisation.VROOM;
    }

    @Override
    public List<List<Integer>> optimize(List<Integer> group, int nbVehicules, RouteConstraints constraints, OptimisationProblem problem) {
        if (group.isEmpty()) {
            return List.of();
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("vehicles", vehicles(nbVehicules, constraints, problem.parking()));
        payload.put("jobs", jobs(group, problem));
        return routesOf(orsClient.optimize(payload));
    }

    private static List<Map<String, Object>> vehicles(int nbVehicules, RouteConstraints constraints, Adresse parking) {
        double[] depot = lngLat(parking);
        List<Map<String, Object>> vehicles = new ArrayList<>();
        for (int id = 1; id <= nbVehicules; id++) {
            Map<String, Object> vehicle = new HashMap<>();
            vehicle.put("id", id);
            vehicle.put("profile", PROFILE);
            vehicle.put("start", depot);
            vehicle.put("end", depot);
            if (constraints.maxTravelSeconds() > 0) {
                vehicle.put("max_travel_time", constraints.maxTravelSeconds());
            }
            vehicles.add(vehicle);
        }
        return vehicles;
    }

    private static List<Map<String, Object>> jobs(List<Integer> group, OptimisationProblem problem) {
        return group.stream()
            .map(index -> {
                Map<String, Object> job = new HashMap<>();
                job.put("id", index);
                job.put("location", lngLat(problem.locations().get(index)));
                job.put("service", montageSeconds(problem.livraisonAt(index)));
                return job;
            })
            .toList();
    }

    private static List<List<Integer>> routesOf(OptimizationResult result) {
        if (result == null || result.routes == null) {
            return List.of();
        }
        return result.routes.stream()
            .map(VroomRouteOptimizer::jobIndexes)
            .filter(route -> !route.isEmpty())
            .toList();
    }

    private static List<Integer> jobIndexes(VehicleRoute route) {
        if (route.steps == null) {
            return List.of();
        }
        return route.steps.stream()
            .filter(Objects::nonNull)
            .filter(step -> "job".equalsIgnoreCase(step.type))
            .map(RouteStep::getId)
            .filter(Objects::nonNull)
            .toList();
    }

    private static long montageSeconds(LivraisonEntity livraison) {
        Duration montage = livraison == null ? null : livraison.getTempsMontage();
        return montage == null ? 0 : montage.toSeconds();
    }

    private static double[] lngLat(Adresse adresse) {
        return new double[]{adresse.coords.lng, adresse.coords.lat};
    }
}
