package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.fixtures.ProblemFixtures;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.services.core.separation.AngularSeparationStrategy;
import fr.uga.miage.l3.services.core.separation.KMedoidsSeparationStrategy;
import fr.uga.miage.l3.services.core.separation.NoSeparationStrategy;
import fr.uga.miage.l3.services.optimisation.routing.GeneticRouteOptimizer;
import fr.uga.miage.l3.services.optimisation.routing.GreedyRouteOptimizer;
import fr.uga.miage.l3.services.optimisation.routing.RouteOptimizers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie l'enchaînement séparation → itinéraire → contrainte de durée, sans base ni réseau.
 */
class OptimisationSolveTest {

    private final OptimisationPipelineService pipeline = new OptimisationPipelineService(
        null, null, null, null, null,
        new GroupSeparator(List.of(new NoSeparationStrategy(), new AngularSeparationStrategy(), new KMedoidsSeparationStrategy())),
        new RouteOptimizers(List.of(new GreedyRouteOptimizer(), new GeneticRouteOptimizer())),
        new MaxDurationConstraint(),
        Runnable::run
    );

    private final OptimisationProblem problem = ProblemFixtures.onPlane(
        new double[]{-5, 0}, new double[]{-6, 1}, new double[]{5, 0}, new double[]{6, 1}, new double[]{0, 0});

    @ParameterizedTest
    @EnumSource(value = Separation.class, names = {"ANGULAR", "KMEDOIDS"})
    void uneSeparationDonneUneTourneeParVehicule(Separation separation) {
        List<List<Integer>> routes = pipeline.solve(strategie(separation, Optimisation.GREEDY, 2, Duration.ofHours(8)), problem);

        assertThat(routes).hasSize(2);
        assertThat(routes.stream().flatMap(List::stream).toList()).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @ParameterizedTest
    @EnumSource(value = Optimisation.class, names = {"GREEDY", "GENETIC"})
    void sansSeparationToutesLesLivraisonsSontDansUneTournee(Optimisation optimisation) {
        List<List<Integer>> routes = pipeline.solve(strategie(Separation.NONE, optimisation, 1, Duration.ofHours(8)), problem);

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0)).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @ParameterizedTest
    @EnumSource(value = Optimisation.class, names = {"GREEDY", "GENETIC"})
    void unBudgetTropCourtRetireDesLivraisons(Optimisation optimisation) {
        List<List<Integer>> routes = pipeline.solve(strategie(Separation.NONE, optimisation, 1, Duration.ofMinutes(10)), problem);

        assertThat(routes.stream().mapToInt(List::size).sum()).isLessThan(4);
    }

    private static Strategie strategie(Separation separation, Optimisation optimisation, int nbVehicules, Duration max) {
        return Strategie.builder()
            .separation(separation)
            .optimisation(optimisation)
            .nbVehicules(nbVehicules)
            .maxTempsParVehicule(max)
            .build();
    }
}
