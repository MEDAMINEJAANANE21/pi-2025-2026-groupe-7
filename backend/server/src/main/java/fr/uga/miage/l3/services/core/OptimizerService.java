package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.OptimizationResult;
import fr.uga.miage.l3.models.core.RouteStep;
import fr.uga.miage.l3.models.core.VehicleRoute;
import fr.uga.miage.l3.models.core.Summary;
import fr.uga.miage.l3.models.core.ComputingTimes;
import fr.uga.miage.l3.services.core.optimization.genetic.RouteGeneticAlgo;
import fr.uga.miage.l3.services.core.optimization.greedy.GreedyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class OptimizerService {
    private final OpenRouteServiceClient orsClient;

    public record RoutesWithResult(List<List<Integer>> routes, OptimizationResult result) {}

    public RoutesWithResult optimizeVroomRoutes(List<Integer> addressesGroup, int nbVehicles, int maxTimePerVehicle, List<Adresse> allLocations, Adresse parking) {
        log.info("Starting VROOM optimization for {} addresses with {} vehicles",
            addressesGroup.size(), nbVehicles);

        // Build request payload for VROOM optimization
        Map<String, Object> payload = new HashMap<>();

        // Configure vehicles
        List<Map<String, Object>> vehicles = new ArrayList<>();
        double[] parkingLngLat = new double[]{parking.coords.lng, parking.coords.lat};

        for (int i = 0; i < nbVehicles; i++) {
            Map<String, Object> vehicle = new HashMap<>();
            vehicle.put("id", i + 1);
            vehicle.put("profile", "driving-car");
            vehicle.put("start", parkingLngLat);
            vehicle.put("end", parkingLngLat);
            vehicle.put("max_travel_time", maxTimePerVehicle);
            vehicles.add(vehicle);
        }
        payload.put("vehicles", vehicles);

        // Configure jobs
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (Integer idx : addressesGroup) {
            Adresse address = allLocations.get(idx);
            Map<String, Object> job = new HashMap<>();
            job.put("id", idx); // Use original index for mapping back
            job.put("location", new double[]{address.coords.lng, address.coords.lat});
            job.put("setup", 30); // 30 minutes setup time
            job.put("service", 300); // 300 seconds (5 minutes) service time
            jobs.add(job);
        }
        payload.put("jobs", jobs);

        // Call ORS optimization endpoint
        OptimizationResult result = orsClient.optimize(payload);
        return new RoutesWithResult(extractRoutesFromResult(result), result);
    }

    public RoutesWithResult optimizeGeneticRoutes(
        List<Integer> addressesGroup,
        int nbVehicles,
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix
    ) {
        log.info("Starting genetic algorithm optimization for {} addresses with {} vehicles",
            addressesGroup.size(), nbVehicles);

        int parkingIdx = resolveParkingIndexLikeFrontend(allLocations, parking);
        int[] idxs = addressesGroup.stream().mapToInt(Integer::intValue).toArray();

        RouteGeneticAlgo algo = new RouteGeneticAlgo(idxs, parkingIdx, distanceMatrix);
        int[] bestRoute = algo.searchForFastestPath();
        List<Integer> route = Arrays.stream(bestRoute).boxed().toList();

        // Convert genetic result to OptimizationResult format
        OptimizationResult synthetic = generateSyntheticResult(route, allLocations, parking, distanceMatrix, parkingIdx);
        return new RoutesWithResult(List.of(route), synthetic);
    }

    public RoutesWithResult optimizeGreedyRoutes(
        List<Integer> addressesGroup,
        int nbVehicles,
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix
    ) {
        log.info("Starting greedy algorithm optimization for {} addresses with {} vehicles",
            addressesGroup.size(), nbVehicles);

        int parkingIdx = resolveParkingIndexLikeFrontend(allLocations, parking);

        // Use greedy service for optimization
        List<Integer> route = new GreedyCommand().searchForFastestPath(addressesGroup, parkingIdx, distanceMatrix);

        // Convert greedy result to OptimizationResult format
        OptimizationResult synthetic = generateSyntheticResult(route, allLocations, parking, distanceMatrix, parkingIdx);
        return new RoutesWithResult(List.of(route), synthetic);
    }

    private int resolveParkingIndexLikeFrontend(List<Adresse> allLocations, Adresse parking) {
        // Frontend uses findIndex((loc) => loc === parking), i.e. reference identity.
        for (int i = 0; i < allLocations.size(); i++) {
            if (allLocations.get(i) == parking) {
                return i;
            }
        }

        // Fallback only if references differ after mapping/deserialization.
        int equalsIdx = allLocations.indexOf(parking);
        if (equalsIdx >= 0) {
            return equalsIdx;
        }

        throw new IllegalArgumentException("Parking must be present in allLocations");
    }

    /**
     * Concatenates multiple OptimizationResults into a single result
     * Equivalent to frontend Optimizator.concatResults()
     */
    public OptimizationResult concatResults(List<OptimizationResult> results) {
        if (results == null || results.isEmpty()) {
            return OptimizationResult.builder()
                .code(0)
                .routes(new ArrayList<>())
                .unassigned(new ArrayList<>())
                .summary(createEmptySummary())
                .build();
        }

        log.info("Concatenating {} optimization results", results.size());

        List<VehicleRoute> allRoutes = new ArrayList<>();
        List<Object> allUnassigned = new ArrayList<>();

        // Aggregate all routes and unassigned items
        int vehicleIdOffset = 1;
        for (OptimizationResult result : results) {
            if (result.routes != null) {
                for (VehicleRoute route : result.routes) {
                    // Update vehicle ID to avoid conflicts
                    VehicleRoute updatedRoute = VehicleRoute.builder()
                        .cost(route.cost)
                        .duration(route.duration)
                        .priority(route.priority)
                        .service(route.service)
                        .setup(route.setup)
                        .vehicle(vehicleIdOffset++)
                        .violations(route.violations)
                        .waiting_time(route.waiting_time)
                        .steps(route.steps)
                        .build();
                    allRoutes.add(updatedRoute);
                }
            }

            if (result.unassigned != null) {
                allUnassigned.addAll(result.unassigned);
            }
        }

        // Aggregate summary statistics
        Summary aggregatedSummary = aggregateSummaries(results);

        return OptimizationResult.builder()
            .code(allRoutes.isEmpty() ? 1 : 0) // 0 = success, 1 = failure
            .routes(allRoutes)
            .unassigned(allUnassigned)
            .summary(aggregatedSummary)
            .build();
    }

    /**
     * Generate synthetic OptimizationResult for local algorithms (genetic, greedy)
     * Mimics frontend generateResult() function
     */
    private OptimizationResult generateSyntheticResult(
        List<Integer> route,
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix,
        int parkingIdx
    ) {
        List<RouteStep> steps = new ArrayList<>();

        steps.add(RouteStep.builder()
            .type("start")
            .arrival(0)
            .location(new double[]{parking.coords.lng, parking.coords.lat})
            .service(0)
            .setup(0)
            .build());

        for (Integer index : route) {
            Adresse adresse = allLocations.get(index);
            steps.add(RouteStep.builder()
                .type("job")
                .id(index)
                .arrival(0)
                .location(new double[]{adresse.coords.lng, adresse.coords.lat})
                .service(300)
                .setup(30)
                .build());
        }

        steps.add(RouteStep.builder()
            .type("end")
            .arrival(0)
            .location(new double[]{parking.coords.lng, parking.coords.lat})
            .service(0)
            .setup(0)
            .build());

        VehicleRoute vehicleRoute = VehicleRoute.builder()
            .cost((int) Math.round(totalDistance(distanceMatrix, route, parkingIdx)))
            .duration((int) Math.round(totalDuration(distanceMatrix, route, parkingIdx)))
            .priority(0)
            .service(route.size() * 300) // 300 seconds per address
            .setup(route.size() * 30) // 30 minutes setup per address
            .vehicle(1)
            .violations(new ArrayList<>())
            .waiting_time(0)
            .steps(steps)
            .build();

        Summary summary = Summary.builder()
            .computing_times(ComputingTimes.builder()
                .loading(0)
                .solving(0)
                .routing(0)
                .build())
            .cost(vehicleRoute.cost)
            .duration(vehicleRoute.duration)
            .priority(0)
            .service(route.size() * 300)
            .setup(route.size() * 30)
            .violations(new ArrayList<>())
            .waiting_time(0)
            .build();

        return OptimizationResult.builder()
            .code(0)
            .routes(List.of(vehicleRoute))
            .unassigned(new ArrayList<>())
            .summary(summary)
            .build();
    }

    private Summary aggregateSummaries(List<OptimizationResult> results) {
        int totalCost = 0;
        int totalDuration = 0;
        int totalPriority = 0;
        int totalService = 0;
        int totalSetup = 0;
        int totalWaitingTime = 0;
        List<Object> allViolations = new ArrayList<>();

        for (OptimizationResult result : results) {
            if (result.summary != null) {
                totalCost += result.summary.cost;
                totalDuration += result.summary.duration;
                totalPriority += result.summary.priority;
                totalService += result.summary.service;
                totalSetup += result.summary.setup;
                totalWaitingTime += result.summary.waiting_time;

                if (result.summary.violations != null) {
                    allViolations.addAll(result.summary.violations);
                }
            }
        }

        return Summary.builder()
            .computing_times(ComputingTimes.builder()
                .loading(0)
                .solving(0)
                .routing(0)
                .build())
            .cost(totalCost)
            .duration(totalDuration)
            .priority(totalPriority)
            .service(totalService)
            .setup(totalSetup)
            .violations(allViolations)
            .waiting_time(totalWaitingTime)
            .build();
    }

    private Summary createEmptySummary() {
        return Summary.builder()
            .computing_times(ComputingTimes.builder()
                .loading(0)
                .solving(0)
                .routing(0)
                .build())
            .cost(0)
            .duration(0)
            .priority(0)
            .service(0)
            .setup(0)
            .violations(new ArrayList<>())
            .waiting_time(0)
            .build();
    }

    private double totalDistance(double[][] matrix, List<Integer> route, int parkingIdx) {
        if (matrix == null || matrix.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        int prev = parkingIdx;
        for (Integer idx : route) {
            sum += safeMatrix(matrix, prev, idx);
            prev = idx;
        }
        if (!route.isEmpty()) {
            sum += safeMatrix(matrix, prev, parkingIdx);
        }
        return sum;
    }

    private double totalDuration(double[][] matrix, List<Integer> route, int parkingIdx) {
        // Reuse distance matrix as duration proxy if durations are not provided for local algos
        return totalDistance(matrix, route, parkingIdx);
    }

    private double safeMatrix(double[][] matrix, int i, int j) {
        if (i < 0 || j < 0 || i >= matrix.length || j >= matrix[i].length) {
            return 0.0;
        }
        return matrix[i][j];
    }

    private List<List<Integer>> extractRoutesFromResult(OptimizationResult result) {
        if (result == null || result.routes == null) {
            return List.of();
        }
        List<List<Integer>> routes = new ArrayList<>();
        for (VehicleRoute route : result.routes) {
            List<Integer> ids = new ArrayList<>();
            if (route.steps != null) {
                for (RouteStep step : route.steps) {
                    if (step != null && "job".equalsIgnoreCase(step.type) && step.id != null) {
                        ids.add(step.id);
                    }
                }
            }
            routes.add(ids);
        }
        return routes;
    }
}
