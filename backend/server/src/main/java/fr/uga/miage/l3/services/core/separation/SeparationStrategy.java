package fr.uga.miage.l3.services.core.separation;

import fr.uga.miage.l3.models.core.Adresse;

import java.util.List;

public interface SeparationStrategy {
    List<List<Integer>> separate(List<Adresse> allLocations, double[][] distanceMatrix, int nbGroups);
}
