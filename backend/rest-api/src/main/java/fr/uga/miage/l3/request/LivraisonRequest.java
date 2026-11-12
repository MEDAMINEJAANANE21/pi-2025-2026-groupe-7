package fr.uga.miage.l3.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Schema(description = "Création ou modification d'une livraison")
public record LivraisonRequest(
    @NotNull @Valid AdresseRequest adresse,
    @NotNull @PositiveOrZero @Schema(description = "Poids en kg") Double poids,
    @Schema(description = "Durée ISO-8601, ex. PT15M ; vide = pas de montage") String tempsMontage,
    @Schema(description = "Date de commande ; vide = aujourd'hui") LocalDate dateCommande
) {
}
