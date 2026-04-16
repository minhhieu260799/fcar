-- Ảnh chứng từ hoàn tiền; bỏ cột tỉ lệ % hoàn (hoàn = 100% tiền cọc, xử lý trong code)
IF COL_LENGTH('dbo.car_orders', 'refund_proof_image_url') IS NULL
BEGIN
    ALTER TABLE dbo.car_orders ADD refund_proof_image_url NVARCHAR(512) NULL;
END
GO

IF COL_LENGTH('dbo.payment_config', 'refund_after_deposit_percent') IS NOT NULL
BEGIN
    ALTER TABLE dbo.payment_config DROP COLUMN refund_after_deposit_percent;
END
GO
