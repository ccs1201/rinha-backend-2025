package br.com.ccs.rinha.api.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentSummary(
        @JsonProperty("default") Summary _default, Summary fallback) {

    public record Summary(long totalRequests, BigDecimal totalAmount) {
    }

    public String toJson() {
        return new StringBuilder(128)
                .append("{\"default\":{")
                .append("\"totalRequests\":").append(_default.totalRequests)
                .append(",\"totalAmount\":").append(_default.totalAmount)
                .append("},")
                .append("\"fallback\":{")
                .append("\"totalRequests\":").append(fallback.totalRequests)
                .append(",\"totalAmount\":").append(fallback.totalAmount)
                .append("}}")
                .toString();
    }
}
