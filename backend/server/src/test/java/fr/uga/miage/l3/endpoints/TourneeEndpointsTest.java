package fr.uga.miage.l3.endpoints;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.fixtures.DatabaseCleaner;
import fr.uga.miage.l3.fixtures.TestDataFactory;
import fr.uga.miage.l3.models.entities.ChauffeurEntity;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.SolutionEntity;
import fr.uga.miage.l3.models.entities.TourneeEntity;
import fr.uga.miage.l3.models.entities.VehiculeEntity;
import fr.uga.miage.l3.repository.ChauffeurRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.VehiculeRepository;
import fr.uga.miage.l3.request.AffectationRequest;
import fr.uga.miage.l3.responses.TourneeResponsesDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@AutoConfigureWebTestClient
class TourneeEndpointsTest {

    @Autowired private WebTestClient webClient;
    @Autowired private TestDataFactory factory;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private ChauffeurRepository chauffeurRepository;
    @Autowired private VehiculeRepository vehiculeRepository;
    @Autowired private LivraisonRepository livraisonRepository;

    private TourneeEntity premiere;
    private TourneeEntity seconde;
    private ChauffeurEntity lea;
    private VehiculeEntity kangoo;

    @BeforeEach
    void setup() {
        SolutionEntity solution = factory.createSolution(LocalDate.of(2026, 6, 1));
        // TestDataFactory : 10 kg par livraison, statistiques PT30M de trajet et 100 mL de carburant.
        premiere = factory.createTournee(solution, List.of(factory.createLivraison(1), factory.createLivraison(1)));
        seconde = factory.createTournee(solution, List.of(factory.createLivraison(1)));
        lea = chauffeurRepository.save(ChauffeurEntity.builder().prenom("Léa").nom("Martin").tauxHoraire(30.0).build());
        kangoo = vehiculeRepository.save(VehiculeEntity.builder().immatriculation("AB-123-CD").capaciteKg(500.0).build());
    }

    @AfterEach
    void cleanup() {
        cleaner.clean();
    }

    @Test
    void affecteUnChauffeurEtUnVehicule() {
        TourneeResponsesDTO tournee = affecter(premiere.getId(), new AffectationRequest(lea.getId(), kangoo.getId()))
            .expectStatus().isOk()
            .expectBody(TourneeResponsesDTO.class).returnResult().getResponseBody();

        assertThat(tournee.chauffeur().prenom()).isEqualTo("Léa");
        assertThat(tournee.vehicule().immatriculation()).isEqualTo("AB-123-CD");
        assertThat(tournee.poidsTotal()).isEqualTo(20.0);
        // 45 minutes payées 30 €/h.
        assertThat(tournee.finances().coutChauffeur()).isEqualTo(22.5);
        assertThat(tournee.finances().chiffreAffaires()).isPositive();
    }

    @Test
    void retirerLAffectationRemetLesValeursParDefaut() {
        affecter(premiere.getId(), new AffectationRequest(lea.getId(), kangoo.getId())).expectStatus().isOk();

        affecter(premiere.getId(), new AffectationRequest(null, null))
            .expectStatus().isOk()
            .expectBody(TourneeResponsesDTO.class)
            .value(tournee -> {
                assertThat(tournee.chauffeur()).isNull();
                assertThat(tournee.vehicule()).isNull();
            });
    }

    @Test
    void unChauffeurNeConduitQuUneTourneeParSolution() {
        affecter(premiere.getId(), new AffectationRequest(lea.getId(), null)).expectStatus().isOk();

        affecter(seconde.getId(), new AffectationRequest(lea.getId(), null))
            .expectStatus().isEqualTo(409)
            .expectBody(ErrorDTO.class)
            .value(error -> assertThat(error.message()).contains("Léa Martin"));
    }

    @Test
    void unVehiculeNeSertQuUneFoisParSolution() {
        affecter(premiere.getId(), new AffectationRequest(null, kangoo.getId())).expectStatus().isOk();

        affecter(seconde.getId(), new AffectationRequest(null, kangoo.getId())).expectStatus().isEqualTo(409);
    }

    @Test
    void refuseUnVehiculeTropPetit() {
        VehiculeEntity scooter = vehiculeRepository.save(VehiculeEntity.builder().immatriculation("SC-001-OO").capaciteKg(15.0).build());

        affecter(premiere.getId(), new AffectationRequest(null, scooter.getId()))
            .expectStatus().isEqualTo(409)
            .expectBody(ErrorDTO.class)
            .value(error -> assertThat(error.message()).contains("20,0 kg").contains("15,0"));
    }

    @Test
    void refuseUnChauffeurInconnu() {
        affecter(premiere.getId(), new AffectationRequest(999_999L, null)).expectStatus().isNotFound();
    }

    @Test
    void terminerMarqueToutesLesLivraisonsCommeLivrees() {
        webClient.post().uri("/api/tournees/" + premiere.getId() + "/terminer")
            .exchange()
            .expectStatus().isOk()
            .expectBody(TourneeResponsesDTO.class)
            .value(tournee -> assertThat(tournee.livraisons()).allMatch(l -> l.livree() && l.dateLivraison() != null));

        assertThat(livraisonRepository.findAll()).filteredOn(LivraisonEntity::isLivree).hasSize(2);
    }

    @Test
    void exporteLaFeuilleDeRouteEnCsv() {
        byte[] csv = webClient.get().uri("/api/tournees/" + premiere.getId() + "/feuille-de-route")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith("text/csv")
            .expectHeader().value(HttpHeaders.CONTENT_DISPOSITION, value -> assertThat(value)
                .contains("attachment")
                .contains("feuille-de-route-2026-06-01-tournee-" + premiere.getId() + ".csv"))
            .expectBody(byte[].class).returnResult().getResponseBody();

        assertThat(new String(csv, StandardCharsets.UTF_8).lines()).hasSize(3);
    }

    @Test
    void supprimerLeChauffeurEtLeVehiculeLibereLaTournee() {
        affecter(premiere.getId(), new AffectationRequest(lea.getId(), kangoo.getId())).expectStatus().isOk();

        webClient.delete().uri("/api/chauffeurs/" + lea.getId()).exchange().expectStatus().isNoContent();
        webClient.delete().uri("/api/vehicules/" + kangoo.getId()).exchange().expectStatus().isNoContent();

        webClient.get().uri("/api/tournees/" + premiere.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(TourneeResponsesDTO.class)
            .value(tournee -> {
                assertThat(tournee.chauffeur()).isNull();
                assertThat(tournee.vehicule()).isNull();
            });
    }

    @Test
    void tourneeInconnueDonne404() {
        webClient.get().uri("/api/tournees/999999").exchange().expectStatus().isNotFound();
    }

    private WebTestClient.ResponseSpec affecter(Long tourneeId, AffectationRequest request) {
        return webClient.put().uri("/api/tournees/" + tourneeId + "/affectation").bodyValue(request).exchange();
    }
}
