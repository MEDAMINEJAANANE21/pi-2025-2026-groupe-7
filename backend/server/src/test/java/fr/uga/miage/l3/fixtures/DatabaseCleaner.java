package fr.uga.miage.l3.fixtures;

import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.ChauffeurRepository;
import fr.uga.miage.l3.repository.JourneeRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.repository.SolutionRepository;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Vide la base entre deux tests, enfants avant parents pour respecter les clés étrangères.
 */
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

    private final TourneeRepository tourneeRepository;
    private final JourneeRepository journeeRepository;
    private final SolutionRepository solutionRepository;
    private final LivraisonRepository livraisonRepository;
    private final ParkingRepository parkingRepository;
    private final AdresseRepository adresseRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;

    public void clean() {
        // Suppressions en masse : elles ne chargent pas les entités, donc pas de références « orphelines »
        // entre journée, solution et tournées au moment du flush.
        journeeRepository.deleteAllInBatch();
        tourneeRepository.deleteAllInBatch();
        // Une à une : la table des identifiants de livraison d'une solution n'a pas de suppression en cascade.
        solutionRepository.deleteAll();
        livraisonRepository.deleteAllInBatch();
        parkingRepository.deleteAllInBatch();
        adresseRepository.deleteAllInBatch();
        chauffeurRepository.deleteAllInBatch();
        vehiculeRepository.deleteAllInBatch();
    }
}
