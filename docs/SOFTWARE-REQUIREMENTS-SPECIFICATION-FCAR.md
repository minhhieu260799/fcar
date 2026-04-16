# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
# HỆ THỐNG WEBSITE BÁN XE Ô TÔ – FCAR

---

## 1. GIỚI THIỆU

### 1.1 Mục đích tài liệu

Tài liệu SRS này mô tả đầy đủ và chi tiết các yêu cầu chức năng và phi chức năng của hệ thống FCar – Website bán xe ô tô trực tuyến, mô hình showroom 1 chủ (một đại lý/chủ cửa hàng duy nhất).

**Mục đích:**

- Làm cơ sở cho phân tích, thiết kế, lập trình, kiểm thử và triển khai hệ thống.
- Là tài liệu tham chiếu chính thức giữa:
  - Nhóm phát triển.
  - Giảng viên hướng dẫn / Hội đồng bảo vệ đồ án.
- Là tiêu chí đánh giá:
  - Mức độ hoàn thành hệ thống.
  - Mức độ đáp ứng yêu cầu nghiệp vụ.

### 1.2 Phạm vi hệ thống

FCar là hệ thống thương mại điện tử cho phép:

**Khách hàng (Customer):**

- Xem danh sách xe, xem chi tiết xe.
- Tìm kiếm, lọc, so sánh xe.
- Thêm xe vào giỏ hàng (tham khảo); **Mua ngay** hoặc **Đặt cọc** từng xe (mỗi đơn hàng chỉ một xe).
- Thanh toán online toàn bộ hoặc đặt cọc theo tỉ lệ cấu hình.
- Đặt lịch lái thử.
- Xem lịch sử đơn hàng, theo dõi trạng thái.
- Đánh giá, nhận xét xe.
- Quản lý danh sách xe yêu thích.

**Quản trị viên (Admin):**

- Quản lý người dùng, phân quyền.
- Quản lý danh mục (hãng xe, loại xe, showroom…).
- Quản lý xe (thêm/sửa/xóa, bật/tắt hiển thị).
- Nhập xe về kho, quản lý tồn kho.
- Quản lý đơn hàng, thanh toán.
- Quản lý lịch lái thử.
- Quản lý đánh giá.
- Xem báo cáo, thống kê.

**Hệ thống không bao gồm (ngoài phạm vi đồ án):**

- Tích hợp thực tế với hệ thống quản lý kho/hóa đơn của hãng xe bên ngoài.
- Tích hợp pháp lý hoàn chỉnh (hóa đơn VAT thật, ký hợp đồng điện tử…).

### 1.3 Định nghĩa, từ viết tắt

| Viết tắt | Ý nghĩa |
|----------|----------|
| SRS | Software Requirements Specification |
| Admin | Quản trị hệ thống |
| Customer | Khách hàng cuối |
| VIN | Vehicle Identification Number |
| Payment Gateway | Cổng thanh toán trực tuyến (VNPay, MoMo…) |
| MVC | Model – View – Controller |
| JWT | JSON Web Token |

### 1.4 Tài liệu tham khảo

- IEEE 830-1998 – Software Requirements Specification.
- IEEE 29148:2018 – Requirements Engineering.
- UML 2.x Specification.
- Tài liệu hướng dẫn đồ án tốt nghiệp (Khoa/CN/Trường …).

---

## 2. MÔ TẢ TỔNG QUAN

### 2.1 Quan điểm sản phẩm

- FCar là ứng dụng web-based, hoạt động trên:
  - Trình duyệt: Chrome, Edge, Firefox, Safari (các phiên bản hiện đại).
  - Thiết bị: Desktop/Laptop là chính, có hỗ trợ Mobile/Tablet (Responsive).
- Hệ thống tích hợp (ở mức mô phỏng hoặc sandbox):
  - Cổng thanh toán: VNPay, MoMo…
  - Email service: gửi email xác nhận đơn hàng, reset mật khẩu.
  - Google Maps API: hiển thị vị trí showroom, hỗ trợ chọn địa điểm lái thử.

### 2.2 Các chức năng chính (tổng quan)

- Quản lý tài khoản người dùng.
- Quản lý danh mục và xe.
- Tìm kiếm, lọc, so sánh xe.
- Giỏ hàng (lưu xe quan tâm); **Mua ngay / Đặt cọc** — mỗi đơn hàng một xe.
- Thanh toán online / đặt cọc.
- Đặt lịch lái thử.
- Quản lý đơn hàng.
- Đánh giá & nhận xét xe.
- Danh sách xe yêu thích (Wishlist).
- Nhập xe & quản lý tồn kho (Admin).
- Dashboard thống kê (Admin).

### 2.3 Đối tượng người dùng (Actor)

- **Guest:** Người truy cập chưa đăng nhập, chỉ xem được nội dung công khai.
- **Customer:** Người dùng đã đăng ký, có thể mua xe, đặt lịch, đánh giá, yêu thích…
- **Admin:** Quản trị viên hệ thống (bao gồm cả nhân viên quản lý kho, bán hàng).

### 2.4 Môi trường hoạt động

- **Backend:** Spring Boot (Java).
- **Frontend:** Thymeleaf (template engine) + HTML/CSS/JavaScript.
- **Database:** SQL Server.
- **Hệ điều hành server:** Windows Server hoặc Linux (tùy môi trường triển khai).
- **Trình duyệt client:** Hỗ trợ các phiên bản từ 2 năm gần nhất.

### 2.5 Ràng buộc hệ thống

- Phải bảo mật thông tin khách hàng (mật khẩu, email, số điện thoại, địa chỉ).
- Phải đảm bảo tính toàn vẹn dữ liệu (đơn hàng, thanh toán, lịch lái thử).
- Hệ thống hoạt động 24/7 trong phạm vi lab/thử nghiệm (không downtime kéo dài).
- Tuân thủ các quy định cơ bản về thương mại điện tử tại Việt Nam (mức mô phỏng).

---

## 3. YÊU CẦU CHỨC NĂNG

*Lưu ý: Các mã FR (Functional Requirement) được dùng để tham chiếu trong thiết kế, kiểm thử và tài liệu Use Case.*

