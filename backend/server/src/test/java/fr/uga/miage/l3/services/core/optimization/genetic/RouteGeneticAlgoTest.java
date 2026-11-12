package fr.uga.miage.l3.services.core.optimization.genetic;

import fr.uga.miage.l3.fixtures.ProblemFixtures;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RouteGeneticAlgoTest {

    @Test
    void trouveLeTourOptimalSurUnPetitCarre() {
        // Quatre coins d'un carré de 1 km et le dépôt au milieu d'un côté : le tour optimal
        // longe le périmètre (4 km) au lieu de traverser en diagonale.
        OptimisationProblem problem = ProblemFixtures.onPlane(
            new double[]{0, 0}, new double[]{1, 0}, new double[]{1, 1}, new double[]{0, 1}, new double[]{0.5, 0});
        RouteGeneticAlgo algo = new RouteGeneticAlgo(new int[]{0, 1, 2, 3}, problem.parkingIndex(), problem.distances());

        int[] route = algo.searchForFastestPath();

        assertThat(Arrays.stream(route).boxed().sorted().toList()).containsExactly(0, 1, 2, 3);
        assertThat(algo.totalDistance(route)).isCloseTo(4000.0, within(1.0));
    }

    @Test
    void distanceTotaleInclutLeRetourAuDepot() {
        OptimisationProblem problem = ProblemFixtures.onPlane(new double[]{1, 0}, new double[]{2, 0}, new double[]{0, 0});
        RouteGeneticAlgo algo = new RouteGeneticAlgo(new int[]{0, 1}, problem.parkingIndex(), problem.distances());

        assertThat(algo.totalDistance(new int[]{0, 1})).isCloseTo(4000.0, within(0.01));
    }
}
