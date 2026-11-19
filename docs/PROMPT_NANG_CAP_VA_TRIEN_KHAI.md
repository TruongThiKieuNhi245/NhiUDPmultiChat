# PROMPT CHÍNH THỨC — Hoàn thiện desktop UDP Multi-room Chat

Quyết định cập nhật ngày 17/11/2026: quay về desktop. Prompt này và `docs/KE_HOACH_HOAN_THIEN_DESKTOP.md` thay thế toàn bộ kế hoạch chuyển web W0–W8. Sao chép phần dưới để giao cho trợ lý lập trình.

## Lệnh bắt đầu cho Agent

Đọc AGENTS.md và docs/BAN_GIAO_AGENT.md trước; sau đó đọc trọn prompt này, ma trận và kế hoạch liên kết. Đây là prompt giao THỰC HIỆN dự án từ hiện trạng đã rà soát: kiểm tra nhanh baseline/git, rồi bắt đầu hoàn thiện M5 đang dở, không chỉ viết lại kế hoạch hoặc hỏi chọn công nghệ. Nếu nhận prompt trong một yêu cầu chỉ đọc/tư vấn thì vẫn tôn trọng phạm vi yêu cầu đó.

Baseline rà soát ngày17/11/2026 có66PASS/1FAIL ở avatar mất20%gói; ưu tiên tái hiện/sửa và hồi quy trước khi tuyên bố ổn định. Xem log và coverage trong bản bàn giao, không lấy số56PASS hoặc11PASS cũ làm kết quả hiện tại.

Điểm dừng đầu tiên là M5 hoàn tất theo đủ tiêu chí; kiểm thử, ảnh giao diện, docs và commit `M5 xong`, rồi dừng xác nhận trước M6. Không tự làm liên tục hết các mốc hoặc ghi M5 xong chỉ vì tests hiện có PASS. Danh sách khoảng trống cần xử lý nằm trong BAN_GIAO_AGENT.md.

## 1. Vai trò, mục tiêu và hướng đã chốt

Bạn là kỹ sư Java/lập trình mạng, thiết kế UI và giảng viên hướng dẫn. Hoàn thiện MỘT ứng dụng DESKTOP chat nhiều người, nhiều phòng, đúng bài Socket UDP + Multicast; giao diện thật đẹp, hiện đại, nhất quán, chức năng hữu ích đầy đủ và dễ thuyết trình. Người dùng máy khác không cần source/IDE/Java cài riêng: nhận bản portable, giải nén toàn bộ, bấm đúp để mở, kết nối ít thao tác rồi chat.

- Giữ Java release 21, Maven, Swing + FlatLaf hiện tại. Không chuyển JSP/Spring Boot/web frontend, không gateway, không HTTP/WebSocket/TCP thay đường chat UDP.
- Một codebase Maven, hai điểm chạy ClientMain và server.ServerMain. ServerMain là cửa sổ giám sát; admin đăng nhập bằng Client và mở trang Quản trị.
- Thầy từng cho phép web nhưng người dùng đã chọn lại desktop để ưu tiên đúng trọng tâm, ít lớp mạng, code dễ hiểu. Không hỏi lại chọn web hay desktop.
- Không hứa dùng website/browser cho bản này. Có thể public link tải ZIP/source nếu người dùng yêu cầu, không coi đó là public chat Internet. Cloud/relay/VPN không thuộc nghiệm thu hiện tại và không tự triển khai.
- Tiêu chí đẹp phải có ảnh component thật và thao tác thật để người dùng duyệt. Không coi việc cài FlatLaf là hoàn tất nâng cấp giao diện, không hứa “đẹp nhất/mới nhất” bằng cảm tính.

## 2. Đọc hiện trạng, giữ công việc đang dở

Workspace: `D:/New folder (2)/6_TH_LapTrinhMang/btap/Nhi_Btap_UDP_multiChat/UDPmultiChat`.
Tài liệu môn: `D:/New folder (2)/6_TH_LapTrinhMang/tailieu/Chap_6_Socket_UDP_2.pdf` và `Chap_6_1_MultiCast.pdf`.

