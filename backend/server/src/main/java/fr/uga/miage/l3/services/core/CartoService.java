package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.Coords;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartoService {
    private final RestTemplate rest = new RestTemplate();
    private final String cartoURL = "https://api-adresse.data.gouv.fr";

    public List<Adresse> getAdressesFromCoordinates(List<Coords> coordsList) {
        // Minimal implementation: call reverse geocoding CSV endpoint or implement later.
        // For now return empty list or map coords to Adresse with empty metadata.
        List<Adresse> res = new ArrayList<>();
        for (Coords c : coordsList) {
            Adresse a = new Adresse(new Coords(c.lat, c.lng), "", "", "");
            res.add(a);
        }
        return res;
    }
}
