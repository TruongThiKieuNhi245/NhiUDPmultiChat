# Bàn giao chặng UX trên Web 1

Ngày 25/11/2026. Tiếp quản HEAD `c3ed958`; có sáu file tài liệu người dùng đang sửa. Chỉ stage phần bổ sung của chặng này trong README và BAN_GIAO_AGENT, giữ các phần sửa có trước ngoài commit. Không nhập/sửa dữ liệu vận hành, không push/public và chưa bắt đầu Web 2–4.

## Vấn đề thực tế → nguyên nhân → thay đổi → kiểm chứng

| Vấn đề | Nguyên nhân source Web 1 | Thay đổi | Kiểm chứng |
|---|---|---|---|
| Mọi tin ở bên trái | Renderer chỉ thêm nhãn “Bạn”, không có căn phải | `message.userId === currentUser.id`; bong bóng của mình bên phải, chữ trái, người khác có tên/avatar và nút xem accountId | Hai BrowserContext trùng tên hiển thị, đo tọa độ A/B cả hai chiều; pending/lỗi/retry/lịch sử/reconnect |
| Tin ngắn trải cả hàng, khối chào chiếm vùng đọc | Body `flex:1`, không có bubble; welcome luôn hiện | Bubble co theo chữ, desktop tối đa 70%/580px, mobile tối đa 85%; welcome chỉ hiện khi rỗng | Tin một ký tự có chiều rộng dưới 80px, URL dài/Việt/emoji/nhiều đoạn; ảnh desktop/mobile |
| Tin mới làm dựng lại toàn DOM | `replaceChildren` mỗi sự kiện | Render theo khoá phòng + người gửi + clientMsgId (fallback msgId), chỉ thêm/cập nhật hàng cần thiết | Pending và confirmed cùng DOM node; selection giữ nguyên khi người khác gửi |
| Mở phòng quét từ đầu, cache tăng mãi | Vòng `after=0` và Map không giới hạn | Trang mới nhất 50 tin, tải trước cursor; cửa sổ 300 tin đã lưu + tối đa 100 pending; cắt cả cache, hàng DOM và separator cache | Fixture 680 tin: mở ở 631–680, tải về tin 1, cache/DOM tối đa 300 khi không pending, tải lại trang mới |
| Vị trí đọc dễ thay đổi | Không giữ anchor, cursor WS không tách rõ | Anchor hàng đang đọc; chỉ tự cuộn gần đáy/chủ động gửi; nút về cuối kèm đếm tin realtime | Sai lệch anchor tối đa trong JSON; reconnect bù tin mã nhỏ bị bỏ trong khi đã nhận mã lớn |
| Drawer không có nút đóng/focus | Chỉ transform sidebar | Đóng/Escape/trap Tab, inert nền, trả focus; nút chính 44px | Browser ở 360px, focus vào đóng và trả về nút mở |
| Composer cao, reconnect mất kiểm soát vòng đời | Textarea hai hàng, timeout không được quản lý tập trung | Một hàng tự giãn tối đa 130px, byte UTF-8, IME, bản nháp RAM; một timer reconnect, tối đa 8 lượt, backoff tối đa 10s và nút nối lại; generation chặn callback phiên cũ | Browser kiểm Shift+Enter/composition/quá byte, giữ nháp qua offline, logout, zoom và không lỗi JS |

## Bản đồ file/hàm

- `static/chat-store.js`: `ChatStore.key/put/trim/list/clear`, cache tách khỏi trạng thái phiên. Khoá có cả userId để clientMsgId của người khác không xóa pending của mình.
- `static/chat-view.js`: `ChatView.render/createRow/updateRow`, hướng duy nhất theo userId; nhóm liên tiếp cùng người/phòng/ngày, cách dưới hai phút; separator ngày và thời gian; nội dung qua `textContent`. Không hiện tên và avatar lặp cho tin của mình. Có nút mở định danh cho người trùng tên.
- `static/app.js`: `connect/scheduleReconnect/stopSession`, `latestPage/syncHistory`, `readingAnchor/render`, `sendItem/clearItemTimer/failItem`, `drawer/updateComposer`. Cursor tiến qua từng trang lịch sử; tin WS mã lớn không tự đẩy cursor vượt khoảng thiếu. Timer gửi được hủy khi nhận tin/đăng xuất/thử lại.
- `static/index.html`, `app.css`: header/composer cố định theo vùng còn hiển thị, timeline cuộn riêng, bảng màu sáng/tối, vùng chạm, giảm chuyển động, thông báo aria-live riêng thay cho đọc lại toàn lịch sử. VisualViewport hỗ trợ vùng bàn phím; chưa thay kiểm thiết bị thật.
- `web/WebApi.historyPage` → `server/WebChatService.historyPage`: `GET /api/history/page?roomId=sanh&before=0` lấy 50 tin cuối; `before=msgId` lấy tối đa 50 tin trước đó, tăng dần. Trái phòng/phiên bị chặn, cursor âm trả lỗi. `/api/history?after=` giữ hợp đồng đồng bộ cũ. `WebSecurity` cho phép tải hai JS module tĩnh cùng origin.
- `Web1BrowserFixture.seedUx`: dữ liệu QA 680 tin và hai tài khoản cùng tên, chỉ dùng khi có `--fixture.ux=true`, bắt buộc thư mục mới. Không có seed hoặc endpoint điều khiển trong artifact sản phẩm.
- `Web1IntegrationTest.historyPagesAreBoundedOrderedAndAuthorized`: kiểm biên phân trang và quyền. Các test UDP/quyền/idempotency/lỗi đĩa/restart được giữ.
- `scripts/web1-ux-browser-qa.py`: QA mới đo hình học và tương tác. Hai script cũ thêm `CHAT_QA_OUTPUT`; package smoke thêm `CHAT_QA_JAR` để thử artifact từ bản sao source mà không chạm JAR đang được sử dụng.

