package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représente de les coordonnées d'un point en geoJSON")
public record CoordonneeDTO (
    Double lat,
    Double lng
) {
}
