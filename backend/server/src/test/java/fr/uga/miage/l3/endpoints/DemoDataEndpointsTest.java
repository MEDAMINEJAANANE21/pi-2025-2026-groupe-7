package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.fixtures.DatabaseCleaner;
import fr.uga.miage.l3.responses.ChauffeurDTO;
import fr.uga.miage.l3.responses.DemoDataResponseDTO;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import fr.uga.miage.l3.responses.VehiculeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class DemoDataEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private DatabaseCleaner cleaner;

    @AfterEach
    void cleanup() {
        cleaner.clean();
    }

    @Test
    void chargeUnJeuCompletEtCoherent() {
        webClient.post().uri("/api/data/demo").exchange()
            .expectStatus().isCreated()
            .expectBody(DemoDataResponseDTO.class)
            .isEqualTo(new DemoDataResponseDTO(32, 6, 5));

        webClient.get().uri("/api/livraisons").exchange()
            .expectBodyList(LivraisonResponsesDTO.class)
            .value(livraisons -> {
                assertThat(livraisons).hasSize(32);
                assertThat(livraisons).allSatisfy(l -> {
                    assertThat(l.adresse().coordonnees()).isNotNull();
                    assertThat(l.adresse().codePostal()).matches("38\\d{3}").isNotEqualTo("38185");
                    assertThat(l.poids()).isPositive();
                    assertThat(l.dateCommande()).isBeforeOrEqualTo(LocalDate.now());
                });
            });
        webClient.get().uri("/api/parkings").exchange()
            .expectStatus().isOk()
            .expectBody(ParkingResponsesDTO.class)
            .value(parking -> assertThat(parking.adresse().ville()).isEqualTo("Seyssins"));
        webClient.get().uri("/api/chauffeurs").exchange()
            .expectBodyList(ChauffeurDTO.class)
            .value(chauffeurs -> assertThat(chauffeurs).filteredOn(ChauffeurDTO::actif).hasSize(5));
        webClient.get().uri("/api/vehicules").exchange()
            .expectBodyList(VehiculeDTO.class)
            .value(vehicules -> assertThat(vehicules).allMatch(v -> v.capaciteKg() != null));
    }

    @Test
    void rechargerRemplaceLesDonneesSansDoublon() {
        webClient.post().uri("/api/data/demo").exchange().expectStatus().isCreated();
        webClient.post().uri("/api/data/demo").exchange().expectStatus().isCreated();

        webClient.get().uri("/api/livraisons").exchange().expectBodyList(LivraisonResponsesDTO.class).hasSize(32);
        webClient.get().uri("/api/vehicules").exchange().expectBodyList(VehiculeDTO.class).hasSize(5);
    }
}