Đọc AGENTS.md nếu có; toàn bộ source/test, pom.xml, README.md, docs/GIAI_THICH.md, ảnh docs/screenshots và .vscode. Chạy java -version, Maven -v, git status/log; dùng Maven NetBeans nếu PATH không có Maven. Không reset/xóa source/data thật.

Thông tin khảo sát cần xác minh lại:
- pom Java21/UTF-8, FlatLaf3.7.2, artifact UDPmultiChat/group com.mycompany; main com.mycompany.udpmultichat.ClientMain. VS Code có Server, Client1/2/3.
- Gói common/model, client, server, ui/components; 65 file main tại thời điểm khảo sát, không file quá400 dòng. Không ép về30–35 file nếu khiến khó đọc.
- Lõi đã có ServerCore/ClientCore/Listener, ReliableUdp/KenhPhong/MangUtil, phòng/tin/hồ sơ/avatar, TruyenTep/TepClient/QuanLyTep/KhoTep, lưu trữ. Sticker PNG Noto đã có nguồn và LICENSE.
- Commit nghiệm thu gần nhất lúc khảo sát `a212fe2 M4 xong`. Working tree có nhiều phần M5 chưa commit: QuanTriServer/Client/Frame, KiemDuyet, NhatKy, ChupThongKe, XuatBaoCao và AdminSelfTest. Tiếp tục kiểm tra/hoàn thiện, không viết lại từ đầu, không gắn nhãn M5 xong chỉ vì compile được.
- M4 từng56 PASS; AdminSelfTest có log riêng11 PASS nhưng chưa nghiệm thu M5 đầy đủ. Chạy lại baseline và ghi đúng kết quả hiện tại, không cộng số cũ thành kết quả mới.
- Đã thay tài liệu hướng web, nhưng source/pom chưa chuyển web. Không cần rollback ứng dụng về commit M4 để quay desktop; làm vậy sẽ mất M5 đang dở.

Lập ma trận yêu cầu→đã có/đang làm/thiếu/lỗi→file/hàm/test→mốc hoàn thiện. Tài liệu cũ có thể chậm hơn code; kiểm chứng từ source và test. Bảo toàn schema/data cũ, có backup/migration nếu cần đổi.

## 3. Kiến trúc mạng desktop bắt buộc

```text
Client A (Swing) -> ClientCore -> UDP unicast -> ServerCore
                                            kiểm quyền, msgId, lưu
                                                       |
                                               UDP multicast/phòng
                                                       v
                              KenhPhong của Client A/B/C joinGroup
                                      lọc roomId, thứ tự, chống trùng
                                                       |
                                               Listener -> Swing EDT
```

- DatagramSocket/DatagramPacket cho lệnh unicast, MulticastSocket cho chat nhóm thật. Server phát một datagram vào nhóm, không thay bằng vòng unicast rồi gọi là multicast.
- Một phòng có group239.255.42.X/cổng riêng, gói có roomId và Client lọc. Một user ở nhiều phòng nền, không lọt tin/typing/thẻ file giữa phòng.
- Giữ IPv4 trước socket và MangUtil.timGiaoDienMang; thêm lựa chọn card qua UI/config khi đa card. Hiển thị IP LAN cùng card mà Server dùng. Mọi socket multicast chọn đúng interface, loopback để nhiều Client cùng máy nhận tin mình khi cần, TTL theo phạm vi LAN.
- Lệnh UDP có seq/ACK/gửi lại/loại trùng, heartbeat5 giây, timeout~15 giây, RESYNC khi thiếu tin. Không coi ACK là đã nhận multicast. Phân biệt đường nhận multicast và lịch sử/RESYNC trong test/trace.
- Text UTF-8, escape pipe/newline, kiểm tra BYTE gói cuối dưới1400; nội dung chat khoảng1000 byte nếu header/escape vẫn vừa. UI counter theo ký tự và lỗi dễ hiểu, không lộ thuật ngữ kỹ thuật.
- File qua UDP unicast engine cửa sổ32 mảnh, DATA nhị phân khoảng1000 byte, ACK tích lũy/bitmap, CRC32, retry tối đa8/mảnh cấu hình được; điều khiển ReliableUdp có retry riêng. Không đổi file về dừng-chờ.
- Quyền luôn kiểm ở Server: phiên/endpoint, membership, private/closed/capacity/password, admin/owner, mute/ban/spam, file/report. Client giả mạo không được vượt.
- Đóng cửa sổ X gửi LOGOUT, logout/rời phòng/timeout dọn socket/threads/transfers/temp đúng. Không treo EDT khi mạng lỗi/CRC/avatar/report.

