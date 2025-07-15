package br.com.ccs.rinha.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ExecutorConfig {

    private final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Produces
    @Named("paymentProcessorExecutor")
    public ExecutorService executorService() {

        String threadPoolSizeStr = System.getenv("THREAD_POOL_SIZE");
        String queueSizeStr = System.getenv("THREAD_QUEUE_SIZE");
        
        int threadPoolSize = threadPoolSizeStr != null ? Integer.parseInt(threadPoolSizeStr) : 10;
        int queueSize = queueSizeStr != null ? Integer.parseInt(queueSizeStr) : 100;

        log.info("Thread pool size: {}", threadPoolSize);
        log.info("Thread pool Queue size {}", queueSize);

        return new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize, true)
        );
    }

}
