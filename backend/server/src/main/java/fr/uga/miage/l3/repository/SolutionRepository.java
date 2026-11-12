package fr.uga.miage.l3.repository;


import fr.uga.miage.l3.models.entities.SolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Set;

@Repository
public interface SolutionRepository extends JpaRepository<SolutionEntity, Long> {
    Set<SolutionEntity> getAllByDate(LocalDate date);

    SolutionEntity getById(Long id);
}
