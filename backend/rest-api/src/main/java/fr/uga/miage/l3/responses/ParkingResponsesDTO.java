package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représente d'un parking")
public record ParkingResponsesDTO (
    AdresseDTO adresse
) {
}
