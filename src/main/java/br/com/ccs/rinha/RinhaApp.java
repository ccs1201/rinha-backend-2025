package br.com.ccs.rinha;

import br.com.ccs.rinha.api.handler.Handler;
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

        log.info("Starting server on port {}", serverPort);
        printPromo(10);

        Undertow server = Undertow.builder()
                .addHttpListener(serverPort, "0.0.0.0")
                .setHandler(Handler.getInstance())
                .setIoThreads(serverIOThreads)
                .setWorkerThreads(serverWorkerThreads)
                .setDirectBuffers(true)
                .setBufferSize(1024 * 16 - 20)
                .build();
        server.start();
        printPromo(5);
        log.info("Server started! Let's play @RinhaDeBackend");
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