### 3.1 QUẢN LÝ TÀI KHOẢN

#### FR-01: Đăng ký tài khoản

- **Actor:** Guest.
- **Mô tả:** Cho phép người dùng tạo tài khoản mới (Customer).
- **Input:**
  - Họ tên.
  - Email.
  - Số điện thoại.
  - Mật khẩu.
  - Xác nhận mật khẩu.
- **Xử lý:**
  - Kiểm tra định dạng email, số điện thoại, độ mạnh mật khẩu.
  - Kiểm tra email chưa tồn tại trong hệ thống.
  - Mã hóa mật khẩu bằng BCrypt.
  - Tạo tài khoản với trạng thái ban đầu là Inactive.
  - Gửi email xác thực chứa link kích hoạt tài khoản.
- **Output:** Thông báo đăng ký thành công, yêu cầu người dùng kiểm tra email để kích hoạt.
- **Pre-condition:** Người dùng chưa đăng nhập.
- **Post-condition:** Bản ghi mới được tạo trong bảng Users với trạng thái Inactive; sau khi người dùng click link kích hoạt, trạng thái chuyển sang Active.

#### FR-02: Đăng nhập

- **Actor:** Guest.
- **Input:** Email, Mật khẩu.
- **Xử lý:**
  - Kiểm tra tài khoản tồn tại và đang ở trạng thái Active.
  - Kiểm tra mật khẩu (so sánh với PasswordHash đã mã hóa).
  - Nếu đúng: Sinh JWT hoặc tạo session đăng nhập; gán quyền truy cập theo Role (Customer/Admin).
  - Nếu sai: Tăng bộ đếm số lần đăng nhập sai; nếu vượt quá số lần cho phép (ví dụ 5 lần), khóa tạm thời tài khoản (15 phút).
- **Output:**
  - Đăng nhập thành công: chuyển đến trang chủ hoặc trang dashboard (nếu Admin).
  - Đăng nhập thất bại: hiển thị thông báo lỗi phù hợp.
- **Pre-condition:** Tài khoản đã được tạo.
- **Post-condition:** Phiên làm việc (session/JWT) được tạo cho người dùng hợp lệ.

#### FR-03: Quên mật khẩu

- **Actor:** Guest.
- **Input:** Email đăng ký.
- **Xử lý:**
  - Kiểm tra email tồn tại trong hệ thống.
  - Sinh token reset mật khẩu có thời hạn (ví dụ 30 phút, cấu hình trong Configurations).
  - **Token chỉ được sử dụng một lần duy nhất** (one-time use): sau khi đổi mật khẩu thành công, token bị vô hiệu hóa.
  - Gửi email kèm link reset mật khẩu tới người dùng.
- **Output:** Thông báo đã gửi email reset mật khẩu (kể cả trong trường hợp không tồn tại email, có thể vẫn hiển thị thông báo chung để tránh lộ thông tin).

#### FR-04: Cập nhật thông tin cá nhân

- **Actor:** Customer.
- **Input:** Họ tên, Số điện thoại, Địa chỉ, Ảnh đại diện, v.v.
- **Xử lý:** Kiểm tra định dạng các trường thông tin; cập nhật vào bảng Users.
- **Output:** Thông báo cập nhật thành công.

### 3.2 QUẢN LÝ XE (DUYỆT & XEM)

#### FR-05: Xem danh sách xe

- **Actor:** Guest, Customer, Admin.
- **Mô tả:** Hiển thị danh sách xe hiện có để bán trên hệ thống.
- **Thông tin hiển thị tối thiểu:**
  - Hình ảnh đại diện của xe.
  - Tên xe.
  - Hãng xe.
  - Năm sản xuất.
  - Giá bán.
  - Tình trạng (mới/cũ).
  - Trạng thái tồn kho (Còn hàng/Hết hàng).
- **Yêu cầu:** Có phân trang (paging); cho phép sắp xếp theo giá, năm sản xuất, mới cập nhật.

#### FR-06: Xem chi tiết xe

- **Actor:** Guest, Customer, Admin.
- **Mô tả:** Hiển thị đầy đủ thông tin chi tiết của một xe.
- **Nội dung:**
  - Bộ ảnh chi tiết xe.
  - Thông số kỹ thuật (động cơ, hộp số, số km đã đi, màu sắc, loại nhiên liệu, v.v.).
  - Mô tả chi tiết.
  - Lịch sử bảo dưỡng (nếu có).
  - Đánh giá, nhận xét từ khách hàng.
  - Tình trạng kho (số lượng tồn).

#### FR-07: Tìm kiếm & lọc xe

- **Actor:** Guest, Customer.
- **Tiêu chí:** Hãng xe; Khoảng giá; Năm sản xuất; Loại xe (SUV, Sedan, Hatchback…); Tình trạng (mới/cũ); Số km đã đi (đối với xe cũ).
- **Chức năng:** Tìm kiếm theo từ khóa (tên xe, model); kết hợp nhiều bộ lọc cùng lúc.

#### FR-08: So sánh xe

- **Actor:** Guest, Customer.
- **Mô tả:** Cho phép người dùng chọn tối đa 3 xe để so sánh.
- **Hiển thị:** Bảng so sánh các thông số chính:
  - Giá bán.
  - Năm sản xuất.
  - Động cơ (dung tích, công suất).
  - Hộp số.
  - Mức tiêu hao nhiên liệu (nếu có).
  - Tình trạng (mới/cũ).

### 3.3 GIỎ HÀNG & ĐẶT MUA

*Ràng buộc nghiệp vụ (xe ô tô là tài sản lớn): Mỗi đơn hàng chỉ gồm **một xe** để đơn giản hóa thủ tục, giấy tờ và theo dõi. Khách có thể "Mua ngay" từ trang chi tiết xe hoặc chọn một xe từ giỏ hàng để tạo đơn.*

#### FR-09: Thêm xe vào giỏ hàng

