package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.services.LivraisonGenerationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetImportService {

  private static final String DATASET_FOLDER = "datasets/";
  private static final String ADDRESS_TYPE_PARKING = "parking";

  private final AdresseRepository adresseRepository;
  private final LivraisonRepository livraisonRepository;
  private final ParkingRepository parkingRepository;

  @Transactional
  public DatasetImportResult importDataset(DatasetDefinition dataset, boolean clearBeforeImport) {
    long importStartNanos = System.nanoTime();
    log.info("Starting dataset import: dataset={}, clearBeforeImport={}", dataset.name(), clearBeforeImport);

    if (clearBeforeImport) {
      log.info("Clearing imported tables before dataset import");
      clearImportedTables();
      log.info("Imported tables cleared");
    }

    log.info("Reading addresses from {}", dataset.getAddressesFile());
    List<AddressCsvRow> rows = readAddressRows(dataset.getAddressesFile());
    if (rows.isEmpty()) {
      throw new IllegalStateException("Dataset " + dataset.name() + " has no addresses");
    }
    log.info("Read {} address rows", rows.size());

    // We do not import matrices in this mode: only persist addresses and the parking entry
    List<AddressCsvRow> orderedRows = orderRowsForMatrixCompatibility(rows);
    log.info("Ordered {} addresses", orderedRows.size());

    log.info("Persisting {} addresses", orderedRows.size());
    Map<String, AdresseEntity> addressesByRef = persistAddresses(orderedRows);
    log.info("Persisted {} addresses", addressesByRef.size());

    log.info("Persisting parking entry");
    persistParking(orderedRows, addressesByRef);
    log.info("Parking entry persisted");

    // No chemins are imported when skipping matrices
    int cheminsCount = 0;

    Duration importDuration = Duration.ofNanos(System.nanoTime() - importStartNanos);
    log.info("Dataset import completed: dataset={}, addresses={}, chemins={}, duration={}ms",
      dataset.name(),
      orderedRows.size(),
      cheminsCount,
      importDuration.toMillis());

    return new DatasetImportResult(dataset.name(), orderedRows.size(), cheminsCount);
  }

  private void clearImportedTables() {
    parkingRepository.deleteAllInBatch();
    livraisonRepository.deleteAllInBatch();
    adresseRepository.deleteAllInBatch();
  }

  private Map<String, AdresseEntity> persistAddresses(List<AddressCsvRow> rows) {
    Map<String, AdresseEntity> addressesByRef = new HashMap<>();
    List<AdresseEntity> entities = new ArrayList<>();

    for (int i = 0; i < rows.size(); i++) {
      AddressCsvRow row = rows.get(i);

      AdresseEntity entity = new AdresseEntity();
      entity.setReference(row.reference());
      entity.setRue(row.name());
      entity.setNumero("");
      entity.setCodePostal(row.postCode());
      entity.setVille(row.city());

      Coords coords = new Coords();
      coords.setLatitude(row.lat());
      coords.setLongitude(row.lng());
      entity.setCoords(coords);

      entities.add(entity);
    }

    List<AdresseEntity> saved = adresseRepository.saveAll(entities);
    for (AdresseEntity entity : saved) {
      addressesByRef.put(entity.getReference(), entity);
    }

    return addressesByRef;
  }

  private void persistParking(List<AddressCsvRow> rows, Map<String, AdresseEntity> addressesByRef) {
    List<AddressCsvRow> parkingRows = rows.stream()
      .filter(row -> ADDRESS_TYPE_PARKING.equalsIgnoreCase(row.type()))
      .toList();

    if (parkingRows.isEmpty()) {
      throw new IllegalStateException("Dataset must contain exactly one parking address, found 0");
    }
    if (parkingRows.size() > 1) {
      throw new IllegalStateException("Dataset must contain exactly one parking address, found " + parkingRows.size());
    }

    AdresseEntity parkingAddress = addressesByRef.get(parkingRows.get(0).reference());
    if (parkingAddress == null) {
      throw new IllegalStateException("Parking address row did not map to a persisted address");
    }

    ParkingEntity parkingEntity = new ParkingEntity();
    parkingEntity.setAdresse(parkingAddress);
    parkingRepository.save(parkingEntity);
  }

  private List<AddressCsvRow> readAddressRows(String fileName) {
    CSVFormat format = CSVFormat.DEFAULT.builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .setTrim(true)
      .setIgnoreEmptyLines(true)
      .build();

    try (BufferedReader reader = openReader(fileName);
         CSVParser parser = format.parse(reader)) {
      List<AddressCsvRow> rows = new ArrayList<>();
      for (CSVRecord record : parser) {
        rows.add(new AddressCsvRow(
          getRequired(record, "type", fileName),
          getRequired(record, "name", fileName),
          getRequired(record, "postCode", fileName),
          getRequired(record, "city", fileName),
          parseDouble(getRequired(record, "lat", fileName), fileName),
          parseDouble(getRequired(record, "lng", fileName), fileName)
        ));
      }

      return rows;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read address file: " + fileName, e);
    }
  }

  private BufferedReader openReader(String fileName) throws IOException {
    ClassPathResource resource = new ClassPathResource(DATASET_FOLDER + fileName);
    return new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
  }

  private List<AddressCsvRow> orderRowsForMatrixCompatibility(List<AddressCsvRow> rows) {
    List<AddressCsvRow> destinations = rows.stream()
      .filter(row -> !ADDRESS_TYPE_PARKING.equalsIgnoreCase(row.type()))
      .toList();
    List<AddressCsvRow> parkings = rows.stream()
      .filter(row -> ADDRESS_TYPE_PARKING.equalsIgnoreCase(row.type()))
      .toList();

    List<AddressCsvRow> ordered = new ArrayList<>(rows.size());
    ordered.addAll(destinations);
    ordered.addAll(parkings);

    for (int i = 0; i < ordered.size(); i++) {
      AddressCsvRow row = ordered.get(i);
      ordered.set(i, new AddressCsvRow(
        row.type(),
        row.name(),
        row.postCode(),
        row.city(),
        row.lat(),
        row.lng(),
        buildRuntimeAddressReference()
      ));
    }

    return ordered;
  }

  private static String buildRuntimeAddressReference() {
    return UUID.randomUUID().toString();
  }

  private static double parseDouble(String raw, String fileName) {
    try {
      return Double.parseDouble(raw.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid numeric value '" + raw + "' in file " + fileName, e);
    }
  }

  private static String getRequired(CSVRecord record, String column, String fileName) {
    if (!record.isMapped(column)) {
      throw new IllegalStateException("Missing column '" + column + "' in file " + fileName);
    }

    String value = record.get(column);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Blank value for column '" + column + "' in file " + fileName);
    }
    return value;
  }

  private record AddressCsvRow(
    String type,
    String name,
    String postCode,
    String city,
    double lat,
    double lng,
    String reference
  ) {
    private AddressCsvRow(String type, String name, String postCode, String city, double lat, double lng) {
      this(type, name, postCode, city, lat, lng, null);
    }
  }

  public record DatasetImportResult(
    String dataset,
    int addressesImported,
    int cheminsImported
  ) {
  }
}