## 4. Chất lượng code dễ hiểu

Giữ Maven chuẩn và khả năng chạy VS Code/NetBeans; không preview, không đổi group/artifact. Không thêm dependency ngoài FlatLaf trừ khi nêu lý do và được đồng ý; ưu tiên JDK có sẵn cho đóng gói và vẽ chart.

- ui chỉ Swing/tương tác, không socket; client/server/common không Swing trừ ServerMain. Listener nối UI và chức năng; không thêm reflection/custom framework/mẫu thiết kế khó học.
- Tên hàm/biến tiếng Việt không dấu, nhất quán; comment không dấu, UI đầy đủ dấu, UTF-8. Class ghi Vai tro / Ai goi / Goi ai; mỗi hàm một dòng comment có ích, khối // ===== ... =====. Giải thích đoạn mạng khó bằng ý nghĩa, không chú thích máy móc mọi dấu ngoặc.
- Hàm khoảng≤40 dòng, Java file khoảng≤400 dòng, tối đa3 tầng lồng; không nhồi nhiều statement một dòng để lách giới hạn. Tách theo nhiệm vụ, không lớp vụn hoặc God class.
- Queue/cache/threads có giới hạn; synchronized/worker ghi file an toàn tạm→đổi tên; snapshot báo cáo nhất quán. Exception có thông báo dễ hiểu, không nuốt lỗi nghiệp vụ/mất tin âm thầm.
- Không mã thử/dead code trong bản nộp; fixture/test ở src/test/java hoặc UiPreview chuyên dụng. Comment, tài liệu và tham chiếu cập nhật khi chuyển/xóa file.

## 5. Khảo sát UI năm 2026 và tiêu chí thiết kế bắt buộc

Tra cứu nguồn chính thức Slack, Discord, Zalo PC đang có thể truy cập; có thể thêm Teams/Telegram nếu có lợi. Ghi ngày khảo sát, URL/ngày cập nhật, phạm vi xem được. Không bịa dùng app/ảnh, không gọi tài liệu cũ là tính năng vừa ra2026. Lập bảng sản phẩm→pattern→áp dụng→lý do→màn/ảnh kiểm chứng. “Đầy đủ hiện đại” là đầy đủ checklist dưới, không tự mở rộng AI/voice/video/mạng xã hội khiến bài quá sức; đề xuất mới ngoài phạm vi phải nêu riêng, không tự bỏ mục đã yêu cầu.

