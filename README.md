# UDPmultiChat — Ứng dụng Web Chat Nhiều Phòng (Lõi UDP)

Ứng dụng chat nhiều phòng thời gian thực xây dựng trên **Java 21**, **Maven** và **Spring Boot 3.5**, sử dụng **UDP (`DatagramSocket` / `DatagramPacket`)** thật ở tầng backend để truyền, xác nhận và ghi nhận toàn bộ nội dung tin nhắn.

---

## 1. Kiến trúc & Đường đi gói tin UDP

```text
Trình duyệt (PC / Tablet / Điện thoại)
   │  HTTPS / WebSocket (WSS)
   ▼
Web Gateway (WebSecurity, WebApi, ChatSocket)
   │  1. Xác thực phiên & kiểm tra đầu vào
   │  2. Đóng gói UTF-8 (CHAT2 + nonce + roomId + clientMsgId + nội dung)
   ▼
UdpChatTransport — DatagramSocket gửi (127.0.0.1:ephemeral)
   │  DatagramPacket thật (có timeout & retry giới hạn)
   ▼
UdpChatTransport — DatagramSocket nhận (127.0.0.1:ephemeral)
   │  3. Kiểm tra endpoint nguồn & giải mã UTF-8
   ▼
WebChatService.receive(...)
   │  4. Kiểm tra quyền phòng, mute/ban, chống spam & chống trùng (idempotency)
   │  5. Ghi lịch sử bền vững (KhoLuuTru / QuanLyTinNhan) -> gán msgId
   ▼
ChatSocket.broadcast(...)
   │  6. Chỉ phát WebSocket tới các thành viên có quyền đọc phòng sau khi UDP commit
   ▼
Trình duyệt người gửi & người nhận trong phòng
```

- **Đường chat UDP bắt buộc:** Mọi tin nhắn văn bản, sticker, trả lời/trích dẫn, thẻ tệp đính kèm và thông báo broadcast từ quản trị viên đều phải đi qua `UdpChatTransport` (`DatagramSocket.send` -> `DatagramSocket.receive` -> `WebChatService.receive`) rồi mới được lưu và phát WebSocket.
- **Kênh HTTP/HTTPS:** Dùng cho đăng ký/đăng nhập, quản lý phòng/thành viên/lời mời, tải tệp nhị phân, avatar, lịch sử phân trang và xuất báo cáo PDF/CSV/HTML.

---

## 2. Chức năng chính

### Cơ bản
- Đăng ký, đăng nhập, đăng xuất; mỗi tài khoản một phiên hoạt động, kiểm tra CSRF, Origin WebSocket và giới hạn tần suất.
- Sảnh chung (`sanh`) và chat thời gian thực qua UDP + WebSocket.
- Giao diện responsive cho máy tính, tablet và điện thoại; hỗ trợ chế độ Sáng / Tối.
- Trạng thái tin nhắn (`Đang gửi…`, `✓ Đã gửi`, lỗi & `Gửi lại` giữ nguyên `clientMsgId` chống trùng), đồng bộ lịch sử khi mất kết nối.

### Trung bình
- **Quản lý nhiều phòng:** Tạo/sửa/xóa/đóng/mở phòng, phòng công khai hoặc riêng tư, mã phòng 6 ký tự, mật khẩu phòng, giới hạn số thành viên, tìm kiếm phòng và đếm tin chưa đọc.
- **Thành viên & Lời mời:** Mời thành viên trong ứng dụng (hỗ trợ lời mời khi offline), nhận/từ chối/hủy lời mời, đặt biệt danh riêng theo từng phòng.
- **Hồ sơ cá nhân:** Đổi tên hiển thị, giới thiệu, tải/xóa ảnh đại diện (PNG), đổi mật khẩu và bắt buộc đổi mật khẩu tạm.
- **Tiện ích chat:** 36 sticker Noto Emoji, trả lời/trích dẫn tin nhắn, thu hồi tin nhắn đúng quyền, tìm kiếm tin nhắn có phân trang & nhảy tới đúng vị trí, chỉ báo đang nhập (`typing`), xuất lịch sử TXT / HTML.

### Nâng cao
- **Tệp & Ảnh:** Upload/download qua HTTP có xác thực (tối đa 10 MB), thanh tiến trình, hủy/thử lại, kiểm tra CRC32, tạo thumbnail xem trước ảnh, gửi thẻ tệp qua luồng UDP.
- **Kiểm duyệt thành viên:** Đuổi (`kick`), cấm chat có thời hạn (`mute`), cấm vào phòng (`ban` / `unban`), chuyển quyền chủ phòng, thông báo riêng cho thành viên bị tác động.
- **Trang Quản trị (`/admin`):** 5 mục (*Tổng quan, Người dùng, Phòng, Nhật ký, Báo cáo*), CRUD tài khoản/phòng, khóa/reset mật khẩu tạm, thu hồi phiên realtime, gửi thông báo broadcast qua UDP, thống kê đồng bộ snapshot, tải báo cáo **PDF khổ A4 tiếng Việt** (PDFBox + Noto Sans), ZIP (CSV + HTML) và nhật ký audit.
- **Sao lưu & Khôi phục dữ liệu (`WebDataTool`):** Công cụ CLI offline hỗ trợ `backup`, `verify`, `restore`, `import-copy` kèm kiểm tra SHA-256 manifest và cấu trúc kho dữ liệu.

---

## 3. Cách chạy & Kiểm thử

### Yêu cầu
- **Java 21** và **Maven 3.9+** (hoặc Maven đi kèm Apache NetBeans).

### Chạy kiểm thử tự động (JUnit 5)
```powershell
mvn clean test
```

### Đóng gói và chạy ứng dụng Web
```powershell
mvn -q package
java -jar target/UDPmultiChat-1.0-SNAPSHOT.jar
```
Mở trình duyệt tại: **http://127.0.0.1:8080**

### Biến môi trường tùy chọn
- `PORT`: Cổng HTTP/WebSocket (mặc định `8080`).
- `BIND_ADDRESS`: Địa chỉ bind (mặc định `127.0.0.1`, đặt `0.0.0.0` nếu muốn mở trong mạng LAN).
- `CHAT_DATA_DIR`: Thư mục lưu dữ liệu (mặc định `./web-data`).
- `CHAT_INITIAL_ADMIN_PASSWORD`: Mật khẩu admin khởi tạo lần đầu cho kho mới (12–128 ký tự).
- `COOKIE_SECURE`: Đặt `true` khi chạy sau reverse proxy HTTPS (`--spring.profiles.active=proxy`, xem mẫu trong thư mục `deploy/`).

### Công cụ sao lưu dữ liệu offline (`WebDataTool`)
```powershell
java -Dloader.main=com.mycompany.udpmultichat.server.WebDataTool `
  -cp target/UDPmultiChat-1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher `
  backup ./web-data ./backups/checkpoint.zip
```
(Hỗ trợ các lệnh: `backup`, `verify`, `restore`, `import-copy` khi server đã dừng.)

---

## 4. Giấy phép tài nguyên
Xem [docs/NGUON_TAI_NGUYEN.md](docs/NGUON_TAI_NGUYEN.md) để biết thông tin bản quyền của bộ sticker Noto Emoji và font Noto Sans tiếng Việt.
