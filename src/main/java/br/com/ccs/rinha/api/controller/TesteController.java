package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;
import br.com.ccs.rinha.service.TesteService;
import jakarta.inject.Inject;

@Endpoint("/teste")
public class TesteController {

    private final TesteService testeService;

    @Inject
    public TesteController(TesteService testeService) {
        this.testeService = testeService;
    }

    @Endpoint.GET
    public String teste() {
        return testeService.test();
    }

}
