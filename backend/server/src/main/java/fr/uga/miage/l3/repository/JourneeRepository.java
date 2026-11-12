package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.JourneeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface JourneeRepository extends JpaRepository<JourneeEntity, LocalDate> {

}
