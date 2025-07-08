package br.com.ccs.rinha.api.controller;

import br.com.ccs.rinha.service.PaymentRouter;
import br.com.ccs.rinha.service.PaymentStorage;
import br.com.ccs.rinha.service.PaymentSummaryAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private static final ResponseEntity<Void> response = ResponseEntity.accepted().build();
    private final PaymentRouter router;
    private final PaymentStorage storage;
    private final PaymentSummaryAggregator aggregator;

    public PaymentController(PaymentRouter router, PaymentStorage storage, PaymentSummaryAggregator aggregator) {
        this.router = router;
        this.storage = storage;
        this.aggregator = aggregator;
    }

    @PostMapping("payments")
    public ResponseEntity<Void> createPayment(@RequestBody PaymentRequest paymentRequest) {
        router.processPayment(paymentRequest.correlationId(), paymentRequest.amount());
        return response;
    }

    @GetMapping("payments-summary")
    public PaymentStorage.PaymentSummary getPaymentsSummary(@RequestParam(required = false) OffsetDateTime from,
                                                            @RequestParam(required = false) OffsetDateTime to) {
        log.info("Getting aggregated payments summary from {} to {}", from, to);
        return aggregator.getAggregatedSummary(from, to);
    }

    @GetMapping("local-summary")
    public PaymentStorage.PaymentSummary getLocalSummary(@RequestParam(required = false) OffsetDateTime from,
                                                        @RequestParam(required = false) OffsetDateTime to) {
        return storage.getSummary(from, to);
    }

    @PostMapping("/purge-payments")
    public ResponseEntity<Void> purgePayments() {
        storage.purge();
        return ResponseEntity.ok().build();
    }


    public record PaymentRequest(UUID correlationId, BigDecimal amount) {
    }
}