- **Actor:** Customer.
- **Mô tả:** Cho phép lưu xe quan tâm vào giỏ (wishlist ngắn hạn). Dùng để so sánh, xem lại hoặc chọn **một xe** để "Mua ngay / Đặt cọc" — mỗi lần thanh toán chỉ tạo đơn cho một xe.
- **Xử lý:**
  - Nếu khách chưa đăng nhập: Có thể lưu giỏ tạm trong session/cookie; khi đăng nhập, có thể hợp nhất giỏ tạm với giỏ trong tài khoản.
  - Nếu khách đã đăng nhập: Lưu thông tin giỏ hàng vào bảng Cart/CartItems (hoặc bảng tương đương).

#### FR-10: Xem và cập nhật giỏ hàng

- **Actor:** Customer.
- **Mô tả:** Xem danh sách xe trong giỏ; có thể xóa xe khỏi giỏ; với **từng xe** có thể thực hiện "Mua ngay" / "Đặt cọc" (chuyển sang luồng thanh toán cho đúng một xe đó).
- **Yêu cầu:**
  - Hiển thị tổng tiền tạm tính (nếu hiển thị nhiều xe).
  - Hiển thị trạng thái kho từng xe; **nếu xe đã hết hàng hoặc đã bị Admin ngừng bán/xóa**: hiển thị cảnh báo và **không cho phép** chọn xe đó để thanh toán (xem Edge Case: hết hàng trong giỏ — mục 3.10).

#### FR-11: Thanh toán (tạo đơn hàng)

- **Actor:** Customer.
- **Pre-condition:** Customer đã đăng nhập; đã chọn **đúng một xe** còn hàng (từ giỏ hoặc nút "Mua ngay" tại trang chi tiết xe).
- **Ràng buộc:** Mỗi đơn hàng chỉ chứa một xe (Quantity = 1 trong OrderDetails).
- **Luồng chính:**
  1. Customer mở trang giỏ hàng và chọn **một xe** "Thanh toán" / "Đặt cọc", hoặc từ trang chi tiết xe nhấn "Mua ngay" / "Đặt cọc".
  2. Hệ thống kiểm tra xe còn hàng (và chưa bị xóa/ẩn); nếu không đáp ứng → báo lỗi (Edge Case).
  3. Hệ thống yêu cầu nhập/chọn thông tin nhận xe: Địa chỉ giao xe hoặc chọn showroom nhận xe.
  4. Customer chọn phương thức thanh toán: Thanh toán online toàn bộ; Thanh toán đặt cọc (theo tỉ lệ lấy từ bảng Configurations, ví dụ 20%).
  5. Hệ thống tạo đơn hàng (một dòng OrderDetails, Quantity=1) ở trạng thái Pending Payment.
  6. Hệ thống chuyển hướng tới Payment Gateway (nếu thanh toán online).
  7. Payment Gateway xử lý và trả kết quả (thành công/thất bại).
  8. Hệ thống cập nhật trạng thái: Nếu thành công: đơn chuyển sang Paid/Deposited; Nếu thất bại: đơn ở trạng thái Payment Failed hoặc bị hủy.
  9. Hệ thống gửi email xác nhận kết quả cho khách hàng.

#### FR-12: Xử lý thanh toán

- **Actor:** Hệ thống (internal), Admin (giám sát).
- **Mô tả:** Lưu thông tin giao dịch thanh toán vào bảng Payments: Mã giao dịch (TransactionCode), Số tiền thanh toán, Phương thức thanh toán, Trạng thái thanh toán (Success/Failed), Thời gian thanh toán. Cập nhật trạng thái đơn hàng tương ứng.
- **Ràng buộc:** Thao tác cập nhật đơn hàng và ghi nhận thanh toán phải đảm bảo tính toàn vẹn (sử dụng transaction trong DB).

### 3.4 ĐẶT LỊCH LÁI THỬ

#### FR-13: Đặt lịch lái thử

- **Actor:** Customer.
- **Input:** Xe muốn lái thử; Ngày giờ mong muốn; Showroom (nếu có nhiều showroom); Thông tin liên hệ (SĐT, ghi chú).
- **Xử lý:**
  - Kiểm tra không trùng với một lịch lái thử đã Approved khác của cùng xe/cùng showroom trong cùng khoảng thời gian (theo Business Rule).
  - Tạo bản ghi trong bảng TestDrives với trạng thái Requested/Pending.
- **Output:** Thông báo đặt lịch thành công, chờ Admin xác nhận.

#### FR-14: Quản lý lịch lái thử

- **Actor:** Admin.
- **Chức năng:**
  - Xem danh sách lịch lái thử.
  - Lọc theo: Trạng thái (Requested, Approved, Rejected, Completed); Thời gian, showroom, xe, khách hàng.
  - Duyệt hoặc từ chối yêu cầu lái thử.
  - Cập nhật trạng thái sau khi lái thử hoàn tất.

### 3.5 QUẢN LÝ ĐƠN HÀNG

#### FR-15: Xem lịch sử đơn hàng

- **Actor:** Customer.
- **Mô tả:** Xem danh sách đơn hàng của chính mình; lọc theo trạng thái: Pending, Paid, Deposited, Shipping, Completed, Cancelled…
- **Nội dung hiển thị:** Mã đơn hàng; Ngày tạo; Tổng tiền; Trạng thái; Chi tiết xe trong đơn.

#### FR-16: Hủy đơn hàng

- **Actor:** Customer, Admin.
- **Ràng buộc nghiệp vụ:**
  - Customer chỉ được phép hủy đơn khi: Đơn chưa chuyển sang trạng thái Shipping, và/hoặc Trước thời điểm giao xe X ngày (theo Business Rule).
  - Admin có thể hủy trong các trường hợp đặc biệt (lỗi hệ thống, khách yêu cầu…).
- **Xử lý:** Cập nhật trạng thái đơn sang Cancelled. Nếu đơn đã đặt cọc hoặc thanh toán: Thực hiện quy tắc hoàn/không hoàn tiền theo Business Rule.

#### FR-17: Cập nhật trạng thái đơn hàng (Admin)

