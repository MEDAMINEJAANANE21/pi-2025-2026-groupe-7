package fr.uga.miage.l3.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Adresse géolocalisée d'un client")
public record AdresseRequest(
    String numero,
    @NotBlank String rue,
    @NotBlank String codePostal,
    @NotBlank String ville,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng
) {
}
