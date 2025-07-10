package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
public class PaymentStorage {

    private final ConcurrentHashMap<UUID, PaymentRequest> payments = new ConcurrentHashMap<>();
    private final AtomicLong defaultCount = new AtomicLong();
    private final AtomicLong fallbackCount = new AtomicLong();
    private final AtomicReference<BigDecimal> defaultAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> fallbackAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final ExecutorService executor = Executors.newFixedThreadPool(20, Thread.ofPlatform().factory());

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public void store(PaymentRequest request) {
        payments.put(request.correlationId, request);

        if (request.isDefault) {
            defaultCount.incrementAndGet();
            defaultAmount.updateAndGet(current -> current.add(request.amount));
        } else {
            fallbackCount.incrementAndGet();
            fallbackAmount.updateAndGet(current -> current.add(request.amount));
        }
    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (from == null && to == null) {
            return getPaymentsSummary();
        }

        if (nonNull(from) && OffsetDateTime.MIN.isBefore(from) && nonNull(to) && OffsetDateTime.now().isAfter(to)) {
            return getPaymentsSummary();
        }

        var filtered = payments
                .values()
                .parallelStream()
                .filter(p -> (from == null || p.processedAt.isAfter(from)) &&
                        (to == null || p.processedAt.isBefore(to)))
                .collect(Collectors
                        .partitioningBy(p -> p.isDefault,
                                Collectors.teeing(
                                        Collectors.counting(),
                                        Collectors.reducing(BigDecimal.ZERO, p -> p.amount, BigDecimal::add),
                                        Summary::new)));

        return new PaymentSummary(filtered.get(true), filtered.get(false));

    }

    private PaymentSummary getPaymentsSummary() {
        return new PaymentSummary(
                new Summary(defaultCount.get(), defaultAmount.get()),
                new Summary(fallbackCount.get(), fallbackAmount.get())
        );
    }

    public void purge() {
        payments.clear();
        defaultCount.set(0);
        fallbackCount.set(0);
        defaultAmount.set(BigDecimal.ZERO);
        fallbackAmount.set(BigDecimal.ZERO);
    }

    public record PaymentSummary(
            @JsonProperty("default") Summary _default, Summary fallback) {
    }

    record Summary(long totalRequests, BigDecimal totalAmount) {
    }
}