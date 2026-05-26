ALTER TABLE return_exchange_requests
    ADD COLUMN IF NOT EXISTS refund_idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS ux_return_exchange_refund_idempotency_key
ON return_exchange_requests(refund_idempotency_key)
WHERE refund_idempotency_key IS NOT NULL;