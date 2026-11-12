package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.LivraisonComponent;
import fr.uga.miage.l3.endpoints.LivraisonEndpoints;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import fr.uga.miage.l3.exceptions.technical.MissingAdresseException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LivraisonController implements LivraisonEndpoints {

  private final LivraisonComponent livraisonComponent;
  private final AdresseMapper adresseMapper;

  @Override
  public List<LivraisonResponsesDTO> getLivraisons(Boolean livree, String dateDebut, String dateFin) {
      try {
          LocalDate parsedDateDebut = dateDebut != null ? LocalDate.parse(dateDebut) : null;
          LocalDate parsedDateFin = dateFin != null ? LocalDate.parse(dateFin) : null;
          List<LivraisonEntity> livraisons = livraisonComponent.getLivraisons(livree, parsedDateDebut, parsedDateFin);
          return livraisons
              .stream()
              .map(l -> new LivraisonResponsesDTO(l.getId(), l.getPoids(), adresseMapper.toDTO(l.getAdresseEntity()), l.isLivree(), l.getDateCommande(), l.getTempsMontage() == null ? null : l.getTempsMontage().toString()))
              .toList();
      } catch (MissingAdresseException e) {
          throw new NotFoundElementRestException("Error fetching livraisons: " + e.getMessage());
      }
  }
}
