# Thiết kế giao diện UDP Multi-room Chat (Design System & UI Architecture)

Tài liệu quy định hệ thống thiết kế giao diện (Design System), cấu trúc bố cục (UI Architecture), các trạng thái tương tác và tiêu chuẩn thẩm mỹ cho ứng dụng Desktop UDP Multi-room Chat (Java 21 + Swing + FlatLaf).

---

## 1. Design Tokens (Hệ thống Token trong `Theme.java`)

Toàn bộ các thành phần giao diện của ứng dụng sử dụng chung một nguồn token duy nhất trong class `com.mycompany.udpmultichat.ui.Theme`. Tuyệt đối không tự định nghĩa mã màu riêng rẽ hoặc lặp mã màu trong các lớp UI con.

### 1.1. Bảng màu (Color Palette)

| Loại Token | Tên hằng số | Mã Hex / RGB | Mô tả & Ứng dụng |
|---|---|---|---|
| **Màu chủ đạo (Primary)** | `CHINH` | `#4F6BED` | Màu thương hiệu chính, nút Gửi, nút Tạo phòng, viền ô nhập khi focus |
| **Gradient Bắt đầu (Indigo)** | `GRADIENT_START` | `#4F6BED` | Điểm bắt đầu dải chuyển sắc cho nút chính, thẻ gradient, chỉ báo phòng |
| **Gradient Kết thúc (Blue)** | `GRADIENT_END` | `#2563EB` | Điểm kết thúc dải chuyển sắc hiện đại |
| **Gradient Hover** | `GRADIENT_HOVER_START/END` | `#5B75F0` / `#1D4ED8` | Trạng thái rê chuột trên nút gradient |
| **Gradient Active** | `GRADIENT_ACTIVE_START/END` | `#3F5BDD` / `#1E40AF` | Trạng thái nhấn chuột trên nút gradient |
| **Gradient Cảnh báo/Tin mới** | `GRADIENT_DO_START/END` | `#EF4444` / `#DC2626` | Pill badge unread gradient nổi bật, chỉ báo tin nhắn mới |
| **Chữ chính** | `CHU` | `#202635` | Nội dung văn bản chính, tên phòng, tiêu đề |
| **Chữ phụ** | `PHU` | `#596273` | Thời gian, vai trò, mô tả phòng, placeholder |
| **Nền ứng dụng** | `NEN` | `#F5F6FA` | Nền tổng thể, sidebar phòng, panel đăng nhập |
| **Nền thẻ / Card** | `THE` | `#FFFFFF` | Nền bong bóng chat trắng, card đăng nhập, popup menu |
| **Nền tin gửi** | `CHON` | `#E9EDFF` | Nền bong bóng tin nhắn của đối phương, highlight phòng đang chọn |
| **Viền chung** | `VIEN` | `#C9CED9` | Viền các khối card, viền bảng quản trị, viền thanh công cụ |
| **Viền ô soạn thảo** | `VIEN_SOAN` | `#B8BFCC` | Viền ô soạn thảo tin nhắn khi chưa focus |
| **Nền khóa / Vô hiệu** | `NEN_KHOA` | `#ECEEF2` | Nền ô soạn tin khi bị cấm chat (mute) hoặc mất kết nối |
| **Thành công (Success)** | `THANH_CONG` | `#217347` | Báo thành công, chấm online, đăng ký thành công |
| **Lỗi (Error / Danger)** | `LOI` | `#BC303A` | Thông báo lỗi form, nút thu hồi nguy hiểm, tin gửi lỗi |
| **Cảnh báo (Warning)** | `CANH_BAO` | `#8A5B00` | Cảnh báo phòng đóng, banner phục hồi kết nối |
| **Thông tin (Info)** | `THONG_TIN` | `#245FB3` | Tin nhắn thông báo của hệ thống, quản trị viên |
| **Avatar mặc định** | `AVATAR` | `#4261B9` | Nền avatar khi người dùng chưa đặt ảnh đại diện |
| **Offline** | `OFFLINE` | `#777F8D` | Chấm trạng thái ngoại tuyến của thành viên |

