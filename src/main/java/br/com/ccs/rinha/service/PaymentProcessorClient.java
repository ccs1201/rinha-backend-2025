package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.config.ObjectMapperFactory;
import br.com.ccs.rinha.exception.HttpClientException;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;

public class PaymentProcessorClient {

    private static final String contentType = "Content-Type";
    private static final String contentTypeValue = "application/json";

    private final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);
    private final PaymentRepository repository;
    private String defaultUrl;
    private String fallbackUrl;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private static final PaymentProcessorClient instance;

    static {
        instance = new PaymentProcessorClient();
    }

    public static PaymentProcessorClient getInstance() {
        return instance;
    }


    private PaymentProcessorClient() {
        this.defaultUrl = System.getenv("payment-processor-default-url").trim();
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = System.getenv("payment-processor-fallback-url").trim();
        this.fallbackUrl = fallbackUrl.concat("/payments");

        this.repository = JdbcPaymentRepository.getInstance();
        this.executorService = ExecutorConfig.getExecutor();
        this.objectMapper = ObjectMapperFactory.getInstance();

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(1000))
                .build();

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
    }

    public void processPayment(PaymentRequest paymentRequest) {
        paymentRequest.requestedAt = OffsetDateTime.now();
        log.info("Processing payment {}", paymentRequest.getJson());
        processPaymentWithRetry(paymentRequest, 0);
    }

    private void processPaymentWithRetry(PaymentRequest paymentRequest, int retryCount) {
        if (retryCount >= 3) {
//            log.error("Max retries reached for payment {}", paymentRequest.correlationId);
            return;
        }

        if (postToDefault(paymentRequest)) {
            repository.save(paymentRequest);
            return;
        }
//        log.error("Error processing payment default {} - retrying...", paymentRequest.correlationId);

        if (postToFallback(paymentRequest)) {
            repository.save(paymentRequest);
            return;
        }
//        log.error("Error processing payment fallback {} - retrying...", paymentRequest.correlationId);

        executorService.submit(() -> processPaymentWithRetry(paymentRequest, retryCount + 1));
    }


    private boolean postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        return doRequest(URI.create(defaultUrl), paymentRequest);
    }

    private boolean postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        return doRequest(URI.create(fallbackUrl), paymentRequest);
    }

    private Boolean doRequest(URI uri, PaymentRequest body) throws HttpClientException {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header(contentType, contentTypeValue)
//                    .header("Accept", "application/json")
                    .version(HttpClient.Version.HTTP_2)
                    .timeout(java.time.Duration.ofMillis(1500))
                    .POST(HttpRequest.BodyPublishers.ofString(body.getJson()))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.info("Error processing payment status code: {}", response.statusCode());
                return Boolean.FALSE;
            }
            log.info("Payment processed: {}", response.body());
            return Boolean.TRUE;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpClientException(e);
        }
    }
}