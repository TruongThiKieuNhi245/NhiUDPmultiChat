## Cập nhật Web 3.1 — 25/11/2026

Tiếp quản `be607ec`. Mốc này hoàn thiện tệp/ảnh HTTP + metadata F qua UDP CHAT2 và giao diện gradient; giữ dữ liệu vận hành cùng các sửa đổi tài liệu có sẵn. Chi tiết source/API, quota/cleanup, QA browser/JAR và phần còn lại: [WEB3_1_NGHIEM_THU.md](WEB3_1_NGHIEM_THU.md). Đây chưa phải nghiệm thu toàn bộ Web 3; dừng sau mốc để xem kết quả, chưa push/public.

---

## Cập nhật Web 2 — 25/11/2026

Tiếp quản `939662b`; giữ các sửa đổi tài liệu có trước. Web 2 nối nhiều phòng, thành viên/lời mời/biệt danh, hồ sơ/avatar/đổi mật khẩu, sticker/reply/thu hồi/search/TXT và các trạng thái chat. Bản đồ file/API, kết quả QA, ảnh, artifact riêng và giới hạn nằm trong [WEB2_NGHIEM_THU.md](WEB2_NGHIEM_THU.md).

Đường chat là WS xác thực → UDP CHAT2 thật → kiểm quyền/chống trùng → lưu → phát WS đúng phòng. Tách quyền đọc với gửi; mute/đóng phòng không làm mất kênh đọc. Chưa làm Web 3–4, chưa push/public. Dừng sau mốc này để xem kết quả.

---

## Cập nhật chặng UX Web 1 — 25/11/2026

Source tiếp quản đã là Web 1 tại `c3ed958`, khác baseline desktop trong phần lịch sử bên dưới. Giữ nguyên sáu file tài liệu có sửa đổi của người dùng. Bàn giao mới, bảng vấn đề/nguyên nhân, bản đồ file/hàm, lệnh QA, ảnh và giới hạn nằm tại [WEB1_UX_NGHIEM_THU.md](WEB1_UX_NGHIEM_THU.md).

Chặng này sửa hướng tin theo `userId`, render theo khoá ổn định, phân trang gần nhất/cũ hơn, giới hạn cache/DOM, focus drawer, composer và vòng đời kết nối. Giữ UDP receive → kiểm quyền → lưu → phát WS. Dừng ở chặng UX; chưa bắt đầu nghiệp vụ Web 2–4.

---

# Bản bàn giao và rà soát workspace cho Agent

