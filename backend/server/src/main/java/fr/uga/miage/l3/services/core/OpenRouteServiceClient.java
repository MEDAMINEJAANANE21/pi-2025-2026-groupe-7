package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.core.OptimizationResult;
import fr.uga.miage.l3.models.core.DirectionsResponse;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Generated // Pour ignorer cette classe dans coverage
@RequiredArgsConstructor
@Service
public class OpenRouteServiceClient {
    private final RestTemplate rest = new RestTemplate();
    private final String orsBaseUrl = "https://api.openrouteservice.org";
    @Value("${ORS_KEY:toto}") // environnment variable
    private String orskey;
    public MatrixResponse getMatrix(List<Adresse> locations, int[] sources, int[] destinations) {

        // Build request payload similar to client implementation
        Map<String, Object> req = new HashMap<>();
        double[][] locs = new double[locations.size()][];
        for (int i = 0; i < locations.size(); i++) {
            locs[i] = new double[]{locations.get(i).coords.lng, locations.get(i).coords.lat};
        }
        req.put("locations", locs);
        req.put("units", "m");
        req.put("metrics", new String[]{"duration", "distance"});
        req.put("sources", sources);
        req.put("destinations", destinations);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orskey);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(req, headers);

        // Call external ORS API and map to MatrixResponse
        return rest.postForObject(orsBaseUrl + "/v2/matrix/driving-car", entity, MatrixResponse.class);
    }

    public OptimizationResult optimize(Object payload) {
        // Optimization request to ORS optimization endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orskey);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        return rest.postForObject(orsBaseUrl + "/optimization", entity, OptimizationResult.class);
    }

    public DirectionsResponse getDirections(List<double[]> coordinates) {
        // Build request payload for directions API
        Map<String, Object> req = new HashMap<>();
        req.put("coordinates", coordinates);
        req.put("profile", "driving-car");
        req.put("format", "geojson");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", orskey);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(req, headers);

        return rest.postForObject(orsBaseUrl + "/v2/directions/driving-car/geojson", entity, DirectionsResponse.class);
    }
}
