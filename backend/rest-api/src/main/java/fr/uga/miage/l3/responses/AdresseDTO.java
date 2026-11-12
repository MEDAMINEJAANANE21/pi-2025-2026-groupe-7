package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représente une adresse")
public record AdresseDTO (
    CoordonneeDTO coordonnees,
    String rue,
    String numero,
    String codePostal,
    String ville
) {
}
