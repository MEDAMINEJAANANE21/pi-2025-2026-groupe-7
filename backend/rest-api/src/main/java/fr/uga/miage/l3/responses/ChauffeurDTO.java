package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chauffeur de l'équipe")
public record ChauffeurDTO(
    Long id,
    String prenom,
    String nom,
    String telephone,
    @Schema(description = "Coût horaire chargé en euros, nul = taux par défaut") Double tauxHoraire,
    boolean actif,
    @Schema(description = "Adresse de la photo, nulle s'il n'y en a pas") String photoUrl
) {
}
