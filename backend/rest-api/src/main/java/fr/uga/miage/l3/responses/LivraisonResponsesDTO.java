package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Représente d'une livraison")
public record LivraisonResponsesDTO (
    Long id,
    double poids,
    AdresseDTO adresse,
    boolean livree,
    LocalDate dateCommande,
    String tempsMontage
) {
}
