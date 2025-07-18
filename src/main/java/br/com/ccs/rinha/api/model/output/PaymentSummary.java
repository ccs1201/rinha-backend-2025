package br.com.ccs.rinha.api.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentSummary(
        @JsonProperty("default") Summary _default, Summary fallback) {

    public record Summary(long totalRequests, BigDecimal totalAmount) {
    }
}
