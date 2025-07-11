package br.com.ccs.rinha.service;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private final PaymentRepository repository;
    private final RestTemplate restTemplate;
    private final String defaultUrl;
    private final String fallbackUrl;

    public PaymentProcessorClient(
            PaymentRepository paymentRepository,
            RestTemplate restTemplate,
            @Value("${payment-processor.default.url}") String defaultUrl,
            @Value("${payment-processor.fallback.url}") String fallbackUrl) {

        this.repository = paymentRepository;

        this.defaultUrl = defaultUrl.concat("/payments");
        this.fallbackUrl = fallbackUrl.concat("/payments");
        this.restTemplate = restTemplate;

        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
    }

    public void processPayment(PaymentRequest paymentRequest) {

        try {
            postToDefault(paymentRequest);
        } catch (RestClientException e) {
            try {
                postToFallback(paymentRequest);
            } catch (RestClientException ex) {
                sleep();
                processPayment(paymentRequest);
            }

        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void postToDefault(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultTrue();
        restTemplate.postForObject(defaultUrl, paymentRequest, Object.class);
        repository.store(paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) {
        paymentRequest.setDefaultFalse();
        restTemplate.postForObject(fallbackUrl, paymentRequest, Object.class);
        repository.store(paymentRequest);
    }
}