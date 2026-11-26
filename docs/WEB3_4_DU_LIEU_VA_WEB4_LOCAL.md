# Web 3.4 — Dữ liệu, khôi phục và Web 4 local

Ngày 26/11/2026, tiếp quản `07f6ee2`. Giữ dữ liệu thật và sáu tài liệu đã sửa trước đó. Không push/public; kiểm HTTPS local không được coi là đã triển khai host.

| Vấn đề thực tế | Nguyên nhân | Thay đổi | Cách kiểm |
|---|---|---|---|
| Chưa có đường backup/khôi phục được kiểm chứng | Kho file nhiều tài nguyên, copy khi đang chạy có thể lệch snapshot | CLI offline giữ web.lock, kiểm định trên bản sao riêng, archive có manifest SHA-256 | JUnit kho đang chạy bị từ chối, byte-for-byte, tệp 5 MB, archive hỏng/CRC/traversal/đích tồn tại |
| Nhập từ bản sao chưa có báo kiểm định | Chưa có công cụ kiểm CSV/phòng/log/file trước đưa vào web | `import-copy` kiểm định và ghi vào thư mục hoàn toàn mới; báo số user/phòng/tin/tệp/bytes | Dữ liệu desktop có mật khẩu mặc định vẫn buộc đổi trên web; source copy không đổi byte |
| Chưa có rollback dữ liệu | Chỉ đổi JAR không khôi phục dữ liệu về checkpoint | Restore archive sang thư mục mới rồi đổi data-dir; giữ cả dữ liệu mới hơn và JAR cũ | JAR thật: ghi thêm sau restore, rollback về mốc hai tin, thư mục ba tin vẫn còn |
| Proxy HTTPS có thể làm lệch Origin/cookie | Scheme/host phía backend khác URL browser | Profile proxy loopback + Secure cookie + forwarded headers; mẫu Caddy/service | Edge qua TLS thật: HTTPS/WSS, UDP commit, HSTS/CSP, CSRF/Origin, binary khớp |
| Gỡ cấm trả về trễ mở lại dialog đã đóng | Callback tải lại danh sách không kiểm dialog hiện hành | Guard theo dialog còn mở ở chat và admin | Browser trì hoãn phản hồi 800 ms, Escape trong lúc chờ, dialog không tự mở lại |

## Công cụ dữ liệu

`server/WebDataTool` có `backup`, `verify`, `restore`, `import-copy`; chạy từ JAR đã đóng gói, không cần endpoint hoặc quyền admin giả trên web. Chỉ người có quyền đọc/ghi thư mục ở hệ điều hành dùng CLI này. Dừng server web trước backup; với desktop, dừng desktop server rồi tạo bản sao để nhập. Lock chỉ phối hợp với web, không tự dừng ứng dụng khác.

Công cụ kiểm manifest và SHA-256 từng tệp, CRC/kích thước binary, metadata/tin F cùng room/user, CSV/phòng/chủ/log/avatar và chính sách mật khẩu hiện có bằng validator tái sử dụng. Kiểm định chạy trên staging riêng để constructor của kho không viết vào nguồn. Giữ file của tác giả đã xóa nếu còn lịch sử hợp lệ. Log phòng đã xóa được giữ nguyên trong archive; số tin báo cáo là tin của phòng còn tồn tại.

Từ chối liên kết ra ngoài, đường dẫn lạ/traversal, entry thiếu/thừa/trùng, ZIP sai hash hoặc vượt quota. Giới hạn 1 GB toàn kho, 64 MB metadata/lịch sử, 20.000 tệp, manifest 8 MB và số đường dẫn quét có hạn; stream binary 32 KB. Chỉ các tên thuộc schema kho được nhận; tệp không biết làm kiểm định thất bại để người vận hành quyết định, không bị xóa hay bỏ qua âm thầm. Không sao lưu web.lock, upload tạm và phần `.tmp`/`.part` chưa commit.

Backup không ghi đè archive. Restore chỉ xuất bản thư mục đích chưa tồn tại sau khi kiểm định xong; không có lệnh ghi đè kho cũ. Rollback là chọn checkpoint được kiểm rồi chạy data-dir mới, giữ thư mục mới hơn để đối chiếu. Dữ liệu phát sinh sau checkpoint không có trong bản rollback. Cần đủ đĩa trống cho archive, staging và bản kiểm định; đây là backup offline, không phải giao dịch DB hoặc backup nóng. Kho vẫn một instance và các ghi nhiều file vẫn có giới hạn giao dịch như mốc trước.

Archive chứa dữ liệu chat và hash tài khoản; checksum chống hỏng dữ liệu, không xác thực người cung cấp và không mã hóa archive. Giữ archive ở thư mục riêng tư; `.gitignore` đã thêm `backups/`, fixture vẫn trong `.selftest/`. Không đẩy archive lên Git/static/public. Tài nguyên/license source không nằm trong data-dir và không bị công cụ dọn/xóa.

## Lệnh dùng

JAR local đã kiểm: `target/data-ready/UDPmultiChat-1.0-SNAPSHOT.jar` (SHA-256 `a8a9531ee0e8449696121d0384b638311c80e944a4f85b5baba9765fe2845c06`); build thông thường bằng Maven tạo `target/UDPmultiChat-1.0-SNAPSHOT.jar`. Không ghi đè một JAR đang chạy trên Windows. Công cụ dùng launcher có sẵn trong Boot JAR, không cần cài thêm thư viện runtime.

