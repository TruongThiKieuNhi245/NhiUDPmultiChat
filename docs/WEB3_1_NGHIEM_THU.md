# Web 3.1 — Tệp/ảnh và giao diện gradient

Ngày 25/11/2026. Tiếp quản HEAD `be607ec` (Web 2), sau các mốc `939662b` và `c3ed958`. Đây là mốc tệp/ảnh trong Web 3; chưa nghiệm thu toàn bộ Web 3 hay Web 4. Không push/public. Giữ sáu file tài liệu có sửa đổi trước khi bắt đầu; chỉ bổ sung phần cập nhật mới vào README và bản bàn giao, không đưa phần sửa có sẵn vào commit của mốc này.

## Vấn đề thực tế và thay đổi

| Vấn đề thực tế | Nguyên nhân | Thay đổi | Kiểm chứng |
|---|---|---|---|
| Chưa gửi/tải tệp trên web | Web 2 chỉ có T/S | HTTP multipart xác thực, metadata F qua UDP CHAT2; thẻ tệp và ảnh | Tệp 5 MB khớp byte/CRC, dừng UDP không có bản ghi/broadcast |
| Upload dễ bị nhầm là gửi thành công | Hai giai đoạn có kết quả khác nhau | “Đã tải lên · Chưa gửi vào hội thoại”; bấm Gửi vào phòng, sau commit mới “Đã gửi” | UI và lịch sử kiểm độc lập trước/sau UDP |
| Cần hủy, thử lại và bảo toàn ID | Nhị phân HTTP và hàng chat có vòng đời riêng | XHR progress/abort, giữ File để thử upload lại; hàng chat retry giữ clientMsgId và metadata | Hủy thật khi upload bị giới hạn tốc độ; lỗi UDP/retry cùng hàng |
| Tệp phải tuân theo quyền phòng và cleanup | Không được tin đường dẫn/metadata từ client | UUID vật lý, tên an toàn, quota, kiểm lại quyền khi hoàn tất và GET; thu hồi/xóa phòng dọn binary | Người ngoài/anonymous/kick/logout bị từ chối; test restart/lỗi đĩa/TTL |
| Nút nhạt, theme chưa rõ phân cấp | Màu phẳng, icon chữ không đồng nhất | Gradient tím–indigo, nền/viền nút rõ, SVG tự viết, composer mobile hai hàng | Ảnh Edge thật; kiểm token tương phản, 360 px và zoom CSS |

## Luồng và bản đồ source

```text
HTTP multipart → stream/checksum → lưu tệp + metadata tạm (chưa có tin)
WS send F → UDP send → DatagramSocket.receive → session/quyền/metadata/chống trùng
→ ghi tin F thành công → WS message/confirmed → thẻ tệp được tải theo quyền
```

Không thêm đường HTTP lưu tin chat. Payload F dùng `TepTin.dongGoi()` gồm UUID, tên an toàn, số byte và CRC32; tối đa 1000 byte nội dung và 1399 byte toàn gói vẫn áp dụng. Tác giả lấy từ phiên backend; hướng tin vẫn so userId. Upload không được coi là xác nhận gửi. Một tệp đã tạo tin không thể được gắn vào mã tin khác để sinh thêm hàng; retry đúng mã/nội dung trả bản ghi cũ, payload khác bị từ chối. Digest thu hồi tiếp tục kiểm retry khi binary đã xóa.

| File/hàm | Trách nhiệm |
|---|---|
| `server/WebFiles.begin/write/finish/validate/open/cleanup` | Reservation/quota, stream CRC, ảnh hợp lệ/thumbnail, metadata đối chiếu, cleanup |
| `server/KhoTep.entries` | Danh sách metadata dùng chung; giữ định dạng và logic desktop |
| `server/WebChatService.beginUpload/finishUpload/download/discardUpload` | Phiên, giới hạn thao tác và quyền nghiệp vụ; kiểm lại quyền sau I/O |
| `WebChatService.receive/message/recall/action/export` | Chấp nhận F sau UDP, projection tên/kích thước/ảnh, thu hồi, xóa phòng và TXT |
| `transport/UdpChatTransport.send/receive` | Cho phép F trong CHAT2; giữ socket thật, nonce, retry và trace theo clientMsgId |
| `common/model/WebFile`, `WebMessage.file` | DTO công khai không chứa đường dẫn vật lý |
| `web/WebFileApi` | POST `/api/files/upload?roomId=…` multipart `file`; GET `/api/files/{id}` và `?thumbnail=true`; DELETE tệp chưa gửi |
| `web/WebSecurity`, `WebApi.Errors` | Upload yêu cầu session + header CSRF trước parse; giới hạn 11 MiB request, 10 MiB/file, lỗi quá cỡ trả 413 |
| `static/file-upload.js` | Chọn/preview, progress, hủy, thử lại, gửi vào phòng, loại callback phiên cũ |
| `static/chat-view.js`, `workspace-ui.js` | Thẻ tải tệp, kích thước byte/KB/MB, thumbnail cố định; outbox hiện tên tệp |
| `static/index.html`, `app.css`, `app.js`, `chat-store.js` | SVG, token/gradient sáng tối, composer mobile, nối module, xóa metadata khỏi cache khi thu hồi |

