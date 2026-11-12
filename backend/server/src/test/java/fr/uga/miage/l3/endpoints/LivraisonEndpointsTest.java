package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.responses.AdresseDTO;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class LivraisonEndpointsTest {
    @Autowired private WebTestClient webClient;
    @Autowired private LivraisonRepository livraisonRepository;
    @Autowired private AdresseRepository adresseRepository;
    @Autowired private TestDataFactory testDataFactory;

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: resolve child→parent dependencies.
        livraisonRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    @Nested
    class GetLivraisonsTest {
        @Test
        void testLivraisonsEmpty() {
            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(0);
        }

        @Test
        void testLivraisonsWithOneLivraison() {
            LivraisonEntity livraison = testDataFactory.createLivraison(2);

            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> responseBody = Objects.requireNonNull(result.getResponseBody());
                    LivraisonResponsesDTO dto = responseBody.get(0);
                    assertThat(dto.poids()).isEqualTo(livraison.getPoids());
                    assertThat(dto.adresse()).isEqualTo(new AdresseDTO(
                        null,
                        "1 Rue de Test",
                        null,
                        "38000",
                        "Grenoble"
                    ));
                });
        }

        @Test
        void testLivraisonsWithMultipleLivraisons() {
            testDataFactory.createLivraison(1);
            testDataFactory.createLivraison(3);

            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(2)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos).allMatch(dto ->
                        dto.adresse().rue().equals("1 Rue de Test") &&
                        dto.adresse().codePostal().equals("38000") &&
                        dto.adresse().ville().equals("Grenoble")
                    );
                });
        }

        @Test
        void testLivraisonsIgnoresLivrees() {
            LivraisonEntity notDelivered = testDataFactory.createLivraison(2);

            // Create delivered livraison (should be filtered out)
            AdresseEntity adresse = new AdresseEntity();
            adresse.setReference("ADR-DELIVERED");
            adresse.setRue("2 Rue Livrée");
            adresse.setVille("Lyon");
            adresse.setCodePostal("69000");
            adresseRepository.save(adresse);

            LivraisonEntity delivered = LivraisonEntity.builder()
                .reference("LIV-DELIVERED")
                .poids(5.0)
                .livree(true)
                .dateCommande(LocalDate.of(2026, 4, 15))
                .adresseEntity(adresse)
                .build();
            livraisonRepository.save(delivered);

            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(LivraisonResponsesDTO.class)
                .hasSize(1)
                .consumeWith(result -> {
                    List<LivraisonResponsesDTO> dtos = Objects.requireNonNull(result.getResponseBody());
                    assertThat(dtos.get(0).poids()).isEqualTo(notDelivered.getPoids());
                });
        }

        @Test
        void testLivraisonsMissingAdresse() {
            LivraisonEntity livraison = livraisonRepository.save(LivraisonEntity.builder()
                .reference("LIV-NO-ADRESSE")
                .poids(12.0)
                .build());

            webClient.get()
                .uri("/api/livraisons")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class)
                .isEqualTo(new ErrorDTO(
                    "/api/livraisons",
                    "Error fetching livraisons: Adresse missing for livraison " + livraison.getId()
                ));
        }
    }

}





