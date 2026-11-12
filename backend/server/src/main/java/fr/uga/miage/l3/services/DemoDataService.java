package fr.uga.miage.l3.services;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.ChauffeurRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Charge un jeu d'essai cohérent : un dépôt, des livraisons autour de Grenoble et une équipe.
 *
 * <p>Les données vivent dans {@code resources/demo/*.csv}. Les dates de commande sont relatives
 * à aujourd'hui, pour que le jeu reste réaliste quel que soit le jour de la démonstration.
 */
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final String FOLDER = "demo/";

    private final JourneeRepository journeeRepository;
    private final SolutionRepository solutionRepository;
    private final LivraisonRepository livraisonRepository;
    private final ParkingRepository parkingRepository;
    private final AdresseRepository adresseRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final Clock clock;

    @Transactional
    public Summary load() {
        clear();
        AdresseEntity depot = adresseRepository.save(read("depot.csv", this::adresse).get(0));
        parkingRepository.save(ParkingEntity.builder().adresse(depot).build());
        List<LivraisonEntity> livraisons = livraisonRepository.saveAll(read("livraisons.csv", this::livraison));
        List<ChauffeurEntity> chauffeurs = chauffeurRepository.saveAll(read("chauffeurs.csv", DemoDataService::chauffeur));
        List<VehiculeEntity> vehicules = vehiculeRepository.saveAll(read("vehicules.csv", DemoDataService::vehicule));
        return new Summary(livraisons.size(), chauffeurs.size(), vehicules.size());
    }

    private void clear() {
        journeeRepository.deleteAllInBatch();
        // Une à une : la suppression d'une solution emporte ses tournées et ses identifiants de livraison.
        solutionRepository.deleteAll();
        // Hibernate insère avant de supprimer au flush : on écrit les suppressions tout de suite,
        // sinon les suppressions en masse et les nouvelles immatriculations entrent en conflit.
        solutionRepository.flush();
        parkingRepository.deleteAllInBatch();
        livraisonRepository.deleteAllInBatch();
        adresseRepository.deleteAllInBatch();
        // Une à une aussi : la cascade supprime les photos.
        chauffeurRepository.deleteAll();
        vehiculeRepository.deleteAll();
        vehiculeRepository.flush();
    }

    private AdresseEntity adresse(CSVRecord row) {
        return AdresseEntity.builder()
            .reference("ADR-" + UUID.randomUUID())
            .numero(row.get("numero"))
            .rue(row.get("rue"))
            .codePostal(row.get("codePostal"))
            .ville(row.get("ville"))
            .coords(new Coords(Double.parseDouble(row.get("lat")), Double.parseDouble(row.get("lng"))))
            .build();
    }

    private LivraisonEntity livraison(CSVRecord row) {
        return LivraisonEntity.builder()
            .reference(LivraisonReference.next())
            .adresseEntity(adresseRepository.save(adresse(row)))
            .poids(Double.parseDouble(row.get("poidsKg")))
            .tempsMontage(Duration.ofMinutes(Long.parseLong(row.get("montageMinutes"))))
            .dateCommande(LocalDate.now(clock).minusDays(Long.parseLong(row.get("joursDepuisCommande"))))
            .build();
    }

    private static ChauffeurEntity chauffeur(CSVRecord row) {
        return ChauffeurEntity.builder()
            .prenom(row.get("prenom"))
            .nom(row.get("nom"))
            .tauxHoraire(optionalDouble(row.get("tauxHoraire")))
            .actif(Boolean.parseBoolean(row.get("actif")))
            .build();
    }

    private static VehiculeEntity vehicule(CSVRecord row) {
        return VehiculeEntity.builder()
            .immatriculation(row.get("immatriculation"))
            .modele(row.get("modele"))
            .capaciteKg(optionalDouble(row.get("capaciteKg")))
            .consommationLitresAux100Km(optionalDouble(row.get("consommationLitresAux100Km")))
            .actif(Boolean.parseBoolean(row.get("actif")))
            .build();
    }

    private static Double optionalDouble(String value) {
        return value == null || value.isBlank() ? null : Double.valueOf(value);
    }

    private static <T> List<T> read(String file, Function<CSVRecord, T> converter) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get();
        try (Reader reader = new InputStreamReader(new ClassPathResource(FOLDER + file).getInputStream(), StandardCharsets.UTF_8)) {
            return format.parse(reader).stream().map(converter).toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Jeu d'essai illisible : " + file, ex);
        }
    }

    public record Summary(int livraisons, int chauffeurs, int vehicules) {
    }
}
