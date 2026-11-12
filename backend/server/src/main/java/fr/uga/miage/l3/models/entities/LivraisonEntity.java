package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.LocalDate;
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


    @ManyToOne
    private AdresseEntity adresseEntity;
}
