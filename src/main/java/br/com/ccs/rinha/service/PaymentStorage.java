package br.com.ccs.rinha.service;

import br.com.ccs.rinha.exception.PaymentSummaryException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Service
public class PaymentStorage {

    private final ConcurrentHashMap<UUID, Payment> payments = new ConcurrentHashMap<>();
    private final AtomicLong defaultCount = new AtomicLong();
    private final AtomicLong fallbackCount = new AtomicLong();
    private final AtomicReference<BigDecimal> defaultAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> fallbackAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final ExecutorService executor = Executors.newFixedThreadPool(8, Thread.ofVirtual().factory());


    public void store(PaymentProcessorClient.PaymentRequest request, boolean isDefault) {
        payments.put(request.correlationId(),
                new Payment(request.correlationId(), request.amount(), isDefault, request.requestedAt()));

        if (isDefault) {
            defaultCount.incrementAndGet();
            defaultAmount.updateAndGet(current -> current.add(request.amount()));
        } else {
            fallbackCount.incrementAndGet();
            fallbackAmount.updateAndGet(current -> current.add(request.amount()));
        }
    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) {
            return new PaymentSummary(
                    new Summary(defaultCount.get(), defaultAmount.get()),
                    new Summary(fallbackCount.get(), fallbackAmount.get())
            );
        }

        var filtered = payments.values().parallelStream()
                .filter(p -> (from == null || !p.processedAt.isBefore(from)) &&
                        (to == null || !p.processedAt.isAfter(to)))
                .toList();

        var futures = new CompletableFuture[4];

        //total req default
        futures[0] = supplyAsync(() -> filtered
                .parallelStream()
                .filter(p -> p.isDefault)
                .count(), executor);

        //total amount default
        futures[1] = supplyAsync(() -> filtered
                .parallelStream()
                .filter(p -> p.isDefault)
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add), executor);

        //total req fallback
        futures[2] = supplyAsync(() -> filtered
                .parallelStream()
                .filter(p -> !p.isDefault)
                .count());

        //total amount fallback
        futures[3] = supplyAsync(() -> filtered
                .parallelStream()
                .filter(p -> !p.isDefault)
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        CompletableFuture.allOf(futures).join();

        try {
            return new PaymentSummary(new Summary((Long) futures[0].get(), (BigDecimal) futures[1].get()), new Summary((Long) futures[2].get(), (BigDecimal) futures[3].get()));
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new PaymentSummaryException(e);
        }
    }

    public void purge() {
        payments.clear();
        defaultCount.set(0);
        fallbackCount.set(0);
        defaultAmount.set(BigDecimal.ZERO);
        fallbackAmount.set(BigDecimal.ZERO);
    }

    record Payment(UUID correlationId, BigDecimal amount, boolean isDefault, OffsetDateTime processedAt) {
    }

    public record PaymentSummary(
            @JsonProperty("default") Summary _default, Summary fallback) {
    }

    record Summary(long totalRequests, BigDecimal totalAmount) {
    }
}