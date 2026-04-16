# Tổng hợp Validation – Modal Thêm mẫu xe & Cập nhật mẫu xe

## 1. Modal **Thêm mẫu xe mới**

### 1.1. Validation phía **client** (HTML5 + JavaScript)

| Trường / Nội dung | Cách kiểm tra | Cách hiển thị thông báo |
|-------------------|---------------|--------------------------|
| **Thương hiệu** | `required` trên `<select name="brandId">` | Browser: bubble "Please fill out this field" (hoặc tương đương) khi submit mà chưa chọn. |
| **Dòng xe** | `required` trên `<select name="modelId">` | Giống trên. |
| **Phân khúc** | `required` trên `<select name="segmentId">` | Giống trên. |
| **Năm sản xuất** | `required` trên `<select name="productionYear">` | Giống trên. |
| **Kiểu dáng** | `required` + `oninvalid="this.setCustomValidity('Vui lòng chọn kiểu dáng')"` | Bubble tiếng Việt: "Vui lòng chọn kiểu dáng" khi chưa chọn. |
| **Số ghế** | `required` trên `<input type="number" name="seats">` | Bubble trình duyệt khi để trống. |
| **Màu sắc trùng nhau** | JS trong `admin-cars-definitions.js`: khi submit form, duyệt các `input[name="colors"]`, chuẩn hóa (trim + lowercase), nếu trùng thì `preventDefault` | `alert('Màu sắc không được trùng')`. |

- **Xác nhận trước khi gửi:** `onsubmit="return confirm('Bạn có chắc rằng muốn thêm mẫu xe mới?');"` → hộp thoại confirm.

---

### 1.2. Validation phía **server** (AdminCarDefinitionController – POST create)

| Điều kiện | Message / Reject | Cách hiển thị thông báo |
|-----------|------------------|--------------------------|
| `brandId == null` | `rejectValue("brandId", "brand.required", "Thương hiệu không được để trống")` | Trả về `list(model)`; **không** set `model.addAttribute("error", ...)` → trang list không có toast. |
| `modelId == null` | `rejectValue("modelId", "model.required", "Dòng xe không được để trống")` | Giống trên. |
| `segmentId == null` | `rejectValue("segmentId", "segment.required", "Phân khúc không được để trống")` | Giống trên. |
| `bodyType == null` | `rejectValue("bodyType", "bodyType.required", "Kiểu dáng không được để trống")` | Giống trên. |
| `productionYear == null` hoặc `> currentYear` | `rejectValue("productionYear", "year.invalid", "Năm sản xuất phải <= năm hiện tại")` | Giống trên. |
| `seats == null` hoặc `<= 1` | `rejectValue("seats", "seats.invalid", "Số ghế phải > 1")` | Giống trên. |
| **Màu trùng nhau** (trong danh sách màu gửi lên) | `reject("colors.duplicate", ...)` + `model.addAttribute("error", "Màu sắc không được trùng")` | Trang list hiển thị **toast** (fragment `admin-error-toast`). |
| **Mẫu xe đã tồn tại** (trùng Thương hiệu + Dòng xe + Phân khúc + Năm) | `reject("duplicate", ...)` + `model.addAttribute("error", "Mẫu xe đã tồn tại (trùng ...). Vui lòng vào \"Nhập xe\" để thêm số lượng.")` | Trang list hiển thị **toast**. |
| **Lỗi lưu ảnh** (IOException khi storeImages) | `reject("image.upload", ...)`; **không** set `model.addAttribute("error", ...)` | Trả về `list(model)`; hiện tại **không** có toast (chỉ bindingResult). |

- Khi có lỗi: `return list(model)` → redirect về trang danh sách mẫu xe, modal đóng.
- Toast chỉ hiện khi có `model.addAttribute("error", ...)` (hiện có: màu trùng, mẫu xe trùng).

---

### 1.3. Hiển thị thông báo – Modal Thêm mẫu xe

- **Trang list** (`definitions.html`):  
  `th:block th:if="${error}"` → render fragment `fragments/admin-error-toast :: toast(error=${error})`.
- **Toast** (`admin-error-toast.html`):  
  Alert Bootstrap (alert-danger), cố định phía trên, có nút đóng, nội dung là `th:text="${error}"`.
