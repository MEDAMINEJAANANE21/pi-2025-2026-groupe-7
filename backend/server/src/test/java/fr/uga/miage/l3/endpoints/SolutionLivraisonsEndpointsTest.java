package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import fr.uga.miage.l3.responses.SolutionLivraisonsResponseDTO;
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
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SolutionLivraisonsEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private JourneeRepository journeeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private TestDataFactory testDataFactory;

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: resolve child→parent dependencies.
        tourneeRepository.deleteAll();
        journeeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    @Nested
    class GetSolutionLivraisonsTest {

        /**
         * Happy path:
         * - 1 solution containing 2 tournées,
         *   - tournée #1 holds 2 livraisons,
         *   - tournée #2 holds 1 livraison.
         * - 2 additional non-livrées livraisons exist outside the solution.
         * Expectations:
         *   - dansSolution contains exactly the 3 livraisons of the solution's tournées.
         *   - horsSolution contains exactly the 2 unrelated non-livrées livraisons.
         */
        @Test
        void getSolutionLivraisons_returnsLivraisonsGrouped() {
            LocalDate date = LocalDate.of(2026, 4, 15);

            SolutionEntity solution = testDataFactory.createSolution(date);

            LivraisonEntity inSolution1 = testDataFactory.createLivraison(1); // poids = 10.0
            LivraisonEntity inSolution2 = testDataFactory.createLivraison(2); // poids = 20.0
            LivraisonEntity inSolution3 = testDataFactory.createLivraison(3); // poids = 30.0

            // Two tournées attached to the same solution
            testDataFactory.createTournee(solution, List.of(inSolution1, inSolution2));
            testDataFactory.createTournee(solution, List.of(inSolution3));

            // Two extra livraisons not assigned to any tournée of this solution
            LivraisonEntity outside1 = testDataFactory.createLivraison(4); // poids = 40.0
            LivraisonEntity outside2 = testDataFactory.createLivraison(5); // poids = 50.0

            webClient.get()
                .uri("/api/solutions/{solutionId}/livraisons", solution.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionLivraisonsResponseDTO.class)
                .consumeWith(result -> {
                    SolutionLivraisonsResponseDTO body = Objects.requireNonNull(result.getResponseBody());

                    assertThat(body.dansSolution()).isNotNull();
                    assertThat(body.horsSolution()).isNotNull();

                    // dansSolution should contain the 3 livraisons of the solution's tournées
                    assertThat(body.dansSolution())
                        .hasSize(3)
                        .extracting(LivraisonResponsesDTO::poids)
                        .containsExactlyInAnyOrder(
                            inSolution1.getPoids(),
                            inSolution2.getPoids(),
                            inSolution3.getPoids()
                        );

                    // horsSolution should contain only the 2 outside livraisons
                    assertThat(body.horsSolution())
                        .hasSize(2)
                        .extracting(LivraisonResponsesDTO::poids)
                        .containsExactlyInAnyOrder(
                            outside1.getPoids(),
                            outside2.getPoids()
                        );

                    // Address fields should be present and non-null for all returned livraisons
                    assertThat(body.dansSolution())
                        .allMatch(dto -> dto.adresse() != null
                            && "Grenoble".equals(dto.adresse().ville())
                            && "38000".equals(dto.adresse().codePostal()));
                    assertThat(body.horsSolution())
                        .allMatch(dto -> dto.adresse() != null
                            && "Grenoble".equals(dto.adresse().ville())
                            && "38000".equals(dto.adresse().codePostal()));
                });
        }

        /**
         * Unknown solutionId → 404 with ErrorDTO body.
         */
        @Test
        void getSolutionLivraisons_notFound() {
            long unknownId = 999_999L;

            webClient.get()
                .uri("/api/solutions/{solutionId}/livraisons", unknownId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .consumeWith(result -> {
                    ErrorDTO err = Objects.requireNonNull(result.getResponseBody());
                    assertThat(err.uri()).isEqualTo("/api/solutions/" + unknownId + "/livraisons");
                    assertThat(err.message()).isNotBlank();
                });
        }

        /**
         * Livraisons already livrée=true must NOT appear in horsSolution.
         */
        @Test
        void getSolutionLivraisons_excludesLivrees() {
            LocalDate date = LocalDate.of(2026, 4, 15);

            SolutionEntity solution = testDataFactory.createSolution(date);

            // One livraison inside the solution
            LivraisonEntity inSolution = testDataFactory.createLivraison(2); // poids = 20.0
            testDataFactory.createTournee(solution, List.of(inSolution));

            // One non-livrée livraison outside → should appear in horsSolution
            LivraisonEntity pendingOutside = testDataFactory.createLivraison(3); // poids = 30.0

            // One livrée=true livraison outside → must be excluded from horsSolution
            AdresseEntity deliveredAdresse = AdresseEntity.builder()
                .reference("ADR-DELIVERED")
                .rue("99 Rue Livrée")
                .codePostal("69000")
                .ville("Lyon")
                .build();
            adresseRepository.save(deliveredAdresse);

            LivraisonEntity delivered = LivraisonEntity.builder()
                .reference("LIV-DELIVERED")
                .poids(7.0)
                .livree(true)
                .dateCommande(LocalDate.of(2026, 4, 15))
                .adresseEntity(deliveredAdresse)
                .build();
            livraisonRepository.save(delivered);

            webClient.get()
                .uri("/api/solutions/{solutionId}/livraisons", solution.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionLivraisonsResponseDTO.class)
                .consumeWith(result -> {
                    SolutionLivraisonsResponseDTO body = Objects.requireNonNull(result.getResponseBody());

                    // dansSolution contains the in-solution livraison only
                    assertThat(body.dansSolution())
                        .hasSize(1)
                        .extracting(LivraisonResponsesDTO::poids)
                        .containsExactly(inSolution.getPoids());

                    // horsSolution must contain only the pending livraison, not the delivered one
                    assertThat(body.horsSolution())
                        .hasSize(1)
                        .extracting(LivraisonResponsesDTO::poids)
                        .containsExactly(pendingOutside.getPoids());

                    assertThat(body.horsSolution())
                        .noneMatch(dto -> dto.poids() == delivered.getPoids());
                });
        }
    }
}
