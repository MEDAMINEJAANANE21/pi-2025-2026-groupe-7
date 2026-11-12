package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.AdresseRequest;
import fr.uga.miage.l3.request.LivraisonRequest;
import fr.uga.miage.l3.services.LivraisonReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LivraisonComponent {

    private final LivraisonRepository livraisonRepository;
    private final AdresseRepository adresseRepository;
    private final TourneeRepository tourneeRepository;
    private final SolutionRepository solutionRepository;
    private final AdresseMapper adresseMapper;
    private final Clock clock;

    /**
     * Sans aucun filtre, seules les livraisons non livrées sont renvoyées : c'est ce que
     * le planificateur veut voir par défaut.
     */
    public List<LivraisonEntity> search(Boolean livree, LocalDate dateDebut, LocalDate dateFin) {
        boolean noFilter = livree == null && dateDebut == null && dateFin == null;
        List<LivraisonEntity> candidates = noFilter || livree != null
            ? livraisonRepository.findAllByLivree(noFilter ? false : livree)
            : livraisonRepository.findAll();
        return candidates.stream()
            .filter(livraison -> isOnOrAfter(livraison.getDateCommande(), dateDebut))
            .filter(livraison -> isOnOrBefore(livraison.getDateCommande(), dateFin))
            .sorted(Comparator.comparing(LivraisonEntity::getId))
            .toList();
    }

    public LivraisonEntity get(Long id) {
        return livraisonRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Livraison", id));
    }

    @Transactional
    public LivraisonEntity create(LivraisonRequest request) {
        LivraisonEntity livraison = LivraisonEntity.builder()
            .reference(LivraisonReference.next())
            .build();
        apply(livraison, request);
        return livraisonRepository.save(livraison);
    }

    @Transactional
    public LivraisonEntity update(Long id, LivraisonRequest request) {
        LivraisonEntity livraison = get(id);
        apply(livraison, request);
        return livraisonRepository.save(livraison);
    }

    @Transactional
    public void delete(Long id) {
        LivraisonEntity livraison = get(id);
        tourneeRepository.findAllByLivraisonEntitiesContaining(livraison)
            .forEach(tournee -> tournee.getLivraisonEntities().removeIf(l -> Objects.equals(l.getId(), id)));
        solutionRepository.findAllByLivraisonsIdsContaining(id)
            .forEach(solution -> solution.getLivraisonsIds().remove(id));
        livraisonRepository.delete(livraison);
    }

    @Transactional
    public LivraisonEntity updateStatut(Long id, boolean livree) {
        LivraisonEntity livraison = get(id);
        livraison.marquerLivree(livree, LocalDateTime.now(clock));
        return livraisonRepository.save(livraison);
    }

    private void apply(LivraisonEntity livraison, LivraisonRequest request) {
        livraison.setPoids(request.poids());
        livraison.setTempsMontage(parseMontage(request.tempsMontage()));
        livraison.setDateCommande(request.dateCommande() == null ? LocalDate.now(clock) : request.dateCommande());
        if (livraison.getAdresseEntity() == null || !sameAddress(livraison.getAdresseEntity(), request.adresse())) {
            livraison.setAdresseEntity(newAddress(request.adresse()));
        }
    }

    // Une adresse peut être partagée par plusieurs livraisons : on en crée une nouvelle
    // plutôt que de déplacer les autres clients en modifiant celle-ci.
    private AdresseEntity newAddress(AdresseRequest request) {
        AdresseEntity adresse = AdresseEntity.builder().reference("ADR-" + UUID.randomUUID()).build();
        adresseMapper.update(adresse, request);
        return adresseRepository.save(adresse);
    }

    private static boolean sameAddress(AdresseEntity current, AdresseRequest requested) {
        return current.getCoords() != null
            && current.getCoords().getLatitude() == requested.lat()
            && current.getCoords().getLongitude() == requested.lng()
            && Objects.equals(current.getRue(), requested.rue())
            && Objects.equals(current.getNumero(), requested.numero())
            && Objects.equals(current.getCodePostal(), requested.codePostal())
            && Objects.equals(current.getVille(), requested.ville());
    }

    private static Duration parseMontage(String iso) {
        if (iso == null || iso.isBlank()) {
            return Duration.ZERO;
        }
        try {
            Duration montage = Duration.parse(iso.trim());
            if (montage.isNegative()) {
                throw new InvalidRequestException("Le temps de montage ne peut pas être négatif");
            }
            return montage;
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException("tempsMontage doit être une durée ISO-8601 (ex. PT15M)");
        }
    }

    private static boolean isOnOrAfter(LocalDate date, LocalDate min) {
        return min == null || (date != null && !date.isBefore(min));
    }

    private static boolean isOnOrBefore(LocalDate date, LocalDate max) {
        return max == null || (date != null && !date.isAfter(max));
    }
}
