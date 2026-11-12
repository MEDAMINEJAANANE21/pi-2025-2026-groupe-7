package fr.uga.miage.l3.models.entities;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourneeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private Statistic statistic;

  @ManyToMany
  @OnDelete(action = OnDeleteAction.CASCADE)
  @OrderColumn(name = "livraison_order")
  private List<LivraisonEntity> livraisonEntities;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private SolutionEntity solutionEntity;

}
