# Rà soát M5 từ working tree hiện tại

Cập nhật rà soát 18/11/2026; chạy kiểm tra lại 19/11/2026. Phạm vi: desktop Java 21, Swing/FlatLaf, UDP unicast cho điều khiển/tệp và multicast thật cho chat. Chưa chuyển sang M6, M3c hoặc M7. Không reset source M5, không sửa dữ liệu `data/`, không push remote.

## Chạy trong NetBeans và làm sạch fixture

Ngày19/11: 1.591 file test cũ (280.583.611 byte) được chuyển nguyên vẹn từ `.selftest` sang `%TEMP%/UDPmultiChat-test-backup-20261119-123200`; không xóa log FAIL. Tạo lại `.selftest` sạch, hiện chỉ có cấu hình NetBeans riêng. Thêm `nbactions.xml` cho Run Server/3 Client, Prepare fixture, SelfTest, AdminSelfTest, Swing test và UiPreview; ServerMain hỗ trợ `udpmultichat.dataDir`, UiPreview hỗ trợ `udpmultichat.previewDir` để tránh `data/` thật và ảnh tham chiếu. NetBeans 31 đang mở, nhưng chưa thực thi action vì helper UI báo `windows sandbox failed: helper_unknown_error: setup refresh had errors`; đây là cấu hình đã chuẩn bị, chưa phải kết quả chạy trong IDE.

## Những lỗi đã xử lý

