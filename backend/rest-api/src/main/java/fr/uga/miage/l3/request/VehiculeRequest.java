package fr.uga.miage.l3.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record VehiculeRequest(
    @NotBlank String immatriculation,
    String modele,
    @Positive Double capaciteKg,
    @Positive Double consommationLitresAux100Km,
    Boolean actif
) {
}
