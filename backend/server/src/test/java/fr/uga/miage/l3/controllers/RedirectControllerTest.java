package fr.uga.miage.l3.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class RedirectControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void rootAndIndexHtmlReturnSameContent() {
        byte[] indexHtmlBody = webClient.get()
                .uri("/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        byte[] rootBody = webClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        assertThat(rootBody).isEqualTo(indexHtmlBody);
    }

    @Test
    void lesRoutesAngularRenvoientAussiIndexHtml() {
        for (String route : new String[]{"/journees/2026-11-12", "/solutions/4/tournees/7/feuille-de-route", "/livraisons", "/equipe"}) {
            webClient.get()
                    .uri(route)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .value(body -> assertThat(body).contains("CyberCommandes"));
        }
    }
}

