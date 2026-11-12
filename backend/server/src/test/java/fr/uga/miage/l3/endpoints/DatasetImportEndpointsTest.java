package fr.uga.miage.l3.endpoints;
import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.request.DatasetImportRequest;
import fr.uga.miage.l3.request.RandomLivraisonsGenerationRequest;
import fr.uga.miage.l3.responses.DatasetImportResponseDTO;
import fr.uga.miage.l3.responses.RandomGenerationResponseDTO;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class DatasetImportEndpointsTest {

  @Autowired
  private WebTestClient webClient;

  // On n'utilise pas de @MockitoBean pour DatasetImportService / Mapper : test d'intégration complet
  @Autowired
  private AdresseRepository adresseRepository;

  @Autowired
  private ParkingRepository parkingRepository;

  @Autowired
  private LivraisonRepository livraisonRepository;

  @AfterEach
  void cleanup() {
    livraisonRepository.deleteAllInBatch();
    parkingRepository.deleteAllInBatch();
    adresseRepository.deleteAllInBatch();
  }

  @Test
  void testListAvailableDatasets() {
    webClient.get()
      .uri("/api/data")
      .exchange()
      .expectStatus().isOk()
      .expectBodyList(String.class)
      .consumeWith(response -> {
        var list = response.getResponseBody();
        assertNotNull(list);
        assertTrue(list.stream().map(Object::toString)
          .anyMatch(s -> s.contains(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1.name())));
      });
  }

  @Test
  void testImportDatasetSuccess() {
    DatasetImportRequest req = new DatasetImportRequest(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1.name(), true);

    DatasetImportResponseDTO response = webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isOk()
      .expectBody(DatasetImportResponseDTO.class)
      .returnResult()
      .getResponseBody();

    assertNotNull(response);

    long addressesInDb = adresseRepository.count();
    long parkingsInDb = parkingRepository.count();

    assertEquals(response.addressesImported(), addressesInDb);
    assertEquals(0, response.cheminsImported());
    assertEquals(1L, parkingsInDb);
    ParkingEntity parking = parkingRepository.findAll().stream().findFirst().orElse(null);
    assertNotNull(parking);
    AdresseEntity parkingAdresse = parking.getAdresse();
    assertNotNull(parkingAdresse);
    assertTrue(adresseRepository.existsById(parkingAdresse.getId()));
  }

  @Test
  void testImportDatasetBadRequest_compareErrorDTO() {
    DatasetImportRequest req = new DatasetImportRequest(null, null);

    webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/import", "Dataset ne peut pas etre null"));
  }

  @Test
  void testGenerateRandomLivraisonsSuccess() {
    // import préalable pour avoir des adresses
    DatasetImportRequest importReq = new DatasetImportRequest(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1.name(), true);
    webClient.post().uri("/api/data/import").header("Content-Type", "application/json").bodyValue(importReq).exchange().expectStatus().isOk();

    long before = livraisonRepository.count();
    int toGenerate = 5;

    RandomLivraisonsGenerationRequest req = new RandomLivraisonsGenerationRequest(toGenerate);

    RandomGenerationResponseDTO response = webClient.post()
      .uri("/api/data/generate/livraisons")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(RandomGenerationResponseDTO.class)
      .returnResult()
      .getResponseBody();

    assertNotNull(response);
    // vérifie que le nombre de livraisons en base a augmenté du nombre généré
    long after = livraisonRepository.count();
    assertEquals(before + response.generated(), after);
    assertEquals(response.addressesAvailable(), adresseRepository.count());
    assertEquals(response.livraisonsAvailable(), after);

    List<?> allLivraisons = livraisonRepository.findAll();
    Set<Object> adresseIds = allLivraisons.stream()
      .map(l -> {
        try {
          var m = l.getClass().getMethod("getAdresseEntity");
          Object adresse = m.invoke(l);
          var mid = adresse.getClass().getMethod("getId");
          return mid.invoke(adresse);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      })
      .collect(Collectors.toSet());

    assertEquals(allLivraisons.size(), adresseIds.size(), "Les livraisons générées doivent avoir des adresses distinctes");
  }

  @Test
  void testGenerateRandomLivraisonsBadRequestWhenNoAdresse_compareErrorDTO() {
    RandomLivraisonsGenerationRequest req = new RandomLivraisonsGenerationRequest(5);

    webClient.post()
      .uri("/api/data/generate/livraisons")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/generate/livraisons", "No adresse available. Import a dataset first."));
  }

  // Branch 1a: importDataset avec dataset blank
  @Test
  void testImportDatasetBadRequest_blankDataset() {
    DatasetImportRequest req = new DatasetImportRequest("   ", true);

    webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/import", "Dataset ne peut pas etre null"));
  }

  // Branch 2: importDataset avec nom de dataset invalide (IllegalArgumentException)
  @Test
  void testImportDatasetBadRequest_unknownDataset() {
    DatasetImportRequest req = new DatasetImportRequest("UNKNOWN_DATASET_XYZ", true);

    webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest();
  }

  // Branch 3a: clearBeforeImport = null → doit se comporter comme true (import réussi)
  @Test
  void testImportDataset_clearBeforeImportNull_defaultsToTrue() {
    DatasetImportRequest req = new DatasetImportRequest(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1.name(), null);

    DatasetImportResponseDTO response = webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isOk()
      .expectBody(DatasetImportResponseDTO.class)
      .returnResult()
      .getResponseBody();

    assertNotNull(response);
    assertTrue(response.addressesImported() > 0);
  }

  // Branch 3b: clearBeforeImport = false → import sans suppression préalable
  @Test
  void testImportDataset_clearBeforeImportFalse() {
    DatasetImportRequest req = new DatasetImportRequest(DatasetDefinition.DATASET_50_PAR_3_EXEMPLE_1.name(), false);

    DatasetImportResponseDTO response = webClient.post()
      .uri("/api/data/import")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isOk()
      .expectBody(DatasetImportResponseDTO.class)
      .returnResult()
      .getResponseBody();

    assertNotNull(response);
    assertTrue(response.addressesImported() >= 0);
  }

  // Branch 4 & 5a: generateRandomLivraisons avec count = null
  @Test
  void testGenerateRandomLivraisons_nullCount_badRequest() {
    RandomLivraisonsGenerationRequest req = new RandomLivraisonsGenerationRequest(null);

    webClient.post()
      .uri("/api/data/generate/livraisons")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/generate/livraisons", "count doit etre un entier strictement positif"));
  }

  // Branch 5b: generateRandomLivraisons avec count <= 0
  @Test
  void testGenerateRandomLivraisons_zeroCount_badRequest() {
    RandomLivraisonsGenerationRequest req = new RandomLivraisonsGenerationRequest(0);

    webClient.post()
      .uri("/api/data/generate/livraisons")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/generate/livraisons", "count doit etre un entier strictement positif"));
  }

  @Test
  void testGenerateRandomLivraisons_negativeCount_badRequest() {
    RandomLivraisonsGenerationRequest req = new RandomLivraisonsGenerationRequest(-3);

    webClient.post()
      .uri("/api/data/generate/livraisons")
      .header("Content-Type", "application/json")
      .bodyValue(req)
      .exchange()
      .expectStatus().isBadRequest()
      .expectBody(ErrorDTO.class)
      .isEqualTo(new ErrorDTO("/api/data/generate/livraisons", "count doit etre un entier strictement positif"));
  }

}
