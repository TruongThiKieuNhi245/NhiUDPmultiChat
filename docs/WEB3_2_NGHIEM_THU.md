# Web 3.2 - Quản trị, thống kê và báo cáo PDF

Ngày 26/11/2026. Baseline `4e02c11`; giữ sáu tài liệu có sửa đổi trước khi bắt đầu, dữ liệu vận hành và các tài nguyên/giấy phép hiện có. Không push/public.

## Yêu cầu và thay đổi

| Vấn đề thực tế | Nguyên nhân | Thay đổi | Kiểm chứng |
|---|---|---|---|
| Sidebar có Tin chờ/lỗi không cần thiết | Web 2/3.1 có panel tổng hợp riêng | Bỏ panel, nút và badge tổng hợp; lỗi/xác nhận/gửi lại vẫn ở cạnh tin để phản ánh đúng kết quả gửi | Browser kiểm không còn nút và retry tại hàng giữ ID, không trùng |
| Xuất lịch sử rời khỏi ngữ cảnh phòng | Nút TXT nằm ở sidebar | Đưa vào Thông tin phòng, ưu tiên HTML dễ đọc có bố cục hai phía, phân ngày và CSS in; giữ TXT tương thích | File tải thật, HTML escape, nội dung thu hồi không xuất hiện |
| Chưa có admin/thống kê/PDF trên web | Nghiệp vụ trước đây mới ở desktop | `/admin` với Tổng quan, Người dùng, Phòng, Nhật ký, Báo cáo; PDF thật và bộ CSV/HTML từ cùng snapshot | Test quyền/phiên/CRUD/UDP, browser hai tài khoản, PDF parse/render và JAR restart |

## Chức năng

- Trang admin riêng cùng origin; người dùng thường không thấy lối vào và bị backend từ chối API admin, kể cả giả vai trò. `/admin` chưa đăng nhập hiển thị form đăng nhập, tài khoản thường hiển thị thông báo không có quyền.
- Tổng quan: tổng tài khoản/online/offline/khóa, phòng mở/đóng, tổng tin, tệp/dung lượng, top người chat/phòng hoạt động, số thành viên và online theo phòng. Online lấy từ phiên nghiệp vụ, không đếm số WebSocket.
- Người dùng: tìm/sắp xếp/phân trang bảng, thêm/sửa/xóa, khóa/mở khóa, cấp mật khẩu tạm, đổi vai trò và kiểm duyệt. Backend bảo vệ chính mình/admin cuối. Reset/khóa/đổi vai trò thu hồi phiên và WebSocket ngay; cookie của phiên đã thu hồi không làm kẹt đăng nhập lại. Mật khẩu tạm chỉ xuất hiện trong phản hồi/dialog, buộc đổi khi đăng nhập; không đưa vào audit/report.
- Phòng: tạo/sửa/đóng/mở/xóa, gán/chuyển chủ, xem thành viên, xóa nội dung bằng tombstone, kick/mute/ban và gỡ theo thời hạn. Gán chủ kiểm khóa/ban/sức chứa; chủ đang online nhận quyền phòng ngay. Sảnh giữ bảo vệ tên/trạng thái/xóa/đuổi.
- Thông báo toàn hệ thống dùng kind Y qua UDP CHAT2 thật, cho cả phòng đóng/riêng. Backend kiểm ADMIN tại receive; HTTP không ghi tắt tin. Retry cùng mã chỉ lưu một tin/phòng; hệ thống hiển thị giữa, tách tin thành viên.
- Nhật ký: xem/tìm/phân trang, xuất CSV riêng; audit theo actor/action/đối tượng, không ghi password/token/nội dung chat. Giữ chống spam và mute tự động của lõi desktop.
- Báo cáo: xem số liệu rồi tải PDF A4 có font Noto Sans nhúng, bảng số người dùng theo từng phòng, top người chat/phòng, tệp và biểu đồ/bảng tin theo ngày. PDF giữ bảng nhỏ cùng trang khi đủ chỗ, lặp tiêu đề cho bảng dài, đánh số trang. HTML và 4 CSV trong ZIP giữ schema desktop, UTF-8 BOM, chống công thức và escape HTML.

