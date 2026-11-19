# Hướng dẫn Chạy Thử nghiệm 2 Máy Thật trong Mạng LAN / Wi-Fi & Kịch bản Demo cho Giảng viên

Tài liệu này cung cấp hướng dẫn chi tiết từng bước để thiết lập, kết nối và nghiệm thu ứng dụng **UDP Multi-room Chat** chạy thực tế giữa hai máy tính vật lý (Windows) trong cùng một mạng LAN hoặc Wi-Fi. Kèm theo là **kịch bản demo 5–7 phút chuẩn chỉ để trình chiếu cho Thầy/Cô** và **bộ câu hỏi vấn đáp điểm 10**.

---

## 1. Mô hình Mạng và Cơ chế Hoạt động

```
+------------------------------------------------+           +------------------------------------------------+
|                     MÁY 1                      |           |                     MÁY 2                      |
|         (Máy Chủ Server + Client 1)            |           |             (Máy Khách Client 2)               |
|                                                |           |                                                |
|  [Cách 1]: Chạy trực tiếp từ NetBeans          |           |  [KHÔNG CẦN CÀI GÌ CẢ]:                        |
|    - ServerMain.java  (UDP Port 5000)          |           |  Nhận file UDPChatClient-Windows-x64.zip       |
|    - ClientMain.java  (Giao diện Admin)        |           |  từ Máy 1 -> Giải nén -> Chạy file .exe        |
|  [Cách 2]: Chạy UDPChatServer.exe (Portable)   |           |                                                |
|                                                |           |                                                |
|  +------------------------------------------+  |           |  +------------------------------------------+  |
|  |             UDPChatServer                |  |           |  |             UDPChatClient                |  |
|  |  IP LAN: 192.168.1.15                    |  |           |  |  Trỏ về IP Server: 192.168.1.15         |  |
|  |  UDP Unicast: Cổng 5000 cố định          |  |           |  |  UDP Unicast: Cổng ngẫu nhiên           |  |
|  +------------------------------------------+  |           |  +------------------------------------------+  |
|                       ▲                        |           |                       ▲                        |
|                       │                        |           |                       │                        |
|  +------------------------------------------+  |           |                       │                        |
|  |             UDPChatClient (1)            |  |           |                       │                        |
|  |  Trỏ về IP Server: 192.168.1.15          |  |           |                       │                        |
|  +------------------------------------------+  |           |                       │                        |
+------------------------------------------------+           +------------------------------------------------+
                        ▲                                                            ▲
                        │                                                            │
                        └──────────────[ ROUTER WI-FI / SWITCH LAN ]─────────────────┘
                                         (Cho phép Multicast)
                              Nhóm Multicast Sảnh chung: 239.255.42.1 : 21001
                              Các phòng chat riêng:      239.255.42.x : 21002 - 35000
```

### 1.1. Các Giao thức & Cổng mạng sử dụng
1. **UDP Unicast (Cổng cố định `5000` trên Server):**
   - Đăng ký, đăng nhập, xác thực phiên làm việc.
   - Đồng bộ danh sách phòng, danh sách online/offline, cấu hình người dùng.
   - Gửi yêu cầu kiểm tra kết nối (Ping/Pong UDP).
   - Truyền nhận tệp tin tin cậy (Sliding Window 32 mảnh, Retry 8 lần, mã băm CRC32).
   - Truyền tải báo cáo quản trị định dạng ZIP.
2. **UDP Multicast (Dải IP `239.255.42.1` – `239.255.42.254`):**
   - **Sảnh chung:** `239.255.42.1:21001`.
   - **Phòng riêng:** Tự động cấp phát IP và Port multicast độc lập trong dải `21002 – 35000`.
   - **Cơ chế:** Khi người dùng vào phòng, ứng dụng tự động gọi `joinGroup()` trên Card mạng LAN. Tin nhắn được phân phối trực tiếp tới mọi thành viên trong nhóm multicast mà Server không phải tốn tài nguyên gửi unicast lặp lại từng người.

