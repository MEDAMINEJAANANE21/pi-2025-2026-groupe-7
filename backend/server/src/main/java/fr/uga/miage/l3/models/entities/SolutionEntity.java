package fr.uga.miage.l3.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolutionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate date;

  @Embedded
  private Strategie strategie;

  @OneToOne(mappedBy = "solutionEntity")
  private JourneeEntity journeeEntity;

  @OneToMany(mappedBy = "solutionEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<TourneeEntity> tourneeEntities;

  @ElementCollection
  @CollectionTable(name = "solution_livraison_ids", joinColumns = @JoinColumn(name = "solution_id"))
  @Column(name = "livraison_id")
  @Builder.Default
  private Set<Long> livraisonsIds = new HashSet<>();
}
