CREATE TABLE IF NOT EXISTS payments
(
    correlation_id UUID           NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    requested_at   TIMESTAMP    NOT NULL,
    is_default     BOOLEAN        NOT NULL
);

-- CREATE INDEX idx_payments_requested_at ON payments (is_default, requested_at);
-- CREATE INDEX idx_payments_req_at_default_true ON payments (requested_at) WHERE is_default = true,
-- CREATE INDEX idx_payments_req_at_default_false ON payments (requested_at) WHERE is_default = false,
CREATE INDEX idx_payments_req_isdef_amt ON payments (requested_at, is_default, amount);
-- -- Covering index for summary queries
-- CREATE INDEX idx_payments_covering ON payments (requested_at, is_default) INCLUDE (amount);