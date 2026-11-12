package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.request.AdresseRequest;
import fr.uga.miage.l3.responses.AdresseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = CoordsMapper.class)
public interface AdresseMapper {

    @Mapping(target = "coords", source = "coords")
    @Mapping(target = "name", source = "rue")
    @Mapping(target = "postCode", source = "codePostal")
    @Mapping(target = "city", source = "ville")
    Adresse toCore(AdresseEntity entity);

    List<Adresse> toCore(List<AdresseEntity> entities);

    @Mapping(target = "coordonnees", source = "coords")
    AdresseDTO toDTO(AdresseEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "livraisonEntities", ignore = true)
    @Mapping(target = "coords.latitude", source = "lat")
    @Mapping(target = "coords.longitude", source = "lng")
    void update(@MappingTarget AdresseEntity entity, AdresseRequest request);
}
