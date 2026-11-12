package fr.uga.miage.l3.responses;

import fr.uga.miage.l3.responses.LivreurDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Représente d'une equipe")
public record EquipeResponseDTO (
    List<LivreurDTO> livreurs
) {
}