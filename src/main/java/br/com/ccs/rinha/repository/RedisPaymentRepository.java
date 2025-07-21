package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.api.model.output.PaymentSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.Objects.isNull;

@Service
public class RedisPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisPaymentRepository.class);

    private final RedisTemplate<String, String> redisTemplate;
    private static final String DEFAULT_COUNT = "default:count";
    private static final String FALLBACK_COUNT = "fallback:count";
    private static final String DEFAULT_AMOUNT = "default:amount";
    private static final String FALLBACK_AMOUNT = "fallback:amount";
    private static final String PAYMENTS = "payments";
    private final boolean shouldShutdownImmediately;

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate, ExecutorService executorService,
                                  @Value("${SHUTDOWN_IMMEDIATELY}") boolean shutdownImmediately) {
        this.redisTemplate = redisTemplate;
        this.shouldShutdownImmediately = shutdownImmediately;
        log.info("SHUTDOWN_IMMEDIATELY: {}", shouldShutdownImmediately);
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
                .add(PAYMENTS, data, request.requestedAt.toEpochSecond());
    }


    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (isNull(from)) {
            from = OffsetDateTime.MIN;
        }
        if (isNull(to)) {
            to = OffsetDateTime.now();
        }

        var payments = redisTemplate.opsForZSet().rangeByScore(PAYMENTS, from.toEpochSecond(), to.toEpochSecond());

        return calculateSummary(payments);
    }

    private PaymentSummary getFullSummary() {

        String defaultCountStr = redisTemplate.opsForValue().get(DEFAULT_COUNT);
        String fallbackCountStr = redisTemplate.opsForValue().get(FALLBACK_COUNT);
        String defaultAmountStr = redisTemplate.opsForValue().get(DEFAULT_AMOUNT);
        String fallbackAmountStr = redisTemplate.opsForValue().get(FALLBACK_AMOUNT);

        long defCount = defaultCountStr != null ? Long.parseLong(defaultCountStr) : 0;
        long fallCount = fallbackCountStr != null ? Long.parseLong(fallbackCountStr) : 0;
        BigDecimal defAmount = defaultAmountStr != null ? new BigDecimal(defaultAmountStr) : BigDecimal.ZERO;
        BigDecimal fallAmount = fallbackAmountStr != null ? new BigDecimal(fallbackAmountStr) : BigDecimal.ZERO;

        return new PaymentSummary(
                new PaymentSummary.Summary(defCount, defAmount.setScale(2, RoundingMode.HALF_UP)),
                new PaymentSummary.Summary(fallCount, fallAmount.setScale(2, RoundingMode.HALF_UP)));
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
        redisTemplate.delete(DEFAULT_COUNT);
        redisTemplate.delete(FALLBACK_COUNT);
        redisTemplate.delete(DEFAULT_AMOUNT);
        redisTemplate.delete(FALLBACK_AMOUNT);
        redisTemplate.delete(PAYMENTS);
    }
}