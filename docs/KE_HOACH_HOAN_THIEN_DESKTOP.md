# Kế hoạch hoàn thiện desktop UDP Multi-room Chat

Quyết định ngày 17/11/2026. Đây là kế hoạch đang áp dụng, thay thế W0–W8 chuyển web. Nguồn yêu cầu đầy đủ: [PROMPT_NANG_CAP_VA_TRIEN_KHAI.md](PROMPT_NANG_CAP_VA_TRIEN_KHAI.md).

Bổ sung bắt buộc: [Ma trận chức năng](MA_TRAN_CHUC_NANG_DESKTOP.md), ghi riêng thống kê/báo cáo của thầy, mời/thêm thành viên, biệt danh nhóm và bộ sticker ảnh nguồn mở.

## 1. Phương án đã chốt

| Nội dung | Quyết định |
|---|---|
| Bản nộp | Desktop Java 21 + Swing + FlatLaf, Maven chuẩn |
| Mạng điều khiển và tệp | UDP unicast; file cửa sổ trượt 32, retry 8 cấu hình |
| Chat theo phòng | Server phát UDP multicast, các Client trực tiếp joinGroup |
| Giao diện | Hiện đại theo Slack/Discord/Zalo PC, dùng Theme thống nhất, duyệt ảnh và thao tác thật |
| Máy khác sử dụng | ZIP portable kèm runtime, giải nén và bấm đúp .exe |
| Kết nối | Server hiện IP LAN/copy; Client nhớ IP, Kiểm tra kết nối; discovery là bổ sung có đánh giá riêng |
| Nghiệm thu chính | Hai máy thật cùng LAN/Wi-Fi, text/sticker/avatar/file/quyền/báo cáo |
| Web/cloud | Không triển khai trong kế hoạch này; public link tải chỉ khi có yêu cầu riêng |

Source chưa chuyển web, pom vẫn Swing/FlatLaf. Không reset về M4: như vậy sẽ mất phần M5 chưa commit. Chỉ thay tài liệu định hướng, tiếp tục từ working tree hiện tại sau khi kiểm tra.

## 2. Thứ tự thực hiện và điểm dừng

| Mốc | Công việc cụ thể | Bằng chứng bắt buộc trước khi báo xong |
|---|---|---|
| Rà soát tiếp quản | Đọc source/PDF/docs, git status/log; compile và test nền, ghi phần M5 đã có/còn thiếu; rà khả năng đóng gói và quyền network hiện tại | Ma trận yêu cầu→file/hàm/test→trạng thái; giữ nguyên data/M5 dở; báo PASS/FAIL hiện tại, không dựa log cũ |
| M5 | Hoàn thiện backend và 5 tab admin Swing; user/room CRUD, guard, mute/ban/kick/spam, broadcast, audit, snapshot CSV/HTML và tải ZIP UDP; Server giám sát/report | SelfTest + AdminSelfTest PASS; số liệu biết trước khớp từng bảng, injection/BOM/rotation/role giả mạo; ảnh đủ tab/form/mute/kick/broadcast/report |
| M6 | Khảo sát UI chính thức 2026, thiết kế trước; Theme sáng/tối, layout/forms/toast/composer/sidebar/header/member/table, HiDPI/co giãn/keyboard; kiểm tra card, nhớ IP và UI chẩn đoán; smoke portable sớm | Tài liệu khảo sát và design tokens; ảnh từng màn sáng/tối/form lỗi/offline ở kích thước/DPI đã thử; thao tác không treo; app-image mở và nạp FlatLaf/resource được |
| M3c | Tìm kiếm, trả lời/trích dẫn, gộp tin liên tiếp, phân cách ngày, tắt thông báo từng phòng, xuất lịch sử .txt; bổ sung mời/thêm thành viên trong app và biệt danh riêng từng nhóm; giữ menu quyền và scroll | Tests search/history/phạm vi kết quả, quote/recall không lộ nội dung, grouping/day đúng, export quyền/UTF-8; mời/nhận/từ chối/ban/full/offline/restart, nickname riêng hai nhóm và quyền; ảnh và kịch bản thao tác thực |
| M7 | Rà soát source/tài nguyên/session/socket/luồng/temp/cache/input/atomic write, loại mã chết; chốt script portable/ZIP/hướng dẫn/VS Code/NetBeans; full regression và hai máy thật | Compile sạch, toàn bộ tests PASS, UiPreview và xem ảnh; portable không cần Java/IDE trên máy đích; LAN 5 MB CRC và chat trực tiếp multicast; báo cáo rà soát/file mapping/demo đầy đủ |

Giữ tên commit các mốc cũ: `M5 xong`, `M6 xong`, `M3c xong`, `M7 xong`, chỉ sau khi đủ tiêu chí. Mỗi mốc dừng xác nhận; nếu tách mốc phụ phải định nghĩa rõ trước khi làm. Không tự commit checkpoint thành mốc đã hoàn tất. Không tự push remote.

## 3. Cổng nghiệm thu giao diện: không chỉ “đổi Look & Feel”

