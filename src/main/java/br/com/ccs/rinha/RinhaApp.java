package br.com.ccs.rinha;

import br.com.ccs.rinha.api.handler.Handler;
import br.com.ccs.rinha.config.ExecutorConfig;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RinhaApp {

    private static final Logger log = LoggerFactory.getLogger(RinhaApp.class);

    public static void main(String[] args) {
        var envPort = System.getenv("server-port");
        var serverIOThreads = System.getenv("server-io-threads") == null ? 2 : Integer.parseInt(System.getenv("server-io-threads"));
        var serverWorkerThreads = System.getenv("server-worker-threads") == null ? 30 : Integer.parseInt(System.getenv("server-worker-threads"));
        int serverPort = Objects.isNull(envPort) ? 8080 : Integer.parseInt(envPort);

        log.info("Starting rinha de backend app");
        printPromo(5);

        Undertow server = Undertow.builder()
                .addHttpListener(serverPort, "0.0.0.0")
                .setHandler(Handler.getInstance())
                .setIoThreads(serverIOThreads)
                .setWorkerThreads(serverWorkerThreads)
                .setDirectBuffers(true)
                .setBufferSize(1024 * 16 - 20)
                .build();
        server.start();
        log.info("Starting server on port {} wait...", serverPort);
        printPromo(5);
        log.info("Server started! | I want to play a game!");

        registerShutDownHook(server);
    }

    private static void registerShutDownHook(Undertow server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.stop();
            ExecutorConfig.getExecutor().shutdown();
            log.info("Server stopped");
            System.out.println("""
                    
                    
                    
                    ########################################
                    ##             Bye Bye ;)            ##
                    ########################################
                    """);
        }));
    }

    private static void printPromo(int sleep) {
        var msg = """
                 
                 ##########     (Si vis pacem, para bellum)     ##########
                >>> ccs1201 follow on linkedysnei -> https://www.linkedin.com/in/ccs1201/
                >>> follow on  github -> https://github.com/ccs1201
                """;
        System.out.println(msg);
        try {
            Thread.sleep(sleep * 1000L);
        } catch (Exception e) {
            log.error("Isto realmente não deveria acontece :(", e);
        }
    }

}
