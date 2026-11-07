CREATE TABLE payments (
  id UUID PRIMARY KEY,
  booking_id UUID NOT NULL UNIQUE,
  payer_id UUID NOT NULL,
  mentor_id UUID NOT NULL,
  amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
  currency CHAR(3) NOT NULL,
  status VARCHAR(24) NOT NULL CONSTRAINT ck_payment_status CHECK (status IN ('PENDING','PAID','FAILED')),
  idempotency_key VARCHAR(128) NOT NULL,
  provider_reference_id VARCHAR(255) UNIQUE,
  provider_transaction_id VARCHAR(255),
  checkout_url VARCHAR(2048),
  expires_at TIMESTAMPTZ,
  checkout_attempt INTEGER NOT NULL DEFAULT 0,
  failure_reason VARCHAR(1000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_payment_payer_idempotency UNIQUE (payer_id,idempotency_key)
);
CREATE INDEX ix_payment_payer_created ON payments (payer_id,created_at DESC);
CREATE INDEX ix_payment_status_expiry ON payments (status,expires_at);