## Snapshot và giới hạn

Một snapshot bất biến gắn token phiên, tồn tại 5 phút; tối đa 2/phiên, 8/toàn dịch vụ, 20.000 dòng/snapshot. Bản xem trước và PDF/CSV/HTML dùng cùng ID và số liệu, không đọc lại kho trong lúc xuất. Khi có tin/thay đổi mới, báo cáo đánh dấu cần làm mới; không âm thầm thay số liệu đang xem. Admin không tham gia phòng chỉ nhận invalidation thống kê không chứa roomId/tên/nội dung phòng. Sau khi mất quyền/đăng xuất, không được lấy lại snapshot cũ. Tối đa 2 lượt tạo tệp đồng thời, PDF tối đa 200 trang; bảng UI phân trang 25 dòng.

Tin gồm text/sticker/file/system/tombstone trong các phòng còn tồn tại. Tệp/dung lượng thống kê là tệp đã gửi còn trong lịch sử, chưa thu hồi; không gồm upload chờ/orphan. Quota lưu trữ tệp vẫn tính cả tệp chờ theo Web 3.1. PDF không chứa mật khẩu/token/nội dung chat. Ký tự ngoài font được biểu diễn `[U+...]`, không làm hỏng cả báo cáo. Font/nguồn/giấy phép: [NGUON_TAI_NGUYEN.md](NGUON_TAI_NGUYEN.md).

Kho vẫn là file dùng chung với desktop, một instance; không thay DB. Giới hạn tin/phòng, session, UDP pending/frame/retry, cache chat và upload của các mốc trước vẫn giữ. CRUD nhiều file kế thừa lõi hiện có, không được coi là giao dịch DB xuyên toàn bộ dữ liệu. Nhập từ bản sao/backup/rollback và kiểm chứng host/HTTPS thuộc mốc sau.

## Bản đồ source/API

| File/hàm | Vai trò |
|---|---|
| `server/WebAdministration` | Quyền admin, capture/read snapshot, cache gắn phiên, CRUD/kiểm duyệt, plan broadcast |
| `server/WebChatService.admin* / broadcastPlan / administrator` | Façade trên khóa chung, revoke phiên, xóa/clear phòng, audit, nhận Y sau UDP |
| `server/ChupThongKe` | Dùng provider file chung desktop/web; đếm tin theo user/room/member một lượt |
| `server/WebReports.pdf/zip/csv/html` | PDFBox 3.0.8 + font Noto, layout/pagination/charts, bộ 4 CSV/1 HTML và audit CSV |
| `server/WebHistoryReport.html` | Bản lịch sử HTML offline, escape và giữ thu hồi/quote |
| `web/WebAdminApi` | GET `/api/admin/snapshot`, POST `/api/admin/actions/{action}`, POST `/api/admin/broadcast`, GET `/api/admin/reports/{id}/{pdf|zip|html|audit}`; shell `/admin` |
| `web/WebApi`, `WebSecurity` | Xuất phòng `/api/export?roomId=…&format=html|txt`, guard 403, xóa danh tính HTTP đã thu hồi, cookie/session/CSRF |
| `web/ChatSocket.attachEvents/broadcast/event` | Đăng ký sau khởi tạo, đóng WS khi revoke, báo thay đổi thống kê chỉ cho admin |
| `static/admin-ui.js` | Năm mục admin, bảng có tìm/sort/page, form/dialog, dữ liệu đang xem và download |
| `static/workspace-ui.js`, `app.js`, `index.html`, `app.css`, `chat-view.js` | Sidebar gọn, xuất trong thông tin phòng, route/theme admin, hệ thống căn giữa |

## Khởi tạo admin