---

## 2. Cách Chạy Giữa 2 Máy (Cực kỳ Đơn giản)

### 📌 Máy 1 (Máy tính của bạn - Làm Server + Client 1):
Bạn có 2 lựa chọn để chạy:
- **Cách A (Khuyên dùng khi demo chấm bài với Thầy):**
  1. Mở phần mềm **Apache NetBeans**.
  2. Mở project `UDPmultiChat`.
  3. Tìm file `com.mycompany.udpmultichat.server.ServerMain.java` -> Bấm chuột phải chọn **Run File** (hoặc gõ phím tắt `Shift + F6`). Cửa sổ Giám sát Server sẽ hiện lên.
  4. Tiếp tục tìm file `com.mycompany.udpmultichat.ClientMain.java` -> Chuột phải chọn **Run File** để mở màn hình Client thứ nhất (dùng đăng nhập tài khoản Admin).
- **Cách B (Chạy độc lập không cần NetBeans):**
  - Mở thư mục `UDPmultiChat/dist/UDPChatServer` -> Bấm đúp `UDPChatServer.exe`.
  - Mở thư mục `UDPmultiChat/dist/UDPChatClient` -> Bấm đúp `UDPChatClient.exe`.

### 📌 Máy 2 (Máy tính thứ 2 của bạn bè / máy thực hành):
> **Câu hỏi:** *Máy 1 gửi file ZIP cho Máy 2 hay làm cách nào? Máy 2 có cần cài NetBeans hay Java không?*
> 
> **Trả lời:**
> - Máy 1 chỉ cần **gửi duy nhất 1 file ZIP** cho Máy 2:
>   📂 Đường dẫn file: `UDPmultiChat\dist\UDPChatClient-Windows-x64.zip` (khoảng ~57 MB).
>   *(Có thể gửi qua Zalo, Telegram, Google Drive hoặc chép qua USB).*
> - **Máy 2 HOÀN TOÀN KHÔNG CẦN CÀI GÌ CẢ:** Không cần cài Apache NetBeans, không cần cài JDK/Java, không cần cài Maven!
> - **Thao tác trên Máy 2:**
>   1. Nhận file `UDPChatClient-Windows-x64.zip`.
>   2. Chuột phải vào file chọn **Extract All...** (Giải nén).
>   3. Mở thư mục vừa giải nén, bấm đúp vào file thực thi **`UDPChatClient.exe`** là ứng dụng khởi động ngay lập tức! (Bên trong đã được tích hợp sẵn Java 21 Runtime chuyên dụng).

---

## 3. Các Bước Chuẩn bị Kết nối Mạng LAN

1. **Kết nối cùng mạng:** Đảm bảo cả Máy 1 và Máy 2 đều đang bắt chung một mạng Wi-Fi hoặc cắm chung dây LAN.
   > [!TIP]
   > Một số mạng Wi-Fi công cộng (trường học, quán cafe) thường bật tính năng **AP Isolation / Client Isolation** (chặn các máy nhìn thấy nhau) và chặn gói tin Multicast.
   > Nếu gặp tình huống này, giải pháp tốt nhất là: **Bật Mobile Hotspot (Phát Wi-Fi)** từ điện thoại hoặc từ Máy 1, sau đó cho Máy 2 kết nối vào. Mạng Hotspot luôn cho phép UDP/Multicast hoạt động mượt mà 100%.

2. **Lấy địa chỉ IP của Máy 1:**
   - Trên cửa sổ **Giám sát Máy chủ (Server)** ở Máy 1, chỉ cần bấm nút **[Sao chép IP]** (Ứng dụng đã tự động lọc lấy đúng địa chỉ IPv4 LAN, ví dụ: `192.168.1.15`).

