package fr.uga.miage.l3.services.statistics;

import fr.uga.miage.l3.config.CoutsProperties;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Calcule ce que coûte et ce que rapporte une tournée.
 *
 * <p>Coût = carburant consommé + heures payées au chauffeur (trajet et montage).
 * Recette = forfait par livraison + supplément au poids.
 */
@Component
@RequiredArgsConstructor
public class FinanceCalculator {

    private static final double MILLILITERS_PER_LITER = 1000.0;
    private static final double SECONDS_PER_HOUR = 3600.0;

    private final CoutsProperties couts;

    public TourneeFinances compute(TourneeEntity tournee) {
        Statistic statistic = tournee.getStatistic();
        List<LivraisonEntity> livraisons = tournee.getLivraisonEntities() == null ? List.of() : tournee.getLivraisonEntities();
        if (statistic == null) {
            return new TourneeFinances(0, 0, revenueOf(livraisons));
        }
        double liters = valueOrZero(statistic.getCarburant()) / MILLILITERS_PER_LITER;
        double hours = workedTime(statistic).toSeconds() / SECONDS_PER_HOUR;
        return new TourneeFinances(
            round(liters * couts.prixCarburantParLitre()),
            round(hours * hourlyRateOf(tournee.getChauffeur())),
            revenueOf(livraisons)
        );
    }

    public long fuelMilliliters(long distanceMeters, Double litresAux100Km) {
        double consumption = litresAux100Km == null ? couts.consommationLitresAux100Km() : litresAux100Km;
        // m × (L/100 km) / 100 donne directement des millilitres : 1 km à 8 L/100 km = 80 mL.
        return Math.round(distanceMeters * consumption / 100.0);
    }

    private double revenueOf(List<LivraisonEntity> livraisons) {
        double totalWeight = livraisons.stream()
            .map(LivraisonEntity::getPoids)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
        return round(livraisons.size() * couts.tarifParLivraison() + totalWeight * couts.tarifParKg());
    }

    private double hourlyRateOf(ChauffeurEntity chauffeur) {
        return chauffeur == null || chauffeur.getTauxHoraire() == null
            ? couts.tauxHoraireChauffeur()
            : chauffeur.getTauxHoraire();
    }

    private static Duration workedTime(Statistic statistic) {
        Duration trajet = statistic.getTempsTrajet() == null ? Duration.ZERO : statistic.getTempsTrajet();
        Duration montage = statistic.getTempsMontage() == null ? Duration.ZERO : statistic.getTempsMontage();
        return trajet.plus(montage);
    }

    private static double valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    static double round(double euros) {
        return Math.round(euros * 100.0) / 100.0;
    }
}
