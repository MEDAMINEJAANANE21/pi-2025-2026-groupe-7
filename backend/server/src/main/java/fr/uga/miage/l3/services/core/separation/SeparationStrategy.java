package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.models.core.Adresse;

import java.util.List;

/**
 * Pattern utilisé : Strategy. Répartit les livraisons en un groupe par véhicule.
 *
 * <p>{@code allLocations} contient les livraisons puis le dépôt en dernière position ;
 * les groupes renvoyés contiennent uniquement des indices de livraisons.
 */
public interface SeparationStrategy {

    Separation type();

    List<List<Integer>> separate(List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups);
}
