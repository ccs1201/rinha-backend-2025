package br.com.ccs.rinha.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentRouter {

    private final PaymentProcessorClient client;

    public PaymentRouter(PaymentProcessorClient client) {
        this.client = client;
    }

    public void processPayment(UUID correlationId, BigDecimal amount) {
        client.processPayment(correlationId, amount);
    }
}