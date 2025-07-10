package br.com.ccs.rinha.api.controller;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import br.com.ccs.rinha.service.PaymentStorage;
import br.com.ccs.rinha.service.PaymentSummaryAggregator;
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
import java.util.concurrent.Executors;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private static final ResponseEntity<Void> response = ResponseEntity.accepted().build();
    private final PaymentProcessorClient client;
    private final PaymentStorage storage;
    private final PaymentSummaryAggregator aggregator;

    public PaymentController(PaymentProcessorClient client, PaymentStorage storage, PaymentSummaryAggregator aggregator) {
        this.client = client;
        this.storage = storage;
        this.aggregator = aggregator;
    }

    @PostMapping("/payments")
    public ResponseEntity<Void> createPayment(@RequestBody PaymentRequest paymentRequest) {
        CompletableFuture.runAsync(() -> {
            paymentRequest.requestedAt = OffsetDateTime.now();
            client.processPayment(paymentRequest);
        }, Executors.newVirtualThreadPerTaskExecutor());
        return response;
    }

    @GetMapping("/payments-summary")
    public PaymentStorage.PaymentSummary getPaymentsSummary(@RequestParam(required = false) OffsetDateTime from,
                                                            @RequestParam(required = false) OffsetDateTime to) {
        long start = System.currentTimeMillis();
        var summary = aggregator.getAggregatedSummary(from, to);
        log.info("Got aggregated payments summary from {} to {} in {}ms", from, to, System.currentTimeMillis() - start);
        return summary;
    }

    @GetMapping("/local-summary")
    public PaymentStorage.PaymentSummary getLocalSummary(@RequestParam(required = false) OffsetDateTime from,
                                                         @RequestParam(required = false) OffsetDateTime to) {
        return storage.getSummary(from, to);
    }

    @PostMapping("/purge-payments")
    public ResponseEntity<Void> purgePayments() {
        log.info("Purging payments");
        storage.purge();
        log.info("Payments purged");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-status")
    @ResponseStatus(HttpStatus.OK)
    public void check() {
        log.info("OK");
    }

}
