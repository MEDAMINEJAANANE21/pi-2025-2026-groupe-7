package fr.uga.miage.l3.services.statistics;

/**
 * Bilan économique d'une tournée, en euros.
 */
public record TourneeFinances(double coutCarburant, double coutChauffeur, double chiffreAffaires) {

    public static final TourneeFinances ZERO = new TourneeFinances(0, 0, 0);

    public double coutTotal() {
        return coutCarburant + coutChauffeur;
    }

    public double marge() {
        return chiffreAffaires - coutTotal();
    }

    public TourneeFinances plus(TourneeFinances other) {
        return new TourneeFinances(
            coutCarburant + other.coutCarburant,
            coutChauffeur + other.coutChauffeur,
            chiffreAffaires + other.chiffreAffaires
        );
    }
}