Kho hoàn toàn mới nhận mật khẩu ban đầu từ `CHAT_INITIAL_ADMIN_PASSWORD`, 12–128 ký tự. Không đặt mật khẩu mặc định công khai; biến này không reset hoặc ghi đè admin của kho đã tồn tại. Đăng ký thường không được tự cấp ADMIN. Mật khẩu trong cấu hình một lần được kho chuyển thành hash; đăng nhập web tiếp tục chính sách nâng hash đã kiểm ở Web 1. Thay mật khẩu trong hồ sơ sau khi nhận thông tin bản xem thử nếu cần.

```powershell
$adminSecret = Read-Host 'Mật khẩu quản trị ban đầu' -AsSecureString
$env:CHAT_INITIAL_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password
java -jar target/web3-admin-preview/UDPmultiChat-1.0-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8084 --server.servlet.session.cookie.name=UDPCHAT_WEB32 --chat.data-dir=.selftest/admin-new-data
Remove-Item Env:CHAT_INITIAL_ADMIN_PASSWORD
```

Lệnh trên chỉ phù hợp thư mục dữ liệu mới được chọn rõ ràng. Bản xem thử của mốc này dùng thư mục riêng, không sửa `data/`, `web-data/` hoặc dữ liệu preview cũ.

## Kết quả và bằng chứng

Các lượt sau đã chạy xong trên fixture riêng. Browser là Edge 158.0.4370.33, hai context độc lập; không dùng tài khoản/dữ liệu vận hành.

| Nhóm đã chạy | Kết quả và bằng chứng |
|---|---|
| Maven/JUnit | **26 test, 0 lỗi/thất bại/bỏ qua**; gồm 6 test Web3AdminTest mới. [Chi tiết Java](screenshots/web3-admin/java-results.json) |
| Desktop AdminSelfTest | **16 PASS, 0 FAIL**, chạy main riêng với test classpath; kiểm nghiệp vụ admin/thống kê/xuất của desktop bị ảnh hưởng |
| Admin từ source | **11 nhóm PASS**: quyền, CRUD, revoke/reset/khóa/role, snapshot/PDF/CSV, broadcast UDP, logout. [JSON](screenshots/web3-admin/browser-results.json) |
| Admin bằng JAR thật | **12 nhóm PASS**, thêm restart giữ user/role/phòng/audit và endpoint PDF. Không có endpoint fixture trong artifact. [JSON](screenshots/web3-admin/packaged/package-results.json) |
| Hồi quy UX | **12 nhóm PASS**: hình học hai chiều, trùng tên, pending/retry cùng node, IME/byte, cuộn/selection, lịch sử/reconnect/cache. [JSON](screenshots/web3-admin/regression-ux/ux-results.json) |
| Hồi quy Web 2 | **11 nhóm PASS**: lời mời offline/quyền phòng/hồ sơ/sticker/reply/thu hồi/TXT/typing/mật khẩu. [JSON](screenshots/web3-admin/regression-web2/browser-results.json) |
| Hồi quy tệp | **8 nhóm PASS**: 5 MB khớp byte/CRC, UDP dừng/retry, quyền tải, thu hồi, tablet/360 px/zoom 125–150%, không tràn trang. [JSON](screenshots/web3-admin/regression-files/browser-results.json) |
| PDF thực tế | Bản mẫu **2 trang A4**, font nhúng/tiếng Việt, kiểm bounds/footer và xem ảnh cả hai trang; fixture **180 phòng** tạo **10 trang**, tiêu đề lặp/bảng không cắt chữ. [Layout dài](screenshots/web3-admin/pdf-long/results.json) |
| JAR và bản xem thử | **220 class production** trùng byte với build Maven riêng; toàn bộ resources trùng source, không chứa class fixture; `/admin` trực tiếp đăng nhập được, 5 tab, cookie riêng, đã logout sau QA. [Preview](screenshots/web3-admin/preview-results.json) |

