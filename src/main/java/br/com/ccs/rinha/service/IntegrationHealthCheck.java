package br.com.ccs.rinha.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class IntegrationHealthCheck {

    private final PaymentProcessorClient client;
    private final AtomicReference<CachedHealth> defaultCache = new AtomicReference<>();
    private final AtomicReference<CachedHealth> fallbackCache = new AtomicReference<>();

    public IntegrationHealthCheck(PaymentProcessorClient client) {
        this.client = client;
    }

    public PaymentProcessorClient.ServiceHealth checkDefault() {
        return getCachedHealth(true);
    }

    private PaymentProcessorClient.ServiceHealth getCachedHealth(boolean isDefault) {
        var cache = isDefault ? defaultCache : fallbackCache;
        var now = Instant.now();
        
        var cached = cache.get();
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.health();
        }

        synchronized (cache) {
            cached = cache.get();
            if (cached != null && now.isBefore(cached.expiresAt())) {
                return cached.health();
            }
            
            var health = client.checkHealth(isDefault);
            cache.set(new CachedHealth(health, now.plusSeconds(5)));
            return health;
        }
    }

    record CachedHealth(PaymentProcessorClient.ServiceHealth health, Instant expiresAt) {
    }
}