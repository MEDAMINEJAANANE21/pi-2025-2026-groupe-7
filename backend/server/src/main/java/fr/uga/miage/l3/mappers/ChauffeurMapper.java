package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.request.ChauffeurRequest;
import fr.uga.miage.l3.responses.ChauffeurDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ChauffeurMapper {

    ChauffeurDTO toDto(ChauffeurEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actif", expression = "java(request.actif() == null || request.actif())")
    void update(@MappingTarget ChauffeurEntity entity, ChauffeurRequest request);
}
