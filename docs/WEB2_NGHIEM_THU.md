# Nghiệm thu Web 2 — phòng và tiện ích chat

Ngày 25/11/2026. Baseline `939662b` (UX Web 1). Giữ sáu file tài liệu người dùng đang sửa; chỉ commit phần bổ sung của chặng này trong README/BAN_GIAO_AGENT. Không nhập/sửa `data/`, `web-data/`, `reports/` hoặc dữ liệu desktop. Fixture riêng trong `.selftest/`; không push/public.

## Phạm vi và ánh xạ

| Nghiệp vụ | Source/hàm | Cách kiểm chứng |
|---|---|---|
| Tạo/sửa/đóng/mở/xóa, công khai/riêng tư, mật khẩu, mã và giới hạn | `WebWorkspace.action` gọi `QuanLyPhong`; `/api/actions/room-*` | Quyền chủ/admin, Sảnh bảo vệ, phòng riêng không lộ mã; UI tạo/join và hai phòng cách ly |
| Thành viên, biệt danh, chuyển chủ/rời, kick/mute/ban/gỡ | `WebWorkspace.members/moderate`, quản lý phòng/kiểm duyệt chung | userId bất biến; biệt danh theo roomId/userId; mute chặn gửi nhưng vẫn đọc, kick/ban thu hồi quyền đọc/gửi |
| Lời mời offline, nhận/từ chối/hủy/hết hạn | `QuanLyLoiMoi`, `QuanLyPhong.chapNhanLoiMoi` | Không giữ chỗ trước khi nhận; kiểm lại người mời, ban, phòng mở, capacity; hai accept đồng thời chỉ một người được chỗ cuối |
| Hồ sơ, ảnh, mật khẩu, mật khẩu bắt buộc đổi | `WebWorkspace.saveProfile`, `QuanLyNguoiDung` và `PhanManh` | Login cố định; preview/xóa avatar; PNG 96×96 được backend kiểm; hash web và trạng thái qua restart |
| Sticker/reply | `UdpChatTransport.send/receive` CHAT2; `WebReply`; `WebChatService.receive/message` | 36 PNG Noto, tìm/nhóm/rỗng; code sticker và replyTo qua datagram thật; reply chỉ lưu ID nguồn |
| Thu hồi, copy, về tin gốc, tìm kiếm, TXT | `WebChatService.recall/search/around/export`, `ChatView`, `WorkspaceUI` | Không lộ nội dung thu hồi trong lịch sử, trích dẫn, kết quả tìm và TXT; phân trang tìm 50 kết quả |
| Typing, tắt thông báo, bản nháp/unread/outbox | `ChatSocket` và `app.js`, `chat-store.js`, `workspace-ui.js` | Typing giới hạn và tự hết hạn; draft RAM theo phòng; hàng lỗi không mất khi đổi phòng; retry cùng mã; bỏ hàng cục bộ có giải thích |

## Kiến trúc và quyền

Giữ Java 21, Maven, Spring Boot và `com.mycompany:UDPmultiChat:1.0-SNAPSHOT`. Browser dùng HTTP/WS cùng origin; cấu hình HTTPS/WSS và public hosting thuộc Web 4.

```text
WS đã xác thực → UDP CHAT2 (nonce, token nội bộ, roomId, clientMsgId, text, kind, replyTo)
→ DatagramSocket.receive thật → kiểm phiên/quyền/mute/ban/chống trùng
→ ghi lịch sử thành công → phát WS đúng thành viên của phòng
```

Không có endpoint HTTP tạo tin chat mới. HTTP xử lý quản lý phòng/hồ sơ/lời mời, đọc lịch sử và thay trạng thái thu hồi. Không dùng ACK socket làm “Đã gửi”. `service.deliver` giữ thứ tự projection/phát và thu hồi, không cho bản broadcast cũ khôi phục nội dung đã thu hồi. `WebMessage` là DTO hiển thị Web 2; danh tính người gửi luôn lấy từ phiên backend.

