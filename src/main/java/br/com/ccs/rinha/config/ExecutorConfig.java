package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class ExecutorConfig {

    private final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean
    public ExecutorService executorService() {

        var virtual = Boolean.parseBoolean(System.getenv("VIRTUAL_THREADS"));
        int threadPoolSize = Integer.parseInt(System.getenv("THREAD_POOL_SIZE"));
        int queueSize = Integer.parseInt(System.getenv("QUEUE_SIZE"));

        log.info("Using Virtual Threads: {}", virtual);
        log.info("Thread pool size: {}", threadPoolSize);
        return new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize, true),
                virtual ? Thread.ofVirtual().factory() : Thread.ofPlatform().factory(),
                new ThreadPoolExecutor.DiscardPolicy());

    }

}
