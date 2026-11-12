package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.models.entities.PhotoEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

/**
 * Réponse HTTP d'une photo. Le cache est long car l'adresse change à chaque nouvelle photo.
 */
final class PhotoResponses {

    private static final Duration CACHE = Duration.ofDays(30);

    private PhotoResponses() {
    }

    static ResponseEntity<byte[]> of(PhotoEntity photo) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getContentType()))
            .cacheControl(CacheControl.maxAge(CACHE).cachePrivate())
            .body(photo.getData());
    }
}