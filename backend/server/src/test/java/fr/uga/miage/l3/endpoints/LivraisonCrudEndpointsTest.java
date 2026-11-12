package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.fixtures.DatabaseCleaner;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.repository.TourneeRepository;
import fr.uga.miage.l3.request.AdresseRequest;
import fr.uga.miage.l3.request.LivraisonRequest;
import fr.uga.miage.l3.request.LivraisonStatutRequest;
import fr.uga.miage.l3.responses.LivraisonResponsesDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class LivraisonCrudEndpointsTest {

    private static final AdresseRequest VICTOR_HUGO = new AdresseRequest("1", "Place Victor Hugo", "38000", "Grenoble", 45.1885, 5.7245);

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory factory;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private TourneeRepository tourneeRepository;
    @Autowired private TransactionTemplate transaction;

    @AfterEach
    void cleanup() {
        cleaner.clean();
    }

    @Test
    void creeUneLivraisonAvecUneNouvelleAdresse() {
        LivraisonResponsesDTO created = create(new LivraisonRequest(VICTOR_HUGO, 4.5, "PT10M", LocalDate.of(2026, 11, 1)));

        assertThat(created.id()).isPositive();
        assertThat(created.reference()).startsWith("LIV-");
        assertThat(created.poids()).isEqualTo(4.5);
        assertThat(created.tempsMontage()).isEqualTo("PT10M");
        assertThat(created.livree()).isFalse();
        assertThat(created.adresse().rue()).isEqualTo("Place Victor Hugo");
        assertThat(created.adresse().coordonnees().lat()).isEqualTo(45.1885);
    }

    @Test
    void sansDateDeCommandeOnPrendAujourdHui() {
        LivraisonResponsesDTO created = create(new LivraisonRequest(VICTOR_HUGO, 1.0, null, null));

        assertThat(created.dateCommande()).isEqualTo(LocalDate.now());
        assertThat(created.tempsMontage()).isEqualTo("PT0S");
    }

    @Test
    void refuseUneLivraisonInvalide() {
        AdresseRequest sansRue = new AdresseRequest("1", "", "38000", "Grenoble", 45.0, 5.0);

        webClient.post().uri("/api/livraisons").bodyValue(new LivraisonRequest(sansRue, 1.0, null, null))
            .exchange().expectStatus().isBadRequest();
        webClient.post().uri("/api/livraisons").bodyValue(new LivraisonRequest(VICTOR_HUGO, -2.0, null, null))
            .exchange().expectStatus().isBadRequest();
        webClient.post().uri("/api/livraisons").bodyValue(new LivraisonRequest(VICTOR_HUGO, 1.0, "dix minutes", null))
            .exchange().expectStatus().isBadRequest();
    }

    @Test
    void modifieLePoidsEtLAdresse() {
        LivraisonResponsesDTO created = create(new LivraisonRequest(VICTOR_HUGO, 1.0, null, null));
        AdresseRequest gare = new AdresseRequest("", "Place de la Gare", "38000", "Grenoble", 45.1913, 5.7146);

        webClient.put().uri("/api/livraisons/" + created.id())
            .bodyValue(new LivraisonRequest(gare, 8.0, "PT5M", null))
            .exchange()
            .expectStatus().isOk()
            .expectBody(LivraisonResponsesDTO.class)
            .value(updated -> {
                assertThat(updated.poids()).isEqualTo(8.0);
                assertThat(updated.adresse().rue()).isEqualTo("Place de la Gare");
                assertThat(updated.reference()).isEqualTo(created.reference());
            });
    }

    @Test
    void marqueLivreePuisAnnule() {
        LivraisonResponsesDTO created = create(new LivraisonRequest(VICTOR_HUGO, 1.0, null, null));

        LivraisonResponsesDTO livree = patchStatut(created.id(), true);
        assertThat(livree.livree()).isTrue();
        assertThat(livree.dateLivraison()).isNotNull();

        LivraisonResponsesDTO annulee = patchStatut(created.id(), false);
        assertThat(annulee.livree()).isFalse();
        assertThat(annulee.dateLivraison()).isNull();
    }

    @Test
    void supprimerUneLivraisonLaRetireDesTournees() {
        LivraisonEntity gardee = factory.createLivraison(1);
        LivraisonEntity supprimee = factory.createLivraison(1);
        SolutionEntity solution = factory.createSolution(LocalDate.of(2026, 5, 1));
        TourneeEntity tournee = factory.createTournee(solution, List.of(gardee, supprimee));

        webClient.delete().uri("/api/livraisons/" + supprimee.getId()).exchange().expectStatus().isNoContent();

        List<Long> restantes = transaction.execute(status -> tourneeRepository.findById(tournee.getId()).orElseThrow()
            .getLivraisonEntities().stream().map(LivraisonEntity::getId).toList());
        assertThat(restantes).containsExactly(gardee.getId());
        webClient.get().uri("/api/livraisons/" + supprimee.getId()).exchange().expectStatus().isNotFound();
    }

    @Test
    void livraisonInconnueDonne404() {
        webClient.get().uri("/api/livraisons/987654").exchange().expectStatus().isNotFound();
        webClient.delete().uri("/api/livraisons/987654").exchange().expectStatus().isNotFound();
        webClient.patch().uri("/api/livraisons/987654/statut").bodyValue(new LivraisonStatutRequest(true))
            .exchange().expectStatus().isNotFound();
    }

    private LivraisonResponsesDTO create(LivraisonRequest request) {
        return webClient.post().uri("/api/livraisons").bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(LivraisonResponsesDTO.class)
            .returnResult().getResponseBody();
    }

    private LivraisonResponsesDTO patchStatut(Long id, boolean livree) {
        return webClient.patch().uri("/api/livraisons/" + id + "/statut").bodyValue(new LivraisonStatutRequest(livree))
            .exchange()
            .expectStatus().isOk()
            .expectBody(LivraisonResponsesDTO.class)
            .returnResult().getResponseBody();
    }
}
