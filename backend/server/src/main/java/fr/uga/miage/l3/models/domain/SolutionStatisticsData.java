package fr.uga.miage.l3.models.domain;

import fr.uga.miage.l3.models.entities.Strategie;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Aggregated statistics for a solution with durations already mapped from numeric seconds.
 */
public record SolutionStatisticsData(
    Long id,
    LocalDate date,
    Strategie strategie,
    Long nbVehicules,
    Long nbLivraisonsTotal,
    Integer nbLivraisonsMin,
    Integer nbLivraisonsMax,
    Double nbLivraisonsMoyenne,
    Long carburantTotal,
    Long carburantMin,
    Long carburantMax,
    Double carburantMoyenne,
    Long distanceTotal,
    Long distanceMin,
    Long distanceMax,
    Double distanceMoyenne,
    Double poidsTotal,
    Double poidsMin,
    Double poidsMax,
    Double poidsMoyenne,
    Duration tempsRouteTotal,
    Duration tempsRouteMin,
    Duration tempsRouteMax,
    Duration tempsMontageTotal,
    Duration tempsMontageMin,
    Duration tempsMontageMax,
    Duration tempsTotalMin,
    Duration tempsTotalMax
) {
}

