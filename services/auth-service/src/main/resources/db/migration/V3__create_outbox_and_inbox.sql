-- Transactional outbox: events are written in the same transaction as the account change and published
-- to RabbitMQ (exchange mentor.events) afterwards by OutboxPublisher. published_at NULL = still to send.
CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(200) NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_events_pending (published_at, created_at),
    KEY idx_outbox_events_aggregate (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

-- Inbox: events consumed from other services (MentorApproved). Written in the same transaction as the
-- effect, so a redelivered event is detected and applied only once.
CREATE TABLE inbox_events (
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
