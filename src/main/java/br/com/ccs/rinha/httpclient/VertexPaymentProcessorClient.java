package br.com.ccs.rinha.httpclient;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class VertexPaymentProcessorClient extends PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(VertexPaymentProcessorClient.class);
    private final ArrayBlockingQueue<PaymentRequest> queue;
    private static final Vertx vertx = Vertx.vertx();
    private static final WebClient webClient = WebClient.create(vertx);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static VertexPaymentProcessorClient instance;
    private final AtomicInteger failedAttemptsDefault = new AtomicInteger(0);
    private final AtomicInteger failedAttemptsFallback = new AtomicInteger(0);
    private final AtomicInteger failedStatuscodeAttempts = new AtomicInteger(0);


    public static VertexPaymentProcessorClient getInstance() {
        if (instance == null) {
            instance = new VertexPaymentProcessorClient();
        }
        return instance;
    }

    public VertexPaymentProcessorClient() {
        super();
        var queueSize = Integer.parseInt(System.getenv("thread-queue-size").trim());
        this.queue = new ArrayBlockingQueue<>(queueSize, false);
        startProcessQueue();
    }

    @Override
    public void processPayment(PaymentRequest paymentRequest) {
        var accepted = queue.offer(paymentRequest);
        if (!accepted) {
            log.error("Payment rejected by queue");
        }
    }

    public void purgeQueue() {
        queue.clear();
        failedStatuscodeAttempts.set(0);
        failedAttemptsFallback.set(0);
        failedRetryAttempsts.set(0);
        failedAttemptsDefault.set(0);
    }

    private void startProcessQueue() {
        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PaymentRequest payment = queue.take();
                    processPaymentWithRetry(payment, 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, ExecutorConfig.getExecutor());
    }

    private void processPaymentWithRetry(PaymentRequest paymentRequest, int retryCount) {
        retryCount++;
        if (retryCount > 3) {
            log.error("Max retries reached for payment {}", paymentRequest.correlationId);
            log.info("Failed payments count:{}", failedRetryAttempsts.incrementAndGet());
            return;
        }
        doRequest(paymentRequest, retryCount);
    }

    private void doRequest(PaymentRequest paymentRequest, int retryCount) {
        webClient
                .post(defaultURI.getPort(), defaultURI.getHost(), defaultURI.getPath())
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(requestTimout)
                .sendBuffer(Buffer.buffer(paymentRequest.getJson()))
                .onSuccess(resp -> {
                    if (resp.statusCode() != 200) {
                        fallback(paymentRequest, retryCount);
                        return;
                    }
                    paymentRequest.setDefaultTrue();
                    repository.save(paymentRequest);

                })
                .onFailure(err -> {
//                    log.error("Error on default processor", err);
                    if (err instanceof NoStackTraceTimeoutException) {
//                        log.info("Failed attempts on timeout default: {}", failedAttemptsDefault.incrementAndGet());
                        fallback(paymentRequest, retryCount);
                        return;
                    }
                    log.error("Unpredictable error", err);
                });
    }

    private void fallback(PaymentRequest paymentRequest, int retryCount) {
        webClient
                .post(fallbackURI.getPort(), fallbackURI.getHost(), fallbackURI.getPath())
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(requestTimout * 2)
                .sendBuffer(Buffer.buffer(paymentRequest.getJson()))
                .onSuccess(resp -> {
                    if (resp.statusCode() != 200) {
                        log.info("All processors fail retry");
                        log.info("Failed attempts on status code total: {}", failedStatuscodeAttempts.incrementAndGet());
                        processPaymentWithRetry(paymentRequest, retryCount);
                        return;
                    }
                    repository.save(paymentRequest);
                })
                .onFailure(err -> {
//                    log.error("Error on fallback processor", err);
                    if (err instanceof NoStackTraceTimeoutException) {
//                        log.info("Failed attempts on timeout fallback: {}", failedAttemptsFallback.incrementAndGet());
                        processPaymentWithRetry(paymentRequest, retryCount);
                        return;
                    }
                    log.error("Unpredictable error", err);
                });
    }
}
