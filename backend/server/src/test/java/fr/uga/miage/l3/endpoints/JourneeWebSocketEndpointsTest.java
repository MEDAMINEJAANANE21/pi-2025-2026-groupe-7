package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.enums.StatutGeneration;
import fr.uga.miage.l3.responses.JourneeStatusUpdateDTO;
import fr.uga.miage.l3.services.JourneeStatusBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JourneeWebSocketEndpointsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JourneeStatusBroadcastService broadcastService;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Nested
    class SubscribeToJourneeTopicTest {

        @Test
        void receivesStatusUpdateOnBroadcast() throws Exception {
            BlockingQueue<JourneeStatusUpdateDTO> received = new LinkedBlockingQueue<>();

            StompSession session = stompClient.connectAsync(
                "ws://localhost:{port}/ws",
                new StompSessionHandlerAdapter() {},
                port
            ).get(5, TimeUnit.SECONDS);

            session.subscribe("/topic/journee/2026-05-09", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return JourneeStatusUpdateDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.add((JourneeStatusUpdateDTO) payload);
                }
            });

            Thread.sleep(200);

            broadcastService.broadcast(LocalDate.of(2026, 5, 9), StatutGeneration.GENERATION_ROUTE);

            JourneeStatusUpdateDTO msg = received.poll(5, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.date()).isEqualTo("2026-05-09");
            assertThat(msg.statutGeneration()).isEqualTo("GENERATION_ROUTE");
            assertThat(msg.timestamp()).isNotNull();

            session.disconnect();
        }

        @Test
        void doesNotReceiveMessageForDifferentDate() throws Exception {
            BlockingQueue<JourneeStatusUpdateDTO> received = new LinkedBlockingQueue<>();

            StompSession session = stompClient.connectAsync(
                "ws://localhost:{port}/ws",
                new StompSessionHandlerAdapter() {},
                port
            ).get(5, TimeUnit.SECONDS);

            session.subscribe("/topic/journee/2026-06-01", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return JourneeStatusUpdateDTO.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.add((JourneeStatusUpdateDTO) payload);
                }
            });

            Thread.sleep(200);

            broadcastService.broadcast(LocalDate.of(2026, 5, 9), StatutGeneration.GENERATION_ROUTE);

            JourneeStatusUpdateDTO msg = received.poll(1, TimeUnit.SECONDS);
            assertThat(msg).isNull();

            session.disconnect();
        }
    }
}
