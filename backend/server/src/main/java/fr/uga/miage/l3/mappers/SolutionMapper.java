package fr.uga.miage.l3.mappers;

import org.mapstruct.Mapper;

import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TourneeMapper.class, fr.uga.miage.l3.mappers.SolutionStatisticsMapper.class})
public interface SolutionMapper {
    @Mappings({
        @Mapping(target = "tournees", source = "tourneeEntities"),
        @Mapping(target = "statistiques", expression = "java(null)") // mapped separately in component
    })
  SolutionResponseDTO toSolutionResponseDTO(SolutionEntity solutionEntity);
}
