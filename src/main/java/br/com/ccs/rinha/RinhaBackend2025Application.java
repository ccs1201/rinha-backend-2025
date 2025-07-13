package br.com.ccs.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RinhaBackend2025Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RinhaBackend2025Application.class);
        app.setLogStartupInfo(false);
        app.setRegisterShutdownHook(false);
        app.run(args);
    }
}
