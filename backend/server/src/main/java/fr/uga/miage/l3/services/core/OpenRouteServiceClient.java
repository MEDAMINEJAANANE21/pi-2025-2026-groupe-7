package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.DirectionsResponse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.core.OptimizationResult;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Pattern utilisé : Adapter. Seul point de contact avec l'API HTTP d'OpenRouteService.
 *
 * <p>Exclu de la couverture : les tests le remplacent par un mock, il n'y a pas de logique ici.
 */
@Generated
@Service
public class OpenRouteServiceClient {

    private static final String PROFILE = "driving-car";

    private final RestTemplate rest;
    private final String baseUrl;
    private final String apiKey;

    public OpenRouteServiceClient(
        @Value("${app.ors.base-url:https://api.openrouteservice.org}") String baseUrl,
        @Value("${app.ors.api-key:}") String apiKey,
        @Value("${app.ors.timeout:PT60S}") Duration timeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.rest = new RestTemplate(requestFactory);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
    }

    public MatrixResponse getMatrix(List<Adresse> locations, int[] sources, int[] destinations) {
        Map<String, Object> body = Map.of(
            "locations", locations.stream().map(a -> new double[]{a.coords.lng, a.coords.lat}).toList(),
            "units", "m",
            "metrics", List.of("duration", "distance"),
            "sources", sources,
            "destinations", destinations
        );
        return rest.postForObject(baseUrl + "/v2/matrix/" + PROFILE, request(body), MatrixResponse.class);
    }

    public OptimizationResult optimize(Object payload) {
        return rest.postForObject(baseUrl + "/optimization", request(payload), OptimizationResult.class);
    }

    public DirectionsResponse getDirections(List<double[]> coordinates) {
        Map<String, Object> body = Map.of("coordinates", coordinates);
        return rest.postForObject(baseUrl + "/v2/directions/" + PROFILE + "/geojson", request(body), DirectionsResponse.class);
    }

    private HttpEntity<Object> request(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, apiKey);
        }
        return new HttpEntity<>(body, headers);
    }
}
