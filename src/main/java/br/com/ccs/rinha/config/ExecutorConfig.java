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
    public ExecutorService executorService(@Value("${thread_pol_size:15}") int threadPoolSize,
                                           @Value("${thread_queue_size:20_000}") int queueSize) {
        log.info("Thread pool size: {}", threadPoolSize);
        log.info("Thread pool Queue size {}", queueSize);

        return new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize, true)
        );
    }

}