Trước thay đổi UI đại trà ở M6, đưa bản thiết kế và ảnh mẫu từ component thật để người dùng nhận xét. Mọi màn áp dụng cùng Theme; không pha nhiều phong cách.

| Thành phần | Phải đạt |
|---|---|
| Đăng nhập/đăng ký | Thẻ căn giữa, tab nhẹ, Hiện mật khẩu, Enter, loading, lỗi đúng ô và input không nén; cấu hình Server gọn |
| Sidebar | Hai nhóm phòng, lọc, tên/preview/time/unread, join rõ nút, menu⋮ đúng quyền; tài khoản 14 đậm/role12 xám |
| Header/member | Mã/copy và thành viên không cắt chữ, thu gọn được; role/online/offline/Bạn rõ và đúng danh tính |
| Composer | Vùng nhập toàn chiều rộng, hàng icon/send riêng, một dòng gọn, lớn tối đa5 dòng; nền disabled bo tròn đồng nhất, không thanh cuộn thừa |
| Tin nhắn | Font PLAIN, tên rõ, avatar đầu dòng, sticker PNG đẹp, file card có preview/progress; group/day/reply/search nhất quán sau M3c |
| Thông báo | Toast gọn không che composer, hover pause; không toast từng tin; banner reconnect có trạng thái đúng |
| Forms | HopThoai chung, lỗi theo key đúng ô, gợi ý ẩn khi lỗi, password mode rõ, Esc/Enter đúng, confirm nguy hiểm đỏ |
| Quản trị | 5 tab gạch chân, JTable36px, hàng xen nhẹ/sort/filter/status pills, biểu đồ đọc được, toolbar không tràn |
| Sáng/tối và DPI | Không màu chữ mất tương phản, icon/nền/viền đồng bộ; ảnh100/125/150% nếu môi trường hỗ trợ, ghi rõ mức chưa thử |
| Trạng thái | Loading/empty/error/retry/disabled/focus/hover cho mỗi thành phần; không nút giả hay chữ “M3/M6” trong menu |

Bộ ảnh đích: login/register bình thường+lỗi; profile có/chưa ảnh; đổi/bắt buộc đổi password; tạo/sửa/vào/info phòng; chat thường/sticker/typing/retry/recall/reply/search/day/group; file chờ/gửi/tải/xong/lỗi/hủy/preview; mute/kick/broadcast/offline;5tabadmin và report; sáng/tối/960×600/cửa sổ lớn. UiPreview giữ assertions hình học/font. Chưa thử chuột/phím không được mô tả là UI đã nghiệm thu tương tác.

## 4. Hành trình hai máy và bản bàn giao portable

### Bộ ứng dụng

```text
UDPChatClient-<Windows-CPU>.zip
  UDPChatClient/
    UDPChatClient.exe
    app/ ...dependencies/resources...
    runtime/ ...Java đi kèm...
    HUONG_DAN.txt

UDPChatServer-<Windows-CPU>.zip
  UDPChatServer/
    UDPChatServer.exe
    app/ ...dependencies/resources...
    runtime/ ...Java đi kèm...
    HUONG_DAN.txt
```

Tên/cấu trúc cuối phải theo jpackage thực tế, không sửa bằng tay thư mục runtime. Source và build script vẫn giữ ở project Maven. Build Windows trên môi trường tương ứng; không hứa tương thích OS/CPU chưa build/test. Không đóng gói dữ liệu hoặc mật khẩu thật.

Script dự kiến: `scripts/build-portable.ps1`, kiểm JDK21/jpackage/Maven, compile/package/copy dependencies và resources vào staging riêng, tạo hai app-image với main tương ứng, ZIP toàn thư mục. Không phá target source hoặc dùng recursive delete trên đường dẫn chưa xác minh. Bản app-image không bắt người nhận cài JRE, không phải installer .msi bắt buộc.

Client config, data/log/report/temp dùng đường dẫn writable rõ, không dựa CWD, không viết vào runtime. Bấm đúp ở đường dẫn có tiếng Việt/khoảng trắng và khởi động lại phải vẫn nạp dữ liệu/cấu hình. Có profile khi mở nhiều Client, tránh ghi chồng cùng file.

### Năm bước người dùng

1. Cả hai máy nối cùng mạng tin cậy có multicast, tránh guest Wi-Fi/AP isolation. Giải nén toàn bộ gói.
2. Máy A mở Server, Bắt đầu, Sao chép IP; mở thêm Client A nếu cần.
3. Máy B mở Client, nhập IP LAN máy A lần đầu, Kiểm tra kết nối; lần sau nhớ IP. Không nhập localhost để trỏ máy A.
4. Đăng ký/login hai tài khoản, A tạo phòng và B vào bằng mã/danh sách; gửi tiếng Việt, sticker, avatar, tệp5MB và kiểm CRC.
5. Thử mute/kick/broadcast/report, đăng xuất/restart, xác nhận dữ liệu còn. Ghi ảnh, log đã che bí mật và kết quả kiểm tra theo checklist.

