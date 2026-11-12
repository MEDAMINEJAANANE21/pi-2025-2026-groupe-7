package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistiques synthétiques d'une solution pour une journée")
public record SolutionStatisticsResponseDTO(
    Long id,
    String date,
    StrategieResponseDTO strategie,
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
    String tempsRouteTotal,
    String tempsRouteMin,
    String tempsRouteMax,
    String tempsRouteMoyenne,
    String tempsMontageTotal,
    String tempsMontageMin,
    String tempsMontageMax,
    String tempsMontageMoyenne,
    String tempsTotal,
    String tempsTotalMin,
    String tempsTotalMax,
    String tempsTotalMoyenne,
    FinancesDTO finances
) {
}

