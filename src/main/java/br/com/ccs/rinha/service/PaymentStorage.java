package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class PaymentStorage {

    private final ConcurrentHashMap<UUID, PaymentRequest> payments = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(20, Thread.ofPlatform().factory());
    private final AtomicReference<BigDecimal> defualtAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> fallbackAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicLong defualtCount = new AtomicLong(0);
    private final AtomicLong fallbackCount = new AtomicLong(0);
    private final int currentYear = LocalDate.now().getYear();

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public void store(PaymentRequest request) {
        payments.put(request.correlationId, request);
        if (request.isDefault) {
            defualtCount.incrementAndGet();
            defualtAmount.accumulateAndGet(request.amount, BigDecimal::add);
        } else {
            fallbackCount.incrementAndGet();
            fallbackAmount.accumulateAndGet(request.amount, BigDecimal::add);
        }
    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        if (currentYear > from.getYear() && currentYear < to.getYear()) {
            return new PaymentSummary(new Summary(defualtCount.get(), defualtAmount.get()),
                    new Summary(fallbackCount.get(), fallbackAmount.get()));
        }

        var filtered = payments
                .values()
                .parallelStream()
                .filter(p -> (p.requestedAt.isAfter(from)) &&
                        (p.requestedAt.isBefore(to)))
                .collect(Collectors
                        .partitioningBy(p -> p.isDefault,
                                Collectors.teeing(
                                        Collectors.counting(),
                                        Collectors.reducing(BigDecimal.ZERO, p -> p.amount, BigDecimal::add),
                                        Summary::new)));

        return new PaymentSummary(filtered.get(true), filtered.get(false));
    }

    public void purge() {
        payments.clear();
    }

    public record PaymentSummary(
            @JsonProperty("default") Summary _default, Summary fallback) {
    }

    record Summary(long totalRequests, BigDecimal totalAmount) {
    }
}