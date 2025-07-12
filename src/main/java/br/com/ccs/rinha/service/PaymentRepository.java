package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface PaymentRepository {
    
    void save(PaymentRequest request);
    
    PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to);
    
    void purge();
    
    record PaymentSummary(
            @JsonProperty("default") Summary _default, Summary fallback) {
    }

    record Summary(long totalRequests, BigDecimal totalAmount) {
    }
}