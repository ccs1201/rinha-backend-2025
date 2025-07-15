package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;
import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Endpoint("/payments")
public class PostPaymentController {

    private final Logger log;

    private final PaymentProcessorClient client;
    private final ExecutorService executor;

    @Inject
    public PostPaymentController(PaymentProcessorClient client, @Named("paymentProcessorExecutor") ExecutorService executor, Logger log) {
        this.client = client;
        this.executor = executor;
        this.log = log;
    }

    @Endpoint.POST
    public void createPayment(PaymentRequest paymentRequest) {
        CompletableFuture.runAsync(() -> {
            paymentRequest.requestedAt = OffsetDateTime.now();
            client.processPayment(paymentRequest);
        });
    }

    @PreDestroy
    public void shutdown() {
        if (executor.isShutdown()) return;
        executor.shutdownNow();
    }

}
