-- ============================================================
-- VYMS - Vehicle Yard Management System
-- Full Database Setup Script for PostgreSQL / Supabase
-- Database: postgres (default) or custom
-- ============================================================

-- ============================================================
-- 1. users table
-- ============================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(255),
    email         VARCHAR(255),
    password      VARCHAR(255),
    role          VARCHAR(20)         -- ADMIN | MANAGER
);

-- ============================================================
-- 2. vehicle table
-- ============================================================
CREATE TABLE vehicle (
    id             BIGSERIAL PRIMARY KEY,
    chassis_number VARCHAR(255) UNIQUE,
    license_plate  VARCHAR(255),
    vehicle_model  VARCHAR(255),
    purchase_price DECIMAL(19, 2),
    repair_cost    DECIMAL(19, 2),
    sale_price     DECIMAL(19, 2),
    status         VARCHAR(50),       -- UNSOLD | SOLD
    image_path     VARCHAR(255),
    make           VARCHAR(255),
    model          VARCHAR(255),
    year           INTEGER,
    vin            VARCHAR(255) UNIQUE
);

-- ============================================================
-- 3. repair table
-- ============================================================
CREATE TABLE repair (
    id          BIGSERIAL PRIMARY KEY,
    vehicle_id  BIGINT NOT NULL,
    description VARCHAR(255),
    cost        DECIMAL(19, 2),
    repair_date DATE,
    repair_type VARCHAR(50),          -- INTERNAL | EXTERNAL
    status      VARCHAR(50),          -- PENDING | INSPECTED
    CONSTRAINT FK_repair_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(id) ON DELETE CASCADE
);

-- ============================================================
-- 4. sale table
-- ============================================================
CREATE TABLE sale (
    id             BIGSERIAL PRIMARY KEY,
    vehicle_id     BIGINT UNIQUE,
    seller_id      BIGINT,
    sale_price     DECIMAL(19, 2),
    sale_date      DATE,
    buyer_type     VARCHAR(20),       -- REGULAR_CUSTOMER | REGULAR_COMPANY | AUCTION | EXPORT
    customer_name  VARCHAR(255),
    contact_number VARCHAR(255),
    email          VARCHAR(255),
    company_name   VARCHAR(255),
    company_address VARCHAR(255),
    company_contact_number VARCHAR(255),
    sale_status    VARCHAR(50),       -- DRAFT | FINALIZED
    total_cost     DECIMAL(19, 2),     -- Snapshot of purchasePrice + repairCost at sale time
    CONSTRAINT FK_sale_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT FK_sale_seller  FOREIGN KEY (seller_id)  REFERENCES users(id)
);

-- ============================================================
-- 5. system_log table
-- ============================================================
CREATE TABLE system_log (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(255),         -- USER_CREATED | LOGIN | DELETE | etc.
    description VARCHAR(500),
    actor       VARCHAR(255),         -- Username or email of the actor
    timestamp   TIMESTAMP WITHOUT TIME ZONE,
    status      VARCHAR(50)           -- SUCCESS | FAILURE
);

-- ============================================================
-- 6. inventory_recommendation table
-- ============================================================
CREATE TABLE inventory_recommendation (
    id                BIGSERIAL PRIMARY KEY,
    brand             VARCHAR(255),
    recommended_count INTEGER,
    score             INTEGER,
    reason            VARCHAR(500),
    computed_at       TIMESTAMP WITHOUT TIME ZONE
);

-- ============================================================
-- 7. uploaded_files table
-- ============================================================
CREATE TABLE uploaded_files (
    id            BIGSERIAL PRIMARY KEY,
    filename      VARCHAR(255) UNIQUE NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    file_data     BYTEA NOT NULL
);

-- ============================================================
-- SEED DATA: Default Admin User
-- Example below is BCrypt hash for "admin123"
-- ============================================================
INSERT INTO users (username, email, password, role)
VALUES (
    'admin',
    'admin@vyms.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- "admin123"
    'ADMIN'
);