3. **Mở Tường lửa (Windows Firewall) nếu có hỏi:**
   - Khi Server hoặc Client bật lên lần đầu, nếu Windows hiện bảng hỏi `Windows Defender Firewall`, bạn tích chọn cả **Private** và **Public** rồi chọn **Allow access**.

---

## 4. Kịch bản Demo 5–7 Phút "Show cho Thầy Xem"

Dưới đây là kịch bản chuẩn từng bước, thao tác gọn gàng, logic và kèm theo câu giải thích kỹ thuật để thầy cô đánh giá tối đa điểm đồ án:

### Bước 1: Khởi động Server & Trực quan hóa Mạng (1 phút)
1. **Thao tác:**
   - Trên Máy 1 (NetBeans), chạy `ServerMain.java`.
   - Bấm nút xanh **[Bắt đầu]** -> Đèn trạng thái chuyển sang xanh lá `● ĐANG CHẠY`.
   - Bấm nút **[Sao chép IP]**.
2. **Nói với Thầy:**
   > *"Thưa thầy, Server quản lý trung tâm lắng nghe trên giao thức UDP Unicast cổng 5000. Ứng dụng tự động phát hiện card mạng LAN thực tế và lấy địa chỉ IPv4 của máy chủ để các client kết nối tới."*

### Bước 2: Máy 2 Kết nối & Kiểm tra Ping UDP Tức thì (30 giây)
1. **Thao tác:**
   - Trên Máy 2, mở `UDPChatClient.exe`.
   - Mở mục **"Cài đặt máy chủ"**, dán địa chỉ IP của Máy 1 vào ô IP Server.
   - Bấm nút **[Kiểm tra kết nối]**.
   - Màn hình hiện thông báo xanh: `Kết nối máy chủ thành công (UDP Ping < 5ms)`.
2. **Nói với Thầy:**
   > *"Client gửi một gói tin UDP Ping Unicast lên Server cổng 5000, Server phản hồi UDP Pong ngay lập tức để đo RTT và kiểm tra thông tuyến mạng trước khi đăng nhập."*

### Bước 3: Đăng nhập & Xác thực Hiện đại (1 phút)
1. **Thao tác:**
   - Máy 1 mở `ClientMain.java`, đăng nhập tài khoản: `admin` / `admin123`.
   - Máy 2 mở tab **"Đăng ký"**, tạo tài khoản mới: `sinhvien_b` / `MatKhau@123` / Tên hiển thị: `Bạn B - Máy 2`.
   - **Điểm nhấn UI:** Thử bấm vào biểu tượng **Mắt nhắm/mở** (`eye` / `eye-off`) trên ô mật khẩu để kiểm tra tính năng ẩn hiện mật khẩu được vẽ bằng Java2D vector sắc nét, hoàn toàn không bị lỗi font ô vuông.
   - Thử đăng nhập cùng tài khoản trên cả 2 máy: Hệ thống từ chối và báo rõ *"Tài khoản đang đăng nhập ở một nơi khác"*.
2. **Nói với Thầy:**
   > *"Hệ thống kiểm soát phiên (Session) qua UDP Token, ngăn chặn đăng nhập trùng lặp và bảo mật mật khẩu bằng băm muối SHA-256."*

### Bước 4: Trò chuyện Multicast Phòng "Sảnh chung" (1.5 phút)
1. **Thao tác:**
   - Cả 2 máy tự động vào phòng **Sảnh chung**.
   - Máy 1 gửi: *"Chào bạn B từ Máy chủ!"* -> Máy 2 nhận tức thì.
   - Bấm nút mặt cười, gửi bộ **Sticker Noto Emoji PNG** độ phân giải cao -> Tin hiển thị sắc nét trên cả 2 máy.
   - Gửi liên tiếp 2 tin nhắn: Quan sát giao diện tự động **Gộp tin nhắn trong 5 phút** (chỉ hiện avatar và tên ở tin đầu tiên, các tin sau xếp liền kề gọn gàng giống Messenger/Telegram).
   - Có dải phân cách ngày gửi tin: `Hôm nay, 20/11/2026`.
