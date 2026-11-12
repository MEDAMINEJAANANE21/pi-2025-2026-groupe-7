package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de génération aléatoire de données")
public record RandomGenerationResponseDTO(
  String resource,
  int generated,
  int addressesAvailable,
  int livraisonsAvailable
) {
}