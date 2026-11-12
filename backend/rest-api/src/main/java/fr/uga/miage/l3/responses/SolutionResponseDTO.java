package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;

@Schema(description = "Représente une solution")
public record SolutionResponseDTO(
    Long id,
    String date,
    StrategieResponseDTO strategie,
    List<TourneeResponsesDTO> tournees,
    SolutionStatisticsResponseDTO statistiques
) {
}
