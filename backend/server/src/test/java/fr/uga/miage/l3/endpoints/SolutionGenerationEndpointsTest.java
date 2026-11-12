package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.fixtures.DatasetMatrixFixture;
import fr.uga.miage.l3.fixtures.DatasetMatrixFixture.DatasetFixtureData;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the solution-generation pipeline.
 *
 * <p>Strategy under test:
 * <ul>
 *   <li>POST {@code /api/journees/{date}/optimization} starts an asynchronous pipeline.</li>
 *   <li>The test polls {@code GET /api/journees/{date}} until
 *       {@code statutGeneration == "TERMINE"}.</li>
 *   <li>After completion, the generated solutions are fetched and verified: every
 *       livraison that was seeded must appear in at least one solution's tournée
 *       (checked via {@code nbLivraisonsTotal} in the statistics).</li>
 * </ul>
 *
 * <p>{@link OpenRouteServiceClient} is mocked:
 * <ul>
 *   <li>{@code getMatrix} – answers with data from the precomputed CSV matrix in
 *       {@code src/main/resources/datasets/}.</li>
 *   <li>{@code optimize} – mocked in every test suite to echo all supplied jobs back
 *       in a single route (used by the VROOM strategy variant).</li>
 * </ul>
 */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SolutionGenerationEndpointsTest {

    // -------------------------------------------------------------------------
    // Spring beans
    // -------------------------------------------------------------------------

    @Autowired private WebTestClient          webClient;
    @Autowired private JourneeRepository      journeeRepository;
    @Autowired private SolutionRepository     solutionRepository;
    @Autowired private TourneeRepository      tourneeRepository;
    @Autowired private LivraisonRepository    livraisonRepository;
    @Autowired private AdresseRepository      adresseRepository;
    @Autowired private ParkingRepository      parkingRepository;
    @Autowired private DatasetMatrixFixture   datasetFixture;

    @MockitoBean
    private OpenRouteServiceClient orsClient;

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order (child → parent)
        tourneeRepository.deleteAll();
        journeeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        parkingRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Nested test suites
    // -------------------------------------------------------------------------

    /**
     * Tests that a default request (nbVehicules=1, no explicit optimisation/separation)
     * always expands to exactly <em>three</em> strategy variants:
     * <ol>
     *   <li>VROOM / NONE / 1 vehicle</li>
     *   <li>GENETIC / NONE / 1 vehicle</li>
     *   <li>GREEDY / NONE / 1 vehicle</li>
     * </ol>
     * ANGULAR, KMEDOIDS and DBSCAN separations are skipped because they require
     * {@code nbVehicules > 1}.  Each strategy places all destinations in one group
     * (NONE separation), so every resulting solution must cover all livraisons.
     */
    @Nested
    class DefaultStrategyExpansionTest {

        private static final String DATE = "2026-04-16";
        private int expectedLivraisonCount;

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1);
            datasetFixture.configureMock(orsClient, data);
            expectedLivraisonCount = data.destinationCount(); // 50 livraisons

            // VROOM variant calls optimize(); echo all jobs back in a single route
            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
        }

        /**
         * Exactly 3 solutions (VROOM/NONE/1, GENETIC/NONE/1, GREEDY/NONE/1) must be
         * generated, and each must contain all 50 livraisons.
         */
        @Test
        void shouldGenerateOnePerStrategyVariantAllCoveringAllLivraisons() {
            // -- Start optimization --
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(1, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            // -- Poll until the background task finishes --
            await()
                    .atMost(Duration.ofSeconds(60))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        String status = getJourneeStatus(DATE);
                        assertThat(status).isEqualTo(StatutGeneration.TERMINE.name());
                    });

            // -- Fetch solutions and verify --
            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            // nbVehicules=1 → exactly 3 strategies: VROOM/NONE/1, GENETIC/NONE/1, GREEDY/NONE/1
            assertThat(solutions)
                    .as("Expected exactly 3 solutions (one per strategy variant)")
                    .hasSize(3);

            // Every solution must satisfy all structural invariants
            for (SolutionStatisticsResponseDTO sol : solutions) {
                String label = String.format("Solution %d (%s/%s)",
                        sol.id(),
                        sol.strategie() != null ? sol.strategie().separation()   : "?",
                        sol.strategie() != null ? sol.strategie().optimisation() : "?");

                // All livraisons must appear across the solution's tournées
                assertThat(sol.nbLivraisonsTotal())
                        .as("%s should cover all %d livraisons", label, expectedLivraisonCount)
                        .isEqualTo((long) expectedLivraisonCount);

                // Number of tournées must equal the strategy's requested vehicle count
                assertThat(sol.nbVehicules())
                        .as("%s should have exactly %d tournée(s) matching the strategy",
                                label, sol.strategie() != null ? sol.strategie().nbVehicules() : -1)
                        .isEqualTo(sol.strategie() != null ? sol.strategie().nbVehicules().longValue() : -1L);

                // Every individual tournée must contain at least 1 livraison (no empty routes)
                assertThat(sol.nbLivraisonsMin())
                        .as("%s: every tournée must have at least 1 livraison", label)
                        .isGreaterThanOrEqualTo(1);
            }
        }
    }

    /**
     * Repeats the same 3-strategy expansion scenario using a different dataset
     * ({@code 50-par-3-exemple-2}) to verify dataset-independence.
     * The same 3 strategies (VROOM/NONE/1, GENETIC/NONE/1, GREEDY/NONE/1) are
     * generated, and each solution must cover all livraisons.
     */
    @Nested
    class MultiStrategyTest {

        private static final String DATE = "2026-04-17";
        private int expectedLivraisonCount;

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_2);
            datasetFixture.configureMock(orsClient, data);
            expectedLivraisonCount = data.destinationCount();

            // Mock optimize() to return all supplied jobs in a single route
            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
        }

        /**
         * With nbVehicules=1 the pipeline always generates exactly 3 solutions.
         * Each solution must cover all livraisons regardless of which algorithm produced it.
         */
        @Test
        void shouldGenerateThreeSolutionsEachCoveringAllLivraisons() {
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(1, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            await()
                    .atMost(Duration.ofSeconds(90))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                            .isEqualTo(StatutGeneration.TERMINE.name()));

            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            // nbVehicules=1 → exactly 3 strategies: VROOM/NONE/1, GENETIC/NONE/1, GREEDY/NONE/1
            assertThat(solutions)
                    .as("Expected exactly 3 solutions (one per strategy variant)")
                    .hasSize(3);

            for (SolutionStatisticsResponseDTO sol : solutions) {
                String label = String.format("Solution %d (%s/%s)",
                        sol.id(),
                        sol.strategie() != null ? sol.strategie().separation()   : "?",
                        sol.strategie() != null ? sol.strategie().optimisation() : "?");

                // All livraisons must appear across the solution's tournées
                assertThat(sol.nbLivraisonsTotal())
                        .as("%s must cover all %d livraisons", label, expectedLivraisonCount)
                        .isEqualTo((long) expectedLivraisonCount);

                // Number of tournées must equal the strategy's requested vehicle count
                assertThat(sol.nbVehicules())
                        .as("%s should have exactly %d tournée(s) matching the strategy",
                                label, sol.strategie() != null ? sol.strategie().nbVehicules() : -1)
                        .isEqualTo(sol.strategie() != null ? sol.strategie().nbVehicules().longValue() : -1L);

                // Every individual tournée must contain at least 1 livraison (no empty routes)
                assertThat(sol.nbLivraisonsMin())
                        .as("%s: every tournée must have at least 1 livraison", label)
                        .isGreaterThanOrEqualTo(1);
            }
        }
    }

    /**
     * Tests that a request with {@code nbVehicules=3} produces exactly <em>17</em>
     * strategy variants:
     * <ul>
     *   <li>NONE    / VROOM   / 3, 2, 1  (3 solutions)</li>
     *   <li>NONE    / GENETIC / 1        (1 solution)</li>
     *   <li>NONE    / GREEDY  / 1        (1 solution)</li>
     *   <li>ANGULAR|KMEDOIDS / VROOM|GENETIC|GREEDY / 3, 2  (2 sep × 3 opt × 2 = 12)</li>
     * </ul>
     * Total: 3 + 1 + 1 + 12 = 17.
     *
     * <p>Note: DBSCAN is intentionally excluded from automatic strategy expansion because it
     * is density-based and ignores the {@code nbGroups} hint, collapsing to the same
     * clustering regardless of the requested vehicle count and producing non-deterministic
     * results. It can still be requested explicitly via the API.
     */
    @Nested
    class ThreeVehiclesStrategyExpansionTest {

        private static final String DATE = "2026-04-18";
        private static final int NB_VEHICULES = 3;
        private static final int EXPECTED_SOLUTIONS = 17;
        private int expectedLivraisonCount;

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_3);
            datasetFixture.configureMock(orsClient, data);
            expectedLivraisonCount = data.destinationCount();

            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
        }

        /**
         * With {@code nbVehicules=3}, exactly 17 solutions must be generated.
         * Each solution must cover all livraisons and respect its strategy's vehicle count.
         */
        @Test
        void shouldGenerate17SolutionsForThreeVehicles() {
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(NB_VEHICULES, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            await()
                    .atMost(Duration.ofSeconds(120))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                            .isEqualTo(StatutGeneration.TERMINE.name()));

            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            assertThat(solutions)
                    .as("Expected exactly %d solutions for nbVehicules=%d", EXPECTED_SOLUTIONS, NB_VEHICULES)
                    .hasSize(EXPECTED_SOLUTIONS);

            for (SolutionStatisticsResponseDTO sol : solutions) {
                String label = String.format("Solution %d (%s/%s/%d)",
                        sol.id(),
                        sol.strategie() != null ? sol.strategie().separation()   : "?",
                        sol.strategie() != null ? sol.strategie().optimisation() : "?",
                        sol.strategie() != null ? sol.strategie().nbVehicules()  : -1);

                assertThat(sol.nbLivraisonsTotal())
                        .as("%s should cover all %d livraisons", label, expectedLivraisonCount)
                        .isEqualTo((long) expectedLivraisonCount);

                assertThat(sol.nbVehicules())
                        .as("%s should have exactly %d tournée(s)", label,
                                sol.strategie() != null ? sol.strategie().nbVehicules() : -1)
                        .isEqualTo(sol.strategie() != null ? sol.strategie().nbVehicules().longValue() : -1L);

                assertThat(sol.nbLivraisonsMin())
                        .as("%s: every tournée must have at least 1 livraison", label)
                        .isGreaterThanOrEqualTo(1);
            }
        }
    }

    /**
     * Tests that a request with {@code nbVehicules=5} produces exactly <em>31</em>
     * strategy variants:
     * <ul>
     *   <li>NONE    / VROOM   / 5, 4, 3, 2, 1  (5 solutions)</li>
     *   <li>NONE    / GENETIC / 1              (1 solution)</li>
     *   <li>NONE    / GREEDY  / 1              (1 solution)</li>
     *   <li>ANGULAR|KMEDOIDS / VROOM|GENETIC|GREEDY / 5, 4, 3, 2  (2×3×4 = 24)</li>
     * </ul>
     * Total: 5 + 1 + 1 + 24 = 31.
     *
     * <p>Note: DBSCAN is intentionally excluded from automatic strategy expansion because it
     * is density-based and ignores the {@code nbGroups} hint, collapsing to the same
     * clustering regardless of the requested vehicle count and producing non-deterministic
     * results. It can still be requested explicitly via the API.
     */
    @Nested
    class FiveVehiclesStrategyExpansionTest {

        private static final String DATE = "2026-04-19";
        private static final int NB_VEHICULES = 5;
        private static final int EXPECTED_SOLUTIONS = 31;
        private int expectedLivraisonCount;

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1);
            datasetFixture.configureMock(orsClient, data);
            expectedLivraisonCount = data.destinationCount();

            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
        }

        /**
         * With {@code nbVehicules=5}, exactly 31 solutions must be generated.
         * Each solution must cover all livraisons and respect its strategy's vehicle count.
         */
        @Test
        void shouldGenerate31SolutionsForFiveVehicles() {
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(NB_VEHICULES, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            await()
                    .atMost(Duration.ofSeconds(180))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                            .isEqualTo(StatutGeneration.TERMINE.name()));

            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            assertThat(solutions)
                    .as("Expected exactly %d solutions for nbVehicules=%d", EXPECTED_SOLUTIONS, NB_VEHICULES)
                    .hasSize(EXPECTED_SOLUTIONS);

            for (SolutionStatisticsResponseDTO sol : solutions) {
                String label = String.format("Solution %d (%s/%s/%d)",
                        sol.id(),
                        sol.strategie() != null ? sol.strategie().separation()   : "?",
                        sol.strategie() != null ? sol.strategie().optimisation() : "?",
                        sol.strategie() != null ? sol.strategie().nbVehicules()  : -1);

                assertThat(sol.nbLivraisonsTotal())
                        .as("%s should cover all %d livraisons", label, expectedLivraisonCount)
                        .isEqualTo((long) expectedLivraisonCount);

                assertThat(sol.nbVehicules())
                        .as("%s should have exactly %d tournée(s)", label,
                                sol.strategie() != null ? sol.strategie().nbVehicules() : -1)
                        .isEqualTo(sol.strategie() != null ? sol.strategie().nbVehicules().longValue() : -1L);

                assertThat(sol.nbLivraisonsMin())
                        .as("%s: every tournée must have at least 1 livraison", label)
                        .isGreaterThanOrEqualTo(1);
            }
        }
    }

    /**
     * Tests resilience when <em>every</em> call to {@code ORS /optimize} responds with
     * an HTTP 429 (Too Many Requests) error.
     *
     * <p>With {@code nbVehicules=1} the pipeline attempts three strategies:
     * VROOM/NONE/1, GENETIC/NONE/1 and GREEDY/NONE/1.  Only the VROOM variant calls
     * {@code optimize()}; GENETIC and GREEDY use internal algorithms and are unaffected
     * by the ORS error.  Expected outcome:
     * <ul>
     *   <li>The journée reaches {@code TERMINE} (the pipeline always completes).</li>
     *   <li>Exactly <strong>2</strong> solutions are saved (GENETIC/NONE/1 and GREEDY/NONE/1).</li>
     *   <li>The failed VROOM/NONE/1 solution is <em>not</em> persisted.</li>
     * </ul>
     */
    @Nested
    class AllOrsOptimizeErrorTest {

        private static final String DATE = "2026-04-20";

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1);
            datasetFixture.configureMock(orsClient, data);

            // Every optimize() call returns a 429 HTTP error
            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatusCode.valueOf(429),
                            "Too Many Requests",
                            null, null, null));
        }

        /**
         * When all ORS optimize calls fail, only the 2 non-VROOM solutions
         * (GENETIC/NONE/1, GREEDY/NONE/1) must be saved, and the journée must
         * still finish with status {@code TERMINE}.
         */
        @Test
        void shouldSaveOnlyNonVroomSolutionsWhenAllOrsCallsFail() {
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(1, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            await()
                    .atMost(Duration.ofSeconds(60))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                            .isEqualTo(StatutGeneration.TERMINE.name()));

            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            // VROOM/NONE/1 failed → only GENETIC/NONE/1 and GREEDY/NONE/1 remain
            assertThat(solutions)
                    .as("Expected 2 solutions when all ORS optimize calls fail")
                    .hasSize(2);

            assertThat(solutions)
                    .as("No VROOM solution should have been saved")
                    .noneMatch(sol -> sol.strategie() != null
                            && "VROOM".equals(sol.strategie().optimisation()));
        }
    }

    /**
     * Tests resilience when only the <em>first</em> call to {@code ORS /optimize}
     * fails with HTTP 429 while subsequent calls succeed normally.
     *
     * <p>With {@code nbVehicules=3} the pipeline generates 17 strategies, 7 of which
     * are VROOM variants (and therefore call {@code optimize()}).  The first VROOM call
     * fails; the remaining 6 VROOM calls and all 10 non-VROOM strategies succeed.
     * Expected outcome:
     * <ul>
     *   <li>The journée reaches {@code TERMINE}.</li>
     *   <li>Exactly <strong>16</strong> solutions are saved (17 − 1 failed).</li>
     * </ul>
     */
    @Nested
    class PartialOrsOptimizeErrorTest {

        private static final String DATE = "2026-04-21";
        private static final int NB_VEHICULES = 3;
        /** 17 total strategies with nbVehicules=3; one VROOM call fails → 16 saved. */
        private static final int EXPECTED_SOLUTIONS = 16;

        @BeforeEach
        void setup() {
            DatasetFixtureData data =
                    datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_2);
            datasetFixture.configureMock(orsClient, data);

            HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                    HttpStatusCode.valueOf(429), "Too Many Requests", null, null, null);

            // First call throws, all subsequent calls return a valid result
            org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                    .thenThrow(tooManyRequests)
                    .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
        }

        /**
         * When only the first ORS optimize call fails, exactly 16 solutions
         * (out of 17) must be saved, and the journée must complete as {@code TERMINE}.
         */
        @Test
        void shouldSave16SolutionsWhenFirstOrsCallFails() {
            webClient.post()
                    .uri("/api/journees/" + DATE + "/optimization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new StrategieRequestDTO(NB_VEHICULES, "PT999H", allLivraisonIds()))
                    .exchange()
                    .expectStatus().isAccepted();

            await()
                    .atMost(Duration.ofSeconds(120))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                            .isEqualTo(StatutGeneration.TERMINE.name()));

            List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

            assertThat(solutions)
                    .as("Expected exactly %d solutions when one ORS call fails", EXPECTED_SOLUTIONS)
                    .hasSize(EXPECTED_SOLUTIONS);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Set<Long> allLivraisonIds() {
        return livraisonRepository.findAll().stream()
                .map(l -> l.getId())
                .collect(Collectors.toSet());
    }

    private String getJourneeStatus(String date) {
        JourneeResponseDTO dto = webClient.get()
                .uri("/api/journees/" + date)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JourneeResponseDTO.class)
                .returnResult()
                .getResponseBody();
        return dto != null ? dto.statutGeneration() : null;
    }

    private List<SolutionStatisticsResponseDTO> getSolutions(String date) {
        return webClient.get()
                .uri("/api/journees/" + date + "/solutions")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SolutionStatisticsResponseDTO.class)
                .returnResult()
                .getResponseBody();
    }

    /**
     * Builds a synthetic {@link fr.uga.miage.l3.models.core.OptimizationResult} from a
     * VROOM-style payload.
     *
     * <p>When the payload contains {@code N} vehicles the jobs are distributed as evenly as
     * possible across {@code N} routes so that downstream assertions on
     * {@code nbVehicules == strategie.nbVehicules()} always pass.
     *
     * <p>The payload is a {@code Map<String, Object>} with:
     * <ul>
     *   <li>{@code "jobs"}     – {@code List<Map<String,Object>>} each with {@code "id"} and
     *       {@code "location"}</li>
     *   <li>{@code "vehicles"} – {@code List<Map<String,Object>>} each with {@code "id"}</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private fr.uga.miage.l3.models.core.OptimizationResult buildVroomResultFromPayload(Object rawPayload) {
        var payload  = (java.util.Map<String, Object>) rawPayload;
        var jobs     = (java.util.List<java.util.Map<String, Object>>) payload.get("jobs");
        var vehicles = (java.util.List<java.util.Map<String, Object>>) payload.get("vehicles");

        int nbVehicles = (vehicles != null && !vehicles.isEmpty()) ? vehicles.size() : 1;

        // Distribute jobs round-robin across vehicles
        var jobsByVehicle = new java.util.ArrayList<java.util.List<java.util.Map<String, Object>>>();
        for (int v = 0; v < nbVehicles; v++) {
            jobsByVehicle.add(new java.util.ArrayList<>());
        }
        for (int i = 0; i < jobs.size(); i++) {
            jobsByVehicle.get(i % nbVehicles).add(jobs.get(i));
        }

        // Build one VehicleRoute per vehicle (skip vehicles with no assigned jobs)
        var routes = new java.util.ArrayList<fr.uga.miage.l3.models.core.VehicleRoute>();
        for (int v = 0; v < nbVehicles; v++) {
            var assignedJobs = jobsByVehicle.get(v);
            if (assignedJobs.isEmpty()) {
                continue; // VROOM never returns empty routes
            }
            int vehicleId = vehicles != null ? ((Number) vehicles.get(v).get("id")).intValue() : v + 1;

            var steps = new java.util.ArrayList<fr.uga.miage.l3.models.core.RouteStep>();
            steps.add(fr.uga.miage.l3.models.core.RouteStep.builder()
                    .type("start").arrival(0).location(new double[]{0, 0}).service(0).setup(0).build());

            for (var job : assignedJobs) {
                int    jobId      = ((Number) job.get("id")).intValue();
                double[] location = (double[]) job.get("location");
                steps.add(fr.uga.miage.l3.models.core.RouteStep.builder()
                        .type("job").id(jobId).arrival(0)
                        .location(location).service(300).setup(30).build());
            }

            steps.add(fr.uga.miage.l3.models.core.RouteStep.builder()
                    .type("end").arrival(0).location(new double[]{0, 0}).service(0).setup(0).build());

            routes.add(fr.uga.miage.l3.models.core.VehicleRoute.builder()
                    .cost(0).duration(0).priority(0)
                    .service(assignedJobs.size() * 300).setup(assignedJobs.size() * 30)
                    .vehicle(vehicleId)
                    .violations(java.util.Collections.emptyList())
                    .waiting_time(0).steps(steps).build());
        }

        var summary = fr.uga.miage.l3.models.core.Summary.builder()
                .computing_times(fr.uga.miage.l3.models.core.ComputingTimes.builder()
                        .loading(0).solving(0).routing(0).build())
                .cost(0).duration(0).priority(0)
                .service(jobs.size() * 300).setup(jobs.size() * 30)
                .violations(java.util.Collections.emptyList()).waiting_time(0).build();

        return fr.uga.miage.l3.models.core.OptimizationResult.builder()
                .code(0)
                .routes(routes)
                .unassigned(java.util.Collections.emptyList())
                .summary(summary)
                .build();
    }
}

