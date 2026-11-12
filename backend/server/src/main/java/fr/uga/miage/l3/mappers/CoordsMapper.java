package fr.uga.miage.l3.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import fr.uga.miage.l3.models.core.Coords;
import fr.uga.miage.l3.responses.CoordonneeDTO;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CoordsMapper {

  // Map from entity Coords (latitude/longitude) to core Coords (lat/lng)
  @Mapping(target = "lat", source = "latitude")
  @Mapping(target = "lng", source = "longitude")
  Coords toCore(fr.uga.miage.l3.models.entities.Coords coords);

  Coords toCoords(CoordonneeDTO coordonneeDTO);
  List<Coords> toCoordsList(List<CoordonneeDTO> coordonneesDTO);

  CoordonneeDTO toCoordonneeDTO(Coords coords);
  List<CoordonneeDTO> toCoordonneeDTOList(List<Coords> coords);
}
