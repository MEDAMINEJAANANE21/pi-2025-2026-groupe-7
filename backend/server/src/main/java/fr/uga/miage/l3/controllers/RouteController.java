package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.mappers.CoordsMapper;
import fr.uga.miage.l3.services.DirectionsService;
import fr.uga.miage.l3.request.RouteRequest;
import fr.uga.miage.l3.responses.RouteResponsesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fr.uga.miage.l3.endpoints.RouteEndpoints;

@RestController
@RequiredArgsConstructor
public class RouteController implements RouteEndpoints {
    private final DirectionsService directionsService;
    private final CoordsMapper coordsMapper;

    @Override
    public RouteResponsesDTO getRoute(RouteRequest request) {
        if (request.adresses() == null) {
            throw new BadRequestRestException("Le corps du request doit contenir une liste d'adresses");
        }
        return new RouteResponsesDTO(coordsMapper.toCoordonneeDTOList(
            directionsService.getDirectionsFromCoords(
                coordsMapper.toCoordsList(request.adresses())
            )
        ));
    }

}
