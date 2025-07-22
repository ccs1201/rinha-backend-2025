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
    private final int repositoryDelay;

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.shouldShutdownImmediately = Boolean.parseBoolean(System.getenv("SHUTDOWN_IMMEDIATELY"));
        this.repositoryDelay = Integer.parseInt(System.getenv("REPOSITORY_DELAY"));
        log.info("ShutDown immediately: {}", shouldShutdownImmediately);
        log.info("Repository delay set to {}", repositoryDelay);
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
        var data = String.format("%s:%s:%s", request.correlationId, request.amount, request.isDefault);
        redisTemplate
                .opsForZSet()
                .add(PAYMENTS, data, request.requestedAt.toInstant().toEpochMilli());

    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        from = getFrom(from);
        to = getTo(to);

        var payments = redisTemplate.opsForZSet().rangeByScore(PAYMENTS, from.toInstant().toEpochMilli(), to.toInstant().toEpochMilli());

        return calculateSummary(payments);
    }

    private static OffsetDateTime getTo(OffsetDateTime to) {
        if (isNull(to)) {
            to = OffsetDateTime.now();
        }
        return to;
    }

    private static OffsetDateTime getFrom(OffsetDateTime from) {
        if (isNull(from)) {
            from = OffsetDateTime.now().minusMinutes(5);
        }
        return from;
    }

    private PaymentSummary calculateSummary(Set<String> payments) {
        if (payments == null || payments.isEmpty()) {
            return new PaymentSummary(new PaymentSummary.Summary(0, BigDecimal.ZERO), new PaymentSummary.Summary(0, BigDecimal.ZERO));
        }

        long defaultCount = 0;
        long fallbackCount = 0;
        BigDecimal defaultAmount = BigDecimal.ZERO;
        BigDecimal fallbackAmount = BigDecimal.ZERO;

        for (String payment : payments) {
            String[] parts = payment.split(":");
            BigDecimal amount = new BigDecimal(parts[1]);
            if ("true".equals(parts[2])) {
                defaultCount++;
                defaultAmount = defaultAmount.add(amount);
            } else {
                fallbackCount++;
                fallbackAmount = fallbackAmount.add(amount);
            }
        }

        return new PaymentSummary(
                new PaymentSummary.Summary(defaultCount, defaultAmount),
                new PaymentSummary.Summary(fallbackCount, fallbackAmount));
    }

    public void purge() {
        redisTemplate.delete(PAYMENTS);
    }

}