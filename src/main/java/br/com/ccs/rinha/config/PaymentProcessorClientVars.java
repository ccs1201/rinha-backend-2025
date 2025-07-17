package br.com.ccs.rinha.config;

import org.slf4j.Logger;

import java.net.URI;

public class PaymentProcessorClientVars {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentProcessorClientVars.class);

    public final String defaultUrl;
    public final String fallbackUrl;
    public final URI defaultURI;
    public final URI fallbackURI;
    public final int requestTimout;
    public final int fallbackRequestTimeout;
    public static final PaymentProcessorClientVars instance = new PaymentProcessorClientVars();

    private PaymentProcessorClientVars() {
        var urlDef = System.getenv("payment-processor-default-url").trim();
        this.defaultUrl = urlDef.concat("/payments");
        var urlFall = System.getenv("payment-processor-fallback-url").trim();
        this.fallbackUrl = urlFall.concat("/payments");
        this.defaultURI = URI.create(defaultUrl);
        this.fallbackURI = URI.create(fallbackUrl);
        this.requestTimout = Integer.parseInt(System.getenv("client-processor-timeout").trim());
        this.fallbackRequestTimeout = (int) (requestTimout * 1.5);


        log.info("Default service URL: {}", this.defaultUrl);
        log.info("Fallback fallback URL: {}", this.fallbackUrl);
        log.info("Request timeout: {}", this.requestTimout);
        log.info("Request Fallback timeout: {}", this.fallbackRequestTimeout);
    }

    public static PaymentProcessorClientVars getInstance() {
        return instance;
    }
}