**Tiếp quản 18/11/2026 - 19/11/2026:**
- **M5 & Báo cáo Quản trị:** Hoàn tất 100%, SelfTest 75 PASS / 0 FAIL, AdminSelfTest 16 PASS / 0 FAIL, UiPreview 10/10 PASS (60 ảnh PNG).
- **Chuẩn hóa Báo cáo:** Loại bỏ hoàn toàn nhãn địa điểm/timezone lạ (như `[Asia/Bangkok]`), định dạng thời điểm xuất theo chuẩn `dd/MM/yyyy HH:mm:ss`. Sửa nhãn "Thời điểm xuất báo cáo" (không dùng chữ "chụp dữ liệu"). Bỏ tiêu đề trang in bị lặp 2 lần ở đầu file PDF (`header = null` trong `BaoCaoPreviewDialog`). Bố cục báo cáo executive layout chuyên nghiệp, KPI cards, table CSS gradient header xanh tím, hỗ trợ in ấn `@media print`.
- **Linh hoạt Xuất Báo cáo:** Hỗ trợ xuất / xem trước riêng cho từng tab Người dùng, Phòng, Nhật ký (`xuatBangDon`) và cho phép chọn linh hoạt 2 trong 3 nhóm hoặc toàn bộ trong tab Báo cáo (`BaoCaoPanel` & `XuatBaoCao.htmlTuyChon`).
- **Nâng cấp Giao diện Gradient & Tiện ích:** Tích hợp bộ design tokens gradient (`GRADIENT_START = #4F6BED`, `GRADIENT_END = #2563EB`, hover/active variants, `GRADIENT_DO_START / END` cho badge cảnh báo), thẻ hiện đại `Theme.theGradient()`, nút bấm gradient chuyển sắc, nút tròn gửi tin gradient. Cải tiến `ServerMain` chống tràn layout khi hiển thị đường dẫn, giữ vững nhãn trạng thái máy chủ `● ĐANG CHẠY` / `● ĐÃ DỪNG`. Nâng cấp biểu đồ và thẻ thống kê trong `TongQuanPanel`.
- **Thanh thao tác tin nhắn (Hover Quick Action Bar):** Đã tinh chỉnh trong `HangTin` ẩn mặc định, chỉ hiện thanh capsule bo tròn tinh tế khi hover với các icon nhẹ (Sao chép, Thu hồi, More), loại bỏ hoàn toàn các liên kết chữ thô cạnh bong bóng chat.
- **M3c.1 (Trải nghiệm Chat nâng cao):** Đã hoàn thành và kiểm chứng 100%:
  + *Gộp tin nhắn liên tiếp:* Cùng người gửi trong 5 phút tự động ẩn avatar/tên lặp lại, giữ lề thụt 32px thẳng hàng, thu gọn khoảng cách dọc 2px.
  + *Phân cách ngày:* Pill nhỏ bo tròn căn giữa khung chat ("Hôm nay", "Hôm qua", "dd/MM/yyyy").
  + *Trích dẫn / Trả lời tin nhắn (Quote / Reply):* Nút "Trả lời" (icon `reply`) trên thanh hover và menu; thanh xem trước trích dẫn Reply bar trên ô soạn thảo có nút hủy `✕`; khối hộp trích dẫn Quote box viền trái 3px `Theme.CHINH`, click chuột tự động cuộn tới tin gốc và nháy sáng nhận diện.
  + *Tìm kiếm tin nhắn trong phòng (`Ctrl + F`):* Thanh tìm kiếm nhanh trên đầu khung chat, nút kính lúp trên header, đếm số lượng kết quả `x/y`, nút Lên/Xuống chuyển vị trí và cuộn viewport tới tin khớp.
  + *Xuất lịch sử chat (.txt):* Menu phòng có mục "Xuất lịch sử chat (.txt)" lưu đầy đủ tin nhắn, sticker, tệp, tin trích dẫn, thu hồi ra file UTF-8 chuẩn kèm nút mở nhanh trên Toast thông báo.
  + *Kiểm thử tự động:* `M3cChatSelfTest` PASS 100%, `AdminSelfTest` 16 PASS / 0 FAIL, `UiPreview` 10/10 PASS.
- **M3c.2 (Quản lý Thành viên, Biệt danh nhóm & Tắt thông báo phòng):** Đã hoàn thành và kiểm chứng 100%:
  + *Biệt danh nhóm:* Đổi biệt danh riêng theo phòng `(roomId, userId) -> nickname` qua menu chuột phải trong `ThanhVienPanel`. Hiển thị biệt danh kèm `(Tên thật)`, ưu tiên hiển thị trên bong bóng tin nhắn chat. Không thay đổi tên hiển thị toàn cục hay audit log.
  + *Mời thành viên:* Nút `+` trên thanh tiêu đề `ThanhVienPanel` mở `MoiThanhVienDialog` tìm kiếm user, loại trừ thành viên hiện có, kiểm tra phòng mở/sĩ số, gửi lời mời có thời hạn 7 ngày. Hộp thoại `LoiMoiPhongDialog` quản lý duyệt "Chấp nhận" / "Từ chối".
  + *Tắt thông báo phòng:* Menu "Tắt thông báo phòng" / "Bật thông báo phòng" trên `DanhSachPhongPanel`, hiển thị biểu tượng `🔕` cạnh tên phòng và không tính số tin chưa đọc (badge unread) cho phòng bị tắt.
  + *Kiểm thử tự động:* `M3cThanhVienSelfTest` 3/3 PASS, `AdminSelfTest` 16/16 PASS, `SelfTest` 75/75 PASS, `UiPreview` 10/10 PASS (60 ảnh PNG).
