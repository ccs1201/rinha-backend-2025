package br.com.ccs.rinha.exception;

public class HttpClientException extends RuntimeException {
    public HttpClientException(Exception e) {
        super(e);
    }
}
