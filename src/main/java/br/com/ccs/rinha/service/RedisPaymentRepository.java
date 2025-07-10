package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class RedisPaymentRepository implements PaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisPaymentRepository.class);

    private final RedisTemplate<String, String> redisTemplate;
    private static final String DEFAULT_PAYMENTS = "default:payments";
    private static final String FALLBACK_PAYMENTS = "fallback:payments";

    public RedisPaymentRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("Using Redis as storage");
    }

    @Override
    public void store(PaymentRequest request) {
        long timestamp = request.requestedAt.toEpochSecond();
        String value = request.correlationId + ":" + request.amount;

        if (request.isDefault) {
            redisTemplate.opsForZSet().add(DEFAULT_PAYMENTS, value, timestamp);
        } else {
            redisTemplate.opsForZSet().add(FALLBACK_PAYMENTS, value, timestamp);
        }
    }

    @Override
    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {
        double fromTimestamp = from != null ? from.toEpochSecond() : Double.NEGATIVE_INFINITY;
        double toTimestamp = to != null ? to.toEpochSecond() : Double.POSITIVE_INFINITY;

        var defaultPayments = redisTemplate.opsForZSet().rangeByScore(DEFAULT_PAYMENTS, fromTimestamp, toTimestamp);
        var fallbackPayments = redisTemplate.opsForZSet().rangeByScore(FALLBACK_PAYMENTS, fromTimestamp, toTimestamp);

        var defaultSummary = calculateSummary(defaultPayments);
        var fallbackSummary = calculateSummary(fallbackPayments);

        return new PaymentSummary(defaultSummary, fallbackSummary);
    }

    private Summary calculateSummary(Set<String> payments) {
        if (payments == null || payments.isEmpty()) {
            return new Summary(0, BigDecimal.ZERO);
        }

        long totalRequests = payments.size();
        BigDecimal totalAmount = payments.stream()
                .map(payment -> {
                    String[] parts = payment.split(":");
                    return new BigDecimal(parts[1]);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Summary(totalRequests, totalAmount);
    }


    @Override
    public void purge() {
        redisTemplate.delete(DEFAULT_PAYMENTS);
        redisTemplate.delete(FALLBACK_PAYMENTS);
    }
}