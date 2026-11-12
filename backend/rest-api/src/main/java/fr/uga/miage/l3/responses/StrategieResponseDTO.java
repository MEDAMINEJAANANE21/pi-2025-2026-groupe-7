package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Strategie appliquee a la journee")
public record StrategieResponseDTO(
    String separation,
    String optimisation,
    Integer nbVehicules,
    String maxTempsParVehicule
) {
}