Design system trong Theme.java:
- Primary #4F6BED, viền #C9CED9, composer #B8BFCC; token sáng/tối và semantic success/error/warning/info/disabled đủ tương phản, không lặp mã màu ở file khác.
- Spacing 4/8/12/16/24 px; bo góc 8 px cho control/card và 12 px cho bong bóng. Font Segoe UI/hệ thống 14 px PLAIN; tên 14 px đậm, chữ phụ 12 px xám; icon thống nhất nét và kích thước. FlatLaf cài trước khi tạo Swing; không nút Metal hoặc gradient xám mặc định.
- Ba cột: sidebar phòng/tài khoản; giữa header/chat/composer; phải thành viên thu gọn. Bỏ banner lớn và tab phòng lặp. Các phòng nền giữ vị trí cuộn; unread chỉ ở sidebar. Tối thiểu khoảng 960×600, co giãn tốt, thử HiDPI 100/125/150%, không cắt chữ.
- Login/register riêng bằng CardLayout; xác nhận mật khẩu, Hiện/Ẩn, Enter, loading, lỗi đúng ô, không nén input khi lỗi. Server nằm trong Cài đặt máy chủ, có lần trước và Kiểm tra kết nối; nhớ username/IP, không mật khẩu.
- Sidebar hai nhóm phòng của tôi/công khai chưa tham gia, lọc cả hai, preview tin/tệp/sticker/thu hồi, thời gian và unread. Nút Tham gia rõ bấm được; menu ba chấm/chuột phải đúng quyền, xác nhận xóa/rời. Thẻ tài khoản có avatar, tên đậm và vai trò nhỏ; menu chỉ hiện chức năng đã hoạt động.
- Header có tên, mã/copy, số thành viên/menu/thu gọn; thông tin phòng dùng HopThoai chung, ngày dd/MM/yyyy. Member nhóm Admin/Chủ/Thành viên, online/offline rõ, nhãn Bạn theo login/id; không chấm online giả trên avatar tin.
- Composer: vùng nhập toàn chiều rộng; hàng dưới icon/sticker/đính kèm trái và nút Gửi tròn cố định phải. Một dòng gọn, auto-grow tối đa 5 dòng, chỉ hiện thanh cuộn khi cần. Enter gửi, Shift+Enter xuống dòng, placeholder/counter dễ hiểu, Gửi vô hiệu khi rỗng. Offline/mute nền bo tròn đồng nhất, lý do rõ; không hình vuông xám hoặc thanh cuộn lộ.
- Chat có avatar/tên/giờ, nội dung PLAIN; sticker ảnh 112–120 px không bong nền, file card 280–320 px và thumbnail tối đa 240 px, progress/hủy/retry. Menu copy/recall/quote theo quyền. Ngày/grouping/search thống nhất; chỉ auto-scroll khi đang ở đáy, có lối về tin mới.
- Toast tối đa 3, khoảng 44–48 px cho một dòng và rộng tối đa 360 px, giữa dưới chat cách composer 12 px; chữ/icon căn giữa dọc, hover pause, đóng được, 4 mức. Không báo thành công mỗi tin. Banner kết nối chỉ hiện khi sự cố/phục hồi; dòng vào/ra hệ thống giữ giữa chat.
- Admin 5 tab gạch chân; JTable cao dòng 36 px, nền xen kẽ nhẹ/selection/header rõ, filter/sort/search, status pill/avatar, toolbar gọn và menu ba chấm. Xác nhận nguy hiểm đỏ, có đối tượng và hậu quả; loading/empty/error/retry cho mỗi bảng, không nút giả.
- Focus, phím tắt, tooltip và tương phản rõ; không ép desktop giống mobile, không dùng emoji font cho icon/sticker quan trọng. Ảnh preview phải component thật; người dùng duyệt ảnh và thao tác trước khi ghi nhận UI hoàn tất.

## 6. Checklist nghiệp vụ phải giữ và hoàn thiện

Đọc thêm `docs/MA_TRAN_CHUC_NANG_DESKTOP.md`: checklist bắt buộc thống kê/báo cáo, nhóm/mời thành viên, biệt danh và sticker; tài liệu này bổ sung chi tiết, không thay thế yêu cầu cũ. Thêm thành viên và biệt danh nhóm chưa được coi là đã cài chỉ vì có trong kế hoạch.

### Tài khoản và hồ sơ

