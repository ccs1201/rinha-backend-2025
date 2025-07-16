package br.com.ccs.rinha.api.model.input;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PaymentRequest {
    public UUID correlationId;
    public BigDecimal amount;
    public OffsetDateTime requestedAt;
    public boolean isDefault;
    public String json;

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public String getJson() {
        if (json == null) {
            toJson();
        }
        return json;
    }

    public static PaymentRequest of(byte[] data) {
        PaymentRequest request = new PaymentRequest();

        request.correlationId =
                UUID.fromString(new String(data, 18, 36, StandardCharsets.UTF_8));

        request.amount = new BigDecimal(new String(data, 65, 4, StandardCharsets.UTF_8));
        request.requestedAt = OffsetDateTime.now();

        return request;
    }

    private void toJson() {
        json = new StringBuilder(128)
                .append("{")
                .append("\"correlationId\":\"").append(correlationId).append("\",")
                .append("\"amount\":").append(amount).append(",")
                .append("\"requestedAt\":\"").append(requestedAt).append("\"")
                .append("}")
                .toString();
    }
}