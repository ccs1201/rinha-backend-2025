package br.com.ccs.rinha.httpclient;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;

public class VertexPaymentProcessor extends PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(VertexPaymentProcessor.class);

    private final ArrayBlockingQueue<PaymentRequest> queue;

    private static final Vertx vertx = Vertx.vertx();
    private static final WebClient webClient = WebClient.create(vertx);
    private static final String CONTENT_TYPE = "Content-Type";

    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static VertexPaymentProcessor instance;


    public static VertexPaymentProcessor getInstance() {
        if (instance == null) {
            instance = new VertexPaymentProcessor();
        }
        return instance;
    }

    public VertexPaymentProcessor() {
        super();
        var queueSize = Integer.parseInt(System.getenv("thread-queue-size"));
        this.queue = new ArrayBlockingQueue<>(queueSize, true);
        startProcessQueue();
    }

    @Override
    public void processPayment(PaymentRequest paymentRequest) {
        paymentRequest.isDefault = true;
        queue.offer(paymentRequest);
    }

    private void startProcessQueue() {
        Thread.ofVirtual()
                .name("VertexPaymentProcessor-queue-thread")
                .unstarted(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            PaymentRequest req = queue.take();
                            doRequest(req, defaultURI);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }).start();
    }

    private void doRequest(PaymentRequest paymentRequest, URI uri) {
        webClient
                .post(uri.getPort(), uri.getHost(), uri.getPath())
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(1500)
                .sendBuffer(Buffer.buffer(paymentRequest.getJson()))
                .onSuccess(resp -> {
                    if (resp.statusCode() == 200) {
                        repository.save(paymentRequest);
                    } else if (uri.equals(defaultURI)) {
                        paymentRequest.isDefault = false;
                        doRequest(paymentRequest, fallbackURI);
                    } else {
                        processPayment(paymentRequest);
                    }
                })
                .onFailure(err -> {
                    log.error(err.getMessage(), err);
                    log.error("Failed attempts: {}", failedPaymentsCounter.incrementAndGet());
                    processPayment(paymentRequest);
                });
    }
}
