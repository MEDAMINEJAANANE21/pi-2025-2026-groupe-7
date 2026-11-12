package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.AffectationRequest;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Tournées", description = "Parcours d'un véhicule dans une solution")
@RequestMapping("api/tournees")
public interface TourneeEndpoints {

    @Operation(summary = "Détail d'une tournée")
    @ApiResponse(responseCode = "404", description = "Tournée introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    TourneeResponsesDTO getTournee(@PathVariable Long id);

    @Operation(summary = "Affecte un chauffeur et un véhicule à la tournée",
        description = "Refusé (409) si le chauffeur ou le véhicule est déjà pris dans la même solution, "
            + "ou si le poids dépasse la capacité du véhicule.")
    @ApiResponse(responseCode = "409", description = "Affectation impossible",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}/affectation")
    TourneeResponsesDTO affecter(@PathVariable Long id, @RequestBody AffectationRequest request);

    @Operation(summary = "Clôture la tournée : toutes ses livraisons passent à « livrée »")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{id}/terminer")
    TourneeResponsesDTO terminer(@PathVariable Long id);

    @Operation(summary = "Feuille de route du chauffeur au format CSV (séparateur ;)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/csv"))
    @GetMapping(value = "/{id}/feuille-de-route", produces = "text/csv")
    ResponseEntity<byte[]> feuilleDeRoute(@PathVariable Long id);
}
