package fr.uga.miage.l3.request;

import fr.uga.miage.l3.responses.AdresseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Représente d'une tournee")
public record TourneeRequestDTO(
    List<AdresseDTO> adresses
) {
}