2. **Nói với Thầy:**
   > *"Tin nhắn trong phòng chat không gửi vòng qua Server mà được gửi trực tiếp giữa các Client thông qua UDP Multicast Socket (nhóm 239.255.42.1). Nhờ đó giảm tải tối đa cho Server khi số lượng thành viên tăng cao."*

### Bước 5: Thử nghiệm Trích dẫn (Reply) & Tìm kiếm (Ctrl + F) (1 phút)
1. **Thao tác:**
   - Trên Máy 2, rê chuột vào tin nhắn của Máy 1 -> bấm icon mũi tên cong **Trả lời** (`reply`).
   - Khung trích dẫn màu xanh nổi bật hiện lên trên thanh soạn thảo. Nhập phản hồi: *"Đã nhận tin rõ nhé Admin!"* -> Gửi.
   - Trên tin nhắn vừa gửi, có khối trích dẫn nội dung tin gốc. Bấm chuột vào khối trích dẫn -> danh sách tin tự động cuộn mượt (scroll) đến đúng vị trí tin gốc.
   - Bấm phím tắt **`Ctrl + F`** -> Thanh tìm kiếm hiện lên -> Gõ từ khóa tìm kiếm -> Bấm phím mũi tên `▲` / `▼` để nhảy đến các kết quả tìm thấy.

### Bước 6: Đổi Màu Nền Phòng Chat Pastel Cá nhân hóa (30 giây)
1. **Thao tác:**
   - Trên thanh tiêu đề phòng chat, bấm vào icon **Bảng màu** (`palette`).
   - Một menu hiện ra danh sách 8 màu pastel nhẹ nhàng: *Trắng mây mặc định, Xanh mây dịu mắt, Bạc hà thanh mát, Cam đào nhẹ, Tím oải hương, Hồng phấn, Vàng nhạt, Xám tuyết*.
   - Chọn màu **Xanh mây** hoặc **Bạc hà** -> Toàn bộ khung chat đổi sang màu pastel thanh lịch, dịu mắt.
   - Đóng client và mở lại -> Cấu hình màu nền vẫn được lưu giữ riêng cho phòng đó trong file cấu hình client.

### Bước 7: Quản lý Phòng & Biệt danh (Nickname) (1 phút)
1. **Thao tác:**
   - **Đổi Biệt danh riêng trong phòng:** Tại cột Thành viên bên phải, chuột phải vào tên thành viên -> chọn *"Đổi biệt danh..."* -> Nhập: *"Nhóm trưởng"*. Cột thành viên hiển thị cập nhật ngay lập tức: `Nhóm trưởng (Bạn B - Máy 2)`.
   - **Tạo phòng riêng & Mời tham gia:** Máy 1 bấm nút **[+ Tạo phòng]** -> Đặt tên: *"Nhóm Đồ Án Mạng"*. Bấm nút `+` trên cột thành viên để mở hộp thoại **Mời thành viên** -> Chọn `sinhvien_b` và gửi lời mời. Máy 2 nhận thông báo mời, bấm Chấp nhận để tự động tham gia vào phòng mới với IP Multicast độc lập.

### Bước 8: Truyền Tệp Tin Tin cậy qua UDP (Sliding Window & CRC32) (1.5 phút)
1. **Thao tác:**
   - Máy 1 bấm nút `+` ở thanh nhập liệu -> Chọn một tệp tin (ảnh hoặc tài liệu Word/PDF dung lượng 2MB – 5MB).
   - Bấm Gửi -> Quan sát thanh tiến trình **Progress Bar** chạy từ 0% đến 100% theo thời gian thực.
   - Máy 2 nhận được thẻ tệp tin, bấm nút **"Tải về"**.
   - Sau khi tải xong, hệ thống thông báo tải hoàn tất và đối chiếu mã kiểm tra toàn vẹn **CRC32** trùng khớp 100%.
