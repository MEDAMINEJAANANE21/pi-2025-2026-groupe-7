package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdresseRepository extends JpaRepository<AdresseEntity,Long> {
}