- Đăng ký, đăng nhập, đăng xuất; tên đăng nhập bất biến và tên hiển thị đổi được; chống trùng và đăng nhập hai nơi. Lưu mật khẩu băm SHA-256 có salt, không plaintext.
- Hồ sơ: tên hiển thị 2–30 ký tự, giới thiệu tối đa 100 ký tự; avatar PNG/JPG tối đa 2 MB, cắt vuông giữa ảnh và thu nhỏ 96×96. Có xem trước, Xóa ảnh, Lưu, Hủy; chỉ gửi khi Lưu. Truyền ảnh bằng mảnh khoảng 600 byte, ACK/CRC32, phiên bản/hash và cache có giới hạn, cập nhật realtime cho Client khác.
- Đổi mật khẩu: cũ/mới/nhập lại, 6–128 ký tự, Hiện riêng và lỗi đúng ô. Admin dùng mật khẩu mặc định hoặc user dùng mật khẩu tạm phải đổi trước khi làm nghiệp vụ; hộp bắt buộc không có Hủy và không thoát bằng Esc/X.
- Nhận diện mình bằng tên đăng nhập/id. Client vào cuối cũng có đúng một nhãn “Bạn”.

### Phòng, chat và tệp

- Phòng CRUD, mã 6 ký tự, công khai/riêng tư, mật khẩu Giữ/Đổi/Xóa, giới hạn 2–500, đóng/mở và tham gia bằng mã. Sảnh chung luôn tồn tại, không xóa/đổi loại/rời. Phòng riêng không lộ cho người chưa tham gia, trừ admin. Chủ chuyển quyền trước khi rời. Cập nhật realtime, 50 tin lịch sử, RESYNC, nhiều phòng nền và unread.
- Chat UTF-8, sticker PNG Noto có LICENSE, typing tối đa một lần/3 giây và tự tắt sau 4 giây, trạng thái đang gửi/đã gửi/lỗi khoảng 8 giây, gửi lại giữ mã chống trùng. Người gửi thu hồi trong 10 phút; chủ/admin thu hồi theo quyền. Tombstone trong lịch sử/RESYNC không lộ nội dung cũ. Có sao chép nội dung.
- M3c đầy đủ: tìm kiếm trong phòng, trả lời/trích dẫn, gộp tin liên tiếp, phân cách ngày, tắt thông báo từng phòng, xuất lịch sử .txt. Trích dẫn tin đã thu hồi không lộ nội dung cũ. Nêu rõ tìm toàn bộ lịch sử hay chỉ cache; không gọi tìm trong 50 tin là tìm toàn bộ.
- Tệp tối đa 10 MB cấu hình được; 0 byte và vượt giới hạn bị chặn ở Client/Server. Hai upload/người, hàng đợi giới hạn; cửa sổ 32, gửi lại 8 lần cấu hình; tiến độ, tốc độ/ETA nếu đo được, hủy/thử lại, CRC32 và so byte. Thẻ tệp, thumbnail, hộp chọn nơi lưu mặc định Downloads, không ghi đè, Mở thư mục.
- Chỉ thành viên gửi/tải; phòng đóng hoặc mute/ban không gửi mới. Làm sạch tên, chống path traversal và tên đặc biệt Windows, lưu UUID.bin và metadata. Dọn tạm khi hủy, timeout 60 giây, đăng xuất/restart. Thu hồi/xóa phòng xóa tệp; thống kê theo phòng/người. Tệp có trong lịch sử/RESYNC, không tự thực thi.

### Nhóm, lời mời và biệt danh bổ sung

- Chủ/admin có nút Thêm thành viên: tìm tài khoản, gửi lời mời trong app; người nhận chấp nhận/từ chối, không chỉ đưa mã phòng ngoài app. Lời mời offline có hạn/persist, hủy được; Server kiểm quyền, ban/locked/closed/full, chống trùng và race capacity khi chấp nhận. Chỉ sau chấp nhận mới ghi membership và vào multicast; mã phòng/lối tham gia cũ vẫn giữ.
- Phân biệt login bất biến, tên hiển thị toàn hệ thống và biệt danh riêng từng nhóm. Thay biệt danh không đổi id/login/quyền, không ảnh hưởng phòng khác; tooltip @login, fallback tên hiển thị khi xóa. Đổi/cập nhật realtime và lưu qua restart, kiểm quyền ở Server theo ma trận chi tiết.
- Kick/Mute/Ban/Transfer giữ đầy đủ như M5. Mời/thêm và nickname nhóm đưa vào M3c; nếu cần chia mốc phụ phải đề xuất trước, không tự cắt.

