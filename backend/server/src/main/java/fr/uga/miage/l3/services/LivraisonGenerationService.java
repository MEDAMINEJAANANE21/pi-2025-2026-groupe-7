package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LivraisonGenerationService {

  public LivraisonEntity generateLivraison() {
    LivraisonEntity livraison = new LivraisonEntity();
    livraison.setReference(UUID.randomUUID().toString());
    return livraison;
  }

  public LivraisonEntity generateLivraison(AdresseEntity adresse) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    LivraisonEntity livraison = generateLivraison();
    livraison.setAdresseEntity(adresse);
    livraison.setPoids(generatePoids(random));
    livraison.setTempsMontage(Duration.ofMinutes(generateTempsMontage(random)));
    livraison.setDateCommande(LocalDate.now().minusDays(random.nextInt(0, 30)));
    return livraison;
  }

  // 90% → 0–5 kg, 9% → 5–10 kg, 1% → 10–50 kg
  private double generatePoids(ThreadLocalRandom random) {
    int bucket = random.nextInt(100);
    double raw;
    if (bucket < 90) {
      raw = random.nextDouble(0, 5);
    } else if (bucket < 99) {
      raw = random.nextDouble(5, 10);
    } else {
      raw = random.nextDouble(10, 50);
    }
    return Math.round(raw * 10.0) / 10.0;
  }

  // 85% → 0 min, 10% → 5 min, 5% → 15 min
  private long generateTempsMontage(ThreadLocalRandom random) {
    int bucket = random.nextInt(100);
    if (bucket < 85) return 0;
    if (bucket < 95) return 5;
    return 15;
  }
}
