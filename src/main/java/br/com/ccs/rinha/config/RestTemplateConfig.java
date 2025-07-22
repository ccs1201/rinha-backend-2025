package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        var connectionTimeOut = Integer.parseInt(System.getenv("REQUEST_CONNECTION_TIMEOUT"));
        var readTimeOut = Integer.parseInt(System.getenv("REQUEST_READ_TIMEOUT"));

        log.info("Connection timeout: {}", connectionTimeOut);
        log.info("Read timeout: {}", readTimeOut);

        return new RestTemplateBuilder()
                .requestFactorySettings(requestFactory -> requestFactory.
                        withConnectTimeout(Duration.ofMillis(connectionTimeOut))
                        .withReadTimeout(Duration.ofMillis(readTimeOut)))
                .build();
    }

    //Esquenta o RestTemplate
//    @Bean
    public CommandLineRunner commandLineRunner(RestTemplate restTemplate, @Value("${server.port}") String serverPort) {
        return args ->
                warm(restTemplate, serverPort);
    }

    private static void warm(RestTemplate restTemplate, String serverPort) {
        String payload = """
                {"correlationId":"%s","amount":19.9}
                """;

        final var url = "http://localhost:" + serverPort + "/payments";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("Warming up RestTemplate...");
        try {
            var futures = new ArrayList<CompletableFuture<Void>>();

            for (int i = 0; i < 102; i++) {
                HttpEntity<String> request = new HttpEntity<>(String.format(payload, UUID.randomUUID()), headers);
                futures.add(CompletableFuture.runAsync(() ->
                                restTemplate.postForObject(url, request, String.class)
                        , Executors.newVirtualThreadPerTaskExecutor()));

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } catch (Exception e) {
            log.error("", e);
        }
        log.info("Warmed up RestTemplate");
        log.info("Application ready...");
    }
}