# FCAR

Ứng dụng website bán xe ô tô: danh sách xe, xem chi tiết xe, đặt cọc, đăng ký lái thử, liên hệ tư vấn, danh sách yêu thích, so sánh xe, đánh giá xe (có duyệt nội dung), và khu quản trị (admin) cho chi nhánh, thương hiệu, dòng xe, phân khúc, mẫu xe, nhập kho, đơn hàng, đăng ký lái thử, liên hệ tư vấn, duyệt đánh giá, thanh toán, báo cáo - thống kê.

## Công nghệ

- **Java 21** · **Spring Boot 3.3**
- **Spring Security** (form login, OAuth2 Google)
- **Spring Data JPA** · **SQL Server**
- **Thymeleaf** · **Bootstrap 5**
- **Spring Mail** (OTP, thông báo)

## Yêu cầu

- JDK 21+
- Maven 3.6+
- SQL Server (chạy schema trước khi chạy app)
- (Tùy chọn) SMTP cho mail; Google OAuth2 client nếu dùng đăng nhập Google

## Cấu hình

1. **Cơ sở dữ liệu**
   - Tạo database và chạy script: `src/main/resources/db/schema-sqlserver.sql`

2. **Secrets (bắt buộc trước khi chạy)** — file `application.yml` trong repo **không** chứa mật khẩu/secret thật. Chọn **một** trong hai cách:

   **Cách A — File cục bộ (khuyến nghị khi dev trên máy cá nhân)**  
   - Trong `src/main/resources/`, sao chép `application-local.example.yml` thành **`application-local.yml`** (cùng thư mục).  
   - Sửa các giá trị placeholder (mật khẩu SQL Server, Gmail app password, Google OAuth, PayOS nếu dùng).  
   - File `application-local.yml` đã nằm trong `.gitignore` — **không** commit.  
   - `application.yml` đã có `spring.config.import: optional:classpath:application-local.yml` nên file này được nạp tự động nếu tồn tại.

   **Cách B — Biến môi trường** (phù hợp CI/CD, Docker, production): đặt các biến tương ứng trước khi chạy `mvn` hoặc `java -jar`. Ví dụ tối thiểu:

   | Biến | Ý nghĩa |
   |------|---------|
   | `SPRING_DATASOURCE_PASSWORD` | Mật khẩu SQL Server |
   | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Gmail (SMTP) |
   | `SPRING_MAIL_PASSWORD` | Mật khẩu ứng dụng Gmail (16 ký tự) |
   | `GOOGLE_OAUTH2_CLIENT_ID`, `GOOGLE_OAUTH2_CLIENT_SECRET` | OAuth2 Google |
   | `FCAR_PAYOS_ENABLED` | `true` / `false` |
   | `FCAR_PAYOS_CLIENT_ID`, `FCAR_PAYOS_API_KEY`, `FCAR_PAYOS_CHECKSUM_KEY` | PayOS (khi bật) |
   | `FCAR_PAYOS_PUBLIC_BASE_URL` | URL gốc site (PayOS return/cancel), production nên là HTTPS |

   Có thể kết hợp: biến môi trường ghi đè giá trị mặc định trong `application.yml`; `application-local.yml` (nếu có) nạp sau và tiếp tục ghi đè phần trùng khóa.

3. **Ứng dụng**
   - `server.port`: mặc định 8080, có thể đổi bằng `SERVER_PORT`.
   - PayOS mặc định **tắt** (`FCAR_PAYOS_ENABLED=false`) cho đến khi bạn bật và điền đủ key.

## Chạy ứng dụng

```bash
mvn spring-boot:run
```

Mở trình duyệt: **http://localhost:8080**

- **Trang chủ:** danh sách xe (phân trang), chi tiết xe, so sánh, yêu thích — xem mục **Cửa hàng** bên dưới.
- **Khách hàng:** đăng ký / đăng nhập (form hoặc Google), thông tin cá nhân, đặt mua xe, đặt cọc, đăng ký lái thử, liên hệ tư vấn, lịch sử đơn hàng.
- **Admin:** sau khi đăng nhập tài khoản admin, vào **Admin** → sidebar với các mục:
  - Quản lý chi nhánh, thương hiệu, dòng xe, phân khúc (thêm/sửa/disable/enable, form thêm trong modal căn giữa).
  - Quản lý xe: thêm mẫu xe mới, nhập xe mới vào kho, lịch sử nhập.
  - Quản lý liên hệ tư vấn, đăng ký lái thử, đơn đặt mua xe, **duyệt đánh giá** khách hàng, người dùng, thanh toán, thống kê báo cáo.

Tài khoản admin mặc định (nếu có khởi tạo qua `DataInitializer`): xem cấu hình/script khởi tạo trong project (số điện thoại / mật khẩu tùy môi trường).

---

## Mô tả chi tiết các chức năng

Phần dưới gồm **tài khoản & xác thực khách hàng**, **mua xe / đặt cọc / thanh toán**, **lịch sử & chi tiết đơn**, **đánh giá xe & duyệt đánh giá (admin)**, **quản lý đơn & thanh toán (admin)**, **thống kê — báo cáo (admin)** và **các mục quản trị catalog** — **khớp với source code hiện tại**.

**Thông báo (toast):** Sau các thao tác thêm / cập nhật / xóa / bật tắt thành công trong khu admin, hệ thống dùng toast thành công (có nút đóng, tự ẩn sau ~5 giây). Thông báo lỗi dùng toast tương tự.

---

### Cửa hàng: hiển thị xe, chi tiết, so sánh, yêu thích

**Code chính:** `HomeController`, `CarController`, `CarCompareController`, `StorefrontCarFragmentController`, `StorefrontCarListingModelHelper`, `CarQueryService`, `FavoriteController`, `FavoriteApiController`, `FavoriteService`; template `index.html`, `cars/list.html`, `fragments/storefront-car-list.html`, `fragments/storefront-filters.html`, `cars/detail.html`, `cars/compare.html`, `favorites/list.html`; `static/js/compare.js`, `static/js/storefront-listing.js`, `static/css/styles.css`; `StorefrontListingFilter`, `StorefrontCarDefinitionSpecification`, `StorefrontFilterOptions`.

#### Hiển thị danh sách xe (trang chủ & danh sách đầy đủ)

- **Trang chủ — `GET /`:** Hiển thị section **Mẫu xe đang kinh doanh** (cùng fragment danh sách xe), có phân trang (`page`, `size`, mặc định tương ứng cấu hình controller).
- **Danh sách đầy đủ — `GET /cars`:** Cùng dữ liệu và cùng fragment Thymeleaf với trang chủ, có tiêu đề trang “Danh sách xe” và breadcrumb/điều hướng phù hợp.
- **Logic dùng chung:** `StorefrontCarListingModelHelper.populateCarListing` gọi `CarQueryService.listDefinitionsForStorefront` để lấy **các mẫu xe (`CarDefinition`) đang được coi là niêm yết** (kho không tắt, mẫu và cây thương hiệu / dòng / phân khúc hoạt động — khớp điều kiện trong service).
- **Thẻ xe (fragment `storefront-car-list`):** Ảnh đại diện (hoặc placeholder), tên mẫu (link tới chi tiết), **năm sản xuất**, badge **Còn hàng** / **Hết xe** theo tổng tồn, giá (kèm giá gạch nếu có khuyến mãi), nút **Xem chi tiết**, liên kết **Liên hệ** / **Lái thử**, nút **So sánh** (JavaScript), nút **tim** (yêu thích — xem mục Yêu thích bên dưới).
- **Đã đăng nhập:** `favoriteDefinitionIds` được nạp bằng `FavoriteRepository.findCarDefinitionIdsByUser` (truy vấn ID mẫu xe trực tiếp, tương thích **`spring.jpa.open-in-view: false`**). Thẻ xe có tim **đỏ / viền** khớp trạng thái; **các mẫu đã yêu thích được sắp lên đầu** danh sách trong cùng trang.
- **Spring Security:** Tham số `FcarUserDetails principal` trên `HomeController` / `CarController` dùng **`@AuthenticationPrincipal`** để luôn nhận đúng user khi đã đăng nhập (nếu thiếu annotation, server không biết user → tim không khớp sau khi F5).