## Giới hạn và chính sách tệp

- File tối đa 10 MiB (10 × 1024 × 1024 byte), không nhận 0 byte. Request multipart tối đa 11 MiB và spool xuống đĩa; không nạp toàn bộ file vào RAM backend. Stream sao chép dùng buffer 32 KiB. HTTP download stream, không dùng `readAllBytes` trong production.
- Loại cho phép: TXT, CSV, JSON, PDF, ZIP, DOCX/XLSX/PPTX, ODT/ODS/ODP, RTF, PNG/JPG/JPEG và BIN. Binary luôn tải dạng attachment/octet-stream + nosniff; không phục vụ HTML/SVG/script inline. Ảnh PNG/JPG phải decode được và dưới ngưỡng pixel của `AnhTep`; thumbnail JPEG tái mã hóa tối đa 240 px.
- Quota gồm cả file đã gửi và file upload chờ: 50 MiB/tài khoản, 100 MiB/phòng, 512 MiB/toàn kho, tối đa 1024 file. Reservation sao chép tối đa 2/tài khoản, 8/toàn dịch vụ; giới hạn multipart/parser và số HTTP worker vẫn áp dụng trước bước này.
- Tệp upload chưa gửi giữ tối đa 24 giờ, có thể dùng metadata cũ để gửi sau restart. Đóng dialog sau upload sẽ yêu cầu hủy tệp chưa gửi. Nếu response mất trước khi browser biết UUID, cleanup TTL xử lý phần còn lại. Chạy cleanup khi bắt đầu upload, khi startup và mỗi giờ; thư mục stream tạm được dọn sau startup.
- GET kiểm membership/ban và tin F còn tồn tại; không cho tải tệp upload chờ, của phòng khác hoặc đã thu hồi. Quyền được kiểm trước khi mở stream; dữ liệu đã tải về thiết bị không bị thu hồi ngược.
- Thu hồi tombstone trước rồi dọn binary/thumbnail/metadata. Tệp không còn tin F bị chặn GET ngay cả khi cleanup gặp lỗi đĩa; orphan được dọn theo TTL hoặc khi phòng đã bị xóa. Lỗi cleanup không làm API báo sai kết quả nghiệp vụ đã commit; ghi log kỹ thuật theo UUID và đưa vào hàng dọn lại (tối đa 1024 UUID tệp và 1024 mã phòng). Khi tiến trình còn chạy, thử dọn lại mỗi giờ hoặc khi có upload mới; qua crash/restart dùng quy tắc orphan/TTL ở trên.
- CRC32 để kiểm toàn vẹn, không dùng làm xác thực nguồn. Tên vật lý là UUID, không dùng tên do client gửi làm đường dẫn. Không giải nén ZIP do người dùng tải lên.
- Preview chỉ dùng tài nguyên same-origin hoặc data URL cho File đang chọn. Không thêm thư viện/frontend framework, tài nguyên hay giấy phép mới. Giữ 36 PNG Noto/catalog/LICENSE/NOTICE.

## Kiểm chứng

| Nhóm đã thực sự chạy | Kết quả và bằng chứng |
|---|---|
| Maven JUnit | 20 ca, 0 lỗi/0 thất bại (chạy cả workspace và bản build đóng gói riêng): UDP transport 2, Web 1 tích hợp 5, Web 2 service 7, Web 3 file 6; [chi tiết](screenshots/web3-files/java-results.json) |
| Edge hai/ba context độc lập | 8 nhóm tệp: 5 MB/CRC/byte, upload hủy/thử lại, UDP dừng/retry cùng hàng, trùng tên, quyền/CSRF/10 MB/type, thumbnail/thu hồi/kick; [kết quả](screenshots/web3-files/browser-results.json) |
| UX hồi quy | 12 nhóm, gồm hình học A phải/B trái và chiều ngược, pending/lỗi/retry/lịch sử/reconnect, 680 tin, anchor/selection và cache 300; [kết quả](screenshots/web3-files/regression-ux/ux-results.json) |
| Web 2 hồi quy | 11 nhóm phòng/lời mời/hồ sơ/sticker/reply/search/thu hồi/outbox/mật khẩu; [kết quả](screenshots/web3-files/regression-web2/browser-results.json) |
| JAR thực tế | 9 nhóm, chạy `java -jar`, tải 5 MB khớp byte/CRC, restart giữ file/phòng/thu hồi, endpoint fixture 404; [kết quả + SHA](screenshots/web3-files/packaged/package-results.json) |
| Desktop SelfTest | 75 PASS, 0 FAIL trên fixture riêng; [tổng hợp](screenshots/web3-files/java-results.json) |

