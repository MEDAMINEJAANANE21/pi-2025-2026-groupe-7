package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = AdresseMapper.class)
public interface LivraisonMapper {

    @Mapping(target = "adresse", source = "adresseEntity")
    @Mapping(target = "tempsMontage", expression = "java(entity.getTempsMontage() == null ? null : entity.getTempsMontage().toString())")
    LivraisonResponsesDTO toDTO(LivraisonEntity entity);

    List<LivraisonResponsesDTO> toDTOList(List<LivraisonEntity> entities);
}
