package fr.uga.miage.l3.request;


public record TourneeCreationRequest (
    int nbVehicules,
    int maxTempsParVehicule
) {
}
