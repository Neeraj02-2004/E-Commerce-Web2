ALTER TABLE order_items
    ALTER COLUMN product_id TYPE BIGINT;

ALTER TABLE wishlist_items
    ALTER COLUMN product_id TYPE BIGINT;

ALTER TABLE product
    ALTER COLUMN id TYPE BIGINT;