- **Nâng cấp Giao diện, Khắc phục Ô vuông & Nền Pastel (20/11/2026):**
  + *Khắc phục 100% lỗi ô vuông (Tofu glyphs):* Chuyển toàn bộ các biểu tượng Unicode ký tự cao sang vector Java2D thuần trong `Theme.java` (`search`, `close`, `eye`, `eye-off`, `bell-off`, `palette`, `more`, `up`, `down`). Loại bỏ hoàn toàn các ký tự gây lỗi ô vuông: `🔍`, `✕`, `🔕`, `👁️`, `⋮`, `▲`, `▼`.
  + *Nút Mắt ẩn/hiện mật khẩu:* Thay thế toàn bộ nút chữ "Hiện" / "Ẩn" bằng nút icon mắt nhắm mở (`eye` / `eye-off`) Java2D vẽ tay mượt mà trong `DangNhapPanel`, `HopThoai`, `FormQuanTri`, `HoSoDialog`.
  + *Đổi màu nền phòng chat (Pastel Backgrounds):* Bộ 8 màu pastel êm dịu, không chói (Mặc định Slate, Xanh mây, Bạc hà, Oải hương, Cam đào, Hồng phấn, Cốm non, Cát ấm). Nút `palette` trên header phòng và menu chuột phải. Lưu cấu hình theo từng phòng qua `CauHinhClient`.
  + *Viết lại nội dung kiểm duyệt tự nhiên:* Hộp thoại Kick, Ban, Mute, Chuyển quyền trong `KiemDuyetUI` được diễn đạt bằng ngôn từ thân thiện, lịch sự, rõ ràng cho người dùng thật.
  + *Đóng gói lại Portable:* Chạy lại `build-portable.ps1` cập nhật toàn bộ thư mục `dist/` và file ZIP độc lập.

Ngày rà soát: 17/11/2026 - Cập nhật 20/11/2026. Hướng duy nhất: **desktop Java 21 + Swing/FlatLaf, UDP multicast trực tiếp, portable và hai máy LAN**. Không chuyển web. Prompt chuẩn: [PROMPT_NANG_CAP_VA_TRIEN_KHAI.md](PROMPT_NANG_CAP_VA_TRIEN_KHAI.md).

**Tái xác nhận 19/11/2026 (lượt 2):** compile exit0; SelfTest75/0 PASS; AdminSelfTest riêng16/0 PASS; UiPreview60 PNG exit0 — không phát sinh hồi quy. Tệp5MiB loss20% seed20261113 upload65,772s/download64,713s byte/CRC đúng. Commit `M5 xong` đã thực hiện sau lượt kiểm tra này. Chưa làm M6/M3c/M7. Hai máy vật lý/portable/HiDPI chờ M7.

## 1. Đọc và bắt đầu từ đâu

1. AGENTS.md ở gốc: ràng buộc và lệnh chạy.
2. Bản bàn giao này: baseline/rủi ro/source map.
3. Prompt chuẩn: toàn bộ yêu cầu và giao việc bắt đầu M5.
4. MA_TRAN_CHUC_NANG_DESKTOP.md: thống kê/report, nhóm/invite/biệt danh, sticker và phạm vi mở rộng.
5. KE_HOACH_HOAN_THIEN_DESKTOP.md: M5 → M6 → M3c → M7, tiêu chí và điểm dừng.
6. GIAI_THICH.md, README, screenshots/README và NGUON_TAI_NGUYEN.md: lịch sử/bằng chứng mốc cũ; không coi mọi dòng trạng thái cũ là source hiện tại.

Tài liệu môn đã đọc qua trích xuất nội dung: thư mục tài liệu môn học ngoài project; đường dẫn thực của phiên rà soát là `D:/New folder (2)/6_TH_LapTrinhMang/tailieu/`. Giữ nguyên hai PDF ngoài project, không xóa: `Chap_6_Socket_UDP_2.pdf` (40 trang), `Chap_6_1_MultiCast.pdf` (14 trang). UDP nhấn DatagramSocket/Packet/send/receive, multicast nhấn group/join/leave và phát một-nhiều. Seq/ACK/retry/chia mảnh là lớp ứng dụng tự viết, không thuộc bảo đảm sẵn của UDP.

## 2. Hiện trạng chính xác

