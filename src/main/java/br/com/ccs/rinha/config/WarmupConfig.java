package br.com.ccs.rinha.config;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class WarmupConfig {

    private static final Logger log = LoggerFactory.getLogger(WarmupConfig.class);

    @Bean
    public CommandLineRunner warmup(RestTemplate restTemplate, ExecutorService executorService,
                                    @Value("${server.undertow.threads.worker}") int undertowWorkers,
                                    @Value("${server.port}") int serverPort) {
        var start = System.currentTimeMillis();
        log.info("Server port: {}", serverPort);
        log.info("Undertow worker threads: {}", undertowWorkers);
        log.info("Starting application warmup...");

        return args -> {
            var warmupFuture = CompletableFuture.runAsync(() -> {
                try {
                    var futures = new ArrayList<CompletableFuture<Void>>();

                    for (int i = 0; i < 5000; i++) {
                        futures.add(CompletableFuture.runAsync(() -> {
                            var warmupRequest = new PaymentRequest(UUID.randomUUID(), BigDecimal.TEN);
                            var response = restTemplate.postForEntity("http://localhost:8080/payments", warmupRequest, Void.class);
                            if (!response.getStatusCode().is2xxSuccessful()) {
                                log.error("Warmup request failed with status code: {}", response.getStatusCode());
                            }
                        }, executorService));
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                    var from = OffsetDateTime.now().minusHours(1);
                    var to = OffsetDateTime.now().plusHours(1);

                    for (int i = 0; i < 200; i++) {
                        restTemplate.getForEntity(
                                "http://localhost:8080/payments-summary?from={from}&to={to}",
                                Object.class, from, to);
                    }

                    restTemplate.postForEntity("http://localhost:8080/purge-payments", null, Void.class);

                    // Wait for JIT optimization and GC cleanup
                    Thread.sleep(3000);

                    // Force GC to clean up warmup objects
                    System.gc();
                    Thread.sleep(500);

                } catch (Exception e) {
                    log.error("Warmup failed: {}", e.getMessage());
                }
            });

            while (!warmupFuture.isDone()) {
                log.info("Application warming up wait....");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("Application warmup completed in {}ms - ready for test!", System.currentTimeMillis() - start);
        };
    }
}