Edge phiên bản 158.0.4370.33 chạy headless thật. Ảnh chụp sau 350 ms; desktop 1440×960, tablet 900×900, mobile viewport 360×800, sáng/tối và zoom CSS 125–150%. Không phải thử bàn phím ảo/thiết bị thật. Các cặp token text/muted/error/nút/gradient đã đo đạt tối thiểu 5,04:1 (sáng), 5,13:1 (tối); đây là phạm vi token được đo, chưa phải audit accessibility toàn ứng dụng.

Ảnh cuối từ JAR: [desktop sáng](screenshots/web3-files/packaged/desktop-light.png), [desktop tối](screenshots/web3-files/packaged/desktop-dark.png), [mobile sáng](screenshots/web3-files/packaged/mobile-light.png), [mobile tối](screenshots/web3-files/packaged/mobile-dark.png), [drawer](screenshots/web3-files/packaged/mobile-drawer.png), [đăng nhập gradient](screenshots/web3-files/packaged/auth-gradient.png). Ảnh lỗi UDP thật: [hàng tệp chờ gửi lại](screenshots/web3-files/file-retry-error.png).

Lệnh chạy lại từ thư mục repo (Maven offline dùng cache đã có):

```powershell
$maven = 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd'
& $maven -q -o test dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
python scripts/web3-files-browser-qa.py
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web3-files/regression-ux'
python scripts/web1-ux-browser-qa.py
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web3-files/regression-web2'
python scripts/web2-browser-qa.py
$env:CHAT_QA_JAR = 'target/web3-files-preview/UDPmultiChat-1.0-SNAPSHOT.jar'
$env:CHAT_QA_OUTPUT = 'docs/screenshots/web3-files/packaged'
python scripts/web3-files-browser-qa.py
Remove-Item Env:CHAT_QA_JAR, Env:CHAT_QA_OUTPUT
$testClasspath = 'target/test-classes;target/classes;' + (Get-Content target/web-classpath.txt -Raw).Trim()
java -cp $testClasspath com.mycompany.udpmultichat.SelfTest
```

Các script tạo `.selftest/<UUID>` và cổng riêng, tự tắt tiến trình của mình. Cần Python Playwright + Edge và Pillow để chạy browser QA. Không chạy Maven đóng gói đè JAR đang mở: dùng bản sao pom/src riêng trong `.selftest`, rồi sao artifact đã kiểm sang thư mục preview.

Bản xem thử chỉ localhost: http://127.0.0.1:8083. Artifact `target/web3-files-preview/UDPmultiChat-1.0-SNAPSHOT.jar`; dữ liệu mới `.selftest/web3-files-preview-c285ede398/data`. Không dùng `data/` hay `web-data/` vận hành. Tạo tài khoản ở form để thử; không cài mật khẩu mẫu vào bản chạy này. Metadata tiến trình ở `target/web3-files-preview.json` (không đưa Git).

Đã chạy lại 20 JUnit ngay trong bản build đóng gói riêng; 209 class production khớp byte với JAR bàn giao. Source copy và toàn bộ static resources cũng được đối chiếu với workspace.

SHA-256 artifact: `085a2d7b07255e7908d7503ff7c37822a19911bd1ee2123a7dd6ceca10d68818`.


## Thử tay

1. Mở bản xem thử bằng hai profile hoặc một cửa sổ thường và một cửa sổ riêng tư; tạo hai tài khoản rồi cùng vào một phòng.
2. Gửi text hai chiều, đổi sáng/tối và thu cửa sổ về 360 px; kiểm bong bóng của mình bên phải, nội dung chữ căn trái, drawer đóng trả focus.
3. Chọn tệp tối đa 10 MB, thử hủy/thử lại upload. Khi thấy “Đã tải lên · Chưa gửi vào hội thoại”, phòng bên kia chưa có tin. Bấm Gửi vào phòng, chờ “Đã gửi”, tải tệp ở phiên kia.
4. Gửi ảnh PNG/JPG và tệp nhỏ, kiểm thumbnail, tên/kích thước, tải xuống và lịch sử sau reload. Thu hồi ảnh rồi kiểm mất thẻ/tải tệp.
5. Thử gửi 0 byte, hơn 10 MB hoặc HTML; xem lỗi ngay trong dialog. Kiểm hộp Tin chờ/lỗi nếu mất kết nối và gửi lại khi kết nối phục hồi.

## Phần còn lại

Mốc sau của Web 3: giao diện quản trị năm mục và nghiệp vụ admin, snapshot thống kê, PDF A4 tiếng Việt + bộ 4 CSV/1 HTML, nhật ký riêng, công cụ nhập từ bản sao/backup/rollback. Kho hiện vẫn là file của desktop, một instance, tối đa 10.000 tin mới/phòng; chưa đổi DB hoặc nhập dữ liệu vận hành. Web 4 còn QA thiết bị thật/bàn phím ảo, rà accessibility đầy đủ, HTTPS/WSS và kiểm chứng hosting chính thức; chưa push/public. Các số đo viewport/zoom ở đây là mô phỏng browser, không đại diện thiết bị thật.
