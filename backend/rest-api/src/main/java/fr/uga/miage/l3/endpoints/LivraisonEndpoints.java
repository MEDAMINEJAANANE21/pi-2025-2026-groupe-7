package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.LivraisonRequest;
import fr.uga.miage.l3.request.LivraisonStatutRequest;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Tag(name = "Livraisons", description = "Commandes à livrer chez les clients")
@RequestMapping("api/livraisons")
public interface LivraisonEndpoints {

    @Operation(summary = "Liste les livraisons",
        description = "Sans filtre, seules les livraisons non livrées sont renvoyées.")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    List<LivraisonResponsesDTO> getLivraisons(
        @Parameter(description = "true = livrées, false = non livrées")
        @RequestParam(required = false) Boolean livree,
        @Parameter(description = "Date de commande minimale (yyyy-MM-dd)")
        @RequestParam(required = false) String dateDebut,
        @Parameter(description = "Date de commande maximale (yyyy-MM-dd)")
        @RequestParam(required = false) String dateFin
    );

    @Operation(summary = "Détail d'une livraison")
    @ApiResponse(responseCode = "404", description = "Livraison introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    LivraisonResponsesDTO getLivraison(@PathVariable Long id);

    @Operation(summary = "Crée une livraison")
    @ApiResponse(responseCode = "400", description = "Données invalides",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    LivraisonResponsesDTO createLivraison(@RequestBody @Valid LivraisonRequest request);

    @Operation(summary = "Modifie une livraison")
    @ApiResponse(responseCode = "404", description = "Livraison introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    LivraisonResponsesDTO updateLivraison(@PathVariable Long id, @RequestBody @Valid LivraisonRequest request);

    @Operation(summary = "Supprime une livraison",
        description = "La livraison est aussi retirée des tournées qui la contiennent.")
    @ApiResponse(responseCode = "404", description = "Livraison introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    void deleteLivraison(@PathVariable Long id);

    @Operation(summary = "Marque une livraison comme livrée ou non livrée")
    @ApiResponse(responseCode = "404", description = "Livraison introuvable",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{id}/statut")
    LivraisonResponsesDTO updateStatut(@PathVariable Long id, @RequestBody @Valid LivraisonStatutRequest request);
}
