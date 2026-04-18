-- Chạy một lần trên database đã có dữ liệu (sau khi bật duyệt đánh giá trong admin).
-- Đưa tất cả đánh giá hiện có về trạng thái chờ duyệt: không hiển thị trên trang xe cho đến khi admin
-- duyệt tại /admin/reviews (cột hidden = 1).

UPDATE dbo.car_reviews SET hidden = 1;
GO
