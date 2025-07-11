package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    private final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean
    public ExecutorService executorService(@Value("${THREAD_POOL_SIZE:10}") int threadPoolSize) {
        log.info("Thread pool size: {}", threadPoolSize);
        return Executors.newFixedThreadPool(threadPoolSize, Thread.ofVirtual().factory());
    }

}
