package br.com.ccs.rinha.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ExecutorMonitor {

    private static final Logger log = LoggerFactory.getLogger(ExecutorMonitor.class);

    private final ExecutorService executor;

    public ExecutorMonitor(ExecutorService executor) {
        this.executor = executor;
    }

    @PostConstruct
    public void startMonitoring() {
        var activeMonitor = Boolean.parseBoolean(System.getenv("ACTIVE_MONITOR"));

        if (activeMonitor) {
            var tp = (ThreadPoolExecutor) executor;

            var t = Thread.ofVirtual();
            t.start(() -> {
                long lastCompleted = tp.getCompletedTaskCount();

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int active = tp.getActiveCount();
                        int poolSize = tp.getPoolSize();
                        int queueSize = tp.getQueue().size();
                        int remainingQueue = tp.getQueue().remainingCapacity();
                        long completed = tp.getCompletedTaskCount();
                        long throughput = completed - lastCompleted;
                        lastCompleted = completed;

                        log.info("active: {}, pool: {}, queue: {}, remaining: {}, completed: {}, throughput: {} P/s",
                                active, poolSize, queueSize, remainingQueue, completed, throughput);

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

        } else {
            log.error("Executor Monitor inactive.");
        }
    }
}
