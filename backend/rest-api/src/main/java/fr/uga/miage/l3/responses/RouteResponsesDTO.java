package fr.uga.miage.l3.responses;

import fr.uga.miage.l3.responses.CoordonneeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

@Schema(description = "Représente de la route geoJSON pour une tournée")
public record RouteResponsesDTO (
    List<CoordonneeDTO> coordinates
) {
}
