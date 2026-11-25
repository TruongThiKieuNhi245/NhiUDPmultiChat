> **Web 2 (25/11/2026):** đã có phòng/thành viên/lời mời offline, hồ sơ/avatar/mật khẩu, 36 sticker Noto, reply/thu hồi/search/TXT, typing và bản nháp theo phòng. Chat mới vẫn qua UDP thật. Xem [bàn giao Web 2, ảnh và 5 bước thử](docs/WEB2_NGHIEM_THU.md).
>
> QA: `python -X utf8 scripts/web2-browser-qa.py`; kiểm artifact/restart: `python -X utf8 scripts/web2-package-smoke.py` (biến `CHAT_QA_JAR` chọn JAR riêng). Chạy `test-compile dependency:build-classpath` như bên dưới trước browser QA. Dùng fixture riêng, không dùng dữ liệu vận hành. Bản xem thử Web 2 dùng cổng/data dir riêng; URL và lệnh ở báo cáo Web 2.

> **Chặng UX Web 1 (25/11/2026):** tin của mình bên phải theo `userId`, bong bóng hai phía, composer gọn, drawer có focus, tải lịch sử gần nhất và cache 300 tin. Xem [bàn giao UX, ảnh thật và 5 bước thử](docs/WEB1_UX_NGHIEM_THU.md). Web 2–4 tiếp tục sau khi người dùng xem chặng này.
>
> QA mới: `python -X utf8 scripts/web1-ux-browser-qa.py` sau `test-compile dependency:build-classpath` như bên dưới. Hai tài khoản trùng tên, fixture 680 tin; dữ liệu riêng trong `.selftest/`. Bản nháp chỉ giữ trong RAM của tab; đóng tab sẽ mất bản nháp và hàng chưa xác nhận.
>
> Trước `mvn package`, dừng tiến trình đang chạy chính JAR đích để Windows cho phép đổi tên artifact. Có thể dựng từ bản sao source trong fixture khi cần giữ server cũ hoạt động. Mốc này dùng artifact riêng; đường dẫn và kết quả được ghi trong báo cáo UX.

# UDPmultiChat Web 1 — chạy bằng URL

Web 1 có đăng ký/đăng nhập, Sảnh chung, tin tiếng Việt, trạng thái gửi/thử lại, lịch sử và reconnect. Java 21, Maven, Spring Boot 3.5.16; giữ `com.mycompany:UDPmultiChat:1.0-SNAPSHOT`.

## Chạy web local

```powershell
$mvnChat = 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd'
& $mvnChat -q package
java -jar target/UDPmultiChat-1.0-SNAPSHOT.jar
```

Mở **http://127.0.0.1:8080**. Bấm **Tạo tài khoản**, dùng tên 3–20 chữ/số/gạch dưới, mật khẩu 6–128 ký tự. Không có mật khẩu demo hoặc admin mặc định của bản web. Mỗi tài khoản chỉ đăng nhập một phiên; dùng hai tài khoản và hai cửa sổ/profile trình duyệt độc lập để thử.

Điểm chạy web: `com.mycompany.udpmultichat.web.WebApplication`. Có thể dùng `& $mvnChat spring-boot:run`. Điểm chạy desktop vẫn là `ClientMain` / `server.ServerMain`, chỉ định `-Dexec.mainClass` khi chạy; các phần phía dưới là tài liệu desktop lịch sử.

