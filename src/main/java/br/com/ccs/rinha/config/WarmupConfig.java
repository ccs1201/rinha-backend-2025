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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class WarmupConfig {

    private static final Logger log = LoggerFactory.getLogger(WarmupConfig.class);
    private String WARMUP_URL = "http://%s:8080";

    @Bean
    public CommandLineRunner warmup(RestTemplate restTemplate,
                                    @Value("${server.undertow.threads.worker}") int undertowWorkers,
                                    @Value("${server.port}") int serverPort,
                                    @Value("${warmup:false}") boolean warmup,
                                    @Value("${warmup_rounds:2000}") int warmupRounds,
                                    @Value("${warmup_instance}") String instance) {
        var start = System.currentTimeMillis();
        log.info("Server port: {}", serverPort);
        log.info("Undertow worker threads: {}", undertowWorkers);
        log.info("Warmup instance {}", instance);
        log.info("Warmup rounds: {}", warmupRounds);
        log.info("Starting application warmup...");

        WARMUP_URL = String.format(WARMUP_URL, instance);

        if (!warmup) {
            return arg -> log.info("Warmup disabled");
        }

        return args -> run(restTemplate, serverPort, warmupRounds, start);

    }

    private void run(RestTemplate restTemplate, int serverPort, int warmupRounds, long start) {
        CompletableFuture.runAsync(() -> {
            var executorService = Executors.newFixedThreadPool(60, Thread.ofVirtual().factory());
            waitForServer(restTemplate, serverPort);
            log.info("Application Warmup started wait termination...");
            var msg = "Warming up...";

            var running = CompletableFuture.runAsync(() -> {
                var futures = warm(restTemplate, executorService, warmupRounds);
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }, Executors.newVirtualThreadPerTaskExecutor());

            while (!running.isDone()) {
                msg = msg + " .";
                System.out.println(msg);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Warmup interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

            restTemplate.postForEntity(WARMUP_URL + "/purge-payments", null, Void.class);
            log.info("Force GC to clean up warmup objects");
            System.gc();

            log.info("Wait for JIT optimization and GC cleanup");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Warmup interrupted", e);
                Thread.currentThread().interrupt();
            }

            log.info("Warmup Executor active count {}.", ((ThreadPoolExecutor) executorService).getActiveCount());
            log.info("Warmup Executor current queue size {}", ((ThreadPoolExecutor) executorService).getTaskCount());

            log.info("Application warmup completed in {}ms - ready for test!", System.currentTimeMillis() - start);

            executorService.shutdownNow();
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private ArrayList<CompletableFuture<Void>> warm(RestTemplate restTemplate, ExecutorService executorService, int warmupRounds) {
        var futures = new ArrayList<CompletableFuture<Void>>();

        for (int i = 0; i < warmupRounds; i++) {
            int requestNumber = i;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    var warmupRequest = new PaymentRequest(UUID.randomUUID(), new BigDecimal("19.90"));

                    var response = restTemplate.postForEntity(WARMUP_URL + "/payments", warmupRequest, Void.class);

                    if (!List.of(200, 201, 202, 204).contains(response.getStatusCode().value())) {
                        log.info("Warmup request {} failed with status: {}", requestNumber, response.getStatusCode());
                    }

                } catch (Exception e) {
                    log.info("Warmup request {} error: {}", requestNumber, e.getMessage());
                }
            }, executorService));
        }

        for (int i = 0; i < warmupRounds / 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    fetchSummary(restTemplate);

                    Thread.sleep(1000); // 10s sleep like checkPayments

                } catch (Exception e) {
                    log.info("Warmup summary check error: {}", e.getMessage());
                }
            }, executorService));
        }

        return futures;
    }

    private void fetchSummary(RestTemplate restTemplate) {
        var to = OffsetDateTime.now();
        var from = to.minusSeconds(10);
        restTemplate.getForEntity(
                WARMUP_URL + "/payments-summary?from={from}&to={to}",
                Object.class, from, to);
    }

    private void waitForServer(RestTemplate restTemplate, int port) {
        int attempts = 0;
        while (attempts < 15) {
            log.info("Waiting for server to be ready... attempt {}", attempts);
            try {
                restTemplate.getForEntity(WARMUP_URL + "/check", String.class);
                log.info("Server {} is ready!", WARMUP_URL);
                return;
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Server not ready after 30 seconds, exiting");
        System.exit(0);
    }
}