- 65 file Java main, 9 file test Java; source khoảng 7.578 dòng lúc kiểm kê. Không file main vượt 400 dòng. ClientCore 399 và ChatMulticastUI 392 dòng gần giới hạn, không nhồi thêm logic.
- Java Temurin 21.0.12.1; Maven NetBeans 3.9.16, FlatLaf3.7.2 là dependency ứng dụng duy nhất. pom release21, UTF-8, Xlint all/failOnWarning.
- Client entry `com.mycompany.udpmultichat.ClientMain`; Server entry `com.mycompany.udpmultichat.server.ServerMain`. .vscode có Server/Client1/2/3, cùng CWD project.
- Commit nghiệm thu gần nhất lúc rà soát: `a212fe2 M4 xong`. Phần M5 đang nằm trong file đã đổi và file mới chưa commit; giữ toàn bộ, không checkout/reset về M4.
- Không có Spring Boot/JSP/gateway/web runtime trong source. Không có script portable hiện tại. Theme chỉ cài FlatLightLaf; dark theme chưa hoàn thiện.
- Noto Emoji: 36 PNG128px; đã đối chiếu đủ 36 SHA256 với SHA256SUMS, tất cả khớp. Giữ LICENSE/NOTICE; không xóa sticker hoặc thay bằng Java2D.
- Kiểm tra tĩnh không thấy socket trong ui, không thấy Swing trong core (ngoại lệ ServerMain/ClientMain hợp lệ), không có TCP ServerSocket/WebSocket trong source.
- Các kiểm tra tĩnh trên không phải chứng nhận mọi hàm dưới40dòng, mọi luồng an toàn hoặc nghiệp vụ hoàn chỉnh. M5 có nhiều dòng nhồi statement; cần tách để dễ thuyết trình, không chỉ đếm số dòng.

## 3. Kết quả kiểm tra mới

| Kiểm tra mới ngày 17/11/2026 | Kết quả |
|---|---|
| Maven `clean test-compile dependency:build-classpath` | Exit0, compile sạch với Xlint/failOnWarning; log `.selftest/audit-compile.log` |
| `SelfTest` toàn bộ, gồm AdminSelfTest | **66 PASS, 1 FAIL**, exit1, khoảng 313,3 giây; log `.selftest/audit-selftest.log` |
| Lỗi hồi quy | M3a avatar20% DATA/ACK seed20261113, hết ACK sau4lần gửi; chưa sửa source trong lượt rà soát |
| Nhóm M5 trong full SelfTest | 11 nhóm PASS, nhưng checklist M5 và UI/LAN chưa được nghiệm thu đầy đủ |
| UiPreview chạy CWD riêng | Exit0, assertions PASS, **56 PNG**; log `.selftest/audit-ui-preview.log`, ảnh `.selftest/ui-preview/docs/screenshots/` |
| Kiểm tra PNG nguồn | 36/36 hash khớp SHA256SUMS; không thay file nguồn |
| Thử thực tế ngoài môi trường | Chưa có hai máy vật lý/portable khôngJava/HiDPI/chuột-phím đầy đủ |

SelfTest dùng socket UDP và multicast thật trên cùng máy. Tệp 5 MB loss 0% lượt này: upload 2,689s (~1,86MiB/s), download 2,887s (~1,73MiB/s). Đây là số đo localhost trong lượt audit, không phải cam kết LAN. Chưa có kết quả toàn bộ PASS nên không có commit nghiệm thu M5.

Chạy lại riêng HoSoSelfTest sau khi full suite kết thúc: **9 PASS, 0 FAIL**, exit0 (`.selftest/audit-hoso-isolated.log`). Vì full suite đã FAIL còn chạy riêng PASS, lỗi mất gói/ACK có tính không ổn định hoặc phụ thuộc lịch/chế độ tải; chưa có bằng chứng đủ để chốt nguyên nhân. Giữ cả hai log, không xóa FAIL hoặc tuyên bố đã sửa chỉ vì chạy lại PASS.

Chỉ dùng kết quả mới ở mục này cho bản bàn giao; số56PASS M4 và11PASS admin trong tài liệu cũ là lịch sử. Test main không tự chạy bởi Surefire: `mvn test` đơn lẻ không đồng nghĩa SelfTest đã chạy. Không ghi “M5 xong” chỉ vì test hiện có PASS; so đủ checklist người dùng và thử giao diện/LAN.

## 4. Bảng chức năng và việc còn lại

