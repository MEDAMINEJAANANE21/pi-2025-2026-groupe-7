package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Matrices approchées sans service externe : distance à vol d'oiseau majorée d'un facteur
 * de détour, et vitesse moyenne urbaine. Assez juste pour comparer des stratégies entre elles.
 */
public final class EstimatedMatrix {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    /** Les routes ne sont pas droites : en ville on parcourt environ 30 % de plus qu'à vol d'oiseau. */
    private static final double DETOUR_FACTOR = 1.3;
    private static final double AVERAGE_SPEED_METERS_PER_SECOND = 30_000 / 3600.0;

    private EstimatedMatrix() {
    }

    public static MatrixResponse of(List<Adresse> addresses) {
        List<List<Integer>> distances = new ArrayList<>();
        List<List<Integer>> durations = new ArrayList<>();
        for (Adresse from : addresses) {
            List<Integer> distanceRow = new ArrayList<>();
            List<Integer> durationRow = new ArrayList<>();
            for (Adresse to : addresses) {
                double meters = roadMeters(from, to);
                distanceRow.add((int) Math.round(meters));
                durationRow.add((int) Math.round(meters / AVERAGE_SPEED_METERS_PER_SECOND));
            }
            distances.add(distanceRow);
            durations.add(durationRow);
        }
        MatrixResponse matrix = new MatrixResponse();
        matrix.distances = distances;
        matrix.durations = durations;
        return matrix;
    }

    static double roadMeters(Adresse from, Adresse to) {
        double lat1 = Math.toRadians(from.coords.lat);
        double lat2 = Math.toRadians(to.coords.lat);
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(to.coords.lng - from.coords.lng);
        double h = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h)) * DETOUR_FACTOR;
    }
}
