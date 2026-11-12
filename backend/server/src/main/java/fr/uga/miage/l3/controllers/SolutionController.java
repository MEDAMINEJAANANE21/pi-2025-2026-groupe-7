package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.endpoints.SolutionEndpoints;
import fr.uga.miage.l3.mappers.LivraisonMapper;
import fr.uga.miage.l3.mappers.SolutionMapper;
import fr.uga.miage.l3.mappers.StrategieMapper;
import fr.uga.miage.l3.request.SolutionCreationRequest;
import fr.uga.miage.l3.request.SolutionTourneesUpdateRequest;
import fr.uga.miage.l3.responses.SolutionLivraisonsResponseDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SolutionController implements SolutionEndpoints {

    private final SolutionComponent solutionComponent;
    private final SolutionMapper solutionMapper;
    private final StrategieMapper strategieMapper;
    private final LivraisonMapper livraisonMapper;

    @Override
    public SolutionResponseDTO getSolution(Long solutionId) {
        return solutionMapper.toDto(solutionComponent.get(solutionId));
    }

    @Override
    public SolutionLivraisonsResponseDTO getSolutionLivraisons(Long solutionId) {
        SolutionComponent.Livraisons livraisons = solutionComponent.livraisons(solutionId);
        return new SolutionLivraisonsResponseDTO(
            livraisonMapper.toDTOList(livraisons.dansSolution()),
            livraisonMapper.toDTOList(livraisons.horsSolution())
        );
    }

    @Override
    public SolutionResponseDTO createSolution(SolutionCreationRequest request) {
        return solutionMapper.toDto(solutionComponent.create(
            Dates.parse(request.date()),
            strategieMapper.toEntity(request.strategie())
        ));
    }

    @Override
    public SolutionResponseDTO choisirSolution(Long solutionId) {
        return solutionMapper.toDto(solutionComponent.choisir(solutionId));
    }

    @Override
    public SolutionResponseDTO updateSolutionTournees(Long solutionId, SolutionTourneesUpdateRequest request) {
        return solutionMapper.toDto(solutionComponent.updateTournees(solutionId, request));
    }

    @Override
    public SolutionResponseDTO recalculateSolutionStats(Long solutionId) {
        return solutionMapper.toDto(solutionComponent.recalculateStatistics(solutionId));
    }
}
