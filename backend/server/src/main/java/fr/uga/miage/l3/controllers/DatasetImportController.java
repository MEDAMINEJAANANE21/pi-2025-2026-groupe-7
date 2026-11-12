package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.endpoints.DatasetImportEndpoints;
import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.mappers.DatasetImportMapper;
import fr.uga.miage.l3.request.DatasetImportRequest;
import fr.uga.miage.l3.request.RandomLivraisonsGenerationRequest;
import fr.uga.miage.l3.responses.DatasetImportResponseDTO;
import fr.uga.miage.l3.responses.RandomGenerationResponseDTO;
import fr.uga.miage.l3.services.RandomDataGenerationService;
import fr.uga.miage.l3.services.core.DatasetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class DatasetImportController implements DatasetImportEndpoints {

  private final DatasetImportService datasetImportService;
  private final DatasetImportMapper datasetImportMapper;
  private final RandomDataGenerationService randomDataGenerationService;

  @Override
  public Set<String> listAvailableDatasets() {
    return Arrays.stream(DatasetDefinition.values())
      .map(Enum::name)
      .collect(java.util.stream.Collectors.toSet());
  }

  @Override
  public DatasetImportResponseDTO importDataset(DatasetImportRequest request) {
    if (request == null || request.dataset() == null || request.dataset().isBlank()) {
      throw new BadRequestRestException("Dataset ne peut pas etre null");
    }

    DatasetDefinition definition;
    try {
      definition = DatasetDefinition.fromValue(request.dataset());
    } catch (IllegalArgumentException e) {
      throw new BadRequestRestException(e.getMessage());
    }

    boolean clearBeforeImport = request.clearBeforeImport() == null || request.clearBeforeImport();

    return datasetImportMapper.toDto(
      datasetImportService.importDataset(definition, clearBeforeImport)
    );
  }

  @Override
  public RandomGenerationResponseDTO generateRandomLivraisons(RandomLivraisonsGenerationRequest request) {
    int count = extractValidCount(request == null ? null : request.count());

    try {
      RandomDataGenerationService.GenerationStats stats = randomDataGenerationService.generateLivraisons(count);
      return new RandomGenerationResponseDTO(
        "livraisons",
        stats.generated(),
        stats.addressesAvailable(),
        stats.livraisonsAvailable()
      );
    } catch (IllegalStateException e) {
      throw new BadRequestRestException(e.getMessage());
    }
  }

  private int extractValidCount(Integer count) {
    if (count == null || count <= 0) {
      throw new BadRequestRestException("count doit etre un entier strictement positif");
    }
    return count;
  }
}
