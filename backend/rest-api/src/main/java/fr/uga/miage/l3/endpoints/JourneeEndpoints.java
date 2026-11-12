package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RequestMapping("api/journees")
public interface JourneeEndpoints {

    @Operation(description = "récupération d'une journée par date")
    @ApiResponse(responseCode = "200",
        description = "La journée a été récupérée")
    @ApiResponse(responseCode = "400",
        description = "La requête n'est pas conforme aux attentes du serveur",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class),
        mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}")
    JourneeResponseDTO getJournee(@PathVariable("date") String date);

    @Operation(description = "récupération des solutions générées pour une journée")
    @ApiResponse(responseCode = "200",
        description = "Les solutions ont été récupérées")
    @ApiResponse(responseCode = "400",
        description = "La requête n'est pas conforme aux attentes du serveur",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class),
        mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}/solutions")
    Set<SolutionStatisticsResponseDTO> getJourneeSolutions(@PathVariable("date") String date);

    @Operation(
        summary = "Démarre l'optimisation d'une journée",
        description = """
            Déclenche l'algorithme d'optimisation des tournées pour la journée indiquée.

            **Post-traitement — contrainte `maxTempsParVehicule`**
            Après l'optimisation, un post-traitement est appliqué : toute livraison dont \
            l'intégration dans une tournée dépasserait la durée maximale autorisée \
            (`maxTempsParVehicule`) est **exclue de la solution finale**. \
            Ces livraisons hors-contrainte n'apparaissent dans aucune tournée de la solution \
            retournée — elles restent dans la journée et peuvent être prises en compte lors \
            d'une prochaine optimisation avec des paramètres différents.
            """)
    @ApiResponse(responseCode = "202",
        description = "L'optimisation a été démarrée")
    @ApiResponse(responseCode = "400",
        description = "La requête n'est pas conforme aux attentes du serveur",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class),
        mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{date}/optimization")
    JourneeResponseDTO startJourneeOptimization(
        @PathVariable("date") String date,
        @RequestBody @Valid StrategieRequestDTO request
    );

    @Operation(description = "récupération de la solution choisie pour une journée")
    @ApiResponse(responseCode = "200",
        description = "La solution choisie a été récupérée")
    @ApiResponse(responseCode = "404",
        description = "Aucune solution choisie pour cette journée",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class),
        mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}/solutions/choisie")
    SolutionResponseDTO getJourneeSolutionChoisie(@PathVariable("date") String date);
}