Tách quyền đọc và quyền gửi: phòng đóng/mute vẫn đọc được, ban/kick thì không. WS gắn với phiên, không gắn cứng vào quyền gửi Sảnh; một WS nhận các phòng mà tài khoản đã tham gia. Tổng thành viên dùng membership bền vững; số online dùng phiên nghiệp vụ, không lấy số socket. Mã phòng riêng chỉ trả cho thành viên/người quản lý. Event invalidation chung không chứa tên/mã/nội dung phòng riêng.

Lời mời không còn bị xóa trước khi kiểm nhận. Đã sửa rollback RAM khi ghi lời mời/biệt danh lỗi. Reply mới lưu `«reply:msgId::»` và body, không sao chép text nguồn. Khi đọc, backend lấy trạng thái nguồn hiện tại. Thu hồi giữ digest SHA-256 trong `web-recall-digests.properties` để retry cùng mã sau thu hồi/restart vẫn đối chiếu payload, không lưu lại plaintext đã thu hồi. Reconnect quét lại danh sách tombstone từ ID 0 theo trang 50 ID, vì một lần thu hồi mới có thể tác động tới msgId cũ; không tải lại toàn bộ body lịch sử. Epoch loại phản hồi cũ đang bay khi WS đóng. Đã kiểm B offline, A thu hồi tin cũ, B nhận đúng tombstone và quote sau reconnect. Tombstone cũ không có digest bị từ chối gửi lại; vẫn xem được trong lịch sử. Import reply desktop cũ và công cụ chuyển dữ liệu thuộc Web 3.

Phiên buộc đổi mật khẩu được phép đăng nhập/đổi mật khẩu/đăng xuất, chưa được vào nghiệp vụ chat. Dialog không đóng bằng Escape trong trạng thái đó. Profile/ảnh dùng cùng kho người dùng, không tạo repository web thứ hai. Logout đóng WS và chặn callback cũ ở cả app lẫn dialog.

## API và bản đồ file

- `server/WebWorkspace.java`: snapshot phòng/profile/lời mời, action và kiểm quyền nghiệp vụ, avatar, preference, digest recall.
- `server/WebChatService.java`: vòng đời phiên, restore membership khi login, tách đọc/gửi, projection quote/nickname/avatar, tìm kiếm/around/recall/TXT, phát event sau mutation thành công.
- `transport/UdpChatTransport.java`: giữ socket loopback/nonce/source/payload/retry; thêm kind/replyTo vào datagram CHAT2. Multicast desktop vẫn giữ nguyên để đối chiếu môn học.
- `web/WebApi.java`: `/workspace`, `/actions/{action}`, `/members`, `/users/search`, `/invitations/sent`, `/avatars/{id}`, `/stickers`, `/stickers/{code}.png`, `/history/page`, `/history/around`, `/search`, `/recalls`, `/recall`, `/export`. Giữ `/csrf`, `/register`, `/login`, `/me`, `/logout`, `/history`, `/health`.
- `web/ChatSocket.java`: ready/ping/send/typing, confirmed/message và workspace/recall events; kiểm lại quyền phòng trước mỗi phát.
- `web/BoundedJsonRequest.java`, `WebSecurity.java`: giới hạn cả body chunked và Content-Length, CSRF/Origin/session/CSP, parser từ chối trailing tokens. Body thường 8192 byte; profile 96000 byte; 30 mutation/10 giây/phiên. Không tăng frame WS 8KiB hoặc pending UDP 256.
- `static/workspace-ui.js`: phòng, lời mời, hồ sơ/avatar, đổi mật khẩu, thành viên/kiểm duyệt, sticker, search, outbox bằng dialog native; focus/Escape/restore và guard phiên.
- `static/app.js`, `chat-store.js`, `chat-view.js`: đổi phòng với epoch chống request cũ, queue pending theo khóa room/user/clientMsgId, draft theo phòng, quote/sticker/actions, tombstone monotonic.

