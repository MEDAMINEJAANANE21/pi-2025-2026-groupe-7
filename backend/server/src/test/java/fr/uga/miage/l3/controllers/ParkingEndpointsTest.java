package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.mappers.ParkingMapper;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import org.junit.jupiter.api.*;
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
class ParkingEndpointsTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private ParkingRepository parkingRepository;
    @Autowired
    private AdresseRepository adresseRepository;
    @Autowired
    private ParkingMapper parkingMapper;

    @AfterEach
    void cleanUp() {
        parkingRepository.deleteAll();
        adresseRepository.deleteAll();
    }

    private ParkingEntity persisterDepot(String reference, String rue, String ville, double lat, double lng) {
        Coords coords = Coords.builder()
            .latitude(lat)
            .longitude(lng)
            .build();

        AdresseEntity adresse = AdresseEntity.builder()
            .reference(reference)
            .rue(rue)
            .numero("1")
            .codePostal("38000")
            .ville(ville)
            .coords(coords)
            .build();

        adresse = adresseRepository.save(adresse);

        ParkingEntity parking = ParkingEntity.builder()
            .adresse(adresse)
            .build();

        return parkingRepository.save(parking);
    }

    @Test
    void getDepot_quandDepotExiste_retourne200AvecAdresseComplete(){
        ParkingEntity depot = persisterDepot("DEPOT-GRE-001", "Rue des Lilas", "Grenoble", 45.1885, 5.7245);
        ParkingResponsesDTO expected = parkingMapper.toDTO(depot);
        webClient.get()
            .uri("/api/parkings")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ParkingResponsesDTO.class).isEqualTo(expected);
    }

    @Test
    void getDepot_quandDepotNExistePas_retourne404() {
        webClient.get()
            .uri("/api/parkings")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorDTO.class)
            .value(error -> {
                assertThat(error).isNotNull();
                assertThat(error.uri()).isEqualTo("/api/parkings");
                assertThat(error.message()).isNotEmpty();
            });
    }

}
