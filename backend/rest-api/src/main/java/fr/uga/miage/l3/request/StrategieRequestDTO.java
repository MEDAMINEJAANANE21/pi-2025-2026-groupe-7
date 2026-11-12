package fr.uga.miage.l3.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(description = "Parametres d'optimisation d'une journee")
public record StrategieRequestDTO(
    Integer nbVehicules,
    String maxTempsParVehicule,
    @NotEmpty
    @Schema(description = "Identifiants des livraisons à optimiser.", required = true)
    Set<Long> livraisonIds
) {
}
