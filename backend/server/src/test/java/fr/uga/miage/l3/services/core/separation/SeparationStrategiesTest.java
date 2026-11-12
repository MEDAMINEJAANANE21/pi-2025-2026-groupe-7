package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.fixtures.ProblemFixtures;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SeparationStrategiesTest {

    // Deux quartiers éloignés : trois livraisons à l'ouest, trois à l'est, dépôt au centre.
    private final OptimisationProblem twoNeighbourhoods = ProblemFixtures.onPlane(
        new double[]{-10, 0}, new double[]{-10, 1}, new double[]{-11, 0},
        new double[]{10, 0}, new double[]{10, 1}, new double[]{11, 0},
        new double[]{0, 0});

    @Nested
    class Angular {

        private final AngularSeparationStrategy strategy = new AngularSeparationStrategy();

        @Test
        void decoupeEnSecteursDeTailleEquilibree() {
            List<List<Integer>> groups = strategy.separate(twoNeighbourhoods.locations(), twoNeighbourhoods.distances(), 2);

            assertThat(groups).hasSize(2).allSatisfy(group -> assertThat(group).hasSize(3));
            assertThat(asSets(groups)).containsExactlyInAnyOrder(Set.of(0, 1, 2), Set.of(3, 4, 5));
        }

        @Test
        void chaqueLivraisonApparaitUneSeuleFois() {
            List<List<Integer>> groups = strategy.separate(twoNeighbourhoods.locations(), twoNeighbourhoods.distances(), 4);

            assertThat(groups.stream().flatMap(List::stream).toList()).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5);
        }

        @Test
        void sansLivraisonNiGroupeRenvoieRien() {
            assertThat(strategy.separate(List.of(), new double[0][0], 2)).isEmpty();
            assertThat(strategy.separate(twoNeighbourhoods.locations(), twoNeighbourhoods.distances(), 0)).isEmpty();
        }
    }

    @Nested
    class KMedoids {

        private final KMedoidsSeparationStrategy strategy = new KMedoidsSeparationStrategy();

        @Test
        void regroupeLesLivraisonsVoisines() {
            List<List<Integer>> groups = strategy.separate(twoNeighbourhoods.locations(), twoNeighbourhoods.distances(), 2);

            assertThat(asSets(groups)).containsExactlyInAnyOrder(Set.of(0, 1, 2), Set.of(3, 4, 5));
        }

        @Test
        void neDemandeJamaisPlusDeGroupesQueDeLivraisons() {
            OptimisationProblem twoDeliveries = ProblemFixtures.onPlane(new double[]{1, 0}, new double[]{2, 0}, new double[]{0, 0});

            List<List<Integer>> groups = strategy.separate(twoDeliveries.locations(), twoDeliveries.distances(), 5);

            assertThat(groups.stream().flatMap(List::stream).toList()).containsExactlyInAnyOrder(0, 1);
        }
    }

    @Nested
    class None {

        @Test
        void renvoieUnSeulGroupeAvecToutesLesLivraisons() {
            List<List<Integer>> groups = new NoSeparationStrategy()
                .separate(twoNeighbourhoods.locations(), twoNeighbourhoods.distances(), 3);

            assertThat(groups).containsExactly(List.of(0, 1, 2, 3, 4, 5));
        }
    }

    private static List<Set<Integer>> asSets(List<List<Integer>> groups) {
        return groups.stream().map(group -> group.stream().collect(Collectors.toSet())).toList();
    }
}
