# Tài liệu Đóng gói Ứng dụng Portable (Windows x64)

Tài liệu này thuyết minh giải pháp đóng gói ứng dụng **UDP Multi-room Chat** thành bản chạy độc lập (Portable), cho phép chạy trực tiếp trên bất kỳ máy tính Windows nào mà **không cần cài đặt Java, Apache NetBeans, Maven hay bất kỳ phần mềm môi trường nào khác**.

---

## 1. Mục tiêu và Giải pháp Kỹ thuật

### 1.1. Mục tiêu
- **Zero Configuration:** Người nhận đồ án / giáo viên chỉ cần giải nén file `.zip` và bấm đúp vào file `.exe` là ứng dụng chạy ngay.
- **Không phụ thuộc môi trường:** Không yêu cầu cài đặt JDK/JRE trên hệ thống; không lo xung đột phiên bản Java cũ/mới (Java 8, 11, 17...).
- **Toàn vẹn tiếng Việt UTF-8:** Cấu hình mã hóa UTF-8 sẵn ở cấp độ JVM arguments để không bị lỗi font trên các phiên bản Windows có locale khác nhau.
- **Dung lượng tối ưu:** Sử dụng tính năng `jpackage` chuẩn của JDK 21 để đóng gói runtime tối giản.

### 1.2. Công nghệ sử dụng
- **JDK 21 `jpackage`:** Công cụ chính thức của OpenJDK/Oracle để tạo native application image cho hệ điều hành đích (Windows x64).
- **Chế độ `--type app-image`:** Tạo ra thư mục ứng dụng hoàn chỉnh chứa file thực thi `.exe`, thư mục `runtime/` (Java Runtime nhúng sẵn) và thư mục `app/` (chứa các file jar ứng dụng và thư viện phụ thuộc FlatLaf).
- **PowerShell Automation (`scripts/build-portable.ps1`):** Script tự động hóa toàn bộ quy trình biên dịch, sao chép dependency, chạy jpackage, tạo file khởi chạy phụ trợ `.bat`, file hướng dẫn `HUONG_DAN.txt` và nén thành các tệp `.zip` phân phối.

---

## 2. Cấu trúc Gói Phân phối

Sau khi chạy quy trình đóng gói, hệ thống tạo ra 2 gói độc lập trong thư mục `dist/`:

### 2.1. Gói Máy chủ: `UDPChatServer-Windows-x64.zip`
```
dist/UDPChatServer/
├── UDPChatServer.exe           # File chạy chính của Máy chủ (bấm đúp để mở)
├── Chay-Server.bat             # File khởi chạy phụ trợ (chcp 65001 UTF-8)
├── HUONG_DAN.txt               # Hướng dẫn sử dụng nhanh dành cho người dùng
├── app/
│   ├── UDPmultiChat-1.0-SNAPSHOT.jar  # Jar mã nguồn ứng dụng
│   └── flatlaf-3.7.2.jar               # Giao diện FlatLaf hiện đại
└── runtime/                    # Toàn bộ máy ảo Java 21 nhúng sẵn (~140MB giải nén)
    ├── bin/ (java.dll, server/jvm.dll...)
    ├── lib/ (modules, tzdb.dat...)
    └── conf/
```

### 2.2. Gói Máy khách: `UDPChatClient-Windows-x64.zip`
```
dist/UDPChatClient/
├── UDPChatClient.exe           # File chạy chính của Client (bấm đúp để mở)
├── Chay-Client.bat             # File khởi chạy phụ trợ
├── Chay-Client-Phien-2.bat     # Khởi chạy Client thứ 2 trên cùng máy (Profile riêng)
├── HUONG_DAN.txt               # Hướng dẫn kết nối và sử dụng
├── app/
│   ├── UDPmultiChat-1.0-SNAPSHOT.jar
│   └── flatlaf-3.7.2.jar
└── runtime/                    # Java Runtime 21 nhúng kèm
```

