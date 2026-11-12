package fr.uga.miage.l3.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true,nullable = false)
  private String reference;

  @Embedded
  private Coords coords;

  private String rue;

  private String numero;

  private String codePostal;

  private String ville;

  @OneToMany(mappedBy = "adresseEntity")
  private Set<LivraisonEntity> livraisonEntities;
}