- **Actor:** Admin.
- **Mô tả:** Cập nhật trạng thái đơn theo luồng (xem Sơ đồ trạng thái Đơn hàng — mục 5.5): Pending Payment → Paid/Deposited; Paid/Deposited → Processing; Processing → Shipping; Shipping → Completed hoặc Cancelled.
- **Yêu cầu:**
  - **Audit Trail:** Mỗi lần đổi trạng thái phải ghi log (bảng Logs/AuditLogs): Admin nào thực hiện, thời điểm, đơn hàng, trạng thái cũ → mới. Bắt buộc để chống gian lận và truy vết.
  - Lưu lịch sử thay đổi trạng thái đơn (OrderStatusHistory hoặc tương đương) khi cần.

### 3.6 ĐÁNH GIÁ & NHẬN XÉT XE

#### FR-18: Đánh giá và nhận xét xe

- **Actor:** Customer.
- **Mô tả:** Cho phép khách hàng đánh giá xe đã mua.
- **Input:** Rating (1–5 sao); Nội dung bình luận (Comment).
- **Ràng buộc:**
  - Chỉ những khách có đơn hàng ở trạng thái Completed với xe đó mới được phép tạo đánh giá (Business Rule).
  - Một khách có thể giới hạn số lần đánh giá trên mỗi xe (ví dụ 1 lần, cho phép sửa đánh giá sau này).
- **Xử lý:** Lưu vào bảng Reviews, liên kết với User và Car.

### 3.7 DANH SÁCH XE YÊU THÍCH (WISHLIST)

#### FR-19: Thêm xe vào danh sách yêu thích

- **Actor:** Customer.
- **Mô tả:** Khi duyệt xe hoặc xem chi tiết xe, Customer có thể nhấn nút “Yêu thích” để đánh dấu xe vào danh sách yêu thích cá nhân.
- **Xử lý:** Kiểm tra người dùng đã đăng nhập; nếu xe chưa nằm trong danh sách yêu thích của người dùng: Tạo bản ghi trong bảng Favorites(UserId, CarId, CreatedAt).
- **Ràng buộc:** Cặp (UserId, CarId) là unique trong bảng Favorites (một xe chỉ được yêu thích một lần bởi cùng một user).

#### FR-20: Quản lý danh sách xe yêu thích

- **Actor:** Customer.
- **Mô tả:** Xem danh sách tất cả xe đã được đánh dấu yêu thích; có thể bỏ yêu thích (xóa khỏi danh sách) với từng xe.
- **Hiển thị:** Ảnh, tên xe, giá, hãng, tình trạng kho (Còn hàng/Hết hàng); Link tới trang chi tiết xe, nút thêm vào giỏ hàng.

### 3.8 NHẬP XE & QUẢN LÝ TỒN KHO (ADMIN)

*Phân biệt **Mẫu xe** và **Chiếc xe cụ thể**:*
- **Cars (Mẫu xe):** Thông tin catalog — tên, hãng, loại, năm, giá đề xuất, thông số, ảnh. Một mẫu có thể có nhiều chiếc trong kho (StockQuantity).
- **CarItems (Chiếc xe cụ thể):** Tùy chọn triển khai — mỗi chiếc xe thực tế có thể có một bản ghi với **số VIN duy nhất**. Khi bán, hệ thống có thể: (1) trừ StockQuantity của mẫu Cars, hoặc (2) đánh dấu CarItem (VIN) đã bán. SRS cho phép mô hình đơn giản (chỉ StockQuantity) hoặc mô hình chi tiết (CarItems + VIN) tùy giai đoạn triển khai.

#### FR-21: Tạo phiếu nhập xe

- **Actor:** Admin.
- **Mô tả:** Cho phép Admin nhập xe về kho từ nhà cung cấp/đối tác.
- **Input:**
  - Thông tin phiếu nhập: Nhà cung cấp (SupplierName) hoặc nguồn nhập; Ngày nhập; Ghi chú.
  - Danh sách chi tiết nhập: Chọn mẫu xe (Car) từ danh mục; Số lượng nhập; Giá nhập (CostPrice). Nếu có bảng CarItems: có thể nhập kèm từng VIN cho từng chiếc.
- **Xử lý:** Tạo bản ghi ImportReceipts và ImportReceiptDetails; cộng dồn StockQuantity của Car (và nếu có: tạo bản ghi CarItems với VIN tương ứng).
- **Output:** Phiếu nhập được lưu thành công, tồn kho được cập nhật.

#### FR-22: Quản lý tồn kho xe

- **Actor:** Admin.
- **Mô tả:** Xem tồn kho theo **mẫu xe** (Cars) và, nếu triển khai, theo **từng chiếc** (CarItems/VIN); xem lịch sử nhập xe.
- **Hiển thị:** Tên mẫu xe, hãng, năm, tình trạng; StockQuantity hiện tại; (tùy chọn) danh sách VIN từng chiếc và trạng thái (Trong kho / Đã bán); tổng đã nhập, đã bán.
- **Ràng buộc:**
  - Khi đơn hàng thanh toán thành công: trừ StockQuantity của Car (và nếu dùng CarItems: gắn VIN cụ thể vào đơn hoặc đánh dấu đã bán).
  - StockQuantity <= 0: mẫu xe vẫn hiển thị để tham khảo nhưng không cho đặt mua (nút mua vô hiệu / Hết hàng).

### 3.9 QUẢN TRỊ HỆ THỐNG (ADMIN)

#### FR-23: Quản lý người dùng

- **Actor:** Admin.
- **Chức năng:** Xem danh sách người dùng; tìm kiếm, lọc theo vai trò (Customer/Admin), trạng thái (Active/Inactive/Locked); khóa/Mở khóa tài khoản.

#### FR-24: Quản lý danh mục

- **Actor:** Admin.
- **Mô tả:** Quản lý các danh mục dùng chung: Hãng xe (Brands); Loại xe (Categories); Showroom.
- **Chức năng:** Thêm/Sửa/Xóa danh mục; đảm bảo danh mục đang được sử dụng không bị xóa nếu không cho phép (ràng buộc FK).

#### FR-25: Quản lý xe (thông tin hiển thị)

