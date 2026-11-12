package fr.uga.miage.l3.services.core.separation.kmedoids;

import fr.uga.miage.l3.models.core.Adresse;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class KMedoidsCommand {

    /**
     * Applique l'algorithme K-Medoids (PAM) pour regrouper les adresses de livraison en clusters.
     *
     * @param adresses       Liste ordonnée : index 0 = entrepôt, index 1..n = clients
     * @param distanceMatrix Matrice carrée symétrique des distances en km
     * @param k              Nombre de clusters (= nombre de camions)
     * @param maxIterations  Nombre max d'itérations avant arrêt forcé
     * @return KMedoidsResult contenant les clusters, medoids, assignments, coût total, etc.
     */
    public KMedoidsResult kMedoids(
            List<Adresse> adresses,
            double[][] distanceMatrix,
            int k,
            int maxIterations
    ) {
        // ============================
        // VALIDATION DES PARAMÈTRES
        // ============================
        validerParametres(adresses, distanceMatrix, k, maxIterations);

        int n = adresses.size(); // nb total de points (entrepôt + clients)
        int nbClients = n - 1;   // nb de clients (indices 1..n-1)

        log.info("Démarrage K-Medoids : {} adresses, {} clusters, {} itérations max", n, k, maxIterations);

        // ============================
        // PHASE 1 : INITIALISATION K-MEANS++
        // ============================
        List<Integer> medoids = initialiserMedoidsKMeansPlusPlus(distanceMatrix, n, k);
        log.info("Medoids initiaux (K-Means++) : {}", medoids);

        // Tableau des assignations : assignments[i] = index du medoid du cluster de i
        // assignments[0] = -1 (entrepôt jamais clusterisé)
        int[] assignments = new int[n];
        assignments[0] = -1;

        boolean converged = false;
        int iteration = 0;

        // ============================
        // PHASE 2 : BOUCLE PRINCIPALE PAM
        // ============================
        while (iteration < maxIterations) {
            iteration++;
            log.debug("Itération {}/{}", iteration, maxIterations);

            // 2.1 ASSIGNMENT : assigner chaque client au medoid le plus proche
            for (int i = 1; i < n; i++) {
                assignments[i] = trouverMedoidLePlusProche(i, medoids, distanceMatrix);
            }

            // 2.2 UPDATE : recalculer les medoids optimaux pour chaque cluster
            List<Integer> nouveauxMedoids = recalculerMedoids(assignments, medoids, distanceMatrix, n);

            // 2.3 CONVERGENCE : vérifier si les medoids ont changé
            if (new HashSet<>(nouveauxMedoids).equals(new HashSet<>(medoids))) {
                converged = true;
                log.info("Convergence atteinte à l'itération {}", iteration);
                break;
            }

            medoids = nouveauxMedoids;
        }

        if (!converged) {
            log.warn("Arrêt forcé après {} itérations sans convergence", maxIterations);
        }

        // Dernière assignation avec les medoids finaux
        for (int i = 1; i < n; i++) {
            assignments[i] = trouverMedoidLePlusProche(i, medoids, distanceMatrix);
        }

        // ============================
        // PHASE 3 : CONSTRUCTION DU RÉSULTAT
        // ============================
        Map<Integer, List<Integer>> clusters = construireClusters(assignments, medoids, n);
        double totalCost = calculerCoutTotal(assignments, distanceMatrix, n);

        // Vérification limite ORS (25 adresses par cluster)
        verifierLimiteClusters(clusters);

        log.info("K-Medoids terminé : coût total = {}, convergé = {}", totalCost, converged);

        return KMedoidsResult.builder()
                .clusters(clusters)
                .medoids(new ArrayList<>(medoids))
                .assignments(assignments)
                .totalCost(totalCost)
                .iterations(iteration)
                .converged(converged)
                .build();
    }

    // ============================
    // INITIALISATION K-MEANS++
    // ============================

    /**
     * Sélectionne k medoids initiaux via la stratégie K-Means++ :
     * - Le premier medoid est choisi aléatoirement parmi les clients (indices 1..n-1)
     * - Chaque medoid suivant est l'adresse la plus éloignée des medoids déjà choisis
     */
    private List<Integer> initialiserMedoidsKMeansPlusPlus(double[][] distanceMatrix, int n, int k) {
        List<Integer> medoids = new ArrayList<>();
        Random random = new Random();

        // Premier medoid aléatoire parmi les clients (exclure entrepôt = index 0)
        int premierMedoid = 1 + random.nextInt(n - 1);
        medoids.add(premierMedoid);
        log.debug("Premier medoid choisi : index {}", premierMedoid);

        // Medoids suivants : choisir le plus éloigné des medoids existants
        while (medoids.size() < k) {
            int meilleurCandidat = -1;
            double distanceMax = -1.0;

            for (int i = 1; i < n; i++) {
                if (medoids.contains(i)) continue; // déjà medoid

                // Distance minimale de i à l'ensemble des medoids déjà choisis
                int finalI = i;
                double distMinAuxMedoids = medoids.stream()
                          .mapToDouble(m -> distanceMatrix[finalI][m])
                          .min()
                          .orElse(0.0);

                  if (distMinAuxMedoids > distanceMax) {
                      distanceMax = distMinAuxMedoids;
                      meilleurCandidat = i;
                  }
            }

            medoids.add(meilleurCandidat);
            log.debug("Medoid {} choisi : index {} (distance max = {})", medoids.size(), meilleurCandidat, distanceMax);
        }

        return medoids;
    }

    // ============================
    // ASSIGNMENT
    // ============================

    /**
     * Trouve le medoid le plus proche d'un point donné.
     *
     * @param pointIdx index de l'adresse à assigner
     * @param medoids  liste des indices medoids actuels
     * @return index du medoid le plus proche
     */
    private int trouverMedoidLePlusProche(int pointIdx, List<Integer> medoids, double[][] distanceMatrix) {
        int meilleurMedoid = medoids.get(0);
        double distanceMin = distanceMatrix[pointIdx][meilleurMedoid];

        for (int m : medoids) {
            double dist = distanceMatrix[pointIdx][m];
            if (dist < distanceMin) {
                distanceMin = dist;
                meilleurMedoid = m;
            }
        }

        return meilleurMedoid;
    }

    // ============================
    // UPDATE DES MEDOIDS
    // ============================

    /**
     * Pour chaque cluster, recalcule le medoid optimal = point qui minimise
     * la somme des distances aux autres points du cluster.
     *
     * @return nouvelle liste de medoids
     */
    private List<Integer> recalculerMedoids(int[] assignments, List<Integer> medoids, double[][] distanceMatrix, int n) {
        List<Integer> nouveauxMedoids = new ArrayList<>();

        for (int medoidActuel : medoids) {
            // Récupérer tous les points de ce cluster
            List<Integer> pointsCluster = new ArrayList<>();
            for (int i = 1; i < n; i++) {
                if (assignments[i] == medoidActuel) {
                    pointsCluster.add(i);
                }
            }

            if (pointsCluster.isEmpty()) {
                // Cluster vide : conserver le medoid actuel
                log.warn("Cluster vide détecté pour le medoid {}", medoidActuel);
                nouveauxMedoids.add(medoidActuel);
                continue;
            }

            // Trouver le nouveau medoid = argmin somme des distances internes
            int meilleurMedoid = pointsCluster.get(0);
            double coutMin = calculerCoutCluster(meilleurMedoid, pointsCluster, distanceMatrix);

            for (int candidat : pointsCluster) {
                double cout = calculerCoutCluster(candidat, pointsCluster, distanceMatrix);
                if (cout < coutMin) {
                    coutMin = cout;
                    meilleurMedoid = candidat;
                }
            }

            nouveauxMedoids.add(meilleurMedoid);
        }

        return nouveauxMedoids;
    }

    /**
     * Calcule la somme des distances d'un candidat medoid à tous les points d'un cluster.
     */
    private double calculerCoutCluster(int candidat, List<Integer> pointsCluster, double[][] distanceMatrix) {
        double cout = 0.0;
        for (int point : pointsCluster) {
            cout += distanceMatrix[candidat][point];
        }
        return cout;
    }

    // ============================
    // CONSTRUCTION DU RÉSULTAT
    // ============================

    /**
     * Construit la Map clusters depuis le tableau assignments.
     * Clé = index medoid, Valeur = liste des indices adresses du cluster.
     */
    private Map<Integer, List<Integer>> construireClusters(int[] assignments, List<Integer> medoids, int n) {
        Map<Integer, List<Integer>> clusters = new LinkedHashMap<>();

        // Initialiser chaque cluster (avec le medoid inclus dans sa propre liste)
        for (int m : medoids) {
            clusters.put(m, new ArrayList<>());
        }

        // Remplir les clusters (index 1..n-1, ignorer entrepôt index 0)
        for (int i = 1; i < n; i++) {
            int medoidAssigne = assignments[i];
            clusters.get(medoidAssigne).add(i);
        }

        return clusters;
    }

    /**
     * Calcule le coût total = somme des distances de chaque client à son medoid.
     */
    private double calculerCoutTotal(int[] assignments, double[][] distanceMatrix, int n) {
        double cout = 0.0;
        for (int i = 1; i < n; i++) {
            cout += distanceMatrix[i][assignments[i]];
        }
        return cout;
    }

    /**
     * Vérifie que chaque cluster ne dépasse pas 25 adresses (limite ORS Matrix API).
     * Log un warning si dépassement, sans bloquer.
     */
    private void verifierLimiteClusters(Map<Integer, List<Integer>> clusters) {
        final int LIMITE_ORS = 25;
        clusters.forEach((medoid, points) -> {
            if (points.size() > LIMITE_ORS) {
                log.warn(
                        "Cluster {} dépasse la limite ORS : {} adresses (max recommandé : {})",
                        medoid, points.size(), LIMITE_ORS
                );
            }
        });
    }

    // ============================
    // VALIDATION DES PARAMÈTRES
    // ============================

    private void validerParametres(List<Adresse> adresses, double[][] distanceMatrix, int k, int maxIterations) {
        if (adresses == null || adresses.isEmpty())
            throw new IllegalArgumentException("La liste des adresses ne peut pas être null ou vide.");

        if (distanceMatrix == null)
            throw new IllegalArgumentException("La matrice de distances ne peut pas être null.");

        if (distanceMatrix.length != adresses.size())
            throw new IllegalArgumentException(
                "La matrice de distances (" + distanceMatrix.length + "x?) " +
                "ne correspond pas au nombre d'adresses (" + adresses.size() + ")."
            );

        for (double[] row : distanceMatrix) {
            if (row.length != adresses.size())
                throw new IllegalArgumentException("La matrice de distances n'est pas carrée.");
        }

        for (int i = 0; i < distanceMatrix.length; i++) {
            for (int j = 0; j < distanceMatrix[i].length; j++) {
                double val = distanceMatrix[i][j];
                if (Double.isNaN(val) || Double.isInfinite(val))
                    throw new IllegalArgumentException(
                        "La matrice contient une valeur invalide à [" + i + "][" + j + "] : " + val
                    );
            }
        }

        if (k < 1 || k > adresses.size() - 1)
            throw new IllegalArgumentException(
                "k doit être compris entre 1 et " + (adresses.size() - 1) + " (valeur reçue : " + k + ")."
            );

        if (maxIterations < 1)
            throw new IllegalArgumentException(
                "maxIterations doit être >= 1 (valeur reçue : " + maxIterations + ")."
            );
    }
}

