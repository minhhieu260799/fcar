-- Chạy một lần trên database đã có sẵn (khi users.phone đang NOT NULL).
-- Cho phép đăng ký / đăng nhập Google không có SĐT; unique chỉ áp dụng khi phone có giá trị.

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_users_phone' AND object_id = OBJECT_ID(N'dbo.users'))
    DROP INDEX IX_users_phone ON dbo.users;
GO

ALTER TABLE dbo.users ALTER COLUMN phone NVARCHAR(20) NULL;
GO

CREATE UNIQUE INDEX IX_users_phone ON dbo.users(phone) WHERE phone IS NOT NULL;
GO