### Sticker ảnh nguồn mở

Project đã có 36 PNG Noto Emoji 2D và LICENSE/NOTICE/SHA256SUMS. Tái sử dụng ảnh, không vẽ Java2D hoặc dùng ký tự emoji làm sticker. Muốn thêm bộ ảnh phải có license phân phối và nguồn rõ. Nếu không tải được, yêu cầu người dùng hỗ trợ với danh sách file/link/định dạng/kích thước/license/thư mục chép cụ thể. Có tìm nhanh, nhóm, tooltip, thu phóng đẹp và test resources trong portable.

### Quản trị, kiểm duyệt và báo cáo M5

- Chỉ ADMIN thấy menu, mọi lệnh quản trị kiểm quyền tại Server. Đủ 5 tab Tổng quan/Người dùng/Phòng/Nhật ký/Báo cáo; tự cập nhật 3–5 giây và theo sự kiện, có Làm mới.
- Bảng user: tên đăng nhập/hiển thị/vai trò/Online-Offline-Bị khóa/số phòng/số tin/ngày tạo/lần login cuối; tìm/lọc/sắp xếp. Thêm user có mật khẩu tạm buộc đổi; sửa tên, giới thiệu, vai trò; reset ngẫu nhiên 10 ký tự hiển thị một lần, sao chép và hủy phiên. Khóa có lý do và ngắt phiên ngay. Xóa xác nhận gõ lại login, gỡ mọi phòng/avatar, chuyển chủ cho thành viên lâu nhất hoặc admin; tin cũ giữ nhãn “Người dùng đã xóa”. Không tự khóa/xóa/hạ quyền; luôn còn admin hoạt động.
- Bảng phòng: mã/tên/loại/trạng thái/chủ/thành viên/tin/tệp/dung lượng/ngày tạo. CRUD, chọn chủ, đóng/mở, xóa cả tệp, bảo vệ Sảnh, xem thành viên, kiểm duyệt và xóa lịch sử. Đồng bộ tombstone/msgId không làm lộ nội dung đã xóa.
- Kiểm duyệt: Kick, Mute 5 phút/1 giờ/24 giờ/vĩnh viễn, Unmute, Ban/Unban, chuyển chủ; xác nhận và lý do tùy chọn. Chủ không kiểm duyệt admin/chủ khác, admin không tự kiểm duyệt. Lưu qua restart và tự hết hạn. Mute khóa text/sticker/file/thu hồi cả ở Server; kick/ban đóng khung phòng và báo toast. Chống spam 5 tin/3 giây, 20 tin/phút cấu hình; vi phạm lặp tự mute 60 giây và ghi audit.
- Broadcast tối đa 200 ký tự đến mọi Client online, nhãn Quản trị viên, lưu tin hệ thống ở mọi phòng. Thống kê tài khoản/online/locked/phòng mở-đóng/tin/tệp/bytes; biểu đồ Java2D top 8 phòng theo thành viên và 7 ngày tin nhắn.
- Audit data/audit.log: một sự kiện/một dòng, append, xoay 5 MB; đăng nhập sai nhiều lần, từ chối quyền, CRUD/role/khóa/reset/moderation/broadcast/export. Có lọc và xuất, không ghi token/password.
- Báo cáo chạy nền từ snapshot nhất quán: reports/baocao_timestamp không trùng; 4 CSV UTF-8 BOM, RFC4180 và chống công thức: nguoidung/phong/thanhvien_theophong/tin_theo_ngay. HTML tự chứa, CSS in được, escape &, <, >, nháy kép/đơn; có tổng, số user theo từng phòng, top 10 người chat, top phòng, biểu đồ ngày và tài khoản bị khóa. Ghi tạm rồi đổi tên, không ghi đè. Admin Client tải ZIP qua UDP M4, giải nén an toàn, toast và mở thư mục/HTML; Server có nút xuất trực tiếp. Số liệu phải tính đúng cả thành viên offline.
- Server UI: IP LAN và sao chép, 6 thẻ gồm phiên và tệp đang truyền, log Consolas 13 có màu lỗi/cảnh báo và Xóa log, xuất báo cáo, Bắt đầu/Dừng không đơ.

