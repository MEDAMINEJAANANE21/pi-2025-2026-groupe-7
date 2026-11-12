package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.SolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SolutionRepository extends JpaRepository<SolutionEntity, Long> {

    List<SolutionEntity> findAllByDateOrderByIdAsc(LocalDate date);

    List<SolutionEntity> findAllByDateBetween(LocalDate debut, LocalDate fin);

    List<SolutionEntity> findAllByLivraisonsIdsContaining(Long livraisonId);
}