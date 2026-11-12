package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.request.SolutionTourneesUpdateRequest;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SolutionComponent {

    private final SolutionRepository solutionRepository;
    private final LivraisonRepository livraisonRepository;
    private final TourneeComponent tourneeComponent;
    private final JourneeComponent journeeComponent;

    public SolutionEntity get(Long id) {
        return solutionRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Solution", id));
    }

    public List<SolutionEntity> findByDate(LocalDate date) {
        return solutionRepository.findAllByDateOrderByIdAsc(date);
    }

    public SolutionEntity create(LocalDate date, Strategie strategie) {
        return solutionRepository.save(SolutionEntity.builder().date(date).strategie(strategie).build());
    }

    /** Enregistre la solution et ses tournées ensemble : en cas d'erreur, rien n'est conservé. */
    @Transactional
    public SolutionEntity saveGenerated(LocalDate date, Strategie strategie, Set<Long> livraisonIds,
                                        List<List<Integer>> routes, OptimisationProblem problem) {
        SolutionEntity solution = solutionRepository.save(SolutionEntity.builder()
            .date(date)
            .strategie(strategie)
            .livraisonsIds(new HashSet<>(livraisonIds))
            .build());
        tourneeComponent.saveForSolution(solution, routes, problem);
        return solution;
    }

    @Transactional
    public SolutionEntity choisir(Long id) {
        SolutionEntity solution = get(id);
        journeeComponent.choisir(solution);
        return solution;
    }

    public Livraisons livraisons(Long id) {
        SolutionEntity solution = get(id);
        Set<LivraisonEntity> dansSolution = new LinkedHashSet<>();
        tourneesOf(solution).forEach(tournee -> dansSolution.addAll(tournee.getLivraisonEntities()));
        Set<Long> idsDansSolution = dansSolution.stream().map(LivraisonEntity::getId).collect(Collectors.toSet());
        List<LivraisonEntity> horsSolution = livraisonRepository.findAllByLivree(false).stream()
            .filter(livraison -> !idsDansSolution.contains(livraison.getId()))
            .toList();
        return new Livraisons(new ArrayList<>(dansSolution), horsSolution);
    }

    @Transactional
    public SolutionEntity updateTournees(Long id, SolutionTourneesUpdateRequest request) {
        ensureNoDuplicate(request);
        SolutionEntity solution = get(id);
        Map<Long, TourneeEntity> tourneesById = tourneesOf(solution).stream()
            .collect(Collectors.toMap(TourneeEntity::getId, Function.identity()));

        for (SolutionTourneesUpdateRequest.TourneeUpdateDTO update : request.tournees()) {
            TourneeEntity tournee = tourneesById.get(update.tourneeId());
            if (tournee == null) {
                throw new ResourceNotFoundException("La tournée " + update.tourneeId() + " n'appartient pas à la solution " + id);
            }
            tournee.setLivraisonEntities(new ArrayList<>(update.livraisonIds().stream().map(this::livraison).toList()));
        }
        tourneesOf(solution).forEach(tourneeComponent::recalculateStatistics);
        return solution;
    }

    @Transactional
    public SolutionEntity recalculateStatistics(Long id) {
        SolutionEntity solution = get(id);
        tourneesOf(solution).forEach(tourneeComponent::recalculateStatistics);
        return solution;
    }

    private LivraisonEntity livraison(Long id) {
        return livraisonRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Livraison", id));
    }

    private static void ensureNoDuplicate(SolutionTourneesUpdateRequest request) {
        List<Long> ids = request.tournees().stream().flatMap(t -> t.livraisonIds().stream()).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidRequestException("Une livraison ne peut figurer que dans une seule tournée");
        }
    }

    private static List<TourneeEntity> tourneesOf(SolutionEntity solution) {
        return solution.getTourneeEntities() == null
            ? List.of()
            : solution.getTourneeEntities().stream().filter(Objects::nonNull).toList();
    }

    public record Livraisons(List<LivraisonEntity> dansSolution, List<LivraisonEntity> horsSolution) {
    }
}