- **Actor:** Admin.
- **Mô tả:** Thêm mới, chỉnh sửa, xóa xe khỏi danh mục bán hàng (ở cấp thông tin hiển thị, khác với StockQuantity).
- **Chức năng:**
  - Thêm xe: Khai báo thông tin cơ bản xe (tên, hãng, loại, năm, tình trạng, giá đề xuất, mô tả, thông số kỹ thuật, VIN nếu có…); Upload ảnh xe (nhiều ảnh).
  - Sửa thông tin xe (kể cả giá): Mỗi lần sửa giá hoặc thông tin nhạy cảm phải ghi **Audit Trail** (bảng Logs): Admin nào, thời điểm, trường thay đổi, giá trị cũ → mới.
  - Bật/tắt hiển thị xe trên website (ví dụ: ngừng kinh doanh một mẫu).

#### FR-26: Thống kê & Dashboard

- **Actor:** Admin.
- **Mô tả:** Cung cấp màn hình dashboard tổng quan tình hình kinh doanh.
- **Nội dung:** Doanh thu theo tháng/quý/năm; Số lượng xe bán ra theo khoảng thời gian; Số lượng đơn hàng theo trạng thái; Số khách hàng mới; Top xe bán chạy.
- **Biểu diễn:** Bảng số liệu; Biểu đồ cột/đường/tròn.

### 3.10 TRƯỜNG HỢP BIÊN (EDGE CASES)

#### EC-01: Xe trong giỏ hết hàng hoặc bị ngừng bán khi Checkout

- **Tình huống:** Khách đã thêm xe vào giỏ; trước khi thanh toán, Admin đã xóa/ẩn xe hoặc xe đã được bán (tồn kho = 0).
- **Xử lý:**
  - Tại bước mở giỏ hàng / bước "Thanh toán": Hệ thống kiểm tra lại tồn kho và trạng thái hiển thị của từng xe.
  - Nếu xe không còn khả dụng: Hiển thị thông báo rõ ràng (ví dụ: "Xe [tên] không còn khả dụng. Vui lòng xóa khỏi giỏ hoặc liên hệ showroom."); **không cho phép** chọn xe đó để tạo đơn.
  - Nút "Thanh toán" / "Mua ngay" cho xe đó bị vô hiệu hóa hoặc ẩn.

#### EC-02: Token reset mật khẩu hết hạn hoặc đã dùng

- **Tình huống:** Link reset mật khẩu (FR-03) quá 30 phút hoặc đã được sử dụng một lần.
- **Xử lý:** Hiển thị thông báo "Link không còn hiệu lực. Vui lòng yêu cầu gửi lại email đặt lại mật khẩu."; cung cấp link quay lại trang "Quên mật khẩu".

---

## 4. YÊU CẦU PHI CHỨC NĂNG

### 4.1 Hiệu năng

- Thời gian tải trang chủ và trang danh sách xe: Dưới 3 giây với dữ liệu mẫu và số lượng user đồng thời khoảng 500 (mô phỏng trong môi trường kiểm thử).
- Thời gian phản hồi API backend cho các request đơn lẻ: Trung bình dưới 1 giây trong điều kiện lab thông thường.
- Hỗ trợ phân trang và tối ưu truy vấn để tránh tải toàn bộ dữ liệu lớn một lần.
- **Ảnh tối ưu (Responsive Images):** Web bán xe có nhiều ảnh chất lượng cao; hệ thống phải tối ưu để đảm bảo LCP (Largest Contentful Paint) chấp nhận được: resize/thumbnail theo kích thước hiển thị, lazy loading, định dạng hiện đại (WebP) khi phù hợp. Tránh tải ảnh gốc full resolution cho danh sách xe.

### 4.2 Bảo mật

- **Mật khẩu:**
  - Được mã hóa bằng BCrypt trước khi lưu.
  - Yêu cầu tối thiểu: Độ dài >= 8 ký tự; Có chữ hoa, chữ thường, số (khuyến khích có ký tự đặc biệt).
- **Xác thực & phân quyền:** Sử dụng cơ chế JWT hoặc session-based; phân quyền theo Role (Guest, Customer, Admin).
- **Giao thức:** Hỗ trợ HTTPS trong môi trường triển khai thực tế.
- **Chống tấn công:**
  - Chống SQL Injection: Sử dụng Prepared Statement/ORM (JPA/Hibernate).
  - Chống XSS: Encode dữ liệu output, validate input phía server.
  - Chống Brute Force đăng nhập: Khóa tạm thời tài khoản sau một số lần đăng nhập sai liên tiếp (số lần và thời gian khóa lấy từ Configurations).
- **Logging & Audit Trail (bắt buộc với hệ thống có thanh toán):**
  - Ghi log hành động quan trọng của Admin: Admin nào đã duyệt/đổi trạng thái đơn hàng; Admin nào đã sửa giá xe, sửa thông tin xe; thời điểm, giá trị cũ/mới. Lưu vào bảng Logs/AuditLogs.
  - Mục đích: chống gian lận, truy vết khi có tranh chấp hoặc kiểm toán.

### 4.3 Khả dụng (Availability)

- Mục tiêu uptime: >= 99% trong thời gian vận hành thử nghiệm.
- Backup: Thực hiện backup database tự động hàng ngày; lưu trữ ít nhất 7 bản backup gần nhất.
- Khả năng khôi phục: Có quy trình restore database từ file backup (thử nghiệm trên môi trường test).

### 4.4 Tính mở rộng (Scalability & Maintainability)

- Thiết kế theo mô hình MVC (Spring Boot + Thymeleaf).
- Tách rõ các lớp: Controller (xử lý request); Service (xử lý nghiệp vụ); Repository (truy cập dữ liệu).
- Cấu trúc module: User, Car, Order, Payment, Review, Inventory, Admin, TestDrive…
- Có khả năng tách thành các service độc lập (microservice) trong tương lai (ở mức ý tưởng).

### 4.5 Khả năng sử dụng (Usability)

- **Giao diện:** Responsive design (tương thích PC, mobile, tablet); thiết kế trực quan, dễ sử dụng, màu sắc hài hòa; navigation rõ ràng (menu, breadcrumb, thanh tìm kiếm).
- **Ngôn ngữ:** Hỗ trợ tiếng Việt đầy đủ (font, dấu); có thể dễ dàng mở rộng đa ngôn ngữ (i18n) trong tương lai.

---

## 5. MÔ HÌNH USE CASE

