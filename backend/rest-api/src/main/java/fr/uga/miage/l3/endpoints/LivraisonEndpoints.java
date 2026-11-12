package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("api/livraisons")
public interface LivraisonEndpoints {
    @Operation(description = "récupération de la liste des livraisons")
    @ApiResponse(responseCode = "200",
                 description = "La liste des livraisons a été récupérée")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping()
    List<LivraisonResponsesDTO> getLivraisons(
        @Parameter(description = "Filtrer par statut de livraison (true = livrée, false = non livrée)")
        @RequestParam(required = false) Boolean livree,
        @Parameter(description = "Filtrer les livraisons dont la dateCommande est >= dateDebut (format yyyy-MM-dd)")
        @RequestParam(required = false) String dateDebut,
        @Parameter(description = "Filtrer les livraisons dont la dateCommande est <= dateFin (format yyyy-MM-dd)")
        @RequestParam(required = false) String dateFin
    );

}
