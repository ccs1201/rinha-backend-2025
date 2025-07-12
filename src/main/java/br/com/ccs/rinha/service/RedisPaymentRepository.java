package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class RedisPaymentRepository implements PaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisPaymentRepository.class);

    private final RedisTemplate<String, String> redisTemplate;
    private static final String DEFAULT_COUNT = "default:count";
    private static final String FALLBACK_COUNT = "fallback:count";
    private static final String DEFAULT_AMOUNT = "default:amount";
    private static final String FALLBACK_AMOUNT = "fallback:amount";
    private static final String PAYMENTS = "payments";
    private final int currentYear = LocalDate.now().getYear();
    private final ExecutorService executorService;
    private final boolean shouldShutdownImmediately;

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate, ExecutorService executorService,
                                  @Value("${SHUTDOWN_IMMEDIATELY}") boolean shutdownImmediately) {
        this.redisTemplate = redisTemplate;
        this.executorService = executorService;
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

    @Override
    public void store(PaymentRequest request) {
        var data = String.format("%s:%s:%s", request.correlationId, request.amount, request.isDefault);

        if (request.isDefault) {
            redisTemplate.opsForValue().increment(DEFAULT_COUNT);
            redisTemplate.opsForValue().increment(DEFAULT_AMOUNT, request.amount.doubleValue());
            redisTemplate
                    .opsForZSet()
                    .add(PAYMENTS, data, request.requestedAt.toEpochSecond());
        } else {
            redisTemplate.opsForValue().increment(FALLBACK_COUNT);
            redisTemplate.opsForValue().increment(FALLBACK_AMOUNT, request.amount.doubleValue());
            redisTemplate
                    .opsForZSet()
                    .add(PAYMENTS, data, request.requestedAt.toEpochSecond());
        }
    }

    @Override
    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (from.getYear() < currentYear && to.getYear() > currentYear) {
            log.info("Getting full summary may be inconsistent until the end of async payment process");

            if (!shouldShutdownImmediately) {
                var start = System.currentTimeMillis();
                while (((ThreadPoolExecutor) executorService).getActiveCount() > 0) {
                    try {
                        log.info("Waiting for async payment process to finish. Active tasks: {}", (long) ((ThreadPoolExecutor) executorService).getQueue().size());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                log.info("Async payment process finished in {}ms", System.currentTimeMillis() - start);
            }
            return getFullSummary();
        }

        long fromTimestamp = from.toEpochSecond();
        long toTimestamp = to.toEpochSecond();

        var payments = redisTemplate.opsForZSet().rangeByScore(PAYMENTS, fromTimestamp, toTimestamp);

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
                new Summary(defCount, defAmount.setScale(2, RoundingMode.HALF_UP)),
                new Summary(fallCount, fallAmount.setScale(2, RoundingMode.HALF_UP)));
    }

    private PaymentSummary calculateSummary(Set<String> payments) {
        if (payments == null || payments.isEmpty()) {
            return new PaymentSummary(new Summary(0, BigDecimal.ZERO), new Summary(0, BigDecimal.ZERO));
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
                new Summary(defaultCount, defaultAmount),
                new Summary(fallbackCount, fallbackAmount));
    }

    @Override
    public void purge() {
        redisTemplate.delete(DEFAULT_COUNT);
        redisTemplate.delete(FALLBACK_COUNT);
        redisTemplate.delete(DEFAULT_AMOUNT);
        redisTemplate.delete(FALLBACK_AMOUNT);
    }
}