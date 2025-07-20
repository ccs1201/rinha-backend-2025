package br.com.ccs.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@RegisterReflectionForBinding({RedisConfig.class,
//        ExecutorConfig.class,
//        RestTemplateConfig.class,
//        PaymentController.class,
//        PaymentProcessorClient.class,
//        RedisPaymentRepository.class
//})
public class RinhaApp {

    public static void main(String[] args) {
        SpringApplication.run(RinhaApp.class, args);
    }

}