| Vấn đề | Bản sửa | Bằng chứng |
|---|---|---|
| Avatar có thể cạn 4 lần gửi của một request giữa nhiều mảnh | `YeuCauAnh` thử lại có giới hạn ở tầng nghiệp vụ, giữ requestId/token; `QuanLyHoSo` lưu phản hồi thành công tối đa 2.048 mục/120 giây | HoSoSelfTest: retry cùng mã, lỗi quyền không retry, BEGIN/SAVE trùng, cancel, rollback avatar và loss20% seed20261113 |
| Reset/khóa có race: thấy `nguoiDung()==null` trước khi close dọn xong, đăng nhập mới bị close cũ hủy | Chỉ công bố null sau khi dọn phiên; chặn kết nối trong giai đoạn đóng; tách map request/response sang `YeuCauClient` | AdminSelfTest CRUD/reset/force-out/login; lượt đầu tái hiện lỗi, lượt sau sửa PASS |
| SACK cũ lặp làm gửi lại nhanh nhiều lần, cạn retry8 | Chỉ đánh dấu gửi lại nhanh sau ít nhất 3 mảnh mới được ACK kể từ lần gửi trước | Ca `TepSelfTest ackcu`: class M4 lấy vào fixture riêng FAIL/exit1, bản sửa PASS/exit0; giữ window32, retry8 và timeout60s |
| Tạo/sửa/reset/khóa tài khoản nhiều bước có thể để RAM/config/CSV lệch khi ghi lỗi | Chụp trạng thái trước giao dịch, khôi phục RAM/phiên và những file đã đổi nếu lỗi; lỗi phục hồi được giữ ở suppressed exception | Fixture chặn `accounts.properties.tmp`/`users.csv.tmp`; kiểm CSV, flags, hash cũ, reset/lock sau restart |
| Mute/ban ghi lỗi vẫn đổi RAM | Khôi phục Properties khi ghi moderation thất bại | Fixture chặn file tạm moderation |
| MOD/MOD_LIST bị từ chối không có audit; request action trùng có thể lặp thao tác/audit | Ghi DENIED theo quyền; cache tối đa 512 action/MOD trong 120 giây, đối chiếu nguyên request; không ghi mật khẩu/token vào audit | Tất cả AD action giả mạo, MOD/MOD_LIST, cùng request CRUD/DENIED/broadcast |
| Broadcast lỗi multicast sau khi lưu dễ bị báo thất bại rồi gửi lặp | Tiếp tục phát các phòng, dữ liệu đã lưu được RESYNC; lỗi đĩa trả rõ số phòng đã lưu/lỗi và audit PARTIAL; replay cùng request không tạo tin mới | Fixture một room log không ghi được, kiểm số tin và audit |
| PING phát mute cho tất cả phiên; AD_CHANGED phát trùng | Heartbeat chỉ bù mute cho phiên PING; dùng một nguồn revision từ capNhatPhong | Rà luồng ServerCore/QuanTriServer; regression moderation |
| ZIP lỗi để lại staging, chưa chặn tên trùng/thiếu bộ file | Whitelist đúng bộ 4 CSV+HTML hoặc nhật ký, containment, tổng giải nén 50 MiB; dọn riêng thư mục vừa tạo khi lỗi | ZIP slip, file lạ, thiếu/empty, duplicate entries và cleanup |
| Báo cáo có thể lộ thư mục kết quả chưa ghi đủ | Ghi cả bộ vào staging riêng, đổi tên thư mục sau khi đủ file, không ghi đè | Xuất hai lần, CSV/HTML/ZIP và tải UDP; source XuatBaoCao |
| Bảng admin mất header trong preview, ngày cắt, glyph ba chấm lỗi | Gắn JTableHeader trực tiếp vào JScrollPane, ngày dd/MM/yyyy HH:mm và tooltip, cột đủ rộng/cuộn ngang, menu `...` | 60 PNG và assertions component thật |
| Bộ lọc user tìm cả dòng; sort đoán số từ dữ liệu | Lọc đúng cột role/status, có Offline; số thống kê dùng BigInteger, login luôn chuỗi; Nhật ký lọc đúng cột loại | UiPreview thử tên/giới thiệu chứa admin/online, login số, số vượt Long và audit chứa từ gây nhầm |
| Refresh4s làm mất dòng đang chọn trước thao tác menu | Giữ lựa chọn theo id qua nạp lại model/filter/sort, chỉ bỏ nếu đối tượng không còn trong kết quả | Assertion UiPreview FAIL trước sửa; test sự kiện đã reset/khóa/xóa và moderation thành công sau sửa |
| UI mute toàn hệ thống không có đường gỡ | Thêm menu Gỡ cấm chat toàn hệ thống, dùng MOD MUTE scope* thời hạn-1 có sẵn; không đổi quyền Server | `m5-global-unmute-before.err` FAIL thiếu menu; `m5-swing-events-3.log` PASS mute/gỡ cấm thật qua UDP |
| Nút mở HTML không có báo cáo; xuất nhật ký ghi đè đích HTML trong UI | Vô hiệu nút mở trước kết quả; giữ thư mục báo cáo riêng khi xuất nhật ký; form X được dispose | UiPreview tab Báo cáo, source QuanTriFrame/FormQuanTri |
| Xác nhận xóa cắt mô tả; form admin có thể giữ chiều cao cũ khi lỗi hiện | Đo chiều cao đoạn mô tả theo bề rộng thật; không cố định chiều cao form; invalidate cache BoxLayout và pack lại khi lỗi hiện | Ảnh52/57; assertion chiều cao trước/sau hai lỗi, kiểm input không nén |
| Top phòng dùng tên làm khóa gây gộp phòng trùng tên | Khóa nhãn gồm tên và mã phòng | TongQuanPanel |
| README nói sửa config sẽ reset admin hiện có | Sửa hướng dẫn theo constructor thật: config chỉ bootstrap; reset qua admin khác | QuanLyNguoiDung, README |

## Ma trận nghiệm thu M5

