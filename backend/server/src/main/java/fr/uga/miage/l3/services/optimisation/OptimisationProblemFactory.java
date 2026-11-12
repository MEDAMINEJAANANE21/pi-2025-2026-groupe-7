package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.services.core.DistanceMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pattern utilisé : Factory. Prépare les données d'une optimisation en deux temps :
 * une validation rapide (synchrone) puis le calcul coûteux des matrices (asynchrone).
 */
@Component
@RequiredArgsConstructor
public class OptimisationProblemFactory {

    private final LivraisonRepository livraisonRepository;
    private final ParkingRepository parkingRepository;
    private final AdresseMapper adresseMapper;
    private final DistanceMatrixService distanceMatrixService;

    public DeliverySet loadDeliveries(Set<Long> livraisonIds) {
        List<LivraisonEntity> livraisons = livraisonRepository.findAllById(livraisonIds).stream()
            .filter(livraison -> !livraison.isLivree())
            .sorted(Comparator.comparing(LivraisonEntity::getId))
            .toList();
        if (livraisons.isEmpty()) {
            throw new InvalidRequestException("Aucune livraison à planifier parmi celles demandées");
        }

        AdresseEntity parking = parkingRepository.findTopByOrderByIdDesc()
            .map(ParkingEntity::getAdresse)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun dépôt n'est configuré"));
        boolean deliveryAtParking = livraisons.stream()
            .anyMatch(livraison -> Objects.equals(livraison.getAdresseEntity().getId(), parking.getId()));
        if (deliveryAtParking) {
            throw new InvalidRequestException("Une livraison ne peut pas avoir l'adresse du dépôt");
        }
        return new DeliverySet(livraisons, parking);
    }

    public OptimisationProblem create(DeliverySet deliveries) {
        List<AdresseEntity> addresses = new ArrayList<>();
        deliveries.livraisons().forEach(livraison -> addresses.add(livraison.getAdresseEntity()));
        addresses.add(deliveries.parking());

        List<Adresse> locations = adresseMapper.toCore(addresses);
        MatrixResponse matrices = distanceMatrixService.getMatrices(locations);

        Map<Integer, LivraisonEntity> livraisonByIndex = new HashMap<>();
        for (int i = 0; i < deliveries.livraisons().size(); i++) {
            livraisonByIndex.put(i, deliveries.livraisons().get(i));
        }
        return new OptimisationProblem(
            locations,
            toDistances(matrices.distances),
            toDurations(matrices.durations),
            livraisonByIndex
        );
    }

    private static double[][] toDistances(List<List<Integer>> meters) {
        int size = meters.size();
        double[][] distances = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                distances[i][j] = meters.get(i).get(j);
            }
        }
        return distances;
    }

    private static Duration[][] toDurations(List<List<Integer>> seconds) {
        int size = seconds.size();
        Duration[][] durations = new Duration[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                durations[i][j] = Duration.ofSeconds(seconds.get(i).get(j));
            }
        }
        return durations;
    }
}
