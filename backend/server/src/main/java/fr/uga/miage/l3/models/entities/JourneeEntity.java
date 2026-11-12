package fr.uga.miage.l3.models.entities;

import fr.uga.miage.l3.enums.StatutGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneeEntity {
  @Id
  private LocalDate date;

  @Enumerated(EnumType.ORDINAL)
  private StatutGeneration statutGeneration ;

  @OneToOne
  private SolutionEntity solutionEntity;
}
