package fr.uga.miage.l3.request;

import fr.uga.miage.l3.responses.AdresseDTO;

public record LivraisonDeletionRequest (
    int poids,
    AdresseDTO adresse
) {
}
