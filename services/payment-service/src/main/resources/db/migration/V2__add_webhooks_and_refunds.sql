ALTER TABLE payments DROP CONSTRAINT ck_payment_status;
ALTER TABLE payments ADD CONSTRAINT ck_payment_status
  CHECK (status IN ('PENDING','PAID','FAILED','PARTIALLY_REFUNDED','REFUNDED'));
ALTER TABLE payments ADD COLUMN refunded_amount NUMERIC(12,2) NOT NULL DEFAULT 0
  CHECK (refunded_amount >= 0);

CREATE TABLE stripe_webhook_events (
  event_id VARCHAR(255) PRIMARY KEY,
  event_type VARCHAR(120) NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refund_transactions (
  id UUID PRIMARY KEY,
  payment_id UUID NOT NULL REFERENCES payments(id),
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  provider_refund_id VARCHAR(255) NOT NULL,
  amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
