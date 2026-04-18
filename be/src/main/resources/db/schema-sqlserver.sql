-- Chạy lại toàn bộ: xóa database fcar cũ (nếu có) rồi tạo mới
USE master;
GO

IF DB_ID('fcar') IS NOT NULL
BEGIN
    ALTER DATABASE fcar SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE fcar;
END
GO

CREATE DATABASE fcar;
GO

USE fcar;
GO

-- Bảng users: lưu thông tin tài khoản người dùng
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(150) NOT NULL,
    phone NVARCHAR(20) NULL,
    email NVARCHAR(150) UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    avatar_url NVARCHAR(255),
    gender NVARCHAR(10),
    birth_date DATE,
    address NVARCHAR(255),
    enabled BIT NOT NULL DEFAULT 1,
    locked_by_admin BIT NOT NULL DEFAULT 0,
    locked_until DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

-- Bảng user_roles: lưu vai trò (ADMIN, CUSTOMER) của từng người dùng
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role NVARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Bảng user_phones: lưu các số điện thoại phụ của người dùng
CREATE TABLE user_phones (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    phone NVARCHAR(20) NOT NULL,
    verified BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_user_phones_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Bảng branches: lưu danh sách chi nhánh showroom
CREATE TABLE branches (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL UNIQUE,
    address NVARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(10) NOT NULL UNIQUE
        CHECK (phone NOT LIKE '%[^0-9]%' AND LEN(phone) = 10 AND phone LIKE '0%'),
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    deleted BIT NOT NULL DEFAULT 0
);

-- Bảng brands: lưu thương hiệu xe (Toyota, Honda, ...) 
CREATE TABLE brands (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL UNIQUE,
    active BIT NOT NULL DEFAULT 1,
    deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

-- Bảng car_models: lưu dòng xe theo từng thương hiệu (Vios, Civic, ...) 
CREATE TABLE car_models (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_models_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT uq_car_models_brand_name UNIQUE (brand_id, name)
);

-- Bảng segments: lưu phân khúc xe (gắn với từng dòng xe cụ thể, VD: Vios có 1.5E-MT, 1.5E-CVT)
CREATE TABLE segments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_model_id BIGINT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_segments_car_model FOREIGN KEY (car_model_id) REFERENCES car_models (id),
    CONSTRAINT uq_segments_car_model_name UNIQUE (car_model_id, name)
);

-- Bảng car_definitions: mẫu xe = thương hiệu + dòng xe + phân khúc + năm (VD: Toyota Vios 1.5E-MT 2026)
-- Các cột khác (body_type, seats, màu, ...) là thông số chung của mẫu xe
CREATE TABLE car_definitions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    segment_id BIGINT NOT NULL,
    production_year INT NOT NULL,
    body_type NVARCHAR(30) NOT NULL,
    fuel_type NVARCHAR(50),
    seats INT NOT NULL,
    sale_price DECIMAL(18,2) NOT NULL,
    promo_price DECIMAL(18,2) NULL,
    horsepower INT NULL,
    description NVARCHAR(MAX),
    active BIT NOT NULL DEFAULT 1,
    deleted BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_definitions_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT fk_car_definitions_model FOREIGN KEY (model_id) REFERENCES car_models (id),
    CONSTRAINT fk_car_definitions_segment FOREIGN KEY (segment_id) REFERENCES segments (id),
    CONSTRAINT uq_car_definitions_variant UNIQUE (brand_id, model_id, segment_id, production_year)
);

-- Bảng car_color: màu sắc theo từng mẫu xe
CREATE TABLE car_color (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_definition_id BIGINT NOT NULL,
    color_value NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_color_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id)
);

-- Bảng car_images: lưu các ảnh của mỗi mẫu xe
CREATE TABLE car_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_definition_id BIGINT NOT NULL,
    image_url NVARCHAR(255) NOT NULL,
    is_cover BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_images_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id)
);