| Yêu cầu | File/hàm chính | Kiểm chứng |
|---|---|---|
| Menu chỉ admin; Server kiểm quyền | ChatMulticastUI.menuAvatar; ServerCore.xuLyGoi; QuanTriServer.xuLy | AdminSelfTest.tuChoi; assertions menu/identity cũ |
| 5 tab, refresh4s và theo revision | QuanTriFrame.nap/capNhat/dispose; QuanTriClient.lay | UiPreview44–48, snapshot qua UDP |
| User CRUD, temp/reset10 ký tự, force change/lock/guard/delete | QuanLyNguoiDung; QuanTriServer.thaoTac/xoaUser | AdminSelfTest.taiKhoan/rangBuoc/xoaUser; QuanTriBoSungSelfTest |
| Phòng CRUD/chọn chủ/đóng/xóa/clear; bảo vệ Sảnh | QuanLyPhong; LenhPhongServer; QuanTriServer | AdminSelfTest.rangBuoc; hồi quy PhongSelfTest |
| Kick/mute/unmute/ban/unban/transfer, persist/expiry | KiemDuyet; QuanTriServer.moderation; KiemDuyetUI | AdminSelfTest.kiemDuyet/khoiDongLai; rollback moderation |
| Mute chặn text/sticker/file/recall | QuanLyPhong.kiemTraGui; ServerCore.thuHoi; QuanLyTep | AdminSelfTest.kiemDuyet |
| Spam5/3s và20/phút, lặp auto mute60s | KiemDuyet.choTin | AdminSelfTest.spam; QuanTriBoSungSelfTest.spamPhut |
| Broadcast200 ký tự, lưu phòng/online | QuanTriServer.thongBao | AdminSelfTest.kiemDuyet; partial/replay fixture |
| Thống kê user/room/tin/file/bytes, offline, biểu đồ | ChupThongKe; TongQuanPanel | AdminSelfTest.baoCao: 5 user,3 phòng,40 tin mới,2 file; bytes/user/room và member offline |
| Audit append/5MiB/escape/deny/duplicate/export | NhatKy; QuanTriServer; KiemDuyet | AdminSelfTest.nhatKy/tuChoi/taiKhoan; QuanTriBoSungSelfTest.lenhTrung |
| 4 CSV BOM/RFC4180/injection, HTML escape/offline/print | XuatBaoCao; AnhChupQuanTri | AdminSelfTest.baoCao/escape/dongThoi |
| ZIP UDP, safe extract, Server xuất trực tiếp | QuanTriClient.xuat/giaiNen; QuanLyTep; ServerMain | AdminSelfTest.baoCao/zipAnToan |
| Server IP/copy,6 thẻ/log/bắt đầu-dừng nền | ServerMain; ServerCore.phienVaTep | UiPreview12; source EDT/worker; thao tác thật còn chờ |

## Định nghĩa số liệu và giới hạn

- Tổng tin và số tin/user/phòng/ngày gồm tin văn bản, sticker, tệp, hệ thống và tombstone thu hồi của phòng còn tồn tại. Xóa phòng loại phòng và lịch sử khỏi snapshot.
- Số tệp/sticker theo ngày là tin còn nội dung; thu hồi được đếm riêng. Schema tombstone cũ không giữ loại gốc, nên **không** gọi đây là thống kê mọi tệp/sticker từng gửi trước thu hồi.
- Tổng tệp/bytes là bản gốc còn lưu, không gồm thumbnail, tệp tạm, ZIP báo cáo hoặc tổng lưu lượng mạng đã truyền. Thành viên bao gồm cả offline; online là phiên đang hoạt động.
- Nhật ký hiển thị/xuất tối đa 1.000 dòng cuối của audit.log hiện tại; audit.1.log là bản xoay trước. Không gọi đây là tìm toàn bộ lịch sử audit.
- Snapshot được giữ trong 30 giây không có lượt đọc, tối đa32 snapshot; action cache120 giây/512 mục; job tối đa8/10 phút. Client giới hạn10.000 dòng/bảng. ZIP tải chịu upload.maxBytes (mặc định10MiB), quá giới hạn hướng dẫn xuất tại Server. Giải nén tối đa50MiB.
- Rollback tài khoản/avatar xử lý lỗi trong tiến trình; chưa phải giao dịch chống mất điện qua nhiều file. Xóa user/phòng liên quan nhiều kho, chưa có transaction journal chống crash toàn bộ.
- CRC32 không xác thực nguồn. Token/mật khẩu/tệp UDP chưa mã hóa; multicast phụ thuộc LAN/card/firewall. Không phù hợp public Internet. Tệp dở chưa resume. RAM lịch sử chưa tối ưu quy mô lớn.

