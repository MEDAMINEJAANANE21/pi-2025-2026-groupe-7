package fr.uga.miage.l3.services.core.optimization.greedy;

import fr.uga.miage.l3.fixtures.ProblemFixtures;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyCommandTest {

    private final GreedyCommand greedy = new GreedyCommand();

    @Test
    void visiteToujoursLaLivraisonLaPlusProche() {
        // Livraisons alignées à 3, 1 et 2 km du dépôt (placé en 0).
        OptimisationProblem problem = ProblemFixtures.onPlane(
            new double[]{3, 0}, new double[]{1, 0}, new double[]{2, 0}, new double[]{0, 0});

        List<Integer> route = greedy.searchForFastestPath(List.of(0, 1, 2), problem.parkingIndex(), problem.distances());

        assertThat(route).containsExactly(1, 2, 0);
    }

    @Test
    void neVisiteQueLesLivraisonsDuGroupe() {
        OptimisationProblem problem = ProblemFixtures.onPlane(
            new double[]{3, 0}, new double[]{1, 0}, new double[]{2, 0}, new double[]{0, 0});

        List<Integer> route = greedy.searchForFastestPath(List.of(0, 2), problem.parkingIndex(), problem.distances());

        assertThat(route).containsExactly(2, 0);
    }

    @Test
    void groupeVideDonneUneTourneeVide() {
        OptimisationProblem problem = ProblemFixtures.onPlane(new double[]{1, 0}, new double[]{0, 0});

        assertThat(greedy.searchForFastestPath(List.of(), problem.parkingIndex(), problem.distances())).isEmpty();
    }
}
