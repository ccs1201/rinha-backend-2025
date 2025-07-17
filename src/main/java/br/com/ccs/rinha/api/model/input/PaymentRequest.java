package br.com.ccs.rinha.api.model.input;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class PaymentRequest {
    public final UUID correlationId;
    public final BigDecimal amount = new BigDecimal("19.9");
    public final OffsetDateTime requestedAt;
    public boolean isDefault;
    public final String json;

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public PaymentRequest(UUID correlationId, OffsetDateTime requestedAt) {
        this.correlationId = correlationId;
        this.requestedAt = requestedAt;
        this.json = this.toJson();
    }

    public String getJson() {
        return json;
    }

    public static PaymentRequest of(byte[] data) {

        var correlationId =
                UUID.fromString(new String(data, 18, 36, StandardCharsets.UTF_8));

//        var amount = new BigDecimal(19.9);//new BigDecimal(new String(data, 65, 4, StandardCharsets.UTF_8));
//        request.requestedAt = OffsetDateTime.now();

        return new PaymentRequest(correlationId, OffsetDateTime.now());
    }

    private String toJson() {
        return new StringBuilder(128)
                .append("{")
                .append("\"correlationId\":\"").append(correlationId).append("\",")
                .append("\"amount\":").append(amount).append(",")
                .append("\"requestedAt\":\"").append(requestedAt).append("\"")
                .append("}")
                .toString();
    }
}