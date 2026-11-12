package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.LivraisonComponent;
import fr.uga.miage.l3.endpoints.LivraisonEndpoints;
import fr.uga.miage.l3.mappers.LivraisonMapper;
import fr.uga.miage.l3.request.LivraisonRequest;
import fr.uga.miage.l3.request.LivraisonStatutRequest;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LivraisonController implements LivraisonEndpoints {

    private final LivraisonComponent livraisonComponent;
    private final LivraisonMapper livraisonMapper;

    @Override
    public List<LivraisonResponsesDTO> getLivraisons(Boolean livree, String dateDebut, String dateFin) {
        return livraisonMapper.toDTOList(livraisonComponent.search(
            livree, Dates.parseOptional(dateDebut), Dates.parseOptional(dateFin)));
    }

    @Override
    public LivraisonResponsesDTO getLivraison(Long id) {
        return livraisonMapper.toDTO(livraisonComponent.get(id));
    }

    @Override
    public LivraisonResponsesDTO createLivraison(LivraisonRequest request) {
        return livraisonMapper.toDTO(livraisonComponent.create(request));
    }

    @Override
    public LivraisonResponsesDTO updateLivraison(Long id, LivraisonRequest request) {
        return livraisonMapper.toDTO(livraisonComponent.update(id, request));
    }

    @Override
    public void deleteLivraison(Long id) {
        livraisonComponent.delete(id);
    }

    @Override
    public LivraisonResponsesDTO updateStatut(Long id, LivraisonStatutRequest request) {
        return livraisonMapper.toDTO(livraisonComponent.updateStatut(id, request.livree()));
    }
}