## Kết quả và điểm dừng
### Chạy kiểm tra lại ngày 19/11/2026 (lượt 2 — tái xác nhận, không hồi quy)

| Kiểm tra | Kết quả | Phạm vi / log |
|---|---|---|
| Maven `compile` | Exit0, không cảnh báo | Java Temurin 21.0.12.1, Maven 3.9.16; `.selftest/audit-compile-m5-new.log` |
| Full `SelfTest` | **75 PASS, 0 FAIL**, exit0 | Chạy liên tục; avatar20% loss seed20261113 PASS; tệp5MiB loss20% upload65,772s/download64,713s, byte/CRC đúng; `.selftest/m5-new-selftest.log` |
| Riêng `AdminSelfTest` | **16 PASS, 0 FAIL**, exit0 | Chạy độc lập; `.selftest/m5-admin-new.log` |
| `UiPreview` | **60 PNG**, 10/10 assertions PASS, exit0 | Sinh vào `.selftest/m5-preview-new`; không ghi đè ảnh tham chiếu; `.selftest/m5-uipreview-new.log` |

Kết quả lượt tái xác nhận 19/11 (phiên mới): không phát hiện hồi quy. Phép đo tệp là localhost; chưa thử hai máy LAN vật lý. Toàn bộ ảnh 44–60 (đã có trong `docs/screenshots` từ phiên 18/11) chờ người dùng duyệt tương tác thật trước khi ghi nhận UI hoàn tất. Chưa kiểm chứng portable không Java, HiDPI khác.


Các log nằm ở `.selftest/`, không đưa fixture/data test vào Git. Giữ cả lượt FAIL và PASS; không cộng kết quả lịch sử thành kết quả mới.

| Kiểm tra cuối18/11/2026 | Kết quả | Bằng chứng |
|---|---|---|
| Maven test-compile và compile sau chỉnh form | Exit0, không cảnh báo | `m5-final-compile.log`, `m5-ui-compile.log` |
| Full SelfTest liên tục | **75 PASS,0 FAIL**, exit0;07:13:27–07:19:17 | `m5-continuous-full.log/.err`, `m5-continuous-result.txt` |
| AdminSelfTest chạy riêng | **16 PASS,0 FAIL**, exit0 | `m5-final-admin.log/.err` |
| UiPreview cuối | **60 PNG**, assertions PASS, exit0; thêm kiểm giữ selection qua refresh | `m5-selection-after.log/.err`; trước đó `m5-60-ui-final.log/.err`; ảnh44–60 trong docs/screenshots |
| Sự kiện Swing + UDP thật bổ sung | **5 PASS,0 FAIL**, exit0; handler/EDT, không phải chuột/phím hệ điều hành | `m5-swing-events-3.log/.err`; QuanTriTuongTacSelfTest/SwingThu |
| Rà tĩnh | 68 file main;14 file Java dưới src/test sau bổ sung fixture/Swing test; không main vượt400 dòng; ui không socket, Swing chỉ ở ServerMain trong core packages; diff whitespace sạch | `rg`, kiểm kê file và `git diff --check` |

Tệp5MiB loss20% DATA/ACK seed20261113: upload63,104s (~0,08MiB/s), download64,527s (~0,08MiB/s), so byte/CRC đúng. Chat khi truyền5,1ms; chat trong lúc xuất báo cáo27ms ở lượt này. Đây là phép đo localhost, không phải tốc độ/kiểm chứng LAN vật lý.

