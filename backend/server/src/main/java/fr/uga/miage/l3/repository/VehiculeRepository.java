package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.VehiculeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculeRepository extends JpaRepository<VehiculeEntity, Long> {

    List<VehiculeEntity> findAllByOrderByImmatriculationAsc();

    boolean existsByImmatriculationIgnoreCase(String immatriculation);

    boolean existsByImmatriculationIgnoreCaseAndIdNot(String immatriculation, Long id);
}