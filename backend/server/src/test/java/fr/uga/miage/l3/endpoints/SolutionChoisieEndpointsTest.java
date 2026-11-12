package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SolutionChoisieEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private JourneeRepository journeeRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private TestDataFactory testDataFactory;

    @AfterEach
    void cleanup() {
        // Detach the chosen-solution OneToOne pointer first so the FK doesn't block
        // SolutionEntity removal when the JourneeEntity gets deleted afterwards.
        journeeRepository.findAll().forEach(j -> {
            j.setSolutionEntity(null);
            journeeRepository.save(j);
        });
        // FK-safe order: tournees → journees → solutions → livraisons → adresses
        tourneeRepository.deleteAll();
        journeeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    @Nested
    class ChoisirSolutionTest {

        @Test
        void testChoisirSolution_solutionExistante() {
            // arrange — a saved solution for 2026-04-20 (no journée yet)
            SolutionEntity solution = testDataFactory.createSolution(LocalDate.of(2026, 4, 20));

            // act + assert — PUT /api/solutions/{id}/choisie returns 200 and the SolutionResponseDTO
            webClient.put()
                .uri("/api/solutions/{id}/choisie", solution.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionResponseDTO.class)
                .consumeWith(result -> {
                    SolutionResponseDTO dto = result.getResponseBody();
                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(solution.getId());
                    assertThat(dto.date()).isEqualTo("2026-04-20");
                    assertThat(dto.strategie()).isNotNull();
                    assertThat(dto.strategie().separation()).isEqualTo("NONE");
                    assertThat(dto.strategie().optimisation()).isEqualTo("GREEDY");
                    assertThat(dto.strategie().nbVehicules()).isEqualTo(1);
                });

            // and the JourneeEntity for that date now references the chosen solution
            JourneeEntity journee = journeeRepository.findById(LocalDate.of(2026, 4, 20)).orElse(null);
            assertThat(journee).isNotNull();
            assertThat(journee.getSolutionEntity()).isNotNull();
            assertThat(journee.getSolutionEntity().getId()).isEqualTo(solution.getId());
        }

        @Test
        void testChoisirSolution_solutionInexistante() {
            ErrorDTO expected = new ErrorDTO(
                "/api/solutions/99999/choisie",
                "Solution introuvable : 99999"
            );
            webClient.put()
                .uri("/api/solutions/99999/choisie")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .isEqualTo(expected);
        }
    }

    @Nested
    class GetSolutionChoisieTest {

        @Test
        void testGetSolutionChoisie_existante() {
            // arrange — a solution for 2026-04-21 explicitly set as the chosen one on its journée
            SolutionEntity solution = testDataFactory.createSolution(LocalDate.of(2026, 4, 21));
            JourneeEntity journee = JourneeEntity.builder()
                .date(LocalDate.of(2026, 4, 21))
                .statutGeneration(StatutGeneration.INITIALISATION)
                .solutionEntity(solution)
                .build();
            journeeRepository.save(journee);

            // act + assert
            webClient.get()
                .uri("/api/journees/2026-04-21/solutions/choisie")
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionResponseDTO.class)
                .consumeWith(result -> {
                    SolutionResponseDTO dto = result.getResponseBody();
                    assertThat(dto).isNotNull();
                    assertThat(dto.id()).isEqualTo(solution.getId());
                    assertThat(dto.date()).isEqualTo("2026-04-21");
                    assertThat(dto.strategie()).isNotNull();
                    assertThat(dto.strategie().separation()).isEqualTo("NONE");
                    assertThat(dto.strategie().optimisation()).isEqualTo("GREEDY");
                    assertThat(dto.strategie().nbVehicules()).isEqualTo(1);
                });
        }

        @Test
        void testGetSolutionChoisie_aucuneChoisie() {
            // arrange — a journée exists but no chosen solution attached
            journeeRepository.save(JourneeEntity.builder()
                .date(LocalDate.of(2026, 4, 22))
                .statutGeneration(StatutGeneration.INITIALISATION)
                .solutionEntity(null)
                .build());

            ErrorDTO expected = new ErrorDTO(
                "/api/journees/2026-04-22/solutions/choisie",
                "Aucune solution choisie pour cette journée"
            );
            webClient.get()
                .uri("/api/journees/2026-04-22/solutions/choisie")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .isEqualTo(expected);
        }

        @Test
        void testGetSolutionChoisie_journeeInexistante() {
            // arrange — no journée at all for 2026-04-23
            ErrorDTO expected = new ErrorDTO(
                "/api/journees/2026-04-23/solutions/choisie",
                "Aucune solution choisie pour cette journée"
            );
            webClient.get()
                .uri("/api/journees/2026-04-23/solutions/choisie")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .isEqualTo(expected);
        }
    }
}
