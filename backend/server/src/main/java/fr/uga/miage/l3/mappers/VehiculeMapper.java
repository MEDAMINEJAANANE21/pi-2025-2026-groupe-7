package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.request.VehiculeRequest;
import fr.uga.miage.l3.responses.VehiculeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehiculeMapper {

    @Mapping(target = "photoUrl", expression = "java(PhotoUrls.of(\"vehicules\", entity.getId(), entity.getPhoto()))")
    VehiculeDTO toDto(VehiculeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "photo", ignore = true)
    @Mapping(target = "immatriculation", expression = "java(request.immatriculation().trim().toUpperCase())")
    @Mapping(target = "actif", expression = "java(request.actif() == null || request.actif())")
    void update(@MappingTarget VehiculeEntity entity, VehiculeRequest request);
}
