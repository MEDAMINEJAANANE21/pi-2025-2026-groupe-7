package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeuilleDeRouteServiceTest {

    private static final String BOM = String.valueOf((char) 0xFEFF);

    private final FeuilleDeRouteService service = new FeuilleDeRouteService();

    @Test
    void produitUneLigneParArretDansLOrdreDePassage() {
        TourneeEntity tournee = TourneeEntity.builder()
            .id(7L)
            .solutionEntity(SolutionEntity.builder().date(LocalDate.of(2026, 11, 12)).build())
            .livraisonEntities(List.of(
                livraison("LIV-1", "12", "Rue Félix Viallet", 2.5, Duration.ZERO, false),
                livraison("LIV-2", "", "Place Grenette", 12.0, Duration.ofMinutes(15), true)
            ))
            .build();

        String csv = new String(service.toCsv(tournee), StandardCharsets.UTF_8);

        assertThat(csv).startsWith(BOM + "Ordre;Référence;Adresse;");
        assertThat(csv.lines().toList()).containsSubsequence(
            "1;LIV-1;12 Rue Félix Viallet;38000;Grenoble;2,5;0;non",
            "2;LIV-2;Place Grenette;38000;Grenoble;12,0;15;oui"
        );
        assertThat(service.fileName(tournee)).isEqualTo("feuille-de-route-2026-11-12-tournee-7.csv");
    }

    @Test
    void tourneeSansLivraisonNeContientQueLEnTete() {
        String csv = new String(service.toCsv(TourneeEntity.builder().id(1L).build()), StandardCharsets.UTF_8);

        assertThat(csv.lines()).hasSize(1);
        assertThat(service.fileName(TourneeEntity.builder().id(1L).build())).contains("sans-date");
    }

    private static LivraisonEntity livraison(String reference, String numero, String rue, double poids, Duration montage, boolean livree) {
        return LivraisonEntity.builder()
            .reference(reference)
            .poids(poids)
            .tempsMontage(montage)
            .livree(livree)
            .adresseEntity(AdresseEntity.builder().numero(numero).rue(rue).codePostal("38000").ville("Grenoble").build())
            .build();
    }
}