Bản PDF mẫu chỉ chứa dữ liệu fixture: [bao-cao-mau-web3.pdf](../output/pdf/bao-cao-mau-web3.pdf). Ảnh browser thật: [tổng quan](screenshots/web3-admin/packaged/02-admin-overview.png), [báo cáo sáng](screenshots/web3-admin/packaged/01-admin-reports-light.png), [tối](screenshots/web3-admin/packaged/03-admin-dark.png), [mobile 360 px](screenshots/web3-admin/packaged/04-admin-mobile.png). PDF đã render: [trang 1](screenshots/web3-admin/packaged/pdf-page-01.png), [trang 2](screenshots/web3-admin/packaged/pdf-page-02.png).

Artifact: `target/web3-admin-preview/UDPmultiChat-1.0-SNAPSHOT.jar`. SHA-256: `efe63532f5a35a320a49e30aef6e4385482c83166bef200b6cbddbc99d0b462a`. Build từ bản sao source riêng `.selftest/web3-admin-build-e1bdbdc19e` để không ghi đè JAR preview cũ đang mở và không trộn bytecode IDE với Maven. JAR cùng hash đã chạy QA/restart. Không chạy Maven compile đồng thời với browser fixture dùng chung classes.

Lệnh chạy lại với build chuẩn (dừng tiến trình đang dùng đúng JAR đích trước package):

```powershell
$mvnChat = 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd'
& $mvnChat test package dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
python -X utf8 scripts/web3-admin-browser-qa.py
python -X utf8 scripts/web1-ux-browser-qa.py
python -X utf8 scripts/web2-browser-qa.py
python -X utf8 scripts/web3-files-browser-qa.py
$chatTestCp = 'target/test-classes;target/classes;' + (Get-Content target/web-classpath.txt -Raw).Trim()
java -cp $chatTestCp com.mycompany.udpmultichat.AdminSelfTest
$env:CHAT_QA_JAR = 'target/UDPmultiChat-1.0-SNAPSHOT.jar'
$env:CHAT_QA_OUTPUT = 'target/admin-package-qa'
python -X utf8 scripts/web3-admin-browser-qa.py
Remove-Item Env:CHAT_QA_JAR, Env:CHAT_QA_OUTPUT
```

Các script nhận `CHAT_QA_CLASSPATH` khi kiểm từ build riêng; đã dùng classpath ở `target/web3-admin-classpath.txt` cùng test/classes của bản sao nói trên. Bản xem thử local: **http://127.0.0.1:8084**, admin **http://127.0.0.1:8084/admin**. Dùng dữ liệu riêng `.selftest/web3-admin-preview-e5e37af04f/data`, cookie `UDPCHAT_WEB32` để không đụng preview cũ cùng host. Thông tin đăng nhập ngẫu nhiên chỉ lưu local trong `.selftest/web3-admin-preview-e5e37af04f/admin-login-local.txt` (ignored, không commit); không dùng mật khẩu mẫu công khai.

Chưa kiểm bàn phím ảo trên điện thoại vật lý hoặc nhiều máy/host đích. Các số PASS trên chỉ áp dụng phạm vi đã chạy; backup/nhập bản sao/rollback và Web 4 chưa nghiệm thu. Dừng tại mốc này để người dùng xem kết quả; chưa push/public.

## Thử tay

1. Mở bản xem thử bằng hai phiên; tài khoản thường không có nút quản trị. Trong thông tin phòng, tải HTML/TXT và mở HTML để xem bố cục hội thoại.
2. Đăng nhập admin, mở `/admin`, đi qua đủ năm mục; tìm/sắp xếp bảng, tạo người dùng/phòng và kiểm chủ phòng online dùng được ngay.
3. Mở Báo cáo, ghi nhận số liệu, tải PDF và ZIP. Gửi thêm tin từ phiên kia; bản đang xem được đánh dấu cũ, PDF vẫn khớp cho tới khi bấm Làm mới.
4. Reset hoặc khóa tài khoản thử trong fixture; phiên/WS cũ phải mất quyền. Đăng nhập cùng trình duyệt bằng mật khẩu tạm và đổi mật khẩu bắt buộc.
5. Kiểm sáng/tối và 360 px; thử thông báo hệ thống và kiểm giữa màn hình. Chỉ thao tác xóa/clear trên dữ liệu thử.
