package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.core.Adresse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Pattern utilisé : Null Object. Un seul groupe : le moteur d'itinéraire répartit lui-même.
 */
@Component
public class NoSeparationStrategy implements SeparationStrategy {

    @Override
    public Separation type() {
        return Separation.NONE;
    }

    @Override
    public List<List<Integer>> separate(List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups) {
        int deliveriesCount = Math.max(0, allLocations.size() - 1);
        return List.of(IntStream.range(0, deliveriesCount).boxed().toList());
    }
}
