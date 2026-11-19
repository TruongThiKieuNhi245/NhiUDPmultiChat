# Khảo sát giao diện nền tảng Chat hiện đại (Năm 2026)

*Ngày khảo sát:* 19/11/2026  
*Phạm vi nghiên cứu:* Ứng dụng Desktop Chat (Slack Desktop, Discord Desktop, Zalo PC, Telegram Desktop)  
*Mục đích:* Chuẩn hóa toàn bộ trải nghiệm người dùng (UX) và giao diện (UI) cho ứng dụng UDP Multi-room Chat (Java Swing + FlatLaf) theo các chuẩn thiết kế chat hiện đại nhất năm 2026.

---

## 1. Nguồn khảo sát chính thức

| Nền tảng | Tài liệu / Nguồn chính thức | Ngày cập nhật / Truy cập | Nội dung tham chiếu |
|---|---|---|---|
| **Discord** | [Discord Search Guide](https://support.discord.com/hc/en-us/articles/115000468588-How-to-Use-Search-on-Discord) | Tháng 11/2026 | Thanh điều hướng, bộ lọc tìm kiếm, phím tắt |
| **Discord** | [Discord Navigation Guide](https://support.discord.com/hc/en-us/articles/31232432266647-Discord-Commands-Shortcuts-and-Navigation-Guide) | Tháng 11/2026 | Message Hover Action Bar, danh sách kênh, thanh trạng thái |
| **Slack** | [Slack Channel Tabs & UI](https://slack.com/help/articles/32562841868307-Add-and-manage-tabs-in-channels-and-direct-messages) | Tháng 11/2026 | Bố cục 3 cột, Composer đa dòng, Floating action toolbar |
| **Zalo PC** | [Zalo PC Trang chủ & Bản cập nhật](https://www.zalo.me/pc) | Tháng 11/2026 | Menu ngữ cảnh tin nhắn, thao tác thu hồi/sao chép, thẻ tệp |
| **FlatLaf** | [FlatLaf Look and Feel](https://www.formdev.com/flatlaf/) | v3.7.2 (2026) | Design system, token bo góc, antialiasing, typography |

---

## 2. Bảng đối chiếu Pattern thiết kế và áp dụng vào UDP Chat

| Nền tảng | Pattern chuẩn | Áp dụng vào UDP Multi-room Chat | Lý do UX / Kỹ thuật | Ảnh kiểm chứng |
|---|---|---|---|---|
| **Discord / Slack** | **Message Hover Action Bar** (Thanh công cụ nổi khi rê chuột) | Thanh thao tác nhỏ (26px) ẩn mặc định; chỉ xuất hiện khi rê chuột vào hàng tin: gồm nút Sao chép, nút Thu hồi (nếu là tin mình), nút `···` mở menu | **Tuyệt đối không để nút văn bản thường trực** bên cạnh tin nhắn gây rối mắt, phá vỡ bố cục bong bóng; người dùng vẫn dễ dàng thấy nút thao tác khi rê chuột mà không cần đoán chuột phải | `33-chat-sticker-trang-thai.png`, `34-menu-tin-nhan.png` |
| **Slack / Discord** | **Bố cục 3 cột kinh điển** (Sidebar - Chat - Member) | Cột trái (280px) danh sách phòng & thẻ hồ sơ; Cột giữa (co giãn) header, tin nhắn & composer; Cột phải danh sách thành viên có thể thu gọn | Tối ưu hóa không gian màn hình desktop, tập trung luồng đọc ở giữa, thao tác chuyển phòng tức thì không cần tab lặp | `04-chat.png` |
| **Slack / Zalo PC** | **Khung soạn thảo Auto-grow** (Composer) | Textarea tự co giãn từ 1 dòng đến tối đa 5 dòng; thanh cuộn chỉ xuất hiện khi vượt 5 dòng; hàng nút tiện ích (Emoji, Đính kèm) bên trái và nút Gửi tròn bên phải | Người dùng nhập tin nhắn ngắn gọn gàng; tin nhắn nhiều dòng mở rộng mượt mà không che lấp lịch sử chat | `13-tin-qua-dai.png`, `04-chat.png` |
| **Telegram / Zalo** | **Bong bóng tin nhắn phân biệt hai chiều** | Tin người khác căn trái nền xám nhạt (`#E9EDFF`), tin của mình căn phải nền xanh thương hiệu (`#4F6BED`), chữ trắng, bo góc 12px | Giúp người dùng phân biệt ngay lập tức tin nhắn của mình và đối phương theo thói quen thị giác tự nhiên | `04-chat.png` |
| **Discord** | **Phân nhóm thành viên theo vai trò** | Cột phải chia nhóm: "QUẢN TRỊ VIÊN", "CHỦ PHÒNG", "THÀNH VIÊN"; chấm xanh báo online, xám báo offline; nhãn "Bạn" duy nhất | Quản trị và giao tiếp nhóm minh bạch, biết rõ ai đang online để tương tác | `04-chat.png` |
| **Zalo PC / Slack** | **Thẻ tệp đính kèm chuyên dụng (File Card)** | Thẻ tệp bo góc (280–320px) có icon tệp/thumbnail ảnh, dung lượng, tiến độ tải và nút hành động (Tải về / Mở thư mục) | Không chèn file raw vào text; giao diện chuyên nghiệp, trạng thái tải rõ ràng | `30-avatar-trong-chat.png`, `04-chat.png` |
| **Slack** | **Thông báo Toast không che ô nhập** | Toast nổi góc dưới giữa cách composer 12px, tối đa 3 thông báo, 4 mức độ (Thành công, Thông tin, Cảnh báo, Lỗi), tự biến mất | Thông báo phản hồi thao tác tức thì, không dùng dialog popup chặn luồng thao tác của người dùng | `06-toast-ba-muc.png`, `07-toast-loi.png` |
| **Discord / Teams** | **Banner trạng thái mạng sự cố** | Banner chỉ hiện khi mất kết nối hoặc đang phục hồi; tự ẩn sau 3.8s khi kết nối ổn định | Tránh làm phiền người dùng trong trạng thái bình thường; cảnh báo rõ ràng khi gặp sự cố mạng | `05-mat-ket-noi.png` |

---

## 3. Phân tích chi tiết lỗi thiết kế cũ và giải pháp chuẩn hóa

### 3.1. Vấn đề nút thao tác tin nhắn (Sao chép / Thu hồi)
- **Thiết kế lỗi trước đó:** Đặt trực tiếp nút văn bản "Sao chép" (màu xanh) và "Thu hồi" (màu đỏ) nằm thường trực ngay bên cạnh hoặc bên dưới bong bóng tin nhắn và sticker.
  - *Hậu quả UX:* Giao diện biến dạng, tin nhắn nào cũng đính kèm chữ "Sao chép" "Thu hồi", tạo cảm giác chắp vá, hoàn toàn không giống bất kỳ ứng dụng chat chuyên nghiệp nào (Discord, Slack, Zalo, Telegram).
- **Thiết kế chuẩn hóa mới (M6):**
  1. **Hover Action Bar:** Mỗi hàng tin (`HangTin`) được tích hợp một thanh công cụ nổi bo góc nhỏ (`barHanhDong`), nền trắng, viền mảnh:
     - Nút icon Sao chép (`copy` icon): Tooltip "Sao chép".
     - Nút icon Thu hồi (`undo` icon màu đỏ): Tooltip "Thu hồi tin nhắn" (chỉ hiển thị với tin nhắn của mình trong vòng 10 phút hoặc quyền quản trị).
     - Nút icon Tuỳ chọn khác (`more` icon `···`): Mở popup menu đầy đủ.
     - *Trạng thái:* **Ẩn mặc định**. Chỉ hiển thị khi chuột rê (`mouseEntered`) vào vùng tin nhắn và tự động ẩn khi chuột rời khỏi hàng (`mouseExited`).
  2. **Menu ngữ cảnh (Context Menu):** Nhấp chuột phải vào bất kỳ vị trí nào (avatar, bong bóng, nội dung, thời gian) đều mở menu ngữ cảnh với đầy đủ tuỳ chọn.

### 3.2. Vấn đề hiển thị đường dẫn dài trong ứng dụng
- **Thiết kế lỗi trước đó:** Hiển thị toàn bộ đường dẫn tuyệt đối dài (ví dụ: `D:\New folder (2)\6_TH_LapTrinhMang\...`) trên giao diện máy chủ (`ServerMain`) và trong khung chat/log, dẫn đến tràn viền, đè lên các nút thao tác ("Xuất báo cáo", "Mở thư mục", "Mở HTML").
- **Thiết kế chuẩn hóa mới (M6):**
  1. Hàm rút gọn tên `tenNgan(Path p)` chỉ lấy tên thư mục cuối (ví dụ: `baocao_20261119_123529`).
  2. Bố cục `BorderLayout` tách biệt: `trangThai` nằm tại `CENTER`, cụm nút xuất báo cáo nằm tại `EAST` với khoảng cách đệm, không bao giờ xảy ra tình trạng chồng lấn.
  3. Đường dẫn đầy đủ được đưa vào Tooltip khi rê chuột (`setToolTipText(p.toAbsolutePath().toString())`), giữ giao diện luôn gọn gàng và thoáng đãng.

---

## 4. Kết luận chuẩn hóa M6
Việc áp dụng các pattern thiết kế năm 2026 từ Slack, Discord và Zalo PC giúp ứng dụng UDP Multi-room Chat đạt chuẩn giao diện desktop chuyên nghiệp, thẩm mỹ cao, thao tác thuận tiện và sẵn sàng cho các tính năng nâng cao tiếp theo ở M3c và M7.
