package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résumé d'une journée pour le calendrier")
public record JourneeResumeDTO(
    String date,
    String statutGeneration,
    int nbSolutions,
    Long solutionChoisieId
) {
}
