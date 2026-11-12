package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.services.statistics.SolutionStatisticsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Construit la vue complète d'une solution : tournées triées et statistiques à jour.
 */
@Component
@RequiredArgsConstructor
public class SolutionMapper {

    private final TourneeMapper tourneeMapper;
    private final JourneeMapper journeeMapper;
    private final SolutionStatisticsCalculator statisticsCalculator;
    private final SolutionStatisticsMapper statisticsMapper;

    public SolutionResponseDTO toDto(SolutionEntity solution) {
        List<TourneeEntity> tournees = solution.getTourneeEntities() == null ? List.of() : solution.getTourneeEntities().stream()
            .sorted(Comparator.comparing(TourneeEntity::getId))
            .toList();
        return new SolutionResponseDTO(
            solution.getId(),
            solution.getDate() == null ? null : solution.getDate().toString(),
            journeeMapper.toStrategieDto(solution.getStrategie()),
            tournees.stream().map(tourneeMapper::toDto).toList(),
            statisticsOf(solution)
        );
    }

    public SolutionStatisticsResponseDTO statisticsOf(SolutionEntity solution) {
        return statisticsMapper.toDto(statisticsCalculator.compute(solution));
    }
}
