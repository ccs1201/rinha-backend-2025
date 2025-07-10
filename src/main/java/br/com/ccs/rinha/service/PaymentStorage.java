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
import java.util.stream.Collectors;

@Service
public class PaymentStorage {

    private final ConcurrentHashMap<UUID, PaymentRequest> payments = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(20, Thread.ofPlatform().factory());

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public void store(PaymentRequest request) {
        payments.put(request.correlationId, request);
    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        var filtered = payments
                .values()
                .parallelStream()
                .filter(p -> (from == null || p.requestedAt.isAfter(from)) &&
                        (to == null || p.requestedAt.isBefore(to)))
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