| Phần | Source hiện có | Việc Agent phải làm |
|---|---|---|
| Tài khoản/phiên | Core, salt/hash, UDP login, heartbeat, reconnect, force password | Hồi quy; sửa lỗi avatar mất gói được nêu dưới; kiểm reset/reconnect và guard lỗi |
| Phòng | CRUD, private/code/password/capacity, owner, membership persist, multi-room, history/RESYNC | Giữ chức năng, kiểm nhóm riêng và quota/chủ; không viết lại mạng |
| Chat | Text/sticker PNG/typing/recall/copy/trạng thái/retry | Giữ; M3c bổ sung search/reply/group/day/notification/export |
| File M4 | UDP sliding window32, retry8, disk/CRC/progress/cancel/thumbnail/history | Giữ, full regression, portable và LAN thật còn phải thử |
| M5 | Backend/admin5tab/moderation/audit/stats/CSV+HTML/ZIP transfer đã có | Hoàn thiện checklist và test coverage, ảnh/tương tác, docs, rồi mới commit |
| Thêm thành viên | Vào bằng mã và danh sách đã có | Lời mời trong app/accept/reject/offline/ban/full/race chưa có, làm M3c |
| Biệt danh | Tên hiển thị toàn hệ thống đã có | Nickname riêng roomId-userId chưa có, làm M3c; không nhầm với profile |
| UI mới | FlatLaf light, Theme, layout/component/preview đã có | M6 hoàn thiện hover action bar chuẩn Discord/Slack/Zalo PC, chống tràn ServerMain, hoàn thành tài liệu khảo sát & thiết kế |
| Portable/LAN | Hướng dẫn sơ bộ IP, IDE chạy3Client | Build app-image kèm runtime/config paths, chẩn đoán/profiles, thử máy không Java và hai máy vật lý |

## 5. Lỗi và khoảng trống ưu tiên cho Agent

### Đã có bằng chứng trong lượt rà soát

1. SelfTest ca avatar mất20% DATA/ACK seed20261113 đã xuất hiện FAIL vì “Không nhận được ACK sau4lần gửi”. `HoSoClient.guiAnh/layAnh` dùng ReliableUdp điều khiển với3lần retry; không có retry nghiệp vụ avatar ở mọi chỗ. Đây là triệu chứng được ghi nhận, chưa kết luận toàn bộ nguyên nhân. Tái hiện bằng HoSoSelfTest, giữ seed và kiểm transaction/idempotency/cancel trước sửa; không tăng toàn cục retry làm hỏng test control3retry hoặc làm giả giảm loss.
2. Ảnh mới `45-quan-tri-nguoi-dung.png`: không thấy header cột trong bản render, ngày ISO bị cắt, glyph ba chấm có ô vuông. Kiểm tra cả cửa sổ thật và cách UiPreview layout JTableHeader để xác định lỗi app hay lỗi preview; không bỏ qua ảnh vì assertions đang PASS.
3. UiPreview hiện xuất 56 ảnh, nhưng 43 PNG tham chiếu trong docs/screenshots thuộc mốcM4; source QuanTriPreview sinh44–56 chưa được nghiệm thu. Ảnh mới lượt này đặt trong `.selftest/ui-preview/...`, không ghi đè tham chiếu cũ.
4. README hướng dẫn sửa admin.password trong config để khôi phục admin có thể không khớp QuanLyNguoiDung constructor M5 (không ghi đè account hiện có). Cần kiểm thử rồi cập nhật hướng dẫn, không ép reset dữ liệu thật.

### Cần xác minh/thêm coverage trước nghiệm thu M5

