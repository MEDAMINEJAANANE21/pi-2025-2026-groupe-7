package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.responses.FinancesDTO;
import fr.uga.miage.l3.services.statistics.TourneeFinances;
import org.springframework.stereotype.Component;

@Component
public class FinancesMapper {

    public FinancesDTO toDto(TourneeFinances finances) {
        if (finances == null) {
            return null;
        }
        return new FinancesDTO(
            round(finances.coutCarburant()),
            round(finances.coutChauffeur()),
            round(finances.coutTotal()),
            round(finances.chiffreAffaires()),
            round(finances.marge())
        );
    }

    private static double round(double euros) {
        return Math.round(euros * 100.0) / 100.0;
    }
}
