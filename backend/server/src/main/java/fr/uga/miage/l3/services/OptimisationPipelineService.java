package fr.uga.miage.l3.services;

import fr.uga.miage.l3.component.JourneeComponent;
import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import fr.uga.miage.l3.exceptions.technical.MissingCheminsDataException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.mappers.StrategieMapper;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.core.OptimizationResult;
import fr.uga.miage.l3.models.domain.Matrices;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;

import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.services.core.AlgosService;
import fr.uga.miage.l3.services.core.OptimizerService;
import fr.uga.miage.l3.services.core.SeparateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisationPipelineService {

    private final SolutionComponent solutionComponent;
    private final JourneeComponent journeeComponent;
    private final JourneeRepository journeeRepository;
    private final LivraisonRepository livraisonRepository;
    private final ParkingRepository parkingRepository;
    private final StrategieMapper strategieMapper;
    private final AdresseMapper adresseMapper;
    private final AlgosService algosService;
    private final OptimizerService optimizerService;
    private final JourneeStatusBroadcastService journeeStatusBroadcastService;


    public JourneeEntity startJourneeOptimization(LocalDate date, StrategieRequestDTO request) {
        if (request == null) {
            throw new BadRequestRestException("Request body cannot be null");
        }
        if (request.nbVehicules() == null || request.nbVehicules() <= 0) {
            throw new BadRequestRestException("nbVehicules must be a strictly positive integer");
        }
        if (request.maxTempsParVehicule() == null || request.maxTempsParVehicule().isBlank()) {
            throw new BadRequestRestException("maxTempsParVehicule is required and must use ISO-8601 duration format (example: PT2H30M)");
        }

        Strategie strategie;
        try {
            strategie = strategieMapper.toEntity(request);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestRestException("Invalid strategy values: " + ex.getMessage());
        }

        JourneeEntity journee = journeeComponent.getOrCreate(date);

        if (journee.getStatutGeneration() != null) {
            return journee;
        }

        journee.setStatutGeneration(StatutGeneration.GENERATION_MATRICE);
        JourneeEntity savedJournee = journeeRepository.save(journee);
        journeeStatusBroadcastService.broadcast(savedJournee.getDate(), StatutGeneration.GENERATION_MATRICE);

        Set<Long> livraisonIds = request.livraisonIds();
        CompletableFuture.runAsync(() -> runBackgroundOptimisation(savedJournee.getDate(), strategie, livraisonIds));

        return savedJournee;
    }

    private void runBackgroundOptimisation(LocalDate date, Strategie baseStrategie, Set<Long> livraisonIds) {
        try {
            OptimizationContext context = getJourneeLivraisonsEtStrategie(date, baseStrategie, livraisonIds);
            DeliveryData deliveryData = buildDeliveryData(context.livraisons());
//      ensureMatricesData(deliveryData);

            SharedOptimisationData sharedData = buildSharedOptimisationData(deliveryData);

            updateJourneeStatus(date, StatutGeneration.GENERATION_ROUTE);

            List<Strategie> possibleStrategies = generatePossibleStrategies(context.strategie());
            List<CompletableFuture<SolutionRunResult>> futures = possibleStrategies.stream()
                .map(strategie -> CompletableFuture.supplyAsync(() -> runOptimisationForStrategie(
                    context.journee(),
                    strategie,
                    deliveryData.deliveryByIndex(),
                    sharedData.allLocations(),
                    sharedData.parking(),
                    sharedData.distanceMatrix(),
                    sharedData.durationMatrix(),
                    livraisonIds
                )))
                .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            List<SolutionRunResult> successfulRuns = futures.stream()
                .map(CompletableFuture::join)
                .filter(run -> run != null && run.result() != null)
                .toList();

        } catch (Exception ex) {
            log.error("Journee optimisation failed for date {}", date, ex);
        } finally {
            updateJourneeStatus(date, StatutGeneration.TERMINE);
        }
    }

    private OptimizationContext getJourneeLivraisonsEtStrategie(LocalDate date, Strategie strategie, Set<Long> livraisonIds) {
        JourneeEntity journee = journeeRepository.findById(date)
            .orElseThrow(() -> new NotFoundElementRestException("Journee not found for date: " + date));

        List<LivraisonEntity> livraisons = livraisonRepository.findAllByLivree(false);

        if (livraisonIds != null && !livraisonIds.isEmpty()) {
            livraisons = livraisons.stream()
                .filter(l -> livraisonIds.contains(l.getId()))
                .toList();
        }

        if (livraisons.isEmpty()) {
            throw new BadRequestRestException("No livraisons available to optimize");
        }

        return new OptimizationContext(journee, livraisons, strategie);
    }

    private SolutionRunResult runOptimisationForStrategie(
        JourneeEntity journee,
        Strategie strategie,
        Map<Integer, LivraisonEntity> deliveryByIndex,
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix,
        Duration[][] durationMatrix,
        Set<Long> livraisonIds
    ) {
        try {
            List<List<Integer>> separatedGroups = splitDestinations(strategie, allLocations, distanceMatrix);

            List<OptimizerService.RoutesWithResult> partialResults = separatedGroups.stream()
                .map(group -> startOptimisationDependingOnStrategie(
                    strategie,
                    group,
                    allLocations,
                    parking,
                    distanceMatrix
                ))
                .toList();

            List<OptimizationResult> legacyResults = partialResults.stream()
                .map(OptimizerService.RoutesWithResult::result)
                .filter(r -> r != null)
                .toList();

            OptimizationResult globalResult = legacyResults.isEmpty()
                ? null
                : optimizerService.concatResults(legacyResults);

            List<List<Integer>> allRoutes = partialResults.stream()
                .flatMap(r -> r.routes().stream())
                .toList();

            int parkingIndex = allLocations.size() - 1;
            List<List<Integer>> constrainedRoutes = applyMaxTempsConstraint(
                allRoutes, strategie.getMaxTempsParVehicule(), durationMatrix, deliveryByIndex, parkingIndex);

            SolutionEntity solution = solutionComponent.saveSolutionWithTournees(
                SolutionEntity.builder()
                    .date(journee.getDate())
                    .strategie(strategie)
                    .livraisonsIds(livraisonIds != null ? new java.util.HashSet<>(livraisonIds) : new java.util.HashSet<>())
                    .build(),
                constrainedRoutes,
                deliveryByIndex,
                distanceMatrix,
                durationMatrix,
                parkingIndex
            );

            journeeStatusBroadcastService.broadcast(journee.getDate(), StatutGeneration.GENERATION_ROUTE);

            return new SolutionRunResult(solution, globalResult);
        } catch (Exception ex) {
            log.error("Failed to run optimisation for strategy {} / {}",
                strategie.getSeparation(),
                strategie.getOptimisation(),
                ex);
            return null;
        }
    }

    private SharedOptimisationData buildSharedOptimisationData(DeliveryData deliveryData) throws MissingCheminsDataException, InterruptedException {
        List<AdresseEntity> allEntities = new ArrayList<>(deliveryData.deliveryAddressesByIndex().values());
        allEntities.add(deliveryData.parkingAddress());

        List<Adresse> allLocations = adresseMapper.toCore(allEntities);
        Adresse parking = allLocations.get(allLocations.size() - 1);
        MatrixResponse response = algosService.getMatrices(allLocations);
        Matrices matrices = toDomainMatrices(response);
        return new SharedOptimisationData(allLocations, parking, matrices.distances(), matrices.durees());
    }

    private OptimizerService.RoutesWithResult startOptimisationDependingOnStrategie(
        Strategie strategie,
        List<Integer> addressesGroup,
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix
    ) {
        Optimisation optimisation = strategie.getOptimisation() == null ? Optimisation.VROOM : strategie.getOptimisation();

        return switch (optimisation) {
            case GENETIC -> optimizerService.optimizeGeneticRoutes(
                addressesGroup,
                strategie.getNbVehicules(),
                allLocations,
                parking,
                distanceMatrix
            );
            case GREEDY -> optimizerService.optimizeGreedyRoutes(
                addressesGroup,
                strategie.getNbVehicules(),
                allLocations,
                parking,
                distanceMatrix
            );
            case VROOM -> optimizerService.optimizeVroomRoutes(
                addressesGroup,
                strategie.getNbVehicules(),
                toSeconds(strategie.getMaxTempsParVehicule()),
                allLocations,
                parking
            );
        };
    }

    private List<Strategie> generatePossibleStrategies(Strategie baseStrategie) {
        List<Optimisation> optimisations = baseStrategie.getOptimisation() == null
            ? List.of(Optimisation.VROOM, Optimisation.GENETIC, Optimisation.GREEDY)
            : List.of(baseStrategie.getOptimisation());

        List<Separation> separations = baseStrategie.getSeparation() == null
            // DBSCAN is excluded from automatic strategy expansion: being density-based it ignores
            // the nbGroups hint and collapses to the same clustering regardless of the requested
            // vehicle count, producing redundant / missing solutions that make the results
            // non-deterministic. It can still be requested explicitly via the API.
            ? List.of(Separation.NONE, Separation.ANGULAR, Separation.KMEDOIDS)
            : List.of(baseStrategie.getSeparation());

        List<Strategie> strategies = new ArrayList<>();
        for (Separation separation : separations) {
            for (Optimisation optimisation : optimisations) {
                for (int nbVehicules = baseStrategie.getNbVehicules(); nbVehicules > 0; nbVehicules--) {
                    if (nbVehicules == 1 && separation != Separation.NONE) {
                        continue; // No need to test separation strategies with only 1 vehicle
                    }
                    if (separation == Separation.NONE && nbVehicules > 1 && optimisation != Optimisation.VROOM) {
                        continue; // No need to test non-VROOM optimizations without separation, as they will be very similar to the base strategy result
                    }
                    strategies.add(Strategie.builder()
                        .nbVehicules(nbVehicules)
                        .maxTempsParVehicule(baseStrategie.getMaxTempsParVehicule())
                        .separation(separation)
                        .optimisation(optimisation)
                        .build());
                }
            }
        }
        return strategies;
    }

    private List<List<Integer>> splitDestinations(Strategie strategie, List<Adresse> allLocations, double[][] distanceMatrix) {
        int deliveriesCount = Math.max(0, allLocations.size() - 1);
        if (deliveriesCount == 0) {
            return List.of(Collections.emptyList());
        }

        int nbGroups = strategie.getNbVehicules() == null ? 1 : Math.max(1, strategie.getNbVehicules());
        Separation separation = strategie.getSeparation() == null ? Separation.NONE : strategie.getSeparation();
        return new SeparateCommand().separate(separation, allLocations, distanceMatrix, nbGroups);
    }

    private DeliveryData buildDeliveryData(List<LivraisonEntity> livraisons) {
        Map<Integer, AdresseEntity> deliveryAddressesByIndex = new LinkedHashMap<>();
        Map<Integer, LivraisonEntity> deliveryByIndex = new LinkedHashMap<>();
        int index = 0;
        for (LivraisonEntity livraison : livraisons) {
            AdresseEntity adresse = livraison.getAdresseEntity();
            if (livraison.getId() == null || adresse == null) {
                continue;
            }
            deliveryAddressesByIndex.put(index, adresse);
            deliveryByIndex.put(index, livraison);
            index++;
        }

        if (deliveryAddressesByIndex.isEmpty()) {
            throw new BadRequestRestException("No livraisons with a valid address were found");
        }

        Set<Long> usedAddressIds = deliveryAddressesByIndex.values().stream()
            .map(AdresseEntity::getId)
            .collect(Collectors.toSet());

        ParkingEntity parkingEntity = parkingRepository.findTopByOrderByIdDesc()
            .orElseThrow(() -> new NotFoundElementRestException("No parking address configured"));

        AdresseEntity parkingAddress = parkingEntity.getAdresse();
        if (parkingAddress == null || parkingAddress.getId() == null) {
            throw new NotFoundElementRestException("Parking address is invalid or missing");
        }
        if (usedAddressIds.contains(parkingAddress.getId())) {
            throw new BadRequestRestException("Parking address must be distinct from delivery addresses");
        }

        return new DeliveryData(deliveryAddressesByIndex, deliveryByIndex, parkingAddress);
    }

    private Matrices toDomainMatrices(MatrixResponse response) {
        int size = response.distances.size();

        double[][] distances = new double[size][size];
        Duration[][] durations = new Duration[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                distances[i][j] = response.distances.get(i).get(j);
                durations[i][j] = Duration.ofSeconds(response.durations.get(i).get(j));
            }
        }

        return new Matrices(distances, durations);
    }

    private int toSeconds(Duration duration) {
        if (duration == null) {
            return 0;
        }
        return (int) Math.min(duration.getSeconds(), Integer.MAX_VALUE);
    }

    private void updateJourneeStatus(LocalDate date, StatutGeneration status) {
        journeeRepository.findById(date).ifPresent(journee -> {
            journee.setStatutGeneration(status);
            journeeRepository.save(journee);
            journeeStatusBroadcastService.broadcast(date, status);
        });
    }

    private List<List<Integer>> applyMaxTempsConstraint(
        List<List<Integer>> routes,
        Duration maxTemps,
        Duration[][] durationMatrix,
        Map<Integer, LivraisonEntity> deliveryByIndex,
        int parkingIndex
    ) {
        if (maxTemps == null || maxTemps.isNegative() || maxTemps.isZero()) {
            return routes;
        }
        return routes.stream()
            .map(route -> trimRouteToMaxTemps(new ArrayList<>(route), maxTemps, durationMatrix, deliveryByIndex, parkingIndex))
            .filter(route -> !route.isEmpty())
            .toList();
    }

    private List<Integer> trimRouteToMaxTemps(
        List<Integer> route,
        Duration maxTemps,
        Duration[][] durationMatrix,
        Map<Integer, LivraisonEntity> deliveryByIndex,
        int parkingIndex
    ) {
        while (!route.isEmpty() && computeRouteDuration(route, durationMatrix, deliveryByIndex, parkingIndex).compareTo(maxTemps) > 0) {
            Integer toRemove = findMostRecentDeliveryIndex(route, deliveryByIndex);
            route.remove(Integer.valueOf(toRemove));
        }
        return route;
    }

    private Duration computeRouteDuration(
        List<Integer> route,
        Duration[][] durationMatrix,
        Map<Integer, LivraisonEntity> deliveryByIndex,
        int parkingIndex
    ) {
        if (durationMatrix == null || durationMatrix.length == 0 || route.isEmpty()) {
            return Duration.ZERO;
        }
        Duration total = Duration.ZERO;
        int prev = parkingIndex;
        for (Integer idx : route) {
            total = total.plus(safeRouteDuration(durationMatrix, prev, idx));
            LivraisonEntity livraison = deliveryByIndex.get(idx);
            if (livraison != null && livraison.getTempsMontage() != null) {
                total = total.plus(livraison.getTempsMontage());
            }
            prev = idx;
        }
        total = total.plus(safeRouteDuration(durationMatrix, prev, parkingIndex));
        return total;
    }

    private Duration safeRouteDuration(Duration[][] matrix, int i, int j) {
        if (matrix == null || i < 0 || j < 0 || i >= matrix.length || j >= matrix[i].length || matrix[i][j] == null) {
            return Duration.ZERO;
        }
        return matrix[i][j];
    }

    private Integer findMostRecentDeliveryIndex(List<Integer> route, Map<Integer, LivraisonEntity> deliveryByIndex) {
        return route.stream()
            .max(Comparator.comparing(idx -> {
                LivraisonEntity l = deliveryByIndex.get(idx);
                return l != null && l.getDateCommande() != null ? l.getDateCommande() : LocalDate.MIN;
            }))
            .orElse(route.get(route.size() - 1));
    }

    private record OptimizationContext(
        JourneeEntity journee,
        List<LivraisonEntity> livraisons,
        Strategie strategie
    ) {
    }

    private record DeliveryData(
        Map<Integer, AdresseEntity> deliveryAddressesByIndex,
        Map<Integer, LivraisonEntity> deliveryByIndex,
        AdresseEntity parkingAddress
    ) {
    }

    private record SolutionRunResult(
        SolutionEntity solution,
        OptimizationResult result
    ) {
    }

    private record SharedOptimisationData(
        List<Adresse> allLocations,
        Adresse parking,
        double[][] distanceMatrix,
        Duration[][] durationMatrix
    ) {
    }
}
