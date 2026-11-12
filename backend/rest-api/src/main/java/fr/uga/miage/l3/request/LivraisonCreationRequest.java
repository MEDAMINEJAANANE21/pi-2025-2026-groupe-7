package fr.uga.miage.l3.request;

import fr.uga.miage.l3.responses.AdresseDTO;

public record LivraisonCreationRequest (
    int poids,
    AdresseDTO adresse
) {
}