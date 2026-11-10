CREATE TABLE inbox_events (
    event_id VARCHAR(255) NOT NULL CONSTRAINT pk_inbox_events PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    received_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_inbox_events_received_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    processed_at DATETIMEOFFSET(6) NULL,
    status VARCHAR(32) NOT NULL CONSTRAINT df_inbox_events_status DEFAULT 'PROCESSED'
);

CREATE TABLE outbox_events (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_outbox_events PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_outbox_events_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    published_at DATETIMEOFFSET(6) NULL,
    publish_attempts INT NOT NULL CONSTRAINT df_outbox_events_publish_attempts DEFAULT (0),
    last_error NVARCHAR(2000) NULL
);

CREATE INDEX ix_outbox_events_pending ON outbox_events (published_at, created_at);
