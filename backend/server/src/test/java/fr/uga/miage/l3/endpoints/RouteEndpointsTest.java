package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.mappers.CoordsMapper;
import fr.uga.miage.l3.models.core.Coords;
import fr.uga.miage.l3.models.core.DirectionsResponse;
import fr.uga.miage.l3.request.RouteRequest;
import fr.uga.miage.l3.responses.RouteResponsesDTO;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class RouteEndpointsTest {
    @Autowired private WebTestClient webClient;
    @Autowired private CoordsMapper coordsMapper;
    @MockitoBean private OpenRouteServiceClient orsClient;

    @Test
    void testPositive() {
        List<Coords> req = buildCoordsSequence(10);
        List<Coords> res = interpolateCoordsSequence(req);

        when(orsClient.getDirections(any()))
            .thenReturn(buildDirection(res));

        webClient.post()
            .uri("/api/routes")
            .header("Content-Type", "application/json")
            .bodyValue(new RouteRequest(
                coordsMapper.toCoordonneeDTOList(req)
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(RouteResponsesDTO.class)
            .isEqualTo(new RouteResponsesDTO(coordsMapper.toCoordonneeDTOList(res)));
    }

    @Test
    void testManyPoints() {
        List<Coords> bigReq = buildCoordsSequence(120);
        List<Coords> bigRes = interpolateCoordsSequence(bigReq);

        List<Coords> req1 = bigReq.subList(0, 50);
        List<Coords> req2 = bigReq.subList(49, 99);
        List<Coords> req3 = bigReq.subList(98, 120);

        List<Coords> res1 = interpolateCoordsSequence(req1);
        List<Coords> res2 = interpolateCoordsSequence(req2);
        List<Coords> res3 = interpolateCoordsSequence(req3);

        when(orsClient.getDirections(argThat(actual -> matchesCoordsTuple(actual, toCoordsTuple(req1)))))
            .thenReturn(buildDirection(res1));
        when(orsClient.getDirections(argThat(actual -> matchesCoordsTuple(actual, toCoordsTuple(req2)))))
            .thenReturn(buildDirection(res2));
        when(orsClient.getDirections(argThat(actual -> matchesCoordsTuple(actual, toCoordsTuple(req3)))))
            .thenReturn(buildDirection(res3));

        webClient.post()
            .uri("/api/routes")
            .header("Content-Type", "application/json")
            .bodyValue(new RouteRequest(
                coordsMapper.toCoordonneeDTOList(bigReq)
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(RouteResponsesDTO.class)
            .isEqualTo(new RouteResponsesDTO(coordsMapper.toCoordonneeDTOList(bigRes)));
    }

    @Test
    void test400() {
        webClient.post()
            .uri("/api/routes")
            .header("Content-Type", "application/json")
            .bodyValue(new ErrorDTO(
                "/api/routes",
                "Invalid request body"
            ))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorDTO.class)
            .isEqualTo(new ErrorDTO(
                "/api/routes",
                "Le corps du request doit contenir une liste d'adresses"
            ));
    }

    @Test
    void test404() {
        List<Coords> req = buildCoordsSequence(10);
        List<Coords> res = interpolateCoordsSequence(req);

        when(orsClient.getDirections(any()))
            .thenThrow(
                HttpClientErrorException.NotFound.create(
                    HttpStatusCode.valueOf(404),
                    "Une des routes n'a pas pu être trouvé",
                    null,
                    null,
                    null
                )
            );

        webClient.post()
            .uri("/api/routes")
            .header("Content-Type", "application/json")
            .bodyValue(new RouteRequest(
                coordsMapper.toCoordonneeDTOList(req)
            ))
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorDTO.class)
            .isEqualTo(new ErrorDTO(
                "/api/routes",
                "404 Une des routes n'a pas pu être trouvé"
            ));
    }

    private List<Coords> buildCoordsSequence(int length) {
        List<Coords> res = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            res.add(Coords.builder()
                .lat(i)
                .lng(i)
                .build());
        }
        return res;
    }

    private List<Coords> interpolateCoordsSequence(List<Coords> sequence) {
        List<Coords> res = new ArrayList<>(sequence.size() * 6);
        for (int i = 0; i < sequence.size(); i++) {
            Coords c = sequence.get(i);
            res.add(c);
            if (i == sequence.size() - 1) continue;
            for (int j = 0; j < 5; j++) {
                res.add(Coords.builder()
                    .lat(c.lat + j * 0.1)
                    .lng(c.lng + j * 0.1)
                    .build());
            }
        }
        return res;
    }

    private List<double[]> toCoordsTuple(List<Coords> sequence) {
        return sequence.stream().map((Coords c) -> new double[]{c.lng, c.lat}).toList();
    }

    private List<Coords> toCoordsObject(List<double[]> sequence) {
        return sequence.stream()
            .map((double[] c) -> Coords.builder().lat(c[1]).lng(c[0]).build())
            .toList();
    }

    private boolean matchesCoordsTuple(List<double[]> actual, List<double[]> expected) {
        if (actual == null || expected == null || actual.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!matchesTuple(actual.get(i), expected.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesTuple(double[] actual, double[] expected) {
        if (actual == null || expected == null || actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (Double.compare(actual[i], expected[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    private DirectionsResponse buildDirection(List<Coords> geometry) {
        return DirectionsResponse.builder()
            .type("Test")
            .features(
                List.of(
                    DirectionsResponse.DirectionsFeature.builder()
                        .type("Test")
                        .geometry(
                            DirectionsResponse.DirectionsGeometry.builder()
                                .type("Test")
                                .coordinates(toCoordsTuple(geometry))
                                .build()
                        )
                        .build()
                )
            )
            .build();
    }
}
