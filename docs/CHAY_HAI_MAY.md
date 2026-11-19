# Hướng dẫn Chạy Thử nghiệm 2 Máy Thật trong Mạng LAN / Wi-Fi

Tài liệu này cung cấp hướng dẫn chi tiết từng bước để thiết lập, kết nối và nghiệm thu ứng dụng **UDP Multi-room Chat** chạy thực tế giữa hai máy tính vật lý (Windows) trong cùng một mạng cục bộ (LAN hoặc Wi-Fi).

---

## 1. Mô hình Mạng và Cấu hình Kết nối

```
+------------------------------------+           +------------------------------------+
|               MÁY 1                |           |               MÁY 2                |
|       (Server + Client 1)          |           |            (Client 2)              |
|                                    |           |                                    |
|  +------------------------------+  |           |  +------------------------------+  |
|  |     UDPChatServer.exe        |  |           |  |     UDPChatClient.exe        |  |
|  |  IP LAN: 192.168.1.15        |  |           |  |  (Trỏ tới IP 192.168.1.15)  |  |
|  |  Port Unicast: UDP 5000      |  |           |  |  Port Unicast: UDP ngẫu nhiên|  |
|  +------------------------------+  |           |  +------------------------------+  |
|                 ▲                  |           |                 ▲                  |
|                 │                  |           |                 │                  |
|  +------------------------------+  |           |                 │                  |
|  |     UDPChatClient.exe        |  |           |                 │                  |
|  |  (Trỏ tới IP 192.168.1.15)  |  |           |                 │                  |
|  +------------------------------+  |           |                 │                  |
+------------------------------------+           +------------------------------------+
                  ▲                                                ▲
                  │                                                │
                  └──────────────[ ROUTER WI-FI / SWITCH LAN ]─────┘
                                  (Multicast Enabled)
                        Nhóm Multicast: 239.255.42.1 (Sảnh chung)
                        Các phòng: 239.255.42.2 ... : 21000 - 35000
```

### 1.1. Các Giao thức & Cổng sử dụng
1. **UDP Unicast (Điều khiển & Truyền tệp tin):**
   - **Cổng Server:** `UDP 5000` (cố định trên máy chủ).
   - **Nhiệm vụ:** Đăng ký, đăng nhập, đồng bộ phòng, heartbeat (ping/pong), truyền tệp sliding window 32 mảnh, tải báo cáo quản trị ZIP.
2. **UDP Multicast (Trò chuyện theo phòng):**
   - **Dải IP Multicast:** `239.255.42.1` đến `239.255.42.254` (thuộc dải Private/Administrative Multicast theo RFC 2365).
   - **Sảnh chung:** `239.255.42.1:21001`.
   - **Các phòng chat riêng:** Cấp phát động địa chỉ IP Multicast và cổng riêng biệt trong khoảng `21002 - 35000`.
   - **Cơ chế hoạt động:** Khi người dùng vào phòng, Client tự động `joinGroup()` vào địa chỉ multicast của phòng đó trên card mạng LAN. Tin nhắn gửi lên được phân phối trực tiếp tới toàn bộ thành viên trong nhóm multicast mà Server không phải gửi unicast lặp lại từng người.

---

## 2. Chuẩn bị Trước khi Chạy

### 2.1. Kiểm tra Mạng
- Cả hai máy tính (Máy 1 và Máy 2) phải kết nối vào cùng một mạng Wi-Fi hoặc cùng một Switch LAN.
- **Lưu ý quan trọng về Wi-Fi:** Một số Wi-Fi công cộng (quán cafe, trường học, khách sạn) thường bật tính năng **"AP Isolation"** hoặc **"Client Isolation"** (ngăn các máy trong mạng nhìn thấy nhau) và chặn gói tin Multicast. 
  > [!TIP]
  > Để thử nghiệm ổn định nhất, nên dùng:
  > - Mạng Wi-Fi gia đình / router cá nhân; HOẶC
  > - Bật tính năng **Mobile Hotspot** từ điện thoại hoặc một trong hai máy tính rồi cho máy còn lại kết nối vào.