*Dark Mode Tokens (Dự phòng tương thích giao diện tối):*
- `TOI_NEN` (`#1E1F22`), `TOI_THE` (`#2B2D31`), `TOI_CHU` (`#DBDEE1`), `TOI_PHU` (`#949BA4`), `TOI_VIEN` (`#3F4147`).

### 1.2. Khoảng cách & Bo góc (Spacing & Radius)
- **Hệ thống Spacing chuẩn:**
  - `S4 = 4px`: Khoảng cách giữa các icon nhỏ, khoảng cách dòng phụ.
  - `S8 = 8px`: Khoảng cách đệm giữa các nút, padding thẻ tệp.
  - `S12 = 12px`: Khoảng cách lề khung chat, padding thẻ tin nhắn.
  - `S16 = 16px`: Lề cửa sổ, lề khối soạn thảo, khoảng cách giữa các phần lớn.
  - `S24 = 24px`: Padding card đăng nhập/đăng ký.
- **Bo góc (Border Radius):**
  - `BO = 8px`: Bo góc tiêu chuẩn cho các nút bấm (`JButton`), ô nhập liệu (`JTextField`), thẻ card (`JPanel`).
  - `BONG = 12px`: Bo góc cho bong bóng tin nhắn (`BongTin`), tạo cảm giác mềm mại, hiện đại.

### 1.3. Typography (Kiểu chữ & Kích thước)
- **Font mặc định:** `Segoe UI` (hệ điều hành Windows), fallback `SansSerif` trên nền tảng khác.
- **Cỡ chữ:**
  - Tiêu chuẩn: `14px PLAIN` (`Theme.FONT`) — áp dụng cho toàn bộ nội dung tin nhắn, nhãn chung.
  - Đậm: `14px BOLD` (`Theme.DAM`) — áp dụng cho tên người gửi, tiêu đề nút bấm chính.
  - Nhỏ: `12px PLAIN` (`Theme.NHO`) — áp dụng cho thời gian (timestamp), nhãn vai trò, gợi ý phụ.
  - Log máy chủ: `13px PLAIN Consolas` (`Theme.DON_CACH`) — hiển thị nhật ký đơn cách sắc nét.

### 1.4. Icon Vector Java2D (`Theme.icon`)
Toàn bộ biểu tượng trong ứng dụng được vẽ trực tiếp bằng Java2D vector với độ phân giải cao, khử răng cưa (`ANTIALIASING`), không phụ thuộc font hệ thống hay file ảnh ngoài:
- `copy`: Biểu tượng hai trang giấy xếp chồng — sao chép tin nhắn / IP.
- `undo`: Biểu tượng mũi tên uốn cong ngược — thu hồi tin nhắn.
- `more`: Biểu tượng 3 dấu chấm dọc `···` — mở menu tuỳ chọn.
- `smile`: Biểu tượng mặt cười — chọn Emoji & Sticker.
- `plus`: Biểu tượng dấu cộng `+` — đính kèm tệp tin.
- `send`: Biểu tượng mũi tên gửi tin.
- `lock`: Biểu tượng ổ khóa — phòng riêng tư có mật khẩu.
- `down` / `up`: Biểu tượng mũi tên mở rộng / thu gọn.

---

## 2. Kiến trúc bố cục giao diện (UI Architecture)

### 2.1. Cửa sổ Chat chính (`ChatMulticastUI`)
Ứng dụng sử dụng cấu trúc **3 cột tỷ lệ vàng** cho màn hình desktop:
1. **Cột trái — Thanh điều hướng & phòng (`DanhSachPhongPanel`, chiều rộng 280px):**
   - Header: Tiêu đề "UDP Chat", ô lọc tìm kiếm phòng tức thì.
   - Thao tác nhanh: Nút "+ Tạo phòng" (Primary), "Nhập mã" (Secondary).
   - Hai nhóm danh sách phòng: "PHÒNG CỦA TÔI" (phòng đã tham gia) và "PHÒNG CÔNG KHAI" (phòng có thể tham gia).
   - Thẻ phòng: Tên phòng, biểu tượng khóa (nếu riêng tư), thời gian tin cuối, xem trước nội dung tin cuối / sticker / file / thu hồi, badge số tin chưa đọc.
   - Thẻ hồ sơ tài khoản (góc dưới trái): Avatar tròn 32px, tên hiển thị (in đậm), vai trò (chữ xám nhỏ), nút mũi tên mở menu cá nhân (Đổi mật khẩu, Hồ sơ, Quản trị, Đăng xuất).
