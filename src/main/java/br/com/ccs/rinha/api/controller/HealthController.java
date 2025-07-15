package br.com.ccs.rinha.api.controller;

import br.com.ccs.boot.annotations.Endpoint;

@Endpoint("/check")
public class HealthController {

    @Endpoint.GET
    public String health() {
        return "OK";
    }
}