Dùng đầy đủ chi tiết yêu cầu các mốc trước còn phù hợp desktop, không tự giảm phạm vi khi chuyển kế hoạch về desktop.

## 7. Mở dễ trên máy khác: portable và kết nối LAN

Bắt buộc ZIP Windows portable bằng `jpackage --type app-image`: Client có `UDPChatClient.exe`, Server có `UDPChatServer.exe`, kèm runtime/dependencies/resources. Người nhận không cần Java/JDK/Maven/IDE/PATH/source hoặc quyền admin khi mở bình thường. Giải nén cả thư mục, không chép riêng .exe. Build theo OS/CPU đích, không hứa một bản chạy mọi hệ điều hành.

Hành trình tối thiểu:
1. Máy A giải nén Server và Client nếu cần; mở Server → Bắt đầu → thấy IP/cổng/nút Sao chép.
2. Máy B giải nén Client, bấm đúp → nhập IP LAN máy A lần đầu → Kiểm tra kết nối → đăng ký/đăng nhập.
3. Hai người vào cùng phòng/mã, chat/sticker/avatar/file. Ghi nhớ IP và login lần sau, không lưu mật khẩu. `localhost` chỉ dùng khi Server cùng máy.

Bổ sung nút Tìm máy chủ nếu đơn giản: discovery UDP riêng, timeout ngắn, danh sách cho người dùng chọn; group/cổng không xung đột phòng. Không tự gửi mật khẩu tới server phát hiện được. Nhập IP và Kiểm tra kết nối luôn có. Nếu discovery cần thay đổi phức tạp, báo trước; không bắt người dùng cấu hình router để thực hiện thao tác cơ bản.

Chẩn đoán UI phân biệt Server chưa chạy/sai IP/ACK timeout, multicast không nhận và kênh tệp bị chặn; kỹ thuật chi tiết ở log. Cùng Wi-Fi chưa chắc đủ: guest/AP isolation, VLAN/multicast filter, card IPv4/subnet, VPN/card ảo; TTL=1 không đi qua mọi router. Có probe UDP HELLO và multicast, không chỉ ping.

Firewall phải kiểm tra với ứng dụng portable thực tế và network profile phù hợp trên mạng tin cậy: UDP 5000, cổng phòng, cổng dữ liệu động. Không chỉ cho Java trong IDE rồi suy ra portable cũng chạy. Windows/SmartScreen có thể hỏi xác nhận hoặc chính sách máy chặn: hướng dẫn trung thực, không tắt toàn bộ bảo vệ hoặc tự sửa firewall bằng quyền admin.

Thư mục data/client config/log/reports/temp phải ổn định khi bấm đúp hoặc đổi CWD; không ghi vào runtime/resources hoặc đường dẫn máy lập trình, không đóng gói dữ liệu/mật khẩu thật. Cho chạy 2–3 Client bằng profile cấu hình rõ ràng, tránh ghi chồng preferences. Tài nguyên nạp đúng khi đường dẫn có dấu/khoảng trắng. Có build-portable.ps1, README một trang, giấy phép dependency.

## 8. Kiểm thử và bằng chứng nghiệm thu

