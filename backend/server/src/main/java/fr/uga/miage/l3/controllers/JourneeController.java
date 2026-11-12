package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.JourneeComponent;
import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.endpoints.JourneeEndpoints;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.JourneeMapper;
import fr.uga.miage.l3.mappers.SolutionMapper;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.JourneeResumeDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.services.optimisation.OptimisationPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JourneeController implements JourneeEndpoints {

    private final JourneeComponent journeeComponent;
    private final SolutionComponent solutionComponent;
    private final OptimisationPipelineService optimisationPipelineService;
    private final JourneeMapper journeeMapper;
    private final SolutionMapper solutionMapper;

    @Override
    public List<JourneeResumeDTO> getJournees(String debut, String fin) {
        return journeeComponent.calendrier(Dates.parse(debut), Dates.parse(fin)).stream()
            .map(resume -> journeeMapper.toResume(resume.journee(), resume.nbSolutions()))
            .toList();
    }

    @Override
    public JourneeResponseDTO getJournee(String date) {
        return journeeMapper.toDto(journeeComponent.find(Dates.parse(date)));
    }

    @Override
    public List<SolutionStatisticsResponseDTO> getJourneeSolutions(String date) {
        return solutionComponent.findByDate(Dates.parse(date)).stream()
            .map(solutionMapper::statisticsOf)
            .toList();
    }

    @Override
    public JourneeResponseDTO startJourneeOptimization(String date, StrategieRequestDTO request) {
        return journeeMapper.toDto(optimisationPipelineService.start(Dates.parse(date), request));
    }

    @Override
    public SolutionResponseDTO getJourneeSolutionChoisie(String date) {
        JourneeEntity journee = journeeComponent.find(Dates.parse(date));
        if (journee.getSolutionEntity() == null) {
            throw new ResourceNotFoundException("Aucune solution choisie pour cette journée");
        }
        return solutionMapper.toDto(journee.getSolutionEntity());
    }
}