```powershell
$chatDataJar = (Resolve-Path 'target/data-ready/UDPmultiChat-1.0-SNAPSHOT.jar').Path
$chatDataMain = 'org.springframework.boot.loader.launch.PropertiesLauncher'
$chatDataClass = '-Dloader.main=com.mycompany.udpmultichat.server.WebDataTool'
# Thu muc backups phai nam ngoai data-dir; nguon da dung va dich chua ton tai.
New-Item -ItemType Directory -Path backups -Force
java $chatDataClass -cp $chatDataJar $chatDataMain backup '.selftest/source-copy' 'backups/checkpoint.zip'
java $chatDataClass -cp $chatDataJar $chatDataMain verify 'backups/checkpoint.zip'
java $chatDataClass -cp $chatDataJar $chatDataMain restore 'backups/checkpoint.zip' '.selftest/restored-new'
java $chatDataClass -cp $chatDataJar $chatDataMain import-copy '.selftest/desktop-copy' '.selftest/imported-new'
```

Các tên trên là ví dụ fixture, không trỏ dữ liệu vận hành. Mỗi lệnh trả Summary accounts/rooms/messages/files/fileBytes, không in nội dung chat/mật khẩu. Mật khẩu mặc định/tạm trong dữ liệu nhập vẫn bị backend buộc đổi; CLI không cấp lại hay reset mật khẩu. Nếu lệnh lỗi, không coi là đã khôi phục; kho nguồn và đích có sẵn được giữ nguyên.

## File/hàm và QA

| File | Vai trò |
|---|---|
| `server/WebDataTool` | backup/verify/restore/importCopy, lock, inventory/whitelist/giới hạn, extract/checksum, validateCopy, cleanup staging do công cụ tạo |
| `web/WebDataToolTest` | Byte-for-byte, 5 MB, lock đang chạy, chống ghi đè, hỏng/traversal, CRC, tác giả đã xóa, import desktop/force-password |
| `application-proxy.properties`, `deploy/*` | Profile reverse proxy và mẫu cấu hình host để xem xét; chưa cài trên host |
| `scripts/web4-local-tls-qa.py` | Proxy TLS fixture bằng aiohttp, hai phiên Edge, UDP/tệp/CSRF/Origin, CLI và restart/rollback |
| `workspace-ui.js`, `admin-ui.js`, `member-moderation-browser-qa.py` | Guard callback dialog đang mở và test phản hồi trễ/Escape |

Maven đã chạy toàn bộ **35 test PASS**, sau đó WebDataToolTest **6 PASS** (thêm một ca CRC/tác giả đã xóa, production Java không đổi): tổng **36 ca khác nhau PASS**, không lỗi/bỏ qua. [Bằng chứng Java](screenshots/web4-local/java-results.json). TLS/data JAR thật có **5 nhóm PASS**, gồm ba lượt start trên original/restored/rollback; [JSON](screenshots/web4-local/tls-data-results.json), [ảnh HTTPS](screenshots/web4-local/01-https-chat.png). Browser quản lý thành viên trên JAR cuối: **11 nhóm PASS**, gồm phản hồi trễ/Escape; xem [JSON](screenshots/web4-local/final-members/browser-results.json). Không cộng các lượt lặp thành số ca mới.

QA TLS dùng Python Playwright, aiohttp 3.14.5 và OpenSSL cục bộ; certificate/key fixture chỉ ở `.selftest`, không cài trust store. Chạy `CHAT_QA_JAR` trỏ artifact rồi `python -X utf8 scripts/web4-local-tls-qa.py`. Dừng compile trước khi chạy fixture dùng chung classes. Bộ hồi quy UX/Web 2/tệp/admin của mốc 3.3 vẫn nằm tại [nghiệm thu thành viên](WEB3_3_THANH_VIEN_NGHIEM_THU.md); production nghiệp vụ của các nhóm này giữ nguyên, chỉ thêm công cụ dữ liệu/profile và guard dialog nêu trên.

## Preview và phần còn lại

Preview **http://127.0.0.1:8085** đã nâng JAR sau khi backup và giữ JAR cũ; giữ nguyên tài khoản/phòng/mật khẩu. Thông tin đăng nhập vẫn ở `.selftest/member-preview-8f11edd4f7/login-local.txt` (ignored). [Kết quả upgrade](screenshots/web4-local/preview-upgrade-results.json). Preview 8084 giữ nguyên bản cũ; không dùng nó để nhận xét thay đổi mới.

Mẫu host, nguồn chính thức và các phép thử còn cần trên đích: [TRIEN_KHAI_0_DONG.md](TRIEN_KHAI_0_DONG.md). Chưa có hostname/quyền triển khai nên chưa thử UDP/WSS/restart trên host; chưa kiểm bàn phím ảo/screen reader trên thiết bị thật. Không đánh dấu Web 4 hoàn tất hoặc toàn bộ sản phẩm đã nghiệm thu vì các giới hạn này.

## 5 bước thử tay

1. Mở 8085 bằng hai phiên chủ/thành viên; cấm/gỡ cấm, đuổi/ban/chuyển chủ và xem thông báo của hai phía. Trong lúc gỡ cấm đang chờ, đóng dialog; phản hồi về không được mở lại.
2. Trên fixture riêng, gửi text/sticker/tệp rồi dừng đúng tiến trình đang giữ data-dir fixture.
3. Backup và verify bằng CLI, ghi lại Summary; thử backup khi còn chạy hoặc đích archive đã có phải bị từ chối.
4. Restore vào thư mục mới, chạy JAR với `--chat.data-dir` đó; đăng nhập, kiểm lịch sử/quyền/metadata và byte tệp. Nguồn cũ vẫn giữ nguyên.
5. Ghi một tin mới ở bản restore, dừng rồi restore checkpoint vào thư mục mới khác để rollback. Tin mới hơn không nằm trong checkpoint nhưng thư mục vừa dùng vẫn được giữ lại.
