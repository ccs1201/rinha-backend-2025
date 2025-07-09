package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class PaymentRouter {

    private final PaymentProcessorClient client;
    private final ConcurrentLinkedDeque<PaymentRequest> queue = new ConcurrentLinkedDeque<>();

    public PaymentRouter(PaymentProcessorClient client) {
        this.client = client;
    }

    public void processPayment(PaymentRequest paymentRequest) {
        queue.addLast(paymentRequest);
        processQueue();
    }

    private void processQueue() {
        PaymentRequest request;
        while ((request = queue.pollFirst()) != null) {
            client.processPayment(request);
        }
    }
}