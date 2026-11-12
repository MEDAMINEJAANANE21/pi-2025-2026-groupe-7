package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.TourneeComponent;
import fr.uga.miage.l3.endpoints.TourneeEndpoints;
import fr.uga.miage.l3.mappers.TourneeMapper;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.request.AffectationRequest;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import fr.uga.miage.l3.services.FeuilleDeRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class TourneeController implements TourneeEndpoints {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final TourneeComponent tourneeComponent;
    private final TourneeMapper tourneeMapper;
    private final FeuilleDeRouteService feuilleDeRouteService;

    @Override
    public TourneeResponsesDTO getTournee(Long id) {
        return tourneeMapper.toDto(tourneeComponent.get(id));
    }

    @Override
    public TourneeResponsesDTO affecter(Long id, AffectationRequest request) {
        return tourneeMapper.toDto(tourneeComponent.affecter(id, request));
    }

    @Override
    public TourneeResponsesDTO terminer(Long id) {
        return tourneeMapper.toDto(tourneeComponent.terminer(id));
    }

    @Override
    public ResponseEntity<byte[]> feuilleDeRoute(Long id) {
        TourneeEntity tournee = tourneeComponent.get(id);
        ContentDisposition attachment = ContentDisposition.attachment()
            .filename(feuilleDeRouteService.fileName(tournee))
            .build();
        return ResponseEntity.ok()
            .contentType(CSV)
            .header(HttpHeaders.CONTENT_DISPOSITION, attachment.toString())
            .body(feuilleDeRouteService.toCsv(tournee));
    }
}
