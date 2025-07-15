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
        int serverPort = Objects.isNull(envPort) ? 8080 : Integer.parseInt(envPort);

        log.info("Starting server on port {}", serverPort);

        Undertow server = Undertow.builder()
                .addHttpListener(serverPort, "0.0.0.0")
                .setHandler(Handler.getInstance())
                .setIoThreads(2)
                .setWorkerThreads(30)
                .setBufferSize(1024 * 16 -20)
                .build();
        server.start();
        log.info("Server started! Let's play @RinhaDeBackend");
    }

}
