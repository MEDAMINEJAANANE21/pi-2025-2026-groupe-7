package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChauffeurRepository extends JpaRepository<ChauffeurEntity, Long> {

    List<ChauffeurEntity> findAllByOrderByNomAscPrenomAsc();
}