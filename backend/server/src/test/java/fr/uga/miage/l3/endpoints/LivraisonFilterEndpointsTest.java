package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Failing tests for the new optional query parameters on
 * {@code GET /api/livraisons}: {@code livree}, {@code dateDebut}, {@code dateFin}.
 *
 * <p>References: issues #52, #53, #54 (Refs #19).
 *
 * <p>The endpoint interface declares these {@link org.springframework.web.bind.annotation.RequestParam}
 * parameters but {@code LivraisonController#getLivraisons()} does not yet implement
 * the new signature, so this test class is expected to fail at compile time of the
 * production sources until the controller is updated.
 */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class LivraisonFilterEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private JourneeRepository journeeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private TestDataFactory testDataFactory;

    private final AtomicInteger refCounter = new AtomicInteger(0);

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: child → parent.
        tourneeRepository.deleteAll();
        journeeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    /**
     * Persists a {@link LivraisonEntity} with the given delivery flag and order date.
     * Each livraison gets its own freshly-created adresse to satisfy the
     * {@code MissingAdresseException} guard in {@code LivraisonComponent}.
     */
    private LivraisonEntity persistLivraison(boolean livree, LocalDate dateCommande) {
        AdresseEntity adresse = new AdresseEntity();
        int id = refCounter.incrementAndGet();
        adresse.setReference("ADR-FILTER-" + id);
        adresse.setRue("1 Rue de Test");
        adresse.setVille("Grenoble");
        adresse.setCodePostal("38000");
        adresse = adresseRepository.save(adresse);

        LivraisonEntity livraison = LivraisonEntity.builder()
            .reference("LIV-FILTER-" + id)
            .poids(10.0)
            .livree(livree)
            .dateCommande(dateCommande)
            .adresseEntity(adresse)
            .build();
        return livraisonRepository.save(livraison);
    }

    @Nested
    class GetLivraisonsFilterTest {

        @Test
        void testGetLivraisons_noFilter() {
            // Given: 2 livraisons, both not delivered (default backward-compat behaviour
            // is to return only non-delivered livraisons when no params are provided).
            LivraisonEntity l1 = persistLivraison(false, LocalDate.of(2026, 4, 15));
            LivraisonEntity l2 = persistLivraison(false, LocalDate.of(2026, 5, 20));

            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(2)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    // The id field must be present (non-null) on every returned item
                    assertThat(dtos).allMatch(dto -> dto.id() != null);
                    // And every persisted id must be present in the response
                    assertThat(dtos)
                        .extracting(LivraisonResponsesDTO::id)
                        .containsExactlyInAnyOrder(l1.getId(), l2.getId());
                });
        }

        @Test
        void testGetLivraisons_filterByLivreeFalse() {
            LivraisonEntity nonLivree = persistLivraison(false, LocalDate.of(2026, 4, 15));
            persistLivraison(true, LocalDate.of(2026, 4, 15));

            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/livraisons")
                    .queryParam("livree", "false")
                    .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).id()).isEqualTo(nonLivree.getId());
                });
        }

        @Test
        void testGetLivraisons_filterByLivreeTrue() {
            persistLivraison(false, LocalDate.of(2026, 4, 15));
            LivraisonEntity livree = persistLivraison(true, LocalDate.of(2026, 4, 15));

            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/livraisons")
                    .queryParam("livree", "true")
                    .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).id()).isEqualTo(livree.getId());
                });
        }

        @Test
        void testGetLivraisons_filterByDateDebut() {
            persistLivraison(false, LocalDate.of(2026, 1, 15));
            LivraisonEntity afterCutoff = persistLivraison(false, LocalDate.of(2026, 3, 1));

            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/livraisons")
                    .queryParam("dateDebut", "2026-02-01")
                    .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).id()).isEqualTo(afterCutoff.getId());
                });
        }

        @Test
        void testGetLivraisons_filterByDateFin() {
            LivraisonEntity beforeCutoff = persistLivraison(false, LocalDate.of(2026, 1, 15));
            persistLivraison(false, LocalDate.of(2026, 3, 1));

            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/livraisons")
                    .queryParam("dateFin", "2026-02-01")
                    .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).id()).isEqualTo(beforeCutoff.getId());
                });
        }

        @Test
        void testGetLivraisons_filterByDateRange() {
            persistLivraison(false, LocalDate.of(2026, 1, 1));
            LivraisonEntity inRange = persistLivraison(false, LocalDate.of(2026, 6, 1));
            persistLivraison(false, LocalDate.of(2026, 12, 1));

            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/livraisons")
                    .queryParam("dateDebut", "2026-02-01")
                    .queryParam("dateFin", "2026-11-01")
                    .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).id()).isEqualTo(inRange.getId());
                });
        }
    }
}
