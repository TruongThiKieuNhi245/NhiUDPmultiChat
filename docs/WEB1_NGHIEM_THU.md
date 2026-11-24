# Nghiệm thu Web 1 — UDPmultiChat

Ngày 25/11/2026. Phạm vi: một ứng dụng web Java 21/Maven, đăng nhập và một phòng text; UDP unicast loopback trong backend. Chưa triển khai Web 2–4 hoặc public.

## Baseline và bảo vệ workspace

HEAD tiếp quản: `ff6133991ce21fb5a742e603d5e5ee0be8888df9`. Sáu file đã có sửa đổi của người dùng: AGENTS.md, README.md, BAN_GIAO_AGENT.md, KE_HOACH_HOAN_THIEN_DESKTOP.md, MA_TRAN_CHUC_NANG_DESKTOP.md, PROMPT_NANG_CAP_VA_TRIEN_KHAI.md. Source desktop khớp bàn giao. Giữ các sửa đổi đó; commit Web 1 chỉ stage phần README mới cùng source/test/tài liệu/ảnh mới. Không nhập, sửa hoặc xoá dữ liệu vận hành.

Đã chạy baseline Maven offline `test-compile dependency:build-classpath`, exit 0, trước khi đổi entrypoint. M3cChatSelfTest: 2 PASS; M3cThanhVienSelfTest: 3 PASS. Kết quả AdminSelfTest 16 PASS trong bàn giao dùng làm mốc so sánh, không coi là QA web. Dùng fixture `.selftest/` và thư mục tạm của JUnit, không dùng `data/` thật.

## Ánh xạ nghiệp vụ và file/hàm

Đường dẫn Java bắt đầu tại `src/main/java/com/mycompany/udpmultichat/`.

| Nghiệp vụ | Source hiện có → quyết định | Điểm vào web / kiểm thử |
|---|---|---|
| Xác thực | QuanLyNguoiDung.dangKy/dangNhap, BaoMat; dùng cùng kho, thêm chính sách PBKDF2 riêng cho web và nâng hash SHA-256 sau login thành công | WebApi.login/register, WebSecurity; Web1IntegrationTest.webLoginUpgradesLegacyPasswordInSameStore |
| Phiên | Phien vẫn là định danh/quyền nghiệp vụ; không lấy sender/role từ JSON browser | Cookie HttpOnly, SameSite=Strict, rotate ID; ChatSocket kiểm session và Origin, logout đóng WS |
| Phòng | QuanLyPhong.datCho/xacNhanVao/kiemTraGui, KiemDuyet; dùng Sảnh chung được bảo vệ, mute/ban/spam vẫn kiểm tại receiver | WebChatService.login/receive/history; test trái phòng |
| Chat/lịch sử | QuanLyTinNhan.them/theoMa/lay; KhoLuuTru.ghiTin force xuống đĩa trước khi thêm RAM, giữ khoá room+user+clientMsgId | WebChatService.receive; /api/history cursor, mỗi trang 50; test retry, lỗi ghi, restart |
| UDP | Đọc ReliableUdp.gui/vongNhan và Goi/Protocol. Giữ ReliableUdp nguyên vẹn cho desktop; web dùng transport nhỏ, vòng đời/nguồn gửi riêng | transport/UdpChatTransport.send/transmit/receive; UdpChatTransportTest |
| Realtime | Thay phatMulticast của ServerCore bằng callback sau commit từ receiver web; giữ desktop để đối chiếu | ChatSocket.broadcast; hai cookie/tài khoản/browser độc lập |
| Giao diện | Thay màn Swing bằng HTML/CSS/JS modules cùng origin; tái dùng quy tắc giới hạn UTF-8 | resources/static/{index.html,app.css,app.js}; scripts/web1-browser-qa.py |
| Lời mời/biệt danh | QuanLyLoiMoi, QuanLyPhong.datBietDanh và lưu properties | Web 2, chưa có UI/API web |
| Sticker/tệp | StickerDanhMuc, 36 PNG Noto/LICENSE/NOTICE; QuanLyTep/KhoTep/quyền | Web 2 sticker; Web 3 upload/download HTTP, metadata tin qua UDP |
| Admin/báo cáo | KiemDuyet, ChupThongKe, XuatBaoCao, snapshot và bộ xuất hiện tại | Web 3: 5 mục admin, số người từng phòng, PDF tiếng Việt và 4 CSV + 1 HTML |

