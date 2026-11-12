package fr.uga.miage.l3.component;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.services.JourneeStatusBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JourneeComponent {

    private static final long MAX_CALENDAR_DAYS = 366;

    private final JourneeRepository journeeRepository;
    private final SolutionRepository solutionRepository;
    private final JourneeStatusBroadcastService broadcastService;

    public JourneeEntity getOrCreate(LocalDate date) {
        return journeeRepository.findById(date)
            .orElseGet(() -> journeeRepository.save(JourneeEntity.builder().date(date).build()));
    }

    /** Renvoie la journée telle quelle, sans la créer : une simple lecture ne doit rien écrire. */
    public JourneeEntity find(LocalDate date) {
        return journeeRepository.findById(date).orElseGet(() -> JourneeEntity.builder().date(date).build());
    }

    public JourneeEntity updateStatus(LocalDate date, StatutGeneration statut) {
        JourneeEntity journee = getOrCreate(date);
        journee.setStatutGeneration(statut);
        JourneeEntity saved = journeeRepository.save(journee);
        broadcast(date, statut);
        return saved;
    }

    public void broadcast(LocalDate date, StatutGeneration statut) {
        broadcastService.broadcast(date, statut);
    }

    @Transactional
    public void choisir(SolutionEntity solution) {
        JourneeEntity journee = getOrCreate(solution.getDate());
        journee.setSolutionEntity(solution);
        journeeRepository.save(journee);
    }

    /**
     * Journées connues sur la période, avec le nombre de solutions de chacune.
     * Une date qui n'a que des solutions (journée jamais ouverte) apparaît aussi.
     */
    public List<JourneeResume> calendrier(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new InvalidRequestException("La date de fin doit suivre la date de début");
        }
        if (ChronoUnit.DAYS.between(debut, fin) > MAX_CALENDAR_DAYS) {
            throw new InvalidRequestException("La période demandée ne peut pas dépasser un an");
        }
        Map<LocalDate, Long> solutionsByDate = solutionRepository.findAllByDateBetween(debut, fin).stream()
            .collect(Collectors.groupingBy(SolutionEntity::getDate, Collectors.counting()));
        Map<LocalDate, JourneeEntity> journees = journeeRepository.findAllByDateBetween(debut, fin).stream()
            .collect(Collectors.toMap(JourneeEntity::getDate, Function.identity()));

        Set<LocalDate> dates = new TreeSet<>(solutionsByDate.keySet());
        journees.values().stream()
            .filter(journee -> journee.getStatutGeneration() != null)
            .forEach(journee -> dates.add(journee.getDate()));
        return dates.stream()
            .map(date -> new JourneeResume(
                journees.getOrDefault(date, JourneeEntity.builder().date(date).build()),
                solutionsByDate.getOrDefault(date, 0L).intValue()))
            .toList();
    }

    public record JourneeResume(JourneeEntity journee, int nbSolutions) {
    }

    /**
     * Après un redémarrage, une optimisation interrompue resterait « en cours » pour toujours
     * et bloquerait toute relance : on la marque terminée.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void closeInterruptedOptimisations() {
        List<JourneeEntity> interrupted = journeeRepository.findAllByStatutGenerationIn(EnumSet.of(
            StatutGeneration.INITIALISATION, StatutGeneration.GENERATION_MATRICE, StatutGeneration.GENERATION_ROUTE));
        interrupted.forEach(journee -> journee.setStatutGeneration(StatutGeneration.TERMINE));
        journeeRepository.saveAll(interrupted);
        if (!interrupted.isEmpty()) {
            log.warn("{} optimisation(s) interrompue(s) par un redémarrage marquée(s) terminée(s)", interrupted.size());
        }
    }
}
