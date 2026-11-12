package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.models.entities.JourneeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface JourneeRepository extends JpaRepository<JourneeEntity, LocalDate> {

    List<JourneeEntity> findAllByDateBetween(LocalDate debut, LocalDate fin);

    List<JourneeEntity> findAllByStatutGenerationIn(Collection<StatutGeneration> statuts);
}