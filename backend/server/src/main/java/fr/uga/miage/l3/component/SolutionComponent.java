package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.domain.SolutionStatisticsData;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.SolutionLivraisonsResponseDTO;
import fr.uga.miage.l3.mappers.LivraisonMapper;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SolutionComponent {
    private final SolutionRepository solutionRepository;
    private final LivraisonRepository livraisonRepository;
    private final LivraisonMapper livraisonMapper;
    private final TourneeComponent tourneeComponent;
    private final OpenRouteServiceClient openRouteServiceClient;
    private final AdresseMapper adresseMapper;
    private final ParkingRepository parkingRepository;
    private final TourneeRepository tourneeRepository;

    public Set<SolutionEntity> getSolutionsByDate(LocalDate date) {
        return solutionRepository.getAllByDate(date);
    }

    public SolutionEntity getSolutionsById(Long id) {
        return solutionRepository.getById(id);
    }

    public SolutionEntity getSolutionsByIdWithTournees(Long id) {
        SolutionEntity entity = solutionRepository.findById(id)
            .orElseThrow(() -> new NotFoundElementRestException("Solution with id [" + id + "] not found"));
        entity.getTourneeEntities().forEach(
            tournee -> tournee.getLivraisonEntities().forEach(
                LivraisonEntity::getAdresseEntity
            )
        );
        return entity;
    }

    public List<SolutionStatisticsData> getStatisticsByDate(LocalDate date) {
        return solutionRepository.getAllByDate(date)
            .stream()
            .map(this::buildStatistics)
            .toList();
    }

    private SolutionStatisticsData buildStatistics(SolutionEntity solution) {
        var tournees = solution.getTourneeEntities();
        int tourCount = tournees == null ? 0 : tournees.size();

        // Vehicules: prefer configured nbVehicules, else fallback to tour count
        long nbVehicules = solution.getStrategie() != null && solution.getStrategie().getNbVehicules() != null
            ? solution.getStrategie().getNbVehicules()
            : tourCount;

        long nbLivraisonsTotal = 0L;
        Integer nbLivraisonsMin = null;
        Integer nbLivraisonsMax = null;
        double nbLivraisonsSum = 0d;

        long carburantTotal = 0L;
        Long carburantMin = null;
        Long carburantMax = null;
        double carburantSum = 0d;

        long distanceTotal = 0L;
        Long distanceMin = null;
        Long distanceMax = null;
        double distanceSum = 0d;

        double poidsTotal = 0d;
        Double poidsMin = null;
        Double poidsMax = null;
        double poidsSum = 0d;

        Duration routeTotal = Duration.ZERO;
        Duration routeMin = null;
        Duration routeMax = null;

        Duration montageTotal = Duration.ZERO;
        Duration montageMin = null;
        Duration montageMax = null;

        Duration tempsTotalMin = null;
        Duration tempsTotalMax = null;

        if (tournees != null) {
            for (TourneeEntity tournee : tournees) {
                int livCount = tournee.getLivraisonEntities() == null ? 0 : tournee.getLivraisonEntities().size();
                nbLivraisonsTotal += livCount;
                nbLivraisonsMin = min(nbLivraisonsMin, livCount);
                nbLivraisonsMax = max(nbLivraisonsMax, livCount);
                nbLivraisonsSum += livCount;

                Long carburant = tournee.getStatistic() != null ? tournee.getStatistic().getCarburant() : null;
                carburantTotal += safeLong(carburant);
                carburantMin = min(carburantMin, carburant);
                carburantMax = max(carburantMax, carburant);
                carburantSum += safeLong(carburant);

                Long distance = tournee.getStatistic() != null ? tournee.getStatistic().getDistance() : null;
                distanceTotal += safeLong(distance);
                distanceMin = min(distanceMin, distance);
                distanceMax = max(distanceMax, distance);
                distanceSum += safeLong(distance);

                double tourneePoids = tournee.getLivraisonEntities() == null ? 0d : tournee.getLivraisonEntities().stream()
                                                                                    .map(l -> l.getPoids() == null ? 0d : l.getPoids())
                                                                                    .reduce(0d, Double::sum);
                poidsTotal += tourneePoids;
                poidsMin = min(poidsMin, tourneePoids);
                poidsMax = max(poidsMax, tourneePoids);
                poidsSum += tourneePoids;

                Duration route = tournee.getStatistic() == null ? Duration.ZERO : safeDuration(tournee.getStatistic().getTempsTrajet());
                Duration montage = tournee.getStatistic() == null ? Duration.ZERO : safeDuration(tournee.getStatistic().getTempsMontage());

                routeTotal = routeTotal.plus(route);
                routeMin = min(routeMin, route);
                routeMax = max(routeMax, route);

                montageTotal = montageTotal.plus(montage);
                montageMin = min(montageMin, montage);
                montageMax = max(montageMax, montage);

                Duration tempsVehicule = route.plus(montage);
                tempsTotalMin = min(tempsTotalMin, tempsVehicule);
                tempsTotalMax = max(tempsTotalMax, tempsVehicule);
            }
        }

        double tourCountDouble = tourCount == 0 ? 1d : (double) tourCount;

        return new SolutionStatisticsData(
            solution.getId(),
            solution.getDate(),
            solution.getStrategie(),
            nbVehicules,
            nbLivraisonsTotal,
            nbLivraisonsMin,
            nbLivraisonsMax,
            tourCount == 0 ? 0d : nbLivraisonsSum / tourCountDouble,
            carburantTotal,
            carburantMin,
            carburantMax,
            tourCount == 0 ? null : carburantSum / tourCountDouble,
            distanceTotal,
            distanceMin,
            distanceMax,
            tourCount == 0 ? null : distanceSum / tourCountDouble,
            poidsTotal,
            poidsMin,
            poidsMax,
            tourCount == 0 ? null : poidsSum / tourCountDouble,
            routeTotal,
            routeMin,
            routeMax,
            montageTotal,
            montageMin,
            montageMax,
            tempsTotalMin,
            tempsTotalMax
        );
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private Duration safeDuration(Duration value) {
        return value == null ? Duration.ZERO : value;
    }

    private Integer min(Integer current, int value) {
        return current == null ? value : Math.min(current, value);
    }

    private Long min(Long current, Long value) {
        if (value == null) return current;
        return current == null ? value : Math.min(current, value);
    }

    private Double min(Double current, double value) {
        return current == null ? value : Math.min(current, value);
    }

    private Duration min(Duration current, Duration value) {
        if (value == null) return current;
        return current == null ? value : (value.compareTo(current) < 0 ? value : current);
    }

    private Integer max(Integer current, int value) {
        return current == null ? value : Math.max(current, value);
    }

    private Long max(Long current, Long value) {
        if (value == null) return current;
        return current == null ? value : Math.max(current, value);
    }

    private Double max(Double current, double value) {
        return current == null ? value : Math.max(current, value);
    }

    private Duration max(Duration current, Duration value) {
        if (value == null) return current;
        return current == null ? value : (value.compareTo(current) > 0 ? value : current);
    }


    public SolutionLivraisonsResponseDTO getSolutionLivraisons(Long solutionId) {
        SolutionEntity solution = solutionRepository.findById(solutionId)
            .orElseThrow(() -> new NotFoundElementRestException("Solution with id [" + solutionId + "] not found"));

        // Collect all livraisons from all tournées of this solution (preserving order, deduplicating)
        Set<LivraisonEntity> dansSolutionSet = new LinkedHashSet<>();
        if (solution.getTourneeEntities() != null) {
            for (TourneeEntity tournee : solution.getTourneeEntities()) {
                if (tournee.getLivraisonEntities() != null) {
                    dansSolutionSet.addAll(tournee.getLivraisonEntities());
                }
            }
        }

        Set<Long> dansSolutionIds = dansSolutionSet.stream()
            .map(LivraisonEntity::getId)
            .collect(Collectors.toSet());

        // All non-delivered livraisons NOT in the solution
        List<LivraisonEntity> nonLivrees = livraisonRepository.findAllByLivree(false);
        List<LivraisonEntity> horsSolution = nonLivrees.stream()
            .filter(l -> !dansSolutionIds.contains(l.getId()))
            .collect(Collectors.toList());

        return new SolutionLivraisonsResponseDTO(
            livraisonMapper.toDTOList(new ArrayList<>(dansSolutionSet)),
            livraisonMapper.toDTOList(horsSolution)
        );
    }

    @Transactional
    public void recalculateTourneeStats(SolutionEntity solution) {
        Optional<ParkingEntity> parkingOpt = parkingRepository.findTopByOrderByIdDesc();

        for (TourneeEntity tournee : solution.getTourneeEntities()) {
            List<LivraisonEntity> livraisons = tournee.getLivraisonEntities();
            if (livraisons == null || livraisons.isEmpty()) continue;

            List<AdresseEntity> adresseEntities = new ArrayList<>();
            parkingOpt.ifPresent(p -> adresseEntities.add(p.getAdresse()));

            for (LivraisonEntity liv : livraisons) {
                if (liv.getAdresseEntity() != null && liv.getAdresseEntity().getCoords() != null) {
                    adresseEntities.add(liv.getAdresseEntity());
                }
            }

            if (adresseEntities.size() < 2) continue;

            List<Adresse> coreAddresses = adresseMapper.toCore(adresseEntities);
            int n = coreAddresses.size();
            int[] all = IntStream.range(0, n).toArray();

            MatrixResponse matrix = openRouteServiceClient.getMatrix(coreAddresses, all, all);
            if (matrix == null || matrix.distances == null || matrix.durations == null) continue;

            long totalDistance = 0L;
            long totalSeconds = 0L;
            for (int i = 0; i < n - 1; i++) {
                totalDistance += getMatrixValue(matrix.distances, i, i + 1);
                totalSeconds += getMatrixValue(matrix.durations, i, i + 1);
            }
            totalDistance += getMatrixValue(matrix.distances, n - 1, 0);
            totalSeconds += getMatrixValue(matrix.durations, n - 1, 0);

            Duration totalTravelDuration = Duration.ofSeconds(totalSeconds);
            Duration totalMontageDuration = livraisons.stream()
                .map(LivraisonEntity::getTempsMontage)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);

            long fuelMilliliters = Math.round((totalDistance / 1000.0) * 80.0);

            tournee.setStatistic(Statistic.builder()
                .distance(totalDistance)
                .tempsTrajet(totalTravelDuration)
                .tempsMontage(totalMontageDuration)
                .carburant(fuelMilliliters)
                .build());
            tourneeRepository.save(tournee);
        }
    }

    private int getMatrixValue(List<List<Integer>> matrix, int i, int j) {
        if (matrix == null || i >= matrix.size()) return 0;
        List<Integer> row = matrix.get(i);
        if (row == null || j >= row.size()) return 0;
        Integer v = row.get(j);
        return v == null ? 0 : v;
    }

    public SolutionEntity save(SolutionEntity solutionEntity) {
        return solutionRepository.save(solutionEntity);
    }

    /**
     * Save the solution and its tournees in a single transaction. If any error occurs while
     * saving the tournees, the whole transaction will be rolled back and the solution won't be persisted.
     */
    @Transactional
    public SolutionEntity saveSolutionWithTournees(
        SolutionEntity solution,
        List<List<Integer>> routesIndexes,
        Map<Integer, LivraisonEntity> livraisonByIndex,
        double[][] distanceMatrix,
        Duration[][] durationMatrix,
        int parkingIndex
    ) {
        SolutionEntity saved = solutionRepository.save(solution);
        // tourneeComponent will use the same transaction / persistence context
        tourneeComponent.saveTourneesForSolution(
            saved,
            routesIndexes,
            livraisonByIndex,
            distanceMatrix,
            durationMatrix,
            parkingIndex
        );
        return saved;
    }
}
