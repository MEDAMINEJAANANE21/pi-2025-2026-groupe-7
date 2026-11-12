package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Véhicule de la flotte")
public record VehiculeDTO(
    Long id,
    String immatriculation,
    String modele,
    @Schema(description = "Charge utile maximale en kg, nulle = inconnue") Double capaciteKg,
    @Schema(description = "Consommation en L/100 km, nulle = consommation par défaut") Double consommationLitresAux100Km,
    boolean actif
) {
}
