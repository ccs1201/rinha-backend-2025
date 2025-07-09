package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
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
            @Value("${payment-processor.fallback.url}") String fallbackUrl,
            @Value("${THREAD_POOL_SIZE:10}") int threadPoolSize,
            @Value("${spring.threads.virtual.enabled}") boolean virtualThread) {

        this.storage = paymentStorage;

        log.info("Default service URL: {}", defaultUrl);
        log.info("Fallback fallback URL: {}", fallbackUrl);
        log.info("Thread pool size: {}", threadPoolSize);
        log.info("Virtual thread enabled: {}", virtualThread);
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.restTemplate = restTemplate;
        this.executor = Executors.newFixedThreadPool(threadPoolSize, Thread.ofVirtual().factory());
    }

    @PreDestroy
    public void destroy() {
        if (nonNull(executor) && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void processPayment(PaymentRequest request) {
        CompletableFuture.runAsync(() ->
                        postToDefault(request), executor)
                .exceptionally(e -> {
                    log.error("Error processing payment on default service: {}", e.getMessage());
                    sleep(2000);
                    postToFallback(request);
                    return null;
                }).exceptionally(e -> {
                    log.error("Error processing payment on fallback service: {}", e.getMessage());
                    sleep(2000);
                    processPayment(request);
                    return null;
                });
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void postToDefault(PaymentRequest request) {
        request.processedAt = OffsetDateTime.now();
        request.setDefaultTrue();
        restTemplate.postForEntity(defaultUrl, request, PaymentResponse.class);
        store(request, true);
    }

    private void postToFallback(PaymentRequest request) {
        request.setDefaultFalse();
        restTemplate.postForEntity(fallbackUrl, request, PaymentResponse.class);
        store(request, false);
    }

    private void store(PaymentRequest request, boolean isDefault) {
        storage.store(request, isDefault);
    }

    record PaymentResponse(String message) {
    }
}