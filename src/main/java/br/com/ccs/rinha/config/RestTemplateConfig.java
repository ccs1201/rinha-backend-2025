package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
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

}