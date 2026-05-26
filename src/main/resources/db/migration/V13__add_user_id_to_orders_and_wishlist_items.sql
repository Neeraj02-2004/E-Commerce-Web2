ALTER TABLE orders
    ADD COLUMN user_id INTEGER;

ALTER TABLE wishlist_items
    ADD COLUMN user_id INTEGER;

UPDATE orders o
SET user_id = u.id
FROM users u
WHERE lower(o.user_email) = lower(u.email);

UPDATE wishlist_items w
SET user_id = u.id
FROM users u
WHERE lower(w.user_email) = lower(u.email);

DELETE FROM wishlist_items
WHERE user_id IS NULL;

DELETE FROM orders
WHERE user_id IS NULL;

ALTER TABLE orders
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE wishlist_items
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id);

ALTER TABLE wishlist_items
    ADD CONSTRAINT fk_wishlist_items_user
        FOREIGN KEY (user_id)
        REFERENCES users(id);

ALTER TABLE wishlist_items
    DROP CONSTRAINT IF EXISTS wishlist_items_user_email_product_id_key;

ALTER TABLE wishlist_items
    ADD CONSTRAINT uk_wishlist_items_user_product
        UNIQUE (user_id, product_id);

CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders(user_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_id
    ON wishlist_items(user_id);

CREATE INDEX IF NOT EXISTS idx_orders_user_email
    ON orders(user_email);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_email
    ON wishlist_items(user_email);