-- =============================================================================
-- Migration: car_inventory = tồn kho (bỏ purchase_price, import_date; unique def+branch+color)
-- Chạy trên DB ĐÃ TỒN TẠI. Sao lưu trước khi thực hiện.
-- =============================================================================
-- Lưu ý:
-- 1) Nếu có nhiều dòng trùng (cùng car_definition_id, branch_id, car_color_id), phải GỘP
--    quantity và chuyển mọi FK (car_orders, favorites, ...) sang một id trước khi ADD UNIQUE.
--    Có thể dùng chức năng trong ứng dụng hoặc script DBA tùy dữ liệu.
-- 2) Nếu branch_id NULL: gán chi nhánh mặc định trước (điều chỉnh theo nghiệp vụ):
--    UPDATE car_inventory SET branch_id = (SELECT TOP 1 id FROM branches WHERE deleted = 0 ORDER BY id)
--    WHERE branch_id IS NULL;
-- 3) Sau khi không còn trùng và branch_id NOT NULL:
/*
ALTER TABLE car_inventory DROP CONSTRAINT ... -- nếu có tên FK cũ, giữ nguyên các FK hiện có

ALTER TABLE car_inventory DROP COLUMN purchase_price;
ALTER TABLE car_inventory DROP COLUMN import_date;

ALTER TABLE car_inventory ALTER COLUMN branch_id BIGINT NOT NULL;

ALTER TABLE car_inventory ADD CONSTRAINT uq_car_inventory_def_branch_color
    UNIQUE (car_definition_id, branch_id, car_color_id);
*/
-- =============================================================================
