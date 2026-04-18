-- PayOS: phiên đặt cọc chờ thanh toán (orderCode gửi lên PayOS phải là số nguyên duy nhất)
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'seq_payos_order_code' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE SEQUENCE dbo.seq_payos_order_code
        AS INT
        START WITH 10000001
        INCREMENT BY 1
        MINVALUE 10000001
        MAXVALUE 2147483647;
END
GO

IF OBJECT_ID(N'dbo.payos_deposit_sessions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payos_deposit_sessions (
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
        CONSTRAINT fk_payos_sessions_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
        CONSTRAINT fk_payos_sessions_definition FOREIGN KEY (car_definition_id) REFERENCES dbo.car_definitions (id),
        CONSTRAINT fk_payos_sessions_car_order FOREIGN KEY (car_order_id) REFERENCES dbo.car_orders (id)
    );
    CREATE INDEX ix_payos_deposit_sessions_user ON dbo.payos_deposit_sessions (user_id);
    CREATE INDEX ix_payos_deposit_sessions_status ON dbo.payos_deposit_sessions (status);
END
GO
