package fr.uga.miage.l3.models.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "adresse_id", nullable = false, unique = true)
  private AdresseEntity adresse;
}

