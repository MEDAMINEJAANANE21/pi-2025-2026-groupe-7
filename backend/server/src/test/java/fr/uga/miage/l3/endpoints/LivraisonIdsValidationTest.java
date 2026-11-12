package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.fixtures.DatasetMatrixFixture;
import fr.uga.miage.l3.fixtures.DatasetMatrixFixture.DatasetFixtureData;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests dedicated to the new mandatory {@code livraisonIds} field on
 * {@code POST /api/journees/{date}/optimization}.
 *
 * <p>Two cases:
 * <ul>
 *   <li>Body without {@code livraisonIds} → 400 Bad Request (Bean Validation
 *       fails because the field is now {@code @NotNull}).</li>
 *   <li>Body with valid {@code livraisonIds} → 202 Accepted, and every saved
 *       {@link SolutionEntity} stores the requested set of IDs in its
 *       {@code livraisonsIds} column.</li>
 * </ul>
 *
 * <p>{@link OpenRouteServiceClient} is fully mocked through
 * {@link DatasetMatrixFixture} so no external HTTP call is performed.
 */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class LivraisonIdsValidationTest {

    private static final String DATE = "2025-01-13";

    @Autowired private WebTestClient        webClient;
    @Autowired private JourneeRepository    journeeRepository;
    @Autowired private SolutionRepository   solutionRepository;
    @Autowired private TourneeRepository    tourneeRepository;
    @Autowired private LivraisonRepository  livraisonRepository;
    @Autowired private ParkingRepository    parkingRepository;
    @Autowired private AdresseRepository    adresseRepository;
    @Autowired private DatasetMatrixFixture datasetFixture;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private OpenRouteServiceClient orsClient;

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

    /**
     * The body has no {@code livraisonIds} field — Bean Validation must reject the request.
     * The pipeline must NOT start (no journée created with INITIALISATION status).
     */
    @Test
    void shouldReturn400_whenLivraisonIdsMissing() {
        // language=JSON
        String bodyWithoutLivraisonIds = """
            { "nbVehicules": 2, "maxTempsParVehicule": "PT2H" }
            """;

        webClient.post()
                .uri("/api/journees/" + DATE + "/optimization")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyWithoutLivraisonIds)
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Same as above but with an explicit {@code "livraisonIds": null} — same rejection.
     */
    @Test
    void shouldReturn400_whenLivraisonIdsExplicitlyNull() {
        // language=JSON
        String bodyWithNullLivraisonIds = """
            { "nbVehicules": 2, "maxTempsParVehicule": "PT2H", "livraisonIds": null }
            """;

        webClient.post()
                .uri("/api/journees/" + DATE + "/optimization")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyWithNullLivraisonIds)
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Happy path: body contains valid {@code livraisonIds}. The endpoint must accept the
     * request (HTTP 202) and every persisted {@link SolutionEntity} must store exactly the
     * requested {@code livraisonIds} in its {@code livraisonsIds} column.
     */
    @Test
    void shouldStartOptimization_andPersistLivraisonIdsOnEverySolution() {
        // ---- Arrange: seed the DB with the smallest dataset and configure ORS mock --------
        DatasetFixtureData fixtureData =
                datasetFixture.seed(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1);
        datasetFixture.configureMock(orsClient, fixtureData);

        // VROOM variant calls optimize(); echo all jobs back in a single route
        org.mockito.Mockito.when(orsClient.optimize(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> buildVroomResultFromPayload(inv.getArgument(0)));

        // Pick 3 livraison IDs from the seeded set — these are the IDs we want to optimize
        List<LivraisonEntity> allLivraisons = livraisonRepository.findAll();
        assertThat(allLivraisons).hasSizeGreaterThanOrEqualTo(3);
        Set<Long> requestedLivraisonIds = allLivraisons.stream()
                .limit(3)
                .map(LivraisonEntity::getId)
                .collect(Collectors.toSet());

        // ---- Act: POST optimization with valid livraisonIds --------------------------------
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("nbVehicules", 2);
        requestBody.put("maxTempsParVehicule", "PT2H");
        requestBody.put("livraisonIds", requestedLivraisonIds);

        JourneeResponseDTO startResponse = webClient.post()
                .uri("/api/journees/" + DATE + "/optimization")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(JourneeResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(startResponse).isNotNull();
        assertThat(startResponse.date()).isEqualTo(DATE);

        // ---- Assert: poll until the pipeline finishes --------------------------------------
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    JourneeResponseDTO dto = webClient.get()
                            .uri("/api/journees/" + DATE)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(JourneeResponseDTO.class)
                            .returnResult()
                            .getResponseBody();
                    assertThat(dto).isNotNull();
                    assertThat(dto.statutGeneration())
                            .isEqualTo(StatutGeneration.TERMINE.name());
                });

        // ---- Assert: every persisted SolutionEntity stores the exact requested IDs ---------
        // Run inside a transaction so the @ElementCollection lazy field can be initialized.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            List<SolutionEntity> persistedSolutions = solutionRepository.findAll();
            assertThat(persistedSolutions)
                    .as("at least one solution should have been generated")
                    .isNotEmpty();

            for (SolutionEntity solution : persistedSolutions) {
                Set<Long> stored = solution.getLivraisonsIds();
                assertThat(stored)
                        .as("solution %d must persist exactly the requested livraisonIds",
                                solution.getId())
                        .isNotNull()
                        .containsExactlyInAnyOrderElementsOf(requestedLivraisonIds);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers (copied from SolutionGenerationEndpointsTest)
    // -------------------------------------------------------------------------

    /**
     * Builds a synthetic {@link fr.uga.miage.l3.models.core.OptimizationResult} from a
     * VROOM-style payload: every job is echoed back into one route per vehicle.
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
                int      jobId    = ((Number) job.get("id")).intValue();
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
