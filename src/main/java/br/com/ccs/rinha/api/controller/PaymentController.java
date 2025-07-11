package br.com.ccs.rinha.api.controller;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import br.com.ccs.rinha.service.PaymentRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentProcessorClient client;
    private final PaymentRepository repository;
    private final ExecutorService executor;

    public PaymentController(PaymentProcessorClient client, PaymentRepository repository, ExecutorService executor) {
        this.client = client;
        this.repository = repository;
        this.executor = executor;
    }

    @PostMapping("/payments")
    public void createPayment(@RequestBody PaymentRequest paymentRequest) {
        CompletableFuture.runAsync(() -> {
            paymentRequest.requestedAt = OffsetDateTime.now();
            client.processPayment(paymentRequest);
        }, executor);
    }

    @GetMapping("/payments-summary")
    public PaymentRepository.PaymentSummary getPaymentsSummary(@RequestParam OffsetDateTime from,
                                                               @RequestParam OffsetDateTime to) {
        log.info("Starting payments summary from {} to {}", from, to);
        long start = System.currentTimeMillis();
        var summary = repository.getSummary(from, to);
        log.info("Got payments summary from {} to {} in {}ms", from, to, System.currentTimeMillis() - start);
        return summary;
    }

    @PostMapping("/purge-payments")
    public ResponseEntity<Void> purgePayments() {
        log.info("Purging payments");
        repository.purge();
        log.info("Payments purged");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-status")
    @ResponseStatus(HttpStatus.OK)
    public void check() {
        log.info("OK");
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

}
