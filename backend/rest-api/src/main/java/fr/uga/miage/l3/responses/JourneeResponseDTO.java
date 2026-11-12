package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation d'une journee")
public record JourneeResponseDTO(
    String date,
    String statutGeneration,
    StrategieResponseDTO strategie
) {
}
