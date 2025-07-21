package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorConfig {

    private final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean
    public ExecutorService executorService(@Value("${THREAD_POOL_SIZE:15}") int threadPoolSize,
                                           @Value("${THREAD_QUEUE_SIZE:200}") int queueSize) {
        log.info("Thread pool size: {}", threadPoolSize);
        log.info("Queue size: {}", queueSize);
        return new ThreadPoolExecutor(threadPoolSize,
                threadPoolSize,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                Thread.ofVirtual().factory());
    }
}
