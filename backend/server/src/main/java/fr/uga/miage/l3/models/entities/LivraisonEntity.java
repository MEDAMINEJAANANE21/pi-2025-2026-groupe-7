package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LivraisonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String reference;

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean livree = false;

    /** Horodatage de la remise au client, renseigné quand {@link #livree} passe à vrai. */
    private LocalDateTime dateLivraison;

    @Column(nullable = false)
    @ColumnDefault("0.0")
    @Builder.Default
    private Double poids = 0.0;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Duration tempsMontage = Duration.ZERO;

    private LocalDate dateCommande;

    @ManyToMany(mappedBy = "livraisonEntities")
    private Set<TourneeEntity> tourneeEntities;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private AdresseEntity adresseEntity;

    public void marquerLivree(boolean estLivree, LocalDateTime quand) {
        this.livree = estLivree;
        this.dateLivraison = estLivree ? quand : null;
    }
}
