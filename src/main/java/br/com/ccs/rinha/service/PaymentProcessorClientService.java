package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.repository.RedisPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentProcessorClientService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClientService.class);

    private final RedisPaymentRepository repository;
    private final RestTemplate restTemplate;
    private final String defaultUrl;
    private final String fallbackUrl;
    private final ThreadPoolExecutor executorService;
    private final AtomicInteger rejectedRetries = new AtomicInteger(0);

    public PaymentProcessorClientService(
            RedisPaymentRepository paymentRepository,
            RestTemplate restTemplate,
            ThreadPoolExecutor executorService,
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl) {

        this.repository = paymentRepository;

        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.restTemplate = restTemplate;
        this.executorService = executorService;

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
    }

    public void processPayment(PaymentRequest paymentRequest) {
        processPaymentWithRetry(paymentRequest, 0);
    }

    private void processPaymentWithRetry(PaymentRequest paymentRequest, int retryCount) {
        if (retryCount >= 3) {
//            log.error("Max retries reached for payment {}", paymentRequest.correlationId);
            return;
        }
//        if (retryCount > 0) {
//            log.info("Retry count {}", retriesCounter.incrementAndGet());
//        }
        postToDefault(paymentRequest, retryCount);
    }

    private void postToDefault(PaymentRequest paymentRequest, int retryCount) {
        try {
            paymentRequest.setDefaultTrue();
            restTemplate.postForObject(defaultUrl, paymentRequest, Object.class);
            repository.store(paymentRequest);
        } catch (Exception e) {
//            log.error("Default Error {}", e.getMessage());
            postToFallback(paymentRequest, retryCount);
        }
    }

    private void postToFallback(PaymentRequest paymentRequest, int retryCount) {
        try {
            paymentRequest.setDefaultFalse();
            restTemplate.postForObject(fallbackUrl, paymentRequest, Object.class);
            repository.store(paymentRequest);
        } catch (Exception e) {
            if (!executorService.getQueue().offer(() -> processPaymentWithRetry(paymentRequest, retryCount + 1))) {
                log.warn("Executor queue full, discarding task...");
                log.warn("Rejected retries: {}", rejectedRetries.incrementAndGet());
            }
        }
    }
}