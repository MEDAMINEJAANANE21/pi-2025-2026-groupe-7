package fr.uga.miage.l3.fixtures;

import fr.uga.miage.l3.enums.DatasetDefinition;
import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.Coords;
import fr.uga.miage.l3.models.entities.LivraisonEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.LivraisonRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.services.core.OpenRouteServiceClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Test fixture that seeds the database from a CSV dataset and configures
 * the {@link OpenRouteServiceClient#getMatrix} mock to return realistic
 * sub-matrices drawn from the dataset's precomputed distance/duration CSVs.
 *
 * <p>Address ordering:
 * <ol>
 *   <li>Destinations 0..N-1 (same order as in the dataset CSV)</li>
 *   <li>Parking at index N</li>
 * </ol>
 * The distance/duration CSVs only cover the N destinations; the parking row
 * and column are filled with zeros.
 */
@Component
public class DatasetMatrixFixture {

    private static final String DATASET_FOLDER = "datasets/";
    private static final double COORD_TOLERANCE = 1e-7;

    @Autowired
    private AdresseRepository adresseRepository;
    @Autowired
    private LivraisonRepository livraisonRepository;
    @Autowired
    private ParkingRepository parkingRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses the dataset CSV, persists addresses + parking + livraisons into the
     * database, and loads the distance / duration matrices.
     *
     * @param dataset which dataset to import
     * @return fixture data needed to configure the ORS mock
     */
    public DatasetFixtureData seed(DatasetDefinition dataset) {
        List<AddressRow> rows = readAddressRows(dataset.getAddressesFile());

        List<AddressRow> destinations = rows.stream()
                .filter(r -> !"parking".equalsIgnoreCase(r.type()))
                .toList();
        List<AddressRow> parkings = rows.stream()
                .filter(r -> "parking".equalsIgnoreCase(r.type()))
                .toList();

        if (parkings.isEmpty()) {
            throw new IllegalStateException("Dataset has no parking row: " + dataset);
        }

        // Persist destination addresses and create one livraison per destination
        for (AddressRow row : destinations) {
            AdresseEntity entity = persistAdresse(row);

            LivraisonEntity livraison = LivraisonEntity.builder()
                    .reference(UUID.randomUUID().toString())
                    .livree(false)
                    .poids(10.0)
                    .dateCommande(LocalDate.of(2026, 4, 16))
                    .adresseEntity(entity)
                    .build();
            livraisonRepository.save(livraison);
        }

        // Persist parking address and parking entity
        AdresseEntity parkingAdresseEntity = persistAdresse(parkings.get(0));
        ParkingEntity parkingEntity = new ParkingEntity();
        parkingEntity.setAdresse(parkingAdresseEntity);
        parkingRepository.save(parkingEntity);

        // Build ordered coordinate list (destinations first, parking last)
        int n = destinations.size();
        double[] lats = new double[n + 1];
        double[] lngs = new double[n + 1];
        for (int i = 0; i < n; i++) {
            lats[i] = destinations.get(i).lat();
            lngs[i] = destinations.get(i).lng();
        }
        lats[n] = parkings.get(0).lat();
        lngs[n] = parkings.get(0).lng();

        // Load NxN distance and duration matrices from CSV (destinations only)
        double[][] rawDist = loadMatrix(dataset.getDistanceMatrixFile());
        double[][] rawDur  = loadMatrix(dataset.getDurationMatrixFile());

        // Extend to (N+1)x(N+1) with zeros for the parking row/column
        double[][] fullDist = extendMatrix(rawDist, n);
        double[][] fullDur  = extendMatrix(rawDur, n);

        return new DatasetFixtureData(lats, lngs, fullDist, fullDur, n);
    }

    /**
     * Configures the Mockito mock for {@link OpenRouteServiceClient#getMatrix}
     * so that it returns sub-matrices derived from the precomputed CSV matrices.
     *
     * <p>The mock matches addresses by lat/lng and returns the correct chunk of
     * the full matrix for every call made by {@code AlgosService.getMatrices()}.
     *
     * @param mock        the mocked {@link OpenRouteServiceClient}
     * @param fixtureData data returned by {@link #seed}
     */
    public void configureMock(OpenRouteServiceClient mock, DatasetFixtureData fixtureData) {
        doAnswer(invocation -> {
            List<Adresse> combined = invocation.getArgument(0);
            int[]         srcIdxs  = invocation.getArgument(1);
            int[]         dstIdxs  = invocation.getArgument(2);

            // Map every location in 'combined' to its global index in the full matrix
            int[] globalIdxs = new int[combined.size()];
            for (int k = 0; k < combined.size(); k++) {
                globalIdxs[k] = findGlobalIndex(
                        combined.get(k).coords.lat,
                        combined.get(k).coords.lng,
                        fixtureData);
            }

            // Build the chunk MatrixResponse
            MatrixResponse chunk = new MatrixResponse();
            chunk.distances    = new ArrayList<>();
            chunk.durations    = new ArrayList<>();
            chunk.destinations = new ArrayList<>();
            chunk.sources      = new ArrayList<>();

            for (int srcLocalIdx : srcIdxs) {
                int gi = globalIdxs[srcLocalIdx];
                List<Integer> distRow = new ArrayList<>();
                List<Integer> durRow  = new ArrayList<>();
                for (int dstLocalIdx : dstIdxs) {
                    int gj = globalIdxs[dstLocalIdx];
                    distRow.add(safeGet(fixtureData.fullDistMatrix(), gi, gj));
                    durRow .add(safeGet(fixtureData.fullDurMatrix(),  gi, gj));
                }
                chunk.distances.add(distRow);
                chunk.durations.add(durRow);
            }

            return chunk;
        }).when(mock).getMatrix(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AdresseEntity persistAdresse(AddressRow row) {
        AdresseEntity entity = new AdresseEntity();
        entity.setReference(UUID.randomUUID().toString());
        entity.setRue(row.name());
        entity.setNumero("");
        entity.setCodePostal(row.postCode());
        entity.setVille(row.city());

        Coords coords = new Coords();
        coords.setLatitude(row.lat());
        coords.setLongitude(row.lng());
        entity.setCoords(coords);

        return adresseRepository.save(entity);
    }

    private int findGlobalIndex(double lat, double lng, DatasetFixtureData data) {
        for (int i = 0; i <= data.destinationCount(); i++) {
            if (Math.abs(data.lats()[i] - lat) < COORD_TOLERANCE
                    && Math.abs(data.lngs()[i] - lng) < COORD_TOLERANCE) {
                return i;
            }
        }
        // Fallback: parking index (last) – happens if parking coords are passed
        return data.destinationCount();
    }

    private int safeGet(double[][] matrix, int i, int j) {
        if (i < 0 || j < 0 || i >= matrix.length || j >= matrix[i].length) {
            return 0;
        }
        return (int) matrix[i][j];
    }

    /**
     * Extends an NxN matrix to (N+1)x(N+1) by adding a zero row and column
     * at index N (parking position).
     */
    private double[][] extendMatrix(double[][] base, int n) {
        int size = n + 1;
        double[][] extended = new double[size][size];
        for (int i = 0; i < n; i++) {
            if (i < base.length) {
                System.arraycopy(base[i], 0, extended[i], 0, Math.min(base[i].length, n));
            }
        }
        // Row/col for parking (index n) remain 0
        return extended;
    }

    private double[][] loadMatrix(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(DATASET_FOLDER + fileName);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                List<double[]> rows = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    double[] row = new double[parts.length];
                    for (int j = 0; j < parts.length; j++) {
                        row[j] = Double.parseDouble(parts[j].trim());
                    }
                    rows.add(row);
                }
                return rows.toArray(new double[0][]);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read matrix file: " + fileName, e);
        }
    }

    private List<AddressRow> readAddressRows(String fileName) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .get();

        try {
            ClassPathResource resource = new ClassPathResource(DATASET_FOLDER + fileName);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                 CSVParser parser = format.parse(reader)) {

                List<AddressRow> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    rows.add(new AddressRow(
                            record.get("type"),
                            record.get("name"),
                            record.get("postCode"),
                            record.get("city"),
                            Double.parseDouble(record.get("lat")),
                            Double.parseDouble(record.get("lng"))
                    ));
                }
                return rows;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read address file: " + fileName, e);
        }
    }

    // -------------------------------------------------------------------------
    // Data types
    // -------------------------------------------------------------------------

    private record AddressRow(String type, String name, String postCode, String city, double lat, double lng) {}

    /**
     * Carries the ordered coordinates and full matrices needed to configure the mock.
     *
     * @param lats              latitude of each location (destinations 0..N-1, parking N)
     * @param lngs              longitude of each location
     * @param fullDistMatrix    (N+1)×(N+1) distance matrix in metres
     * @param fullDurMatrix     (N+1)×(N+1) duration matrix in seconds
     * @param destinationCount  number of delivery destinations (N)
     */
    public record DatasetFixtureData(
            double[] lats,
            double[] lngs,
            double[][] fullDistMatrix,
            double[][] fullDurMatrix,
            int destinationCount
    ) {}
}



