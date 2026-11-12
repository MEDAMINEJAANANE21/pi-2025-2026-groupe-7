package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.services.core.separation.kmedoids.KMedoidsCommand;
import fr.uga.miage.l3.services.core.separation.AngularSeparationStrategy;
import fr.uga.miage.l3.services.core.separation.KMedoidsSeparationStrategy;
import fr.uga.miage.l3.services.core.separation.SeparationStrategy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SeparateCommand {

    private final Map<Separation, SeparationStrategy> strategies;

    public SeparateCommand() {
        this.strategies = new EnumMap<>(Separation.class);
        strategies.put(Separation.ANGULAR, new AngularSeparationStrategy());
        strategies.put(Separation.KMEDOIDS, new KMedoidsSeparationStrategy());
    }

    public List<List<Integer>> separate(Separation separation, List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups) {
        int deliveriesCount = Math.max(0, allLocations.size() - 1);
        if (deliveriesCount == 0) {
            return List.of(Collections.emptyList());
        }

        if (separation == null || separation == Separation.NONE) {
            return List.of(IntStream.range(0, deliveriesCount).boxed().toList());
        }

        SeparationStrategy strategy = strategies.get(separation);
        if (strategy == null) {
            return List.of(IntStream.range(0, deliveriesCount).boxed().toList());
        }

        List<List<Integer>> clusters = strategy.separate(allLocations, distanceMatrix, Math.max(1, nbGroups));
        if (clusters == null || clusters.isEmpty()) {
            return List.of(IntStream.range(0, deliveriesCount).boxed().toList());
        }
        return clusters;
    }
}
