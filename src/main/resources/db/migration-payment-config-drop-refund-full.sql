-- DB đã tạo trước khi gỡ luồng thanh toán toàn bộ: xóa cột không còn dùng.
-- Chạy một lần trên SQL Server. Nếu cột đã không tồn tại, bỏ qua lỗi hoặc bọc kiểm tra.

ALTER TABLE payment_config DROP COLUMN refund_after_full_percent;
