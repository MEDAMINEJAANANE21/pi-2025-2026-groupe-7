package fr.uga.miage.l3.websocket;

/**
 * Contrat WebSocket STOMP pour les mises à jour de statut de journée en temps réel.
 *
 * <p>Connexion STOMP : {@code /ws}
 *
 * <p>Le serveur publie des messages {@code JourneeStatusUpdateDTO} sur le topic
 * {@code /topic/journee/{date}} à chaque changement de {@code statutGeneration}.
 *
 * <p>Le client s'abonne à {@code TOPIC_PREFIX + date} pour recevoir les mises à jour
 * d'une journée spécifique.
 */
public interface JourneeWebSocketEndpoints {

    /** Endpoint de connexion STOMP (SockJS handshake). */
    String STOMP_ENDPOINT = "/ws";

    /** Préfixe des topics de statut journée. Concaténer la date pour obtenir le topic complet. */
    String TOPIC_PREFIX = "/topic/journee/";
}
