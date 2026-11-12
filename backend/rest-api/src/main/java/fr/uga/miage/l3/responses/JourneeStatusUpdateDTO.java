package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mise à jour de statut d'une journée envoyée via WebSocket STOMP")
public record JourneeStatusUpdateDTO(
    @Schema(description = "La date de la journée", example = "2025-05-09")
    String date,
    @Schema(
        description = "Le statut courant de génération",
        allowableValues = {"INITIALISATION", "GENERATION_MATRICE", "GENERATION_ROUTE", "TERMINE"}
    )
    String statutGeneration,
    @Schema(description = "Horodatage ISO-8601 de l'événement", example = "2025-05-09T14:30:00Z")
    String timestamp
) {
}
