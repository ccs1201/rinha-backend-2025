package br.com.ccs.rinha.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TesteServiceImpl implements TesteService{

    @Override
    public String test() {
        return "testado com sucesso";
    }
}