- Compile Java21 bằng Maven, xử lý cảnh báo; SelfTest đầy đủ và AdminSelfTest, không sửa test để che lỗi. Mất gói 20% có seed; fixture .selftest riêng với data thật.
- Ít nhất 3 Client/2 phòng: cách ly, login/id/Bạn/quyền, CRUD/private/password/full/transfer/history/RESYNC; avatar CRC/password default-temp; sticker/retry/typing/recall; moderation/spam/report số liệu biết trước/audit/race.
- Giữ ACK và chặn multicast trong fixture: người khác không nhận live, người gửi không SENT chỉ vì ACK; RESYNC được kiểm soát/đánh dấu riêng, khôi phục thì bù không trùng. Có bằng chứng send/receive/join/leave/roomId, không multicast giả bằng unicast.
- Tệp 5 MB so byte và CRC hai chiều; 1 byte/1 mảnh/1 MB/0 byte/>10 MB, mất 20%/trùng/đảo thứ tự, hủy cả hai phía, disconnect/timeout, quyền/tên độc hại, thu hồi/xóa/restart/đồng thời/độ trễ chat/ảnh hỏng. Đo localhost và LAN riêng, không hứa tốc độ giống nhau.
- UiPreview dùng component THẬT: mọi màn, form lỗi/offline/mute, sáng/tối/DPI, admin/file/reply/search. Assertions font PLAIN, input không nén, không thanh cuộn khi một dòng, menu đúng quyền. Xem ảnh và sửa; preview không thay thao tác chuột/phím thật.
- Portable trên máy không Java/Maven/IDE: đường dẫn có dấu/khoảng trắng, bấm đúp, restart và nhớ cấu hình, sticker/FlatLaf/avatar/download/report; đúng OS/CPU.
- HAI MÁY THẬT: Server A + Client A, Client B cùng LAN; đăng ký, vào phòng bằng mã, text/sticker/avatar, file 5 MB CRC, kiểm duyệt/broadcast/report/restart. Gói multicast từ Server A tới Client B; capture/probe nếu có. Một máy/VM không được báo đã thử hai máy vật lý; ghi giới hạn và checklist để người dùng thực hiện.

## 9. Mốc, tài liệu và dừng xác nhận

Theo kế hoạch desktop: rà soát ngắn → M5 → M6 → M3c → M7. M6 gồm thiết kế UI, sáng/tối/HiDPI và kết nối tiện dụng. M7 chốt portable, hai máy, hồi quy và tài liệu. Smoke đóng gói sớm ở M6, không chờ cuối mới phát hiện runtime không chạy.

Lần tiếp quản đầu đọc bản bàn giao và kiểm baseline, nêu ngắn gọn việc sẽ làm rồi thực hiện M5 đang dở; không dừng chỉ ở lập kế hoạch. Không hỏi lại công nghệ đã duyệt. Chưa chuyển sang nâng cấp UI M6 trước khi nghiệm thu M5. Mỗi mốc compile/test/UiPreview/xem ảnh/docs, commit local đúng “M? xong” chỉ khi đạt; nêu 3–5 bước thử tay và điều chưa kiểm chứng, DỪNG chờ xác nhận. Không push remote/public/mua cloud tự ý. Nếu cần chia M6a/M6b thì chốt phạm vi và điểm dừng trước khi làm.

Bàn giao README; docs/GIAI_THICH.md (yêu cầu→chức năng→file/hàm→test toàn bộ), KHAO_SAT_CHAT_2026.md, THIET_KE_GIAO_DIEN.md, CHAY_HAI_MAY.md, DONG_GOI_PORTABLE.md, BAO_CAO_RA_SOAT.md; demo 5 phút, cây file cuối, mọi file chuyển/xóa và giấy phép tài nguyên.

Giới hạn ghi rõ: UDP không bảo đảm; multicast phụ thuộc LAN/router/Windows Firewall; token/mật khẩu đi UDP chưa mã hóa nên chưa phù hợp public Internet; tệp dở không resume; quy mô/RAM lịch sử và phạm vi thử thực tế. Không tuyên bố production thương mại nếu chưa kiểm chứng.

## 10. Nguồn kiểm chứng

- FlatLaf: https://www.formdev.com/flatlaf/
- jpackage: https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html
- App image: https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html
- MulticastSocket: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/MulticastSocket.html
- Slack: https://slack.com/help/articles/32562841868307-Add-and-manage-tabs-in-channels-and-direct-messages
- Discord tìm kiếm: https://support.discord.com/hc/en-us/articles/115000468588-How-to-Use-Search-on-Discord
- Discord điều hướng: https://support.discord.com/hc/en-us/articles/31232432266647-Discord-Commands-Shortcuts-and-Navigation-Guide
- Zalo PC: https://www.zalo.me/pc
