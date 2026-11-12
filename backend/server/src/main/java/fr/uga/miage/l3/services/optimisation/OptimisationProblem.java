package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.entities.LivraisonEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Données partagées par toutes les stratégies d'une même optimisation.
 *
 * <p>Convention d'indexation : les livraisons occupent les indices {@code 0..n-1},
 * le dépôt est toujours le dernier point. Les matrices suivent le même ordre.
 */
public record OptimisationProblem(
    List<Adresse> locations,
    double[][] distances,
    Duration[][] durations,
    Map<Integer, LivraisonEntity> livraisonByIndex
) {

    public int parkingIndex() {
        return locations.size() - 1;
    }

    public Adresse parking() {
        return locations.get(parkingIndex());
    }

    public int deliveriesCount() {
        return parkingIndex();
    }

    public LivraisonEntity livraisonAt(int index) {
        return livraisonByIndex.get(index);
    }
}
