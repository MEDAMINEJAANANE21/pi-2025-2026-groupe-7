package fr.uga.miage.l3.services.core.optimization.greedy;

import java.util.ArrayList;
import java.util.List;

public class GreedyCommand {

    /**
     * Calcule le plus court chemin avec l'algorithme Greedy (Nearest Neighbor).
     * Équivalent de searchForFastestPath() en TypeScript.
     *
     * @param idxs       indices des adresses à visiter
     * @param parkingIdx index du point de départ (entrepôt)
     * @param matrix     matrice des distances
     * @return liste ordonnée des indices à visiter
     */
    public List<Integer> searchForFastestPath(List<Integer> idxs, int parkingIdx, double[][] matrix) {
        List<Integer> toVisit = new ArrayList<>(idxs);
        List<Integer> route = new ArrayList<>();
        int currentAddress = parkingIdx;

        int maxIterations = idxs.size();
        int i = 0;

        while (i < maxIterations && !toVisit.isEmpty()) {
            // Trouver l'adresse non visitée la plus proche
            final int current = currentAddress;
            int closestAddress = toVisit.stream()
                    .reduce((closest, mayBeClosest) ->
                            matrix[current][mayBeClosest] < matrix[current][closest]
                                    ? mayBeClosest
                                    : closest
                    )
                    .orElseThrow(() -> new IllegalStateException("Liste toVisit vide inattendue"));

            // Ajouter au chemin
            route.add(closestAddress);

            // Retirer de la liste des adresses à visiter
            toVisit.remove(Integer.valueOf(closestAddress));

            // Se déplacer vers l'adresse la plus proche
            currentAddress = closestAddress;
            i++;
        }

        if (!toVisit.isEmpty()) {
            throw new IllegalStateException(
                "Certaines adresses n'ont pas été visitées : " + toVisit
            );
        }

        return route;
    }
}
