package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.request.RouteRequest;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.responses.RouteResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("api/routes")
public interface RouteEndpoints {

    @Operation(description = "récupération de la routes geoJSON pour une tournée")
    @ApiResponse(responseCode = "200",
                 description = "La route a été récupérée")
    @ApiResponse(responseCode = "400",
                 description = "La requête n'est pas conforme aux attentes du serveur",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "500",
                 description = "Une des routes n'a pas pu être trouvé",
                 content = @Content(schema = @Schema(implementation = ErrorDTO.class),
                 mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ResponseStatus(HttpStatus.OK)
    @PostMapping() //pas get car on envoie une liste d'adresses dans le body, pas de paramètre dans l'url
    RouteResponsesDTO getRoute(@RequestBody RouteRequest request); //pas toute les routes d'un coup pour éviter de toutes les recalculer lorsque l'on ne modifie qu'une tournée

}
