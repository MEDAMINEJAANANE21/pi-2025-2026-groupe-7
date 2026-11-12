package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.request.StrategieRequestDTO;
import fr.uga.miage.l3.responses.JourneeResponseDTO;
import fr.uga.miage.l3.responses.JourneeResumeDTO;
import fr.uga.miage.l3.responses.SolutionResponseDTO;
import fr.uga.miage.l3.responses.SolutionStatisticsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Tag(name = "Journées", description = "Planification et optimisation d'une journée")
@RequestMapping("api/journees")
public interface JourneeEndpoints {

    @Operation(summary = "Journées planifiées sur une période", description = "Alimente le calendrier.")
    @ApiResponse(responseCode = "400", description = "Dates invalides",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    List<JourneeResumeDTO> getJournees(
        @Parameter(description = "Premier jour inclus (yyyy-MM-dd)") @RequestParam String debut,
        @Parameter(description = "Dernier jour inclus (yyyy-MM-dd)") @RequestParam String fin
    );

    @Operation(summary = "État d'une journée")
    @ApiResponse(responseCode = "400", description = "Date invalide",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}")
    JourneeResponseDTO getJournee(@PathVariable("date") String date);

    @Operation(summary = "Solutions générées pour une journée, avec leurs statistiques")
    @ApiResponse(responseCode = "400", description = "Date invalide",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}/solutions")
    List<SolutionStatisticsResponseDTO> getJourneeSolutions(@PathVariable("date") String date);

    @Operation(
        summary = "Lance l'optimisation d'une journée (asynchrone)",
        description = """
            Génère une solution par combinaison séparation × moteur × nombre de véhicules.
            L'avancement est publié sur le sujet WebSocket /topic/journee/{date}.

            **Contrainte maxTempsParVehicule** : une livraison qui ferait dépasser la durée maximale
            d'une tournée est retirée de la solution. Elle reste disponible pour une prochaine optimisation.
            """)
    @ApiResponse(responseCode = "202", description = "Optimisation démarrée")
    @ApiResponse(responseCode = "400", description = "Paramètres invalides ou aucune livraison à planifier",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "409", description = "Une optimisation est déjà en cours",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{date}/optimization")
    JourneeResponseDTO startJourneeOptimization(
        @PathVariable("date") String date,
        @RequestBody @Valid StrategieRequestDTO request
    );

    @Operation(summary = "Solution retenue pour la journée")
    @ApiResponse(responseCode = "404", description = "Aucune solution choisie",
        content = @Content(schema = @Schema(implementation = ErrorDTO.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{date}/solutions/choisie")
    SolutionResponseDTO getJourneeSolutionChoisie(@PathVariable("date") String date);
}