- **Tóm tắt:**  
  - Client: bubble (HTML5) + 1 lần `alert` (màu trùng).  
  - Server: chỉ một số lỗi (màu trùng, mẫu xe trùng) hiện toast; các lỗi field (brand/model/segment/bodyType/year/seats) và lỗi ảnh **không** có toast, chỉ trả về trang list.

---

## 2. Modal **Cập nhật mẫu xe**

### 2.1. Validation phía **client** (HTML5 + JavaScript)

| Trường / Nội dung | Cách kiểm tra | Cách hiển thị thông báo |
|-------------------|---------------|--------------------------|
| **Kiểu dáng** | `required` + `oninvalid` / `oninput` setCustomValidity('Vui lòng chọn kiểu dáng') | Bubble tiếng Việt. |
| **Số ghế** | `required` trên `input#editSeats` | Bubble trình duyệt. |
| **Màu mới trùng với màu đã có** | Trước khi gửi AJAX: lấy tập “màu đã có” từ các `.edit-existing-color-item` còn hiển thị (`data-color-value`), so sánh với từng `input[name="newColors"]` (trim + lowercase) | Hiển thị trong **alert trong modal**: `#editDefinitionError` — gán `textContent = 'Bạn vừa thêm màu sắc trùng với màu đã có.'`, bỏ class `d-none`. |

- **Xác nhận:** `confirm('Bạn có chắc rằng muốn sửa thông tin mẫu xe?')` trước khi gửi fetch.

---

### 2.2. Validation phía **server** (AdminCarDefinitionController – POST update, JSON)

| Điều kiện | Response | Cách hiển thị thông báo |
|-----------|----------|--------------------------|
| `seats == null` hoặc `<= 1` | `ResponseEntity.badRequest().body(Map.of("error", "Số ghế phải > 1"))` | JS nhận `data.error`, gán vào `#editDefinitionError`, bỏ `d-none` → **alert đỏ trong modal**. |
| `bodyType == null` | `ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn kiểu dáng"))` | Giống trên. |
| **Màu mới trùng với màu đã có** (so với car_color hiện tại) | `ResponseEntity.badRequest().body(Map.of("error", "Bạn vừa thêm màu sắc trùng với màu đã có"))` | Giống trên. |
| **Lỗi lưu ảnh** (IOException) | `ResponseEntity.badRequest().body(Map.of("error", "Không thể lưu ảnh, vui lòng thử lại."))` | Giống trên. |
| **Thành công** | `ResponseEntity.ok(Map.of("success", true))` | JS reload trang. |
| **Lỗi mạng / catch** | JS trong `catch` | Gán `#editDefinitionError` = "Có lỗi xảy ra, vui lòng thử lại." trong modal. |

---

### 2.3. Hiển thị thông báo – Modal Cập nhật mẫu xe

- **Trong modal:**  
  `<div id="editDefinitionError" class="alert alert-danger d-none">` — JS gán `textContent` và bỏ `d-none` khi có lỗi (client hoặc server trả về `data.error`).
- Form gửi bằng **fetch** (AJAX); không redirect khi lỗi → toàn bộ thông báo lỗi hiển thị **ngay trong modal**.

---

## 3. Bảng tóm tắt nhanh

| Modal | Nơi kiểm tra | Khi lỗi hiển thị ở đâu |
|-------|----------------|-------------------------|
| **Thêm mẫu xe** | Client: required, màu trùng (JS) | Bubble / `alert` |
| **Thêm mẫu xe** | Server: trùng mẫu, màu trùng, (và một số field) | Toast trên **trang list** (chỉ khi có `model.addAttribute("error", ...)`) |
| **Cập nhật mẫu xe** | Client: required, màu mới trùng (JS) | Bubble / **alert đỏ trong modal** (`#editDefinitionError`) |
| **Cập nhật mẫu xe** | Server: seats, bodyType, màu trùng, ảnh | **Alert đỏ trong modal** (`#editDefinitionError`) qua `data.error` |

---

## 4. File liên quan

- **Controller:** `AdminCarDefinitionController.java` (create + update).
- **Template:** `templates/admin/cars/definitions.html` (modal thêm + modal cập nhật, toast trang list).
- **Fragment toast:** `templates/fragments/admin-error-toast.html`.
- **JS:** `static/js/admin-cars-definitions.js` (submit add form, submit edit form qua fetch, kiểm tra màu trùng).
