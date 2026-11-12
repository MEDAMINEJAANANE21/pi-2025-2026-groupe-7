package fr.uga.miage.l3.services.optimisation.routing;

import fr.uga.miage.l3.enums.Optimisation;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des moteurs d'itinéraire : ajouter un moteur revient à déclarer un nouveau bean.
 */
@Component
public class RouteOptimizers {

    private final Map<Optimisation, RouteOptimizer> byType = new EnumMap<>(Optimisation.class);

    public RouteOptimizers(List<RouteOptimizer> optimizers) {
        optimizers.forEach(optimizer -> byType.put(optimizer.type(), optimizer));
    }

    public RouteOptimizer get(Optimisation type) {
        RouteOptimizer optimizer = byType.get(type == null ? Optimisation.VROOM : type);
        if (optimizer == null) {
            throw new IllegalStateException("Aucun moteur d'itinéraire pour " + type);
        }
        return optimizer;
    }
}
