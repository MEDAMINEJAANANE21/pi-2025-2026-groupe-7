package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;
import java.util.Objects;

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

    @ManyToOne
    private ChauffeurEntity chauffeur;

    @ManyToOne
    private VehiculeEntity vehicule;

    public double poidsTotal() {
        if (livraisonEntities == null) {
            return 0;
        }
        return livraisonEntities.stream()
            .map(LivraisonEntity::getPoids)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
    }

    public int nbLivraisons() {
        return livraisonEntities == null ? 0 : livraisonEntities.size();
    }
}
