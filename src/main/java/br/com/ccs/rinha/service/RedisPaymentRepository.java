package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class RedisPaymentRepository implements PaymentRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final Object summaryLock = new Object();
    private static final String DEFAULT_PAYMENTS = "default:payments";
    private static final String FALLBACK_PAYMENTS = "fallback:payments";
    private final AtomicReference<BigDecimal> defaultAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> fallbackAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicLong defaultCount = new AtomicLong(0L);
    private final AtomicLong fallbackCount = new AtomicLong(0L);
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
            defaultCount.incrementAndGet();
            defaultAmount.updateAndGet(total -> total.add(request.amount));
            redisTemplate
                    .opsForZSet()
                    .add(DEFAULT_PAYMENTS, request.correlationId + ":" + request.amount, request.requestedAt.toEpochSecond());
        } else {
            fallbackCount.incrementAndGet();
            fallbackAmount.updateAndGet(total -> total.add(request.amount));
            redisTemplate
                    .opsForZSet()
                    .add(FALLBACK_PAYMENTS, request.correlationId + ":" + request.amount, request.requestedAt.toEpochSecond());
        }
    }

    @Override
    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (currentYear > from.getYear() && currentYear < to.getYear()) {
            return new PaymentSummary(
                    new Summary(defaultCount.get(), defaultAmount.get()),
                    new Summary(fallbackCount.get(), fallbackAmount.get()));
        }

        long fromTimestamp = from != null ? from.toEpochSecond() : Long.MIN_VALUE;
        long toTimestamp = to != null ? to.toEpochSecond() : Long.MAX_VALUE;

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
    }
}