### 2.2. Mở Tường lửa (Windows Defender Firewall)
Khi ứng dụng khởi chạy lần đầu, Windows có thể hiện bảng thông báo **Windows Security Alert**:
- Tích chọn cả hai ô: **Private networks** và **Public networks**.
- Nhấn **Allow access** (Cho phép truy cập).
- Nếu không thấy hiện thông báo nhưng không kết nối được, có thể mở nhanh qua PowerShell (quyền Administrator):
  ```powershell
  New-NetFirewallRule -DisplayName "UDP Chat App" -Direction Inbound -Protocol UDP -Action Allow -Profile Any
  ```

---

## 3. Quy trình Thử nghiệm 5 Bước Thực tế

### Bước 1: Khởi động Máy chủ (Máy 1)
1. Trên **Máy 1**, giải nén file `UDPChatServer-Windows-x64.zip` (hoặc mở thư mục `dist/UDPChatServer`).
2. Bấm đúp vào file `UDPChatServer.exe` (hoặc chạy `Chay-Server.bat`).
3. Trên giao diện cửa sổ Giám sát Máy chủ:
   - Nhấn nút **[Bắt đầu]** (màu xanh).
   - Nhãn trạng thái chuyển thành `● ĐANG CHẠY`.
   - Nhấn nút **[Sao chép IP]** để lấy địa chỉ IP LAN của máy (ví dụ: `192.168.1.15`).

### Bước 2: Khởi động Client trên Máy 2 (và Máy 1)
1. Trên **Máy 2**, giải nén file `UDPChatClient-Windows-x64.zip`.
2. Bấm đúp vào file `UDPChatClient.exe`.
3. Tại màn hình Đăng nhập:
   - Mở rộng mục **"Cài đặt máy chủ"**.
   - Nhập IP LAN của Máy 1 vừa sao chép ở Bước 1 vào ô **"Địa chỉ máy chủ"** (ví dụ: `192.168.1.15`).
   - Nhấn nút **[Kiểm tra kết nối]**. Nếu hiển thị thông báo xanh: *"Kết nối máy chủ thành công (UDP Ping < 5ms)"* nghĩa là đường truyền UDP Unicast đã thông suốt 100%!

### Bước 3: Đăng ký & Đăng nhập
1. Trên Máy 1: Mở Client 1, đăng nhập bằng tài khoản Quản trị:
   - Tên đăng nhập: `admin`
   - Mật khẩu: `admin123` (hoặc mật khẩu được cấu hình).
2. Trên Máy 2: Chọn tab **"Đăng ký"**, tạo tài khoản mới:
   - Tên đăng nhập: `sinhvien_b`
   - Tên hiển thị: `Bạn B - Máy 2`
   - Mật khẩu: `MatKhau@123`
3. Cả hai máy tự động gia nhập phòng **"Sảnh chung"**.

### Bước 4: Kiểm tra Trò chuyện Multicast & Chia sẻ Dữ liệu
1. **Chat trực tiếp Multicast:**
   - Máy 1 gửi tin nhắn: `"Chào bạn từ Máy 1!"`.
   - Ngay lập tức Máy 2 nhận được tin nhắn trong tích tắc mà không có độ trễ.
2. **Gửi Sticker & Emoji:**
   - Bấm nút mặt cười `😊`, chọn sticker Noto Emoji chuẩn PNG độ phân giải cao.
   - Tin nhắn sticker hiển thị sắc nét trên cả 2 màn hình.
3. **Đổi Ảnh đại diện (Avatar):**
   - Vào menu người dùng (góc dưới trái) -> "Hồ sơ cá nhân" -> Đổi avatar.
   - Avatar mới được đồng bộ qua UDP tới máy bên kia.
4. **Truyền tệp tin 5MB kèm kiểm tra CRC32:**
   - Máy 1 bấm nút `+` -> Chọn một tệp tin tài liệu hoặc ảnh dung lượng khoảng 5MB -> Gửi.
   - Thẻ tệp tin hiển thị thanh tiến trình truyền (Progress Bar) theo thời gian thực (cửa sổ trượt 32 mảnh, retry 8 lần).
   - Máy 2 nhận được thẻ tệp tin, bấm "Tải xuống". Sau khi tải xong, hệ thống tự động kiểm tra mã băm CRC32 khớp 100%, bảo đảm không bị mất dữ liệu hay lỗi bit.

