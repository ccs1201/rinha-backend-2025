package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.api.model.output.PaymentSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import static java.util.Objects.isNull;

@Repository
public class RedisPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisPaymentRepository.class);

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PAYMENTS = "payments";
    private final boolean shouldShutdownImmediately;

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.shouldShutdownImmediately = Boolean.parseBoolean(System.getenv("SHUTDOWN_IMMEDIATELY"));
        log.info("ShutDown immediately: {}", shouldShutdownImmediately);
    }

    @PostConstruct
    public void warmup() {
        try {
            // Aquece conexão Redis
            redisTemplate.opsForValue().get("warmup");
            redisTemplate.opsForZSet().count("warmup", 0, 1);
        } catch (Exception e) {
            // Ignora erros de warmup
        }
    }


    public void store(PaymentRequest request) {
        String data = request.correlationId + ":" + request.amount.multiply(BigDecimal.valueOf(100)).longValue() + ":" + request.isDefault;

        redisTemplate
                .opsForZSet()
                .add(PAYMENTS, data, request.requestedAt.toInstant().toEpochMilli());

    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (isNull(from)) {
            from = OffsetDateTime.now().minusMinutes(5);
        }

        if (isNull(to)) {
            to = OffsetDateTime.now();
        }
        var payments = redisTemplate.opsForZSet().rangeByScore(PAYMENTS, from.toInstant().toEpochMilli(), to.toInstant().toEpochMilli());

        return calculateSummary(payments);
    }

    private PaymentSummary calculateSummary(Set<String> payments) {
        if (payments == null || payments.isEmpty()) {
            return new PaymentSummary(new PaymentSummary.Summary(0, BigDecimal.ZERO), new PaymentSummary.Summary(0, BigDecimal.ZERO));
        }

        long defaultCount = 0;
        long fallbackCount = 0;
        long defaultAmount = 0L;
        long fallbackAmount = 0L;

        for (String payment : payments) {
            String[] parts = payment.split(":");
            long amount = Long.parseLong(parts[1]); // armazenar centavos
            boolean isDefault = Boolean.parseBoolean(parts[2]);

            if (isDefault) {
                defaultCount++;
                defaultAmount += amount;
            } else {
                fallbackCount++;
                fallbackAmount += amount;
            }
        }

        return new PaymentSummary(
                new PaymentSummary.Summary(defaultCount, new BigDecimal(defaultAmount).movePointLeft(2)),
                new PaymentSummary.Summary(fallbackCount, new BigDecimal(fallbackAmount).movePointLeft(2)));
    }

    public void purge() {
        redisTemplate.delete(PAYMENTS);
    }

}