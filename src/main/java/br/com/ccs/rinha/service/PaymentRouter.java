package br.com.ccs.rinha.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentRouter {

    private static final Logger log = LoggerFactory.getLogger(PaymentRouter.class);
    private final PaymentProcessorClient client;
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);

    public PaymentRouter(PaymentProcessorClient client) {
        this.client = client;
    }

    public void processPayment(UUID correlationId, BigDecimal amount) {
        requestCount.incrementAndGet();
        totalAmount.updateAndGet(current -> current.add(amount));
        client.processPayment(correlationId, amount);
    }

    @PreDestroy
    public void printSummary() {
        log.info("Total requests: {}", requestCount.get());
        log.info("Total amount: {} ", totalAmount.get());
    }

}