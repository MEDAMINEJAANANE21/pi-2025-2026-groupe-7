package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.services.core.OrsMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Génère toutes les combinaisons séparation × moteur × nombre de véhicules à essayer.
 *
 * <p>Deux règles évitent des calculs inutiles :
 * <ul>
 *   <li>une séparation n'a pas de sens avec un seul véhicule ;</li>
 *   <li>sans séparation, seul VROOM sait répartir entre plusieurs véhicules.</li>
 * </ul>
 * En mode estimation (sans clé ORS), VROOM n'est pas disponible et n'est donc pas essayé.
 */
@Component
@RequiredArgsConstructor
public class StrategieCombinations {

    private static final List<Separation> SEPARATIONS = List.of(Separation.NONE, Separation.ANGULAR, Separation.KMEDOIDS);
    private static final List<Optimisation> OPTIMISATIONS = List.of(Optimisation.VROOM, Optimisation.GENETIC, Optimisation.GREEDY);

    private final OrsMode orsMode;

    public List<Strategie> expand(Strategie base) {
        List<Strategie> strategies = new ArrayList<>();
        for (Separation separation : SEPARATIONS) {
            for (Optimisation optimisation : availableOptimisations()) {
                for (int nbVehicules = base.getNbVehicules(); nbVehicules > 0; nbVehicules--) {
                    if (isWorthTrying(separation, optimisation, nbVehicules)) {
                        strategies.add(Strategie.builder()
                            .separation(separation)
                            .optimisation(optimisation)
                            .nbVehicules(nbVehicules)
                            .maxTempsParVehicule(base.getMaxTempsParVehicule())
                            .build());
                    }
                }
            }
        }
        return strategies;
    }

    private List<Optimisation> availableOptimisations() {
        return orsMode.estimation()
            ? OPTIMISATIONS.stream().filter(o -> o != Optimisation.VROOM).toList()
            : OPTIMISATIONS;
    }

    private static boolean isWorthTrying(Separation separation, Optimisation optimisation, int nbVehicules) {
        if (separation != Separation.NONE) {
            return nbVehicules > 1;
        }
        return nbVehicules == 1 || optimisation == Optimisation.VROOM;
    }
}