### 5.1 Danh sách Actor

- Guest.
- Customer.
- Admin.

### 5.2 Danh sách Use Case chính (tổng quan)

- UC-01: Đăng ký tài khoản.
- UC-02: Đăng nhập.
- UC-03: Quên mật khẩu.
- UC-04: Cập nhật thông tin cá nhân.
- UC-05: Xem danh sách xe.
- UC-06: Xem chi tiết xe.
- UC-07: Tìm kiếm & lọc xe.
- UC-08: So sánh xe.
- UC-09: Thêm xe vào giỏ hàng.
- UC-10: Xem & cập nhật giỏ hàng.
- UC-11: Thanh toán & tạo đơn hàng.
- UC-12: Xem lịch sử đơn hàng.
- UC-13: Hủy đơn hàng.
- UC-14: Đặt lịch lái thử.
- UC-15: Quản lý lịch lái thử (Admin).
- UC-16: Đánh giá & nhận xét xe.
- UC-17: Quản lý danh sách xe yêu thích (Customer).
- UC-18: Tạo phiếu nhập xe (Admin).
- UC-19: Quản lý tồn kho xe (Admin).
- UC-20: Quản lý người dùng (Admin).
- UC-21: Quản lý danh mục (Admin).
- UC-22: Quản lý xe (Admin).
- UC-23: Xem thống kê & Dashboard (Admin).

### 5.3 Ví dụ mô tả chi tiết Use Case: UC-11 – Thanh toán & tạo đơn hàng

- **Tên:** Thanh toán & tạo đơn hàng.
- **Mã:** UC-11.
- **Actor chính:** Customer.
- **Mục tiêu:** Hoàn tất quy trình thanh toán cho **một xe** đã chọn (từ giỏ hoặc Mua ngay) và tạo đơn hàng. Mỗi đơn hàng chỉ gồm một xe.
- **Pre-conditions:** Customer đã đăng nhập; đã chọn đúng một xe còn hàng (từ giỏ hoặc trang chi tiết xe).
- **Post-conditions:** Đơn hàng được tạo trong bảng Orders (một dòng OrderDetails, Quantity=1); bản ghi thanh toán trong Payments (nếu thanh toán online); tồn kho StockQuantity trừ tương ứng khi thanh toán thành công.
- **Luồng sự kiện chính:**
  1. Customer mở trang giỏ hàng.
  2. Hệ thống hiển thị danh sách xe trong giỏ, tổng tiền tạm tính.
  3. Customer nhấn nút “Thanh toán”.
  4. Hệ thống yêu cầu nhập/chọn địa chỉ giao xe hoặc showroom nhận xe.
  5. Customer chọn phương thức thanh toán (online/đặt cọc).
  6. Hệ thống tạo đơn hàng ở trạng thái Pending Payment.
  7. Hệ thống chuyển hướng sang Payment Gateway (đối với thanh toán online).
  8. Payment Gateway trả kết quả (success/fail).
  9. Nếu success: Hệ thống cập nhật trạng thái đơn sang Paid/Deposited; tạo bản ghi trong Payments; trừ tồn kho StockQuantity tương ứng; gửi email xác nhận tới Customer.
  10. Nếu fail: Hệ thống cập nhật đơn hàng sang trạng thái Payment Failed hoặc hủy; hiển thị thông báo lỗi tới Customer và cho phép thử lại.
- **Luồng ngoại lệ:** Payment Gateway không phản hồi: Hệ thống hiển thị thông báo “Lỗi kết nối thanh toán” và không trừ tiền/tồn kho.

### 5.4 Ma trận Actor – Use Case (tóm tắt)

- **Guest:** UC-01, UC-02, UC-03, UC-05, UC-06, UC-07, UC-08.
- **Customer:** Tất cả của Guest + UC-04, UC-09, UC-10, UC-11, UC-12, UC-13, UC-14, UC-16, UC-17.
- **Admin:** UC-15, UC-18, UC-19, UC-20, UC-21, UC-22, UC-23; có thể xem hầu hết thông tin/luồng khác với quyền quản trị.

### 5.5 Sơ đồ trạng thái (State Diagram)

Hai thực thể quan trọng cần mô tả rõ luồng trạng thái:

**Đơn hàng (Order):**

- **Pending Payment** → (thanh toán thành công) → **Paid** hoặc **Deposited**
- **Pending Payment** → (thanh toán thất bại / hủy) → **Payment Failed** hoặc **Cancelled**
- **Paid** / **Deposited** → (Admin xử lý) → **Processing**
- **Processing** → (chuẩn bị giao) → **Shipping**
- **Shipping** → (giao xong) → **Completed**
- **Shipping** / **Processing** → (hủy) → **Cancelled**

*Sơ đồ chi tiết (vẽ bằng Draw.io, StarUML…) đính kèm trong Phụ lục.*

**Lịch lái thử (Test Drive):**

- **Requested** (Pending) → (Admin duyệt) → **Approved** (Confirmed)
- **Requested** → (Admin từ chối) → **Rejected**
- **Approved** → (đã lái thử xong) → **Completed**
- **Requested** / **Approved** → (hủy) → **Cancelled**

*Sơ đồ chi tiết đính kèm trong Phụ lục.*

---

## 6. YÊU CẦU DỮ LIỆU

### 6.1 Các bảng chính