## Đường đi thật của tin nhắn

```text
Browser A -- WS /ws có session + Origin --> ChatSocket
    -- token nội bộ + roomId + clientMsgId + text UTF-8 --> UdpChatTransport.send
    -- DatagramSocket.send / DatagramPacket --> 127.0.0.1:cổng-ngẫu-nhiên
    -- DatagramSocket.receive --> kiểm endpoint + nonce + payload + phiên
    --> WebChatService.receive: quyền phòng/mute/ban/spam/chống trùng
    --> QuanLyPhong.themTin --> QuanLyTinNhan.them --> KhoLuuTru.ghiTin + force
    --> MESSAGE_COMMITTED --> ChatSocket.broadcast --> WS đúng phòng
Browser A nhận confirmed; Browser B nhận message với cùng msgId/clientMsgId.
```

Nhánh receiver UDP là nơi duy nhất gọi `WebChatService.receive` để chấp nhận tin mới. Handler WS không gọi kho và không có đường HTTP tạo tin. Trạng thái pending không phải đã lưu. Kết quả nghiệp vụ được hoàn thành từ nhánh receiver sau commit; Web 1 không dùng ACK datagram riêng. Timeout/gửi lại dựa trên xác nhận nghiệp vụ: tối đa 3 lần gửi cách 800 ms, sau khoảng 2,4 giây báo chưa xác nhận. Retry bằng nút Gửi lại giữ clientMsgId. Kho chống trùng bền vững trả msgId cũ; payload khác với cùng mã bị từ chối. Bản lặp không broadcast lại; người gửi vẫn được confirmed. Mất xác nhận có thể là tin đã lưu, nên retry luôn giữ mã.

Mỗi JVM dùng một sender socket, một receiver socket và một timer retry; không tạo socket theo tab. Datagram UTF-8 tối đa 1399 byte, nội dung tối đa 1000 byte và còn phải qua kiểm tra header/escape của Goi. Chỉ nhận từ đúng endpoint socket gateway và nonce đang chờ, đối chiếu toàn bộ bytes; token nội bộ không trả cho browser. Pending tối đa 256, 128 phiên, 128 WS và tối đa 3 WS/phiên; WS frame 8 KiB, buffer gửi 64 KiB và tối đa 40 frame/10 giây/WS. Timer retry huỷ/xoá task ngay khi hoàn thành để không giữ backlog đã xong. Chặn tốc độ login/register 15 lần/phút/IP, body HTTP có Content-Length quá 8 KiB bị từ chối; parser giới hạn string 4096 và depth 10. Tài khoản web tối đa 1000, Sảnh tối đa 10.000 tin mới; DOM giữ 500 tin gần nhất. Luồng/timer/socket đóng khi bean tắt.

MsgId theo thứ tự server nhận và commit, không hứa theo thời điểm browser gửi. DOM sắp msgId và loại trùng; cursor lịch sử chỉ tiến khi đã đọc trang liên tục để tin đảo thứ tự không làm mất khoảng bù. Giữ log kỹ thuật gồm stage/roomId/clientMsgId/msgId, không ghi body/password/token. WS_DELIVER nghĩa server đã ghi frame ra phiên, không khẳng định người dùng đã đọc; browser QA xác nhận phía B thực sự nhận.

## Đối chiếu hai PDF môn học

