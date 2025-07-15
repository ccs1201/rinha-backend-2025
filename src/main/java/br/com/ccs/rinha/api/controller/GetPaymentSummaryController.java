package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;
import br.com.ccs.rinha.service.PaymentRepository;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;

@Endpoint("/payments-summary")
public class GetPaymentSummaryController {

    private final PaymentRepository repository;

    @Inject
    public GetPaymentSummaryController(PaymentRepository repository) {
        this.repository = repository;
    }

    @Endpoint.GET
    public PaymentRepository.PaymentSummary getPaymentsSummary() {
        var now = OffsetDateTime.now();
        return repository.getSummary(now.minusSeconds(10), now.minusNanos(1000));
    }
}
