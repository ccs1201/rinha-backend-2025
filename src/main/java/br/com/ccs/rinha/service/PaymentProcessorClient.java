package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.exception.HttpClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
public class PaymentProcessorClient {

    private final Logger log;
    private final PaymentRepository repository;
    private String defaultUrl;
    private String fallbackUrl;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Inject
    public PaymentProcessorClient(
            PaymentRepository paymentRepository,
            @Named("paymentProcessorExecutor") ExecutorService executorService,
            Logger log,
            ObjectMapper objectMapper) {

        this.repository = paymentRepository;
        this.log = log;
        this.defaultUrl = System.getenv("payment-processor-default-url");
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = System.getenv("payment-processor-fallback-url");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.executorService = executorService;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(1000))
                .build();

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
    }

    public void processPayment(PaymentRequest paymentRequest) {
        processPaymentWithRetry(paymentRequest, 0);
    }

    private void processPaymentWithRetry(PaymentRequest paymentRequest, int retryCount) {
        if (retryCount >= 3) {
            log.error("Max retries reached for payment {}", paymentRequest.correlationId);
            return;
        }

        try {
            postToDefault(paymentRequest);
            repository.save(paymentRequest);
        } catch (HttpClientException e) {
            log.error("Error processing payment default {} - retrying...", paymentRequest.correlationId, e);
            try {
                postToFallback(paymentRequest);
                repository.save(paymentRequest);
            } catch (HttpClientException ex) {
                log.error("Error processing payment fallback {} - retrying...", paymentRequest.correlationId, e);
                executorService.submit(() -> processPaymentWithRetry(paymentRequest, retryCount + 1));
            }
        }
    }

    private void postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        doRequest(URI.create(defaultUrl), paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        doRequest(URI.create(fallbackUrl), paymentRequest);
    }

    private void doRequest(URI uri, Object body) throws HttpClientException {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(java.time.Duration.ofMillis(1500))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body)))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new HttpClientException(new Exception("Error processing payment - status code: " + response.statusCode()));
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpClientException(e);
        }
    }
}