## Tính trung thực và giới hạn

“Đã kết nối” chỉ mô tả kênh realtime, có giải thích ở title. “✓ Đã gửi” chỉ từ tin đã commit nhận qua WS/lịch sử. Receiver UDP dừng thì pending chuyển lỗi; không có bản ghi/broadcast. Retry giữ nguyên roomId, userId, nội dung và clientMsgId; lỗi ở cạnh tin, nút dùng được bằng bàn phím. Không có nhãn đã đọc/xem.

Giữ backend Java 21, Maven, Spring Boot và groupId/artifactId; chat mới vẫn theo `ChatSocket` → DatagramSocket send/receive thật → quyền/chống trùng → `KhoLuuTru.ghiTin` thành công → WS. HTTP chỉ đọc trang lịch sử mới, không tạo đường ghi chat.

Đã đối chiếu PDF thực tế ở `../tailieu`: UDP 40 trang (12,22,25), multicast 14 trang (11). Đường thực tế tại workspace là `../tailieu`, khác đường tuyệt đối tài liệu trong environment không tồn tại. Web dùng unicast loopback; multicast desktop là nhánh minh chứng riêng.

Cache browser tối đa 300 tin đã lưu + 100 pending và các tham chiếu DOM tương ứng. Khi đọc cửa sổ cũ đã đầy, tin mới được báo bằng nút về cuối, không đẩy hàng đang đọc ra khỏi cache. Chuyển về cuối tải lại trang cuối 50 tin. Backend vẫn dùng kho desktop nạp RAM, quota Sảnh 10.000 tin; chưa phải DB lớn. Bản nháp/hàng chưa xác nhận chỉ trong RAM tab và bị xóa khi logout; không bền qua đóng tab. Chưa có sticker/reply/tệp/tin hệ thống trên web để tuyên bố nghiệm thu hướng của các loại đó.

## Chạy và kiểm thử

```powershell
$mvnChat = 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd'
& $mvnChat -q -o test-compile dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
& $mvnChat -q -o test
python -X utf8 scripts/web1-ux-browser-qa.py
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web1-ux/regression'
python -X utf8 scripts/web1-browser-qa.py
# Dừng tiến trình đang giữ JAR đích trước package, hoặc build từ bản sao source riêng.
& $mvnChat -q -o package
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web1-ux'
python -X utf8 scripts/web1-package-smoke.py
```

Kiểm thử dùng fixture riêng. Mobile/tablet và bàn phím composition là mô phỏng browser; zoom là CSS 125/150%, chưa phải nút zoom trình duyệt hoặc thiết bị vật lý. Chưa kiểm bàn phím ảo thật, screen reader thật, HTTPS/WSS, browser ngoài Edge hay cloud. Lượt QA đầu phát hiện nút gửi nằm ngoài viewport khi CSS zoom 125%; đã sửa chiều cao theo zoom/VisualViewport, giữ `ux-first-run-fail.json` và chạy lại. Không tính lượt lỗi là PASS.

Maven JUnit chạy thành công nhưng `package` tại target ban đầu lỗi rename do có sẵn tiến trình người dùng chạy `java -jar target/UDPmultiChat-1.0-SNAPSHOT.jar`. Không dừng tiến trình đó. Artifact nghiệm thu được dựng từ bản sao source/POM trong `.selftest/ux-package-*`; đường dẫn thật ghi tại `package-results.json`. Không dùng bản JAR target không repackage được làm bản phát hành. Cảnh báo Mockito/Byte Buddy attach agent có trong test runtime, không phải compiler failure.

## Nguồn tham khảo