## Giới hạn tài nguyên và dữ liệu

Cache tin đã lưu 300, pending toàn tab 100, draft 20 phòng, danh sách recall gần đây 1024 ID. Search hiển thị mỗi trang 50; không cộng dồn DOM/RAM tất cả kết quả. Lời mời tối đa 4096 toàn kho, 128/phòng. Room/core hiện có tối đa 254 phòng, sức chứa phòng tạo mới 2–500; Sảnh giữ giới hạn riêng. Backend vẫn dùng file/kho desktop nạp RAM, tối đa 10.000 tin mới/phòng; chưa phải DB lớn. Quota không phải công cụ import dữ liệu vận hành.

Bản nháp và pending chỉ trong RAM tab, mất khi đóng tab/logout. “Bỏ hàng trên thiết bị” chỉ bỏ hàng cục bộ, không thu hồi một tin có thể đã đến server; muốn thu hồi phải dùng nghiệp vụ Thu hồi. Khi không còn quyền phòng, outbox vẫn cho xem/sao chép/bỏ hàng cục bộ.

Avatar chọn PNG/JPG tối đa 2MB, browser crop thành PNG 96×96; backend kiểm định dạng/kích thước trước ghi. Ảnh có kích thước hiển thị cố định để giảm nhảy cuộn. Noto PNG/catalog/LICENSE/NOTICE được tái sử dụng, không thêm thư viện frontend hay tải tài nguyên mới.

## Kiểm thử và bằng chứng

Kết quả máy đọc được ở [screenshots/web2](screenshots/web2/). Chỉ tính PASS cho lần thực chạy có JSON/log tương ứng. Mobile/tablet/zoom là mô phỏng Edge; chưa kiểm điện thoại, bàn phím ảo hay screen reader vật lý. Snapshot ảnh không thay thử tương tác.

Các lượt đầu đã bắt và sửa lỗi whitelist tên kho, focus sau tìm kiếm, phân biệt ping với message trong test, trạng thái snapshot tham gia phòng và giới hạn body chunked trả 413. QA đợi dialog đóng trước nhập composer; input phía nền bị dialog native chặn là hành vi đúng. Log lỗi cũ giữ tại `target/web2-*-fail.txt`/fixture, không coi là PASS.

Lệnh (Maven NetBeans của máy):

```powershell
$mvnChat = 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd'
& $mvnChat -q -o test-compile dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
& $mvnChat -q -o test
python -X utf8 scripts/web2-browser-qa.py
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web2/ux-regression'
python -X utf8 scripts/web1-ux-browser-qa.py
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web2/web1-regression'
python -X utf8 scripts/web1-browser-qa.py
# Sau package hoac chi dinh JAR rieng neu JAR target dang duoc su dung:
$env:CHAT_QA_JAR = 'target/UDPmultiChat-1.0-SNAPSHOT.jar'
python -X utf8 scripts/web2-package-smoke.py
```

Maven không tự chạy các main SelfTest desktop. Các lệnh và baseline desktop ở README. Artifact Web 2 được dựng trong bản sao source dưới `.selftest/web2-package-*` để không ghi đè JAR đang chạy; checksum/đường dẫn thật nằm trong package-results.json. Không đóng gói các controller/seed fixture vào JAR sản phẩm.

## Kết quả lượt cuối

| Kiểm tra | Kết quả |
|---|---|
| Maven/JUnit | 14 test, 0 failure, 0 error (2 transport + 5 tích hợp HTTP/WS + 7 nghiệp vụ Web 2) |
| Browser Web 2 | 11 nhóm PASS, ba tài khoản và phiên bổ sung kiểm mật khẩu bắt buộc đổi |
| Hồi quy UX | 12 nhóm PASS, hình học hai phía/cùng tên, 680 tin, cache 300, sai lệch anchor tối đa 0px |
| Hồi quy browser Web 1 | 11 nhóm PASS |
| Desktop | 75 PASS/0 FAIL; M3cChat 2 PASS; M3cThanhVien 3 PASS; đều exit 0 |
| Maven package/JAR restart | Exit 0; 7 nhóm smoke PASS, phòng/profile/avatar/lời mời/quote/sticker giữ qua restart |

