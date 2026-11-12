package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Représente d'une tournee")
public record TourneeResponsesDTO (
    Long id,
    List<LivraisonResponsesDTO> livraisons,
    TourneeStatisticsResponseDTO statistiques
) {
}
