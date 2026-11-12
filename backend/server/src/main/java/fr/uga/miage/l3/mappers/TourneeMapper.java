package fr.uga.miage.l3.mappers;

import org.mapstruct.Mapper;

import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import fr.uga.miage.l3.responses.TourneeStatisticsResponseDTO;
import fr.uga.miage.l3.config.SpringContext;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TourneeMapper {
    default TourneeResponsesDTO toTourneeResponsesDTO(TourneeEntity tourneeEntity) {
        LivraisonMapper livraisonMapper = SpringContext.getBean(LivraisonMapper.class);

        Long id = tourneeEntity.getId();
        List<LivraisonResponsesDTO> livraisons = new ArrayList<>();
        if (tourneeEntity.getLivraisonEntities() != null) {
            tourneeEntity.getLivraisonEntities().forEach(
                livraisonEntity -> livraisons.add(livraisonMapper.toDTO(livraisonEntity))
            );
        }

        var statEntity = tourneeEntity.getStatistic();
        TourneeStatisticsResponseDTO stats = null;
        if (statEntity != null) {
            String tempsTrajet = statEntity.getTempsTrajet() == null ? null : statEntity.getTempsTrajet().toString();
            String tempsMontage = statEntity.getTempsMontage() == null ? null : statEntity.getTempsMontage().toString();
            stats = new TourneeStatisticsResponseDTO(
                statEntity.getCarburant(),
                statEntity.getDistance(),
                tempsTrajet,
                tempsMontage
            );
        }
        return new TourneeResponsesDTO(id, livraisons, stats);
    }
}
