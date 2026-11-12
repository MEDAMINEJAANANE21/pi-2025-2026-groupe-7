package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.fixtures.DatabaseCleaner;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResumeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class JourneeCalendrierEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory factory;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private JourneeRepository journeeRepository;
    @Autowired private ParkingRepository parkingRepository;
    @Autowired private AdresseRepository adresseRepository;

    @AfterEach
    void cleanup() {
        cleaner.clean();
    }

    @Test
    void listeLesJourneesPlanifieesDeLaPeriode() {
        factory.createJourneeWithSolutions(LocalDate.of(2026, 7, 3), 2, 1, 1);
        factory.createSolution(LocalDate.of(2026, 7, 10));
        factory.createSolution(LocalDate.of(2026, 8, 1));

        webClient.get().uri("/api/journees?debut=2026-07-01&fin=2026-07-31")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(JourneeResumeDTO.class)
            .value(journees -> {
                assertThat(journees).extracting(JourneeResumeDTO::date).containsExactly("2026-07-03", "2026-07-10");
                assertThat(journees).extracting(JourneeResumeDTO::nbSolutions).containsExactly(2, 1);
            });
    }

    @Test
    void indiqueLaSolutionChoisie() {
        Long solutionId = factory.createSolution(LocalDate.of(2026, 7, 4)).getId();
        webClient.put().uri("/api/solutions/" + solutionId + "/choisie").exchange().expectStatus().isOk();

        webClient.get().uri("/api/journees?debut=2026-07-04&fin=2026-07-04")
            .exchange()
            .expectBodyList(JourneeResumeDTO.class)
            .value(journees -> assertThat(journees).singleElement()
                .extracting(JourneeResumeDTO::solutionChoisieId).isEqualTo(solutionId));
    }

    @Test
    void refuseUnePeriodeIncoherente() {
        webClient.get().uri("/api/journees?debut=2026-07-31&fin=2026-07-01").exchange().expectStatus().isBadRequest();
        webClient.get().uri("/api/journees?debut=2026-01-01&fin=2027-06-01").exchange().expectStatus().isBadRequest();
        webClient.get().uri("/api/journees?debut=hier&fin=2026-07-01").exchange().expectStatus().isBadRequest();
    }

    @Test
    void consulterUneJourneeNeLaCreePas() {
        webClient.get().uri("/api/journees/2030-01-01").exchange().expectStatus().isOk();

        assertThat(journeeRepository.findById(LocalDate.of(2030, 1, 1))).isEmpty();
    }

    @Test
    void refuseDeRelancerUneOptimisationEnCours() {
        LocalDate date = LocalDate.of(2026, 7, 20);
        Long livraisonId = factory.createLivraison(1).getId();
        AdresseEntity depot = adresseRepository.save(AdresseEntity.builder()
            .reference("DEPOT").rue("Rue du Dépôt").ville("Grenoble").codePostal("38000")
            .coords(new Coords(45.18, 5.72)).build());
        parkingRepository.save(ParkingEntity.builder().adresse(depot).build());
        journeeRepository.save(JourneeEntity.builder().date(date).statutGeneration(StatutGeneration.GENERATION_ROUTE).build());

        webClient.post().uri("/api/journees/" + date + "/optimization")
            .bodyValue(new StrategieRequestDTO(1, "PT8H", Set.of(livraisonId)))
            .exchange()
            .expectStatus().isEqualTo(409);
    }

    @Test
    void refuseUneOptimisationSansLivraisonAPlanifier() {
        webClient.post().uri("/api/journees/2026-07-21/optimization")
            .bodyValue(new StrategieRequestDTO(1, "PT8H", Set.of(424242L)))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
