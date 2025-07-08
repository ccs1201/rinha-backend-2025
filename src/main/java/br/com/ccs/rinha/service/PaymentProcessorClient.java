package br.com.ccs.rinha.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.nonNull;

@Service
public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private final PaymentStorage storage;
    private final ExecutorService executor;
    private final RestTemplate restTemplate;
    private final String defaultUrl;
    private final String fallbackUrl;

    public PaymentProcessorClient(
            PaymentStorage paymentStorage,
            RestTemplate restTemplate,
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl) {

        this.storage = paymentStorage;

        log.info("Default service URL: {}", defaultUrl);
        log.info("Fallback fallback URL: {}", fallbackUrl);
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.restTemplate = restTemplate;
        this.executor = Executors.newFixedThreadPool(100, Thread.ofVirtual().factory());
    }

    @PreDestroy
    public void destroy() {
        if (nonNull(executor) && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void processPayment(UUID correlationId, BigDecimal amount) {
        var request = new PaymentRequest(correlationId, amount, OffsetDateTime.now());
        CompletableFuture.runAsync(() ->
                        postToDefault(request), executor)
                .exceptionally(e -> {
                    log.error("Error processing payment on default service: {}", e.getMessage());
                    sleep();
                    postToFallback(request);
                    return null;
                }).exceptionally(e -> {
                    log.error("Error processing payment on fallback service: {}", e.getMessage());
                    sleep();
                    processPayment(request.correlationId, request.amount);
                    return null;
                });
    }

    private static void sleep() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void postToDefault(PaymentRequest request) {
        restTemplate.postForEntity(defaultUrl, request, PaymentResponse.class);
        store(request, true);
    }

    private void postToFallback(PaymentRequest request) {
        restTemplate.postForEntity(fallbackUrl, request, PaymentResponse.class);
        store(request, false);
    }

    private void store(PaymentRequest request, boolean isDefault) {
        storage.store(request, isDefault);
    }

    public record PaymentRequest(UUID correlationId, BigDecimal amount,
                                 @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") OffsetDateTime requestedAt) {
    }

    record PaymentResponse(String message) {
    }
}