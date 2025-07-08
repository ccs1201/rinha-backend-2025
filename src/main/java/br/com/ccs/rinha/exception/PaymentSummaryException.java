package br.com.ccs.rinha.exception;

public class PaymentSummaryException extends RuntimeException {
    public PaymentSummaryException(Exception e) {
        super(e);
    }
}
