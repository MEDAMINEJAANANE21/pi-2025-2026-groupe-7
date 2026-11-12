package fr.uga.miage.l3.services;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.responses.JourneeStatusUpdateDTO;
import fr.uga.miage.l3.websocket.JourneeWebSocketEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class JourneeStatusBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(LocalDate date, StatutGeneration statut) {
        String dateStr = date.toString();
        messagingTemplate.convertAndSend(
            JourneeWebSocketEndpoints.TOPIC_PREFIX + dateStr,
            new JourneeStatusUpdateDTO(dateStr, statut.name(), Instant.now().toString())
        );
    }
}
