ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gateway_signature VARCHAR(500),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status IS NULL;

ALTER TABLE orders
    ALTER COLUMN payment_status SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_status
    CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_orders_gateway_order_id
ON orders(gateway_order_id);