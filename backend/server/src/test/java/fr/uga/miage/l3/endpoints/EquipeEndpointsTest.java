package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.DatabaseCleaner;
import fr.uga.miage.l3.request.ChauffeurRequest;
import fr.uga.miage.l3.request.VehiculeRequest;
import fr.uga.miage.l3.responses.ChauffeurDTO;
import fr.uga.miage.l3.responses.VehiculeDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
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
class EquipeEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private DatabaseCleaner cleaner;

    @AfterEach
    void cleanup() {
        cleaner.clean();
    }

    @Nested
    class Chauffeurs {

        @Test
        void creeListeModifieEtSupprime() {
            ChauffeurDTO lea = create(new ChauffeurRequest("Léa", "Martin", "0601020304", 21.5, null));
            create(new ChauffeurRequest("Hugo", "Bernard", null, null, true));

            assertThat(lea.actif()).isTrue();
            webClient.get().uri("/api/chauffeurs").exchange()
                .expectStatus().isOk()
                .expectBodyList(ChauffeurDTO.class)
                .value(list -> assertThat(list).extracting(ChauffeurDTO::nom).containsExactly("Bernard", "Martin"));

            webClient.put().uri("/api/chauffeurs/" + lea.id())
                .bodyValue(new ChauffeurRequest("Léa", "Martin", null, 25.0, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChauffeurDTO.class)
                .value(updated -> {
                    assertThat(updated.tauxHoraire()).isEqualTo(25.0);
                    assertThat(updated.actif()).isFalse();
                });

            webClient.delete().uri("/api/chauffeurs/" + lea.id()).exchange().expectStatus().isNoContent();
            webClient.delete().uri("/api/chauffeurs/" + lea.id()).exchange().expectStatus().isNotFound();
        }

        @Test
        void refuseUnChauffeurSansNom() {
            webClient.post().uri("/api/chauffeurs").bodyValue(new ChauffeurRequest("Léa", " ", null, null, null))
                .exchange().expectStatus().isBadRequest();
        }

        private ChauffeurDTO create(ChauffeurRequest request) {
            return webClient.post().uri("/api/chauffeurs").bodyValue(request)
                .exchange().expectStatus().isCreated()
                .expectBody(ChauffeurDTO.class).returnResult().getResponseBody();
        }
    }

    @Nested
    class Vehicules {

        @Test
        void normaliseLImmatriculation() {
            webClient.post().uri("/api/vehicules").bodyValue(new VehiculeRequest(" ab-123-cd ", "Kangoo", 600.0, 6.5, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(VehiculeDTO.class)
                .value(vehicule -> assertThat(vehicule.immatriculation()).isEqualTo("AB-123-CD"));
        }

        @Test
        void refuseUneImmatriculationEnDouble() {
            webClient.post().uri("/api/vehicules").bodyValue(new VehiculeRequest("AB-123-CD", null, null, null, null))
                .exchange().expectStatus().isCreated();

            webClient.post().uri("/api/vehicules").bodyValue(new VehiculeRequest("ab-123-cd", null, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ErrorDTO.class)
                .value(error -> assertThat(error.message()).contains("AB-123-CD"));
        }

        @Test
        void modifieUnVehiculeSansConflitAvecLuiMeme() {
            VehiculeDTO vehicule = webClient.post().uri("/api/vehicules")
                .bodyValue(new VehiculeRequest("AA-001-AA", "Master", 1000.0, 9.0, null))
                .exchange().expectStatus().isCreated()
                .expectBody(VehiculeDTO.class).returnResult().getResponseBody();

            webClient.put().uri("/api/vehicules/" + vehicule.id())
                .bodyValue(new VehiculeRequest("AA-001-AA", "Master L2", 1200.0, 9.5, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(VehiculeDTO.class)
                .value(updated -> assertThat(updated.capaciteKg()).isEqualTo(1200.0));

            webClient.get().uri("/api/vehicules").exchange().expectBodyList(VehiculeDTO.class).hasSize(1);
            webClient.delete().uri("/api/vehicules/" + vehicule.id()).exchange().expectStatus().isNoContent();
        }

        @Test
        void refuseUneCapaciteNegative() {
            webClient.post().uri("/api/vehicules").bodyValue(new VehiculeRequest("ZZ-999-ZZ", null, -1.0, null, null))
                .exchange().expectStatus().isBadRequest();
        }
    }
}