Dữ liệu web mặc định ở **web-data/** (được bỏ qua trong Git), không dùng `data/`, `reports/` hoặc `client.properties` desktop. Dừng ứng dụng trước khi sao lưu thư mục. Chỉ chạy một instance và không cho desktop cùng ghi một data dir. Giữ thư mục này qua restart; xoá thư mục sẽ mất tài khoản/lịch sử web.

Cấu hình tùy chọn:

```powershell
$env:PORT = '8080'
$env:CHAT_DATA_DIR = 'D:/duong-dan-rieng/chat-data'
# Chỉ bật khi muốn thử trong LAN tin cậy:
$env:BIND_ADDRESS = '0.0.0.0'
java -jar target/UDPmultiChat-1.0-SNAPSHOT.jar
```

Trên thiết bị LAN mở `http://<IPv4-máy-chạy>:8080`; cần máy/thiết bị cùng mạng và cổng HTTP được phép. UDP chỉ bind `127.0.0.1`, không mở ra LAN/Internet. Mặc định local là HTTP/WS. HTTPS/WSS, reverse proxy, cookie `COOKIE_SECURE=true`, host 0 đồng và kiểm thử thiết bị thật nằm ở Web 4; chưa public/deploy.

## Thử tay Web 1 (5 bước)

1. Build/chạy bằng lệnh trên, tạo tài khoản A trong cửa sổ thường và B trong cửa sổ ẩn danh/profile khác.
2. Trong Sảnh chung, A gửi tiếng Việt, B trả lời. Cả hai thấy cùng tin; người gửi có `Đang gửi…` rồi `✓ Đã gửi`.
3. Đổi sáng/tối, thu cửa sổ xuống 360 px, mở/đóng danh sách phòng. Enter gửi, Shift+Enter xuống dòng; ô soạn chặn hơn 1000 byte UTF-8.
4. Dùng DevTools cho B Offline rồi Online: A gửi trong lúc B mất mạng; khi nối lại B nhận bù và không nhân tin. Đóng/mở lại ứng dụng, đăng nhập và kiểm tra lịch sử.
5. Chạy QA fixture dưới đây để tắt **riêng receiver UDP** mà WebSocket vẫn mở. Tin lỗi chưa vào lịch sử; khôi phục rồi bấm `Gửi lại` giữ cùng mã, chỉ có một bản ghi. API điều khiển fixture không có trong JAR chạy thật.

## Kiểm thử tự động và bằng chứng

```powershell
& $mvnChat -q test
& $mvnChat -q test-compile dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
# Python có Playwright, Microsoft Edge đã cài:
python -X utf8 scripts/web1-browser-qa.py
# Cần chạy package trước khi thử artifact:
python -X utf8 scripts/web1-package-smoke.py
# SelfTest desktop là main, Maven test không tự chạy nhóm này:
$chatCp = 'target/test-classes;target/classes;' + (Get-Content -Raw target/web-classpath.txt).Trim()
java -cp $chatCp com.mycompany.udpmultichat.SelfTest
```

Nếu Python chưa có Playwright: `python -m pip install playwright`. QA dùng Edge headless đã cài, hai BrowserContext có cookie độc lập; dữ liệu mỗi lượt ở `.selftest/web1-*`. Viewport mobile và zoom là mô phỏng, chưa thay thử điện thoại thật. Test tích hợp dùng HTTP, WebSocket và DatagramSocket thật, gồm mất/trùng/đảo thứ tự gói, lỗi đĩa, session/Origin/CSRF và chống trùng qua restart.

Xem [báo cáo nghiệm thu, bản đồ file/hàm và kế hoạch Web 2–4](docs/WEB1_NGHIEM_THU.md), [ảnh desktop](docs/screenshots/web1/02-chat-desktop-sang.png), [ảnh mobile](docs/screenshots/web1/06-chat-mobile-sang.png), [kết quả browser](docs/screenshots/web1/browser-results.json) và [log đối chiếu UDP](docs/screenshots/web1/udp-trace.txt).

---

> **Bản bàn giao hoàn thiện đồ án tại [docs/BAN_GIAO_AGENT.md](docs/BAN_GIAO_AGENT.md).**
> Hướng ứng dụng: **Desktop UDP Multi-room Chat (Java 21 + Swing + FlatLaf hiện đại), 100% UDP Unicast & Multicast trực tiếp, bản đóng gói Portable độc lập và hỗ trợ chạy 2 máy thật trong mạng LAN**.
> Toàn bộ mốc phát triển **M1 -> M7 đã hoàn thành và kiểm chứng 100%**:
> - `SelfTest`: **75 PASS / 0 FAIL**
> - `AdminSelfTest`: **16 PASS / 0 FAIL**
> - `M3cChatSelfTest`: **2 PASS / 0 FAIL**
> - `M3cThanhVienSelfTest`: **3 PASS / 0 FAIL**
> - `UiPreview`: **10/10 PASS** (kết xuất 60 ảnh màn hình PNG tại `docs/screenshots/`)

# UDP Multi-room Chat (Đồ án Lập trình mạng)

Ứng dụng trò chuyện nhiều phòng qua mạng cục bộ sử dụng hoàn toàn giao thức **UDP (UDP Unicast cho điều khiển/truyền tệp và UDP Multicast cho chat nhóm)**, xây dựng trên nền tảng **Java 21**, giao diện người dùng **Swing** hiện đại kết hợp thư viện **FlatLaf 3.7.2** (Apache 2.0).

---

## 1. Cách Chạy Nhanh Ứng Dụng

### Cách 1: Chạy Bản Portable Độc Lập (KHÔNG CẦN CÀI ĐẶT JAVA)
Các bản đóng gói độc lập kèm sẵn Java Runtime Environment (JRE 21) nhúng sẵn nằm trong thư mục `dist/`:
- **Khởi động Máy chủ (Server):**
  - Mở thư mục `dist/UDPChatServer/` (hoặc giải nén `UDPChatServer-Windows-x64.zip`).
  - Bấm đúp vào file `UDPChatServer.exe` -> Nhấn nút **[Bắt đầu]** -> Bấm **[Sao chép IP]**.
- **Khởi động Máy khách (Client):**
  - Mở thư mục `dist/UDPChatClient/` (hoặc giải nén `UDPChatClient-Windows-x64.zip`).
  - Bấm đúp vào file `UDPChatClient.exe` (để mở Client thứ 2 trên cùng máy, bấm `Chay-Client-Phien-2.bat`).
  - Nhập IP của Máy chủ, bấm **[Kiểm tra kết nối]** và đăng nhập/đăng ký.

*Chi tiết đóng gói xem tại: [docs/DONG_GOI_PORTABLE.md](docs/DONG_GOI_PORTABLE.md).*

### Cách 2: Chạy Thử Nghiệm 2 Máy Thật trong Mạng LAN / Wi-Fi
- Kết nối cả 2 máy tính vào cùng một mạng Wi-Fi (hoặc Mobile Hotspot).
- Máy 1 chạy `UDPChatServer.exe`, lấy IP LAN.
- Máy 2 chạy `UDPChatClient.exe`, trỏ tới IP của Máy 1.
- *Xem hướng dẫn từng bước và kịch bản thử nghiệm tại: [docs/CHAY_HAI_MAY.md](docs/CHAY_HAI_MAY.md).*

### Cách 3: Chạy từ Mã nguồn bằng Apache NetBeans hoặc VS Code
- **Apache NetBeans 21+:** `File` -> `Open Project` -> Chọn thư mục `UDPmultiChat`. Chuột phải vào `ServerMain.java` chọn `Run File`, sau đó chuột phải vào `ClientMain.java` chọn `Run File`.
- **Dòng lệnh Maven:**
  ```powershell
  # Chạy Server
  mvn compile exec:java -Dexec.mainClass="com.mycompany.udpmultichat.server.ServerMain"
  # Chạy Client
  mvn compile exec:java -Dexec.mainClass="com.mycompany.udpmultichat.ClientMain"
  ```

---

## 2. Bảng Tiến Độ và Các Mốc Hoàn Thành

| Mốc | Mô tả chức năng chính | Trạng thái |
|---|---|---|
| **M1** | Tài khoản SHA-256 + Salt, xác thực UDP token, phát hiện offline qua Heartbeat, chống đăng nhập 2 nơi | **HOÀN THÀNH (PASS)** |
| **M2** | Quản lý đa phòng (CRUD), mã phòng 6 ký tự, bảo mật mật khẩu phòng, đồng bộ RESYNC chống mất tin | **HOÀN THÀNH (PASS)** |
| **M3a** | Hồ sơ cá nhân: Tên hiển thị, tiểu sử, tải ảnh đại diện Avatar qua UDP mảnh cắt giữa 96×96 PNG | **HOÀN THÀNH (PASS)** |
| **M3b** | Bộ 36 Sticker PNG Noto Emoji, thu hồi tin nhắn, sao chép tin, chỉ báo trạng thái gửi/lỗi, typing realtime | **HOÀN THÀNH (PASS)** |
| **M3c.1** | Trích dẫn & Trả lời tin nhắn (Quote/Reply), tìm kiếm tin nhắn trong phòng (`Ctrl + F`), gộp tin nhắn liên tiếp trong 5 phút, phân cách ngày, xuất lịch sử chat ra file `.txt` | **HOÀN THÀNH (PASS)** |
| **M3c.2** | Đổi biệt danh riêng theo từng phòng (Room Nickname), mời thành viên vào phòng có duyệt lời mời (Invite Dialog), tắt/bật thông báo riêng cho từng phòng (Mute Notifications `🔕`) | **HOÀN THÀNH (PASS)** |
| **M4** | Truyền tệp tin qua UDP (cửa sổ trượt 32 mảnh, retry 8 lần, kiểm tra toàn vẹn CRC32, thanh tiến trình, hủy tệp, xem trước ảnh thumbnail) | **HOÀN THÀNH (PASS)** |
| **M5** | Trung tâm Quản trị Admin 5 tab: Dashboard tổng quan, Quản lý tài khoản, Quản lý phòng, Nhật ký kiểm toán Audit log, Xuất báo cáo hệ thống (In/Lưu PDF qua Print Preview, HTML gradient tự chứa, CSV UTF-8 BOM) | **HOÀN THÀNH (PASS)** |
| **M6** | Thiết kế giao diện hiện đại chuẩn 2026 (Slack / Discord / Zalo PC), thanh thao tác tin nhắn nổi (Hover Quick Action Bar), gradient tokens, layout 3 cột tỷ lệ vàng, chống tràn giao diện | **HOÀN THÀNH (PASS)** |
| **M7** | Rà soát toàn bộ hệ thống, tự động đóng gói Portable Windows x64 (`build-portable.ps1` tạo file `.exe` nhúng JRE 21), kịch bản thử nghiệm 2 máy LAN, tài liệu kỹ thuật hoàn chỉnh | **HOÀN THÀNH (PASS)** |

Menu tài khoản có **Hồ sơ của tôi / Đổi mật khẩu / Giới thiệu / Đăng xuất**. Sáng/Tối chưa triển khai nên vẫn ẩn. Nút mặt cười mở bảng sticker; nút + mở Gửi tệp/Gửi ảnh, vô hiệu khi offline hoặc phòng đóng. Quản trị thành viên thực hiện ở M5. Discovery tự tìm Server chưa triển khai; hiện nhập IP thủ công.

## Chạy bằng VS Code / NetBeans

1. Mở thư mục chứa `pom.xml`, dùng JDK 21 và Extension Pack for Java/Maven for Java. `.vscode/settings.json` trỏ JDK/Maven của máy hiện tại; máy khác cần sửa đường dẫn.
2. Đóng **toàn bộ Server/Client bản cũ**: gói đăng nhập/hồ sơ M3a có thêm trường, cần chạy cùng phiên bản. Nhấn **Ctrl+Shift+D**, chọn **Server**, F5; bấm **Bắt đầu** trong cửa sổ giám sát.
3. Lần lượt chọn **Client 1**, **Client 2**, **Client 3**, F5 cho mỗi cấu hình; giữ các phiên trước đang chạy. Đây là ba tiến trình riêng, cùng chạy ClientMain.
4. IP mặc định `localhost`, cổng 5000. Đổi IP tại **Cài đặt máy chủ**, bấm **Kiểm tra kết nối**. Tạo tài khoản ở tab Đăng ký, sau đó đăng nhập.
5. Admin mặc định **admin / admin123**; phải đổi mật khẩu trước khi vào phòng (hộp không có Đóng/Hủy, Esc không thoát). Tên tài khoản 3–20 chữ `a-z`, số hoặc `_`, không phân biệt hoa/thường; mật khẩu 6–128 ký tự.

Nút Run ngay trong ClientMain/ServerMain cũng dùng được. NetBeans: Open Project, chọn JDK 21, Run File trên một trong hai class này. Main mặc định trong POM là ClientMain; không còn class ChatMulticastUI ở package gốc.

## Lệnh Maven (PowerShell)

Chạy tại thư mục chứa POM. Nếu đã có Maven trong PATH, thay `& $mvnChat` bằng `mvn`.

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.server.ServerMain'
```

Mở terminal khác cho mỗi Client:

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ClientMain'
```

Lần đầu Maven cần Internet để tải FlatLaf/plugin. Các lần sau dùng cache Maven. Mọi nguồn UTF-8, compiler release 21, không preview, bật `-Xlint:all` và `failOnWarning`.

## Dùng phòng và gửi tin

- **+ Tạo phòng**: tên, mô tả, loại, giới hạn, mật khẩu tùy chọn. Chủ phòng tự vào; mã nằm trên tiêu đề giữa, có nút sao chép. Danh sách không hiển thị địa chỉ multicast.
- Bấm phòng đã tham gia để chuyển ngay sang khung chat đang giữ ở nền, không gửi yêu cầu mạng. Bấm **Tham gia** ở nhóm công khai để vào; phòng có mật khẩu sẽ hỏi thêm. **Nhập mã** để vào phòng riêng tư: sai mã, sai mật khẩu, phòng đầy đều báo rõ. Admin nhìn thấy mọi phòng nhưng vẫn chịu giới hạn số thành viên.
- Menu **⋮** khi rê chuột/chọn phòng, hoặc chuột phải: Sửa, Đóng/Mở, Chuyển chủ, Xóa cho chủ/admin; Rời cho thành viên thường; Sao chép mã và Thông tin phòng. Xóa/Rời có xác nhận. Sảnh chung chỉ cho xem thông tin/sao chép mã.
- **Chủ phòng phải chuyển quyền cho thành viên khác trước khi rời**, hoặc xóa phòng. Người nhận quyền có thể đang offline. Sau chuyển quyền, chủ cũ trở thành thành viên thường.
- **Đóng phòng** chặn gửi tin và người mới vào; thành viên đã có có thể mở lại khung chat để xem lịch sử. **Rời phòng** xóa tư cách thành viên; **đăng xuất/tắt Client/timeout** chỉ chuyển offline, vẫn là thành viên. Giới hạn tính cả thành viên offline.
- Enter gửi, Shift+Enter xuống dòng; ô soạn cao tối đa 5 dòng, nút Gửi tròn không cao theo ô. Tin tối đa **1000 byte UTF-8**: khoảng 1000 ký tự ASCII hoặc 333 ký tự Việt loại 3 byte, tùy nội dung/escape/header. Gần ngưỡng, bộ đếm chỉ hiện số **ký tự**; quá giới hạn khóa Gửi và báo “Tin nhắn quá dài. Hãy rút gọn hoặc tách thành nhiều tin.” ngay dưới ô. Gửi lỗi giữ nội dung để thử lại.
- Tin phòng khác tăng badge chỉ trên sidebar, mở phòng thì badge về 0; lịch sử không tăng badge. Thành viên chia theo quyền và có chấm online/offline; nhãn **Bạn** chỉ chính mình.
- Đăng xuất nằm trong menu thẻ tài khoản dưới thanh bên. Nút X cũng gửi đăng xuất trên luồng nền. Tắt cưỡng bức được Server phát hiện sau khoảng 15 giây.
- Mất Server: sau thời gian phát hiện heartbeat, dải vàng hiện và khóa soạn tin. Client thử lại mỗi 5 giây, đăng nhập lại, khôi phục các phòng đang mở và RESYNC từ msgId đã nhận; thành công hiện dải xanh rồi tự ẩn. Mật khẩu phiên chỉ giữ trong RAM để nối lại, xóa khi đóng phiên.

## SelfTest

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q test-compile
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.SelfTest' '-Dexec.classpathScope=test'
```

Cách chạy trực tiếp, không cần FlatLaf vì SelfTest không tạo Swing:

```powershell
java '-Djava.net.preferIPv4Stack=true' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'target/test-classes;target/classes' com.mycompany.udpmultichat.SelfTest
```

Sáu file test ở `src/test/java`: SelfTest, PhongSelfTest, GiaoDienSelfTest, DanhTinhSelfTest, HoSoSelfTest, TinNhanSelfTest. Dữ liệu riêng `.selftest/`, cấu hình Client test cũng tách riêng; không dùng data thật. UDP điều khiển dùng cổng tạm, multicast test dùng gốc 19000/21000/23000/25000/27000/29000/31000. Chạy một phiên SelfTest mỗi lần.

Kết quả riêng mốc M3b trước đây: **43 PASS, 0 FAIL**, gồm:

- Hồ sơ phát realtime sang Client khác, avatar tải lên/tải về CRC trùng kể cả mất 20% gói; từ chối ảnh giả/quá lớn/sai kích thước, hủy/xóa ảnh, mật khẩu cũ sai/đúng và 6–128 ký tự, buộc đổi admin mặc định, nạp lại hồ sơ/hash và chuyển users.csv cũ 7 cột.
- Tài khoản, timeout, mất ngẫu nhiên 20% DATA/ACK với seed `20261112/20261113` (thứ tự lập lịch hệ điều hành vẫn có thể khác).
- CRUD/quyền/mật khẩu/phòng đầy, phòng riêng không lộ, lưu đúng loại, chuyển quyền và nạp lại sau restart.
- Multicast thật qua card Wi-Fi: cách ly phòng, cả hai nhóm dùng cùng cổng; lịch sử đúng 50; RESYNC tin giữa/tin cuối và hơn 50 tin liên tiếp.
- Tin Việt quá 1000 byte bị chặn ở Client và Server; biên 1000 byte được nhận. Thử nội dung 1000 byte qua multicast thật và kiểm tra gói RESYNC bọc ReliableUdp dài **1379 byte** với ID tối đa và tên hiển thị 30 ký tự loại 4 byte; nhiều escape vượt gói bị chặn.
- Client vào sau cùng có đúng một nhãn Bạn ngay snapshot đầu; profile phát trước các sự kiện phòng, laToi đúng, chủ phòng sửa được còn người ngoài không được Sửa/Xóa; cờ membership chia nhóm đúng theo từng tài khoản.
- HELLO, thành viên offline, Server dừng/bật lại, Client tự nối lại và bù 75 tin đã thiếu, không trùng/sai thứ tự; cấu hình Client không có mật khẩu/token.

Tên GiaoDienSelfTest chỉ nhóm các lỗi phát hiện khi thử giao diện; test này gọi logic mạng/quản lý, không giả lập thao tác chuột Swing. Việc bấm F5, hover, kéo cửa sổ cần kiểm tra tay.

## UiPreview — xem giao diện không cần Server

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview'
```

Xuất **43 PNG** vào [docs/screenshots](docs/screenshots/README.md): đăng nhập/đăng ký/lỗi inline, chat, mất kết nối, toast, menu tài khoản, hộp thoại phòng, Server và giới hạn tin, thông tin phòng, Client bbb vào cuối có Bạn, cửa sổ 960×600, ô soạn 5 dòng và số 111 và năm ảnh lỗi form bổ sung (20–24); sáu ảnh hồ sơ/mật khẩu/avatar (25–30). Công cụ vẽ chính component Swing đang dùng lên BufferedImage với dữ liệu mẫu; không mở socket, không phải ảnh chụp thao tác desktop. Cần môi trường đồ họa để tạo JFrame. Đã xem PNG và chạy kiểm tra component thật: đổi profile vẽ lại đúng Bạn kể cả trùng tên hiển thị, chuyển phòng xóa badge không cần mạng, nút Tạo phòng không cắt chữ, chữ số không có viền RGB do LCD AA; mọi input vẫn đủ cao khi có lỗi, nội dung JTextArea là Font.PLAIN, toast không che ô soạn và nhóm công khai rỗng được ẩn. Đây không thay thế thử tương tác thực tế.

## Dữ liệu

```text
data/
├── config.properties    # Cau hinh admin, multicast.portBase=9000
├── users.csv            # Tai khoan, salt/hash, gioiThieu, anhHash
├── rooms.csv            # Phong, loai, ma, salt/hash mat khau phong
├── members.properties   # Tu cach thanh vien ben vung roomId.userId
├── rooms/<roomId>.log    # Tin nhan UTF-8 da escape
└── avatars/<username>.png # Avatar PNG 96x96
client.properties        # Chi username/server lan dang nhap thanh cong gan nhat
```

Chỉ chạy một Server cho mỗi data, từ thư mục project. Token/trạng thái online chỉ trong RAM. CSV/properties ghi file tạm rồi thay thế, có thử lại ngắn khi Windows tạm khóa file; log ghi tuần tự và force xuống đĩa. Phòng đã xóa không còn trong danh sách; log cũ giữ để bảo toàn dữ liệu. Khi nâng từ M2, chỉ chủ phòng có thể khôi phục tư cách thành viên từ dữ liệu cũ; người khác cần vào bằng mã một lần để ghi membership mới.

Đổi mật khẩu qua menu tài khoản. `admin.password` trong config chỉ dùng để tạo admin ban đầu khi chưa có tài khoản; sửa giá trị này không đặt lại mật khẩu của admin đã tồn tại trong `users.csv`. Khi cần reset, dùng một admin khác → Quản trị → Người dùng → Đặt lại mật khẩu; mật khẩu tạm chỉ hiển thị một lần và buộc đổi khi đăng nhập. Nếu mất mật khẩu của admin duy nhất, sao lưu toàn bộ `data/` trước khi xử lý phục hồi; hiện chưa có công cụ phục hồi offline, không xóa `users.csv` hoặc dữ liệu thật. Nếu đổi khi Client đang mất kết nối, đăng xuất và đăng nhập bằng mật khẩu mới.

`target/`, `data/`, `reports/`, `.selftest/`, log, `client.properties` và file tạm được bỏ qua bởi Git. Commit chỉ ở repository cục bộ, không tự push lên GitHub.

## Thử tay M2.7 (1 Server + 3 Client)

1. Mở Server + Client 1/2/3. Ở form đăng nhập/đăng ký, thử tên sai/trùng, mật khẩu ngắn và nhập lại không khớp; mở Cài đặt máy chủ, thử IP sai. Ô nhập phải giữ chiều cao/chữ khi lỗi hiện rồi biến mất.
2. Tạo/sửa phòng với tên/mô tả sai, thử mã và mật khẩu phòng sai; kiểm tra các ô không bị nén, nút Hủy/Lưu luôn dưới nội dung. Chức năng Đổi mật khẩu chưa có ở M2.7; bố cục hộp thật sẽ kiểm tra tại M3a.
3. Tạo phòng công khai ở A, B thấy nút Tham gia có nền/viền và hover. Sau khi B tham gia hết phòng còn lại, tiêu đề PHÒNG CÔNG KHAI phải ẩn. Kiểm tra thẻ tài khoản có vai trò chữ thường màu xám.
4. Sao chép mã/đổi trạng thái phòng để xem toast ngắn; toast nhiều dòng tự tăng chiều cao, không che ô soạn. Mở Thông tin phòng: ngày dd/MM/yyyy và Sao chép sát mã; tiêu đề phòng thẳng hàng cụm nút.
5. Dừng/bật Server: ô soạn xám đồng nhất lúc khóa, nối lại về trắng. Kiểm tra log Consolas 13 (máy không có dùng Monospaced), chữ Việt; thử soạn tin/111 và kiểm tra chữ nội dung không đậm.

## Giới hạn đã biết

- **UDP không đảm bảo**: điều khiển gửi lần đầu + tối đa 3 lần gửi lại cách 500 ms; hết lượt báo lỗi. ACK xác nhận vào hàng xử lý, RESPONSE mới là kết quả nghiệp vụ. Multicast không ACK; RESYNC cần Server và lịch sử đã lưu.
- **Multicast cần chung mạng LAN** hỗ trợ multicast và **cho Java qua Windows Firewall**: UDP 5000 và 9001–9254 mặc định. Wi-Fi cô lập thiết bị/VPN có thể chặn multicast. Chưa thử nhiều máy LAN trong mốc này.
- **Token/mật khẩu đi qua UDP không mã hóa**; multicast chưa xác thực nguồn phát. Salt/hash chỉ bảo vệ dữ liệu trên đĩa. Dùng mạng tin cậy cho bài demo.
- Tối đa 254 phòng, gói dưới 1400 byte; nội dung có nhiều escape có thể chạm giới hạn gói trước 1000 byte và sẽ báo lỗi. Lịch sử hiện nạp RAM; chưa tối ưu hàng triệu tin.
- Phát hiện mất kết nối không tức thì, phụ thuộc heartbeat/gửi lại (thường khoảng 15–20 giây). Tự nối lại cần IP/cổng/credentials còn đúng; phòng đã bị xóa không được khôi phục. Đổi cổng multicast cần khởi động lại Client.
- Chưa kiểm tra tương tác F5/Run File trực tiếp trong VS Code/NetBeans, hay DPI/phóng to chữ trên các máy khác. Đã kiểm tra cấu hình, biên dịch, mạng thật trong SelfTest và PNG của component Swing.

## Cấu trúc và tài liệu chỉ code

40 file Java ứng dụng + 6 file test ở mốc này, mỗi file dưới 400 dòng (dài nhất ClientCore 399 dòng); các hàm được tách dưới khoảng 40 dòng. Comment đầu file ghi Vai trò/Ai gọi/Gọi ai; từng hàm có comment không dấu. Giữ bốn gói common/client/server/ui; UI không có Socket, logic không import Swing (ngoại lệ ServerMain). Quyền phòng gộp trong QuanLyPhong, lưu trữ gộp trong KhoLuuTru, DTO thành viên gộp trong Phong. Dialog tách khỏi DanhSachPhongPanel sang HopThoai để dùng chung và giữ file ngắn. Màu/font/nút/vẽ icon gom vào Theme, Avatar/BongTin còn trong ThanhPhan. Các phần M3–M5 sẽ ưu tiên mở rộng các lớp theo nhiệm vụ hiện có.

Xem [docs/GIAI_THICH.md](docs/GIAI_THICH.md) để tra bảng chức năng → file → hàm và đối chiếu UDP/Multicast chương 6.

Trong tài khoản admin, phòng riêng chưa tham gia vẫn hiển thị để quản lý ở nhóm PHÒNG CỦA TÔI; đây là quyền giám sát, không tự cấp membership hay quyền chat. Cờ membership do Server gửi, không suy từ tên hay việc phòng xuất hiện trong danh sách.

Đốm góc thẻ đăng nhập trong ảnh cũ chưa tái hiện riêng được ở lần thử này. Đã sửa điểm vẽ có thể để lại pixel cũ: card bo góc trước đây khai báo opaque nhưng không tô kín các góc; nay transparent, nền cha vẽ đầy đủ. PNG mới không có đốm. Chữ số viền màu đã xác nhận là LCD subpixel AA trong PNG, đổi grayscale AA tập trung ở Theme và kiểm tra pixel.

M2.7 chỉ thay bố cục/thành phần giao diện; không đổi định dạng gói hay dữ liệu M2.6. Hộp Đổi mật khẩu chưa tồn tại ở mốc này: các JPasswordField hiện có đã được kiểm tra, còn ba ô của hộp Đổi mật khẩu thật sẽ được kiểm tra và chụp ở M3a, cùng Hồ sơ và hộp bắt buộc đổi admin.

## M2.8

Vai trò thẻ tài khoản dùng JLabel riêng: tên 14px đậm, vai trò 12px thường màu xám (không dùng kích thước HTML). Ô soạn chia vùng nhập toàn chiều rộng và hàng công cụ bên dưới; nền khóa vẽ bo tròn, không còn góc xám vuông. Bộ đếm gần ngưỡng hiển thị số ký tự / giới hạn ước tính từ tỷ lệ ký tự so với dung lượng nội dung hiện tại; giới hạn thật vẫn kiểm tra UTF-8 và gói UDP. Mô tả phòng giới hạn 80 ký tự ở cả Client và Server, thông báo bỏ thuật ngữ byte. Theme.vietHoaDau chuẩn hóa chữ đầu lỗi. Kiểm tra Font.PLAIN của JTextArea trong UiPreview vẫn được giữ; tên người gửi là nhãn riêng.

File/hàm: ChatMulticastUI.theTaiKhoan/ganNguoiDung; SoanTinPanel constructor/capNhat/paintComponent; Theme.vietHoaDau; HopThoai.kiemTra; QuanLyPhong.kiemTraNoiDung. Ảnh 05/18 là offline/nhiều dòng được xuất lại.


## M3a — hồ sơ, avatar và mật khẩu

- Tên đăng nhập/id cố định; tên hiển thị (2–30 ký tự) và giới thiệu (tối đa 100 ký tự) đổi được. Nhãn Bạn, quyền và tin của mình vẫn dựa vào tên đăng nhập/id. Server kiểm tra lại tất cả dữ liệu.
- Bấm thẻ tài khoản → **Hồ sơ của tôi**. Chọn PNG/JPG ≤2 MB; ảnh được kiểm tra nội dung, cắt vuông giữa và thu nhỏ 96×96 PNG. Đổi/Xóa chỉ xem trước cục bộ, chỉ bấm Lưu mới truyền. Khi tải có trạng thái phần trăm, Hủy dừng trước lệnh lưu; nếu lệnh lưu đã gửi thì có thể đã được Server xử lý.
- `PhanManh` chia 600 byte thô → Base64, gói cuối dưới 1400 byte, ghép đủ và CRC32 trùng mới chấp nhận. Server giữ ảnh tạm tối đa 60 giây, không thay ảnh chính trước PROFILE_SAVE. Cache Client tối đa 100 hồ sơ + 100 ảnh, tối đa 16 lượt tải ảnh và 32 yêu cầu hồ sơ đồng thời; không lưu ảnh xuống đĩa Client ở mốc này. `anhHash` là CRC32 phiên bản, dùng kiểm tra thay đổi và lỗi truyền, không phải chữ ký bảo mật.
- Lưu thành công: Server phát PROFILE_CHANGED bằng UDP unicast tin cậy tới chính mình và Client chung phòng, cập nhật snapshot thành viên/phòng; tin chat vẫn qua multicast. Tên trong bong bóng đang mở đổi theo hồ sơ, dữ liệu tin gốc không bị sửa. Client tải ảnh nền và vẽ lại thành viên/bong bóng/thẻ tài khoản sau khi cache sẵn sàng.
- **Đổi mật khẩu** có ba ô, Hiện/Ẩn riêng, lỗi ngay dưới ô, Enter gửi. Mật khẩu 6–128 ký tự; kiểm tra mật khẩu cũ ở Server; salt/hash mới và config admin được lưu. Chỉ cho một phiên/tài khoản nên không có phiên khác để hủy; phiên hiện tại giữ đăng nhập và dùng mật khẩu mới trong RAM khi nối lại.
- Admin đang dùng `admin123` nhận cờ bắt buộc đổi từ hash hiện tại; Server chỉ nhận PING/LOGOUT/PASSWORD_CHANGE cho tới khi đổi, không chỉ dựa vào việc khóa UI. Hộp không có Hủy/Đóng và Esc bị vô hiệu; mật khẩu mới phải khác mặc định.
- Nạp CSV 7 cột cũ rồi bổ sung hai cột giới thiệu/hash ảnh; không đổi id, mật khẩu hay dữ liệu phòng cũ. Phải đóng cả Server/Client cũ để chạy cùng giao thức M3a.

File mới: `common/PhanManh.java`, `client/HoSoClient.java`, `server/QuanLyHoSo.java`, `ui/HoSoDialog.java` và `src/test/.../HoSoSelfTest.java`. Mỗi lớp tách theo đúng nhiệm vụ để tổng 35 file ứng dụng, không trộn Swing với mạng; không thêm thư viện. Các file DTO, Protocol, ClientCore/Listener, ServerCore/QuanLyNguoiDung/KhoLuuTru, Theme/ThanhPhan, ChatMulticastUI/ThanhVienPanel/KhungChatPanel/UiPreview và bốn test cũ đã cập nhật.

### Thử tay M3a (1 Server + 3 Client)

1. Chạy Server rồi Client 1/2/3 bằng F5; đăng nhập admin mặc định: thử Esc/X, mật khẩu cũ sai, mới quá ngắn/nhập lại sai, rồi đổi hợp lệ. Hộp chỉ đóng sau thành công, sau đó vào Sảnh.
2. Đăng nhập hai tài khoản thường chung Sảnh. A mở Hồ sơ: đổi tên/giới thiệu, chọn ảnh ngang JPG/PNG; kiểm tra cắt giữa, avatar tròn và xem trước. Chưa bấm Lưu thì B chưa đổi; Lưu xong B thấy tên/ảnh mới trên thành viên và bong bóng, nhãn Bạn vẫn đúng.
3. Thử tệp giả ảnh và >2 MB; thử Hủy sau khi chọn ảnh, Xóa ảnh rồi Lưu. Avatar rỗng trở về chữ cái; đổi/xóa của A không ảnh hưởng B.
4. Đổi mật khẩu thường: ba ô không bị nén khi có lỗi, từng nút Hiện hoạt động; đăng xuất, mật khẩu cũ bị từ chối, mật khẩu mới đăng nhập được. Dừng/bật Server, kiểm tra tên/giới thiệu/avatar và mật khẩu admin mới còn.
5. Xem ảnh 25–30 trong docs/screenshots; thử tiếp soạn nhiều dòng/offline/font từ M2.8. Chạy SelfTest để kiểm tra CRC và mất 20% gói có seed; không mô phỏng mất mạng bằng tắt Wi-Fi khi đang demo phòng chung.

Đã biên dịch Java 21 bằng Maven NetBeans, SelfTest chạy mạng thật trên một máy và UiPreview xuất PNG từ component thật. Chưa kiểm tra thao tác chuột/phím toàn bộ ba cửa sổ trong IDE, nhiều máy LAN hoặc DPI khác. M4, M5, M6, M3c và M7 còn chờ xác nhận.


## M3a.1 — sửa lỗi từ ảnh

Assertion đã tái hiện lỗi `Mô tả` bị gán khóa `ten`: hàm HopThoai.loiServer tìm chuỗi `mô tả` phân biệt hoa/thường. Đã chuẩn hóa khi phân loại lỗi và tách thông báo Server theo từng trường. FormKiemTra.chay nhập riêng tên 1 ký tự, mô tả 81 ký tự, giới hạn 1, mật khẩu 129 ký tự vào form thật để xác nhận chỉ đúng ô có lỗi; kiểm tra cả tạo/sửa/vào, cả validator và thông báo Server. Ảnh 22/23 dùng đúng lỗi tên và mô tả.

SoanTinPanel.capNhat chỉ cho thanh cuộn dọc hiện khi vượt 5 dòng kể cả dòng tự ngắt; UiPreview xác nhận ô một dòng không lộ thanh cuộn và thêm ảnh 31-soan-co-thanh-cuon.png. Nút Hiện mật khẩu dùng nút viền; lỗi cũ/mới/nhập lại phân biệt đúng, ẩn gợi ý khi ô mới có lỗi. Hồ sơ vô hiệu Xóa ảnh khi chưa có ảnh, bộ đếm giới thiệu n/100. Login thật trả cùng thông báo “Tên đăng nhập hoặc mật khẩu không đúng.”; thông báo tồn tại chỉ dùng khi đăng ký. Admin mặc định có khung cảnh báo vàng. Tên hiển thị ngoài BMP bị từ chối ở cả Client/Server với thông báo rõ để người dùng sửa, không âm thầm mất ký tự.

File/hàm: ui/HopThoai.loiServer/kiemTra; ui/FormKiemTra.chay/kiemTraPhong/kiemTraCuon; ui/SoanTinPanel.capNhat; ui/HoSoDialog.formHoSo/formMatKhau/kiemTra/oGioiThieu; server/QuanLyPhong.kiemTraNoiDung; server/QuanLyNguoiDung.dangNhap/luuHoSo; ui/UiPreview.xuatLoiForm. SelfTest hồi quy 37 PASS; UiPreview 31 PNG. Thao tác thật nhiều máy LAN và DPI khác vẫn cần thử tay.


## M3b rút gọn (cập nhật tài nguyên ở M3b.1) — sticker, thu hồi, trạng thái gửi, đang nhập

- **Sticker:** nút mặt cười mở popup 4 nhóm, mỗi nhóm 9 hình; 36 PNG Noto Emoji 128×128, hiển thị trong chat 112px, không có nền bong bóng. Hover hiện tên. Lệnh `STK` chỉ chứa mã hình; Server từ chối mã lạ, renderer có dòng “Sticker không hỗ trợ” nếu gặp dữ liệu từ phiên bản khác.
- **Thu hồi / Sao chép:** chuột phải trực tiếp lên tin. Người gửi có 10 phút; chủ phòng/admin được thu hồi tin khác không hạn thời gian, phải đang ở phòng. Server kiểm tra lại. Tin thu hồi thành chữ xám nghiêng; bỏ nội dung cũ khỏi bản lưu, lịch sử/RESYNC và sidebar. Bản tin cũ đến chậm không làm nội dung xuất hiện lại. Thu hồi không thể xóa bản sao mà người khác đã chép/lưu trước đó.
- **Trạng thái:** khi gửi xuất hiện một hàng “Đang gửi”; chỉ “Đã gửi” sau khi có RESPONSE đúng msgId và tin multicast quay về. Thiếu xác nhận khoảng 8 giây thành “Lỗi” kèm Gửi lại/Xóa. Khi gửi lại giữ nguyên mã 128-bit do Client tạo; Server dùng room + user + mã để trả lại cùng msgId và phát lại, không lưu thêm tin. Xóa chỉ bỏ hàng lỗi cục bộ, không xóa tin có thể đã tới Server. Khi vào lại phòng, lịch sử đã lưu vẫn có thể xuất hiện.
- **Đang nhập:** chỉ gửi khi có chữ, tối đa 1 lần/3 giây theo từng phòng ở cả Client/Server; UDP unicast nhanh tới Server rồi multicast đúng nhóm, không ACK/không lịch sử. Tự tắt sau 4 giây, nhiều người hiện “An và 2 người khác đang nhập...”. Mất một sự kiện có thể không hiện dòng này, không ảnh hưởng tin chat.
- Gói chat/log vẫn giữ 7 trường để nạp lịch sử cũ, trường nội dung mới có phần đánh dấu mã Client/loại. Chat văn bản vẫn 1000 byte, luôn kiểm tra thêm header/escape dưới 1400 byte. Gói `HEAD` thêm revision thu hồi; `RECALLS` trả từng lô tối đa 50 ID đã thu hồi để đồng bộ cả tin cũ không đổi msgId.
- Tách lịch sử/chống trùng/thu hồi ra `server/QuanLyTinNhan`, tách trạng thái/typing/đồng bộ thu hồi ra `client/TroChuyenClient`, và renderer hàng tin `ui/HangTin`, kho hình `ui/StickerKho`. `ui/FormKiemTra` ở M3a.1 giữ assertions form tách khỏi UiPreview. Tổng tăng từ 35 lên 40 file ứng dụng để giữ ranh giới mạng/Swing và từng file dưới 400 dòng; không thêm thư viện.

Kiểm thử: toàn bộ **43 PASS, 0 FAIL** (37 hồi quy + 6 nhóm M3b). Nhóm mới kiểm tra mã hợp lệ/lạ, cách ly phòng, biên 10 phút, chủ/admin/không có quyền, nội dung trên đĩa đã xóa, HISTORY/RESYNC sau thu hồi, thiếu multicast 8 giây rồi retry không trùng, limiter typing và restart giữ mã/tombstone. UiPreview **37 PNG**, bổ sung 31 thanh cuộn khi quá 5 dòng, 32 picker, 33 sticker/thu hồi/ba trạng thái/typing, 34 menu, 35 cập nhật không trùng và hết typing, 36 sticker không hỗ trợ. Có assertions hàng gửi không trùng, typing tắt sau 4 giây và tin cũ không khôi phục nội dung.

### Thử tay M3b (1 Server + 3 Client)

1. Đóng toàn bộ tiến trình cũ; chạy Server rồi Client 1/2/3, đăng nhập và vào cùng một phòng. Chọn mặt cười, thử cả 4 nhóm sticker; Client khác phải thấy đúng hình/tên/giờ, không có bong bóng nền.
2. Gửi văn bản, quan sát Đang gửi → Đã gửi. Dừng Server rồi thử gửi trước khi heartbeat khóa soạn: hàng lỗi có Gửi lại/Xóa. Bật lại, chờ dải kết nối phục hồi rồi bấm Gửi lại; Client khác chỉ có một tin. Có thể dùng SelfTest để mô phỏng thiếu multicast chính xác hơn.
3. Chuột phải tin của mình → Sao chép/Thu hồi; kiểm tra nội dung đổi sang dòng thu hồi ở mọi Client. Thử tài khoản thường thu hồi tin khác (không có menu); chủ/admin trong phòng có quyền. Đăng xuất/vào lại vẫn không lộ nội dung cũ.
4. A gõ liên tục không gửi: B thấy đang nhập; hai người gõ thì hiện số người còn lại. Dừng gõ hơn 4 giây thì dòng biến mất. Chuyển sang phòng khác không được thấy typing của phòng cũ.
5. Dừng/bật Server, kiểm tra lịch sử sticker/thu hồi; xem ảnh 31–36 và chạy lại SelfTest. Chức năng M3c chưa có ở menu và không được tính là đã làm.

Lệnh chạy giữ như trên: Maven `-q compile`, `-q test-compile`; SelfTest với `-Dexec.classpathScope=test`, UiPreview bằng main tương ứng. Chưa thử tương tác trực tiếp đầy đủ trong IDE, nhiều máy LAN hay DPI khác. UDP không đảm bảo, multicast cần chung LAN/Windows Firewall; token/mật khẩu chưa mã hóa. Lịch sử và tombstone hiện giữ RAM theo số tin; thu hồi ghi lại file log nên chưa phù hợp lịch sử rất lớn. Cache mã chống trùng nạp từ log; Client giữ tối đa 256 hàng chờ/lỗi, logout dọn. Mã chỉ chống gửi lại trùng, không phải cơ chế xác thực nguồn multicast.

Ảnh [37-kho-sticker-day-du.png](docs/screenshots/37-kho-sticker-day-du.png) cho xem đầy đủ 36 PNG Noto Emoji (ảnh nguồn 128px, thu phóng bicubic).


### M4: kiểm thử tệp

```powershell
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q clean test-compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.TepSelfTest' '-Dexec.classpathScope=test'
```

Chỉ chạy kiểm thử mất gói: thêm `'-Dexec.args=matgoi'`. Dữ liệu kiểm thử nằm trong `.selftest/`,
không đụng `data/` thật. Kiểm thử timeout chờ thực tế khoảng 60 giây.
Nút **+** gửi tệp/ảnh; có thể kéo thả vào vùng chat. Hai tệp gửi đồng thời, các tệp sau chờ.
Tải về hỏi nơi lưu, mặc định Downloads; tự đổi tên nếu đã có tệp cùng tên.
Cấu hình giới hạn tại `data/config.properties`: `upload.maxBytes=10485760`, `upload.maxRetries=8`.
Nguồn sticker và giấy phép: [NGUON_TAI_NGUYEN.md](docs/NGUON_TAI_NGUYEN.md).


Kết quả nghiệm thu M4 ngày 14/11/2026: **56 PASS, 0 FAIL** cho toàn bộ SelfTest;
riêng TepSelfTest **12 PASS, 0 FAIL**. UiPreview **43 PNG**, Maven compile không cảnh báo.
Tệp 5 MB không mất gói đạt khoảng 1,66 MB/s gửi lên và 1,88 MB/s tải xuống trên localhost;
mất 20% DATA/ACK khoảng 0,11–0,18 MB/s gửi lên và 0,13–0,17 MB/s tải xuống ở hai lượt cuối.
Đây chưa phải phép đo hai máy LAN/Wi-Fi. Bảng chức năng/file/hàm, kết quả chi tiết và giới hạn
được ghi tại [GIAI_THICH.md](docs/GIAI_THICH.md).
