package fr.uga.miage.l3.services.statistics;

import fr.uga.miage.l3.config.CoutsProperties;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FinanceCalculatorTest {

    // 8 L/100 km, 2 €/L, 20 €/h, 10 € par livraison, 0,50 € par kg.
    private final FinanceCalculator calculator = new FinanceCalculator(new CoutsProperties(8.0, 2.0, 20.0, 10.0, 0.5));

    @Test
    void calculeCoutsEtRecettesDUneTournee() {
        TourneeEntity tournee = tournee(Statistic.builder()
            .carburant(4_000L)
            .tempsTrajet(Duration.ofMinutes(90))
            .tempsMontage(Duration.ofMinutes(30))
            .build(), 20.0, 40.0);

        TourneeFinances finances = calculator.compute(tournee);

        assertThat(finances.coutCarburant()).isEqualTo(8.0);
        assertThat(finances.coutChauffeur()).isEqualTo(40.0);
        assertThat(finances.chiffreAffaires()).isEqualTo(50.0);
        assertThat(finances.coutTotal()).isEqualTo(48.0);
        assertThat(finances.marge()).isEqualTo(2.0);
    }

    @Test
    void utiliseLeTauxHoraireDuChauffeurAffecte() {
        TourneeEntity tournee = tournee(Statistic.builder().carburant(0L).tempsTrajet(Duration.ofHours(1)).build(), 1.0);
        tournee.setChauffeur(ChauffeurEntity.builder().tauxHoraire(30.0).build());

        assertThat(calculator.compute(tournee).coutChauffeur()).isEqualTo(30.0);
    }

    @Test
    void sansStatistiqueSeuleLaRecetteEstConnue() {
        TourneeFinances finances = calculator.compute(tournee(null, 2.0));

        assertThat(finances.coutTotal()).isZero();
        assertThat(finances.chiffreAffaires()).isEqualTo(11.0);
    }

    @Test
    void convertitLaDistanceEnCarburant() {
        assertThat(calculator.fuelMilliliters(10_000, null)).isEqualTo(800);
        assertThat(calculator.fuelMilliliters(10_000, 12.0)).isEqualTo(1200);
    }

    @Test
    void additionneDesBilans() {
        TourneeFinances total = new TourneeFinances(1, 2, 10).plus(new TourneeFinances(3, 4, 20));

        assertThat(total.coutTotal()).isEqualTo(10.0);
        assertThat(total.marge()).isCloseTo(20.0, within(1e-9));
    }

    private static TourneeEntity tournee(Statistic statistic, Double... poids) {
        List<LivraisonEntity> livraisons = java.util.Arrays.stream(poids)
            .map(p -> LivraisonEntity.builder().poids(p).build())
            .toList();
        return TourneeEntity.builder().statistic(statistic).livraisonEntities(livraisons).build();
    }
}
