-- Thêm mã BIN ngân hàng (6 số NAPAS) để tạo VietQR. Chạy một lần trên DB đã tạo trước đó.

ALTER TABLE store_bank_accounts ADD bank_bin NVARCHAR(10) NULL;