2. **Nói với Thầy:**
   > *"Giao thức UDP vốn không đảm bảo thứ tự và có thể mất gói tin. Để giải quyết, nhóm đã tự thiết kế tầng truyền tin tin cậy: Chia nhỏ file thành các mảnh 1400 byte, đánh số thứ tự (Sequence Number), dùng cơ chế cửa sổ trượt (Sliding Window 32 mảnh), gửi xác nhận ACK và thử lại (Retry) tối đa 8 lần, kết hợp mã băm CRC32 để đảm bảo tệp đến nơi nguyên vẹn 100%."*

### Bước 9: Báo cáo Quản trị & Xem Trước In PDF Hiện đại (1 phút)
1. **Thao tác:**
   - Trên Máy 1 (tài khoản Admin), vào menu người dùng góc dưới trái -> Chọn **"Trung tâm quản trị"** (hoặc mở từ cửa sổ Server).
   - Chuyển sang tab **"Báo cáo"**:
     - Cho phép tùy chọn xuất từng nhóm riêng biệt: [x] Danh sách Người dùng, [x] Phòng chat, [x] Nhật ký hệ thống.
     - Bấm nút **[Xem trước báo cáo]** -> Cửa sổ **Print Preview** hiện lên với giao diện trực quan, chuyên nghiệp, hiển thị bảng biểu và danh sách Top người chat nhiều nhất.
     - Bấm **[In / Lưu PDF]** để xuất báo cáo PDF chuẩn mực ra Desktop.
   - Thử nghiệm tính năng Kiểm duyệt: Chuột phải vào một user -> chọn Kick/Mute -> Thông báo xác nhận bằng câu từ tự nhiên, thân thiện (*"Bạn có chắc muốn mời người dùng ra khỏi phòng?"*).

---

## 5. Tổng hợp Tất cả Chức năng Đã Hoàn thiện trong Dự án