### Bước 5: Kiểm tra các Tính năng Nâng cao (M3c & Quản trị M5)
1. **Trích dẫn / Trả lời tin nhắn (Quote & Reply):**
   - Rê chuột vào tin nhắn của đối phương, bấm nút mũi tên cong `reply` (Trả lời).
   - Nhập nội dung phản hồi. Khối trích dẫn viền xanh xuất hiện kèm tên và nội dung tin gốc.
   - Bấm vào khối trích dẫn để nhảy thẳng tới vị trí tin nhắn gốc.
2. **Đặt Biệt danh riêng trong phòng (Room Nickname):**
   - Tại cột Thành viên bên phải, chuột phải vào tên thành viên -> chọn "Đặt biệt danh...".
   - Đặt biệt danh: `"Trưởng nhóm B"`.
   - Danh sách thành viên hiển thị `"Trưởng nhóm B (Bạn B - Máy 2)"`. Tin nhắn gửi đi hiển thị tên tác giả là biệt danh mới.
3. **Mời thành viên vào phòng riêng:**
   - Máy 1 tạo phòng mới: `"Dự án UDP MultiChat"`.
   - Bấm nút `+` trên thanh tiêu đề Thành viên, tìm kiếm `sinhvien_b` và bấm "Gửi lời mời".
   - Máy 2 mở menu Avatar -> chọn "Lời mời tham gia phòng..." -> bấm "Chấp nhận" để tự động vào phòng.
4. **Tắt thông báo phòng (Mute Notifications):**
   - Chuột phải vào phòng ở danh sách bên trái -> chọn "Tắt thông báo phòng".
   - Biểu tượng `🔕` xuất hiện cạnh tên phòng, không làm phiền khi có tin mới.
5. **Tìm kiếm tin nhắn trong phòng (`Ctrl + F`):**
   - Bấm `Ctrl + F`, nhập từ khóa cần tìm. Khung tìm kiếm đếm kết quả và cho phép bấm `▲` / `▼` để cuộn tới tin tương ứng.
6. **Xuất báo cáo PDF / HTML:**
   - Tại máy Admin (Máy 1), mở "Trung tâm quản trị" -> Tab "Báo cáo" (hoặc từ giao diện Máy chủ).
   - Bấm "Xem trước báo cáo" -> Bấm "In / Lưu PDF" để xuất báo cáo hệ thống chuẩn đẹp.

---

## 4. Xử lý Sự cố Thường gặp (Troubleshooting)

| Hiện tượng | Nguyên nhân | Cách khắc phục |
|---|---|---|
| Bấm "Kiểm tra kết nối" báo lỗi timeout | 1. Nhập sai địa chỉ IP LAN của Máy 1.<br>2. Windows Firewall trên Máy 1 chặn UDP cổng 5000.<br>3. Hai máy không cùng subnet (ví dụ một máy nối Wi-Fi, một máy 4G). | - Kiểm tra lệnh `ipconfig` trên Máy 1 để lấy đúng IPv4.<br>- Thêm rule Firewall cho phép UDP cổng 5000.<br>- Đảm bảo cả hai máy cùng kết nối vào một mạng Wi-Fi hoặc dùng Mobile Hotspot. |
| Đăng nhập được nhưng không nhận được tin nhắn chat | Router Wi-Fi đang bật tính năng "Multicast Filtering" hoặc "AP Isolation". | - Bật tính năng Mobile Hotspot trên một trong hai laptop/điện thoại, kết nối cả 2 máy vào hotspot đó.<br>- Trên Router Wi-Fi, bật hỗ trợ IGMP Snooping / Multicast. |
| Tải tệp tin bị gián đoạn hoặc thất bại | Quyền ghi thư mục tải về hoặc tệp vượt quá kích thước cho phép (tối đa 25MB). | - Đảm bảo ổ đĩa còn dung lượng trống.<br>- Chọn thư mục Downloads mặc định có quyền ghi. |
