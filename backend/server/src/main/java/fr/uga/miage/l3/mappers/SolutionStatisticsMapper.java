package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.domain.SolutionStatisticsData;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import fr.uga.miage.l3.responses.StrategieResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SolutionStatisticsMapper {
    private final JourneeMapper journeeMapper;

    public SolutionStatisticsResponseDTO toDto(SolutionStatisticsData data) {
        if (data == null) {
            return null;
        }

        StrategieResponseDTO strategie = journeeMapper.toStrategieDto(data.strategie());

        Duration routeTotal = data.tempsRouteTotal();
        Duration routeMin = data.tempsRouteMin();
        Duration routeMax = data.tempsRouteMax();

        Duration montageTotal = data.tempsMontageTotal();
        Duration montageMin = data.tempsMontageMin();
        Duration montageMax = data.tempsMontageMax();

        Duration routeAvg = average(routeTotal, data.nbVehicules());
        Duration montageAvg = average(montageTotal, data.nbVehicules());

        Duration total = add(routeTotal, montageTotal);
        Duration totalMin = data.tempsTotalMin();
        Duration totalMax = data.tempsTotalMax();
        Duration totalAvg = add(routeAvg, montageAvg);

        return new SolutionStatisticsResponseDTO(
            data.id(),
            data.date() == null ? null : data.date().toString(),
            strategie,
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
            toIso(routeTotal),
            toIso(routeMin),
            toIso(routeMax),
            toIso(routeAvg),
            toIso(montageTotal),
            toIso(montageMin),
            toIso(montageMax),
            toIso(montageAvg),
            toIso(total),
            toIso(totalMin),
            toIso(totalMax),
            toIso(totalAvg)
        );
    }

    private String toIso(Duration duration) {
        return duration == null ? null : duration.toString();
    }

    private Duration add(Duration first, Duration second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.plus(second);
    }

    private Duration average(Duration total, Long count) {
        if (total == null || count == null || count <= 0) {
            return null;
        }
        return Duration.ofMillis(total.toMillis() / count);
    }
}