- Lượt full trước sửa SACK: 73 PASS/1 FAIL, exit1 (`m5-full.log/.err`); avatar loss20% PASS, file5MiB loss20% timeout.
- Ca SACK có kiểm soát: `m5-ack-before.log/.err` exit1 trên class M4; `m5-ack-after.log/.err` exit0 trên bản sửa. Source M4 chỉ được lấy vào `.selftest/ack-truoc/`, không checkout/reset project.
- Lượt full `m5-final-full.log/.err` kéo dài05:50–07:12 qua khoảng gián đoạn phiên làm việc:68 PASS/7 FAIL. Cả ba Client hết heartbeat ở nhóm file, làm hỏng fixture phụ thuộc. Không có bằng chứng đủ để quy nguyên nhân gián đoạn cho source hoặc Windows sleep; không xóa log hoặc dùng lượt này làm nghiệm thu. Lượt liên tục sau đó mới là kết quả75/0.
- Assertion form admin mới từng FAIL trong `m5-60-ui.log/.err`; sau sửa invalidate/đo mô tả, `m5-60-ui-final.log/.err` PASS. Không bỏ assertion để lấy ảnh đẹp.
- Công cụ điều khiển cửa sổ bị lỗi `helper_unknown_error: setup refresh had errors`, chưa có bằng chứng chuột/phím thật của lượt này.
- Bài Swing bổ sung đã bắt được lỗi selection và thiếu đường unmute global. Lượt `m5-swing-events-2.err` dừng do driver test tìm nút Thành viên trong toolbar thay vì menu; đã sửa driver, không gán lỗi đó cho ứng dụng. Lượt `m5-swing-events-3` sau sửa đạt5/0. Không cộng số này vào75/0 của SelfTest mạng.
- Chưa kiểm chứng hai máy vật lý, portable không Java, HiDPI khác. Các phần đó thuộc M6/M7; không nhận là đã thử.
- **Chưa có commit “M5 xong”**. Prompt mục5 yêu cầu người dùng duyệt ảnh và thao tác trước khi ghi nhận UI hoàn tất; mục9 yêu cầu chỉ commit đúng tên khi đủ tiêu chí. Không dùng PASS của preview để thay sự xác nhận này.

## Năm bước thử tay trước khi chốt M5

Có thể dùng [bộ thử một lệnh](THU_TAY_M5.md): `scripts/thu-tay-m5.ps1` compile và tạo Server +3 Client với fixture mới trong `.selftest/`. `ChuanBiThuTayM5` nằm ở src/test/java, tạo3 tài khoản và tệp5MiB, không sửa dữ liệu thật. Chế độ `-ChiChuanBi` đã chạy thành công; vẫn không thay bằng chứng chuột/phím. Script này không thuộc đóng gói portable M6/M7.

1. Chạy Server → Bắt đầu; chạy Client admin và hai Client thành viên bằng cấu hình hiện có. Admin mặc định đổi mật khẩu trước khi vào Sảnh. Mở Quản trị, kiểm đủ5 tab, bảng có header, sort/filter/refresh4s; user thường không có menu Quản trị.
2. Tạo user tạm → login → bắt buộc đổi mật khẩu. Reset10 ký tự, sao chép một lần, xác nhận phiên cũ bị ngắt và login mới được. Thử khóa/mở; xác nhận không tự khóa/xóa/hạ quyền.
3. Tạo phòng/chọn chủ; dùng chủ và admin thử mute5phút/unmute, kick, ban/unban, chuyển chủ. Client bị mute không gửi text/sticker/file/recall; kick đóng khung đúng phòng. Thử user offline vẫn hiện trong danh sách.
4. Gửi broadcast, kiểm hai Client nhận và lịch sử các phòng có tin. Xuất báo cáo tại admin, mở4 CSV/HTML; kiểm số thành viên offline/bytes; xuất Nhật ký riêng và lọc theo loại. Thử nút xuất trực tiếp trên Server.
5. Khởi động lại Server, kiểm user/role/lock/mute/ban/mật khẩu và báo cáo còn đúng. Thử resize/Enter/Esc/click menu; ghi lại lỗi nếu có. Đây chưa thay bài hai máy LAN và portable của M7.

