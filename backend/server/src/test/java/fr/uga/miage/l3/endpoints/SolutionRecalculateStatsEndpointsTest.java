package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@code POST /api/solutions/{solutionId}/recalculate-stats}.
 *
 * <p>The endpoint recomputes the statistics of every {@link TourneeEntity}
 * of a solution by calling {@link OpenRouteServiceClient#getMatrix} and
 * persists the new {@code distance}/{@code tempsTrajet} on each tournée.
 *
 * <p>These tests are expected to <strong>fail</strong> until the controller
 * method {@code SolutionController.recalculateSolutionStats} is implemented.
 */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SolutionRecalculateStatsEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory testDataFactory;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;

    @MockitoBean
    private OpenRouteServiceClient openRouteServiceClient;

    private final AtomicInteger refCounter = new AtomicInteger();

    @BeforeEach
    void setupOrsMock() {
        // Default ORS matrix response: 3x3 (parking + 2 livraisons), all symmetric.
        // distances are in meters, durations in seconds.
        MatrixResponse mockMatrix = new MatrixResponse();
        mockMatrix.distances = List.of(
            List.of(0,    1200, 800),
            List.of(1200, 0,    600),
            List.of(800,  600,  0)
        );
        mockMatrix.durations = List.of(
            List.of(0,   180, 120),
            List.of(180, 0,   90),
            List.of(120, 90,  0)
        );
        when(openRouteServiceClient.getMatrix(any(), any(), any())).thenReturn(mockMatrix);
    }

    @AfterEach
    void cleanup() {
        // FK-safe order: tournees → solutions → livraisons → adresses
        tourneeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    /**
     * Creates and saves an {@link AdresseEntity} populated with non-null
     * coordinates so the recalculation logic has real lat/lng to send
     * to the (mocked) ORS matrix call.
     */
    private AdresseEntity createAdresseWithCoords(double latitude, double longitude) {
        AdresseEntity adresse = new AdresseEntity();
        adresse.setReference("ADR-COORDS-" + refCounter.incrementAndGet());
        adresse.setRue("1 Rue de Test");
        adresse.setVille("Grenoble");
        adresse.setCodePostal("38000");
        adresse.setCoords(new Coords(latitude, longitude));
        return adresseRepository.save(adresse);
    }

    /**
     * Builds and saves a {@link LivraisonEntity} attached to an adresse
     * holding the given coordinates.
     */
    private LivraisonEntity createLivraisonAt(double latitude, double longitude) {
        AdresseEntity adresse = createAdresseWithCoords(latitude, longitude);
        LivraisonEntity livraison = LivraisonEntity.builder()
            .reference("LIV-COORDS-" + refCounter.incrementAndGet())
            .poids(10.0)
            .dateCommande(LocalDate.of(2026, 4, 15))
            .adresseEntity(adresse)
            .build();
        return livraisonRepository.save(livraison);
    }

    @Test
    void shouldRecalculateStats_whenSolutionExists() {
        // -- Arrange: a solution with one tournée containing 2 livraisons with real coords
        LivraisonEntity liv1 = createLivraisonAt(45.188, 5.724);
        LivraisonEntity liv2 = createLivraisonAt(45.190, 5.730);

        SolutionEntity solution = testDataFactory.createSolution(LocalDate.of(2026, 4, 19));
        TourneeEntity tournee = testDataFactory.createTournee(solution, List.of(liv1, liv2));

        // Sanity check on the seeded baseline (set by TestDataFactory.createTournee).
        Long initialDistance = tournee.getStatistic().getDistance();
        Duration initialTempsTrajet = tournee.getStatistic().getTempsTrajet();
        assertThat(initialDistance).isEqualTo(5000L);
        assertThat(initialTempsTrajet).isEqualTo(Duration.ofMinutes(30));

        // -- Act + Assert: POST returns 200 with a SolutionResponseDTO
        webClient.post()
            .uri("/api/solutions/{solutionId}/recalculate-stats", solution.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(SolutionResponseDTO.class)
            .consumeWith(result -> {
                SolutionResponseDTO body = Objects.requireNonNull(result.getResponseBody());
                assertThat(body.id()).isEqualTo(solution.getId());
                assertThat(body.tournees()).hasSize(1);

                TourneeResponsesDTO updated = body.tournees().iterator().next();
                assertThat(updated.id()).isEqualTo(tournee.getId());
                assertThat(updated.livraisons().stream().map(l -> l.id()).toList())
                    .containsExactly(liv1.getId(), liv2.getId());

                // The recalculated stats must be present (non-null) in the response.
                assertThat(updated.statistiques()).isNotNull();
                assertThat(updated.statistiques().distance())
                    .as("distance must be recomputed from the ORS matrix and non-null")
                    .isNotNull();
                assertThat(updated.statistiques().tempsTrajet())
                    .as("tempsTrajet must be recomputed from the ORS matrix and non-null/non-blank")
                    .isNotNull()
                    .isNotBlank();
            });

        // -- Persistence assertion: the tournée stored in DB has been updated.
        TourneeEntity reloaded = tourneeRepository.findById(tournee.getId()).orElseThrow();
        assertThat(reloaded.getStatistic()).isNotNull();
        assertThat(reloaded.getStatistic().getDistance()).isNotNull();
        assertThat(reloaded.getStatistic().getTempsTrajet()).isNotNull();
    }

    @Test
    void shouldReturn404_whenSolutionDoesNotExist() {
        long missingSolutionId = 999_999L;

        webClient.post()
            .uri("/api/solutions/{solutionId}/recalculate-stats", missingSolutionId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorDTO.class)
            .consumeWith(result -> {
                ErrorDTO body = Objects.requireNonNull(result.getResponseBody());
                assertThat(body.uri())
                    .isEqualTo("/api/solutions/" + missingSolutionId + "/recalculate-stats");
                assertThat(body.message()).isNotBlank();
            });
    }

    /**
     * Helper kept for symmetry with other tests — exercise the typed list builder.
     */
    @SuppressWarnings("unused")
    private static List<LivraisonEntity> mutableList(LivraisonEntity... items) {
        return new ArrayList<>(List.of(items));
    }
}
