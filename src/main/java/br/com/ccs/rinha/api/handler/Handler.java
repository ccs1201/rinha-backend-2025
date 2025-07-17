package br.com.ccs.rinha.api.handler;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.exception.HandlerException;
import br.com.ccs.rinha.httpclient.VertexPaymentProcessorClient;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.repository.PaymentRepository;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Handler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    private static PaymentProcessorClient paymentProcessorClient;
    private static PaymentRepository paymentRepository;
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
        paymentProcessorClient = VertexPaymentProcessorClient.getInstance();//PaymentProcessorClient.getInstance();
        executor = ExecutorConfig.getExecutor();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
            try {
                var requestURI = exchange.getRequestURI();

                if (requestURI.equals(postPaymentURI)) {
//                    CompletableFuture.runAsync(() -> {
                        try {
                            paymentProcessorClient.processPayment(PaymentRequest.of(data));
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            throw new HandlerException(e);
                        }
//                    }, executor);
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
                                    .wrap(paymentRepository.getSummary(from, to).toJson().getBytes(StandardCharsets.UTF_8)));
                }

                if (requestURI.equals(postPurgePaymentsURI)) {
                    paymentRepository.purge();
                    paymentProcessorClient.failedRetryAttempsts.set(0);
                    paymentProcessorClient.purge();
                    ex.setStatusCode(200);
                    ex.getResponseSender().send(emptyResnpose);
                }

            } catch (Exception e) {
                throw new HandlerException(e);
            }
        });
    }
}
