package fr.uga.miage.l3.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Photo d'un chauffeur ou d'un véhicule.
 *
 * <p>Table à part : les listes de l'équipe ne chargent pas les octets, seulement l'identifiant.
 * Une nouvelle photo crée une nouvelle ligne, ce qui sert aussi de version pour le cache du navigateur.
 */
@Entity
@Table(name = "photo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoEntity {

    public static final int MAX_BYTES = 3 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contentType;

    // VARBINARY donne un bytea sous PostgreSQL : pas de « large object » à nettoyer à part.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = MAX_BYTES)
    private byte[] data;
}
