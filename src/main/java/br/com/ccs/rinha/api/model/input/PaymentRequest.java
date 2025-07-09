package br.com.ccs.rinha.api.model.input;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PaymentRequest {
    private UUID correlationId;
    private BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private OffsetDateTime processedAt;
    private boolean isDefault;

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

    public UUID correlationId() {
        return correlationId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public OffsetDateTime processedAt() {
        return processedAt;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}