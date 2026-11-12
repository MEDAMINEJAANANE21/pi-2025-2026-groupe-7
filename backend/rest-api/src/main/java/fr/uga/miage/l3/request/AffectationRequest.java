package fr.uga.miage.l3.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Affectation d'une tournée ; un identifiant nul retire l'affectation")
public record AffectationRequest(Long chauffeurId, Long vehiculeId) {
}
