package fr.uga.miage.l3.mappers;

import fr.uga.miage.l3.models.entities.PhotoEntity;

/**
 * Adresse publique d'une photo. Le paramètre {@code v} change à chaque nouvelle photo :
 * le navigateur peut donc la garder en cache sans jamais afficher une version périmée.
 */
public final class PhotoUrls {

    private PhotoUrls() {
    }

    public static String of(String resource, Long ownerId, PhotoEntity photo) {
        // getId() sur un proxy Hibernate ne charge pas les octets de l'image.
        return photo == null ? null : "/api/" + resource + "/" + ownerId + "/photo?v=" + photo.getId();
    }
}
