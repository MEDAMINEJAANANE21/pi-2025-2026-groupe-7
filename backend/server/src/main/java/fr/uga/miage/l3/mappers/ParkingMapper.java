package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AdresseMapper.class)

public interface ParkingMapper {

    ParkingResponsesDTO toDTO(ParkingEntity parking);
}
