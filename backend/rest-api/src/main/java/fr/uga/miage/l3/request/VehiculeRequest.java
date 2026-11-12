package fr.uga.miage.l3.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VehiculeRequest(
    @NotBlank @Size(max = 20) String immatriculation,
    @Size(max = 80) String modele,
    @Positive @DecimalMax(value = "44000", message = "doit rester sous 44 t") Double capaciteKg,
    @Positive @DecimalMax(value = "60", message = "doit rester sous 60 L/100 km") Double consommationLitresAux100Km,
    Boolean actif
) {
}