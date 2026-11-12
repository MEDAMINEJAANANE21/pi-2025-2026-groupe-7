package fr.uga.miage.l3.services.statistics;

import fr.uga.miage.l3.models.domain.SolutionStatisticsData;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Agrège les statistiques des tournées d'une solution (total, min, max, moyenne).
 */
@Component
@RequiredArgsConstructor
public class SolutionStatisticsCalculator {

    private final FinanceCalculator financeCalculator;

    public SolutionStatisticsData compute(SolutionEntity solution) {
        List<TourneeFigures> tournees = solution.getTourneeEntities() == null
            ? List.of()
            : solution.getTourneeEntities().stream().map(this::figuresOf).toList();
        int count = tournees.size();

        List<Long> carburants = values(tournees, TourneeFigures::carburant);
        List<Long> distances = values(tournees, TourneeFigures::distance);
        List<Double> poids = values(tournees, TourneeFigures::poids);
        List<Integer> livraisons = values(tournees, TourneeFigures::nbLivraisons);
        List<Duration> routes = values(tournees, TourneeFigures::route);
        List<Duration> montages = values(tournees, TourneeFigures::montage);
        List<Duration> totals = values(tournees, TourneeFigures::total);

        long carburantTotal = carburants.stream().mapToLong(Long::longValue).sum();
        long distanceTotal = distances.stream().mapToLong(Long::longValue).sum();
        double poidsTotal = poids.stream().mapToDouble(Double::doubleValue).sum();
        long livraisonsTotal = livraisons.stream().mapToLong(Integer::longValue).sum();

        return new SolutionStatisticsData(
            solution.getId(),
            solution.getDate(),
            solution.getStrategie(),
            (long) count,
            livraisonsTotal,
            min(livraisons),
            max(livraisons),
            count == 0 ? 0d : (double) livraisonsTotal / count,
            carburantTotal,
            min(carburants),
            max(carburants),
            average(carburantTotal, count),
            distanceTotal,
            min(distances),
            max(distances),
            average(distanceTotal, count),
            poidsTotal,
            min(poids),
            max(poids),
            average(poidsTotal, count),
            routes.stream().reduce(Duration.ZERO, Duration::plus),
            min(routes),
            max(routes),
            montages.stream().reduce(Duration.ZERO, Duration::plus),
            min(montages),
            max(montages),
            min(totals),
            max(totals),
            tournees.stream().map(TourneeFigures::finances).reduce(TourneeFinances.ZERO, TourneeFinances::plus)
        );
    }

    private TourneeFigures figuresOf(TourneeEntity tournee) {
        Statistic statistic = tournee.getStatistic();
        Duration route = statistic == null || statistic.getTempsTrajet() == null ? Duration.ZERO : statistic.getTempsTrajet();
        Duration montage = statistic == null || statistic.getTempsMontage() == null ? Duration.ZERO : statistic.getTempsMontage();
        return new TourneeFigures(
            tournee.nbLivraisons(),
            statistic == null ? null : statistic.getCarburant(),
            statistic == null ? null : statistic.getDistance(),
            tournee.poidsTotal(),
            route,
            montage,
            financeCalculator.compute(tournee)
        );
    }

    private static <T> List<T> values(List<TourneeFigures> tournees, Function<TourneeFigures, T> extractor) {
        return tournees.stream().map(extractor).filter(Objects::nonNull).toList();
    }

    private static <T extends Comparable<T>> T min(List<T> values) {
        return values.stream().min(Comparator.naturalOrder()).orElse(null);
    }

    private static <T extends Comparable<T>> T max(List<T> values) {
        return values.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    private static Double average(double total, int count) {
        return count == 0 ? null : total / count;
    }

    private record TourneeFigures(
        int nbLivraisons,
        Long carburant,
        Long distance,
        double poids,
        Duration route,
        Duration montage,
        TourneeFinances finances
    ) {
        Duration total() {
            return route.plus(montage);
        }
    }
}
