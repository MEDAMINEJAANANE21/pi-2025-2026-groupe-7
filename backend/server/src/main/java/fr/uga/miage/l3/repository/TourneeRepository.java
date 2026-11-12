package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourneeRepository extends JpaRepository<TourneeEntity, Long> {

    List<TourneeEntity> findAllByLivraisonEntitiesContaining(LivraisonEntity livraison);

    List<TourneeEntity> findAllByChauffeurId(Long chauffeurId);

    List<TourneeEntity> findAllByVehiculeId(Long vehiculeId);
}