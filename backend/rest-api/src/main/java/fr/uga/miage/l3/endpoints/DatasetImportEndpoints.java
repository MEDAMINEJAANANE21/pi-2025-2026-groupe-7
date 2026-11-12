package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.DatasetImportRequest;
import fr.uga.miage.l3.request.RandomLivraisonsGenerationRequest;
import fr.uga.miage.l3.responses.DatasetImportResponseDTO;
import fr.uga.miage.l3.responses.RandomGenerationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;

@RequestMapping("api/data")
public interface DatasetImportEndpoints {

    @Operation(description = "récupération de la liste des datasets disponibles")
    @ApiResponse(responseCode = "200",
                 description = "La liste des datasets disponibles a été récupérée")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    Set<String> listAvailableDatasets();

    @Operation(description = "importe un dataset (adresses + matrices) en base de données")
    @ApiResponse(responseCode = "200",
                 description = "Le dataset a été importé")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/import")
    DatasetImportResponseDTO importDataset(@RequestBody DatasetImportRequest request);

    @Operation(description = "génère aléatoirement des livraisons")
    @ApiResponse(responseCode = "201",
                 description = "Les livraisons ont été générées")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/generate/livraisons")
    RandomGenerationResponseDTO generateRandomLivraisons(@RequestBody RandomLivraisonsGenerationRequest request);
}