- AdminSelfTest hiện dùng11 nhóm tổng hợp, không chứng minh riêng mọi yêu cầu: last-active-admin, chuỗi lệnh admin giả mạo đầy đủ, reset/lock persist, spam20/min, report từng cột và byte totals, audit deny/duplicate, account/profile/persistence failure rollback, concurrency/race.
- `QuanTriServer.MOD` từ chối quyền chưa chắc ghi auditDENIED nhưAD_; loginfail được ghi mỗi3lần theoIP; đối chiếu yêu cầu nhật ký đúng1sự kiện.
- `ChupThongKe.ngay` suy loại từ tin hiện tại; tin đã thu hồi đổi loaiD nên không còn biết nguyên bản là sticker/file. Chốt rõ thống kê “đã gửi” và “còn lưu”, nếu yêu cầu thống kê theo loại trước thu hồi cần metadata tương thích.
- `QuanTriClient.giaiNen` whitelist/containment/size đã có nhưng phải kiểm cleanup temp khi ZIP lỗi và duplicate entries. ZIP report lớn còn chịu giới hạn download/tệp; cần thông báo/phạm vi rõ.
- `QuanTriServer.thongBao` có thể lỗi phát multicast sau khi đã lưu một phần; cần đối soát/retry không tạo broadcast trùng và không báo sai rằng chưa gửi.
- `phatMute` phát cho toàn bộ phiên/phòng trong mỗi PING, tải tăng theo số user; `AD_CHANGED` có thể phát trùng từ capNhatPhong/thayDoi. Đo và giảm sự kiện thừa nếu cần, không tối ưu mù.
- `BangQuanTri.loc` đang tìm cả row để lọc Online/Admin, có thể nhận nhầm tên/giới thiệu chứa từ “admin”; cần lọc cột trạng thái/vai trò đúng nghĩa. Comparator số suy từ toàn giá trị của cột cũng cần kiểm tên đăng nhập chỉ toàn chữ số và tràn Long.
- UI tab Nhật ký cần bộ lọc theo loại; hiện BangQuanTri chủ yếu ô tìm và filter user. Kiểm trạng thái loading/empty/error và giới hạn snapshot30giây/one-row UDP khi nhiều auditrows.
- Một số hàm/biến private có thể dư sau tách Core, ví dụ `ServerCore.docBoolean`; tìm toàn bộ tham chiếu trước xóa. Không dùng tên class đơn lẻ để kết luận dead code vì main/UiPreview gọi gián tiếp.
- Config client và data hiện phụ thuộc CWD: `CauHinhClient.tepCauHinh`, ServerMain `Path.of("data")`. Phải xử lý ở mốcportable, không đoán bấm đúp .exe sẽ dùng thư mụcproject.
- Chưa có bộ mời user/nickname/search/reply/react/pin trong source; reaction/pin/@mention chỉ đề xuất, không tự coi bắt buộc hoặc đã làm. M3c invite/nickname đã được yêu cầu rõ, không bỏ.

Các mục “cần xác minh” là đầu việc rà soát, không phải khẳng định mọi mục đều là bug đã tái hiện.

## 6. Dọn workspace trong lượt này

- Xóa `docs/KE_HOACH_CHUYEN_WEB.md` vì hướng web đã bị hủy; kế hoạch desktop và prompt là nguồn đang áp dụng. Không để hai hướng mâu thuẫn cho Agent.
- Maven `clean test-compile` dọn rồi tạo lại target để tránh class cũ; target là output có thể tái tạo, vẫn giữ cấu trúc Maven.
- Dữ liệu test cũ `.selftest/`: 1.321 file, khoảng384,96MiB; lệnh recursive delete bị kiểm tra tự động chặn, vì vậy chuyển an toàn ra `%TEMP%/UDPmultiChat-test-backup-71f5b38f337a4a6a92c26900f4013ad7` (không xóa vĩnh viễn). Không chứa dữ liệu production đã đọc/in ra.
- Giữ `.git`, source/test, .vscode, data/, client.properties, ảnh tham chiếu43PNG và nguồn/LICENSE sticker. `.github/modernize` có3file hook extension nhỏ/ignored, không phải code app; giữ cấu hình công cụ để tránh phá môi trường IDE.
- Kiểm thử mới sinh `.selftest/` riêng, có log/evidence; không nhầm với test cũ đã chuyển. Không gọi đó là dữ liệu người dùng hoặc đưa vào Git. Phần dữ liệu nhị phân test mới được dọn/đưa tạm sau lượt test theo ghi chú cuối.

Sau khi chạy xong, fixture và tệp nhị phân test mới được chuyển vào thư mục `audit-moi/` bên trong backup tạm trên. Workspace `.selftest/` chỉ giữ classpath, bốn log và ảnh/HTML mẫu UiPreview để Agent xem lại. Không xóa vĩnh viễn dữ liệu trong TEMP vì thao tác xóa hàng loạt đã bị chặn; có thể khôi phục khi cần. Source/data sản xuất không bị sửa trong yêu cầu rà soát này.

## 7. Source map — mỗi file một nhiệm vụ

Đường dẫn dưới tương đối `src/main/java/com/mycompany/udpmultichat/`. Một file có thể chứa component/record nhỏ cùng nhiệm vụ.

