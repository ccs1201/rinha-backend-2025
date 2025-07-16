package br.com.ccs.rinha.api.handler;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.config.ObjectMapperFactory;
import br.com.ccs.rinha.exception.HandlerException;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import br.com.ccs.rinha.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Handler implements HttpHandler {

    private static PaymentProcessorClient paymentProcessorClient;
    private static PaymentRepository paymentRepository;
    private static ObjectMapper objectMapper;
    private static Executor executor;

    private final String postPaymentURI = "/payments";
    private final String getSummaryURI = "/payments-summary";
    private final String postPurgePaymentsURI = "/purge-payments";
    private final String emptyResnpose = "";
    private final HttpString responseContentType = new HttpString("Content-Type");

    private static final Handler instance;

    static {
        instance = new Handler();
    }

    public static Handler getInstance() {
        return instance;
    }

    private Handler() {
        paymentRepository = JdbcPaymentRepository.getInstance();
        paymentProcessorClient = PaymentProcessorClient.getInstance();
        objectMapper = ObjectMapperFactory.getInstance();
        executor = ExecutorConfig.getExecutor();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
            try {
                var requestURI = exchange.getRequestURI();

                if (requestURI.equals(postPaymentURI)) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            paymentProcessorClient.processPayment(objectMapper.readValue(data, PaymentRequest.class));
                        } catch (Exception e) {
                            throw new HandlerException(e);
                        }
                    }, executor);
                    ex.setStatusCode(202);
                    ex.getResponseSender().send(emptyResnpose);
                }

                if (requestURI.equals(getSummaryURI)) {
                    var from = OffsetDateTime.parse(exchange.getQueryParameters().get("from").getFirst());
                    var to = OffsetDateTime.parse(exchange.getQueryParameters().get("to").getFirst());
                    ex.setStatusCode(200);
                    ex.getResponseHeaders().put(responseContentType, "application/json");
                    ex.getResponseSender()
                            .send(ByteBuffer
                                    .wrap(objectMapper
                                            .writeValueAsBytes(paymentRepository.getSummary(from, to))));

                }

                if (requestURI.equals(postPurgePaymentsURI)) {
                    paymentRepository.purge();
                    ex.setStatusCode(200);
                    ex.getResponseSender().send(emptyResnpose);
                }
            } catch (Exception e) {
                throw new HandlerException(e);
            }
        });
    }
}