-- Bảng car_attributes: lưu các thông số khác của mẫu xe
CREATE TABLE car_attributes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_definition_id BIGINT NOT NULL,
    attr_name NVARCHAR(50) NOT NULL,
    attr_value NVARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_attributes_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id)
);

-- Bảng car_inventory: tồn kho (1 dòng / mẫu xe + chi nhánh + màu). Giá/ngày nhập từng lần nằm ở car_import_history.
CREATE TABLE car_inventory (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_definition_id BIGINT NOT NULL,
    car_color_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    disabled BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_inventory_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id),
    CONSTRAINT fk_car_inventory_car_color FOREIGN KEY (car_color_id) REFERENCES car_color (id),
    CONSTRAINT fk_car_inventory_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT uq_car_inventory_def_branch_color UNIQUE (car_definition_id, branch_id, car_color_id)
);

-- DB đã tạo trước khi có car_color_id:
-- ALTER TABLE car_inventory ADD car_color_id BIGINT NULL;
-- (cập nhật từng dòng theo nghiệp vụ, rồi)
-- ALTER TABLE car_inventory ALTER COLUMN car_color_id BIGINT NOT NULL;
-- ALTER TABLE car_inventory ADD CONSTRAINT fk_car_inventory_car_color FOREIGN KEY (car_color_id) REFERENCES car_color (id);

-- DB cũ (xe đã qua sử dụng): gỡ cột
-- ALTER TABLE car_inventory DROP COLUMN is_used;
-- ALTER TABLE car_inventory DROP COLUMN mileage_km;
-- ALTER TABLE car_inventory DROP COLUMN documents_image_url;
-- ALTER TABLE car_inventory DROP COLUMN condition_text;
-- ALTER TABLE car_inventory DROP COLUMN maintenance_status;

-- Bảng favorites: lưu danh sách xe yêu thích của người dùng
CREATE TABLE favorites (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_inventory_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_favorites_inventory FOREIGN KEY (car_inventory_id) REFERENCES car_inventory (id)
);

-- Bảng car_orders: lưu đơn đặt mua xe của khách hàng
CREATE TABLE car_orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_inventory_id BIGINT NOT NULL,
    status NVARCHAR(30) NOT NULL,
    total_price DECIMAL(18,2) NOT NULL,
    deposit_amount DECIMAL(18,2) NULL,
    paid_amount DECIMAL(18,2) NULL,
    refunded_amount DECIMAL(18,2) NULL,
    refund_method NVARCHAR(50) NULL,
    refund_bank_info NVARCHAR(255) NULL,
    refund_proof_image_url NVARCHAR(512) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_car_orders_inventory FOREIGN KEY (car_inventory_id) REFERENCES car_inventory (id)
);

-- Bảng contact_requests: lưu yêu cầu liên hệ tư vấn về xe
CREATE TABLE contact_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_inventory_id BIGINT NOT NULL,
    branch_id BIGINT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_contact_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_contact_requests_inventory FOREIGN KEY (car_inventory_id) REFERENCES car_inventory (id),
    CONSTRAINT fk_contact_requests_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

-- Bảng test_drive_bookings: lưu lịch đăng ký lái thử xe
CREATE TABLE test_drive_bookings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_inventory_id BIGINT NOT NULL,
    branch_id BIGINT NULL,
    test_datetime DATETIME2 NOT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_test_drive_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_test_drive_inventory FOREIGN KEY (car_inventory_id) REFERENCES car_inventory (id),
    CONSTRAINT fk_test_drive_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

-- Bảng car_reviews: lưu đánh giá và nhận xét của người dùng về xe
CREATE TABLE car_reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_inventory_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(MAX) NULL,
    hidden BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_car_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_car_reviews_inventory FOREIGN KEY (car_inventory_id) REFERENCES car_inventory (id),
    CONSTRAINT fk_car_reviews_order FOREIGN KEY (order_id) REFERENCES car_orders (id)
);