2. **Cột giữa — Khung trò chuyện chính:**
   - Header phòng: Tên phòng (in đậm 18px), mã phòng kèm nút "Sao chép mã", nút hiển thị/thu gọn danh sách thành viên, menu ba chấm quản trị phòng.
   - Luồng tin nhắn (`KhungChatPanel`): Tự động cuộn xuống đáy khi có tin mới; tin nhắn hệ thống căn giữa màu xám; bong bóng tin nhắn trái/phải rõ ràng.
   - Khung soạn thảo (`SoanTinPanel`): Ô nhập đa dòng toàn chiều rộng, thanh công cụ tích hợp đính kèm tệp và sticker, nút gửi tròn cố định.
3. **Cột phải — Danh sách thành viên (`ThanhVienPanel`, 240px, có thể bật/tắt):**
   - Tiêu đề: Tổng số thành viên và số người đang online.
   - Phân nhóm rõ ràng: "QUẢN TRỊ VIÊN", "CHỦ PHÒNG", "THÀNH VIÊN".
   - Chấm tròn xanh lá (`THANH_CONG`) báo online, chấm xám (`OFFLINE`) báo offline.
   - Nhãn "Bạn" màu xanh nhận diện chính xác tài khoản đang đăng nhập.

### 2.2. Màn hình Đăng nhập & Đăng ký (`DangNhapPanel`)
- Thẻ căn giữa màn hình (`GridBagLayout`), kích thước co giãn theo nội dung.
- Hai tab chuyển đổi nhẹ nhàng "Đăng nhập" / "Đăng ký".
- Trường mật khẩu có nút bật/tắt Hiện/Ẩn mắt mật khẩu.
- Báo lỗi nội tuyến (inline error) từng ô, chiều cao trường nhập không bị co nén khi có lỗi.
- Mục "Cài đặt máy chủ" dạng thu gọn (accordion), cho phép đổi IP và có nút "Kiểm tra kết nối" trước khi đăng nhập.

### 2.3. Cửa sổ Giám sát Máy chủ (`ServerMain`)
- Header: Tên máy chủ, địa chỉ IP LAN đang lắng nghe, cổng UDP 5000, nút "Sao chép IP".
- Bảng điều khiển: Nút "Bắt đầu", "Dừng", "Xóa log".
- 6 thẻ số liệu thống kê thời gian thực: Tài khoản, Đang online, Phòng, Tin nhắn, Phiên hoạt động, Tệp đang truyền.
- Thanh trạng thái xuất báo cáo: Nằm ở hàng riêng, rút gọn tên thư mục `baocao_...`, tách rời hoàn toàn cụm nút "Xuất báo cáo", "Mở thư mục", "Mở HTML", chống tràn giao diện 100%.
- Khung nhật ký đơn cách (Consolas 13px) có màu phân biệt Cảnh báo (vàng) và Lỗi (đỏ).

### 2.4. Trung tâm Quản trị & Báo cáo nâng cao (`QuanTriFrame`, `BaoCaoPanel`, `BaoCaoPreviewDialog`)
- **Cấu trúc 5 tab hiện đại:** "Tổng quan", "Người dùng", "Phòng", "Nhật ký", "Báo cáo".
- **Xuất riêng lẻ từng bảng:** Mỗi tab (Người dùng, Phòng, Nhật ký) có nút xuất riêng ("Xuất danh sách", "Xuất nhật ký") trực tiếp trên thanh công cụ của bảng, cho phép mở xem trước và in PDF tức thì mà không cần phải xuất toàn bộ hệ thống.
- **Tab Báo cáo — Thiết kế 2 Thẻ (Cards) khoa học:**
  - *Thẻ 1: Chọn nhóm dữ liệu:* Hộp chọn checkbox độc lập cho từng nhóm (Tài khoản người dùng, Phòng trò chuyện, Nhật ký kiểm toán, Thống kê 7 ngày & Biểu đồ) kèm 3 nút chọn nhanh: "Chọn tất cả", "Bỏ chọn", "Mặc định". Cho phép xuất linh hoạt bất kỳ tổ hợp nào (ví dụ: chỉ xuất 2 trong 3, hoặc 3 trong 4 nhóm).
  - *Thẻ 2: Định dạng & Kết quả:* Chọn định dạng xuất (Trang web HTML tự chứa & In/Lưu PDF, Tệp CSV Excel UTF-8 BOM), hiển thị thư mục đích và các nút "Mở thư mục", "Mở báo cáo HTML".
  - *Thanh nút hành động:* Nút "👁️ Xem trước báo cáo (In/PDF)" nổi bật (Primary) và "Xuất báo cáo đã chọn".
