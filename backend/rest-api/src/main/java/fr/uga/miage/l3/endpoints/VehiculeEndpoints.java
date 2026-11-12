package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.VehiculeRequest;
import fr.uga.miage.l3.responses.VehiculeDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Équipe", description = "Chauffeurs et véhicules")
@RequestMapping("api/vehicules")
public interface VehiculeEndpoints {

    @Operation(summary = "Liste les véhicules, triés par immatriculation")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    List<VehiculeDTO> getVehicules();

    @Operation(summary = "Ajoute un véhicule")
    @ApiResponse(responseCode = "409", description = "Immatriculation déjà utilisée",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    VehiculeDTO createVehicule(@RequestBody @Valid VehiculeRequest request);

    @Operation(summary = "Modifie un véhicule")
    @ApiResponse(responseCode = "404", description = "Véhicule introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    VehiculeDTO updateVehicule(@PathVariable Long id, @RequestBody @Valid VehiculeRequest request);

    @Operation(summary = "Supprime un véhicule", description = "Ses tournées perdent leur affectation.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    void deleteVehicule(@PathVariable Long id);

    @Operation(summary = "Ajoute ou remplace la photo du véhicule", description = "JPEG, PNG ou WebP, 3 Mo au plus.")
    @ApiResponse(responseCode = "400", description = "Fichier absent, trop lourd ou d'un format non pris en charge",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VehiculeDTO uploadVehiculePhoto(@PathVariable Long id, @RequestPart("photo") MultipartFile photo);

    @Operation(summary = "Photo du véhicule")
    @ApiResponse(responseCode = "404", description = "Pas de photo",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @GetMapping("/{id}/photo")
    ResponseEntity<byte[]> getVehiculePhoto(@PathVariable Long id);

    @Operation(summary = "Retire la photo du véhicule")
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}/photo")
    VehiculeDTO deleteVehiculePhoto(@PathVariable Long id);
}