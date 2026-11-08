CREATE TABLE payment_attempts (
  id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_payment_attempts PRIMARY KEY,
  payment_id UNIQUEIDENTIFIER NOT NULL,
  attempt_number INT NOT NULL,
  status VARCHAR(24) NOT NULL,
  provider_reference_id VARCHAR(255) NULL,
  checkout_url VARCHAR(2048) NULL,
  expires_at DATETIMEOFFSET(6) NULL,
  failure_reason NVARCHAR(1000) NULL,
  created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_payment_attempts_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
  CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
  CONSTRAINT uk_payment_attempt_number UNIQUE (payment_id, attempt_number),
  CONSTRAINT uk_payment_attempt_provider_reference UNIQUE (provider_reference_id),
  CONSTRAINT ck_payment_attempt_status CHECK (status IN ('PENDING','COMPLETED','FAILED','EXPIRED'))
);
CREATE INDEX ix_payment_attempts_payment_created ON payment_attempts (payment_id, created_at DESC);

CREATE TABLE webhook_receipts (
  provider_event_id VARCHAR(255) NOT NULL CONSTRAINT pk_webhook_receipts PRIMARY KEY,
  event_type VARCHAR(120) NOT NULL,
  received_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_webhook_receipts_received_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00')
);

CREATE TABLE refund_transactions (
  id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_refund_transactions PRIMARY KEY,
  payment_id UNIQUEIDENTIFIER NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  provider_refund_id VARCHAR(255) NOT NULL,
  amount DECIMAL(12,2) NOT NULL CONSTRAINT ck_refund_amount_positive CHECK (amount > 0),
  created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_refund_transactions_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
  CONSTRAINT fk_refund_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
  CONSTRAINT uk_refund_idempotency_key UNIQUE (idempotency_key),
  CONSTRAINT uk_provider_refund UNIQUE (provider_refund_id)
);
CREATE INDEX ix_refund_transactions_payment_created ON refund_transactions (payment_id, created_at DESC);
