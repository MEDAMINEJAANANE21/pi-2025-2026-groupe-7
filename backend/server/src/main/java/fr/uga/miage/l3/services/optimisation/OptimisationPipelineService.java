package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.component.JourneeComponent;
import fr.uga.miage.l3.component.SolutionComponent;
import fr.uga.miage.l3.config.AsyncConfig;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.exceptions.ConflictException;
import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.mappers.StrategieMapper;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.services.optimisation.routing.RouteConstraints;
import fr.uga.miage.l3.services.optimisation.routing.RouteOptimizer;
import fr.uga.miage.l3.services.optimisation.routing.RouteOptimizers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Orchestre l'optimisation d'une journée : validation, matrices, puis une solution par stratégie.
 *
 * <p>La requête HTTP rend la main tout de suite (202) ; l'avancement est diffusé par WebSocket.
 * Les futures sont chaînées sans attente bloquante, pour qu'un seul pool suffise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisationPipelineService {

    private final JourneeComponent journeeComponent;
    private final SolutionComponent solutionComponent;
    private final StrategieMapper strategieMapper;
    private final OptimisationProblemFactory problemFactory;
    private final StrategieCombinations strategieCombinations;
    private final GroupSeparator groupSeparator;
    private final RouteOptimizers routeOptimizers;
    private final MaxDurationConstraint maxDurationConstraint;
    @Qualifier(AsyncConfig.OPTIMISATION_EXECUTOR)
    private final Executor executor;

    public JourneeEntity start(LocalDate date, StrategieRequestDTO request) {
        Strategie base = toValidStrategie(request);
        DeliverySet deliveries = problemFactory.loadDeliveries(request.livraisonIds());

        JourneeEntity journee = journeeComponent.getOrCreate(date);
        if (isRunning(journee.getStatutGeneration())) {
            throw new ConflictException("Une optimisation est déjà en cours pour le " + date);
        }
        JourneeEntity started = journeeComponent.updateStatus(date, StatutGeneration.GENERATION_MATRICE);
        runInBackground(date, base, deliveries, Set.copyOf(request.livraisonIds()));
        return started;
    }

    private void runInBackground(LocalDate date, Strategie base, DeliverySet deliveries, Set<Long> requestedIds) {
        CompletableFuture
            .supplyAsync(() -> problemFactory.create(deliveries), executor)
            .thenCompose(problem -> {
                journeeComponent.updateStatus(date, StatutGeneration.GENERATION_ROUTE);
                CompletableFuture<?>[] runs = strategieCombinations.expand(base).stream()
                    .map(strategie -> CompletableFuture.runAsync(
                        () -> solveSafely(date, strategie, problem, requestedIds), executor))
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(runs);
            })
            .whenComplete((ignored, error) -> {
                if (error != null) {
                    log.error("Optimisation du {} interrompue", date, error);
                }
                journeeComponent.updateStatus(date, StatutGeneration.TERMINE);
            });
    }

    private void solveSafely(LocalDate date, Strategie strategie, OptimisationProblem problem, Set<Long> requestedIds) {
        try {
            List<List<Integer>> routes = solve(strategie, problem);
            solutionComponent.saveGenerated(date, strategie, new HashSet<>(requestedIds), routes, problem);
            journeeComponent.broadcast(date, StatutGeneration.GENERATION_ROUTE);
        } catch (RuntimeException ex) {
            // Une stratégie en échec (ex. quota ORS) ne doit pas empêcher les autres d'aboutir.
            log.warn("Stratégie {}/{}/{} abandonnée : {}",
                strategie.getSeparation(), strategie.getOptimisation(), strategie.getNbVehicules(), ex.getMessage());
        }
    }

    List<List<Integer>> solve(Strategie strategie, OptimisationProblem problem) {
        int nbVehicules = strategie.getNbVehicules();
        boolean separated = strategie.getSeparation() != Separation.NONE;
        int vehiculesPerGroup = separated ? 1 : nbVehicules;

        RouteOptimizer optimizer = routeOptimizers.get(strategie.getOptimisation());
        RouteConstraints constraints = new RouteConstraints(strategie.getMaxTempsParVehicule());
        List<List<Integer>> routes = groupSeparator.separate(strategie.getSeparation(), problem, nbVehicules).stream()
            .flatMap(group -> optimizer.optimize(group, vehiculesPerGroup, constraints, problem).stream())
            .toList();
        return maxDurationConstraint.apply(routes, strategie.getMaxTempsParVehicule(), problem);
    }

    private Strategie toValidStrategie(StrategieRequestDTO request) {
        if (request.nbVehicules() == null || request.nbVehicules() <= 0) {
            throw new InvalidRequestException("nbVehicules doit être un entier strictement positif");
        }
        if (request.maxTempsParVehicule() == null || request.maxTempsParVehicule().isBlank()) {
            throw new InvalidRequestException("maxTempsParVehicule est obligatoire (format ISO-8601, ex. PT2H30M)");
        }
        return strategieMapper.toEntity(request);
    }

    private static boolean isRunning(StatutGeneration statut) {
        return statut != null && statut != StatutGeneration.TERMINE;
    }
}
