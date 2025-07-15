package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;
import br.com.ccs.rinha.service.PaymentRepository;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;

@Endpoint("/summary")
public class GetFullSummaryController {

    private final PaymentRepository repository;

    @Inject
    public GetFullSummaryController(PaymentRepository repository) {
        this.repository = repository;
    }

    @Endpoint.GET
    public PaymentRepository.PaymentSummary getSummary() {
        return repository.getSummary(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));
    }
}
