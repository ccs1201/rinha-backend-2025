package br.com.ccs.rinha.api.model.input;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PaymentRequest {
    public UUID correlationId;
    public BigDecimal amount;
    public OffsetDateTime requestedAt;
    public boolean isDefault;
    private static final String json_pattern = """
            {
            "correlationId": "%s",
            "amount": %s,
            "requestedAt": "%s"
            }""";

    private String json;

    public PaymentRequest() {
    }

    public PaymentRequest(UUID correlationId, BigDecimal amount) {
        this(correlationId, amount, null, true);
    }

    public PaymentRequest(UUID correlationId, BigDecimal amount, OffsetDateTime requestedAt, boolean isDefault) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.requestedAt = requestedAt;
        this.isDefault = isDefault;
    }

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public String getJson() {
        if (json == null) {
            var sb = new StringBuilder(128);
            sb.append("{")
                    .append("\"correlationId\":\"").append(correlationId).append("\",")
                    .append("\"amount\":").append(amount).append(",")
                    .append("\"requestedAt\":\"").append(requestedAt).append("\"")
                    .append("}");
            json = sb.toString();
        }
        return json;
    }
}