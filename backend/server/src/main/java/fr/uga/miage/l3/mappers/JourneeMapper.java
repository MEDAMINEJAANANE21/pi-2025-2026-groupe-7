package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.StrategieResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JourneeMapper {

  @Mapping(target = "date", expression = "java(toDateString(journee.getDate()))")
  @Mapping(target = "statutGeneration", expression = "java(toStatusString(journee))")
  @Mapping(target = "strategie", expression = "java(toStrategieDto(journee))")
  JourneeResponseDTO toDto(JourneeEntity journee);

  default String toDateString(java.time.LocalDate date) {
    return date == null ? null : date.toString();
  }

  default String toStatusString(JourneeEntity journee) {
    if (journee == null || journee.getStatutGeneration() == null) {
      return null;
    }
    return journee.getStatutGeneration().name();
  }

  default StrategieResponseDTO toStrategieDto(JourneeEntity journee) {
    if (journee == null || journee.getSolutionEntity() == null) {
      return null;
    }
    return toStrategieDto(journee.getSolutionEntity().getStrategie());
  }

  default StrategieResponseDTO toStrategieDto(Strategie strategie) {
    if (strategie == null) {
      return null;
    }

    String maxTempsParVehicule = formatDuration(strategie.getMaxTempsParVehicule());

    return new StrategieResponseDTO(
      strategie.getSeparation() == null ? null : strategie.getSeparation().name(),
      strategie.getOptimisation() == null ? null : strategie.getOptimisation().name(),
      strategie.getNbVehicules(),
      maxTempsParVehicule
    );
  }

  default String formatDuration(java.time.Duration duration) {
    if (duration == null) {
      return null;
    }

    return duration.toString();
  }
}