| Phân hệ | Danh sách Chức năng Chi tiết | Trạng thái |
|---|---|:---:|
| **Giao thức UDP Core** | • Server UDP Unicast cổng 5000 quản lý phiên, xác thực, danh bạ.<br>• Client UDP Multicast trò chuyện nhóm theo phòng (dải `239.255.42.x`).<br>• Tự động chọn đúng Card mạng LAN/Wi-Fi (bỏ qua card ảo VMware/VirtualBox).<br>• Cơ chế bù gói tin bị mất qua bản tin kiểm tra Resync tự động. | **100% Hoàn thành** |
| **Giao diện & Trải nghiệm (UI/UX)** | • Giao diện FlatLaf hiện đại, bo tròn góc, bóng đổ nhẹ, màu sắc chuẩn thanh lịch.<br>• **Khắc phục triệt để lỗi ô vuông** (`▯` Tofu): 100% icon vẽ vector Java2D (`search`, `close`, `reply`, `copy`, `eye`, `palette`, `bell-off`, `more`).<br>• Nút Ẩn/Hiện mật khẩu hình Mắt nhắm mở mượt mà.<br>• Phân cách ngày (`Hôm nay`, `Hôm qua`) và gộp tin nhắn thông minh trong 5 phút.<br>• Bộ Sticker Noto Emoji PNG sắc nét kèm thanh cảm xúc nhanh.<br>• **Đổi màu nền phòng chat Pastel** (8 gam màu nhẹ nhàng, lưu cấu hình riêng). | **100% Hoàn thành** |
| **Tương tác Tin nhắn Nâng cao** | • Trích dẫn / Trả lời tin nhắn (Quote & Reply), nhấp chuột cuộn đến tin gốc.<br>• Thu hồi tin nhắn của chính mình (Hiển thị nhãn *"Tin nhắn đã được thu hồi"*).<br>• Sao chép nội dung tin nhắn nhanh.<br>• Tìm kiếm tin nhắn trong phòng bằng phím tắt `Ctrl + F`.<br>• Tắt thông báo phòng riêng biệt (`bell-off`). | **100% Hoàn thành** |
| **Phòng chat & Thành viên** | • Tạo phòng riêng có mật khẩu bảo vệ.<br>• Nhập mã phòng để tham gia phòng.<br>• Đổi Biệt danh riêng trong từng phòng (Room Nickname).<br>• Hộp thoại mời bạn bè vào phòng (Invite Dialog) kèm kiểm tra trạng thái.<br>• Rời phòng, chuyển quyền chủ phòng. | **100% Hoàn thành** |
| **Truyền Tệp tin Tin cậy (File Transfer)** | • Hỗ trợ gửi mọi loại tệp tin (ảnh, tài liệu, nén) lên tới 25MB qua UDP.<br>• Giao thức cửa sổ trượt (Sliding Window 32 fragments), gói ACK phản hồi.<br>• Thanh tiến trình (Progress Bar) cập nhật % thời gian thực.<br>• Kiểm tra tính toàn vẹn bằng mã băm kiểm tra CRC32. | **100% Hoàn thành** |
| **Quản trị & Báo cáo (Admin)** | • Quản trị người dùng: Khóa/Mở tài khoản, Đổi quyền Quản trị / Thành viên.<br>• Kiểm duyệt phòng: Kick, Mute, Ban kèm thông báo thân thiện với người dùng.<br>• Xem trước bản in báo cáo trực quan (Print Preview).<br>• Tùy chọn xuất báo cáo linh hoạt theo từng nhóm (Người dùng, Phòng, Nhật ký).<br>• Xuất báo cáo ra định dạng PDF hoặc trang HTML tổng hợp kèm biểu đồ. | **100% Hoàn thành** |
| **Đóng gói Triển khai** | • Bản cài đặt tự hành Portable đóng gói bằng `jpackage` (kèm JRE Java 21).<br>• Máy phụ chỉ cần giải nén file ZIP là chạy ngay, **không cần cài thêm bất kỳ phần mềm nào**. | **100% Hoàn thành** |

---

## 6. Bộ Câu hỏi & Trả lời Vấn đáp Điểm 10 với Giảng viên

Khi thầy cô kiểm tra đồ án, dưới đây là các câu hỏi thường gặp nhất và cách trả lời chuẩn kỹ thuật:

### ❓ Câu 1: Tại sao đồ án Lập trình mạng UDP lại dùng Multicast cho chat nhóm mà không dùng TCP hay UDP Unicast?
> **Trả lời:**
> *"Dạ thưa thầy, nếu dùng TCP hoặc UDP Unicast, khi một người gửi tin trong phòng có $N$ thành viên, Client phải gửi tin lên Server, sau đó Server phải tốn $N-1$ lần gửi lại gói tin đến từng người, gây nghẽn băng thông của Server khi số lượng thành viên đông.
> Khi dùng **UDP Multicast**, Client chỉ cần phát gói tin một lần duy nhất vào địa chỉ nhóm (ví dụ `239.255.42.1`). Router và switch mạng sẽ tự động nhân bản gói tin tại tầng mạng và chuyển đến đúng các máy đã đăng ký (`joinGroup`), giúp Server hoàn toàn không bị quá tải."*

