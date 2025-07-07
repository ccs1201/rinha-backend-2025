package br.com.ccs.rinha.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class PaymentController {

    private static final ResponseEntity<Void> response = ResponseEntity.accepted().build();

    @PostMapping("payments")
    public ResponseEntity<Void> createPayment(@RequestBody String body) {

        System.out.println(body);


        return response;
    }

    @GetMapping("payments-summary")
    public ResponseEntity<Void> getPaymentsSummary(@RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        System.out.println(from);
        System.out.println(to);
        return response;
    }

}
