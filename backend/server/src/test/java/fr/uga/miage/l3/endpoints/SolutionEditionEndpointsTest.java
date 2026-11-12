package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.SolutionTourneesUpdateRequest;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SolutionEditionEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory testDataFactory;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: resolve child→parent dependencies.
        // tourneeRepository.deleteAll() also removes ManyToMany join-table rows via Hibernate.
        tourneeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    @Nested
    class UpdateSolutionTourneesTest {

        private SolutionEntity solution;
        private TourneeEntity tournee1;
        private TourneeEntity tournee2;
        private LivraisonEntity liv1;
        private LivraisonEntity liv2;
        private LivraisonEntity liv3;
        private LivraisonEntity liv4;
        private LivraisonEntity liv5;

        @BeforeEach
        void setup() {
            // Create a solution with 2 tournees:
            // - tournee1 contains [liv1, liv2, liv3]
            // - tournee2 contains [liv4, liv5]
            liv1 = testDataFactory.createLivraison(1);
            liv2 = testDataFactory.createLivraison(1);
            liv3 = testDataFactory.createLivraison(1);
            liv4 = testDataFactory.createLivraison(1);
            liv5 = testDataFactory.createLivraison(1);

            solution = testDataFactory.createSolution(LocalDate.of(2026, 4, 19));
            tournee1 = testDataFactory.createTournee(solution, List.of(liv1, liv2, liv3));
            tournee2 = testDataFactory.createTournee(solution, List.of(liv4, liv5));
        }

        @Test
        void shouldSwapLivraisonsBetweenTournees() {
            // Swap: tournee1 gets [liv4, liv5, liv1], tournee2 gets [liv2, liv3]
            SolutionTourneesUpdateRequest request = new SolutionTourneesUpdateRequest(List.of(
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee1.getId(),
                    List.of(liv4.getId(), liv5.getId(), liv1.getId())
                ),
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee2.getId(),
                    List.of(liv2.getId(), liv3.getId())
                )
            ));

            webClient.put()
                .uri("/api/solutions/{solutionId}/tournees", solution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionResponseDTO.class)
                .consumeWith(result -> {
                    SolutionResponseDTO body = Objects.requireNonNull(result.getResponseBody());
                    assertThat(body.id()).isEqualTo(solution.getId());
                    assertThat(body.tournees()).hasSize(2);

                    TourneeResponsesDTO updatedT1 = body.tournees().stream()
                        .filter(t -> t.id().equals(tournee1.getId()))
                        .findFirst()
                        .orElseThrow();
                    TourneeResponsesDTO updatedT2 = body.tournees().stream()
                        .filter(t -> t.id().equals(tournee2.getId()))
                        .findFirst()
                        .orElseThrow();

                    assertThat(updatedT1.livraisons().stream().map(l -> l.id()).toList())
                        .containsExactly(liv4.getId(), liv5.getId(), liv1.getId());
                    assertThat(updatedT2.livraisons().stream().map(l -> l.id()).toList())
                        .containsExactly(liv2.getId(), liv3.getId());
                });
        }

        @Test
        void shouldReorderLivraisonsWithinATournee() {
            // Reverse the order of livraisons in tournee1 only: [liv3, liv2, liv1]
            SolutionTourneesUpdateRequest request = new SolutionTourneesUpdateRequest(List.of(
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee1.getId(),
                    List.of(liv3.getId(), liv2.getId(), liv1.getId())
                ),
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee2.getId(),
                    List.of(liv4.getId(), liv5.getId())
                )
            ));

            webClient.put()
                .uri("/api/solutions/{solutionId}/tournees", solution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolutionResponseDTO.class)
                .consumeWith(result -> {
                    SolutionResponseDTO body = Objects.requireNonNull(result.getResponseBody());
                    assertThat(body.tournees()).hasSize(2);

                    TourneeResponsesDTO updatedT1 = body.tournees().stream()
                        .filter(t -> t.id().equals(tournee1.getId()))
                        .findFirst()
                        .orElseThrow();
                    TourneeResponsesDTO updatedT2 = body.tournees().stream()
                        .filter(t -> t.id().equals(tournee2.getId()))
                        .findFirst()
                        .orElseThrow();

                    assertThat(updatedT1.livraisons().stream().map(l -> l.id()).toList())
                        .containsExactly(liv3.getId(), liv2.getId(), liv1.getId());
                    assertThat(updatedT2.livraisons().stream().map(l -> l.id()).toList())
                        .containsExactly(liv4.getId(), liv5.getId());
                });
        }

        @Test
        void shouldReturn404WhenSolutionNotFound() {
            long missingSolutionId = 999_999L;

            SolutionTourneesUpdateRequest request = new SolutionTourneesUpdateRequest(List.of(
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee1.getId(),
                    List.of(liv1.getId(), liv2.getId(), liv3.getId())
                )
            ));

            webClient.put()
                .uri("/api/solutions/{solutionId}/tournees", missingSolutionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .consumeWith(result -> {
                    ErrorDTO body = Objects.requireNonNull(result.getResponseBody());
                    assertThat(body.uri())
                        .isEqualTo("/api/solutions/" + missingSolutionId + "/tournees");
                    assertThat(body.message()).isNotBlank();
                });
        }

        @Test
        void shouldReturn404WhenTourneeNotFound() {
            long missingTourneeId = 999_999L;

            SolutionTourneesUpdateRequest request = new SolutionTourneesUpdateRequest(List.of(
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    missingTourneeId,
                    List.of(liv1.getId(), liv2.getId())
                )
            ));

            webClient.put()
                .uri("/api/solutions/{solutionId}/tournees", solution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .consumeWith(result -> {
                    ErrorDTO body = Objects.requireNonNull(result.getResponseBody());
                    assertThat(body.uri())
                        .isEqualTo("/api/solutions/" + solution.getId() + "/tournees");
                    assertThat(body.message()).isNotBlank();
                });
        }

        @Test
        void shouldReturn400WhenLivraisonNotFound() {
            long missingLivraisonId = 999_999L;

            SolutionTourneesUpdateRequest request = new SolutionTourneesUpdateRequest(List.of(
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee1.getId(),
                    List.of(liv1.getId(), missingLivraisonId, liv3.getId())
                ),
                new SolutionTourneesUpdateRequest.TourneeUpdateDTO(
                    tournee2.getId(),
                    List.of(liv4.getId(), liv5.getId())
                )
            ));

            webClient.put()
                .uri("/api/solutions/{solutionId}/tournees", solution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().value(status ->
                    assertThat(status).isIn(400, 404)
                )
                .expectBody(ErrorDTO.class)
                .consumeWith(result -> {
                    ErrorDTO body = Objects.requireNonNull(result.getResponseBody());
                    assertThat(body.uri())
                        .isEqualTo("/api/solutions/" + solution.getId() + "/tournees");
                    assertThat(body.message()).isNotBlank();
                });
        }
    }
}