- **Users** (UserId, FullName, Email, Phone, PasswordHash, RoleId, Status, CreatedAt, UpdatedAt, Address, AvatarUrl, ...)
- **Roles** (RoleId, RoleName, Description) — Ví dụ: 1-ADMIN, 2-CUSTOMER.
- **Cars** (CarId, Title, BrandId, CategoryId, Year, Price, Mileage, Condition, Description, Status, CreatedAt, UpdatedAt, StockQuantity, ...) — *Mẫu xe; VIN có thể lưu ở cấp mẫu hoặc ở CarItems.*
- **CarItems** (CarItemId, CarId, VIN, Status, ...) — *Tùy chọn: từng chiếc xe thực tế với VIN duy nhất; nếu không dùng thì quản lý tồn kho chỉ bằng StockQuantity của Cars.*
- **CarImages** (ImageId, CarId, ImageUrl, IsThumbnail)
- **Brands** (BrandId, BrandName, Country, Description)
- **Categories** (CategoryId, CategoryName, Description)
- **Showrooms** (ShowroomId, Name, Address, Latitude, Longitude, Phone)
- **Orders** (OrderId, UserId, TotalAmount, Status, PaymentMethod, CreatedAt, UpdatedAt, DeliveryAddress, ShowroomId, Note, ...)
- **OrderDetails** (OrderDetailId, OrderId, CarId, UnitPrice, Quantity) — *Ràng buộc nghiệp vụ: mỗi đơn chỉ một dòng (Quantity=1) tương ứng một xe.*
- **Payments** (PaymentId, OrderId, Amount, PaymentStatus, PaymentDate, TransactionCode, PaymentMethod)
- **TestDrives** (TestDriveId, UserId, CarId, ShowroomId, TestDriveDateTime, Status, Note, CreatedAt)
- **Reviews** (ReviewId, UserId, CarId, Rating, Comment, CreatedAt)
- **Favorites** (FavoriteId, UserId, CarId, CreatedAt)
- **ImportReceipts** (ImportId, CreatedByUserId, SupplierName, ImportDate, TotalQuantity, TotalCost, Note, CreatedAt)
- **ImportReceiptDetails** (ImportDetailId, ImportId, CarId, Quantity, CostPrice)
- **Configurations** (ConfigKey, ConfigValue, Description, UpdatedAt) — *Tham số hệ thống: DepositRate (%), AccountLockoutDurationMinutes, MaxFailedLoginAttempts, PasswordResetTokenValidityMinutes, Hotline, ... Tránh hard-code trong code.*
- **Logs** (LogId, UserId, Action, EntityType, EntityId, OldValue, NewValue, IpAddress, CreatedAt) — *Audit trail: lưu vết thao tác Admin (duyệt đơn, sửa giá xe, sửa thông tin…).*
- **Promotions** (PromotionId, Code, DiscountType, DiscountValue, StartDate, EndDate, Status, ...) — *Tùy chọn / mở rộng: khuyến mãi, voucher; nếu không triển khai giai đoạn 1 có thể bỏ qua.*

### 6.2 Ràng buộc dữ liệu

- Email trong Users là unique.
- Mỗi Order phải có ít nhất một OrderDetails.
- Rating trong Reviews nằm trong khoảng [1, 5].
- VIN (nếu sử dụng) là unique cho mỗi xe.
- Favorites: cặp (UserId, CarId) là unique.
- **Quan hệ khóa ngoại:**
  - Users (1) – (N) Orders
  - Orders (1) – (N) OrderDetails
  - Cars (1) – (N) OrderDetails
  - Cars (1) – (N) CarImages
  - Cars (1) – (N) CarItems *(nếu dùng)*
  - Cars (1) – (N) Reviews
  - Users (1) – (N) Reviews
  - Users (1) – (N) Favorites
  - Cars (1) – (N) Favorites
  - Users (1) – (N) TestDrives
  - Cars (1) – (N) TestDrives
  - Showrooms (1) – (N) TestDrives
  - Orders (1) – (1) Payments
  - ImportReceipts (1) – (N) ImportReceiptDetails
  - Cars (1) – (N) ImportReceiptDetails
  - Users (1) – (N) Logs *(UserId = Admin thực hiện)*

---

## 7. MÔ HÌNH DỮ LIỆU (ERD)

- ERD thể hiện các thực thể chính và mối quan hệ đã liệt kê ở mục 6.2.
- Sơ đồ ERD chi tiết (vẽ bằng công cụ như Draw.io, StarUML, v.v.) sẽ được đính kèm trong phụ lục, bao gồm:
  - **Thực thể:** Users, Roles, Cars, CarItems (tùy chọn), CarImages, Brands, Categories, Showrooms, Orders, OrderDetails, Payments, TestDrives, Reviews, Favorites, ImportReceipts, ImportReceiptDetails, Configurations, Logs, Promotions (tùy chọn).
  - **Quan hệ** 1–N, 1–1 tương ứng.

---

## 8. YÊU CẦU GIAO DIỆN

- **Trang chủ:** Banner, danh sách xe nổi bật, thanh tìm kiếm nhanh.
- **Trang danh sách xe:** Lưới/bảng xe với ảnh, tên, giá, hãng, tình trạng; bộ lọc (hãng, giá, năm, loại xe, mới/cũ); phân trang, sắp xếp.
- **Trang chi tiết xe:** Ảnh lớn, gallery ảnh; thông số kỹ thuật, mô tả chi tiết; thông tin tồn kho, trạng thái còn hàng; nút “Thêm vào giỏ”, “Yêu thích”, “Đặt lịch lái thử”; khu vực đánh giá & bình luận.
- **Trang giỏ hàng:** Danh sách xe, thông tin giá, tổng tiền; nút xóa khỏi giỏ, nút “Thanh toán”.
- **Trang thanh toán:** Thông tin đơn, địa chỉ/showroom; tùy chọn phương thức thanh toán; nút xác nhận thanh toán, hiển thị trạng thái xử lý.
- **Trang lịch sử đơn hàng:** Danh sách đơn, trạng thái, filter theo thời gian/trạng thái; link xem chi tiết, nút hủy (nếu được phép).
- **Trang “Xe yêu thích”:** Danh sách xe mà khách đã yêu thích; nút “Bỏ yêu thích”.
- **Trang Admin Dashboard:** Biểu đồ doanh thu, đơn hàng, xe bán chạy, khách hàng mới; danh sách công việc cần xử lý (duyệt lịch lái thử, đơn mới…).
- **Trang quản lý nhập xe và tồn kho:** Danh sách phiếu nhập, form tạo phiếu nhập; bảng tồn kho xe.
- **Trang quản lý người dùng, danh mục, xe:** Bảng dữ liệu với chức năng thêm/sửa/xóa, tìm kiếm, phân trang.

---

## 9. YÊU CẦU KIỂM THỬ

