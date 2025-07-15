package br.com.ccs.rinha.service;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

@Singleton
@Default
public class TesteServiceImpl implements TesteService{

    @Override
    public String test() {
        return "testado com sucesso";
    }
}
