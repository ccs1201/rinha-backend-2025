package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.exception.HttpClientException;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentProcessorClient {

    private static final String contentType = "Content-Type";
    private static final String contentTypeValue = "application/json";
    public final AtomicInteger failedRetryAttempsts = new AtomicInteger(0);

    private final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);
    protected final PaymentRepository repository;
    private String defaultUrl;
    private String fallbackUrl;
    private final HttpClient httpClient;
    private static final PaymentProcessorClient instance;
    protected final URI defaultURI;
    protected final URI fallbackURI;
    protected final int requestTimout;

    static {
        instance = new PaymentProcessorClient();
    }

    public static PaymentProcessorClient getInstance() {
        return instance;
    }


    protected PaymentProcessorClient() {
        this.defaultUrl = System.getenv("payment-processor-default-url").trim();
        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = System.getenv("payment-processor-fallback-url").trim();
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.defaultURI = URI.create(defaultUrl);
        this.fallbackURI = URI.create(fallbackUrl);
        this.requestTimout = Integer.parseInt(System.getenv("client-processor-timeout").trim());

        this.repository = JdbcPaymentRepository.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(requestTimout))
                .build();

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
        log.info("Request timeout: {}", this.requestTimout);
    }

    public void purge() {
        // do nothing
    }

    public void processPayment(PaymentRequest paymentRequest) {
        processPaymentWithRetry(paymentRequest, 0);
    }

    private void processPaymentWithRetry(PaymentRequest paymentRequest, int retryCount) {
        if (retryCount > 3) {
            log.error("Max retries reached for payment {}", paymentRequest.correlationId);
            log.info("Failed payments counter:{}", failedRetryAttempsts.incrementAndGet());
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

//        executorService.submit(() -> {
//            log.info("Send payment back to queue {}", paymentRequest.correlationId);
//            processPaymentWithRetry(paymentRequest, retryCount + 1);
//        });
    }

    private boolean postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        return doRequest(defaultURI, paymentRequest);
    }

    private boolean postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        return doRequest(fallbackURI, paymentRequest);
    }

    private Boolean doRequest(URI uri, PaymentRequest paymentRequest) throws HttpClientException {
//        log.info("Sending payment to processor {}", paymentRequest.correlationId);
        try {
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header(contentType, contentTypeValue)
//                    .header("Accept", "application/json")
                    .version(HttpClient.Version.HTTP_2)
                    .timeout(java.time.Duration.ofMillis(requestTimout))
                    .POST(HttpRequest.BodyPublishers.ofString(paymentRequest.getJson()))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
//                log.info("Error processing payment status code: {}", response.statusCode());
                return Boolean.FALSE;
            }
//            log.info("Payment processed: {}", response.body());
            return Boolean.TRUE;
        } catch (IOException | InterruptedException e) {
            log.info("Payment process timeout {}", paymentRequest.correlationId);
            log.info("Failed payment counter: {}", failedRetryAttempsts.incrementAndGet());
            Thread.currentThread().interrupt();
            return Boolean.FALSE;
        }
    }
}