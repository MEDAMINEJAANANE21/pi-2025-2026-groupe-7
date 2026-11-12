package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.JourneeResumeDTO;
import fr.uga.miage.l3.responses.StrategieResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class JourneeMapper {

    public JourneeResponseDTO toDto(JourneeEntity journee) {
        return new JourneeResponseDTO(
            journee.getDate().toString(),
            journee.getStatutGeneration() == null ? null : journee.getStatutGeneration().name(),
            journee.getSolutionEntity() == null ? null : toStrategieDto(journee.getSolutionEntity().getStrategie())
        );
    }

    public JourneeResumeDTO toResume(JourneeEntity journee, int nbSolutions) {
        return new JourneeResumeDTO(
            journee.getDate().toString(),
            journee.getStatutGeneration() == null ? null : journee.getStatutGeneration().name(),
            nbSolutions,
            journee.getSolutionEntity() == null ? null : journee.getSolutionEntity().getId()
        );
    }

    public StrategieResponseDTO toStrategieDto(Strategie strategie) {
        if (strategie == null) {
            return null;
        }
        return new StrategieResponseDTO(
            strategie.getSeparation() == null ? null : strategie.getSeparation().name(),
            strategie.getOptimisation() == null ? null : strategie.getOptimisation().name(),
            strategie.getNbVehicules(),
            strategie.getMaxTempsParVehicule() == null ? null : strategie.getMaxTempsParVehicule().toString()
        );
    }
}
