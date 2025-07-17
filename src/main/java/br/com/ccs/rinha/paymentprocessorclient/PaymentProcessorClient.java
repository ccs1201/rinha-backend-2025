package br.com.ccs.rinha.paymentprocessorclient;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.PaymentProcessorClientVars;
import br.com.ccs.rinha.exception.HttpClientException;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class PaymentProcessorClient {

    private static final String contentType = "Content-Type";
    private static final String contentTypeValue = "application/json";
    public final AtomicInteger failedRetryAttempsts = new AtomicInteger(0);

    private final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);
    private final JdbcPaymentRepository repository;
    private final HttpClient httpClient;
    private final PaymentProcessorClientVars vars;
    private static final PaymentProcessorClient instance;


    static {
        instance = new PaymentProcessorClient();
    }

    public static PaymentProcessorClient getInstance() {
        return instance;
    }


    private PaymentProcessorClient() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                200,
                1000,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000, true),
                Thread.ofVirtual().factory(),
                new ThreadPoolExecutor.DiscardPolicy());

        this.vars = PaymentProcessorClientVars.getInstance();
        this.repository = JdbcPaymentRepository.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(vars.requestTimout))
                .executor(executor)
                .build();


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
        return doRequest(vars.defaultURI, paymentRequest);
    }

    private boolean postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        return doRequest(vars.fallbackURI, paymentRequest);
    }

    private Boolean doRequest(URI uri, PaymentRequest paymentRequest) {
        return httpClient.sendAsync(buildRequest(uri, paymentRequest), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> {
                    failedRetryAttempsts.incrementAndGet();
                    return false;
                }).join();
    }


    private HttpRequest buildRequest(URI uri, PaymentRequest paymentRequest) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header(contentType, contentTypeValue)
                .version(HttpClient.Version.HTTP_2)
                .timeout(java.time.Duration.ofMillis(vars.requestTimout))
                .POST(HttpRequest.BodyPublishers.ofByteArray(paymentRequest.jsonBytes))
                .build();
    }
}