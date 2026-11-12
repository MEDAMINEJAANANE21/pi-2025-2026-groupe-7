package fr.uga.miage.l3.services.optimisation.routing;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;

import java.util.List;

/**
 * Pattern utilisé : Strategy. Calcule l'ordre de passage d'un groupe de livraisons.
 */
public interface RouteOptimizer {

    Optimisation type();

    /**
     * @param group        indices des livraisons à desservir
     * @param nbVehicules  nombre de véhicules disponibles pour ce groupe
     * @return une liste d'indices ordonnés par véhicule effectivement utilisé
     */
    List<List<Integer>> optimize(List<Integer> group, int nbVehicules, RouteConstraints constraints, OptimisationProblem problem);
}
