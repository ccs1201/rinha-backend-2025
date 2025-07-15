package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.exception.HttpClientException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

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

    @Inject
    public PaymentProcessorClient(
            PaymentRepository paymentRepository,
            @Named("paymentProcessorExecutor") ExecutorService executorService, Logger log) {

        this.repository = paymentRepository;
        this.log = log;
        this.defaultUrl = System.getenv("payment-processor-default-url");
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = System.getenv("payment-processor-fallback-url");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.executorService = executorService;

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
        } catch (HttpClientException e) {
            try {
                postToFallback(paymentRequest);
            } catch (HttpClientException ex) {
                executorService.submit(() -> processPaymentWithRetry(paymentRequest, retryCount + 1));
            }
        }
    }

    private void postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        doRequestAsync(URI.create(defaultUrl), paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        doRequestAsync(URI.create(fallbackUrl), paymentRequest);
        repository.save(paymentRequest);
    }

    private void doRequestAsync(URI uri, Object body) throws HttpClientException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(String.valueOf(body)))
                    .timeout(java.time.Duration.ofMillis(1500))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new HttpClientException(e);
        }
    }
}