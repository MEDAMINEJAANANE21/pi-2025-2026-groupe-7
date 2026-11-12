package fr.uga.miage.l3.services.core;

import fr.uga.miage.l3.models.core.Adresse;
import fr.uga.miage.l3.models.core.MatrixResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.function.Supplier;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.IntervalFunction;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class AlgosService {
    private final OpenRouteServiceClient orsClient;

    // Resilience4j components
    private final RateLimiter resilienceRateLimiter;
    private final Retry resilienceRetry;

    // Rate limiting: 40 requests per 2 minutes
    private static final int MAX_REQUESTS_PER_WINDOW = 35;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 1;
    private final ConcurrentLinkedQueue<LocalDateTime> requestTimes = new ConcurrentLinkedQueue<>();

    public AlgosService(OpenRouteServiceClient orsClient) {
        this.orsClient = orsClient;

        // Configure Resilience4j RateLimiter for 40 calls per 2 minutes
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
            // limit for period is number of permits per refresh period
            .limitForPeriod(MAX_REQUESTS_PER_WINDOW)
            .limitRefreshPeriod(Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES))
            .timeoutDuration(Duration.ofSeconds(5)) // max time to wait for permission
            .build();

        RateLimiterRegistry rlRegistry = RateLimiterRegistry.of(rlConfig);
        this.resilienceRateLimiter = rlRegistry.rateLimiter("orsRateLimiter", rlConfig);

        // Configure Retry: max attempts and exponential backoff; we'll additionally honor Retry-After for 429
        IntervalFunction intervalFn = IntervalFunction.ofExponentialBackoff(500L, 2.0); // 500ms initial
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(5)
            .intervalFunction(intervalFn)
            .retryExceptions(HttpClientErrorException.class)
            .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        this.resilienceRetry = retryRegistry.retry("orsRetry", retryConfig);

        // Add listener to handle Retry events; when we see a 429 we will sleep based on Retry-After header
        this.resilienceRetry.getEventPublisher().onRetry(this::handleRetryEvent);
    }

    /**
     * Retry event handler: if the last throwable was an HTTP 429, try to honor Retry-After header
     */
    private void handleRetryEvent(RetryOnRetryEvent event) {
        Throwable last = event.getLastThrowable();
        if (last instanceof HttpClientErrorException httpEx) {
            if (httpEx.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                String retryAfter = null;
                if (httpEx.getResponseHeaders() != null) {
                    retryAfter = httpEx.getResponseHeaders().getFirst("Retry-After");
                }

                long waitMillis = 60_000; // default 60s
                if (retryAfter != null && !retryAfter.isBlank()) {
                    try {
                        int seconds = Integer.parseInt(retryAfter.trim());
                        waitMillis = seconds * 1000L;
                    } catch (NumberFormatException ex) {
                        try {
                            ZonedDateTime retryDate = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME);
                            long millisTo = ChronoUnit.MILLIS.between(LocalDateTime.now().atZone(retryDate.getZone()), retryDate);
                            if (millisTo > 0) waitMillis = millisTo;
                        } catch (DateTimeParseException ignore) {
                            // keep default
                        }
                    }
                }

                log.warn("Received 429 from ORS during retry. Waiting {} ms before next attempt", waitMillis);
                try {
                    Thread.sleep(waitMillis + 100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Rate limiting check before making API requests
     * Ensures we don't exceed 40 requests per 2 minutes
     */
    private void enforceRateLimit() throws InterruptedException {
        LocalDateTime now = LocalDateTime.now();

        // Remove requests older than 2 minutes
        while (!requestTimes.isEmpty()) {
            LocalDateTime oldest = requestTimes.peek();
            if (oldest.isBefore(now.minusMinutes(RATE_LIMIT_WINDOW_MINUTES))) {
                requestTimes.poll();
            } else {
                break;
            }
        }

        // If we're at the limit, wait until we can proceed
        if (requestTimes.size() >= MAX_REQUESTS_PER_WINDOW) {
            LocalDateTime oldestRequest = requestTimes.peek();
            LocalDateTime canProceedAt = oldestRequest.plusMinutes(RATE_LIMIT_WINDOW_MINUTES);
            long waitMillis = ChronoUnit.MILLIS.between(now, canProceedAt) + 100; // +100ms buffer

            if (waitMillis > 0) {
                log.info("Rate limit reached. Waiting {} ms before next request", waitMillis);
                Thread.sleep(waitMillis);
            }
        }

        // Record this request
        requestTimes.offer(LocalDateTime.now());
    }

    public MatrixResponse getMatrices(List<Adresse> addresses) throws InterruptedException {
        final int limit = 50; // ORS limit per request
        final int totalAddresses = addresses.size();

        log.info("Getting matrix for {} addresses with chunking limit {}", totalAddresses, limit);

        // Initialize result matrix
        MatrixResponse result = new MatrixResponse();
        result.distances = new ArrayList<>();
        result.durations = new ArrayList<>();
        result.destinations = new ArrayList<>();
        result.sources = new ArrayList<>();

        // Initialize empty matrices
        for (int i = 0; i < totalAddresses; i++) {
            result.distances.add(new ArrayList<>());
            result.durations.add(new ArrayList<>());
            for (int j = 0; j < totalAddresses; j++) {
                result.distances.get(i).add(0);
                result.durations.get(i).add(0);
            }
        }

        // Initialize destination and source metadata
        for (Adresse address : addresses) {
            MatrixResponse.LocationRef locationRef = MatrixResponse.LocationRef.builder()
                .location(new double[]{address.coords.lng, address.coords.lat})
                .name(address.name)
                .snapped_distance(0)
                .build();
            result.destinations.add(locationRef);
            result.sources.add(locationRef);
        }

        // Process matrix in chunks
        int requestCount = 0;
        for (int sourceStart = 0; sourceStart < totalAddresses; sourceStart += limit) {
            for (int destStart = 0; destStart < totalAddresses; destStart += limit) {
                int sourceEnd = Math.min(sourceStart + limit, totalAddresses);
                int destEnd = Math.min(destStart + limit, totalAddresses);

                List<Adresse> sourceChunk = addresses.subList(sourceStart, sourceEnd);
                List<Adresse> destChunk = addresses.subList(destStart, destEnd);

                // Create combined locations list
                List<Adresse> combinedLocations = new ArrayList<>(sourceChunk);
                combinedLocations.addAll(destChunk);

                // Create source and destination indices
                int[] sourceIndices = new int[sourceChunk.size()];
                for (int i = 0; i < sourceChunk.size(); i++) {
                    sourceIndices[i] = i;
                }

                int[] destIndices = new int[destChunk.size()];
                for (int i = 0; i < destChunk.size(); i++) {
                    destIndices[i] = i + sourceChunk.size();
                }

                // Also try to respect the existing local window limiter as a defensive measure
                enforceRateLimit();

                log.debug("Making request {}: sources {}-{}, destinations {}-{}",
                    ++requestCount, sourceStart, sourceEnd - 1, destStart, destEnd - 1);

                // Build a supplier that performs the ORS matrix call
                Supplier<MatrixResponse> matrixCall = () -> orsClient.getMatrix(combinedLocations, sourceIndices, destIndices);

                // Decorate with Retry then RateLimiter
                Supplier<MatrixResponse> decorated = io.github.resilience4j.retry.Retry.decorateSupplier(resilienceRetry,
                    io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(resilienceRateLimiter, matrixCall));

                // Execute the decorated supplier (it will apply rate limiting and retries)
                MatrixResponse chunkResponse = decorated.get();

                // Merge the chunk response into the main result
                mergeMatrixChunk(result, chunkResponse, sourceStart, destStart);
            }
        }

        log.info("Matrix generation completed with {} API requests", requestCount);
        return result;
    }

    /**
     * Merges a chunk matrix response into the main result matrix
     */
    private void mergeMatrixChunk(MatrixResponse main, MatrixResponse chunk, int sourceOffset, int destOffset) {
        if (chunk == null || chunk.distances == null || chunk.durations == null) {
            log.warn("Received null or incomplete chunk response");
            return;
        }

        // Copy distances and durations from chunk to main matrix
        for (int i = 0; i < chunk.distances.size(); i++) {
            List<Integer> distanceRow = chunk.distances.get(i);
            List<Integer> durationRow = chunk.durations.get(i);

            for (int j = 0; j < distanceRow.size(); j++) {
                int mainRow = sourceOffset + i;
                int mainCol = destOffset + j;

                if (mainRow < main.distances.size() && mainCol < main.distances.get(mainRow).size()) {
                    main.distances.get(mainRow).set(mainCol, distanceRow.get(j));
                    main.durations.get(mainRow).set(mainCol, durationRow.get(j));
                }
            }
        }
    }
}
