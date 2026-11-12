package fr.uga.miage.l3.fixtures;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.Coords;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit des problèmes d'optimisation sur un plan, sans base ni appel réseau.
 *
 * <p>Chaque point est donné en (x, y) kilomètres ; le dernier point est le dépôt.
 * Distance = distance euclidienne en mètres, durée = 1 seconde par 10 mètres.
 */
public final class ProblemFixtures {

    private ProblemFixtures() {
    }

    public static OptimisationProblem onPlane(double[]... pointsWithParkingLast) {
        int size = pointsWithParkingLast.length;
        List<Adresse> locations = new ArrayList<>();
        double[][] distances = new double[size][size];
        Duration[][] durations = new Duration[size][size];
        for (int i = 0; i < size; i++) {
            double[] point = pointsWithParkingLast[i];
            // On place les points autour de Grenoble pour garder des coordonnées réalistes.
            locations.add(new Adresse(new Coords(45.0 + point[1] / 111.0, 5.0 + point[0] / 78.0), "Point " + i, "38000", "Grenoble"));
            for (int j = 0; j < size; j++) {
                double meters = Math.hypot(point[0] - pointsWithParkingLast[j][0], point[1] - pointsWithParkingLast[j][1]) * 1000;
                distances[i][j] = meters;
                durations[i][j] = Duration.ofSeconds(Math.round(meters / 10));
            }
        }
        Map<Integer, LivraisonEntity> livraisons = new HashMap<>();
        for (int i = 0; i < size - 1; i++) {
            livraisons.put(i, LivraisonEntity.builder()
                .id((long) i + 1)
                .poids(10.0)
                .tempsMontage(Duration.ZERO)
                .dateCommande(LocalDate.of(2026, 1, 1).plusDays(i))
                .build());
        }
        return new OptimisationProblem(locations, distances, durations, livraisons);
    }
}
