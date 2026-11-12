package fr.uga.miage.l3.request;

import jakarta.validation.constraints.NotNull;

public record LivraisonStatutRequest(@NotNull Boolean livree) {
}
