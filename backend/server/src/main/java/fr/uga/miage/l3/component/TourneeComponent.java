package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.ConflictException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.repository.ChauffeurRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.repository.VehiculeRepository;
import fr.uga.miage.l3.request.AffectationRequest;
import fr.uga.miage.l3.services.core.DistanceMatrixService;
import fr.uga.miage.l3.services.optimisation.OptimisationProblem;
import fr.uga.miage.l3.services.statistics.TourneeStatisticsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TourneeComponent {

    private final TourneeRepository tourneeRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ParkingRepository parkingRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final AdresseMapper adresseMapper;
    private final TourneeStatisticsCalculator statisticsCalculator;
    private final Clock clock;

    public TourneeEntity get(Long id) {
        return tourneeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tournée", id));
    }

    public List<TourneeEntity> saveForSolution(SolutionEntity solution, List<List<Integer>> routes, OptimisationProblem problem) {
        List<TourneeEntity> tournees = routes.stream()
            .filter(route -> !route.isEmpty())
            .map(route -> TourneeEntity.builder()
                .solutionEntity(solution)
                .livraisonEntities(new ArrayList<>(route.stream().map(problem::livraisonAt).toList()))
                .statistic(statisticsCalculator.fromProblem(route, problem))
                .build())
            .toList();
        return tourneeRepository.saveAll(tournees);
    }

    @Transactional
    public TourneeEntity affecter(Long id, AffectationRequest request) {
        TourneeEntity tournee = get(id);
        ChauffeurEntity chauffeur = request.chauffeurId() == null ? null : chauffeurRepository.findById(request.chauffeurId())
            .orElseThrow(() -> ResourceNotFoundException.of("Chauffeur", request.chauffeurId()));
        VehiculeEntity vehicule = request.vehiculeId() == null ? null : vehiculeRepository.findById(request.vehiculeId())
            .orElseThrow(() -> ResourceNotFoundException.of("Véhicule", request.vehiculeId()));

        ensureAvailableInSolution(tournee, chauffeur, vehicule);
        ensureCapacity(tournee, vehicule);

        tournee.setChauffeur(chauffeur);
        tournee.setVehicule(vehicule);
        tournee.setStatistic(statisticsCalculator.withVehicule(tournee.getStatistic(), vehicule));
        return tourneeRepository.save(tournee);
    }

    @Transactional
    public TourneeEntity terminer(Long id) {
        TourneeEntity tournee = get(id);
        LocalDateTime now = LocalDateTime.now(clock);
        tournee.getLivraisonEntities().stream()
            .filter(livraison -> !livraison.isLivree())
            .forEach(livraison -> livraison.marquerLivree(true, now));
        return tourneeRepository.save(tournee);
    }

    /**
     * Recalcule distance et durée à partir des trajets réels dépôt → livraisons → dépôt.
     */
    public void recalculateStatistics(TourneeEntity tournee) {
        List<LivraisonEntity> livraisons = tournee.getLivraisonEntities();
        AdresseEntity parking = parkingRepository.findTopByOrderByIdDesc().map(p -> p.getAdresse()).orElse(null);
        if (livraisons == null || livraisons.isEmpty() || parking == null) {
            return;
        }
        List<AdresseEntity> stops = new ArrayList<>();
        stops.add(parking);
        livraisons.stream().map(LivraisonEntity::getAdresseEntity).filter(a -> a.getCoords() != null).forEach(stops::add);
        if (stops.size() < 2) {
            return;
        }

        List<Adresse> locations = adresseMapper.toCore(stops);
        MatrixResponse matrix = distanceMatrixService.getMatrices(locations);
        if (matrix == null || matrix.distances == null || matrix.durations == null) {
            return;
        }
        long distance = 0;
        long seconds = 0;
        for (int i = 0; i < locations.size(); i++) {
            int next = (i + 1) % locations.size();
            distance += valueAt(matrix.distances, i, next);
            seconds += valueAt(matrix.durations, i, next);
        }
        tournee.setStatistic(statisticsCalculator.fromTotals(distance, Duration.ofSeconds(seconds), livraisons, tournee.getVehicule()));
        tourneeRepository.save(tournee);
    }

    @Transactional
    public void detachChauffeur(Long chauffeurId) {
        tourneeRepository.findAllByChauffeurId(chauffeurId).forEach(tournee -> tournee.setChauffeur(null));
    }

    @Transactional
    public void detachVehicule(Long vehiculeId) {
        tourneeRepository.findAllByVehiculeId(vehiculeId).forEach(tournee -> {
            tournee.setVehicule(null);
            tournee.setStatistic(statisticsCalculator.withVehicule(tournee.getStatistic(), null));
        });
    }

    private static void ensureAvailableInSolution(TourneeEntity tournee, ChauffeurEntity chauffeur, VehiculeEntity vehicule) {
        List<TourneeEntity> others = tournee.getSolutionEntity().getTourneeEntities().stream()
            .filter(other -> !Objects.equals(other.getId(), tournee.getId()))
            .toList();
        if (chauffeur != null && others.stream().anyMatch(other -> sameId(other.getChauffeur(), chauffeur))) {
            throw new ConflictException(chauffeur.getPrenom() + " " + chauffeur.getNom() + " conduit déjà une autre tournée de cette solution");
        }
        if (vehicule != null && others.stream().anyMatch(other -> other.getVehicule() != null
            && Objects.equals(other.getVehicule().getId(), vehicule.getId()))) {
            throw new ConflictException("Le véhicule " + vehicule.getImmatriculation() + " est déjà utilisé dans cette solution");
        }
    }

    private static void ensureCapacity(TourneeEntity tournee, VehiculeEntity vehicule) {
        if (vehicule != null && vehicule.getCapaciteKg() != null && tournee.poidsTotal() > vehicule.getCapaciteKg()) {
            throw new ConflictException(String.format(Locale.FRANCE,
                "La tournée pèse %.1f kg, le véhicule %s n'en porte que %.1f",
                tournee.poidsTotal(), vehicule.getImmatriculation(), vehicule.getCapaciteKg()));
        }
    }

    private static boolean sameId(ChauffeurEntity a, ChauffeurEntity b) {
        return a != null && Objects.equals(a.getId(), b.getId());
    }

    private static long valueAt(List<List<Integer>> matrix, int i, int j) {
        if (i >= matrix.size() || matrix.get(i) == null || j >= matrix.get(i).size() || matrix.get(i).get(j) == null) {
            return 0;
        }
        return matrix.get(i).get(j);
    }
}
