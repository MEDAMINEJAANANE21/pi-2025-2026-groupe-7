package fr.uga.miage.l3.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ChauffeurRequest(
    @NotBlank String prenom,
    @NotBlank String nom,
    String telephone,
    @Positive Double tauxHoraire,
    Boolean actif
) {
}
