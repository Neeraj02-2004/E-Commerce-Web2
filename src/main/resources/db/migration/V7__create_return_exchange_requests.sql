ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS return_exchange_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(80) NOT NULL UNIQUE,
    order_db_id BIGINT NOT NULL,
    order_id VARCHAR(80) NOT NULL,
    user_email VARCHAR(150) NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    refund_status VARCHAR(40) NOT NULL,
    admin_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_return_exchange_order
        FOREIGN KEY (order_db_id)
        REFERENCES orders(id),

    CONSTRAINT chk_return_exchange_type
        CHECK (request_type IN ('RETURN', 'EXCHANGE')),

    CONSTRAINT chk_return_exchange_status
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED')),

    CONSTRAINT chk_return_exchange_refund_status
        CHECK (refund_status IN ('NOT_REQUIRED', 'REFUND_PROCESSING', 'MANUAL_REFUND_REQUIRED', 'REFUNDED'))
);

CREATE INDEX IF NOT EXISTS idx_return_exchange_user_email
ON return_exchange_requests(user_email);

CREATE INDEX IF NOT EXISTS idx_return_exchange_order_id
ON return_exchange_requests(order_id);

CREATE INDEX IF NOT EXISTS idx_return_exchange_status
ON return_exchange_requests(status);