- **Hộp thoại Xem trước & In/Lưu PDF (`BaoCaoPreviewDialog`):**
  - Khung xem trước trực quan render HTML/CSS hoàn chỉnh trước khi xuất file.
  - Nút **"In / Lưu PDF"**: Tích hợp hộp thoại in chuẩn của hệ điều hành qua `JEditorPane.print(...)` (sử dụng "Microsoft Print to PDF" để lưu PDF không cần dependency bên ngoài).
  - Nút **"Mở trình duyệt"** và **"Lưu HTML"** hỗ trợ tương tác đa nền tảng.

---

## 3. Quy chuẩn tương tác và trạng thái thành phần (Component States)

### 3.1. Thanh thao tác tin nhắn nổi (Hover Action Bar)
- **Cơ chế:** Khi con trỏ chuột di chuyển vào bất kỳ phần tử nào của một hàng tin (`HangTin`), thanh công cụ `barHanhDong` (bo góc 8px, viền mảnh, nền trắng) tự động xuất hiện.
- **Nút bấm:**
  - Tin nhắn văn bản: Nút Sao chép (`copy`).
  - Tin nhắn của người gửi hợp lệ: Nút Thu hồi (`undo` đỏ).
  - Nút tuỳ chọn khác (`more` `···`): Mở popup menu.
- Khi chuột rời khỏi vùng tin nhắn, thanh công cụ tự động ẩn hoàn toàn.

### 3.2. Trạng thái gửi tin nhắn
- **Đang gửi (WAIT):** Chữ xám nhỏ "Đang gửi" dưới bong bóng tin nhắn.
- **Đã gửi (SENT):** Chữ xám nhỏ "Đã gửi" chỉ hiển thị ở tin nhắn mới nhất của người gửi.
- **Lỗi (ERROR):** Chữ đỏ "Lỗi" kèm tooltip lý do, bên cạnh có 2 nút thao tác nhanh "Gửi lại" và "Xóa" gạch chân khi hover.

### 3.3. Thông báo Toast (`ThongBao`)
- Nằm cách khung soạn thảo 12px về phía trên, căn giữa chiều ngang, rộng tối đa 360px, cao 44–48px.
- 4 phân loại màu:
  - Thành công: Nền xanh lá nhạt (`#EDF8F0`), chữ xanh lá đậm.
  - Lỗi: Nền đỏ nhạt (`#FFF0F1`), chữ đỏ đậm.
  - Cảnh báo: Nền vàng nhạt (`#FFF6DC`), chữ nâu vàng.
  - Thông tin: Nền xanh dương nhạt (`#EDF4FF`), chữ xanh dương.
- Hỗ trợ nút hành động nhanh (ví dụ: "Mở thư mục" sau khi xuất báo cáo / tải tệp).
- Tạm dừng đếm ngược tự ẩn khi người dùng rê chuột lên toast.

---

## 4. Tương thích HiDPI & Responsive
- Hỗ trợ tỷ lệ thu phóng màn hình Windows 100%, 125%, 150% thông qua cơ chế vector painting và FlatLaf scaling.
- Kích thước cửa sổ tối thiểu: `960 × 600 px`. Kích thước khuyến nghị: `1240 × 800 px`.
- Không bị tràn viền, không cắt chữ, không thanh cuộn ngang không cần thiết.
