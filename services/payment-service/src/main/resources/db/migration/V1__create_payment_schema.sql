CREATE TABLE payments (
  id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_payments PRIMARY KEY,
  booking_id UNIQUEIDENTIFIER NOT NULL,
  payer_id UNIQUEIDENTIFIER NOT NULL,
  mentor_id UNIQUEIDENTIFIER NOT NULL,
  amount DECIMAL(12,2) NOT NULL CONSTRAINT ck_payment_amount_positive CHECK (amount > 0),
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(24) NOT NULL CONSTRAINT ck_payment_status CHECK (status IN ('PENDING','PAID','FAILED','PARTIALLY_REFUNDED','REFUNDED')),
  refunded_amount DECIMAL(12,2) NOT NULL CONSTRAINT df_payment_refunded_amount DEFAULT (0)
    CONSTRAINT ck_payment_refunded_amount CHECK (refunded_amount >= 0),
  idempotency_key VARCHAR(128) NOT NULL,
  provider_reference_id VARCHAR(255) NULL,
  provider_transaction_id VARCHAR(255) NULL,
  checkout_url VARCHAR(2048) NULL,
  expires_at DATETIMEOFFSET(6) NULL,
  checkout_attempt INT NOT NULL CONSTRAINT df_payment_checkout_attempt DEFAULT 0,
  failure_reason NVARCHAR(1000) NULL,
  created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_payments_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
  updated_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_payments_updated_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
  CONSTRAINT uk_payment_booking UNIQUE (booking_id),
  CONSTRAINT uk_payment_payer_idempotency UNIQUE (payer_id, idempotency_key)
);

CREATE UNIQUE INDEX uk_payment_provider_reference ON payments (provider_reference_id)
  WHERE provider_reference_id IS NOT NULL;
CREATE INDEX ix_payment_payer_created ON payments (payer_id, created_at DESC);
CREATE INDEX ix_payment_mentor_created ON payments (mentor_id, created_at DESC);
CREATE INDEX ix_payment_status_expiry ON payments (status, expires_at);
