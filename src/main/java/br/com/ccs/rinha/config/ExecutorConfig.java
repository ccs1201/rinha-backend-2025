package br.com.ccs.rinha.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);
    private static ExecutorService instance;

    static {
        configure();
    }

    private ExecutorConfig() {
    }

    public static ExecutorService getExecutor() {
        return instance;
    }

    private static void configure() {

        String threadPoolSizeStr = System.getenv("thread-pool-size").trim();
        String queueSizeStr = System.getenv("thread-queue-size").trim();

        int threadPoolSize = threadPoolSizeStr.isBlank() ? 11 : Integer.parseInt(threadPoolSizeStr) + 1; //+1 pra thread vertexpaymentprocessor
        int queueSize = queueSizeStr.isBlank() ? 1000 : Integer.parseInt(queueSizeStr);

        log.info("Thread pool size: {} + 1 para VertexPaymentProcessor", threadPoolSize);
        log.info("Thread pool Queue size {}", queueSize);

        ExecutorConfig.instance = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize, true),
                Thread.ofVirtual().factory(),
                new ThreadPoolExecutor.DiscardPolicy());
    }
}
