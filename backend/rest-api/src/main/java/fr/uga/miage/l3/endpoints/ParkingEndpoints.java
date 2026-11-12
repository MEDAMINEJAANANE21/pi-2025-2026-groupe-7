package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.request.ParkingCreationRequest;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("api/parkings")
public interface ParkingEndpoints {

    @Operation(description = "récupération de l'adresse du parking")
    @ApiResponse(responseCode = "200",
                 description = "L'adresse du parking a été récupérée")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping()
    ParkingResponsesDTO getParking();

}
