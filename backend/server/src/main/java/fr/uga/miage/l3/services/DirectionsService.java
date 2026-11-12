package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.core.DirectionsResponse;
import fr.uga.miage.l3.models.core.Coords;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import fr.uga.miage.l3.services.core.OrsMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectionsService {
    private final OpenRouteServiceClient orsClient;
    private final OrsMode orsMode;

    private static final int COORDINATES_LIMIT_PER_REQUEST = 50;

    /**
     * Get directions for a route with chunking support
     * Equivalent to frontend AlgosService.getDirections()
     *
     * @param coordinates List of [lng, lat] coordinate pairs
     * @return List of coordinate points for the complete route
     */
    public List<Coords> getDirections(List<double[]> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            log.warn("Invalid coordinates provided for directions: {}",
                coordinates == null ? "null" : coordinates.size());
            return new ArrayList<>();
        }
        if (orsMode.estimation()) {
            // Sans service d'itinéraire, on relie les arrêts en ligne droite.
            return coordinates.stream().map(c -> Coords.builder().lng(c[0]).lat(c[1]).build()).toList();
        }

        List<Coords> allRoutePoints = new ArrayList<>();

        // If coordinates fit within limit, make single request
        if (coordinates.size() <= COORDINATES_LIMIT_PER_REQUEST) {
            List<Coords> routePoints = getSingleDirectionRoute(coordinates);
            allRoutePoints.addAll(routePoints);
        } else {
            // Chunk the coordinates and make multiple requests
            for (int i = 0; i < coordinates.size() - 1; i += COORDINATES_LIMIT_PER_REQUEST - 1) {
                int endIndex = Math.min(i + COORDINATES_LIMIT_PER_REQUEST, coordinates.size());
                List<double[]> chunk = coordinates.subList(i, endIndex);

                log.debug("Processing directions chunk: {} coordinates (indices {}-{})",
                    chunk.size(), i, endIndex - 1);

                List<Coords> chunkRoutePoints = getSingleDirectionRoute(chunk);

                // Avoid duplicate points at chunk boundaries
                if (i > 0 && !chunkRoutePoints.isEmpty()) {
                    chunkRoutePoints.remove(0); // Remove first point to avoid duplication
                }

                allRoutePoints.addAll(chunkRoutePoints);
            }
        }

        log.info("Generated route with {} coordinate points from {} input coordinates",
            allRoutePoints.size(), coordinates.size());

        return allRoutePoints;
    }

    /**
     * Get directions for a single chunk of coordinates (≤50 points)
     */
    private List<Coords> getSingleDirectionRoute(List<double[]> coordinates) {
        try {
            DirectionsResponse response = orsClient.getDirections(coordinates);
            return extractCoordinatesFromResponse(response);
        } catch (Exception e) {
            log.error("Failed to get directions for {} coordinates: {}",
                coordinates.size(), e.getMessage());
            throw e;
        }
    }

    /**
     * Extract coordinate points from ORS directions response
     * Mimics frontend coordinate extraction from GeoJSON
     */
    private List<Coords> extractCoordinatesFromResponse(DirectionsResponse response) {
        List<Coords> routePoints = new ArrayList<>();

        if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
            log.warn("Empty or invalid directions response");
            return routePoints;
        }

        // Extract coordinates from first feature (should be the route LineString)
        var firstFeature = response.getFeatures().get(0);
        if (firstFeature.getGeometry() != null && firstFeature.getGeometry().getCoordinates() != null) {
            for (double[] coordinate : firstFeature.getGeometry().getCoordinates()) {
                if (coordinate.length >= 2) {
                    // GeoJSON format: [longitude, latitude, elevation?]
                    Coords point = Coords.builder()
                        .lng(coordinate[0])
                        .lat(coordinate[1])
                        .build();
                    routePoints.add(point);
                }
            }
        }

        log.debug("Extracted {} coordinate points from directions response", routePoints.size());
        return routePoints;
    }

    /**
     * Convenience method that accepts Coords objects instead of double arrays
     */
    public List<Coords> getDirectionsFromCoords(List<Coords> coordsList) {
        List<double[]> coordinates = coordsList.stream()
            .map(coords -> new double[]{coords.getLng(), coords.getLat()})
            .toList();
        return getDirections(coordinates);
    }
}
