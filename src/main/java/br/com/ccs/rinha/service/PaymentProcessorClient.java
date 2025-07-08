package br.com.ccs.rinha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private final WebClient defaultClient;
    private final WebClient fallbackClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(300, Thread.ofVirtual().factory());

    public PaymentProcessorClient(
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl) {
        log.info("Default URL: {}", defaultUrl);
        log.info("Fallback URL: {}", fallbackUrl);
        this.defaultClient = WebClient.builder()
                .baseUrl(defaultUrl)
                .build();
        this.fallbackClient = WebClient.builder()
                .baseUrl(fallbackUrl)
                .build();
    }

    public CompletableFuture<Void> processPayment(UUID correlationId, BigDecimal amount, boolean useDefault) {
        var request = new PaymentRequest(correlationId, amount, OffsetDateTime.now());
        var client = useDefault ? defaultClient : fallbackClient;

        return CompletableFuture.runAsync(() ->
                        client.post()
                                .uri("/payments")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(PaymentResponse.class)
                                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                                .timeout(Duration.ofSeconds(5))
                                .onErrorResume(e -> Mono.empty())
                                .block()
                , executor).orTimeout(5, TimeUnit.SECONDS);
    }

    public ServiceHealth checkHealth(boolean isDefault) {
        var client = isDefault ? defaultClient : fallbackClient;

        return client.get()
                .uri("/payments/service-health")
                .retrieve()
                .bodyToMono(ServiceHealth.class)
                .block();
    }

    record PaymentRequest(UUID correlationId, BigDecimal amount, OffsetDateTime requestedAt) {
    }

    record PaymentResponse(String message) {
    }

    public record ServiceHealth(boolean failing, int minResponseTime) {
    }
}