#### Tìm kiếm và lọc danh sách xe (trang chủ & `/cars`)

Ô **tìm kiếm** và **bộ lọc** cột bên trái dùng **cùng một bộ điều kiện**: mọi thay đổi (gõ từ khóa, tick checkbox, “Chỉ xe còn hàng”) đều cập nhật danh sách qua cùng luồng dữ liệu. **Giữa các nhóm điều kiện là AND**; **trong cùng một nhóm nhiều ID được chọn là OR** (ví dụ tick Honda và Toyota → hiển thị xe thuộc một trong hai hãng).

**Tham số URL (query string), khớp server:**

| Tham số | Ý nghĩa |
|---------|---------|
| `q` | Từ khóa tìm theo tên **hãng**, **dòng xe**, **phân khúc** (nếu có), và **năm sản xuất** (so khớp dạng chuỗi). Kết hợp với các lọc khác bằng **AND**. |
| `brands` | Danh sách ID thương hiệu, định dạng **`id1,id2,...`**. |
| `models` | Danh sách ID dòng xe, **`id1,id2,...`**. |
| `segments` | Danh sách ID phân khúc, **`id1,id2,...`**. |
| `inStock` | `true` = chỉ các mẫu xe còn **ít nhất một** dòng tồn kho hoạt động (`CarInventory` không `disabled`, `quantity > 0`). |

**Luồng server**

- `GET /` và `GET /cars` đọc các tham số trên, gom vào `StorefrontListingFilter` (`StorefrontListingFilter.fromHttpParams`), rồi `CarQueryService.listDefinitionsForStorefront(page, size, filter)`.
- Khi **không** có điều kiện lọc/tìm “có hiệu lực” (ô `q` trống và không tick gì thêm so với mặc định), service dùng truy vấn ID niêm yết tối ưu `CarDefinitionRepository.findListedIds` (cùng quy tắc “niêm yết” với phần còn lại của storefront).
- Khi **có** ít nhất một trong `q` / `brands` / `models` / `segments` / `inStock=true`, service áp dụng **`StorefrontCarDefinitionSpecification`** (JPA Criteria) trên `CarDefinition`: điều kiện niêm yết + `LIKE` cho `q` (nếu có) + `IN` theo ID đã chọn + tồn kho (nếu bật). Thứ tự sắp xếp: tên hãng → tên dòng → năm SX.
- **Tùy chọn checkbox** (hãng / dòng / phân khúc) được build từ các mẫu đang niêm yết: `CarQueryService.loadStorefrontFilterOptions()` + `CarDefinitionRepository.findDistinctListedBrandIds` (và tương tự cho model/segment).

**Luồng giao diện (không reload trang)**

- Fragment HTML: **`GET /fragments/storefront-car-listing`** (`StorefrontCarFragmentController`), trả về nội dung `fragments/storefront-car-list :: carListingContent` (chỉ phần lưới xe + phân trang), dùng chung model với trang đầy đủ. Endpoint này **permitAll** trong `SecurityConfig` (giống `/`, `/cars`).
- **`storefront-listing.js`:** `fetch` fragment với cùng tham số hiện tại; ô tìm có **debounce**; đổi checkbox hoặc “Chỉ xe còn hàng” thì gọi ngay; đồng bộ **địa chỉ trình duyệt** bằng `history.replaceState` (giữ `q`, `brands`, `models`, `segments`, `inStock` trên `/` hoặc `/cars`).
- **Cascade UI:** khi chọn hãng, danh sách **dòng xe** chỉ hiện các dòng thuộc hãng đã chọn (ẩn bằng class, theo `data-brand-id`); tương tự **phân khúc** theo **dòng** (`data-model-id`). Nút **Xóa bộ lọc** xóa `q`, bỏ tick tất cả và gọi lại fragment.
- Dòng trạng thái dưới ô tìm (“Tìm thấy *n* mẫu…” / “Không có mẫu…”) cập nhật sau mỗi lần tải fragment thành công. Nếu request lỗi, script hiển thị thông báo lỗi và **không** giữ lưới xe cũ để tránh lệch với bộ lọc mới.

**Phân trang:** Link trang trước/sau trong fragment giữ nguyên `q` và các tham số lọc (Thymeleaf `listingFilter.*`).

#### Xem chi tiết xe (`GET /cars/{id}`)

- **Controller:** `CarController.carDetail` — tải mẫu xe qua `CarQueryService.findListedDefinitionForDetail(id)`; không tồn tại hoặc không còn niêm yết → **404**.
- **Hiển thị:** Ảnh đại diện, carousel ảnh phụ (nếu có), **tiêu đề** (formatter thương hiệu + dòng + … + năm), **nút tim** góc phải cạnh tiêu đề (thêm / bỏ yêu thích qua API, không rời trang), thông số cơ bản, accordion **Thông số khác** (thuộc tính tùy chỉnh), khối **Giá bán** + badge tồn + **tổng tồn kho toàn hệ thống**.
- **Hành động:** **Đặt mua xe** (riêng một hàng, chỉ khi còn tồn), nhóm nút **Liên hệ tư vấn**, **Đăng ký lái thử**, **So sánh**; block mô tả (nếu có); phần **đánh giá** (đọc + form viết khi đủ điều kiện) — chi tiết luồng nghiệp vụ và kỹ thuật ở mục **Đánh giá xe (khách & admin)** ngay bên dưới.
- **Yêu thích trên server:** `favorite = !favoriteRepository.findByUserAndCarDefinitionId(user, id).isEmpty()` khi có principal — dùng chung `@AuthenticationPrincipal`.

#### Đánh giá xe (khách & admin)

**Mục đích:** Khách đã có đơn **đã giao xe** (`OrderStatus.DELIVERED`) với đúng mẫu xe trên trang chi tiết có thể gửi hoặc cập nhật **một** đánh giá (sao 1–5, nhận xét tùy chọn) cho mẫu đó. Nội dung **không** hiển thị công khai ngay: **admin** phải **duyệt** (hoặc có thể **ẩn** sau khi đã hiển thị).

**Dữ liệu & bảo mật**

- Entity **`CarReview`**: gắn `user`, `carInventory`, `order` (đơn đã giao), `rating`, `comment`, cờ **`hidden`** (boolean, cột `car_reviews.hidden` trong SQL Server).
- **Khách gửi / sửa:** `POST /cars/{definitionId}/reviews` — `CarReviewController`: kiểm tra đăng nhập, mẫu xe còn niêm yết, tồn tại đơn **DELIVERED** của user với mẫu đó; tạo hoặc cập nhật bản ghi duy nhất theo `(user, carInventory, order)`; sau lưu luôn đặt **`hidden = true`** (chờ duyệt); flash thông báo đã gửi và sẽ hiển thị sau khi duyệt.
- **Storefront chỉ đọc công khai:** `CarReviewRepository.findVisibleByCarDefinitionId(definitionId)` — điều kiện **`r.hidden = false`** và `ci.carDefinition.id = :defId`. Điểm trung bình và số lượt dùng `getAggregateStatsByCarDefinitionId` với cùng điều kiện ẩn/hiện.
- **`spring.jpa.open-in-view: false`:** Truy vấn danh sách đánh giá hiển thị trên trang xe dùng **`JOIN FETCH r.user`** để nạp sẵn tên khách (view dùng `r.user.fullName`), tránh `LazyInitializationException` khi render Thymeleaf.

