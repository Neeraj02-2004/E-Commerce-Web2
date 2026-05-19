ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_payment_mode;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_mode
    CHECK (payment_mode IN ('CASH_ON_DELIVERY', 'ONLINE'));