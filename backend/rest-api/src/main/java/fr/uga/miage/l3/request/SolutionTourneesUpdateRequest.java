package fr.uga.miage.l3.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SolutionTourneesUpdateRequest(
    @NotNull @Valid List<TourneeUpdateDTO> tournees
) {
    public record TourneeUpdateDTO(
        @NotNull Long tourneeId,
        @NotNull List<Long> livraisonIds
    ) {}
}
