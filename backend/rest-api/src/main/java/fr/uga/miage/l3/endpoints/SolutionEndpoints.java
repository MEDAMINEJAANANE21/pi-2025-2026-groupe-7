package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.request.SolutionCreationRequest;
import fr.uga.miage.l3.request.SolutionTourneesUpdateRequest;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.responses.SolutionLivraisonsResponseDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("api/solutions")
public interface SolutionEndpoints {

    @Operation(description = "récupération d'une solution'")
    @ApiResponse(responseCode = "200",
                 description = "La solution a été récupérée")
    @ApiResponse(responseCode = "404",
                 description = "La solution n'a pas été trouvée",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{solutionId}")
    SolutionResponseDTO getSolution(@PathVariable Long solutionId);

    @Operation(description = "récupération des livraisons dans et hors d'une solution")
    @ApiResponse(responseCode = "200",
                 description = "Les livraisons ont été récupérées")
    @ApiResponse(responseCode = "404",
                 description = "La solution n'a pas été trouvée",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{solutionId}/livraisons")
    SolutionLivraisonsResponseDTO getSolutionLivraisons(@PathVariable Long solutionId);

    @Operation(description = "création d'un nouvelle solution à partie de la modification d'une'tournée existante")
    @ApiResponse(responseCode = "201",
                 description = "La solution a été créée")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    SolutionResponseDTO createSolution(@RequestBody SolutionCreationRequest request);

    @Operation(description = "marque une solution comme choisie pour la journée correspondante")
    @ApiResponse(responseCode = "200",
                 description = "La solution a été marquée comme choisie")
    @ApiResponse(responseCode = "404",
                 description = "La solution n'a pas été trouvée",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{solutionId}/choisie")
    SolutionResponseDTO choisirSolution(@PathVariable Long solutionId);

    @Operation(description = "mise à jour de l'ordre des livraisons dans les tournées d'une solution")
    @ApiResponse(responseCode = "200",
                 description = "Les tournées de la solution ont été mises à jour")
    @ApiResponse(responseCode = "400",
                 description = "Requête invalide : livraison appartenant à une autre date ou ID invalide",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404",
                 description = "Solution ou tournée non trouvée",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{solutionId}/tournees")
    SolutionResponseDTO updateSolutionTournees(@PathVariable Long solutionId,
                                               @RequestBody @Valid SolutionTourneesUpdateRequest request);

    @Operation(description = "Recalcule les statistiques de chaque tournée via OpenRouteService et met à jour la solution")
    @ApiResponse(responseCode = "200",
                 description = "Les statistiques de la solution ont été recalculées")
    @ApiResponse(responseCode = "404",
                 description = "La solution n'a pas été trouvée",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{solutionId}/recalculate-stats")
    SolutionResponseDTO recalculateSolutionStats(@PathVariable Long solutionId);

}
