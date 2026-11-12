package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.JourneeComponent;
import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.endpoints.SolutionEndpoints;
import fr.uga.miage.l3.mappers.SolutionMapper;
import fr.uga.miage.l3.mappers.StrategieMapper;
import fr.uga.miage.l3.mappers.SolutionStatisticsMapper;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.SolutionCreationRequest;
import fr.uga.miage.l3.request.SolutionTourneesUpdateRequest;
import fr.uga.miage.l3.responses.SolutionLivraisonsResponseDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SolutionController implements SolutionEndpoints {

    private final SolutionComponent solutionComponent;
    private final SolutionMapper solutionMapper;
    private final StrategieMapper strategieMapper;
    private final SolutionStatisticsMapper solutionStatisticsMapper;
    private final JourneeComponent journeeComponent;
    private final JourneeRepository journeeRepository;
    private final TourneeRepository tourneeRepository;
    private final LivraisonRepository livraisonRepository;

    @Override
    public SolutionResponseDTO getSolution(Long solutionId) {
        SolutionEntity entity;
        try {
            entity = solutionComponent.getSolutionsByIdWithTournees(solutionId);
        } catch (Exception e) {
            throw new NotFoundElementRestException("Solution not found: " + solutionId);
        }
        return buildResponseWithStats(entity);
    }

    private SolutionResponseDTO buildResponseWithStats(SolutionEntity entity) {
        var dto = solutionMapper.toSolutionResponseDTO(entity);
        var sortedTournees = dto.tournees() == null ? null
            : dto.tournees().stream().sorted(Comparator.comparingLong(t -> t.id())).toList();
        var stats = solutionComponent.getStatisticsByDate(entity.getDate())
            .stream()
            .filter(s -> s.id().equals(entity.getId()))
            .findFirst()
            .orElse(null);
        if (stats != null) {
            var statsDto = solutionStatisticsMapper.toDto(stats);
            return new SolutionResponseDTO(dto.id(), dto.date(), dto.strategie(), sortedTournees, statsDto);
        }
        return new SolutionResponseDTO(dto.id(), dto.date(), dto.strategie(), sortedTournees, null);
    }

    @Override
    public SolutionLivraisonsResponseDTO getSolutionLivraisons(Long solutionId) {
        return solutionComponent.getSolutionLivraisons(solutionId);
    }

    @Override
    public SolutionResponseDTO choisirSolution(Long solutionId) {
        SolutionEntity solution;
        try {
            solution = solutionComponent.getSolutionsByIdWithTournees(solutionId);
        } catch (Exception e) {
            throw new NotFoundElementRestException("Solution not found");
        }
        JourneeEntity journee = journeeComponent.getOrCreate(solution.getDate());
        journee.setSolutionEntity(solution);
        journeeRepository.save(journee);
        return solutionMapper.toSolutionResponseDTO(solution);
    }

    @Override
    public SolutionResponseDTO createSolution(SolutionCreationRequest request) {
        LocalDate date;
        try {
            date = LocalDate.parse(request.date());
        } catch (DateTimeParseException e) {
            throw new BadRequestRestException("Invalid date format: '" + request.date() + "'. Expected ISO-8601 (example: 2026-04-19)");
        }

        SolutionEntity toCreate = SolutionEntity.builder()
                .date(date)
                .strategie(strategieMapper.toEntity(request.strategie()))
                .build();

        SolutionEntity saved = solutionComponent.save(toCreate);
        return solutionMapper.toSolutionResponseDTO(saved);
    }

    @Override
    public SolutionResponseDTO recalculateSolutionStats(Long solutionId) {
        SolutionEntity solution = solutionComponent.getSolutionsByIdWithTournees(solutionId);
        solutionComponent.recalculateTourneeStats(solution);
        return buildResponseWithStats(solutionComponent.getSolutionsByIdWithTournees(solutionId));
    }

    @Override
    public SolutionResponseDTO updateSolutionTournees(Long solutionId, SolutionTourneesUpdateRequest request) {
        SolutionEntity solution = solutionComponent.getSolutionsByIdWithTournees(solutionId);

        for (SolutionTourneesUpdateRequest.TourneeUpdateDTO tourneeUpdate : request.tournees()) {
            TourneeEntity tournee = tourneeRepository.findById(tourneeUpdate.tourneeId())
                .orElseThrow(() -> new NotFoundElementRestException(
                    "Tournee not found: " + tourneeUpdate.tourneeId()));

            if (tournee.getSolutionEntity() == null
                || !tournee.getSolutionEntity().getId().equals(solutionId)) {
                throw new NotFoundElementRestException(
                    "Tournee " + tourneeUpdate.tourneeId() + " does not belong to solution " + solutionId);
            }

            List<LivraisonEntity> newLivraisons = new ArrayList<>();
            for (Long livraisonId : tourneeUpdate.livraisonIds()) {
                LivraisonEntity livraison = livraisonRepository.findById(livraisonId)
                    .orElseThrow(() -> new NotFoundElementRestException(
                        "Livraison not found: " + livraisonId));
                newLivraisons.add(livraison);
            }

            tournee.setLivraisonEntities(newLivraisons);
            tourneeRepository.save(tournee);
        }

        SolutionEntity updated = solutionComponent.getSolutionsByIdWithTournees(solutionId);
        solutionComponent.recalculateTourneeStats(updated);
        return buildResponseWithStats(solutionComponent.getSolutionsByIdWithTournees(solutionId));
    }
}
