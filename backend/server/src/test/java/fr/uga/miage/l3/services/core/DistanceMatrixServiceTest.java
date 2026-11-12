package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.Coords;
import fr.uga.miage.l3.models.core.MatrixResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistanceMatrixServiceTest {

    private final OpenRouteServiceClient client = mock(OpenRouteServiceClient.class);
    private final DistanceMatrixService service = new DistanceMatrixService(client, new OrsMode("cle", true));

    @Test
    void decoupeLesGrandesMatricesEtRecolleLesBlocs() {
        // 60 points : 2 blocs de sources × 2 blocs de destinations = 4 appels.
        List<Adresse> addresses = IntStream.range(0, 60)
            .mapToObj(i -> new Adresse(new Coords(45, 5 + i * 0.001), "Point " + i, "38000", "Grenoble"))
            .toList();
        // Chaque bloc renvoie une valeur reconnaissable : distance = 1000 × source + destination (indices globaux).
        when(client.getMatrix(any(), any(), any())).thenAnswer(invocation -> {
            List<Adresse> locations = invocation.getArgument(0);
            int[] sources = invocation.getArgument(1);
            int[] destinations = invocation.getArgument(2);
            return chunk(addresses, locations, sources, destinations);
        });

        MatrixResponse matrix = service.getMatrices(addresses);

        verify(client, times(4)).getMatrix(any(), any(), any());
        assertThat(matrix.distances).hasSize(60);
        assertThat(matrix.distances.get(0).get(59)).isEqualTo(59);
        assertThat(matrix.distances.get(55).get(3)).isEqualTo(55_003);
        assertThat(matrix.durations.get(59).get(59)).isEqualTo(59_059);
    }

    @Test
    void sansCleLesDistancesSontEstimeesSansAppelReseau() {
        DistanceMatrixService estimation = new DistanceMatrixService(client, new OrsMode("", true));
        // Gare de Grenoble → place Victor Hugo : environ 1 km à vol d'oiseau.
        List<Adresse> addresses = List.of(
            new Adresse(new Coords(45.1913, 5.7146), "Gare", "38000", "Grenoble"),
            new Adresse(new Coords(45.1885, 5.7245), "Victor Hugo", "38000", "Grenoble"));

        MatrixResponse matrix = estimation.getMatrices(addresses);

        verify(client, times(0)).getMatrix(any(), any(), any());
        assertThat(matrix.distances.get(0).get(0)).isZero();
        assertThat(matrix.distances.get(0).get(1)).isBetween(1000, 1300).isEqualTo(matrix.distances.get(1).get(0));
        assertThat(matrix.durations.get(0).get(1)).isBetween(110, 160);
    }

    @Test
    void lisLEnTeteRetryAfter() {
        assertThat(DistanceMatrixService.parseRetryAfter("12")).isEqualTo(Duration.ofSeconds(12));
        assertThat(DistanceMatrixService.parseRetryAfter(null)).isEqualTo(Duration.ofSeconds(60));
        assertThat(DistanceMatrixService.parseRetryAfter("pas une date")).isEqualTo(Duration.ofSeconds(60));
        assertThat(DistanceMatrixService.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT")).isEqualTo(Duration.ZERO);
    }

    private static MatrixResponse chunk(List<Adresse> all, List<Adresse> locations, int[] sources, int[] destinations) {
        List<List<Integer>> values = new ArrayList<>();
        for (int source : sources) {
            List<Integer> row = new ArrayList<>();
            for (int destination : destinations) {
                row.add(all.indexOf(locations.get(source)) * 1000 + all.indexOf(locations.get(destination)));
            }
            values.add(row);
        }
        MatrixResponse response = new MatrixResponse();
        response.distances = values;
        response.durations = values;
        return response;
    }
}