**Admin — duyệt / ẩn**

- **Đường dẫn:** `GET /admin/reviews` — sidebar **Duyệt đánh giá**; cần **`ROLE_ADMIN`** (cùng rule `/admin/**`).
- **Code:** `AdminCarReviewController`; template `admin/reviews/list.html`; danh sách nạp bằng `findAllForAdminOrderByCreatedAtDesc()` (fetch graph đủ user, xe, đơn).
- **Lọc:** `?filter=all` | `pending` (đang ẩn / chờ duyệt) | `visible` (đang hiển thị).
- **Thao tác:** `POST /admin/reviews/{id}/approve` → `hidden = false`; `POST /admin/reviews/{id}/hide` → `hidden = true`. Form có CSRF; redirect giữ bộ lọc (tham số `filter`).
- **Cột “Mã mẫu xe”:** Hiển thị `car_definition_id` của dòng kho gắn đánh giá và link **Xem trang** tới **`/cars/{id}`** đúng mẫu đó — tránh nhầm khi thử nghiệm (ví dụ đánh giá thuộc mẫu id 2 nhưng mở nhầm `/cars/1`).

**Dữ liệu cũ (một lần trên DB đã có sẵn)**

- Script tùy chọn: `src/main/resources/db/migration-reviews-all-pending-moderation.sql` — `UPDATE dbo.car_reviews SET hidden = 1` để **toàn bộ** đánh giá hiện có chuyển sang chờ duyệt (sau khi bật chính sách duyệt). Database mới, chưa có bản ghi `car_reviews`, thường **không** cần chạy.

**Code tham chiếu nhanh:** `CarController` (model `reviews`, `reviewCount`, `reviewAverage`, `canReview`, `reviewForm`), `CarReviewController`, `CarReviewRepository`, `cars/detail.html` (khối đánh giá + modal), `fragments/admin-sidebar.html`.

#### So sánh xe (`GET /cars/compare`)

- **URL:** `/cars/compare` với tham số tùy chọn **`?ids=id1,id2,id3`** (tối đa **3** mẫu xe). `CarCompareController` chỉ chấp nhận ID hợp lệ và vẫn còn “niêm yết” (`findListedDefinitionForDetail`); ID lỗi bị bỏ qua.
- **Bảng so sánh:** Thông số cố định + cột động theo **thông số khác** (giao thoa tên thuộc tính giữa các xe); có **Chỉ xem các thông số khác nhau** (`initCompareDiffFilter` trong `compare.js`).
- **Thêm xe từ thẻ / chi tiết:** `compare.js` lưu danh sách ID trong **`localStorage`** (`compareCars`), đồng bộ với `?ids=` khi mở trang so sánh; giới hạn 3 xe, có thông báo khi đủ chỗ hoặc trùng.
- **Trang so sánh:** Có modal chọn thêm xe từ danh sách gợi ý (`listDefinitionsForComparePicker`); có thể gỡ xe khỏi so sánh (redirect cập nhật URL).

#### Yêu thích

- **Mục đích:** User đã đăng nhập lưu mẫu xe vào bảng **`favorites`** (gắn với một dòng **`CarInventory`** đại diện qua `CarQueryService.resolveRepresentativeInventory` khi thêm mới).
- **API không redirect:** `POST /api/favorites/toggle/{definitionId}` (`FavoriteApiController`) trả JSON `{ "favorited": true|false }`. Gọi từ `compare.js` (`toggleFavoriteFromList`) bằng `fetch` + CSRF; **chặn bubble** để bấm tim trên thẻ không điều hướng nhầm. Chưa đăng nhập → **401** → redirect `/auth/login?redirect=...`.
- **Giao diện:** Tim trên thẻ listing và cạnh tiêu đề chi tiết cập nhật class/icon sau khi thành công; **toast** storefront (thêm / gỡ) qua `showFavoriteActionToast`.
- **Danh sách yêu thích — `GET /favorites`:** Cần đăng nhập; hiển thị các xe đã lưu (`FavoriteController` + `findByUserWithDetails`). Có thể **bỏ yêu thích** qua link GET `/favorites/toggle/{id}?redirect=/favorites` (luồng redirect truyền thống).
- **GET `/favorites/toggle/{id}` (không tham số redirect):** Sau khi toggle vẫn redirect về `/cars/{id}` — dùng khi mở trực tiếp URL; luồng chính trên thẻ/chi tiết dùng **POST API** để không rời trang.

#### Kiểm tra độ hoàn chỉnh (storefront)

| Hạng mục | Trạng thái | Ghi chú |
|----------|------------|---------|
| Danh sách niêm yết + phân trang | Hoàn chỉnh | Thứ tự trong DB: `brand.name` → `model.name` → `productionYear` (`findListedIds`). Đã đăng nhập: thêm **ưu tiên xe yêu thích lên đầu** *trong cùng một trang* (`StorefrontCarListingModelHelper`). **Tìm kiếm + lọc** (`q`, `brands`, `models`, `segments`, `inStock`) và fragment `/fragments/storefront-car-listing` — xem mục **Tìm kiếm và lọc danh sách xe**. |
| Chi tiết xe, tồn, CTA | Hoàn chỉnh | 404 khi không còn niêm yết; đánh giá khi đủ điều kiện đơn **DELIVERED**; chỉ thấy đánh giá **đã duyệt** (`hidden = false`); xem mục **Đánh giá xe (khách & admin)**. |
| So sánh tối đa 3 xe | Hoàn chỉnh | `compare.js` + `localStorage`; đồng bộ URL trên trang so sánh; lọc “chỉ khác nhau”. |
| Yêu thích (API + hiển thị sau F5) | Hoàn chỉnh | `@AuthenticationPrincipal` + `findCarDefinitionIdsByUser`; toast khi thêm/gỡ. |
| Trang `/favorites` | Hoàn chỉnh | Bỏ yêu thích bằng GET có `redirect` (khác API POST trên thẻ). |

**Chưa có (không phải lỗi, là phạm vi mở rộng):** **So sánh** không đồng bộ lên tài khoản (chỉ trình duyệt). Dòng phụ trên trang chủ *“Sắp xếp theo hãng”* đúng với query; khi đã đăng nhập, các xe đã tim vẫn **được đưa lên đầu trang hiện tại**, có thể làm lệch nhẹ so với thứ tự thuần hãng trên trang đó (chấp nhận được).

---

### Tài khoản khách hàng: đăng ký, đăng nhập, quên / đổi mật khẩu, hồ sơ

**Code chính:** `AuthController`, `OtpAuthController`, `ForgotPasswordController`, `AccountProfileController`, `AccountPhoneModalApiController`, `AccountProfileApiController`, `SecurityConfig`, `LoginSuccessHandler`, `PostLoginRedirectService`, `FcarUserDetailsService`, `OtpService`, `UserService`, `GlobalModelAttributes`, `PhoneNumberRequiredInterceptor`; template `auth/*.html`, `account/profile.html`, `account/change-password.html`, `layout.html` (menu người dùng, modal bắt buộc SĐT).

#### Đăng ký (`/auth/register`)

