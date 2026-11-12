package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Construit les matrices distance/durée complètes en découpant les appels OpenRouteService.
 *
 * <p>ORS limite la taille d'une matrice et le nombre d'appels par minute : on découpe donc en
 * blocs de {@value #CHUNK_SIZE} points, on limite le débit et on réessaie sur erreur 429.
 */
@Slf4j
@Service
public class DistanceMatrixService {

    static final int CHUNK_SIZE = 50;
    private static final int MAX_REQUESTS_PER_MINUTE = 35;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(500);
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(60);

    private final OpenRouteServiceClient orsClient;
    private final OrsMode orsMode;
    private final RateLimiter rateLimiter;
    private final Retry retry;

    public DistanceMatrixService(OpenRouteServiceClient orsClient, OrsMode orsMode) {
        this.orsClient = orsClient;
        this.orsMode = orsMode;
        this.rateLimiter = RateLimiter.of("ors-matrix", RateLimiterConfig.custom()
            .limitForPeriod(MAX_REQUESTS_PER_MINUTE)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofMinutes(2))
            .build());
        this.retry = Retry.of("ors-matrix", RetryConfig.<MatrixResponse>custom()
            .maxAttempts(MAX_ATTEMPTS)
            .retryExceptions(HttpClientErrorException.class)
            .intervalBiFunction(waitBeforeRetry())
            .build());
    }

    public MatrixResponse getMatrices(List<Adresse> addresses) {
        if (orsMode.estimation()) {
            return EstimatedMatrix.of(addresses);
        }
        int size = addresses.size();
        MatrixResponse result = emptyMatrix(size);
        int requestCount = 0;
        for (int sourceStart = 0; sourceStart < size; sourceStart += CHUNK_SIZE) {
            for (int destStart = 0; destStart < size; destStart += CHUNK_SIZE) {
                mergeChunk(result, fetchChunk(addresses, sourceStart, destStart), sourceStart, destStart);
                requestCount++;
            }
        }
        log.info("Matrices de {} points calculées en {} appels ORS", size, requestCount);
        return result;
    }

    private MatrixResponse fetchChunk(List<Adresse> addresses, int sourceStart, int destStart) {
        List<Adresse> sources = addresses.subList(sourceStart, Math.min(sourceStart + CHUNK_SIZE, addresses.size()));
        List<Adresse> destinations = addresses.subList(destStart, Math.min(destStart + CHUNK_SIZE, addresses.size()));

        List<Adresse> locations = new ArrayList<>(sources);
        locations.addAll(destinations);
        int[] sourceIndexes = IntStream.range(0, sources.size()).toArray();
        int[] destinationIndexes = IntStream.range(sources.size(), locations.size()).toArray();

        Supplier<MatrixResponse> call = () -> orsClient.getMatrix(locations, sourceIndexes, destinationIndexes);
        return Retry.decorateSupplier(retry, RateLimiter.decorateSupplier(rateLimiter, call)).get();
    }

    private static MatrixResponse emptyMatrix(int size) {
        MatrixResponse matrix = new MatrixResponse();
        matrix.distances = zeros(size);
        matrix.durations = zeros(size);
        return matrix;
    }

    private static List<List<Integer>> zeros(int size) {
        List<List<Integer>> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rows.add(new ArrayList<>(Collections.nCopies(size, 0)));
        }
        return rows;
    }

    private static void mergeChunk(MatrixResponse target, MatrixResponse chunk, int rowOffset, int columnOffset) {
        if (chunk == null || chunk.distances == null || chunk.durations == null) {
            log.warn("Réponse ORS incomplète ignorée (bloc {}x{})", rowOffset, columnOffset);
            return;
        }
        for (int i = 0; i < chunk.distances.size(); i++) {
            List<Integer> distanceRow = chunk.distances.get(i);
            List<Integer> durationRow = chunk.durations.get(i);
            for (int j = 0; j < distanceRow.size(); j++) {
                target.distances.get(rowOffset + i).set(columnOffset + j, distanceRow.get(j));
                target.durations.get(rowOffset + i).set(columnOffset + j, durationRow.get(j));
            }
        }
    }

    private static IntervalBiFunction<MatrixResponse> waitBeforeRetry() {
        IntervalFunction backoff = IntervalFunction.ofExponentialBackoff(INITIAL_BACKOFF.toMillis(), 2.0);
        return (attempt, outcome) -> retryAfter(outcome)
            .map(Duration::toMillis)
            .orElseGet(() -> backoff.apply(attempt));
    }

    private static Optional<Duration> retryAfter(Either<Throwable, MatrixResponse> outcome) {
        if (!outcome.isLeft()
            || !(outcome.getLeft() instanceof HttpClientErrorException error)
            || error.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
            return Optional.empty();
        }
        String header = error.getResponseHeaders() == null ? null : error.getResponseHeaders().getFirst("Retry-After");
        return Optional.of(parseRetryAfter(header));
    }

    static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return DEFAULT_RETRY_AFTER;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException notSeconds) {
            try {
                Duration untilDate = Duration.between(ZonedDateTime.now(),
                    ZonedDateTime.parse(header.trim(), DateTimeFormatter.RFC_1123_DATE_TIME));
                return untilDate.isNegative() ? Duration.ZERO : untilDate;
            } catch (DateTimeParseException notADate) {
                return DEFAULT_RETRY_AFTER;
            }
        }
    }
}
