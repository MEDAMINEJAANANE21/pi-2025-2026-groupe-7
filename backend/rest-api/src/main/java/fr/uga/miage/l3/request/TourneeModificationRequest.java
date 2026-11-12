package fr.uga.miage.l3.request;


public record TourneeModificationRequest (
    Long idTourneeSource,
    Long indiceLivraisonSource,
    Long idTourneeDestination,
    Long indiceLivraisonDestination
) {
}