- **GET:** Hiển thị form: họ tên, SĐT, email, mật khẩu, xác nhận mật khẩu; CSRF; checkbox **Hiển thị mật khẩu** (áp dụng cho hai ô mật khẩu).
- **POST:** `@Valid` trên `RegisterForm`, `@InitBinder` trim chuỗi; kiểm tra **SĐT Việt Nam** (10 số, bắt đầu 0, không chữ) qua `VietnamPhoneRules`; **mật khẩu mạnh** (tối thiểu 8 ký tự, có chữ hoa, số, ký tự đặc biệt); **khớp xác nhận**; **trùng email / SĐT** → báo lỗi theo từng trường (`rejectValue`); email chuẩn hóa **chữ thường** khi lưu.
- **Thành công:** `redirect:/auth/login?registered`.
- **Giao diện lỗi:** Bootstrap `is-invalid` + `th:errors` dưới từng ô; khối global cho lỗi không gắn field.

#### Đăng nhập

1. **Form email hoặc SĐT + mật khẩu** — `/auth/login`
   - POST tới `/auth/login` (Spring Security `formLogin`), CSRF trong form.
   - `FcarUserDetailsService`: tìm user theo **email** hoặc **SĐT** (`findByPhone`).
   - **Thành công:** `LoginSuccessHandler` + `PostLoginRedirectService` (URL an toàn `redirect` param → `SavedRequest` → `/` hoặc `/admin` nếu `ROLE_ADMIN`).
   - Checkbox **Hiển thị mật khẩu** (client); link quên mật khẩu, đăng ký, đăng nhập OTP, Google.

2. **Đăng nhập bằng SĐT (OTP)** — `/auth/login-phone`
   - Gửi OTP (`OtpService.sendLoginPhoneOtp`), nhập OTP, xác minh rồi **tạo `SecurityContext` và `SecurityContextRepository.saveContext`** (giữ phiên sau redirect).
   - Khi bật `fcar.otp.log-phone-delivery` (mặc định trong `application.yml`): **hiển thị mã demo** trong box info (không điền sẵn ô nhập), đồng thời log server.
   - Form có CSRF; ràng buộc SĐT/OTP giống luồng phone khác.

3. **Đăng nhập Google** — OAuth2, `GoogleOAuth2SuccessHandler`: kiểm tra `email_verified`, tạo/lấy user, redirect qua `PostLoginRedirectService`.

#### Quên mật khẩu (`/auth/forgot-password`)

- Hai bước trên cùng form model: **gửi OTP qua email** và **đặt lại mật khẩu**.
- Bean Validation **theo nhóm** (`OnSendOtp` / `OnReset`) để POST gửi OTP không fail vì các field bước 2 đang trống.
- **Gửi OTP:** Chuẩn hóa email; kiểm tra **email đã đăng ký** (`existsByEmailIgnoreCase` + `OtpService` với `findByEmailIgnoreCase`); gửi mail chứa OTP; lỗi gửi mail / giới hạn OTP → thông báo global.
- **Đặt lại:** Mã OTP 6 số, mật khẩu mới đúng **policy** + khớp xác nhận; `verifyOtp` loại `FORGOT_PASSWORD`; lỗi OTP gắn field `code`.
- **Giao diện:** CSRF, bubble lỗi từng trường; checkbox **Hiển thị mật khẩu** cho mật khẩu mới + xác nhận.
- **Thành công:** `redirect:/auth/login?passwordReset`.

#### Đổi mật khẩu (`/account/change-password`, cần đăng nhập)

- **GET/POST:** `ChangePasswordForm` với `@Valid`, trim qua `@InitBinder("form")`.
- Kiểm tra **mật khẩu hiện tại** (`PasswordEncoder.matches`), **policy** mật khẩu mới, **khớp xác nhận**, không trùng mật khẩu cũ; cập nhật hash và **hết phiên các thiết bị khác** (`SessionService.expireOtherSessions`).
- **Giao diện:** card, CSRF, bubble lỗi, gợi ý policy, checkbox hiện mật cho cả 3 ô; thông báo thành công khi `?changed`.

#### Thông tin cá nhân (`/account/profile`, cần đăng nhập)

- **Hiển thị:** Hồ sơ user (họ tên, email, SĐT, avatar, giới tính, ngày sinh, địa chỉ, …), danh sách **SĐT phụ** (`user_phones`), luồng xác nhận email/OTP tùy trạng thái tài khoản.
- **Cập nhật / API:** Nhiều thao tác qua REST (`/account/api/...`): cập nhật hồ sơ, upload avatar, modal thêm SĐT + OTP; **`PhoneNumberRequiredInterceptor`:** user **customer** chưa có SĐT chính và không có SĐT phụ thì gắn cờ **bắt nhập SĐT** (modal toàn layout, trừ admin và một số path API).
- **Điều hướng sau đăng nhập:** Tránh kẹt khi thiếu SĐT nhưng vẫn cho dùng app với modal.

#### Đăng xuất

- `POST /logout` — **`logoutSuccessUrl`:** `/auth/login` (về trang đăng nhập sau khi đăng xuất).

---

### Mua xe (khách hàng — niêm yết, chi tiết, đặt mua)

**Code chính:** `CarController`, `OrderController`, `CarQueryService`, `CarOrderDepositService`; template `cars/list.html`, `cars/detail.html`, `orders/deposit.html`, `orders/error.html`.

- **Danh sách, chi tiết, so sánh, yêu thích (giao diện & luồng đầy đủ):** xem mục **Cửa hàng: hiển thị xe, chi tiết, so sánh, yêu thích** ở trên. Phần dưới tập trung **đặt mua / đặt cọc** từ trang chi tiết.
- **Tóm tắt:** Trang `/cars` phân trang các mẫu xe đang niêm yết. Trang `/cars/{id}` có **tổng tồn kho** (`totalStock`), badge **Còn hàng** / **Hết xe**; nút **Đặt mua xe** chỉ hiện khi `totalStock > 0`. Liên hệ, lái thử, yêu thích, so sánh vẫn dùng được khi hết hàng (chi tiết UX trong mục Cửa hàng).
- **Luồng đặt mua:** `GET /orders/buy/{definitionId}` (cần đăng nhập) redirect tới **`/orders/deposit/{definitionId}`**. Hệ thống chọn **một dòng kho đại diện** (`CarQueryService.resolveRepresentativeInventory`): ưu tiên dòng còn `quantity > 0` trong các kho không bị tắt.
- **Khi hết xe:** Nếu không còn xe (`quantity <= 0`), trang đặt cọc trả về `orders/error` với thông báo thân thiệi; các lần thử PayOS / xác nhận chuyển khoản bị chặn kèm flash *«Xe đã hết hàng.»*
- **Sau khi đặt cọc thành công:** `CarOrderDepositService.createDepositedOrder` tạo đơn trạng thái **DEPOSITED**, **trừ 1** trên đúng dòng `CarInventory` đã gắn với đơn, và gửi **email HTML** xác nhận (`OrderEmailHtmlService`).

---

### Thanh toán đặt cọc (cấu hình, PayOS, chuyển khoản thủ công)

**Code chính:** `OrderController`, `PayOsDepositService`, `CarOrderDepositService`, `VietQrImageService` (chuẩn hóa nội dung CK); admin: `AdminPaymentController`; entity `PaymentConfig`, `StoreBankAccount`, `SupportHotline`.

- **Cấu hình admin (`/admin/payment`):**
  - **Phần trăm đặt cọc** (`PaymentConfig.depositPercent`): ví dụ 20% nghĩa là tiền cọc = **giá niêm yết (`salePrice`)** × (phần trăm / 100). Hệ thống dùng **`salePrice`**, không tự lấy `promoPrice` làm cơ sở tính cọc trong code hiện tại.
  - **Tài khoản ngân hàng showroom** (`StoreBankAccount`): hiển thị tài khoản active đầu tiên trên trang đặt cọc cho khách chuyển khoản thủ công.
  - **Hotline hỗ trợ:** thêm / bật tắt danh sách số điện thoại.