[Tổng hợp test](screenshots/web2/test-results.json) · [browser Web 2](screenshots/web2/browser-results.json) · [UX/hình học](screenshots/web2/ux-regression/ux-results.json) · [Web 1](screenshots/web2/web1-regression/browser-results.json) · [artifact/checksum](screenshots/web2/package-results.json) · [đường UDP](screenshots/web2/web1-regression/udp-trace.txt).

Ảnh thực tế: [chat nhiều phòng](screenshots/web2/01-chat-nhieu-phong.png), [thành viên/lời mời](screenshots/web2/02-thanh-vien-loi-moi.png), [chat tối](screenshots/web2/03-chat-toi.png), [mobile](screenshots/web2/04-mobile.png), [drawer phòng](screenshots/web2/05-mobile-phong.png), [thông tin mobile](screenshots/web2/06-mobile-thong-tin.png). Ảnh viewport và CSS zoom bổ sung nằm trong thư mục ux-regression.

Bản xem thử riêng: **http://127.0.0.1:8082**. Tạo tài khoản mới để xem. Dữ liệu mới `.selftest/web2-preview-8180f85b/data`, không dùng thư mục vận hành. PID tại `target/web2-preview-pid.txt`; chỉ có hiệu lực khi tiến trình còn chạy. Chạy lại:

```powershell
java -jar target/web2-preview/UDPmultiChat-1.0-SNAPSHOT.jar --server.port=8082 --server.address=127.0.0.1 '--chat.data-dir=.selftest/web2-preview-8180f85b/data'
```

Checksum JAR xem thử khớp bản đã nghiệm thu: `8c6e607de7b04aca3440dd64582538cb9cf03efd4a2681c9808ee04ade1058c5`. Giữ nguyên server và dữ liệu Web 1; target/.selftest không đưa vào commit.

## Năm bước thử tay

1. Mở bản Web 2 ở cổng riêng, tạo A và B trong hai profile/ẩn danh. A tạo phòng riêng; B không thấy mã/phòng trước khi được cấp quyền.
2. A mời tài khoản chưa online; thử nhận khi phòng đầy/đóng rồi nhận sau khi đủ điều kiện. Kiểm tổng thành viên khác số online.
3. B đổi hồ sơ/avatar và biệt danh trong phòng. Gửi hai chiều, sticker và reply; copy và nhảy về tin nguồn. Thu hồi nguồn rồi tải lại lịch sử/tìm kiếm/TXT để kiểm nội dung không lộ lại.
4. Mở hai phòng, viết nháp rồi đổi phòng; thử typing, tắt thông báo, mute/gỡ/kick/ban, chuyển chủ. Ở 360px kiểm drawer/dialog và focus.
5. Chạy fixture tắt receiver UDP trong khi WS còn mở: tin lỗi ở đúng hàng/outbox; khôi phục và retry cùng mã. Chạy package smoke để restart JAR và nhận lời mời offline sau restart.

## Chặng kế tiếp

Web 2 là mốc phòng/hồ sơ/sticker/tiện ích, không phải toàn bộ sản phẩm hoàn thiện. Web 3 còn tệp 10MB qua HTTP (metadata UDP), admin 5 mục, audit/chống spam quản trị, thống kê/PDF tiếng Việt A4 và bốn CSV + HTML, persistence/import từ bản sao/backup/rollback. Web 4 còn QA thiết bị/accessibility đầy đủ, HTTPS/WSS và xác minh hai host 0 đồng bằng nguồn chính thức. Chỉ push/public khi có đích và quyền rõ ràng.
