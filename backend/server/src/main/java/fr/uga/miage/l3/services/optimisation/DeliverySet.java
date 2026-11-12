package fr.uga.miage.l3.services.optimisation;

import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;

import java.util.List;

/**
 * Livraisons retenues pour une optimisation et dépôt de départ, validés avant le calcul.
 */
public record DeliverySet(List<LivraisonEntity> livraisons, AdresseEntity parking) {
}
