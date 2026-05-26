CREATE TABLE IF NOT EXISTS razorpay_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    raw_body TEXT NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_razorpay_webhook_events_event_type
ON razorpay_webhook_events(event_type);

CREATE INDEX IF NOT EXISTS idx_razorpay_webhook_events_processed_at
ON razorpay_webhook_events(processed_at);