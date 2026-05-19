ALTER TABLE return_exchange_requests
    ADD COLUMN IF NOT EXISTS gateway_refund_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS refund_processed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS refund_failure_reason VARCHAR(1000);

ALTER TABLE return_exchange_requests
    DROP CONSTRAINT IF EXISTS chk_return_exchange_refund_status;

ALTER TABLE return_exchange_requests
    ADD CONSTRAINT chk_return_exchange_refund_status
        CHECK (refund_status IN (
            'NOT_REQUIRED',
            'REFUND_PROCESSING',
            'MANUAL_REFUND_REQUIRED',
            'REFUNDED',
            'REFUND_FAILED'
        ));

CREATE INDEX IF NOT EXISTS idx_return_exchange_approved_at
ON return_exchange_requests(approved_at);