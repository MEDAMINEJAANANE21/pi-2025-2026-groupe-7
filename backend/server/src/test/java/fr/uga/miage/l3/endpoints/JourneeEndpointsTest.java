package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.responses.StrategieResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class JourneeEndpointsTest {
    @Autowired private WebTestClient webClient;
    @Autowired private JourneeRepository journeeRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private TestDataFactory testDataFactory;

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: resolve child→parent dependencies.
        // tourneeRepository.deleteAll() also removes ManyToMany join-table rows via Hibernate.
        tourneeRepository.deleteAll();
        journeeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    @Nested
    class GetJourneeTest {
        @Test
        void testJourneeNonExisting() {
            JourneeResponseDTO expected = new JourneeResponseDTO(
                "2026-04-15",
                null,
                null
            );
            webClient.get()
                .uri("/api/journees/2026-04-15")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JourneeResponseDTO.class)
                .isEqualTo(expected);
        }

        @Test
        void testJourneeExisting() {
            JourneeEntity journeeEntity = journeeRepository.save(JourneeEntity.builder()
                .date(LocalDate.of(2026, 4, 15))
                .statutGeneration(StatutGeneration.INITIALISATION)
                .build());

            JourneeResponseDTO expected = new JourneeResponseDTO(
                "2026-04-15",
                "INITIALISATION",
                null
            );
            webClient.get()
                .uri("/api/journees/2026-04-15")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JourneeResponseDTO.class)
                .isEqualTo(expected);
        }

        @Test
        void testInvalidDate() {
            ErrorDTO expected = new ErrorDTO(
                "/api/journees/toto",
                "Invalid date format, expected yyyy-MM-dd"
            );
            webClient.get()
                .uri("/api/journees/toto")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class)
                .isEqualTo(expected);
        }
    }

    @Nested
    class GetJourneeSolutionsTest {
        JourneeEntity journee;

        @BeforeEach
        void setup() {
            journee = journeeRepository.save(JourneeEntity.builder()
                .date(LocalDate.of(2026, 4, 15))
                .statutGeneration(StatutGeneration.INITIALISATION)
                .build());
        }

        @Test
        void testJourneeSolutionsNonExisting() {
            webClient.get()
                .uri("/api/journees/2026-04-15/solutions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SolutionStatisticsResponseDTO.class)
                .hasSize(0);
        }

        @Test
        void testJourneeSolutionsWithOneSolution() {
            // 1 solution • 1 tournée • 2 livraisons each with 2 commandes (same adresse per livraison)
            SolutionEntity solution = testDataFactory.createSolution(LocalDate.of(2026, 4, 15));
            List<LivraisonEntity> livraisons = List.of(
                testDataFactory.createLivraison(2),
                testDataFactory.createLivraison(2)
            );
            testDataFactory.createTournee(solution, livraisons);

            // Expected statistics for 1 tournée:
            //   nbVehicules=1 (strategie), nbLivraisonsTotal=2, poids=4×10.0=40.0
            //   carburant=100, distance=5000, tempsTrajet=PT30M, tempsMontage=PT15M
            webClient.get()
                .uri("/api/journees/2026-04-15/solutions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SolutionStatisticsResponseDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    SolutionStatisticsResponseDTO dto = result.getResponseBody().get(0);
                    assertThat(dto.id()).isEqualTo(solution.getId());
                    assertThat(dto.date()).isEqualTo("2026-04-15");
                    assertThat(dto.strategie()).isEqualTo(new StrategieResponseDTO("NONE", "GREEDY", 1, null));
                    assertThat(dto.nbVehicules()).isEqualTo(1L);
                    assertThat(dto.nbLivraisonsTotal()).isEqualTo(2L);
                    assertThat(dto.nbLivraisonsMin()).isEqualTo(2);
                    assertThat(dto.nbLivraisonsMax()).isEqualTo(2);
                    assertThat(dto.nbLivraisonsMoyenne()).isEqualTo(2.0);
                    assertThat(dto.carburantTotal()).isEqualTo(100L);
                    assertThat(dto.carburantMin()).isEqualTo(100L);
                    assertThat(dto.carburantMax()).isEqualTo(100L);
                    assertThat(dto.carburantMoyenne()).isEqualTo(100.0);
                    assertThat(dto.distanceTotal()).isEqualTo(5000L);
                    assertThat(dto.distanceMin()).isEqualTo(5000L);
                    assertThat(dto.distanceMax()).isEqualTo(5000L);
                    assertThat(dto.distanceMoyenne()).isEqualTo(5000.0);
                    assertThat(dto.poidsTotal()).isEqualTo(40.0);
                    assertThat(dto.poidsMin()).isEqualTo(40.0);
                    assertThat(dto.poidsMax()).isEqualTo(40.0);
                    assertThat(dto.poidsMoyenne()).isEqualTo(40.0);
                    assertThat(dto.tempsRouteTotal()).isEqualTo("PT30M");
                    assertThat(dto.tempsRouteMin()).isEqualTo("PT30M");
                    assertThat(dto.tempsRouteMax()).isEqualTo("PT30M");
                    assertThat(dto.tempsRouteMoyenne()).isEqualTo("PT30M");
                    assertThat(dto.tempsMontageTotal()).isEqualTo("PT15M");
                    assertThat(dto.tempsMontageMin()).isEqualTo("PT15M");
                    assertThat(dto.tempsMontageMax()).isEqualTo("PT15M");
                    assertThat(dto.tempsMontageMoyenne()).isEqualTo("PT15M");
                    assertThat(dto.tempsTotal()).isEqualTo("PT45M");
                    assertThat(dto.tempsTotalMin()).isEqualTo("PT45M");
                    assertThat(dto.tempsTotalMax()).isEqualTo("PT45M");
                    assertThat(dto.tempsTotalMoyenne()).isEqualTo("PT45M");
                });
        }

        @Test
        void testJourneeSolutionsWithMultipleSolutionsSharingLivraisons() {
            // 2 livraisons shared across both solutions — validates ManyToMany reuse
            List<LivraisonEntity> sharedLivraisons = List.of(
                testDataFactory.createLivraison(2),
                testDataFactory.createLivraison(2)
            );

            SolutionEntity solution1 = testDataFactory.createSolution(LocalDate.of(2026, 4, 15));
            testDataFactory.createTournee(solution1, sharedLivraisons);

            SolutionEntity solution2 = testDataFactory.createSolution(LocalDate.of(2026, 4, 15));
            testDataFactory.createTournee(solution2, sharedLivraisons);

            webClient.get()
                .uri("/api/journees/2026-04-15/solutions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SolutionStatisticsResponseDTO.class)
                .hasSize(2)
                .consumeWith(result -> result.getResponseBody().forEach(dto -> {
                    assertThat(dto.nbLivraisonsTotal()).isEqualTo(2L);
                    assertThat(dto.carburantTotal()).isEqualTo(100L);
                    assertThat(dto.distanceTotal()).isEqualTo(5000L);
                    assertThat(dto.poidsTotal()).isEqualTo(40.0);
                }));
        }
    }


}