- **Unit Test:** Kiểm thử các lớp Service chính: UserService, CarService, OrderService, PaymentService, InventoryService, TestDriveService, ReviewService…
- **Integration Test:**
  - Kiểm thử luồng đăng ký – đăng nhập – mua xe – thanh toán – xem lịch sử đơn.
  - Kiểm thử luồng đặt lịch lái thử – duyệt lịch.
  - Kiểm thử nhập xe – cập nhật tồn kho – bán xe.
- **UAT (User Acceptance Test):** Kiểm thử với các kịch bản tương tự người dùng thực tế: Khách vào xem và mua xe; Admin quản lý kho, đơn hàng.
- **Số lượng test case:** Tối thiểu 80 test case, bao phủ: Các chức năng mức ưu tiên cao (đăng nhập, đăng ký, mua xe, thanh toán); các trường hợp ngoại lệ quan trọng (thanh toán lỗi, hết hàng, hủy đơn…).

---

## 10. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

- **BR-01: Mức đặt cọc** — Tỉ lệ đặt cọc lấy từ bảng Configurations (ví dụ key DepositRate = 20). Khi chọn thanh toán đặt cọc, số tiền thanh toán = (Tỉ lệ đặt cọc / 100) × Tổng tiền. Tránh hard-code trong code.
- **BR-02: Hủy đơn hàng** — Customer được phép hủy đơn khi: Đơn chưa chuyển sang trạng thái Shipping; và/hoặc trước thời điểm giao xe X ngày (cấu hình). Chính sách hoàn tiền đặt cọc: Có thể quy định hoàn 100% nếu hủy trước một khoảng thời gian nhất định; không hoàn/hoàn một phần nếu hủy sát ngày giao (tùy bài toán).
- **BR-03: Đánh giá xe** — Chỉ cho phép đánh giá đối với xe đã được mua và đơn ở trạng thái Completed. Mỗi khách hàng chỉ được một đánh giá chính thức trên mỗi xe (cho phép sửa).
- **BR-04: Lịch lái thử** — Không cho phép hai lịch lái thử trùng giờ + showroom + xe (hoặc trong cùng một khoảng thời gian xác định).
- **BR-05: Khóa tài khoản** — Số lần đăng nhập sai tối đa (N) và thời gian khóa (phút) lấy từ Configurations (ví dụ MaxFailedLoginAttempts=5, AccountLockoutDurationMinutes=15). Sau N lần sai liên tiếp, tài khoản bị khóa tạm thời.
- **BR-06: Tồn kho xe** — Chỉ cho phép đặt mua khi StockQuantity >= số lượng cần mua. Khi đơn hàng được thanh toán thành công, hệ thống trừ StockQuantity tương ứng.
- **BR-07: Yêu thích xe** — Chỉ người dùng đã đăng nhập mới có thể thêm xe vào danh sách yêu thích. Xe có thể được yêu thích ngay cả khi đang hết hàng, nhưng cần hiển thị rõ trạng thái “Hết hàng”.

---

## 11. TIÊU CHÍ NGHIỆM THU

- Tất cả Use Case mức ưu tiên cao (đăng ký, đăng nhập, quản lý xe, mua xe, thanh toán, lịch lái thử) đều phải được kiểm thử và pass.
- Không tồn tại lỗi mức Critical hoặc Major tại thời điểm nghiệm thu.
- Hệ thống đáp ứng: Đầy đủ các yêu cầu chức năng được liệt kê trong SRS; đạt hoặc gần đạt các yêu cầu phi chức năng chính (bảo mật, hiệu năng, tính ổn định) trong môi trường lab.

---

## 12. PHỤ LỤC

- Sơ đồ Use Case tổng thể và chi tiết (UC-01 → UC-23).
- **Sơ đồ trạng thái (State Diagram):** Đơn hàng (Order); Lịch lái thử (Test Drive) — xem mô tả tại mục 5.5.
- Sơ đồ ERD chi tiết toàn hệ thống (bao gồm Configurations, Logs, CarItems, Promotions khi áp dụng).
- Sơ đồ Sequence cho các luồng chính: Đăng ký & Đăng nhập; Mua xe & Thanh toán; Đặt lịch lái thử; Nhập xe & cập nhật tồn kho.
- Mockup/Wireframe các màn hình chính: Trang chủ, danh sách xe, chi tiết xe, giỏ hàng, thanh toán, lịch sử đơn, admin dashboard, tồn kho, nhập xe.

---

## 13. GHI CHÚ CHỈNH SỬA VÀ NHẬN XÉT BỔ SUNG

Tài liệu này đã được đối soát và cập nhật theo nhận xét từ góc độ Business Analyst và Solution Architect. Các thay đổi chính:

- **Quy trình mua xe:** Làm rõ ràng mỗi đơn hàng một xe; giỏ hàng dùng để lưu xe quan tâm, khi thanh toán chọn một xe (Mua ngay / Đặt cọc). Tránh luồng "nhiều xe trong một đơn" không phù hợp nghiệp vụ ô tô.
- **Tồn kho & VIN:** Phân biệt mẫu xe (Cars) và chiếc xe cụ thể (CarItems/VIN); cho phép triển khai đơn giản (StockQuantity) hoặc chi tiết (VIN từng chiếc) tùy giai đoạn.
- **Audit Trail & Configurations:** Bắt buộc log thao tác Admin và lưu tham số cấu hình (đặt cọc, khóa tài khoản, token reset…) vào bảng, tránh hard-code.
- **Edge cases:** Xử lý rõ khi xe trong giỏ hết hàng/bị xóa lúc checkout; token reset mật khẩu one-time use.
- **NFR:** Bổ sung Responsive Images (LCP), Logging/Audit Trail; sơ đồ trạng thái Order và Test Drive; bảng Promotions (mở rộng tương lai).

*Nhận xét riêng (Editor):* Bảng **Promotions** được đưa vào ở mức tùy chọn/mở rộng — nếu đồ án không triển khai voucher/giảm giá trong phase 1 có thể bỏ qua, nhưng có sẵn trong schema giúp mở rộng sau này mà không phải sửa SRS lại. Tương tự, **CarItems** cho phép nhóm dev chọn mô hình đơn giản (chỉ Cars + StockQuantity) trước, sau đó nâng cấp theo VIN khi cần.
