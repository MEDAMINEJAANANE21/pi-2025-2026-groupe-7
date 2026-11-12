package fr.uga.miage.l3.services.optimisation.routing;

import java.time.Duration;

/**
 * Contraintes transmises au moteur d'itinéraire. Seul VROOM sait en tenir compte pendant le calcul.
 */
public record RouteConstraints(Duration maxTravelTime) {

    public long maxTravelSeconds() {
        return maxTravelTime == null ? 0 : maxTravelTime.toSeconds();
    }
}
