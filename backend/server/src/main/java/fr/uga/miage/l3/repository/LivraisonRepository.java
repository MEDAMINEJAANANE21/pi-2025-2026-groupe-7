package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LivraisonRepository extends JpaRepository<LivraisonEntity,Long> {
    List<LivraisonEntity> findAllByLivree(boolean livree);

    @Query("SELECT l FROM LivraisonEntity l WHERE " +
           "(:livree IS NULL OR l.livree = :livree) AND " +
           "(:dateDebut IS NULL OR l.dateCommande >= :dateDebut) AND " +
           "(:dateFin IS NULL OR l.dateCommande <= :dateFin)")
    List<LivraisonEntity> findAllWithFilters(
        @Param("livree") Boolean livree,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin
    );
}
