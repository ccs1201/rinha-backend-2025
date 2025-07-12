CREATE TABLE payments
(
    correlation_id UUID           NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    requested_at   TIMESTAMP      NOT NULL,
    is_default     BOOLEAN        NOT NULL
);

CREATE INDEX idx_payments_requested_at ON payments (requested_at);
-- CREATE INDEX idx_payments_req_at_default_true ON payments (requested_at) WHERE is_default = true;
-- CREATE INDEX idx_payments_req_at_default_false ON payments (requested_at) WHERE is_default = false;
CREATE INDEX idx_payments_req_isdef_amt ON payments (requested_at, is_default, amount);

