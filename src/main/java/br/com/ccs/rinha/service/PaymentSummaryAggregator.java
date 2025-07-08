package br.com.ccs.rinha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class PaymentSummaryAggregator {

    private static final Logger log = LoggerFactory.getLogger(PaymentSummaryAggregator.class);

    private final PaymentStorage storage;
    private final RestTemplate restTemplate;
    private final String otherInstanceUrl;

    public PaymentSummaryAggregator(PaymentStorage storage, RestTemplate restTemplate,
                                    @Value("${INSTANCE_NAME:app1}") String instanceName) {
        this.storage = storage;
        this.restTemplate = restTemplate;
        this.otherInstanceUrl = "app1".equals(instanceName) ? "http://app2:8080" : "http://app1:8080";
        log.info("Current instance {} Other instance URL: {}", instanceName, otherInstanceUrl);
    }

    public PaymentStorage.PaymentSummary getAggregatedSummary(OffsetDateTime from, OffsetDateTime to) {
        var localSummary = storage.getSummary(from, to);

        try {
            var params = buildParams(from, to);

            var remoteSummary = CompletableFuture.supplyAsync(() -> {
                try {
                    return restTemplate.getForObject(otherInstanceUrl + "/local-summary" + params,
                            PaymentStorage.PaymentSummary.class);
                } catch (Exception e) {
                    log.error("Error while fetching remote summary", e);
                    return new PaymentStorage.PaymentSummary(
                            new PaymentStorage.Summary(0, BigDecimal.ZERO),
                            new PaymentStorage.Summary(0, BigDecimal.ZERO)
                    );
                }
            }).orTimeout(1000, MILLISECONDS).join();

            return mergeSummaries(localSummary, remoteSummary);
        } catch (Exception e) {
            log.error("Error while fetching remote summary", e);
            return localSummary;
        }
    }

    private String buildParams(OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) return "";
        var params = new StringBuilder("?");
        if (from != null) params.append("from=").append(from);
        if (to != null) {
            if (from != null) params.append("&");
            params.append("to=").append(to);
        }
        return params.toString();
    }

    private PaymentStorage.PaymentSummary mergeSummaries(PaymentStorage.PaymentSummary local,
                                                         PaymentStorage.PaymentSummary remote) {
        return new PaymentStorage.PaymentSummary(
                new PaymentStorage.Summary(
                        local._default().totalRequests() + remote._default().totalRequests(),
                        local._default().totalAmount().add(remote._default().totalAmount())
                ),
                new PaymentStorage.Summary(
                        local.fallback().totalRequests() + remote.fallback().totalRequests(),
                        local.fallback().totalAmount().add(remote.fallback().totalAmount())
                )
        );
    }
}