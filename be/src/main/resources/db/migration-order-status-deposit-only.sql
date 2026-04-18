-- Chạy một lần trên DB đã có dữ liệu cũ (enum/string status).
-- Đồng bộ với luồng chỉ đặt cọc: bỏ PENDING_PAYMENT, PAID_FULL.
--
-- PENDING_PAYMENT: coi như đơn đã giữ chỗ — map sang DEPOSITED (kiểm tra lại tồn kho thủ công nếu cần).
-- PAID_FULL: thanh toán đủ — map sang DELIVERED (đã bán xong).

UPDATE car_orders SET status = 'DEPOSITED' WHERE status = 'PENDING_PAYMENT';
UPDATE car_orders SET status = 'DELIVERED' WHERE status = 'PAID_FULL';