Nút Tìm máy chủ có thể giảm nhập IP nếu làm đơn giản, không phải lý do bỏ lối IP thủ công. Discovery phải tách group/cổng, không xung đột room, timeout rõ và người dùng tự chọn; không gửi credential tự động cho server lạ.

### Chẩn đoán có thứ tự

- UDP HELLO không phản hồi: Server chưa Bắt đầu, IP/card sai, khác subnet hoặc firewall. Ping chỉ là kiểm tra bổ trợ.
- Login được nhưng không có tin live: kiểm joinGroup/interface/loopback/roomId, multicast router/AP filtering và firewall cổng phòng; không âm thầm thay bằng unicast để báo đạt.
- Chat được nhưng file lỗi: kênh UDP cấp động, quyền lưu, max size, disk/CRC, timeout; hướng dẫn bằng ngôn ngữ dễ hiểu.
- Portable không mở: giải nén đầy đủ, đúng Windows/CPU, đường dẫn writable, log khởi động, Windows/SmartScreen/policy. Không khuyên tắt bảo vệ.
- Firewall kiểm với launcher/runtime portable thực tế trên profile mạng phù hợp; cho app Java trong IDE không thay việc thử bản đóng gói.

## 5. Kiểm thử và bảng bằng chứng

| Nhóm | Nội dung bắt buộc |
|---|---|
| Hồi quy | Toàn bộ SelfTest và AdminSelfTest, không sử dụng số cũ làm bằng chứng mới |
| Đúng multicast | Server gửi một datagram/nhóm, nhiều Client nhận; chặn multicast giữ ACK để test trạng thái; phân biệt RESYNC bù |
| Phòng/danh tính | 3Client2phòng, không lẫn tin, private không lộ, capacity/password/owner, Client cuối đúng1Bạn |
| Hồ sơ/chat | Avatar CRC/loss20%, đổi mật khẩu/default/temp; sticker/typing/recall/dedup/retry và M3c |
| Tệp | Byte/CRC hai chiều, loss20%seed, trùng/đảo,0/>max/cancel/timeout/persist/rights/path/concurrent/chat latency |
| Admin/report | Guards giả mạo, self/lastadmin, lock session, mute expiry/ban persist, spam/audit, CSV/HTML known counts và escape |
| Tài nguyên | Login/logout/rời phòng/server restart/mất mạng, socket/thread/queue/cache/temp về mức nền, không deadlock |
| Portable | Máy không Java/Maven/IDE, doubleclick/resources/data/preferences/profile, đường dẫn dấu/khoảng trắng, đúngarch |
| LAN thật | Hai máy vật lý cùng mạng, text/sticker/avatar/file5MB/quyền/report/restart, tốc độ và bằng chứng multicast |
| UI | Ảnh component thật và thao tác thật, sáng/tối/DPI/kích thước/form lỗi/font/plain/scroll/toast/menu |

Nếu không có máy thứ hai hoặc chỉ chạy VM/localhost, ghi rõ “chưa kiểm chứng hai máy vật lý” và để người dùng chạy checklist. Không chứng nhận toàn bộ đã đạt trước bằng chứng.

## 6. Tài liệu, nguồn và tiêu chí xong

- README: build/run bằng Maven/VS Code/NetBeans và cách dùng portable.
- docs/KHAO_SAT_CHAT_2026.md: nguồn chính thức, ngày, pattern chọn/bỏ và lý do; không bịa “mới nhất”.
- docs/THIET_KE_GIAO_DIEN.md: Theme tokens, sáng/tối, bố cục, states, danh sách ảnh đã duyệt.
- docs/CHAY_HAI_MAY.md: năm bước, topology, firewall/card/UDP probes, checklist thật.
- docs/DONG_GOI_PORTABLE.md: build, runtime, dependency/license, paths/profiles, test không có Java.
- docs/GIAI_THICH.md: toàn bộ yêu cầu→chức năng→file/hàm→test, UDP/multicast từng đoạn và demo5phút.
- docs/BAO_CAO_RA_SOAT.md: file tree/đổi-xóa, multi-thread/resource/input/cache/atomicwrite, kết quả test/ảnh/LAN/portable và giới hạn.

Nguồn khởi đầu: [FlatLaf](https://www.formdev.com/flatlaf/), [Oracle jpackage21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html), [App image](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html), [Discord search](https://support.discord.com/hc/en-us/articles/115000468588-How-to-Use-Search-on-Discord), [Zalo PC](https://www.zalo.me/pc). Khảo sát tiếp khi thực hiện M6, không tự nâng dependency chỉ vì có bản mới.

Cuối mỗi mốc: đã làm gì, file mới/đổi, lệnh compile/run/test, kết quả thật, 3–5 bước thử tay, điều chưa kiểm chứng; commit local chỉ khi đủ điều kiện, dừng xác nhận. Bản hiện tại mới là kế hoạch; chưa tuyên bố UI mới hay portable đã hoàn thành.
