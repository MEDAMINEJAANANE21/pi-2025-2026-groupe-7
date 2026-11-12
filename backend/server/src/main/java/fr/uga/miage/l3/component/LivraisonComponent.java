package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.technical.MissingAdresseException;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.repository.LivraisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LivraisonComponent {
    private final LivraisonRepository livraisonRepository;

    public List<LivraisonEntity> getLivraisonsNonLivrees() throws MissingAdresseException {
        List<LivraisonEntity> livraisons = livraisonRepository.findAllByLivree(false);
        for (LivraisonEntity livraison : livraisons) {
            if (livraison.getAdresseEntity() == null) {
                throw new MissingAdresseException("Adresse missing for livraison " + livraison.getId());
            }
        }
        return livraisons;
    }

    /**
     * Returns livraisons with optional filtering.
     * When all parameters are null, returns only non-delivered livraisons (backward compat).
     * When {@code livree} is explicitly set, filters by that value.
     * Date filters apply on top of the livree filter.
     */
    public List<LivraisonEntity> getLivraisons(Boolean livree, LocalDate dateDebut, LocalDate dateFin) throws MissingAdresseException {
        // Backward compatibility: no params → return only non-delivered livraisons
        boolean allParamsNull = livree == null && dateDebut == null && dateFin == null;

        // Load base set
        List<LivraisonEntity> livraisons;
        if (allParamsNull) {
            // backward compat: only non-delivered
            livraisons = livraisonRepository.findAllByLivree(false);
        } else if (livree != null) {
            livraisons = livraisonRepository.findAllByLivree(livree);
        } else {
            livraisons = livraisonRepository.findAll();
        }

        // Apply date filters in Java
        final LocalDate effectiveDebut = dateDebut;
        final LocalDate effectiveFin = dateFin;
        if (effectiveDebut != null || effectiveFin != null) {
            livraisons = livraisons.stream()
                .filter(l -> effectiveDebut == null || (l.getDateCommande() != null && !l.getDateCommande().isBefore(effectiveDebut)))
                .filter(l -> effectiveFin == null || (l.getDateCommande() != null && !l.getDateCommande().isAfter(effectiveFin)))
                .toList();
        }

        for (LivraisonEntity livraison : livraisons) {
            if (livraison.getAdresseEntity() == null) {
                throw new MissingAdresseException("Adresse missing for livraison " + livraison.getId());
            }
        }
        return livraisons;
    }

}
