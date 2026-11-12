package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import fr.uga.miage.l3.responses.TourneeStatisticsResponseDTO;
import fr.uga.miage.l3.services.statistics.FinanceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Écrit à la main car le bilan financier est calculé à la lecture, pas stocké.
 */
@Component
@RequiredArgsConstructor
public class TourneeMapper {

    private final LivraisonMapper livraisonMapper;
    private final ChauffeurMapper chauffeurMapper;
    private final VehiculeMapper vehiculeMapper;
    private final FinancesMapper financesMapper;
    private final FinanceCalculator financeCalculator;

    public TourneeResponsesDTO toDto(TourneeEntity tournee) {
        return new TourneeResponsesDTO(
            tournee.getId(),
            livraisonMapper.toDTOList(tournee.getLivraisonEntities() == null ? List.of() : tournee.getLivraisonEntities()),
            toDto(tournee.getStatistic()),
            financesMapper.toDto(financeCalculator.compute(tournee)),
            tournee.poidsTotal(),
            tournee.getChauffeur() == null ? null : chauffeurMapper.toDto(tournee.getChauffeur()),
            tournee.getVehicule() == null ? null : vehiculeMapper.toDto(tournee.getVehicule())
        );
    }

    private static TourneeStatisticsResponseDTO toDto(Statistic statistic) {
        if (statistic == null) {
            return null;
        }
        return new TourneeStatisticsResponseDTO(
            statistic.getCarburant(),
            statistic.getDistance(),
            toIso(statistic.getTempsTrajet()),
            toIso(statistic.getTempsMontage())
        );
    }

    private static String toIso(Duration duration) {
        return duration == null ? null : duration.toString();
    }
}
