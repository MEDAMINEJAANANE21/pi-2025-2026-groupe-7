package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contenu du jeu d'essai chargé")
public record DemoDataResponseDTO(int livraisons, int chauffeurs, int vehicules) {
}
