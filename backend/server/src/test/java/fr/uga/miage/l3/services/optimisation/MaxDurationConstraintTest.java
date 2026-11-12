package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.fixtures.ProblemFixtures;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaxDurationConstraintTest {

    private final MaxDurationConstraint constraint = new MaxDurationConstraint();

    // Livraisons à 1, 2 et 3 km sur une ligne ; aller-retour complet = 6 km = 600 s.
    // ProblemFixtures date la livraison i au 1er janvier + i jours : la n°2 est la plus récente.
    private final OptimisationProblem line = ProblemFixtures.onPlane(
        new double[]{1, 0}, new double[]{2, 0}, new double[]{3, 0}, new double[]{0, 0});

    @Test
    void calculeLaDureeAllerRetour() {
        assertThat(constraint.durationOf(List.of(0, 1, 2), line)).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void retireLaCommandeLaPlusRecenteQuandLeBudgetEstDepasse() {
        List<List<Integer>> routes = constraint.apply(List.of(List.of(0, 1, 2)), Duration.ofSeconds(450), line);

        assertThat(routes).containsExactly(List.of(0, 1));
    }

    @Test
    void supprimeUneTourneeDevenueVide() {
        List<List<Integer>> routes = constraint.apply(List.of(List.of(2)), Duration.ofSeconds(10), line);

        assertThat(routes).isEmpty();
    }

    @Test
    void sansBudgetLesTourneesSontConservees() {
        List<List<Integer>> routes = List.of(List.of(0, 1, 2));

        assertThat(constraint.apply(routes, null, line)).isSameAs(routes);
        assertThat(constraint.apply(routes, Duration.ZERO, line)).isSameAs(routes);
    }
}
