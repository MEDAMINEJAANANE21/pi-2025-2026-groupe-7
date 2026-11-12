package fr.uga.miage.l3.repository;

import fr.uga.miage.l3.models.entities.ParkingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingRepository extends JpaRepository<ParkingEntity, Long> {

  Optional<ParkingEntity> findTopByOrderByIdDesc();
}

