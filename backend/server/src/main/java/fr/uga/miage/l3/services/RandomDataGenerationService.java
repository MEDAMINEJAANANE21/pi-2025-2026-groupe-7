package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RandomDataGenerationService {

  private final AdresseRepository adresseRepository;
  private final LivraisonRepository livraisonRepository;
  private final LivraisonGenerationService livraisonGenerationService;
  private final ParkingRepository parkingRepository;

  @Transactional
  public GenerationStats generateLivraisons(int count) {
    List<AdresseEntity> adresses = adresseRepository.findAll();
    if (adresses.isEmpty()) {
      throw new IllegalStateException("No adresse available. Import a dataset first.");
    }

    Set<Long> parkingAddressIds = parkingRepository.findAll().stream()
      .map(ParkingEntity::getAdresse)
      .filter(Objects::nonNull)
      .map(AdresseEntity::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    List<AdresseEntity> destinations = adresses.stream()
      .filter(adresse -> !parkingAddressIds.contains(adresse.getId()))
      .collect(Collectors.toList());

    if (destinations.isEmpty()) {
      throw new IllegalStateException("No eligible address for livraisons (parking excluded).");
    }

    // Prefer addresses without livraisons; only allow duplicates when all destinations already have one
    List<AdresseEntity> withoutLivraison = destinations.stream()
      .filter(a -> a.getLivraisonEntities() == null || a.getLivraisonEntities().isEmpty())
      .collect(Collectors.toList());

    List<LivraisonEntity> livraisons = new ArrayList<>(count);
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int remaining = count;
    if (!withoutLivraison.isEmpty()) {
      Collections.shuffle(withoutLivraison, random);
      int take = Math.min(remaining, withoutLivraison.size());
      for (int i = 0; i < take; i++) {
        livraisons.add(livraisonGenerationService.generateLivraison(withoutLivraison.get(i)));
      }
      remaining -= take;
    }

    for (int i = 0; i < remaining; i++) {
      AdresseEntity adresse = destinations.get(random.nextInt(destinations.size()));
      livraisons.add(livraisonGenerationService.generateLivraison(adresse));
    }

    List<LivraisonEntity> created = livraisonRepository.saveAll(livraisons);
    int addressesAvailable = adresses.size();
    int livraisonsAvailable = (int) livraisonRepository.count();
    return new GenerationStats(created.size(), addressesAvailable, livraisonsAvailable);
  }

  public record GenerationStats(int generated, int addressesAvailable, int livraisonsAvailable) {
  }
}
