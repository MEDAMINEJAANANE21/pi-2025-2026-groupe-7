package fr.uga.miage.l3.component;

import fr.uga.miage.l3.exceptions.ConflictException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.ChauffeurMapper;
import fr.uga.miage.l3.mappers.VehiculeMapper;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.repository.ChauffeurRepository;
import fr.uga.miage.l3.repository.VehiculeRepository;
import fr.uga.miage.l3.request.ChauffeurRequest;
import fr.uga.miage.l3.request.VehiculeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de l'équipe : chauffeurs et véhicules affectables aux tournées.
 */
@Component
@RequiredArgsConstructor
public class EquipeComponent {

    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ChauffeurMapper chauffeurMapper;
    private final VehiculeMapper vehiculeMapper;
    private final TourneeComponent tourneeComponent;

    public List<ChauffeurEntity> chauffeurs() {
        return chauffeurRepository.findAllByOrderByNomAscPrenomAsc();
    }

    @Transactional
    public ChauffeurEntity saveChauffeur(Long id, ChauffeurRequest request) {
        ChauffeurEntity chauffeur = id == null ? new ChauffeurEntity() : chauffeur(id);
        chauffeurMapper.update(chauffeur, request);
        return chauffeurRepository.save(chauffeur);
    }

    @Transactional
    public void deleteChauffeur(Long id) {
        ChauffeurEntity chauffeur = chauffeur(id);
        tourneeComponent.detachChauffeur(id);
        chauffeurRepository.delete(chauffeur);
    }

    public List<VehiculeEntity> vehicules() {
        return vehiculeRepository.findAllByOrderByImmatriculationAsc();
    }

    @Transactional
    public VehiculeEntity saveVehicule(Long id, VehiculeRequest request) {
        String immatriculation = request.immatriculation().trim();
        boolean taken = id == null
            ? vehiculeRepository.existsByImmatriculationIgnoreCase(immatriculation)
            : vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(immatriculation, id);
        if (taken) {
            throw new ConflictException("L'immatriculation " + immatriculation.toUpperCase() + " existe déjà");
        }
        VehiculeEntity vehicule = id == null ? new VehiculeEntity() : vehicule(id);
        vehiculeMapper.update(vehicule, request);
        return vehiculeRepository.save(vehicule);
    }

    @Transactional
    public void deleteVehicule(Long id) {
        VehiculeEntity vehicule = vehicule(id);
        tourneeComponent.detachVehicule(id);
        vehiculeRepository.delete(vehicule);
    }

    private ChauffeurEntity chauffeur(Long id) {
        return chauffeurRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Chauffeur", id));
    }

    private VehiculeEntity vehicule(Long id) {
        return vehiculeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Véhicule", id));
    }
}
