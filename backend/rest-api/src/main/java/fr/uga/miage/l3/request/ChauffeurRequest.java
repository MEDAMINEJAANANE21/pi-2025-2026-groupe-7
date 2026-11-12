package fr.uga.miage.l3.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChauffeurRequest(
    @NotBlank @Size(max = 80) String prenom,
    @NotBlank @Size(max = 80) String nom,
    @Size(max = 30) String telephone,
    @Positive @DecimalMax(value = "200", message = "doit rester sous 200 €/h") Double tauxHoraire,
    Boolean actif
) {
}