- **Trang đặt cọc (`/orders/deposit/{definitionId}`):** Hiển thị số tiền cọc, mô tả % cọc, **nội dung chuyển khoản bắt buộc khớp** (chuỗi rút gọn kiểu `FCAR coc D{id} U{userId}`, qua `VietQrImageService.sanitizeVietQrText`), thông tin STK (sao chép), form **Xác nhận đã chuyển khoản** (`POST /orders/deposit/confirm`): server so khớp **đúng số tiền** với cấu hình hiện tại; nếu khớp thì tạo đơn như mục trên.
- **PayOS (nếu bật trong `application.yml` và đủ credential):** Nút **Thanh toán đặt cọc qua PayOS** → `POST /orders/deposit/payos/start` tạo phiên thanh toán và redirect sang PayOS; sau khi thanh toán thành công, **webhook / luồng return** (`/orders/deposit/payos/return`) hoàn tất tạo đơn **DEPOSITED** và trừ tồn (không cần bấm xác nhận CK thủ công). Cần có **cấu hình thanh toán** (repository `PaymentConfig`) — nếu chưa có bản ghi, luồng đặt cọc báo lỗi cấu hình.
- **Thông báo:** Các sự kiện quan trọng (đặt cọc thành công, hủy, giao xe, hoàn tiền) gửi **email HTML** qua `MailNotificationService.sendHtml` / `OrderEmailHtmlService` (SMTP trong `spring.mail.*`).

---

### Lịch sử mua hàng & chi tiết đơn (khách hàng)

**Code chính:** `AccountController`, `OrderController`; template `account/history.html`, `orders/order-detail.html`.

- **Lịch sử tổng hợp (`/account/history`, cần đăng nhập):** Một trang hiển thị **đơn đặt mua xe** (danh sách `CarOrder` của user, kèm thông tin xe), đồng thời **yêu cầu liên hệ tư vấn** và **đăng ký lái thử** của cùng user — tiện theo dõi mọi tương tác với showroom. **Chi tiết luồng, URL, trạng thái và email:** xem mục **Liên hệ tư vấn** và **Đăng ký lái thử** ngay bên dưới.
- **Chi tiết đơn (`/orders/detail/{orderId}`):** Chỉ chủ đơn mới xem được; hiển thị trạng thái (**DEPOSITED**, **DELIVERED**, **CANCELED**, **REFUNDED**), giá, cọc, tiến trình; có thể **hủy đơn** khi đơn đang **DEPOSITED** (`POST /orders/{orderId}/cancel`) → chuyển **CANCELED** (tồn kho **chưa** cộng lại; admin xử lý hoàn tiền sau).
- **Hóa đơn / chứng từ in cho khách:** `GET /orders/{orderId}/invoice` **không** mở trang in — redirect về lịch sử kèm thông báo in chỉ dành cho showroom. **In chứng từ nội bộ** nằm ở admin (`/admin/orders/{id}/invoice`).

---

### Liên hệ tư vấn

**Code chính:** `ContactController`, `AdminContactRequestController`, `ContactRequestRepository`, entity `ContactRequest`, enum `ContactStatus` (nhãn tiếng Việt cho UI/email), `ContactTestDriveEmailHtmlService`, `CustomerEmailHtmlShell`, `MailNotificationService`; template `contacts/request.html`, `contacts/already.html`, `admin/contacts/list.html`, `admin/contacts/detail.html`. Liên kết CTA từ `cars/detail.html`, `fragments/storefront-car-list.html`, `favorites/list.html`.

**Bảo mật:** Mọi endpoint `/contacts/**` yêu cầu **đã đăng nhập** (không `permitAll`). Chưa đăng nhập khi mở form → redirect `/auth/login?redirect=/contacts/request/{definitionId}`.

#### Khách hàng

| Đường dẫn | Mô tả |
|-----------|--------|
| `GET /contacts/request/{definitionId}` | Form gửi yêu cầu. Hệ thống chọn **một dòng kho đại diện** (`CarQueryService.resolveRepresentativeInventory`) cho mẫu xe. Nếu user đã có yêu cầu **PENDING** cùng xe → hiển thị `contacts/already` (trừ `?forceNew=true`). Trang form liệt kê **các chi nhánh** đang có mẫu xe (query `JOIN FETCH` trên kho, tránh lỗi lazy khi `spring.jpa.open-in-view: false`). |
| `POST /contacts/request/{definitionId}` | Tạo `ContactRequest` trạng thái **PENDING**, gửi **email HTML** xác nhận (`sendContactRequestSubmitted`), flash thành công, redirect **`/account/history?tab=contacts`**. Form có CSRF. |
| `GET /contacts/cancel/{id}` | Chỉ **chủ yêu cầu** và chỉ khi trạng thái **PENDING**; đổi sang **CANCELED**, gửi email HTML (`sendContactRequestCanceledByCustomer`), redirect lịch sử. *(Thao tác hủy qua GET — có thể chuyển POST + CSRF nếu muốn chặt hơn.)* |

#### Lịch sử cá nhân

Trong **`/account/history`**, tab **Lịch sử yêu cầu liên hệ**: danh sách từ `ContactRequestRepository.findByUserWithCarDetails` (fetch đủ để render). Cột trạng thái hiển thị **tiếng Việt** (Chờ xử lý / Đã liên hệ / Đã hủy); nút **Hủy** chỉ khi **PENDING**.

#### Quản trị (`ROLE_ADMIN`)

| Đường dẫn | Mô tả |
|-----------|--------|
| `GET /admin/contacts` | Danh sách yêu cầu; **lọc năm / tháng** (cùng quy tắc tháng-năm như quản lý đơn) và **lọc trạng thái**; nhãn trạng thái trong dropdown **tiếng Việt**. Dữ liệu: `findAllWithUserAndCarDetails()`. |
| `GET /admin/contacts/{id}` | Chi tiết: khách, xe, trạng thái; form đổi trạng thái (chỉ khi chưa kết thúc). |
| `POST /admin/contacts/{id}/status` | Từ **PENDING** chỉ sang **CONTACTED** hoặc **CANCELED**; terminal (**CONTACTED** / **CANCELED**) không đổi lại được. Lưu DB, gửi **email HTML** (`sendContactRequestStatusUpdated`), redirect chi tiết + flash. |

#### Email cho khách

Giống luồng đặt cọc: nội dung **HTML** bọc trong `CustomerEmailHtmlShell`, gửi qua `MailNotificationService.sendHtml` — **lỗi SMTP chỉ ghi log**, không làm hỏng giao dịch nghiệp vụ. Có đủ cho: đăng ký thành công, khách hủy, admin cập nhật (Đã liên hệ / Đã hủy).

#### Trạng thái (`ContactStatus`)

- **PENDING** — Chờ xử lý  
- **CONTACTED** — Đã liên hệ  
- **CANCELED** — Đã hủy  

*Số yêu cầu liên hệ ghi nhận trong tháng/năm còn được dùng trong màn **Thống kê — báo cáo** — xem mục **Thống kê — báo cáo (admin)** bên dưới.*

---

### Đăng ký lái thử

**Code chính:** `TestDriveController`, `AdminTestDriveController`, `TestDriveBookingRepository`, entity `TestDriveBooking`, enum `TestDriveStatus`, `ContactTestDriveEmailHtmlService`; template `testdrives/register.html`, `testdrives/already.html`, `admin/testdrives/list.html`, `admin/testdrives/detail.html`. CTA trên storefront giống liên hệ tư vấn.

