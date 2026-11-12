package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Livraisons associées à une solution, séparées en deux listes")
public record SolutionLivraisonsResponseDTO(
        @Schema(description = "Livraisons assignées à au moins une tournée de la solution")
        List<LivraisonResponsesDTO> dansSolution,
        @Schema(description = "Livraisons non livrées et non assignées à cette solution")
        List<LivraisonResponsesDTO> horsSolution
) {
}
