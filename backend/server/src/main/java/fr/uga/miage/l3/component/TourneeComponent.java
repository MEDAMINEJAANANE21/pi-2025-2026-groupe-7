package fr.uga.miage.l3.component;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.TourneeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class TourneeComponent {
  private final TourneeRepository tourneeRepository;

  private static final double DEFAULT_FUEL_LITERS_PER_100_KM = 8.0;

  public List<TourneeEntity> saveTourneesForSolution(
    SolutionEntity solution,
    List<List<Integer>> routesIndexes,
    Map<Integer, LivraisonEntity> livraisonByIndex,
    double[][] distanceMatrix,
    Duration[][] durationMatrix,
    int parkingIndex
  ) {
    if (solution == null || routesIndexes == null) {
      return List.of();
    }

    List<TourneeEntity> tournees = new ArrayList<>();
    for (List<Integer> orderedIndexes : routesIndexes) {
      if (orderedIndexes == null || orderedIndexes.isEmpty()) {
        continue;
      }

      List<LivraisonEntity> orderedLivraisons = orderedIndexes.stream()
        .map(livraisonByIndex::get)
        .filter(Objects::nonNull)
        .toList();

      if (orderedLivraisons.isEmpty()) {
        continue;
      }

      if (orderedLivraisons.size() != orderedIndexes.size()) {
        log.warn("Some optimized job indexes cannot be mapped to a livraison for solution {}", solution.getId());
      }

      Statistic statistic = computeStatistic(orderedIndexes, orderedLivraisons, distanceMatrix, durationMatrix, parkingIndex);

      TourneeEntity tournee = TourneeEntity.builder()
        .solutionEntity(solution)
        .livraisonEntities(orderedLivraisons)
        .statistic(statistic)
        .build();
      tournees.add(tourneeRepository.save(tournee));
    }

    return tournees;
  }

  private Statistic computeStatistic(
    List<Integer> orderedIndexes,
    List<LivraisonEntity> orderedLivraisons,
    double[][] distanceMatrix,
    Duration[][] durationMatrix,
    int parkingIndex
  ) {
    long totalDistance = 0L;
    Duration totalTravelDuration = Duration.ZERO;

    int previousIndex = parkingIndex;
    for (Integer index : orderedIndexes) {
      if (index == null) {
        continue;
      }
      totalDistance += safeDistance(distanceMatrix, previousIndex, index);
      totalTravelDuration = totalTravelDuration.plus(safeDuration(durationMatrix, previousIndex, index));
      previousIndex = index;
    }

    if (!orderedIndexes.isEmpty()) {
      int lastIndex = orderedIndexes.get(orderedIndexes.size() - 1);
      totalDistance += safeDistance(distanceMatrix, lastIndex, parkingIndex);
      totalTravelDuration = totalTravelDuration.plus(safeDuration(durationMatrix, lastIndex, parkingIndex));
    }

    Duration totalMontageDuration = orderedLivraisons.stream()
      .map(LivraisonEntity::getTempsMontage)
      .filter(Objects::nonNull)
      .reduce(Duration.ZERO, Duration::plus);

    long fuelMilliliters = Math.round((totalDistance / 1000.0) * (DEFAULT_FUEL_LITERS_PER_100_KM * 10.0));

    return Statistic.builder()
      .distance(totalDistance)
      .tempsTrajet(totalTravelDuration)
      .tempsMontage(totalMontageDuration)
      .carburant(fuelMilliliters)
      .build();
  }

  private long safeDistance(double[][] matrix, int i, int j) {
    if (!isValidIndex(matrix, i) || !isValidIndex(matrix, j)) {
      return 0L;
    }
    return Math.round(matrix[i][j]);
  }

  private Duration safeDuration(Duration[][] matrix, int i, int j) {
    if (!isValidIndex(matrix, i) || !isValidIndex(matrix, j)) {
      return Duration.ZERO;
    }
    return matrix[i][j] == null ? Duration.ZERO : matrix[i][j];
  }

  private boolean isValidIndex(double[][] matrix, int index) {
    return matrix != null && index >= 0 && index < matrix.length;
  }

  private boolean isValidIndex(Duration[][] matrix, int index) {
    return matrix != null && index >= 0 && index < matrix.length;
  }
}
