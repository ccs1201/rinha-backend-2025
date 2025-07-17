package br.com.ccs.rinha.paymentprocessorclient;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.config.PaymentProcessorClientVars;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class VertexPaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(VertexPaymentProcessorClient.class);
    private final ArrayBlockingQueue<PaymentRequest> queue;
    private static final Vertx vertx = Vertx.vertx();
    private static final WebClient webClient = WebClient.create(vertx);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final AtomicInteger failedAttemptsDefault = new AtomicInteger(0);
    private static final AtomicInteger failedAttemptsFallback = new AtomicInteger(0);
    private static final AtomicInteger failedStatuscodeAttempts = new AtomicInteger(0);
    private static final AtomicInteger failedRetryAttempsts = new AtomicInteger(0);
    private final PaymentProcessorClientVars vars;
    private final JdbcPaymentRepository repository;

    private static final VertexPaymentProcessorClient instance = new VertexPaymentProcessorClient();

    private VertexPaymentProcessorClient() {
        super();
        var queueSize = Integer.parseInt(System.getenv("thread-queue-size").trim());
        this.queue = new ArrayBlockingQueue<>(queueSize, false);
        vars = PaymentProcessorClientVars.getInstance();
        this.repository = JdbcPaymentRepository.getInstance();
//        startProcessQueue();
    }

    public static VertexPaymentProcessorClient getInstance() {
        return instance;
    }

    public void processPayment(PaymentRequest paymentRequest) {
        processPaymentWithRetry(paymentRequest, 0);
//        var accepted = queue.offer(paymentRequest);
//        if (!accepted) {
//            log.error("Payment rejected by queue");
//        }
    }

    public void purge() {
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
            processPayment(paymentRequest);
            return;
        }
        doRequest(paymentRequest, retryCount);
    }

    private void doRequest(PaymentRequest paymentRequest, int retryCount) {
        webClient
                .post(vars.defaultURI.getPort(), vars.defaultURI.getHost(), vars.defaultURI.getPath())
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(vars.requestTimout)
                .sendBuffer(Buffer.buffer(paymentRequest.jsonBytes))
                .onSuccess(resp -> {
                    onSuccess(paymentRequest, retryCount, resp);
                })
                .onFailure(err -> {
                    onSuccessFailure(paymentRequest, retryCount, err);
                });
    }

    private void fallback(PaymentRequest paymentRequest, int retryCount) {
        webClient
                .post(vars.fallbackURI.getPort(), vars.fallbackURI.getHost(), vars.fallbackURI.getPath())
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(vars.requestTimout)
                .sendBuffer(Buffer.buffer(paymentRequest.jsonBytes))
                .onSuccess(resp -> {
                    paymentRequest.setDefaultFalse();
                    onSuccess(paymentRequest, retryCount, resp);
                })
                .onFailure(err -> {
                    onFallbackFailure(paymentRequest, retryCount, err);

                });
    }

    private void onSuccess(PaymentRequest paymentRequest, int retryCount, HttpResponse<Buffer> resp) {
        if (resp.statusCode() != 200) {
            fallback(paymentRequest, retryCount);
            return;
        }
        paymentRequest.setDefaultTrue();
        repository.save(paymentRequest);
    }

    private void onSuccessFailure(PaymentRequest paymentRequest, int retryCount, Throwable err) {
        log.error("Error on default processor", err);
        if (err instanceof NoStackTraceTimeoutException) {
//                        log.info("Failed attempts on timeout default: {}", failedAttemptsDefault.incrementAndGet());
            fallback(paymentRequest, retryCount);
            return;
        }
        logError(err);
    }

    private void onFallbackFailure(PaymentRequest paymentRequest, int retryCount, Throwable err) {
        //                    log.error("Error on fallback processor", err);
        if (err instanceof NoStackTraceTimeoutException) {
//                        log.info("Failed attempts on timeout fallback: {}", failedAttemptsFallback.incrementAndGet());
            processPaymentWithRetry(paymentRequest, retryCount);
            return;
        }
        logError(err);
    }

    private static void logError(Throwable err) {
        log.error("Unpredictable error", err);
    }
}
