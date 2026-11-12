package fr.uga.miage.l3.services.statistics;

import fr.uga.miage.l3.config.CoutsProperties;
import fr.uga.miage.l3.models.domain.SolutionStatisticsData;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SolutionStatisticsCalculatorTest {

    private final SolutionStatisticsCalculator calculator = new SolutionStatisticsCalculator(
        new FinanceCalculator(new CoutsProperties(8.0, 2.0, 20.0, 10.0, 0.0)));

    @Test
    void agregeTotalMinimumMaximumEtMoyenne() {
        SolutionEntity solution = SolutionEntity.builder().id(1L).tourneeEntities(Set.of(
            tournee(1L, 1000L, 100L, Duration.ofMinutes(10), Duration.ofMinutes(5), 2.0),
            tournee(2L, 3000L, 300L, Duration.ofMinutes(30), Duration.ofMinutes(15), 4.0, 6.0)
        )).build();

        SolutionStatisticsData stats = calculator.compute(solution);

        assertThat(stats.nbVehicules()).isEqualTo(2L);
        assertThat(stats.nbLivraisonsTotal()).isEqualTo(3L);
        assertThat(stats.nbLivraisonsMin()).isEqualTo(1);
        assertThat(stats.nbLivraisonsMax()).isEqualTo(2);
        assertThat(stats.distanceTotal()).isEqualTo(4000L);
        assertThat(stats.distanceMin()).isEqualTo(1000L);
        assertThat(stats.distanceMoyenne()).isEqualTo(2000.0);
        assertThat(stats.carburantMax()).isEqualTo(300L);
        assertThat(stats.poidsTotal()).isEqualTo(12.0);
        assertThat(stats.tempsRouteTotal()).isEqualTo(Duration.ofMinutes(40));
        assertThat(stats.tempsTotalMin()).isEqualTo(Duration.ofMinutes(15));
        assertThat(stats.tempsTotalMax()).isEqualTo(Duration.ofMinutes(45));
        assertThat(stats.finances().chiffreAffaires()).isEqualTo(30.0);
    }

    @Test
    void solutionSansTourneeDonneDesZeros() {
        SolutionStatisticsData stats = calculator.compute(SolutionEntity.builder().id(2L).build());

        assertThat(stats.nbVehicules()).isZero();
        assertThat(stats.nbLivraisonsTotal()).isZero();
        assertThat(stats.distanceMin()).isNull();
        assertThat(stats.distanceMoyenne()).isNull();
        assertThat(stats.finances()).isEqualTo(TourneeFinances.ZERO);
    }

    private static TourneeEntity tournee(Long id, long distance, long carburant, Duration trajet, Duration montage, Double... poids) {
        return TourneeEntity.builder()
            .id(id)
            .statistic(Statistic.builder().distance(distance).carburant(carburant).tempsTrajet(trajet).tempsMontage(montage).build())
            .livraisonEntities(List.of(poids).stream().map(p -> LivraisonEntity.builder().poids(p).build()).toList())
            .build();
    }
}
