package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.nonNull;

@Service
public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private final PaymentRepository repository;
    private final ExecutorService executor;
    private final RestTemplate restTemplate;
    private final String defaultUrl;
    private final String fallbackUrl;

    public PaymentProcessorClient(
            PaymentRepository paymentRepository,
            RestTemplate restTemplate,
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl,
            @Value("${THREAD_POOL_SIZE:10}") int threadPoolSize,
            @Value("${spring.threads.virtual.enabled}") boolean virtualThread) {

        this.repository = paymentRepository;

        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.restTemplate = restTemplate;
        this.executor = Executors.newFixedThreadPool(threadPoolSize, Thread.ofVirtual().factory());

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
        log.info("Thread pool size: {}", threadPoolSize);
        log.info("Virtual thread enabled: {}", virtualThread);
    }

    @PreDestroy
    public void destroy() {
        if (nonNull(executor) && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void processPayment(PaymentRequest paymentRequest) {
        doProcessAsync(paymentRequest);
    }

    private void doProcessAsync(PaymentRequest paymentRequest) {
              CompletableFuture.runAsync(() -> {
                    try {
                        postToDefault(paymentRequest);
                    } catch (RestClientException e) {
//                        log.error("Error processing payment on default service: {}", e.getMessage());
                        postToFallback(paymentRequest);
                    }
                }, executor)
                .exceptionally(ex -> {
//                    log.error("Error processing payment on fallback service: {}", ex.getMessage());
                    processPayment(paymentRequest);
                    return null;
                });
    }

    private void postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        restTemplate.postForEntity(defaultUrl, paymentRequest, PaymentResponse.class);
        repository.store(paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        restTemplate.postForEntity(fallbackUrl, paymentRequest, PaymentResponse.class);
        repository.store(paymentRequest);
    }

    record PaymentResponse(String message) {
    }
}