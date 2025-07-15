package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;
import br.com.ccs.rinha.service.PaymentRepository;
import jakarta.inject.Inject;

@Endpoint("/purge-payments")
public class PostPurgeController {

    private final PaymentRepository repository;

    @Inject
    public PostPurgeController(PaymentRepository repository) {
        this.repository = repository;
    }

    @Endpoint.POST
    public void purge() {
        repository.purge();
    }
}
