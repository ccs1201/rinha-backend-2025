package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.api.model.input.PaymentRequest;
import br.com.ccs.rinha.api.model.output.PaymentSummary;

import java.time.OffsetDateTime;

public interface PaymentRepository {

    void save(PaymentRequest request);

    PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to);

    void purge();

}