Tham khảo tổ chức bubble/header/footer từ [daisyUI Chat 5.8.2](https://daisyui.com/components/chat/), bố cục conversation/message input của [ChatScope](https://chatscope.io/) và luồng đọc hội thoại của [Zulip](https://zulip.com/help/reading-strategies), truy cập 25/11/2026. Tự viết HTML/CSS/JS; không nhập thư viện, sao chép code, font hay tài nguyên từ ba sản phẩm. Giữ nguyên tài nguyên và giấy phép Noto hiện có.

## Kết quả thực chạy

| Nhóm | Kết quả lượt cuối |
|---|---|
| Maven/JUnit | 6 test, 0 failure, 0 error: 4 integration + 2 transport |
| Browser UX mới | 12 nhóm PASS; hai context độc lập, cùng tên hiển thị |
| Browser Web 1 hồi quy | 11 nhóm PASS; kiểm giữ bản nháp và nút gửi trong viewport khi zoom |
| Fixture lịch sử dài | 680 tin; cache tối đa 300; sai lệch anchor tối đa 0px trong lượt đo |
| Desktop tham chiếu | M3cChatSelfTest 2 PASS; M3cThanhVienSelfTest 3 PASS; cả hai exit 0 |
| Artifact riêng | Maven package exit 0; JAR smoke PASS UI/WS/UDP, API fixture 404, restart giữ account/tin |

Kết quả: [UX JSON](screenshots/web1-ux/ux-results.json), [hồi quy JSON](screenshots/web1-ux/regression/browser-results.json), [JUnit/SelfTest tổng hợp](screenshots/web1-ux/test-results.json), [artifact/restart/SHA-256](screenshots/web1-ux/package-results.json), [log đường UDP](screenshots/web1-ux/regression/udp-trace.txt). Lượt lỗi zoom ban đầu: [JSON riêng](screenshots/web1-ux/ux-first-run-fail.json).

Ảnh sau khi hết transition: [desktop sáng](screenshots/web1-ux/01-desktop-sang.png), [desktop tối](screenshots/web1-ux/02-desktop-toi.png), [lỗi và gửi lại](screenshots/web1-ux/03-loi-va-gui-lai.png), [tablet 900px](screenshots/web1-ux/04-tablet.png), [mobile sáng](screenshots/web1-ux/05-mobile-sang.png), [mobile tối](screenshots/web1-ux/06-mobile-toi.png), [zoom 125%](screenshots/web1-ux/07-zoom-1.25.png), [zoom 150%](screenshots/web1-ux/07-zoom-1.5.png).

Bản xem thử riêng tại **http://127.0.0.1:8081**, dữ liệu mới `.selftest/ux-preview-data`, không có tài khoản/mật khẩu mẫu mặc định. Tạo tài khoản để xem. Tiến trình chỉ còn chạy nếu máy chưa dừng; PID ghi trong `target/ux-preview/pid.txt`. Khởi động lại bằng:

```powershell
java -jar target/ux-preview/UDPmultiChat-1.0-SNAPSHOT.jar --server.port=8081 '--chat.data-dir=.selftest/ux-preview-data'
```

JAR xem thử là bản sao có cùng SHA-256 với artifact đã smoke test (xem JSON). Thư mục target/.selftest không đưa vào commit. Build sau khi dừng JAR cũ dùng lại lệnh Maven thông thường trong README.

Rà soát cuối đã sửa các nhãn mới bị PowerShell chuyển sang ASCII khi pipe sang Python; QA kiểm lại nút đóng bằng accessible name tiếng Việt, vùng lịch sử không dùng aria-live và đường thử lại khi API lịch sử lỗi. Các thay đổi tài liệu có trước giữ nguyên.

## Năm bước thử tay

1. Chạy JAR mới bằng lệnh/đường dẫn artifact ở kết quả package, dùng data dir riêng và một cổng trống. Mở hai profile/ẩn danh, tạo hai tài khoản.
2. A gửi `A`, B trả lời nhiều dòng có tiếng Việt/emoji. Mỗi bên thấy tin mình bên phải, tin người kia bên trái; chữ trong bubble căn trái.
3. Cuộn lên đọc, gửi từ profile kia: không bị kéo xuống. Bấm Về tin mới. Với fixture 680 tin, dùng Tải tin cũ hơn rồi về cuối để xem phân trang.
4. Đổi sáng/tối; ở 360px mở drawer, đóng bằng nút/Escape và kiểm focus. Thử Shift+Enter, tin dài quá 1000 byte và tự giãn ô nhập.
5. Dùng script fixture tắt riêng receiver UDP: tin hiện lỗi, Gửi lại bằng Enter sau khôi phục; chỉ một hàng và một bản ghi. Offline/online giữ bản nháp và bù lịch sử.

Kết quả máy đọc được và ảnh nằm tại `docs/screenshots/web1-ux/`. Dừng sau chặng UX để người dùng xem trước khi mở rộng nghiệp vụ.
