package br.com.ccs.rinha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentRouter {

    private static final Logger log = LoggerFactory.getLogger(PaymentRouter.class);

    private final PaymentProcessorClient client;
    private final IntegrationHealthCheck healthCheck;
    private final PaymentStorage storage;

    public PaymentRouter(PaymentProcessorClient client, IntegrationHealthCheck healthCheck, PaymentStorage storage) {
        this.client = client;
        this.healthCheck = healthCheck;
        this.storage = storage;
    }

    public void processPayment(UUID correlationId, BigDecimal amount) {
        boolean useDefault = shouldUseDefault();
        var now = OffsetDateTime.now();

        client.processPayment(correlationId, amount, useDefault)
                .exceptionally(ex -> {
                    log.error("Error processing payment: {}", ex.getMessage());
                    if (useDefault) {
                        // Retry com fallback se default falhar
                        client.processPayment(correlationId, amount, false)
                                .thenRun(() -> storage.store(correlationId, amount, false, now));
                    }
                    return null;
                })
                .thenRun(() -> storage.store(correlationId, amount, useDefault, now));
    }

    private boolean shouldUseDefault() {
        return !healthCheck.checkDefault().failing();
    }
}