Đã đọc/trích xuất hai PDF thực tế tại `../tailieu` tính từ repo, kiểm trang render. `Chap_6_Socket_UDP_2.pdf` có 40 trang: tr.12 giải thích DatagramPacket/DatagramSocket, tr.22–25 socket và send/receive, tr.26–28 vòng receive. `Chap_6_1_MultiCast.pdf` có 14 trang: tr.11 joinGroup/leaveGroup/send/receive, tr.12 ví dụ gia nhập nhóm.

Web 1 dùng đúng DatagramSocket/DatagramPacket/send/receive cho nội dung trong backend. Browser dùng HTTP/WS và không tham gia multicast. Desktop vẫn có ServerCore.phatMulticast và client/KenhPhong join/leave nhóm, làm bản demo môn học riêng trong LAN. Không gọi UDP loopback unicast là multicast. Retry, idempotency, thứ tự commit và bù lịch sử là giao thức ứng dụng, không phải bảo đảm sẵn của UDP. Multicast web chưa được chạy hoặc tuyên bố nghiệm thu.

## Kết quả và bằng chứng

Các tệp máy đọc được tại `screenshots/web1/`: `browser-results.json`, `package-results.json`, `test-results.json`, `udp-trace.txt`. Browser/fixture đều dừng sau QA; URL cổng ngẫu nhiên trong JSON là URL kiểm thử đã dừng.

- JUnit/Maven: **5 test, 0 failure, 0 error; Maven package exit 0**. Hai test transport mất/trùng/đảo gói; ba test tích hợp HTTP/WS, persistence/lỗi đĩa, và nâng hash cũ. Có cảnh báo runtime Mockito/Byte Buddy tự attach agent của test starter trên Java 21; không có warning compiler hoặc lỗi kiểm thử.
- Desktop SelfTest: **75 PASS, 0 FAIL, exit 0** trên source cuối; M3cChatSelfTest **2 PASS**, M3cThanhVienSelfTest **3 PASS** chạy lại sau build, đều exit 0. Một lượt áp PBKDF2 cho cả desktop đã gặp timeout M2.5/M2.6. Đã sửa chính sách chỉ áp hash mới cho web và giữ lượt lỗi trong target/web1-desktop-firsttry.txt. Không tính lượt lỗi là PASS.
- Edge 158.0.4370.33 headless, hai BrowserContext/tài khoản: **11 nhóm PASS**. Một mẫu frame A gửi → B nhận trên localhost: **86.83 ms**, gồm scheduling browser/server và ghi đĩa; không coi là benchmark LAN/cloud. Đã thử tiếng Việt hai chiều, XSS text, UDP dừng khi WS vẫn mở, retry cùng mã, mất mạng/reconnect, cookie, logout, giới hạn byte, 360px sáng/tối và zoom CSS 150%.
- JAR thật: kiểm UI/WS, tin qua UDP, endpoint fixture trả 404 và restart giữ account/lịch sử (package-results.json).

Ảnh thật: [đăng nhập desktop](screenshots/web1/01-dang-nhap-desktop.png), [chat sáng](screenshots/web1/02-chat-desktop-sang.png), [UDP dừng/WS còn mở](screenshots/web1/03-udp-dung-websocket-con-ket-noi.png), [chat tối](screenshots/web1/04-chat-desktop-toi.png), [zoom 150%](screenshots/web1/05-chat-zoom-150.png), [mobile sáng](screenshots/web1/06-chat-mobile-sang.png), [mobile tối](screenshots/web1/07-chat-mobile-toi.png). Mobile là viewport mô phỏng; zoom là CSS zoom, chưa thay kiểm zoom trình duyệt thật.

Công cụ CUA/Node kernel lỗi `windows sandbox failed: helper_unknown_error: setup refresh had errors`; QA chuyển sang Python Playwright điều khiển Edge headless đã cài. Không coi đó là thử trực tiếp trên điện thoại hoặc tablet.