**Bảo mật:** `/test-drives/**` cần **đăng nhập**; redirect `/auth/login` kèm `redirect` giữ nguyên URL đăng ký.

#### Khách hàng

| Đường dẫn | Mô tả |
|-----------|--------|
| `GET /test-drives/register/{definitionId}` | Form: chọn **ngày**, **giờ**, xe (hidden `carId` từ kho đại diện). Danh sách chi nhánh hiển thị theo mẫu xe (`distinctBranchesForDefinition`). `registerDateMin` = hôm nay (tránh `T(...)` trong Thymeleaf). Nếu user đã có booking **chưa CANCELED** cho cùng xe → `testdrives/already`; có thể **`?forceNew=true`** để bỏ qua và mở form (vẫn kiểm tra trùng slot lúc POST). |
| `POST /test-drives/register` | Model `RegisterForm` (`carId`, `date`, `time`). Kiểm tra **trùng lịch với khách khác** (cùng `CarInventory` + `testDateTime`, user khác đã đặt). Tạo `TestDriveBooking`: **PENDING**, gắn `branch` từ kho xe, gửi email HTML, redirect **`/account/history?tab=testdrives`**. CSRF. |
| `GET /test-drives/cancel/{id}` | Chủ booking và trạng thái **PENDING** hoặc **APPROVED** → **CANCELED**, email HTML, redirect lịch sử. |

#### Lịch sử cá nhân

Tab **Lịch sử đăng ký lái thử**: `findByUserWithCarDetails`. Cột **Giờ lái thử đăng ký** là `testDateTime`; badge trạng thái tiếng Việt; **Hủy** khi còn **PENDING** hoặc **APPROVED**.

#### Quản trị

| Đường dẫn | Mô tả |
|-----------|--------|
| `GET /admin/test-drives` | Danh sách + lọc **năm / tháng / trạng thái** (nhãn VN). `findAllWithUserAndCarDetails()`. |
| `GET /admin/test-drives/{id}` | Chi tiết đầy đủ (fetch user, xe, chi nhánh …). |
| `POST /admin/test-drives/{id}/status` | Máy trạng thái: **PENDING → APPROVED** hoặc **CANCELED**; **APPROVED → COMPLETED** hoặc **CANCELED**; terminal (**COMPLETED** / **CANCELED**) không đổi. Lưu, email HTML theo trạng thái mới (`sendTestDriveBookingStatusUpdated`), redirect chi tiết. |

#### Email cho khách

Cùng stack HTML như liên hệ và đơn hàng: đăng ký thành công, khách hủy, admin duyệt / hoàn thành / hủy (nội dung và màu header/badge khác nhau theo sự kiện).

#### Trạng thái (`TestDriveStatus`)

- **PENDING** — Chờ xác nhận  
- **APPROVED** — Đã duyệt  
- **COMPLETED** — Hoàn thành  
- **CANCELED** — Đã hủy  

#### Ghi chú kỹ thuật

Repository dùng **`JOIN FETCH`** / `DISTINCT` cho list và `findByIdWithUserAndCarDetails` cho chi tiết admin, phù hợp **`spring.jpa.open-in-view: false`**.

---

### Quản lý đơn đặt hàng (admin)

**Đường dẫn:** `/admin/orders`  
**Code chính:** `AdminOrderController`, `CarOrderRefundRules`, `OrderEmailHtmlService`; template `admin/orders/list.html`, `admin/orders/detail.html`, `admin/orders/invoice-print.html`.

- **Danh sách:** Toàn bộ đơn (nạp qua repository có fetch graph), **lọc theo năm / tháng** và **theo trạng thái** (`OrderStatus`). Bảng hiển thị mã đơn, khách, xe, trạng thái, thời gian tạo.
- **Chi tiết đơn (`/admin/orders/{id}`):** Ảnh xe, thông tin khách, thanh toán (giá, cọc, hoàn nếu có), ảnh chứng từ hoàn tiền (khi **REFUNDED**), tiến trình trạng thái.
- **Thao tác theo trạng thái:**
  - **DEPOSITED → DELIVERED:** Form **Đánh dấu đã giao xe** (`POST .../status`, `status=DELIVERED`) — gửi email thông báo giao xe.
  - **DEPOSITED → CANCELED (admin):** **Hủy đơn** — áp dụng quy tắc hoàn (`CarOrderRefundRules.applyCanceled`), email thông báo hủy từ phía showroom.
  - **CANCELED → REFUNDED:** **Xác nhận đã hoàn tiền** bắt buộc **upload ảnh chứng từ** (ảnh); lưu file dưới `uploads/refunds/`, ghi URL vào đơn, **cộng lại +1** tồn kho trên `CarInventory` gắn đơn, ghi nhận số tiền hoàn, gửi email hoàn tất.
- **In chứng từ / phiếu nội bộ:** `GET /admin/orders/{id}/invoice` — trang HTML tối ưu in (`window.print`), giao diện khác nhau theo trạng thái đơn; nút **In chứng từ** trên chi tiết admin mở tab mới.

**Trạng thái đơn (`OrderStatus`):** `DEPOSITED` (đã cọc), `DELIVERED` (đã giao xe), `CANCELED` (đã hủy, chờ hoàn), `REFUNDED` (đã hoàn tiền, kết thúc — tồn đã hoàn lại).

---

### Duyệt đánh giá xe (admin)

**Đường dẫn:** `GET /admin/reviews`  
**Bảo mật:** Cùng rule **`/admin/**`** — cần **`ROLE_ADMIN`**.  
**Code chính:** `AdminCarReviewController`, `CarReviewRepository` (danh sách admin + truy vấn hiển thị storefront), entity `CarReview`; template `admin/reviews/list.html`; mục menu sidebar **Duyệt đánh giá**.

#### Luồng nghiệp vụ (tóm tắt)

1. Khách có đơn **đã giao xe** với mẫu xe X gửi hoặc sửa đánh giá trên **`/cars/{id}`** (form modal khi `canReview = true`) → hệ thống lưu và đặt **`hidden = true`**.
2. Trên trang công khai cùng mẫu X, chỉ các đánh giá **`hidden = false`** được đếm vào điểm trung bình / số lượt và hiển thị trong danh sách.
3. Admin mở **`/admin/reviews`**, lọc **Chờ duyệt** hoặc **Tất cả**, bấm **Duyệt hiển thị** để đặt `hidden = false`, hoặc **Ẩn** để gỡ khỏi storefront (`hidden = true`). Mỗi mẫu xe có **mã định danh riêng** trong URL: dùng cột **Mã mẫu xe** + **Xem trang** để mở đúng `/cars/{id}` tương ứng với đánh giá đang xử lý.

#### Migration dữ liệu (tùy chọn)

- File **`src/main/resources/db/migration-reviews-all-pending-moderation.sql`**: cập nhật một lần để mọi bản ghi `car_reviews` cũ chuyển sang trạng thái chờ duyệt (cột `hidden = 1`). Chỉ chạy khi cần đồng bộ dữ liệu lịch sử với chính sách duyệt mới.

#### Chi tiết kỹ thuật (đồng bộ với mục Cửa hàng)

Luồng đầy đủ (entity, POST khách, JPQL `JOIN FETCH user`, script SQL) được mô tả trong mục **Đánh giá xe (khách & admin)** trong phần **Xem chi tiết xe** — tránh trùng lặp, hai mục tham chiếu lẫn nhau.

---

### Thống kê — báo cáo (admin)