## Tài liệu môn và thay đổi workspace

Đã đọc nội dung trích xuất cả 40 trang UDP và14 trang Multicast tại thư mục tailieu; lưu bản text riêng trong `.selftest/`. Giữ hai PDF gốc. Kiến trúc vẫn DatagramSocket/Packet/send/receive; multicast join/leave trực tiếp từng phòng. ACK/seq/retry/CRC/RESYNC là lớp ứng dụng tự cài, không phải bảo đảm có sẵn của UDP.

Thêm `YeuCauAnh`, `YeuCauClient`, `QuanTriBangKiemTra` cùng hai file test bổ sung. Sau lượt75/0 thêm tiện ích fixture `ChuanBiThuTayM5`, `QuanTriTuongTacSelfTest`/`SwingThu` và script/hướng dẫn thử tay; hiện có68 file main,14 file Java dưới src/test. Phần M5 dở có sẵn được giữ và sửa tiếp. Không xóa/chuyển source, dữ liệu thật, sticker, ảnh tham chiếu01–43 hoặc giấy phép. Không thêm dependency Maven.

## Nâng cấp và hoàn thiện giao diện M6 (19/11/2026)

| Hạng mục | Thực hiện trong M6 | Kiểm chứng & Bằng chứng |
|---|---|---|
| **Hover Action Bar tin nhắn** | Thay thế nút inline thô bằng thanh công cụ nổi bo góc nhỏ (26px), ẩn mặc định, xuất hiện khi rê chuột vào hàng tin: icon Sao chép, icon Thu hồi (tin của mình / admin trong 10 phút), icon `···` mở popup menu | `HangTin.java`, `UiPreview` 33-chat-sticker-trang-thai.png, 34-menu-tin-nhan.png |
| **Giao diện ServerMain** | Rút gọn đường dẫn dài (`tenNgan(p)`), tách layout `trangThai` ở `CENTER` và cụm nút xuất báo cáo ở `EAST`, chống đè chữ/tràn nút | `ServerMain.java`, `UiPreview` 12-server.png |
| **Vector Icons Java2D** | Bổ sung vector icon `copy` và `undo` trong `Theme.java`, chuẩn hóa nét vẽ không phụ thuộc font hệ thống | `Theme.java` |
| **Nâng cấp Báo cáo Quản trị** | Thêm hộp thoại xem trước báo cáo trực quan `BaoCaoPreviewDialog` hỗ trợ **In / Lưu PDF** (chuẩn JDK Java Print Service, không thêm thư viện ngoài), mở trình duyệt, lưu HTML; thiết kế lại Tab Báo cáo `BaoCaoPanel` 2 Card (chọn nhóm dữ liệu: Người dùng, Phòng, Nhật ký, Thống kê 7 ngày; định dạng HTML/PDF, CSV); thêm nút xuất riêng lẻ từng bảng trên toolbar bảng Người dùng, Phòng, Nhật ký | `BaoCaoPreviewDialog.java`, `BaoCaoPanel.java`, `XuatBaoCao.java`, `BangQuanTri.java`, `QuanTriFrame.java` |
| **Tài liệu chuẩn hóa M6** | Hoàn thành `docs/KHAO_SAT_CHAT_2026.md` (khảo sát Discord, Slack, Zalo PC) và `docs/THIET_KE_GIAO_DIEN.md` (Design tokens, 3 cột, trạng thái) | Docs đầy đủ |
| **Kiểm thử tự động** | Full SelfTest: **75 PASS / 0 FAIL**; AdminSelfTest: **16 PASS / 0 FAIL**; UiPreview: **60 PNG / 10 assertions PASS**; Rà soát dòng: `QuanTriFrame` 319 dòng, `BaoCaoPanel` 201 dòng (tất cả file ≤ 400 dòng) | Log & console output |
