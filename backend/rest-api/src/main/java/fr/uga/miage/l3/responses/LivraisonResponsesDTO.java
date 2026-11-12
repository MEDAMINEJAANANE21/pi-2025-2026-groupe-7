package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Livraison à effectuer chez un client")
public record LivraisonResponsesDTO(
    Long id,
    String reference,
    double poids,
    AdresseDTO adresse,
    boolean livree,
    LocalDate dateCommande,
    @Schema(description = "Durée ISO-8601, ex. PT15M") String tempsMontage,
    @Schema(description = "Date de remise au client, nulle tant que la livraison n'est pas faite") LocalDateTime dateLivraison
) {
}
