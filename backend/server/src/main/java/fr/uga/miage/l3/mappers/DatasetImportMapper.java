package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.responses.DatasetImportResponseDTO;
import fr.uga.miage.l3.services.core.DatasetImportService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DatasetImportMapper {

  @Mapping(target = "dataset", source = "dataset")
  @Mapping(target = "addressesImported", source = "addressesImported")
  @Mapping(target = "cheminsImported", source = "cheminsImported")
  DatasetImportResponseDTO toDto(DatasetImportService.DatasetImportResult result);
}
