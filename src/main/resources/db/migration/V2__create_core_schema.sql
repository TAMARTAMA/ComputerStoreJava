-- Core domain schema for the Computer Store Management API.
-- Enums are stored as VARCHAR columns guarded by CHECK constraints so the values
-- stay readable and portable. All DATETIME(6) values are written in UTC
-- (hibernate.jdbc.time_zone=UTC and serverTimezone=UTC on the JDBC URL).

CREATE TABLE user_accounts (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email),
    CONSTRAINT ck_user_accounts_role CHECK (role IN ('ADMIN', 'CUSTOMER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- An ADMIN account needs no customer profile, so the profile is optional here.
-- The unique key on user_account_id keeps the relationship one-to-one.
CREATE TABLE customers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT       NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30)  NULL,
    address         VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_customers_user_account FOREIGN KEY (user_account_id) REFERENCES user_accounts (id),
    KEY idx_customers_last_name (last_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE products (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    sku            VARCHAR(64)    NOT NULL,
    name           VARCHAR(150)   NOT NULL,
    description    VARCHAR(1000)  NULL,
    price          DECIMAL(12, 2) NOT NULL,
    stock_quantity INT            NOT NULL DEFAULT 0,
    category       VARCHAR(20)    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_category CHECK (category IN ('HARDWARE', 'SOFTWARE')),
    KEY idx_products_category (category),
    KEY idx_products_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE purchase_orders (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id  BIGINT         NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    created_at   DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_purchase_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_purchase_orders_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_purchase_orders_total_non_negative CHECK (total_amount >= 0),
    KEY idx_purchase_orders_customer_created (customer_id, created_at),
    KEY idx_purchase_orders_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Items belong to their order and are removed with it; products are never
-- deleted through an order, so that foreign key restricts instead.
CREATE TABLE order_items (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_order_id BIGINT         NOT NULL,
    product_id        BIGINT         NOT NULL,
    quantity          INT            NOT NULL,
    unit_price        DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    KEY idx_order_items_purchase_order (purchase_order_id),
    KEY idx_order_items_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
