package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.ChauffeurRequest;
import fr.uga.miage.l3.responses.ChauffeurDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Tag(name = "Équipe", description = "Chauffeurs et véhicules")
@RequestMapping("api/chauffeurs")
public interface ChauffeurEndpoints {

    @Operation(summary = "Liste les chauffeurs, triés par nom")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    List<ChauffeurDTO> getChauffeurs();

    @Operation(summary = "Ajoute un chauffeur")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    ChauffeurDTO createChauffeur(@RequestBody @Valid ChauffeurRequest request);

    @Operation(summary = "Modifie un chauffeur")
    @ApiResponse(responseCode = "404", description = "Chauffeur introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    ChauffeurDTO updateChauffeur(@PathVariable Long id, @RequestBody @Valid ChauffeurRequest request);

    @Operation(summary = "Supprime un chauffeur", description = "Ses tournées perdent leur affectation.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    void deleteChauffeur(@PathVariable Long id);
}