-- Bảng payment_config: lưu cấu hình thanh toán (tỷ lệ đặt cọc)
CREATE TABLE payment_config (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    deposit_percent DECIMAL(5,2) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

-- Bảng support_hotlines: lưu danh sách số điện thoại hỗ trợ khách hàng
CREATE TABLE support_hotlines (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    phone_number NVARCHAR(30) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

-- Bảng store_bank_accounts: lưu tài khoản ngân hàng nhận thanh toán của showroom
CREATE TABLE store_bank_accounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    bank_name NVARCHAR(100) NOT NULL,
    account_name NVARCHAR(150) NOT NULL,
    account_number NVARCHAR(50) NOT NULL,
    bank_bin NVARCHAR(10) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

-- Bảng car_import_history: lịch sử nhập xe mới vào kho
CREATE TABLE car_import_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    car_definition_id BIGINT NOT NULL,
    car_color_id BIGINT NOT NULL,
    branch_id BIGINT NULL,
    purchase_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    import_date DATE NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_import_history_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id),
    CONSTRAINT fk_import_history_car_color FOREIGN KEY (car_color_id) REFERENCES car_color (id),
    CONSTRAINT fk_import_history_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

-- DB đã tạo trước khi có car_color_id trên car_import_history: tương tự car_inventory

-- DB cũ: gỡ cột (nếu còn)
-- ALTER TABLE car_import_history DROP COLUMN sale_price;
-- ALTER TABLE car_import_history DROP COLUMN promo_price;
-- ALTER TABLE car_import_history DROP COLUMN is_used;
-- ALTER TABLE car_import_history DROP COLUMN mileage_km;
-- ALTER TABLE car_import_history DROP COLUMN documents_image_url;
-- ALTER TABLE car_import_history DROP COLUMN condition_text;
-- ALTER TABLE car_import_history DROP COLUMN maintenance_status;

-- Bảng otp_tokens: lưu mã OTP gửi cho người dùng (xác thực, quên mật khẩu, ...) 
CREATE TABLE otp_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NULL,
    type NVARCHAR(30) NOT NULL,
    target NVARCHAR(150) NOT NULL,
    code NVARCHAR(10) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    send_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME2 NULL,
    used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_otp_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- PayOS: mã đơn gửi PayOS (INT duy nhất) + phiên đặt cọc chờ thanh toán
CREATE SEQUENCE dbo.seq_payos_order_code
    AS INT
    START WITH 10000001
    INCREMENT BY 1
    MINVALUE 10000001
    MAXVALUE 2147483647;

CREATE TABLE payos_deposit_sessions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    payos_order_code INT NOT NULL,
    user_id BIGINT NOT NULL,
    car_definition_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    payment_link_id NVARCHAR(64) NULL,
    checkout_url NVARCHAR(1024) NULL,
    car_order_id BIGINT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT uq_payos_deposit_sessions_order_code UNIQUE (payos_order_code),
    CONSTRAINT fk_payos_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payos_sessions_definition FOREIGN KEY (car_definition_id) REFERENCES car_definitions (id),
    CONSTRAINT fk_payos_sessions_car_order FOREIGN KEY (car_order_id) REFERENCES car_orders (id)
);

CREATE INDEX ix_payos_deposit_sessions_user ON payos_deposit_sessions (user_id);
CREATE INDEX ix_payos_deposit_sessions_status ON payos_deposit_sessions (status);

-- ======================================================================
-- Indexes for common queries
-- ======================================================================

CREATE UNIQUE INDEX IX_users_email ON users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IX_users_phone ON users(phone) WHERE phone IS NOT NULL;

CREATE INDEX IX_car_inventory_def_branch_disabled
    ON car_inventory(car_definition_id, branch_id, disabled);

CREATE INDEX IX_car_orders_user_status_created_at
    ON car_orders(user_id, status, created_at);

CREATE INDEX IX_contact_requests_user_status_created_at
    ON contact_requests(user_id, status, created_at);

CREATE INDEX IX_test_drive_bookings_user_status_datetime
    ON test_drive_bookings(user_id, status, test_datetime);

CREATE INDEX IX_otp_tokens_user_type_target
    ON otp_tokens(user_id, type, target);
