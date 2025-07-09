package br.com.ccs.rinha.api.model.input;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PaymentRequest {
    public UUID correlationId;
    public BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    public OffsetDateTime processedAt;
    public boolean isDefault;

    public PaymentRequest() {
    }

    public PaymentRequest(UUID correlationId, BigDecimal amount) {
        this(correlationId, amount, OffsetDateTime.now(), true);
    }

    public PaymentRequest(UUID correlationId, BigDecimal amount, OffsetDateTime processedAt, boolean isDefault) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.processedAt = processedAt;
        this.isDefault = isDefault;
    }

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }
}