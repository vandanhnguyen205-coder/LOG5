# LOG-05 - Quản lý vận chuyển hàng đặc biệt 

Ứng dụng mô phỏng quy trình quản lý vận chuyển hàng cần kiểm soát nhiệt độ. Hệ thống có các vai trò người gửi hàng, điều phối viên, kiểm duyệt viên, tài xế và quản trị viên; hỗ trợ quản lý đơn, chứng từ, phân công, theo dõi nhiệt độ, bàn giao và báo cáo.

## Công nghệ

- **Ứng dụng web và REST API chính:** Java 17, Spring Boot 3, Thymeleaf, Spring Data JPA.
- **Cơ sở dữ liệu:** PostgreSQL 15 (khởi chạy bằng Docker Compose).
- **Ứng dụng di động:** Flutter/Dart.
- Thư mục `backend/` chứa một bản API khác bằng Spring Boot và Express/TypeScript; đây là cấu hình riêng, không cần chạy để khởi động ứng dụng web ở thư mục gốc.

## Yêu cầu

- JDK 17
- Maven 3.8 trở lên
- Docker Desktop có Docker Compose
- Flutter SDK (chỉ cần nếu chạy ứng dụng di động)

## Cài đặt và chạy ứng dụng web

### 1. Khởi động PostgreSQL

Từ thư mục gốc dự án, chạy:

```bash
docker compose up -d postgres
```

Compose tạo database `logistics_db` với thông tin kết nối mặc định:

| Thông tin | Giá trị |
|---|---|
| Host | `localhost` |
| Port | `5434` |
| Database | `logistics_db` |
| Username | `postgres` |
| Password | `postgres` |

### 2. Chuẩn bị cấu trúc dữ liệu

Ứng dụng web gốc truy vấn các bảng PostgreSQL theo tên dạng `don_van_chuyen`, `nguoi_dung`, `loai_hang_lanh` và các bảng liên quan. **Lưu ý:** `database/schema.sql` và `database/seeds.sql` hiện dùng tên bảng khác với truy vấn của ứng dụng web, nên chưa thể dùng trực tiếp để khởi tạo database cho ứng dụng này. Cần tạo database theo đúng cấu trúc mà ứng dụng web sử dụng trước khi đăng nhập, tạo đơn hoặc gọi các API có truy vấn dữ liệu.

Ngoài ra, `database/schema.sql` có lệnh xóa bảng (`DROP TABLE`). Chỉ chạy script này trên database thử nghiệm không chứa dữ liệu cần giữ.

### 3. Chạy Spring Boot

Từ thư mục gốc dự án:

```bash
mvn spring-boot:run
```

Hoặc đóng gói rồi chạy file JAR:

```bash
mvn clean package
java -jar target/logistics-coldchain-1.0.0.jar
```

Mở [http://localhost:8085](http://localhost:8085). REST API chính bắt đầu tại `/api/orders`, ví dụ:

- `GET http://localhost:8085/api/orders`
- `GET http://localhost:8085/api/orders/categories`
- `GET http://localhost:8085/api/orders/reports/statistics`

Nhấn `Ctrl+C` trong terminal để dừng ứng dụng. Dừng PostgreSQL khi không sử dụng:

```bash
docker compose down
```

Lệnh trên giữ lại dữ liệu trong volume `postgres_data`. Không dùng `docker compose down -v` nếu muốn giữ dữ liệu.

## Chạy ứng dụng Flutter

Đảm bảo PostgreSQL và ứng dụng Spring Boot đang chạy, sau đó mở terminal:

```bash
cd mobile
flutter pub get
flutter run
```

Ứng dụng Flutter gọi API của Spring Boot ở cổng `8085`. Mặc định, mã nguồn dùng `localhost:8085` cho web/desktop và `10.0.2.2:8085` cho Android Emulator. Khi chạy trên điện thoại thật, hãy nhập địa chỉ IP của máy tính đang chạy Spring Boot trong ứng dụng; hai thiết bị cần cùng mạng.

## Các thư mục chính

```text
.
├── src/                 # Ứng dụng Spring Boot chính (web + REST API)
├── database/            # SQL schema và dữ liệu mẫu
├── backend/             # Bản API khác, có cấu hình chạy riêng
├── mobile/              # Ứng dụng Flutter
├── docker-compose.yml   # PostgreSQL cho ứng dụng ở thư mục gốc
└── pom.xml              # Cấu hình Maven của ứng dụng chính
```