---

## 3. Lệnh Tự động Đóng gói (Build Command)

Để tạo mới hoặc cập nhật các gói portable từ mã nguồn, chạy lệnh PowerShell sau tại thư mục gốc của dự án:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-portable.ps1
```

### Các bước script thực hiện:
1. **Kiểm tra công cụ:** Xác định JDK 21, `jpackage.exe` và Maven trong hệ thống.
2. **Dọn dẹp môi trường:** Xóa sạch thư mục tạm `target/staging-portable` và thư mục `dist/` cũ.
3. **Biên dịch mã nguồn:** Thực thi `mvn package -DskipTests` và `mvn dependency:copy-dependencies`.
4. **Đóng gói Máy chủ:** Sử dụng `jpackage` với cấu hình main class `com.mycompany.udpmultichat.server.ServerMain` và tham số `-Dfile.encoding=UTF-8`.
5. **Đóng gói Máy khách:** Sử dụng `jpackage` với cấu hình main class `com.mycompany.udpmultichat.ClientMain`.
6. **Tạo tài liệu và file hỗ trợ:** Sinh tự động `HUONG_DAN.txt`, `Chay-Client.bat`, `Chay-Client-Phien-2.bat`, `Chay-Server.bat`.
7. **Nén ZIP:** Nén thành `dist/UDPChatServer-Windows-x64.zip` (~57MB) và `dist/UDPChatClient-Windows-x64.zip` (~57MB).

---

## 4. Hướng dẫn Chạy trên Máy Đích (Target Machine)

### Kịch bản 1: Máy tính KHÔNG CÓ Java / NetBeans
1. Copy file `UDPChatServer-Windows-x64.zip` hoặc `UDPChatClient-Windows-x64.zip` sang máy đích (qua USB, Google Drive, Zalo...).
2. Nhấp chuột phải vào file ZIP -> Chọn **Extract All...** (Giải nén toàn bộ).
3. Mở thư mục đã giải nén:
   - Nếu là Máy chủ: Bấm đúp vào `UDPChatServer.exe`.
   - Nếu là Máy khách: Bấm đúp vào `UDPChatClient.exe`.
4. Ứng dụng sẽ khởi động ngay lập tức với đầy đủ giao diện FlatLaf, font Segoe UI sắc nét, không yêu cầu cài thêm bất kỳ thành phần nào.

### Kịch bản 2: Chạy nhiều Client trên cùng một máy để thử nghiệm
Khi cần mở 2 hoặc 3 cửa sổ Client trên cùng một máy tính để kiểm tra chat qua lại:
- Cửa sổ 1: Bấm đúp `UDPChatClient.exe` (sử dụng cấu hình mặc định `client.properties`).
- Cửa sổ 2: Bấm đúp `Chay-Client-Phien-2.bat` (sử dụng cấu hình riêng `client-phien-2.properties`, không bị ghi đè thông tin tài khoản hay IP của Cửa sổ 1).

---

## 5. Đảm bảo Tính Tin cậy & Không Gặp Lỗi
- **Đường dẫn có khoảng trắng / tiếng Việt:** Ứng dụng đã được kiểm thử và hỗ trợ đường dẫn chứa dấu cách hoặc ký tự có dấu (ví dụ: `D:\Mon Hoc\Lap Trinh Mang\`).
- **Phân tách Dữ liệu:** Toàn bộ dữ liệu của Server (tài khoản, lịch sử phòng, tệp tin tải lên, nhật ký kiểm toán) được lưu trữ độc lập trong thư mục `data/` ngay tại vị trí file `.exe`, không ghi vào thư mục hệ thống Windows hay Registry.
- **Tiêu chuẩn Kỹ thuật:** Tuân thủ tuyệt đối quy định không dùng TCP / HTTP / WebSocket; sử dụng 100% UDP Unicast cho điều khiển và UDP Multicast cho chat nhóm.