**Đường dẫn:** `GET /admin/reports`  
**Bảo mật:** Cùng rule **`/admin/**`** — cần **`ROLE_ADMIN`**.  
**Code chính:** `AdminReportController`, `ReportService`, `ReportDashboardModel` (`com.fcar.service.report`); template `admin/reports/dashboard.html`; biểu đồ **Chart.js** (CDN 4.x, mixed bar + line).

#### Mục đích

Màn hình tổng hợp số liệu kinh doanh và tương tác khách theo **một tháng cụ thể** (chọn **năm** + **tháng**), kèm **xu hướng 12 tháng** trong **năm đã chọn** và **top mẫu xe** bán chạy trong tháng đó. Không dùng khoảng “từ ngày — đến ngày”; mặc định mở trang là **tháng hiện tại** nếu không có query.

#### Tham số URL

| Tham số | Bắt buộc | Ý nghĩa |
|---------|----------|---------|
| `year` | Không | Năm báo cáo (giới hạn hợp lệ trong code, ví dụ từ 2020 đến năm hiện tại + 1). Mặc định: năm hiện tại. |
| `month` | Không | Tháng 1–12. Mặc định: tháng hiện tại. |

Form **GET** trên trang: hai dropdown + nút **Xem báo cáo** (không POST).

#### Định nghĩa số liệu (theo `created_at` bản ghi)

Mốc thời gian áp dụng cho mọi đếm / cộng trong **tháng/năm đã chọn**: **`createdAt` ≥ đầu tháng 00:00:00** và **≤ cuối tháng 23:59:59** (chuẩn `LocalDateTime` trên entity kế thừa `BaseEntity`).

- **Doanh thu (đã giao xe):** Tổng **`totalPrice`** của các đơn có trạng thái **`DELIVERED`**. Đây là **giá niêm yết ghi trên đơn** (không dùng `paidAmount` / tiền cọc làm doanh thu).
- **Tổng hoàn tiền:** `COALESCE(SUM(refundedAmount), 0)` cho các đơn **`REFUNDED`** trong tháng (chỉ bản ghi có `refundedAmount`).
- **Đếm đơn theo trạng thái:** Bốn số riêng — số đơn **`DEPOSITED`**, **`DELIVERED`**, **`CANCELED`**, **`REFUNDED`** có `createdAt` trong tháng (mỗi đơn chỉ một trạng thái tại một thời điểm; đếm theo trạng thái hiện tại khi báo cáo — đơn tạo tháng trước nhưng giao tháng này vẫn có `createdAt` tháng trước, không vào KPI “tháng này” trừ khi định nghĩa lại; hệ thống hiện **nhất quán theo ngày tạo đơn**).
- **Liên hệ tư vấn:** `ContactRequestRepository.countByCreatedAtBetween` trong cùng khoảng tháng.
- **Đăng ký lái thử:** `TestDriveBookingRepository.countByCreatedAtBetween` trong cùng khoảng tháng.

#### Biểu đồ (năm = năm đã chọn trên form)

Trong **`ReportService`**, với **mỗi tháng 1…12** của **cùng năm** đó:

- **Cột (bar):** Doanh thu VNĐ (tổng `totalPrice` đơn `DELIVERED` trong tháng đó).
- **Đường (line):** Số đơn `DELIVERED` trong tháng đó.

Dữ liệu truyền cho JavaScript dạng `List<Double>` / `List<Long>` (Thymeleaf inline), trục Y kép (tiền + số xe). Tooltip format tiền theo `vi-VN`.

#### Top mẫu xe bán chạy

- JPQL: nhóm theo **`CarDefinition.id`**, đếm đơn **`DELIVERED`** trong **tháng đã chọn**, `ORDER BY COUNT` giảm, **TOP 5** (`PageRequest.of(0, 5)`).
- Hiển thị tên qua **`CarDefinitionLabelFormatter`**; nạp entity bằng `CarDefinitionRepository.findByIdsWithBrandModelSegment` để đủ brand/model/segment.

#### Tầng dữ liệu (không `findAll()` toàn bảng)

- `CarOrderRepository`: `sumTotalPriceByStatusAndCreatedAtBetween`, `countByStatusAndCreatedAtBetween`, `sumRefundedAmountByStatusAndCreatedAtBetween`, `countDeliveredByDefinitionId` (GROUP BY).
- Tránh nạp toàn bộ đơn vào bộ nhớ như phiên bản cũ.

#### Giao diện

- Hero gradient, card KPI (doanh thu, hoàn tiền, liên hệ, lái thử), bốn ô đếm trạng thái đơn (màu viền phân biệt), card biểu đồ, bảng top xe; chú thích ngắn về cách tính doanh thu (`totalPrice`).

#### Liên quan README khác

- Sidebar admin: **Thống kê - báo cáo** → `/admin/reports`.
- **Quản lý đơn**, **Liên hệ**, **Lái thử** mô tả nghiệp vụ từng màn; mục này mô tả **cách gộp số** trên một màn tổng hợp.

---

### 1. Quản lý chi nhánh

**Đường dẫn:** `/admin/branches`

- **Danh sách:** Các chi nhánh chưa xóa (`deleted = false`): ID, tên, địa chỉ, SĐT, trạng thái (ACTIVE / SUSPENDED), thao tác.
- **Thêm:** Modal — tên, địa chỉ, SĐT (đúng 10 chữ số). Server kiểm tra không trùng tên / địa chỉ / SĐT với bản ghi khác. Có xác nhận trước khi gửi (client).
- **Cập nhật:** Modal tương tự; cùng quy tắc trùng với khi thêm. **Không cho sửa** nếu chi nhánh đang được dùng bởi xe trong kho còn hoạt động (`CarInventory`: `disabled = false` và `quantity > 0`).
- **Tạm ngưng / Hoạt động:** Đổi `BranchStatus` giữa `SUSPENDED` và `ACTIVE`. **Không cho tạm ngưng** nếu vẫn còn xe kho hoạt động gắn chi nhánh (điều kiện tồn kho như trên).
- **Xóa:** Chỉ **xóa cứng** khi **không** còn xe kho đang dùng chi nhánh (cùng điều kiện tồn kho). Nếu còn → toast lỗi, không xóa.

---

### 2. Quản lý thương hiệu

**Đường dẫn:** `/admin/brands`

- **Danh sách:** Thương hiệu chưa xóa: ID, tên, trạng thái (Hoạt động / Tạm ngưng), thao tác.
- **Thêm:** Modal — tên; không trùng tên (so khớp không phân biệt hoa thường, trim). Có xác nhận trước khi gửi.
- **Cập nhật:** Chỉ đổi tên; không trùng tên khác. **Không cho đổi tên** nếu thương hiệu đang được xe kho hoạt động sử dụng (theo brand trên `CarInventory`).
- **Tạm ngưng / Hoạt động (cascade — `CatalogActiveCascadeService`):**
  - **Tạm ngưng:** Không chặn theo số lượng tồn kho. Tắt `active` cho thương hiệu, toàn bộ dòng xe, phân khúc, mẫu xe thuộc cây; đồng thời đặt `CarInventory.disabled = true` cho các dòng kho liên quan.
  - **Bật lại:** Bật thương hiệu rồi cascade bật xuống dưới và mở lại `disabled = false` cho tồn kho tương ứng.
- **Xóa:** Chỉ **xóa cứng** khi **không** còn **dòng xe** con trực tiếp (`CarModel` với `deleted = false`). Nếu còn dòng xe → toast lỗi, không xóa.

---

### 3. Quản lý dòng xe

**Đường dẫn:** `/admin/models`

