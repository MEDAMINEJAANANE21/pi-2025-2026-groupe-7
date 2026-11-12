package fr.uga.miage.l3.request;


import java.util.Set;

public record SolutionCreationRequest(
    String date,
    StrategieRequestDTO strategie,
    Set<TourneeRequestDTO> tournees
) {
}
