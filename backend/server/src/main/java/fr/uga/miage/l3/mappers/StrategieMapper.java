package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import org.mapstruct.Mapper;

import java.time.Duration;
import java.time.format.DateTimeParseException;

@Mapper(componentModel = "spring")
public interface StrategieMapper {

  default Strategie toEntity(StrategieRequestDTO request) {
    if (request == null) {
      return null;
    }

    Strategie strategie = new Strategie();
    strategie.setNbVehicules(request.nbVehicules());
    strategie.setMaxTempsParVehicule(toDuration(request.maxTempsParVehicule()));
    return strategie;
  }

  default Duration toDuration(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Duration.parse(value.trim());
    } catch (DateTimeParseException ex) {
      throw new InvalidRequestException("maxTempsParVehicule doit être une durée ISO-8601 (ex. PT2H30M)");
    }
  }
}
