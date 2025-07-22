package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.repository.RedisPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class WebClientPaymentProcessorClientService {

    private static final Logger log = LoggerFactory.getLogger(WebClientPaymentProcessorClientService.class);

    private final WebClient webClient;
    private final RedisPaymentRepository repository;
    private final String defaultUrl;
    private final String fallbackUrl;
    private final ThreadPoolExecutor executor;

    public WebClientPaymentProcessorClientService(
            RedisPaymentRepository repository,
            ThreadPoolExecutor executor,
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl) {

        var readTimeOut = Integer.parseInt(System.getenv("REQUEST_READ_TIMEOUT"));

        log.info("Response timeout: {}", readTimeOut);

        this.repository = repository;
        this.executor = executor;
        this.defaultUrl = defaultUrl + "/payments";
        this.fallbackUrl = fallbackUrl + "/payments";

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .compress(true)
                                .responseTimeout(Duration.ofMillis(readTimeOut))
                ))
                .build();
    }

    public void processPayment(PaymentRequest paymentRequest) {
        submitWithRetry(paymentRequest, 0);
    }

    private void submitWithRetry(PaymentRequest request, int retryCount) {
        request.setDefaultTrue();

        webClient.post()
                .uri(defaultUrl)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.createException().flatMap(Mono::error))
                .toBodilessEntity()
                .doOnSuccess(resp -> repository.store(request))
                .doOnError(ex -> handleFallback(request, retryCount))
                .subscribe(success -> {
                        },
                        error -> handleFallback(request, retryCount)
                );
    }

    private void handleFallback(PaymentRequest request, int retryCount) {
        request.setDefaultFalse();

        webClient.post()
                .uri(fallbackUrl)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.createException().flatMap(Mono::error)
                )
                .toBodilessEntity()
                .doOnSuccess(resp -> repository.store(request))
                .doOnError(ex -> {

                    int nextRetry = retryCount + 1;
                    if (nextRetry <= 3) {
                        long baseDelay = Math.min(100L * (1L << retryCount), 1000L);
                        long jitter = ThreadLocalRandom.current().nextLong(50);
                        long delay = baseDelay + jitter;

                        Runnable retryTask = () -> submitWithRetry(request, nextRetry);
                        boolean offered = executor.getQueue().offer(() -> {
                            try {
                                Thread.sleep(delay);
                                retryTask.run();
                            } catch (InterruptedException e) {
                                return;
                            }
                        });

                        if (!offered) {
                            log.warn("Fila cheia, descartando retry para pagamento {}", request.correlationId);
                        }
                    } else {
                        log.warn("Máximo de tentativas atingido para pagamento {}", request.correlationId);
                    }
                })
                .subscribe(success -> {
                        },
                        error -> handleFallback(request, retryCount)
                );
    }
}
