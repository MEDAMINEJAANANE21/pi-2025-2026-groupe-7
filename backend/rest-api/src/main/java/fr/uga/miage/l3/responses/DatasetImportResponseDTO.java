package fr.uga.miage.l3.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de l'import d'un dataset")
public record DatasetImportResponseDTO(
    String dataset,
    int addressesImported,
    int cheminsImported
) {
}