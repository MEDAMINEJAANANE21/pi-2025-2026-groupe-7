package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Tournée d'un véhicule : livraisons dans l'ordre de passage")
public record TourneeResponsesDTO(
    Long id,
    List<LivraisonResponsesDTO> livraisons,
    TourneeStatisticsResponseDTO statistiques,
    FinancesDTO finances,
    double poidsTotal,
    ChauffeurDTO chauffeur,
    VehiculeDTO vehicule
) {
}
