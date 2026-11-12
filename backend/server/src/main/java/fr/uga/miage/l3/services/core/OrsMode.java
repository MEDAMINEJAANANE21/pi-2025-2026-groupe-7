package fr.uga.miage.l3.services.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Indique si l'on peut interroger OpenRouteService ou s'il faut estimer les distances.
 *
 * <p>Le mode estimation permet de faire tourner l'application juste après un clone,
 * sans créer de compte ORS. Il est désactivé par défaut, donc jamais actif par surprise.
 */
@Slf4j
@Component
public class OrsMode {

    private final boolean estimation;

    public OrsMode(
        @Value("${app.ors.api-key:}") String apiKey,
        @Value("${app.ors.estimation-sans-cle:false}") boolean estimationSansCle
    ) {
        this.estimation = estimationSansCle && (apiKey == null || apiKey.isBlank());
        if (estimation) {
            log.warn("Aucune clé OpenRouteService : distances estimées à vol d'oiseau, moteur VROOM désactivé");
        }
    }

    public boolean estimation() {
        return estimation;
    }
}
