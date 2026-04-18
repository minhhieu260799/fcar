-- Bỏ cột avatar_url trên car_definitions (ảnh đại diện chỉ dùng bảng car_images, is_cover).
-- Chạy nếu DB đã từng có migration thêm avatar_url.
IF COL_LENGTH('car_definitions', 'avatar_url') IS NOT NULL
BEGIN
    ALTER TABLE car_definitions DROP COLUMN avatar_url;
END
GO
