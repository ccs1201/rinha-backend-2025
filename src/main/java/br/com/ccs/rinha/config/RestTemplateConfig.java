package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);
    private final String instanceName;

    public RestTemplateConfig(@Value("${INSTANCE_NAME:app1}") String instanceName) {
        this.instanceName = instanceName;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    //Apenas esquentar o rest template e dispatcher servelet
    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            log.info("Checking other instance status");
            var otherInstanceUrl = "app1".equals(instanceName) ? "http://app2:8080" : "http://app1:8080";
            try {
                restTemplate.getForObject(otherInstanceUrl + "/check-status",Object.class);
                log.info("Other instance {} is up", otherInstanceUrl);
            } catch (Exception e) {
                Thread.sleep(1000);
                this.run(restTemplate);
            }
        };
    }
}