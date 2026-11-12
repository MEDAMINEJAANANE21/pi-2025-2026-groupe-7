package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.services.core.separation.kmedoids.KMedoidsResult;
import fr.uga.miage.l3.services.core.separation.kmedoids.KMedoidsCommand;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Regroupe les livraisons proches autour de « medoids » (des livraisons réelles servant de centre).
 */
@Component
public class KMedoidsSeparationStrategy implements SeparationStrategy {

    @Override
    public Separation type() {
        return Separation.KMEDOIDS;
    }

    private static final int KMEDOIDS_MAX_ITERATIONS = 50;

    @Override
    public List<List<Integer>> separate(List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups) {
        if (allLocations == null || allLocations.isEmpty() || nbGroups <= 0) {
            return new ArrayList<>();
        }

        int deliveriesCount = Math.max(0, allLocations.size() - 1);
        if (deliveriesCount == 0) {
            return List.of(Collections.emptyList());
        }

        int parkingIndex = allLocations.size() - 1;

        List<Integer> newToOldIndexes = new ArrayList<>();
        newToOldIndexes.add(parkingIndex);
        for (int i = 0; i < deliveriesCount; i++) {
            newToOldIndexes.add(i);
        }

        double[][] reorderedMatrix = reorderMatrix(distanceMatrix, newToOldIndexes);
        List<Adresse> reorderedLocations = newToOldIndexes.stream()
            .map(allLocations::get)
            .toList();

        int requestedGroups = Math.min(nbGroups, deliveriesCount);
        KMedoidsResult result = new KMedoidsCommand().kMedoids(reorderedLocations, reorderedMatrix, requestedGroups, KMEDOIDS_MAX_ITERATIONS);

        List<List<Integer>> clusters = new ArrayList<>();
        for (List<Integer> cluster : result.getClusters().values()) {
            List<Integer> deliveries = cluster.stream()
                .map(newToOldIndexes::get)
                .filter(idx -> idx != parkingIndex)
                .toList();
            if (!deliveries.isEmpty()) {
                clusters.add(deliveries);
            }
        }

        if (clusters.isEmpty()) {
            return List.of(IntStream.range(0, deliveriesCount).boxed().toList());
        }

        return clusters;
    }

    private double[][] reorderMatrix(double[][] matrix, List<Integer> newToOldIndexes) {
        int size = newToOldIndexes.size();
        double[][] reordered = new double[size][size];
        for (int i = 0; i < size; i++) {
            int oldI = newToOldIndexes.get(i);
            for (int j = 0; j < size; j++) {
                int oldJ = newToOldIndexes.get(j);
                reordered[i][j] = matrix[oldI][oldJ];
            }
        }
        return reordered;
    }
}

