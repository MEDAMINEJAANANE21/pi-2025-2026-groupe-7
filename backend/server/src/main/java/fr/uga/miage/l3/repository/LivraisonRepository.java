package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivraisonRepository extends JpaRepository<LivraisonEntity, Long> {

    List<LivraisonEntity> findAllByLivree(boolean livree);
}