# Web 3.3 — Quản lý thành viên và thông báo

Ngày 26/11/2026, baseline `1351412`. Giữ sáu tài liệu đã sửa trước lượt này, dữ liệu vận hành và toàn bộ preview cũ. Không push/public.

| Vấn đề thực tế | Nguyên nhân | Thay đổi | Kiểm chứng |
|---|---|---|---|
| Thao tác xong chỉ đóng dialog, không rõ kết quả | Form chưa có phản hồi thành công | Phản hồi rõ cho cấm/gỡ cấm chat, đuổi, ban/gỡ ban, chuyển chủ và các thao tác phòng/tài khoản/lời mời | Browser thao tác thật, kiểm nội dung cả bên quản lý và người nhận |
| Người bị đuổi không nhận lý do | Sự kiện cũ chỉ gửi tới người còn quyền đọc phòng | `member-notice` gửi riêng theo userId sau kiểm duyệt, kể cả đã mất quyền phòng; `room-activity` chỉ tới người còn quyền | Bốn context owner/member/admin/outsider, trùng tên; người ngoài không nhận notice/lý do |
| Không biết vì sao nút gửi khóa | Snapshot có mutedUntil nhưng UI chưa giải thích | Banner thời hạn/lý do trên composer, text/sticker/file khóa đúng; vẫn đọc và giữ nháp; hết hạn tự mở | Tải lại, hết hạn, gỡ cấm, gửi UDP trái quyền bị chặn |
| Gỡ ban phải tìm tên lại, không biết đang cấm ai | Không có API/list lệnh còn hiệu lực | Danh sách cấm theo phòng/toàn hệ thống, @login + userId, loại/thời hạn/lý do; gỡ trực tiếp | Ban người đã rời phòng vẫn có trong list; gỡ ban không tự thêm lại thành viên |
| Admin đuổi chủ nhưng chưa vào được phòng thay thế | Kiểm lại chuId trên object Phong cũ sau khi đã thay object | Ghi nhận nhánh thay chủ trước; nối phiên chủ mới ngay | JUnit và browser admin chưa tham gia phòng vẫn đọc/gửi được ngay sau thay chủ |
| Đăng ký quá sớm đôi lúc Forbidden; typing mất khi đổi phòng nhanh | Hai request CSRF khởi tạo/submit cạnh tranh; throttle typing dùng chung và bỏ lần cuối | Gộp request CSRF đang chạy; typing hẹn lượt cuối có giới hạn, hủy theo phòng/phiên | Lượt browser đăng ký bốn phiên; hồi quy Web 2 typing và logout |

## Luồng và giới hạn

Quyền nằm ở backend. Thành viên thường không xem được danh sách kiểm duyệt, không cấm/đuổi chủ; chủ không kiểm duyệt admin/chính mình. Sảnh không được đuổi/ban. Thời hạn cấm từ 1 giây tới 365 ngày; UI admin có thao tác gỡ riêng, không phải nhập -1. Cấm chat toàn hệ thống và từng phòng áp dụng độc lập; gỡ một phạm vi không gỡ phạm vi khác. Lý do tối đa 200 ký tự, hiển thị text an toàn. Lỗi ghi moderation rollback trạng thái và không phát thông báo thành công.

Thông báo trạng thái không giả làm tin chat đã lưu: sự kiện WS thuần nghiệp vụ. Nội dung chat mới vẫn qua socket UDP receive → kiểm quyền → lưu → WS. Thông báo realtime không phải hộp thư thông báo offline có lịch sử; trạng thái cấm hiện hành và lý do được đọc lại từ kho sau đăng nhập/reload/restart. Người bị kick không tự được thêm lại; người bị ban còn phải gỡ ban rồi tham gia theo quyền/mật khẩu/sức chứa. Khi reconnect, workspace được làm mới trước đồng bộ lịch sử để bỏ quyền phòng đã mất.

Feedback tối đa 3 mục DOM/3 timer và 64 ID chống lặp; giới hạn chiều cao 40dvh, đóng được bằng bàn phím. Notice riêng giữ tới khi đóng hoặc bị mục mới thay thế; logout dọn DOM/timer/ID. Composer có banner riêng nên đóng toast không che mất trạng thái cấm.

## Bản đồ file/hàm

