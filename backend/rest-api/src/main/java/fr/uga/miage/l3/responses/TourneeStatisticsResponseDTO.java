package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistiques synthétiques d'une tournée")
public record TourneeStatisticsResponseDTO(
    Long carburant,
    Long distance,
    String tempsTrajet,
    String tempsMontage
) {
}

