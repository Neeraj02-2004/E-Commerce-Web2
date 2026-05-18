ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS address VARCHAR(500);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(50);

UPDATE orders
SET address = 'Address not provided'
WHERE address IS NULL;

UPDATE orders
SET payment_mode = 'CASH_ON_DELIVERY'
WHERE payment_mode IS NULL;

ALTER TABLE orders
    ALTER COLUMN address SET NOT NULL;

ALTER TABLE orders
    ALTER COLUMN payment_mode SET NOT NULL;

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_payment_mode;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_mode
    CHECK (payment_mode IN ('CASH_ON_DELIVERY'));