| File/hàm | Vai trò |
|---|---|
| `server/KiemDuyet.Rule/danhSach/lyDoMute` | Đọc lệnh còn hạn, phân biệt global/phòng và lấy lý do |
| `server/WebWorkspace.Member/Room/restrictions/moderate` | Snapshot trạng thái, API lệnh theo quyền, lý do/giới hạn, bảo vệ Sảnh |
| `server/WebChatService.memberNotice/action/adminAction` | Sự kiện theo người nhận và hoạt động phòng sau thành công; audit kiểm duyệt |
| `server/WebAdministration.moderate` | Kết nối ngay chủ thay thế khi admin đuổi/ban chủ hiện tại |
| `web/ChatSocket.event` | Notice đích kiểm phiên + userId, sự kiện phòng vẫn kiểm quyền đọc |
| `web/WebApi` | GET `/api/moderation?roomId=...`, cùng session/CSRF/Origin đang có |
| `static/feedback.js` | Nội dung phản hồi, quản lý toast/ID/timer/focus dialog |
| `static/workspace-ui.js`, `admin-ui.js` | Lý do, danh sách cấm, gỡ đúng ID, chọn thao tác/phạm vi, quản lý từng dòng thành viên admin |
| `static/app.js`, `index.html`, `app.css` | Notice riêng, banner composer, reconnect workspace, single-flight CSRF và typing có hạn |

## Kết quả thực chạy

| Nhóm | Kết quả |
|---|---|
| Maven/JUnit | **30 test, 0 lỗi/thất bại/bỏ qua**, gồm 4 ca WebMemberModerationTest mới: quyền/UDP, notice sau mất quyền, lỗi đĩa, thời hạn/global/restart, chủ thay thế. [JSON](screenshots/member-moderation/java-results.json) |
| Desktop AdminSelfTest main riêng | **16 PASS / 0 FAIL** |
| Browser thành viên, JAR thật | **11 nhóm PASS** trên Edge 158.0.4370.33, bốn context độc lập. [JSON](screenshots/member-moderation/browser-results.json) |
| Browser UX / Web 2 / tệp | **12 / 11 / 8 nhóm PASS**. [UX](screenshots/member-moderation/regression-ux/ux-results.json), [Web 2](screenshots/member-moderation/regression-web2/browser-results.json), [tệp](screenshots/member-moderation/regression-files/browser-results.json) |
| Browser admin/PDF, JAR thật + restart | **12 nhóm PASS**, báo cáo cùng snapshot và dữ liệu giữ sau restart. [JSON](screenshots/member-moderation/regression-admin/package-results.json) |
| Artifact | Tất cả main Java/resources trùng source build riêng, mọi class production trùng byte với Maven; không chứa fixture. SHA-256: `f9d952aac35675d023bb2b48c72abce3bf3e838cf50eb148f36d157b6ae4b6b9` |

Ảnh thật: [cấm chat và nháp](screenshots/member-moderation/01-member-muted-light.png), [danh sách cấm](screenshots/member-moderation/02-owner-restrictions.png), [bị đuổi](screenshots/member-moderation/03-kicked-notice.png), [mobile tối](screenshots/member-moderation/04-mobile-notice-dark.png). Không có kiểm bàn phím ảo trên điện thoại vật lý. PASS chỉ nói phạm vi đã chạy.

Build riêng: `.selftest/member-build-795fffeddd`; artifact `target/member-preview/UDPmultiChat-1.0-SNAPSHOT.jar`. Script mới `python -X utf8 scripts/member-moderation-browser-qa.py` nhận `CHAT_QA_JAR` hoặc `CHAT_QA_CLASSPATH`; dữ liệu tự tạo trong `.selftest`. Các script cũ nhận `CHAT_QA_OUTPUT` để lưu hồi quy riêng; admin QA thêm `CHAT_QA_PDF_DIR` để không ghi đè PDF mẫu đã bàn giao.

## Bản xem thử và 5 bước thử

Bản mới **http://127.0.0.1:8085**, admin `/admin`; cookie riêng `UDPCHAT_MEMBER`. Ba tài khoản admin/chuphong/thanhvien có mật khẩu ngẫu nhiên chỉ ở `.selftest/member-preview-8f11edd4f7/login-local.txt` (ignored). Phòng thử đã tạo và hai tài khoản đã tham gia; đã logout sau khi kiểm setup. Dữ liệu cũ/preview 8084 giữ nguyên; phiên bản mới ở 8085. [Kết quả preview](screenshots/member-moderation/preview-results.json).

1. Dùng hai phiên độc lập đăng nhập chuphong và thanhvien, chọn **Phòng thử quản lý thành viên**.
2. Chủ mở Thông tin phòng → Quản lý thành viên → Cấm chat, nhập thời hạn/lý do; bên thành viên thấy notice và banner, vẫn đọc và giữ nháp.
3. Chủ mở Danh sách cấm/gỡ cấm và gỡ cấm chat; nút gửi bên kia mở lại. Thử cấm 5 giây để xem tự hết hạn.
4. Thử đuổi rồi tham gia lại; thử ban và kiểm bị chặn vào lại, gỡ ban rồi mới tham gia. Quan sát thông báo dù người nhận đã mất quyền phòng.
5. Thử chuyển chủ; admin `/admin` có kiểm duyệt theo dòng thành viên, global mute và danh sách gỡ cấm toàn hệ thống. Không thử phá dữ liệu thật.

Backup/nhập bản sao/rollback, QA thiết bị thật và HTTPS/host đích còn ở phần tiếp theo của đặc tả. Chưa có đích/quyền push/public.