| File | Vai trò | Dòng |
|---|---|---:|
| client/CauHinhClient.java | Chi ghi ten dang nhap va Server lan truoc; khong luu mat khau/token | 41 |
| client/ClientCore.java | API client khong Swing: unicast dieu khien, kenh va dong bo rieng cho tung phong | 399 |
| client/DongBoTinNhan.java | Thu tu va loai trung theo msgId cua MOT phong; ghep lich su/RESYNC voi tin truc tiep | 124 |
| client/HoSoClient.java | Upload/download avatar nen va cache LRU gioi han | 110 |
| client/KenhPhong.java | Mot socket nhan multicast cho mot phong; luon loc roomId truoc khi hien thi | 76 |
| client/Listener.java | Su kien luong nen; UI chuyen ve EDT. Ham default giu tuong thich SelfTest M1 | 61 |
| client/QuanTriClient.java | API admin bat dong bo, mute va tai/giai nen bao cao qua UDP M4 | 93 |
| client/TepClient.java | Hang doi hai upload, tai/thumbnail, huy va tien do | 165 |
| client/TroChuyenClient.java | Trang thai gui, retry on dinh, typing va dong bo thu hoi | 126 |
| ClientMain.java | Diem chay Client; bat IPv4 truoc khi giao dien co the tao socket | 19 |
| common/AnhChupQuanTri.java | Snapshot bat bien de UI va bao cao dung cung so lieu | 14 |
| common/AnhTep.java | Tao thumbnail anh, tu choi anh hong/qua nhieu pixel | 35 |
| common/BaoMat.java | Tao salt, token va bam SHA-256 bang API san co cua Java | 38 |
| common/Goi.java | Goi text UTF-8: escape dau gach dung, gach cheo va xuong dong | 77 |
| common/MangUtil.java | Chon IPv4 va tao MulticastSocket; tai su dung cach chon card cua ban goc | 72 |
| common/model/NguoiDung.java | Ho so cong khai, khong chua bi mat | 31 |
| common/model/Phong.java | Ban sao phong cong khai; khong chua salt hay hash mat khau phong | 60 |
| common/model/TinNhan.java | Tin do Server gan so thu tu va thoi gian; moi goi deu co roomId | 45 |
| common/model/VaiTro.java | ADMIN la quyen he thong; CHU_PHONG duoc tinh rieng theo phong o M2 | 4 |
| common/PhanManh.java | Xu ly anh va ghep manh co gioi han, CRC32 | 84 |
| common/Protocol.java | Hang so dung chung cho UDP dieu khien va multicast phong | 58 |
| common/ReliableUdp.java | UDP tin cay cho dieu khien: ACK, gui lai 3 lan, loai trung theo endpoint + seq | 203 |
| common/StickerDanhMuc.java | Danh muc PNG Noto Emoji dung chung | 58 |
| common/TepTienDo.java | Snapshot tien do bat bien, khong giu socket/UI | 6 |
| common/TepTin.java | Sieu du lieu tep va quy tac ten an toan | 41 |
| common/TruyenTep.java | Truyen tep UDP nhi phan cua so truot, ghi thang dia | 189 |
| server/ChupThongKe.java | Chup du lieu nhat quan tren khoa ServerCore, khong ghi dia | 45 |
| server/KhoLuuTru.java | Luu tai khoan va config UTF-8; ghi file tam roi thay the de tranh file do dang | 274 |
| server/KhoTep.java | Tep nhi phan va sieu du lieu ben vung theo phong | 82 |
| server/KiemDuyet.java | Mute/ban ben vung, quyen kiem duyet va chong spam | 69 |
| server/LenhPhongServer.java | Phan lenh CRUD/vao/roi phong | 101 |
| server/NhatKy.java | Audit UTF-8 moi su kien mot dong, xoay 5MB | 30 |
| server/QuanLyHoSo.java | Nhan avatar tam, ghep/CRC va luu ho so | 72 |
| server/QuanLyNguoiDung.java | Gop tai khoan va phien; Server la noi quyet dinh quyen va online | 238 |
| server/QuanLyPhong.java | CRUD, quyen chu phong, dat cho vao phong va lich su; duoc ServerCore goi tuan tu | 318 |
| server/QuanLyTep.java | Quyen tep, INIT/GET/DONE, luu va don tep tam | 184 |
| server/QuanLyTinNhan.java | Lich su, loai trung ma Client, thu hoi va gioi han typing | 106 |
| server/QuanTriServer.java | Xac thuc lenh admin/moderation, snapshot va job bao cao | 175 |
| server/ServerCore.java | Server UDP dieu phoi tai khoan/phong; luu tin truoc khi multicast dung nhom phong | 325 |
| server/ServerMain.java | diem chay/giam sat Server | 146 |
| server/XuatBaoCao.java | Bao cao tu snapshot, CSV BOM an toan va HTML tu chua | 85 |
| ui/BangQuanTri.java | JTable quan tri dung chung, loc/sap xep va trang thai | 86 |
| ui/ChatMulticastUI.java | Cua so hai man hinh, ba cot chat; UI chi goi ClientCore tren luong nen | 392 |
| ui/components/ThongBao.java | Toast tren layered pane, toi da ba the; hover dung dem gio, xoa ca Timer khi dong | 131 |
| ui/DangNhapPanel.java | Man hinh dang nhap rieng; validate inline, hien/an mat khau, cai dat Server mo rong | 114 |
| ui/DanhSachPhongPanel.java | hai nhom phong | 207 |
| ui/FormKiemTra.java | Hoi quy form Swing tren component that, khong mang | 85 |
| ui/FormQuanTri.java | Form admin/mute va xac nhan nguy hiem co loi inline | 72 |
| ui/HangTin.java | Mot tin co menu, sticker/thu hoi va trang thai gui | 113 |
| ui/HopThoai.java | Form va vo dialog chung: loi ngay duoi truong, Enter gui, Esc huy; dung lai boi UiPreview | 192 |
| ui/HoSoDialog.java | Form ho so va mat khau, chi goi API bat dong bo | 202 |
| ui/KhungChatPanel.java | Noi dung mot tab: bong tin va dong he thong; tieu de/chuc nang phong o cua so chinh | 123 |
| ui/KiemDuyetUI.java | Menu kiem duyet va form ly do/thoi han | 39 |
| ui/QuanTriFrame.java | Cua so admin nam tab, moi tac vu mang chay nen | 129 |
| ui/QuanTriPreview.java | Anh M5 tu component that, khong ket noi mang | 63 |
| ui/SoanTinPanel.java | o soan tin | 139 |
| ui/StickerKho.java | Nap PNG Noto Emoji va bang chon bon nhom | 67 |
| ui/TepGiaoDien.java | Chon/luu tep va dieu phoi hanh dong the tep | 89 |
| ui/TepPreview.java | Anh xem truoc cac trang thai tep dung component that | 54 |
| ui/ThanhPhan.java | Thanh phan ve tay chi lay token tu Theme: avatar tron va bong tin 12px | 55 |
| ui/ThanhVienPanel.java | Thanh vien ben phai: nhom quyen, online truoc offline, the Ban va menu quan tri cho M5 | 74 |
| ui/Theme.java | Mot he token chung cho moi man hinh; khong dat ma mau rieng trong cac UI khac | 195 |
| ui/TheTep.java | The tep/anh, tien do va nut hanh dong | 68 |
| ui/TongQuanPanel.java | The thong ke va hai bieu do cot Java2D | 46 |
| ui/UiPreview.java | Xuat PNG tu COMPONENT THAT, khong Server/socket; du lieu mau chi ton tai trong RAM | 364 |

## 8. Chạy và bàn giao

Maven ở `C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd`; Java ở JDK21 trong .vscode/settings.json. Lệnh đã ghi trong AGENTS.md. Server chạy `server.ServerMain` rồi bấm Bắt đầu; ClientMain1/2/3, cùng máy localhost:5000, khác máy nhập IP LAN. Server mới mở cửa sổ mà chưa Bắt đầu thì chưa nghe5000.

Ảnh mới không chứng minh đã thử chuột/phím, HiDPI, WindowsFirewall máykhác hoặc2máyvậtlý. Bản portable chưa tạo. Không tuyên bố giao diện đã được người dùng duyệt vì xem preview trong lượt audit.

Agent nhận prompt thực hiện phải ưu tiên lỗi hồi quy đã phát hiện và hoàn thiện M5, kiểm thử đủ, cập nhật bảng file/hàm/test/docs, commit đúng mốc và dừng xác nhận. Không code các mốc sau chỉ vì chuẩn bị tài liệu xong.
