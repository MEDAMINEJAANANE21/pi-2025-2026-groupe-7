package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.domain.SolutionStatisticsData;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SolutionStatisticsMapper {

    private final JourneeMapper journeeMapper;
    private final FinancesMapper financesMapper;

    public SolutionStatisticsResponseDTO toDto(SolutionStatisticsData data) {
        Duration routeAverage = average(data.tempsRouteTotal(), data.nbVehicules());
        Duration montageAverage = average(data.tempsMontageTotal(), data.nbVehicules());
        return new SolutionStatisticsResponseDTO(
            data.id(),
            data.date() == null ? null : data.date().toString(),
            journeeMapper.toStrategieDto(data.strategie()),
            data.nbVehicules(),
            data.nbLivraisonsTotal(),
            data.nbLivraisonsMin(),
            data.nbLivraisonsMax(),
            data.nbLivraisonsMoyenne(),
            data.carburantTotal(),
            data.carburantMin(),
            data.carburantMax(),
            data.carburantMoyenne(),
            data.distanceTotal(),
            data.distanceMin(),
            data.distanceMax(),
            data.distanceMoyenne(),
            data.poidsTotal(),
            data.poidsMin(),
            data.poidsMax(),
            data.poidsMoyenne(),
            toIso(data.tempsRouteTotal()),
            toIso(data.tempsRouteMin()),
            toIso(data.tempsRouteMax()),
            toIso(routeAverage),
            toIso(data.tempsMontageTotal()),
            toIso(data.tempsMontageMin()),
            toIso(data.tempsMontageMax()),
            toIso(montageAverage),
            toIso(add(data.tempsRouteTotal(), data.tempsMontageTotal())),
            toIso(data.tempsTotalMin()),
            toIso(data.tempsTotalMax()),
            toIso(add(routeAverage, montageAverage)),
            financesMapper.toDto(data.finances())
        );
    }

    private static String toIso(Duration duration) {
        return duration == null ? null : duration.toString();
    }

    private static Duration add(Duration first, Duration second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first.plus(second);
    }

    private static Duration average(Duration total, Long count) {
        if (total == null || count == null || count <= 0) {
            return null;
        }
        return Duration.ofMillis(total.toMillis() / count);
    }
}
