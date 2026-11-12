package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.request.TourneeCreationRequest;
import fr.uga.miage.l3.request.TourneeModificationRequest;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RequestMapping("api/tournees")
public interface TourneeEndpoints {

    @Operation(description = "récupération de la liste des tournees")
    @ApiResponse(responseCode = "200",
                 description = "La liste des tournees a été récupérée")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    Set<TourneeResponsesDTO> getTournees();

    @Operation(description = "création des tournees par optimisation à partir des livraisons déja stocké")
    @ApiResponse(responseCode = "201",
                 description = "Les tournees ont été créées")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "500",
                 description = "L'optimisation a échoué",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    Set<TourneeResponsesDTO> createTournees(@RequestBody TourneeCreationRequest request);

    @Operation(description = "modification des tournees existantes")
    @ApiResponse(responseCode = "200",
                 description = "Les tournees ont été modifiée")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404",
                 description = "Au moins une des tournées ou des adresses n'a pas été trouvé",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping
    Set<TourneeResponsesDTO> updateTournees(@RequestBody Set<TourneeModificationRequest> requests); //il faudra rerécupérer/recalculer les routes après les modifications (à voir ducoup si on dois pas renvoyer des List<RouteResponseDTO> ducoup) mais ne pas réoptimiser

}
