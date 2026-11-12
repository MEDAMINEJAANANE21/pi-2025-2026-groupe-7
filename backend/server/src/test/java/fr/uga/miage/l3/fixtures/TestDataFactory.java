package fr.uga.miage.l3.fixtures;

import fr.uga.miage.l3.enums.Optimisation;
import fr.uga.miage.l3.enums.Separation;
import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.Statistic;
import fr.uga.miage.l3.models.entities.Strategie;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for building and persisting the full journée fixture hierarchy:
 * journée → solutions → tournées → livraisons → commandes → adresses.
 *
 * <p>Invariant enforced by this factory: every commande assigned to a livraison
 * targets the <em>same</em> adresse as all other commandes in that livraison.
 *
 * <p>Each livraison can be shared across multiple solutions (via the ManyToMany
 * relationship) so that it appears in every solution of a given journée.
 */
@Component
public class TestDataFactory {

    @Autowired private AdresseRepository adresseRepository;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private JourneeRepository journeeRepository;

    private final AtomicInteger counter = new AtomicInteger(0);

    private String next(String prefix) {
        return prefix + "-" + counter.incrementAndGet();
    }

    /**
     * Creates and saves an {@link AdresseEntity} with minimal required fields.
     */
    public AdresseEntity createAdresse() {
        AdresseEntity adresse = new AdresseEntity();
        adresse.setReference(next("ADR"));
        adresse.setRue("1 Rue de Test");
        adresse.setVille("Grenoble");
        adresse.setCodePostal("38000");
        return adresseRepository.save(adresse);
    }

    /**
     * Creates and saves a {@link LivraisonEntity} with {@code nbCommandes} commandes,
     * all assigned to the same adresse.
     *
     * <p>This enforces the invariant: every commande within a livraison targets
     * the same adresse.
     *
     * @param nbCommandes number of commandes to attach to the livraison
     * @return the saved livraison
     */
    public LivraisonEntity createLivraison(int nbCommandes) {
        AdresseEntity adresse = createAdresse();

        LivraisonEntity livraison = LivraisonEntity.builder()
            .reference(next("LIV"))
            .poids(10.0 *  nbCommandes)
            .dateCommande(LocalDate.of(2026, 4, 15))
            .adresseEntity(adresse)
            .build();
        livraison = livraisonRepository.save(livraison);

        return livraison;
    }

    /**
     * Creates and saves a {@link TourneeEntity} linked to the given solution
     * and containing the given livraisons.
     *
     * <p>Default statistic values: carburant=100, distance=5000,
     * tempsTrajet=PT30M, tempsMontage=PT15M.
     *
     * @param solution   the parent solution
     * @param livraisons ordered list of livraisons for this tournée
     * @return the saved tournée
     */
    public TourneeEntity createTournee(SolutionEntity solution, List<LivraisonEntity> livraisons) {
        TourneeEntity tournee = TourneeEntity.builder()
                .solutionEntity(solution)
                .livraisonEntities(new ArrayList<>(livraisons))
                .statistic(Statistic.builder()
                        .carburant(100L)
                        .distance(5000L)
                        .tempsTrajet(Duration.ofMinutes(30))
                        .tempsMontage(Duration.ofMinutes(15))
                        .build())
                .build();
        return tourneeRepository.save(tournee);
    }

    /**
     * Creates and saves a {@link SolutionEntity} for the given date with a default
     * stratégie (separation=NONE, optimisation=GREEDY, nbVehicules=1).
     *
     * @param date the planning date
     * @return the saved solution
     */
    public SolutionEntity createSolution(LocalDate date) {
        SolutionEntity solution = SolutionEntity.builder()
                .date(date)
                .strategie(Strategie.builder()
                        .nbVehicules(1)
                        .separation(Separation.NONE)
                        .optimisation(Optimisation.GREEDY)
                        .build())
                .build();
        return solutionRepository.save(solution);
    }

    /**
     * Creates and saves a {@link JourneeEntity} for the given date.
     *
     * @param date the journée date (primary key)
     * @return the saved journée
     */
    public JourneeEntity createJournee(LocalDate date) {
        return journeeRepository.save(JourneeEntity.builder()
                .date(date)
                .statutGeneration(StatutGeneration.INITIALISATION)
                .build());
    }

    /**
     * Creates a complete test scenario for a single journée:
     * <ul>
     *   <li>One {@link JourneeEntity} for {@code date}.</li>
     *   <li>{@code nbSolutions} {@link SolutionEntity}s for the same date.</li>
     *   <li>Each solution has exactly one {@link TourneeEntity}.</li>
     *   <li>All tournées share the same pool of {@code nbLivraisons} livraisons,
     *       so each livraison is associated with every solution of the journée.</li>
     *   <li>Each livraison has {@code nbCommandesPerLivraison} commandes,
     *       all targeting the same adresse.</li>
     * </ul>
     *
     * @param date                   the journée/solution date
     * @param nbSolutions            number of solutions to create
     * @param nbLivraisons           number of livraisons shared across all solutions
     * @param nbCommandesPerLivraison number of commandes per livraison (same adresse)
     * @return a {@link JourneeScenario} holding all created entities
     */
    public JourneeScenario createJourneeWithSolutions(
            LocalDate date, int nbSolutions, int nbLivraisons, int nbCommandesPerLivraison) {

        JourneeEntity journee = createJournee(date);

        List<LivraisonEntity> sharedLivraisons = new ArrayList<>();
        for (int i = 0; i < nbLivraisons; i++) {
            sharedLivraisons.add(createLivraison(nbCommandesPerLivraison));
        }

        List<SolutionEntity> solutions = new ArrayList<>();
        List<TourneeEntity> tournees = new ArrayList<>();
        for (int i = 0; i < nbSolutions; i++) {
            SolutionEntity solution = createSolution(date);
            TourneeEntity tournee = createTournee(solution, sharedLivraisons);
            solutions.add(solution);
            tournees.add(tournee);
        }

        return new JourneeScenario(journee, solutions, tournees, sharedLivraisons);
    }

    /**
     * Holds all entities created by {@link #createJourneeWithSolutions}.
     */
    public record JourneeScenario(
            JourneeEntity journee,
            List<SolutionEntity> solutions,
            List<TourneeEntity> tournees,
            List<LivraisonEntity> livraisons
    ) {}
}
