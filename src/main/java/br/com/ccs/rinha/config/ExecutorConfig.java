package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public class ExecutorConfig {

    private final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean
    public ExecutorService executorService() {
        int threadPoolSize = Integer.parseInt(System.getenv("THREAD_POOL_SIZE"));
        log.info("Thread pool size: {}", threadPoolSize);
        return Executors.newFixedThreadPool(threadPoolSize, Thread.ofVirtual().factory());
    }

}