## Chạy và thử tay

Lệnh build/run/QA và 5 bước thử tay có ở đầu README. Fixture điều khiển receiver nằm hoàn toàn trong src/test, khởi động bằng Web1BrowserFixture trên test classpath; không đóng gói API tắt UDP vào artifact. `mvn test` chạy JUnit web, không tự chạy các main SelfTest desktop. Script browser tự tạo tài khoản mẫu và thư mục mới, tắt server khi xong.

## Giới hạn và kế hoạch tiếp theo

- Web 1 có một Sảnh chung, chưa có nhóm riêng/UI hồ sơ/admin. Lịch sử hiện dùng format/kho desktop và nạp RAM; quota Web 1 kiểm soát tăng mới, chưa phải DB phân trang cho dữ liệu lớn. Pending browser giữ trong RAM của tab, chưa khôi phục hàng lỗi chưa xác nhận sau đóng tab. Đăng nhập/tài khoản của web dùng chung repository, không đồng bộ trực tiếp dữ liệu vận hành desktop.
- Chỉ một instance, mặc định bind 127.0.0.1 và HTTP/WS local. Chưa kiểm HTTPS/WSS, proxy, host cloud, persistent disk cloud, browser khác Edge hoặc thiết bị vật lý. PBKDF2 web 210.000 vòng cần benchmark khi chọn host; các luồng xác thực legacy desktop giữ SHA-256 để đối chiếu. Import phải từ bản sao, chưa có công cụ import/rollback UI.
- Web 2: nhiều nhóm/quyền/CRUD, lời mời online/offline và badge, biệt danh theo roomId+userId, hồ sơ/avatar/đổi mật khẩu; sticker 36 PNG Noto giữ nguồn và giấy phép; reply/recall/search/paging/export TXT/typing/unread/mute. Nghiệm thu hai nhóm cách ly và quyền thật.
- Web 3: tệp HTTP có tiến độ/hủy/retry/checksum/quyền (metadata tin đi UDP); kiểm duyệt và 5 mục admin; số thành viên từng phòng tách số online; cùng snapshot cho PDF tiếng Việt A4, **nguoidung.csv, phong.csv, thanhvien_theophong.csv, tin_theo_ngay.csv, tong_hop.html**. Persistence/giao dịch/nhập từ bản sao và backup/rollback.
- Web 4: hoàn thiện responsive/accessibility, QA desktop/điện thoại/tablet thật, TLS/WSS/security/network; đối chiếu ít nhất hai host miễn phí bằng nguồn chính thức, thử UDP nội bộ + restart/dữ liệu + HTTPS/WS trên host. Chưa chọn host, chưa xác nhận gói miễn phí và chưa push/public. Chỉ deploy theo đích/quyền người dùng giao.

## Nguồn kỹ thuật và tham khảo UI (25/11/2026)

[Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) xác nhận nhánh 3.5.16 tương thích Java 21; dependency tải thành công từ Maven Central. [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) và [Java 21 DatagramSocket](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/DatagramSocket.html) làm đối chiếu giao thức/bảo vệ mutation.

Tham khảo tương tác phòng/soạn tin từ [Slack](https://slack.com/help/articles/201457107-Send-and-read-messages-in-Slack), phân chia text channel từ [Discord](https://support.discord.com/hc/en-us/articles/4412085582359-Text-Channels-Text-Chat-In-Voice-Channels), và bảng thông tin hội thoại từ [Zalo PC](https://help.zalo.me/huong-dan/chuyen-muc/nhan-tin-va-goi/nhan-tin/cac-tien-ich-co-ban-khi-nhan-tin-tren-zalo-may-tinh/). Áp dụng sidebar phòng, tiêu đề phòng và composer cố định; màu/chữ/bố cục được xây trong CSS của dự án. Các tính năng tham khảo ngoài phạm vi Web 1 không được đánh dấu đã làm.
