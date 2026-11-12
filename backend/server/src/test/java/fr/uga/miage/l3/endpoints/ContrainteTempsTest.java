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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for issue #78 — enforcement of {@code maxTempsParVehicule}.
 *
 * <p>When the optimisation pipeline receives a {@code maxTempsParVehicule}
 * smaller than the actual {@code tempsTrajet} of every realistic tournée,
 * the post-processing step must exclude livraisons from the resulting solutions
 * so that no tournée exceeds the requested time budget.
 *
 * <p>This test is expected to <strong>fail</strong> until the post-processing
 * defined by issue #78 is implemented: today the pipeline always returns
 * tournées containing every requested livraison, regardless of the time budget.
 */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ContrainteTempsTest {

    private static final String DATE = "2026-04-22";

    // -------------------------------------------------------------------------
    // Spring beans
    // -------------------------------------------------------------------------

    @Autowired private WebTestClient        webClient;
    @Autowired private JourneeRepository    journeeRepository;
    @Autowired private SolutionRepository   solutionRepository;
    @Autowired private TourneeRepository    tourneeRepository;
    @Autowired private LivraisonRepository  livraisonRepository;
    @Autowired private AdresseRepository    adresseRepository;
    @Autowired private ParkingRepository    parkingRepository;
    @Autowired private DatasetMatrixFixture datasetFixture;

    @MockitoBean
    private OpenRouteServiceClient orsClient;

    private int expectedLivraisonCount;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setup() {
        DatasetFixtureData data =
                datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_2);
        datasetFixture.configureMock(orsClient, data);
        expectedLivraisonCount = data.destinationCount();

        // VROOM variant calls optimize(); echo all jobs back in a single route
        org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));
    }

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
    // Test
    // -------------------------------------------------------------------------

    /**
     * With {@code maxTempsParVehicule = "PT1S"}, the realistic distances drawn
     * from the dataset CSV guarantee that every tournée's {@code tempsTrajet}
     * exceeds 1 second.
     *
     * <p>The post-processing required by issue #78 must therefore exclude
     * at least one livraison from at least one solution so that the resulting
     * tournées respect the time budget.  Because the post-processing does not
     * exist yet, every solution is currently saved with all livraisons attached
     * and {@code nbLivraisonsTotal} equals the input count, making this
     * assertion fail.
     */
    @Test
    void shouldExcludeLivraisonsWhenMaxTempsBudgetIsExceeded() {
        Set<Long> livraisonIds = allLivraisonIds();

        // -- Start optimization with an absurdly small time budget --
        webClient.post()
                .uri("/api/journees/" + DATE + "/optimization")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StrategieRequestDTO(1, "PT1S", livraisonIds))
                .exchange()
                .expectStatus().isAccepted();

        // -- Poll until the background task finishes --
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                        .isEqualTo(StatutGeneration.TERMINE.name()));

        // -- Fetch solutions --
        List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

        assertThat(solutions)
                .as("At least one solution must be produced by the pipeline")
                .isNotEmpty();

        // -- Assertion that must FAIL today (issue #78 not yet implemented) --
        // The post-processing should remove livraisons that don't fit the
        // time budget so at least one solution covers strictly fewer
        // livraisons than were initially requested.
        long requestedCount = livraisonIds.size();
        assertThat(solutions)
                .as("With maxTempsParVehicule=PT1S, the post-processing of issue #78 "
                        + "should produce at least one solution where some livraisons "
                        + "have been excluded for exceeding the time budget. "
                        + "Requested %d livraisons.", requestedCount)
                .anyMatch(sol -> sol.nbLivraisonsTotal() != null
                        && sol.nbLivraisonsTotal() < requestedCount);
    }

    /**
     * With a large {@code maxTempsParVehicule} (PT999H), post-processing must
     * NOT remove any livraison — all requested livraisons must appear in every solution.
     */
    @Test
    void shouldPreserveAllLivraisonsWhenBudgetIsSufficient() {
        Set<Long> livraisonIds = allLivraisonIds();
        long requestedCount = livraisonIds.size();

        webClient.post()
                .uri("/api/journees/" + DATE + "/optimization")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StrategieRequestDTO(1, "PT999H", livraisonIds))
                .exchange()
                .expectStatus().isAccepted();

        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(getJourneeStatus(DATE))
                        .isEqualTo(StatutGeneration.TERMINE.name()));

        List<SolutionStatisticsResponseDTO> solutions = getSolutions(DATE);

        assertThat(solutions)
                .as("At least one solution must be produced")
                .isNotEmpty();

        assertThat(solutions)
                .as("With PT999H budget, no livraison should be excluded — "
                        + "every solution must cover all %d requested livraisons", requestedCount)
                .allMatch(sol -> sol.nbLivraisonsTotal() != null
                        && sol.nbLivraisonsTotal() == requestedCount);
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
     * Builds a synthetic {@link fr.uga.miage.l3.models.core.OptimizationResult}
     * from a VROOM-style payload (jobs distributed round-robin over vehicles).
     * Adapted from {@code SolutionGenerationEndpointsTest}.
     */
    @SuppressWarnings("unchecked")
    private fr.uga.miage.l3.models.core.OptimizationResult buildVroomResultFromPayload(Object rawPayload) {
        var payload  = (java.util.Map<String, Object>) rawPayload;
        var jobs     = (java.util.List<java.util.Map<String, Object>>) payload.get("jobs");
        var vehicles = (java.util.List<java.util.Map<String, Object>>) payload.get("vehicles");

        int nbVehicles = (vehicles != null && !vehicles.isEmpty()) ? vehicles.size() : 1;

        var jobsByVehicle = new java.util.ArrayList<java.util.List<java.util.Map<String, Object>>>();
        for (int v = 0; v < nbVehicles; v++) {
            jobsByVehicle.add(new java.util.ArrayList<>());
        }
        for (int i = 0; i < jobs.size(); i++) {
            jobsByVehicle.get(i % nbVehicles).add(jobs.get(i));
        }

        var routes = new java.util.ArrayList<fr.uga.miage.l3.models.core.VehicleRoute>();
        for (int v = 0; v < nbVehicles; v++) {
            var assignedJobs = jobsByVehicle.get(v);
            if (assignedJobs.isEmpty()) {
                continue;
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