### ❓ Câu 2: UDP là giao thức không tin cậy (Unreliable), có thể mất gói hoặc sai thứ tự. Ứng dụng giải quyết bài toán này như thế nào?
> **Trả lời:**
> *"Dạ thưa thầy, nhóm đã tự cài đặt tầng tin cậy (Reliable UDP Layer) bên trên UDP tiêu chuẩn:
> 1. Mỗi gói tin gửi đi đều được đánh số thứ tự tuần tự (**Sequence Number**).
> 2. Phía nhận kiểm tra số Sequence. Nếu phát hiện bị hổng một tin nhắn (ví dụ có tin số 1 và số 3 nhưng thiếu tin số 2), Client sẽ tự động gửi gói yêu cầu **`RESYNC`** để lấy lại tin nhắn bị thiếu từ Server.
> 3. Đối với truyền file lớn: Nhóm áp dụng kỹ thuật **Cửa sổ trượt (Sliding Window)** kích thước 32 mảnh, có gói tin **ACK** xác nhận, cơ chế Timeout và Retry tối đa 8 lần, đồng thời kết thúc file bằng việc đối chiếu mã kiểm tra **CRC32** toàn vẹn."*

### ❓ Câu 3: Làm thế nào để Máy 2 chạy được mà không cần cài NetBeans hay Java?
> **Trả lời:**
> *"Dạ thưa thầy, nhóm đã sử dụng công cụ **`jpackage`** của bộ JDK 21 kết hợp cùng script build tự động. Công cụ này tiến hành quét các module Java cần thiết, trích xuất một bản rút gọn của Java Runtime Environment (JRE) và đóng gói trọn gói cùng với file JAR ứng dụng thành một tệp thực thi `.exe` độc lập dạng Self-contained. Nhờ đó, bất kỳ máy tính Windows nào chỉ cần mở file `.exe` là chạy được ngay mà không cần cấu hình biến môi trường hay cài đặt Java."*

### ❓ Câu 4: Vì sao trên giao diện trước đây có lúc xuất hiện các ký tự ô vuông, và nhóm đã khắc phục bằng cách nào?
> **Trả lời:**
> *"Dạ thưa thầy, lỗi ký tự ô vuông (gọi là Tofu glyphs) xảy ra khi giao diện Swing sử dụng các emoji Unicode mới (như kính lúp, mắt mật khẩu, chuông) nhưng font chữ mặc định của hệ điều hành Windows (Segoe UI) không có sẵn các glyph này trong bảng ký tự.
> Nhóm đã giải quyết triệt để vấn đề này bằng cách **chuyển toàn bộ các biểu tượng sang vẽ vector trực tiếp bằng Java2D (`Graphics2D`, `Path2D`)**. Các nét vẽ icon được render toán học theo tỉ lệ điểm ảnh, bật cơ chế khử răng cưa `ANTIALIASING`, nên đảm bảo hiển thị sắc nét, đồng nhất 100% trên mọi phiên bản Windows mà không bao giờ bị phụ thuộc vào font chữ hệ thống."*

---

## 7. Xử lý Nhanh các Sự cố Mạng Thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý trong 10 giây |
|---|---|---|
| **Bấm [Kiểm tra kết nối] báo Timeout** | 1. Nhập sai IP của Máy 1.<br>2. Chưa bấm [Bắt đầu] trên Server.<br>3. Windows Firewall chặn cổng UDP 5000. | • Xem lại IP hiển thị trên Server của Máy 1.<br>- Bấm nút [Bắt đầu] trên Server.<br>- Mở Windows Firewall cho phép ứng dụng. |
| **Đăng nhập được nhưng chat không thấy bên kia nhận** | Mạng Wi-Fi chặn gói tin Multicast (chế độ AP Isolation của router công cộng). | • **Giải pháp tốt nhất:** Bật **Mobile Hotspot** từ điện thoại hoặc một trong hai laptop, cho máy còn lại kết nối vào mạng Hotspot đó rồi thử lại. |
| **Mất file hoặc tải file bị lỗi** | Ổ cứng đầy hoặc chọn lưu vào thư mục hệ thống bị hạn chế quyền ghi. | • Chọn lưu file vào thư mục `Downloads` hoặc `Desktop` thông thường. |

---
*Tài liệu này được biên soạn đầy đủ để phục vụ cho buổi thuyết minh và chấm điểm đồ án Lập trình Mạng.*
