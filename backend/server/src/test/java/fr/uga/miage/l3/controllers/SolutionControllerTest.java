package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.fixtures.TestDataFactory.JourneeScenario;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.SolutionCreationRequest;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.StrategieResponseDTO;
import fr.uga.miage.l3.responses.TourneeStatisticsResponseDTO;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@code GET /api/solutions/{id}} and
 * {@code POST /api/solutions}.
 *
 * <p>Uses an in-memory H2 database. All state is cleaned up after each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class SolutionControllerTest {

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory factory;

    @Autowired private SolutionRepository solutionRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private JourneeRepository journeeRepository;

    @AfterEach
    void cleanup() {
        tourneeRepository.deleteAll();
        solutionRepository.deleteAll();
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
        journeeRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // GET /api/solutions/{solutionId}
    // -------------------------------------------------------------------------

    @Nested
    class GetSolution {

        private static final LocalDate DATE = LocalDate.of(2026, 3, 1);

        @Test
        void shouldReturnSolutionWithCorrectId() {
            JourneeScenario scenario = factory.createJourneeWithSolutions(DATE, 1, 2, 1);
            Long expectedId = scenario.solutions().get(0).getId();

            webClient.get()
                    .uri("/api/solutions/" + expectedId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.id()).isEqualTo(expectedId);
                    });
        }

        @Test
        void shouldReturnSolutionWithCorrectDate() {
            JourneeScenario scenario = factory.createJourneeWithSolutions(DATE, 1, 2, 1);
            Long solutionId = scenario.solutions().get(0).getId();
            String expectedDate = DATE.toString();

            webClient.get()
                    .uri("/api/solutions/" + solutionId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.date()).isEqualTo(expectedDate);
                    });
        }

        @Test
        void shouldReturnSolutionWithTournees() {
            // 1 solution with 1 tournée containing 3 livraisons
            JourneeScenario scenario = factory.createJourneeWithSolutions(DATE, 1, 3, 1);
            Long solutionId = scenario.solutions().get(0).getId();

            // Expected tournée statistics (from TestDataFactory defaults)
            TourneeStatisticsResponseDTO expectedStats = new TourneeStatisticsResponseDTO(
                    100L, 5000L, "PT30M", "PT15M"
            );

            webClient.get()
                    .uri("/api/solutions/" + solutionId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.tournees()).isNotNull().isNotEmpty();
                        assertThat(r.tournees())
                                .allSatisfy(t -> assertThat(t.statistiques()).isEqualTo(expectedStats));
                    });
        }

        @Test
        void shouldReturn404WhenSolutionDoesNotExist() {
            String uri = "/api/solutions/999999";
            ErrorDTO expectedError = new ErrorDTO(uri, "Solution not found: 999999");

            webClient.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(expectedError);
        }

        @Test
        void shouldReturnStrategieInResponse() {
            JourneeScenario scenario = factory.createJourneeWithSolutions(DATE, 1, 1, 1);
            Long solutionId = scenario.solutions().get(0).getId();

            // Factory always uses GREEDY / NONE / 1 vehicle / no time limit
            StrategieResponseDTO expectedStrategie = new StrategieResponseDTO("NONE", "GREEDY", 1, null);

            webClient.get()
                    .uri("/api/solutions/" + solutionId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.strategie()).isEqualTo(expectedStrategie);
                    });
        }

        @Test
        void shouldReturnStatistiquesWhenJourneeHasMultipleSolutions() {
            JourneeScenario scenario = factory.createJourneeWithSolutions(DATE, 2, 3, 1);
            Long solutionId = scenario.solutions().get(0).getId();

            webClient.get()
                    .uri("/api/solutions/" + solutionId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.statistiques()).isNotNull();
                    });
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/solutions
    // -------------------------------------------------------------------------

    @Nested
    class CreateSolution {

        private static final LocalDate DATE = LocalDate.of(2026, 3, 2);

        @Test
        void shouldCreateSolutionAndReturnCreatedStatus() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(1, "PT8H", null),
                    null
            );

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.id()).isNotNull().isPositive();
                        assertThat(r.date()).isEqualTo(DATE.toString());
                    });
        }

        @Test
        void shouldReturnSolutionWithCorrectDateAfterCreation() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(2, "PT4H", null),
                    null
            );

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.date()).isEqualTo(DATE.toString());
                    });
        }

        @Test
        void shouldReturnSolutionWithIdAfterCreation() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(1, "PT8H", null),
                    null
            );

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.id()).isNotNull().isPositive();
                    });
        }

        @Test
        void shouldPersistSolutionInDatabase() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(1, "PT8H", null),
                    null
            );

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(solutionRepository.findById(r.id())).isPresent();
                    });
        }

        @Test
        void shouldReturnStrategieWithCorrectNbVehicules() {
            int nbVehicules = 3;
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(nbVehicules, "PT6H", null),
                    null
            );

            // Only nbVehicules is set; separation/optimisation are null from StrategieMapper
            StrategieResponseDTO expectedStrategie = new StrategieResponseDTO(null, null, nbVehicules, "PT6H");

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.strategie()).isEqualTo(expectedStrategie);
                    });
        }

        @Test
        void shouldAllowCreatingMultipleSolutionsForSameDate() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(1, "PT8H", null),
                    null
            );

            webClient.post().uri("/api/solutions").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request).exchange().expectStatus().isCreated();
            webClient.post().uri("/api/solutions").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request).exchange().expectStatus().isCreated();

            long count = solutionRepository.findAll().stream()
                    .filter(s -> DATE.equals(s.getDate()))
                    .count();
            assertThat(count).isEqualTo(2);
        }

        @Test
        void shouldReturn400WhenDateIsInvalid() {
            String uri = "/api/solutions";
            ErrorDTO expectedError = new ErrorDTO(
                    uri,
                    "Invalid date format: 'not-a-date'. Expected ISO-8601 (example: 2026-04-19)"
            );

            webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SolutionCreationRequest("not-a-date", new StrategieRequestDTO(1, "PT8H", null), null))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(expectedError);
        }

        @Test
        void shouldReturn400WhenMaxTempsParVehiculeIsInvalid() {
            String uri = "/api/solutions";
            ErrorDTO expectedError = new ErrorDTO(
                    uri,
                    "maxTempsParVehicule must use ISO-8601 duration format (example: PT2H30M)"
            );

            webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SolutionCreationRequest(DATE.toString(), new StrategieRequestDTO(1, "not-a-duration", null), null))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(expectedError);
        }

        @Test
        void createdSolutionShouldBeRetrievableViaGetEndpoint() {
            SolutionCreationRequest request = new SolutionCreationRequest(
                    DATE.toString(),
                    new StrategieRequestDTO(1, "PT8H", null),
                    null
            );

            // Capture id, date and strategie from the POST response
            Long[] createdId = {null};
            String[] createdDate = {null};
            StrategieResponseDTO[] createdStrategie = {null};

            webClient.post()
                    .uri("/api/solutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        createdId[0] = r.id();
                        createdDate[0] = r.date();
                        createdStrategie[0] = r.strategie();
                    });

            // GET must return the same identity fields
            webClient.get()
                    .uri("/api/solutions/" + createdId[0])
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SolutionResponseDTO.class)
                    .value(r -> {
                        assertThat(r).isNotNull();
                        assertThat(r.id()).isEqualTo(createdId[0]);
                        assertThat(r.date()).isEqualTo(createdDate[0]);
                        assertThat(r.strategie()).isEqualTo(createdStrategie[0]);
                    });
        }
    }
}



