package fr.uga.miage.l3.request;

import fr.uga.miage.l3.responses.CoordonneeDTO;
import java.util.List;

public record RouteRequest (
    List<CoordonneeDTO> adresses
) {
}
