package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RedisPaymentRepository implements PaymentRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final Object summaryLock = new Object();
    private static final String DEFAULT_PAYMENTS = "default:payments";
    private static final String FALLBACK_PAYMENTS = "fallback:payments";
    private static final String DEFAULT_COUNT = "default:count";
    private static final String FALLBACK_COUNT = "fallback:count";
    private static final String DEFAULT_AMOUNT = "default:amount";
    private static final String FALLBACK_AMOUNT = "fallback:amount";
    private final int currentYear = LocalDate.now().getYear();

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
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

        if (request.isDefault) {
            redisTemplate.opsForValue().increment(DEFAULT_COUNT);
            redisTemplate.opsForValue().increment(DEFAULT_AMOUNT, request.amount.doubleValue());
            redisTemplate
                    .opsForZSet()
                    .add(DEFAULT_PAYMENTS, request.correlationId + ":" + request.amount, request.requestedAt.toEpochSecond());
        } else {
            redisTemplate.opsForValue().increment(FALLBACK_COUNT);
            redisTemplate.opsForValue().increment(FALLBACK_AMOUNT, request.amount.doubleValue());
            redisTemplate
                    .opsForZSet()
                    .add(FALLBACK_PAYMENTS, request.correlationId + ":" + request.amount, request.requestedAt.toEpochSecond());
        }
    }

    @Override
    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (from.getYear() < currentYear && to.getYear() > currentYear) {
            String defaultCountStr = redisTemplate.opsForValue().get(DEFAULT_COUNT);
            String fallbackCountStr = redisTemplate.opsForValue().get(FALLBACK_COUNT);
            String defaultAmountStr = redisTemplate.opsForValue().get(DEFAULT_AMOUNT);
            String fallbackAmountStr = redisTemplate.opsForValue().get(FALLBACK_AMOUNT);

            long defCount = defaultCountStr != null ? Long.parseLong(defaultCountStr) : 0;
            long fallCount = fallbackCountStr != null ? Long.parseLong(fallbackCountStr) : 0;
            BigDecimal defAmount = defaultAmountStr != null ? BigDecimal.valueOf(Double.parseDouble(defaultAmountStr)) : BigDecimal.ZERO;
            BigDecimal fallAmount = fallbackAmountStr != null ? BigDecimal.valueOf(Double.parseDouble(fallbackAmountStr)) : BigDecimal.ZERO;

            return new PaymentSummary(
                    new Summary(defCount, defAmount.setScale(2, RoundingMode.FLOOR)),
                    new Summary(fallCount, fallAmount.setScale(2, RoundingMode.FLOOR)));
        }

        long fromTimestamp = from.toEpochSecond();
        long toTimestamp = to.toEpochSecond();

        synchronized (summaryLock) {
            var defaultPayments = redisTemplate.opsForZSet().rangeByScore(DEFAULT_PAYMENTS, fromTimestamp, toTimestamp);
            var fallbackPayments = redisTemplate.opsForZSet().rangeByScore(FALLBACK_PAYMENTS, fromTimestamp, toTimestamp);

            var defaultSummary = calculateSummary(defaultPayments);
            var fallbackSummary = calculateSummary(fallbackPayments);

            return new PaymentSummary(defaultSummary, fallbackSummary);
        }
    }

    private Summary calculateSummary(Set<String> payments) {
        if (payments == null || payments.isEmpty()) {
            return new Summary(0, BigDecimal.ZERO);
        }

        return payments.parallelStream()
                .map(payment -> new BigDecimal(payment.split(":")[1]))
                .collect(Collectors.teeing(
                        Collectors.counting(),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add),
                        Summary::new
                ));
    }

    @Override
    public void purge() {
        redisTemplate.delete(DEFAULT_PAYMENTS);
        redisTemplate.delete(FALLBACK_PAYMENTS);
        redisTemplate.delete(DEFAULT_COUNT);
        redisTemplate.delete(FALLBACK_COUNT);
        redisTemplate.delete(DEFAULT_AMOUNT);
        redisTemplate.delete(FALLBACK_AMOUNT);
    }
}