- **Danh sách & lọc:** Bảng dòng xe chưa xóa; có lọc theo thương hiệu (tự submit form khi đổi dropdown).
- **Thêm:** Modal — chọn thương hiệu, tên dòng; không trùng tên trong cùng thương hiệu. Nếu thương hiệu đang tạm ngưng, dòng xe mới mặc định không hoạt động.
- **Cập nhật:** Trang chỉnh sửa (theo route controller); không đổi thương hiệu; không trùng tên. **Không cho sửa tên** khi dòng xe đang được xe kho hoạt động sử dụng.
- **Tạm ngưng / Hoạt động (cascade):**
  - **Tạm ngưng:** Cascade xuống phân khúc, mẫu xe và tồn kho (giống luồng thương hiệu), không chặn theo tồn kho.
  - **Bật lại:** Chỉ khi **thương hiệu cha** đang hoạt động; nếu không → toast lỗi (nêu rõ thương hiệu đang tạm ngưng). Cascade bật xuống và mở tồn kho tương ứng.
- **Xóa:** Chỉ **xóa cứng** khi **không** còn **phân khúc** hoặc **mẫu xe** con trực tiếp (`deleted = false`). Nếu còn → toast lỗi, không xóa.

---

### 4. Quản lý phân khúc xe

**Đường dẫn:** `/admin/segments`

- **Danh sách & lọc:** Phân khúc chưa xóa; lọc theo thương hiệu và dòng xe (form tự submit).
- **Thêm:** Modal — thương hiệu, dòng xe, tên phân khúc; không trùng tên trong cùng dòng xe. Nếu dòng xe tạm ngưng, phân khúc mới mặc định không hoạt động.
- **Cập nhật:** Trang chỉnh sửa; chỉ đổi tên phân khúc khi hợp lệ.
- **Tạm ngưng / Hoạt động (cascade):**
  - **Tạm ngưng:** Tắt phân khúc, các mẫu xe thuộc phân khúc và tồn kho liên quan; không chặn theo tồn kho.
  - **Bật lại:** Chỉ khi **dòng xe cha** (con trực tiếp) đang hoạt động; nếu không → toast lỗi (nêu rõ dòng xe). Không còn kiểm tra riêng thương hiệu trong message; cascade bật mẫu xe và tồn kho.
- **Xóa:** Chỉ **xóa cứng** khi **không** còn **mẫu xe** con trực tiếp (`CarDefinition` với `deleted = false` gắn phân khúc). Nếu còn → toast lỗi, không xóa.

---

### 5. Quản lý xe (mẫu xe + nhập xe)

Gồm hai phần chính: **danh sách mẫu xe** và **trang nhập xe** (kho + lịch sử nhập).

#### 5.1. Quản lý mẫu xe (`CarDefinition`)

**Đường dẫn:** `/admin/cars/definitions`

- **Danh sách:** Các mẫu xe chưa xóa; có lọc theo thương hiệu / dòng xe. Cột **Mẫu xe** hiển thị chuẩn: *Thương hiệu + Dòng + Phân khúc - Năm* (qua `CarDefinitionLabelFormatter`).
- **Thêm mẫu xe:** Form (modal) — thương hiệu, dòng, phân khúc, năm, kiểu dáng, giá, màu, ảnh, v.v.; không trùng bộ (thương hiệu + dòng + phân khúc + năm) với mẫu đã có.
- **Cập nhật:** Trang chỉnh sửa + API JSON; sau khi lưu redirect về danh sách với thông báo thành công (query `success=updated` hoặc flash tương đương).
- **Tạm ngưng / Hoạt động (cascade tồn kho):**
  - **Tạm ngưng:** Đặt `active = false` cho mẫu và `disabled = true` cho mọi `CarInventory` của mẫu đó.
  - **Bật lại:** Chỉ khi **phân khúc** cha còn hoạt động (hoặc nếu không có phân khúc thì kiểm tra **dòng xe**); nếu không → toast lỗi. Khi bật: `active = true` và mở lại `disabled = false` cho tồn kho tương ứng.
- **Xóa:** Chỉ **xóa cứng** khi **không** còn **bản ghi tồn kho** (`CarInventory`) nào gắn mẫu xe. Nếu còn → toast lỗi. Khi xóa được, xóa kèm màu / ảnh / thuộc tính liên quan theo code hiện tại.

**Ảnh hưởng storefront:** Trang chủ và danh sách xe chỉ hiển thị xe thỏa điều kiện (kho không `disabled`, mẫu và cây thương hiệu/dòng/phân khúc đang hoạt động, v.v.). Phân khúc `null` vẫn được xử lý trong truy vấn tìm kiếm. Chi tiết xe (`/cars/{id}`) trả **404** nếu xe không còn được coi là “niêm yết”.

#### 5.2. Nhập xe (`/admin/cars/import`)

- **Kho xe hiện tại:** Bảng các dòng tồn kho (`CarInventory`): mẫu xe (cùng formatter như trên), màu, chi nhánh, số lượng, hành động (nhập thêm, chuyển chi nhánh, …). Có **lọc theo tên mẫu xe** (dropdown theo các `CarModel` đang có trong kho; mặc định “Tất cả”; đổi lọc tự submit, giữ bộ lọc lịch sử nếu có).
- **Cột trạng thái Hoạt động / Tạm ngưng** (toggle `CarInventory.disabled`): có thể được **ẩn tạm** trong template (code vẫn còn endpoint toggle).
- **Nhập xe mới:** Modal chọn mẫu xe (từ danh sách định nghĩa), màu, chi nhánh, giá nhập, số lượng — tạo/cập nhật tồn kho và ghi **lịch sử nhập**.
- **Nhập xe theo dòng kho:** Modal điền từ một dòng trong bảng kho (mẫu/màu/chi nhánh cố định).
- **Lịch sử nhập xe:** Modal với lọc **năm / tháng / ngày** (tháng/ngày phụ thuộc năm theo dữ liệu thực tế + fallback lịch); đổi lọc tự submit; cột **Xe** dùng cùng formatter mẫu xe.

**Chuyển / gộp kho:** Trang chỉnh sửa dòng kho (chi nhánh, số lượng); nếu trùng mẫu + màu + chi nhánh với dòng khác có thể **gộp** (merge) và redirect về nhập xe kèm thông báo thành công.

**Liên quan:** Cấu hình **thanh toán đặt cọc** (phần trăm cọc, STK, hotline) xem mục **Thanh toán đặt cọc** và **`/admin/payment`**. **Đơn đặt mua xe** xem mục **Quản lý đơn đặt hàng (admin)** và **Lịch sử mua hàng & chi tiết đơn**.

---

## Cấu trúc project (tóm tắt)

```
src/main/java/com/fcar/
├── config/          # Security, Web, GlobalModelAttributes, DataInitializer
├── controller/      # Trang chủ, xe, đơn hàng, auth, profile, ...
├── controller/admin/# Các trang quản trị
├── domain/          # Entity JPA
├── repository/      # Spring Data JPA
├── security/       # UserDetails, LoginSuccessHandler, OAuth2
├── service/        # OtpService, UserService, CarQueryService, MailNotificationService, OrderEmailHtmlService, ContactTestDriveEmailHtmlService, …
├── service/email/  # CustomerEmailHtmlShell (khung email HTML gửi khách)
└── exception/      # Xử lý lỗi toàn cục

src/main/resources/
├── application.yml
├── templates/       # Thymeleaf (layout, fragments, admin, account, ...)
├── static/          # CSS, JS
└── db/
    ├── schema-sqlserver.sql
    └── migration-*.sql   # script SQL chạy tay khi cập nhật DB đã tồn tại (vd. duyệt đánh giá hàng loạt)
```

## License

Dự án nội bộ / học tập. Chỉnh sửa theo nhu cầu nhóm.
