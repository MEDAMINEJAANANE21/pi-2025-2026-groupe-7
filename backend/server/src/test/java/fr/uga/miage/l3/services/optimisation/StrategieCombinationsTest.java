package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.services.core.OrsMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StrategieCombinationsTest {

    private final StrategieCombinations combinations = new StrategieCombinations(new OrsMode("cle", true));

    @ParameterizedTest(name = "{0} véhicule(s) → {1} stratégies")
    @CsvSource({"1, 3", "2, 10", "3, 17", "5, 31"})
    void nombreDeStrategiesSelonLeNombreDeVehicules(int nbVehicules, int attendu) {
        assertThat(combinations.expand(base(nbVehicules))).hasSize(attendu);
    }

    @Test
    void aucuneSeparationAvecUnSeulVehicule() {
        List<Strategie> strategies = combinations.expand(base(3));

        assertThat(strategies)
            .filteredOn(s -> s.getNbVehicules() == 1)
            .allMatch(s -> s.getSeparation() == Separation.NONE);
    }

    @Test
    void sansSeparationSeulVroomGerePlusieursVehicules() {
        List<Strategie> strategies = combinations.expand(base(3));

        assertThat(strategies)
            .filteredOn(s -> s.getSeparation() == Separation.NONE && s.getNbVehicules() > 1)
            .allMatch(s -> s.getOptimisation() == Optimisation.VROOM);
    }

    @Test
    void conserveLaDureeMaximale() {
        assertThat(combinations.expand(base(2)))
            .allMatch(s -> Duration.ofHours(7).equals(s.getMaxTempsParVehicule()));
    }

    @Test
    void sansCleOrsLesStrategiesVroomSontIgnorees() {
        StrategieCombinations estimation = new StrategieCombinations(new OrsMode("", true));

        assertThat(estimation.expand(base(3)))
            .hasSize(10)
            .noneMatch(s -> s.getOptimisation() == Optimisation.VROOM);
    }

    private static Strategie base(int nbVehicules) {
        return Strategie.builder().nbVehicules(nbVehicules).maxTempsParVehicule(Duration.ofHours(7)).build();
    }
}
