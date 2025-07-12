package br.com.ccs.rinha.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    //Esquenta o RestTemplate
    @Bean
    public CommandLineRunner commandLineRunner(RestTemplate restTemplate, @Value("${server.port}") String serverPort) {
        return args ->
                LoggerFactory.getLogger(RestTemplateConfig.class)
                        .info("Warming up RestTemplate {}",
                                restTemplate.getForEntity(
                                        String.format("http://localhost:%s/payments-summary?from=2000-01-01T00:00Z&to=2900-01-01T00:00Z",
                                                serverPort)
                                        , String.class));
    }
}