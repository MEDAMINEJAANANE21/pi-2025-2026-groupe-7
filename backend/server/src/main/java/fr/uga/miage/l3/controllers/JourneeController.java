package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.endpoints.JourneeEndpoints;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import fr.uga.miage.l3.mappers.JourneeMapper;
import fr.uga.miage.l3.mappers.SolutionMapper;
import fr.uga.miage.l3.mappers.SolutionStatisticsMapper;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.services.OptimisationPipelineService;
import fr.uga.miage.l3.component.JourneeComponent;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class JourneeController implements JourneeEndpoints {

  private final JourneeComponent journeeComponent;
  private final SolutionComponent solutionComponent;
  private final JourneeMapper journeeMapper;
  private final OptimisationPipelineService optimisationPipelineService;
  private final SolutionStatisticsMapper solutionStatisticsMapper;
  private final JourneeRepository journeeRepository;
  private final SolutionMapper solutionMapper;

  @Override
  public JourneeResponseDTO getJournee(String date) {
    LocalDate parsedDate = parseDate(date);

    JourneeEntity journee = journeeComponent.getOrCreate(parsedDate);

    return journeeMapper.toDto(journee);
  }

  @Override
  public Set<SolutionStatisticsResponseDTO> getJourneeSolutions(String date) {
    LocalDate parsedDate = parseDate(date);
    return solutionComponent.getStatisticsByDate(parsedDate)
        .stream()
        .map(solutionStatisticsMapper::toDto)
        .collect(java.util.stream.Collectors.toSet());
  }

  @Override
  public JourneeResponseDTO startJourneeOptimization(String date, StrategieRequestDTO request) {
    LocalDate parsedDate = parseDate(date);
    JourneeEntity journee = optimisationPipelineService.startJourneeOptimization(parsedDate, request);
    return journeeMapper.toDto(journee);
  }

  @Override
  public SolutionResponseDTO getJourneeSolutionChoisie(String date) {
    LocalDate parsedDate = parseDate(date);
    JourneeEntity journee = journeeRepository.findById(parsedDate).orElse(null);
    if (journee == null || journee.getSolutionEntity() == null) {
      throw new NotFoundElementRestException("Aucune solution choisie pour cette journée");
    }
    return solutionMapper.toSolutionResponseDTO(journee.getSolutionEntity());
  }

  private LocalDate parseDate(String date) {
    if (date == null || date.isBlank()) {
      throw new BadRequestRestException("date path param must be provided with format yyyy-MM-dd");
    }

    try {
      return LocalDate.parse(date);
    } catch (DateTimeParseException ex) {
      throw new BadRequestRestException("Invalid date format, expected yyyy-MM-dd");
    }
  }

}
