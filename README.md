# UDPmultiChat — mốc M2.5

Java **21**, Swing, Maven. Hai điểm chạy rõ ràng: `com.mycompany.udpmultichat.ClientMain` và `com.mycompany.udpmultichat.server.ServerMain`. Giao diện dùng **FlatLaf 3.7.2**, giấy phép Apache 2.0 ([trang chính thức](https://www.formdev.com/flatlaf/)); đây là thư viện duy nhất của ứng dụng, được bổ sung theo yêu cầu M2.5.

## Đã hoàn thành

- M1: tài khoản SHA-256 + salt, token gắn endpoint UDP, chặn đăng nhập hai nơi, điều khiển có seq/ACK/gửi lại; heartbeat 5 giây, phiên quá 15 giây không PING bị coi là offline.
- M2: CRUD phòng, mã 6 ký tự, công khai/riêng tư, mật khẩu phòng, nhiều tab, cập nhật realtime, lịch sử 50 tin, badge chưa đọc, RESYNC theo từng lô 50 tin.
- M2.5: đăng nhập/đăng ký riêng, xác nhận mật khẩu, hiện/ẩn mật khẩu, lỗi ngay dưới ô, kiểm tra Server, vòng chờ. Chat ba cột, danh sách phòng hai dòng, menu ⋮, thẻ tài khoản, nhóm thành viên online/offline, toast bốn mức, ô soạn nhiều dòng và bộ đếm byte. Mọi màu/font dùng chung `Theme.java`.
- Vá M2.5: lưu đúng loại phòng; giấu phòng riêng tư với người ngoài; lưu tư cách thành viên khi offline; chuyển chủ phòng trước khi rời; kiểm tra dữ liệu ở Client và Server; đóng cửa sổ gửi đăng xuất; tự nối lại và RESYNC sau mất kết nối.

**Sảnh chung** luôn công khai, mở, không mật khẩu; không được Sửa/Xóa/Rời/Đóng. Phòng tạo hoặc sửa dùng tên 3–30 ký tự, mô tả tối đa 240 byte UTF-8, giới hạn 2–500 thành viên. Sảnh hệ thống giữ giới hạn cũ 1000 nếu dữ liệu được tạo từ M2.

Mốc tiếp theo, mỗi mốc đều dừng chờ xác nhận:

| Mốc | Phần việc |
|---|---|
| M3 | Hồ sơ, avatar UDP, sticker, typing, tìm kiếm, thu hồi, trạng thái gửi/gửi lại, phân cách ngày/gộp tin/sao chép, đổi mật khẩu và bắt buộc đổi mật khẩu admin mặc định |
| M4 | File UDP có ACK/CRC32/tiến trình/hủy, tải xuống, xem trước ảnh, quyền truy cập |
| M5 | Dashboard, quản trị tài khoản/phòng, kick/mute/ban, broadcast, chống spam, nhật ký, báo cáo CSV + HTML |
| M6 | Sáng/Tối, co giãn, phím tắt, hoàn thiện cấu hình và tài liệu |
| M7 | Rà soát toàn bộ, sửa lỗi, chạy lại SelfTest/UiPreview, báo cáo đối chiếu yêu cầu |

Hồ sơ/đổi mật khẩu/Sáng-Tối và các nút sticker/đính kèm/quản trị thành viên hiện là chỗ chờ có ghi mốc tương ứng, chưa hoạt động. Discovery tự tìm Server chưa triển khai; hiện nhập IP thủ công.

## Chạy bằng VS Code / NetBeans

1. Mở thư mục chứa `pom.xml`, dùng JDK 21 và Extension Pack for Java/Maven for Java. `.vscode/settings.json` trỏ JDK/Maven của máy hiện tại; máy khác cần sửa đường dẫn.
2. Đóng các tiến trình bản cũ. Nhấn **Ctrl+Shift+D**, chọn **Server**, F5; bấm **Bắt đầu** trong cửa sổ giám sát.
3. Lần lượt chọn **Client 1**, **Client 2**, **Client 3**, F5 cho mỗi cấu hình; giữ các phiên trước đang chạy. Đây là ba tiến trình riêng, cùng chạy ClientMain.
4. IP mặc định `localhost`, cổng 5000. Đổi IP tại **Cài đặt máy chủ**, bấm **Kiểm tra kết nối**. Tạo tài khoản ở tab Đăng ký, sau đó đăng nhập.
5. Admin mặc định **admin / admin123**. Tên tài khoản 3–20 chữ `a-z`, số hoặc `_`, không phân biệt hoa/thường; mật khẩu 6–128 ký tự.

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
- Bấm phòng công khai để vào/chuyển tab; phòng có mật khẩu sẽ hỏi thêm. **Nhập mã** để vào phòng riêng tư: sai mã, sai mật khẩu, phòng đầy đều báo rõ. Admin nhìn thấy mọi phòng nhưng vẫn chịu giới hạn số thành viên.
- Menu **⋮** khi rê chuột/chọn phòng, hoặc chuột phải: Sửa, Đóng/Mở, Chuyển chủ, Xóa cho chủ/admin; Rời cho thành viên thường; Sao chép mã và Thông tin phòng. Xóa/Rời có xác nhận. Sảnh chung chỉ cho xem thông tin/sao chép mã.
- **Chủ phòng phải chuyển quyền cho thành viên khác trước khi rời**, hoặc xóa phòng. Người nhận quyền có thể đang offline. Sau chuyển quyền, chủ cũ trở thành thành viên thường.
- **Đóng phòng** chặn gửi tin và người mới vào; thành viên đã có có thể mở lại tab để xem lịch sử. **Rời phòng** xóa tư cách thành viên; **đăng xuất/tắt Client/timeout** chỉ chuyển offline, vẫn là thành viên. Giới hạn tính cả thành viên offline.
- Enter gửi, Shift+Enter xuống dòng; ô soạn cao tối đa 5 dòng. Tin tối đa **600 byte UTF-8**: 600 ký tự ASCII hoặc khoảng 200 ký tự Việt loại 3 byte, tùy nội dung. Bộ đếm hiện từ 480 byte; quá giới hạn khóa Gửi và báo ngay dưới ô. Gửi lỗi giữ nội dung để thử lại.
- Tin phòng khác tăng badge; lịch sử không tăng badge. Thành viên chia theo quyền và có chấm online/offline; nhãn **Bạn** chỉ chính mình.
- Đăng xuất nằm trong menu thẻ tài khoản dưới thanh bên. Nút X cũng gửi đăng xuất trên luồng nền. Tắt cưỡng bức được Server phát hiện sau khoảng 15 giây.
- Mất Server: sau thời gian phát hiện heartbeat, dải vàng hiện và khóa soạn tin. Client thử lại mỗi 5 giây, đăng nhập lại, khôi phục các tab đang mở và RESYNC từ msgId đã nhận; thành công hiện dải xanh rồi tự ẩn. Mật khẩu phiên chỉ giữ trong RAM để nối lại, xóa khi đóng phiên.

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

Ba file test ở `src/test/java`: SelfTest, PhongSelfTest, GiaoDienSelfTest. Dữ liệu riêng `.selftest/`, cấu hình Client test cũng tách riêng; không dùng data thật. UDP điều khiển dùng cổng tạm, multicast test dùng gốc 19000/21000/23000. Chạy một phiên SelfTest mỗi lần.

Kết quả M2.5 trên máy hiện tại: **26 PASS, 0 FAIL**, gồm:

- Tài khoản, timeout, mất ngẫu nhiên 20% DATA/ACK với seed `20261112/20261113` (thứ tự lập lịch hệ điều hành vẫn có thể khác).
- CRUD/quyền/mật khẩu/phòng đầy, phòng riêng không lộ, lưu đúng loại, chuyển quyền và nạp lại sau restart.
- Multicast thật qua card Wi-Fi: cách ly phòng, cả hai nhóm dùng cùng cổng; lịch sử đúng 50; RESYNC tin giữa/tin cuối và hơn 50 tin liên tiếp.
- Tin Việt quá 600 byte bị chặn ở Client và Server; biên 600 byte được nhận.
- HELLO, thành viên offline, Server dừng/bật lại, Client tự nối lại và bù 75 tin đã thiếu, không trùng/sai thứ tự; cấu hình Client không có mật khẩu/token.

Tên GiaoDienSelfTest chỉ nhóm các lỗi phát hiện khi thử giao diện; test này gọi logic mạng/quản lý, không giả lập thao tác chuột Swing. Việc bấm F5, hover, kéo cửa sổ cần kiểm tra tay.

## UiPreview — xem giao diện không cần Server

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview'
```

Xuất **14 PNG** vào [docs/screenshots](docs/screenshots/README.md): đăng nhập/đăng ký/lỗi inline, chat, mất kết nối, toast, menu tài khoản, hộp thoại phòng, Server và giới hạn tin. Công cụ vẽ chính component Swing đang dùng lên BufferedImage với dữ liệu mẫu; không mở socket, không phải ảnh chụp thao tác desktop. Cần môi trường đồ họa để tạo JFrame. Đã xem các PNG để kiểm tra bố cục; đây không thay thế thử tương tác.

## Dữ liệu

```text
data/
├── config.properties    # Cau hinh admin, multicast.portBase=9000
├── users.csv            # Tai khoan, salt/hash
├── rooms.csv            # Phong, loai, ma, salt/hash mat khau phong
├── members.properties   # Tu cach thanh vien ben vung roomId.userId
└── rooms/<roomId>.log    # Tin nhan UTF-8 da escape
client.properties        # Chi username/server lan dang nhap thanh cong gan nhat
```

Chỉ chạy một Server cho mỗi data, từ thư mục project. Token/trạng thái online chỉ trong RAM. CSV/properties ghi file tạm rồi thay thế, có thử lại ngắn khi Windows tạm khóa file; log ghi tuần tự và force xuống đĩa. Phòng đã xóa không còn trong danh sách; log cũ giữ để bảo toàn dữ liệu. Khi nâng từ M2, chỉ chủ phòng có thể khôi phục tư cách thành viên từ dữ liệu cũ; người khác cần vào bằng mã một lần để ghi membership mới.

Đổi admin trước khi M3 có menu đổi mật khẩu: dừng Server, thêm `admin.password=MatKhauMoiDu6KyTu` vào config rồi bật lại. Server chuyển thành salt/hash và xóa giá trị thô sau khi đọc. Nếu đổi khi Client đang mất kết nối, Client phải đăng xuất và đăng nhập bằng mật khẩu mới.

`target/`, `data/`, `reports/`, `.selftest/`, log, `client.properties` và file tạm được bỏ qua bởi Git. Commit chỉ ở repository cục bộ, không tự push lên GitHub.

## Thử tay M2.5 (1 Server + 3 Client)

1. Chạy Server và Client 1/2/3 theo cấu hình F5. Thử đăng ký nhập lại sai, hiện/ẩn mật khẩu, kiểm tra kết nối; đăng nhập hai user và admin. Sau đăng nhập chỉ còn màn hình chat ba cột.
2. A tạo phòng Riêng tư có mật khẩu và giới hạn 2. B chưa thấy phòng, nhập sai mã/sai mật khẩu rồi vào đúng; admin thấy phòng nhưng vào sẽ báo đầy. Sửa loại/mật khẩu rồi mở lại hộp sửa để kiểm tra. Sảnh không có Sửa/Xóa/Rời.
3. Chat giữa hai phòng, thử Shift+Enter, tin Việt dài và bộ đếm; để B xem phòng khác để thấy badge. Thử toast đóng/hover; ẩn/hiện thành viên. A chuyển chủ cho B, sau đó rời phòng bằng menu và xác nhận.
4. Đóng một Client bằng X: Server ghi đăng xuất, người đó chuyển offline trong danh sách thành viên. Thử tắt cưỡng bức: đợi khoảng 15–20 giây để Server phát hiện. Menu tài khoản là nơi duy nhất có Đăng xuất.
5. Dừng Server khi Client còn mở; chờ dải vàng và ô soạn khóa. Bật lại Server, chờ Client nối lại/RESYNC và dải xanh. Thử chat tiếp, kiểm tra lịch sử/loại phòng/membership còn sau restart.

## Giới hạn đã biết

- **UDP không đảm bảo**: điều khiển gửi lần đầu + tối đa 3 lần gửi lại cách 500 ms; hết lượt báo lỗi. ACK xác nhận vào hàng xử lý, RESPONSE mới là kết quả nghiệp vụ. Multicast không ACK; RESYNC cần Server và lịch sử đã lưu.
- **Multicast cần chung mạng LAN** hỗ trợ multicast và **cho Java qua Windows Firewall**: UDP 5000 và 9001–9254 mặc định. Wi-Fi cô lập thiết bị/VPN có thể chặn multicast. Chưa thử nhiều máy LAN trong mốc này.
- **Token/mật khẩu đi qua UDP không mã hóa**; multicast chưa xác thực nguồn phát. Salt/hash chỉ bảo vệ dữ liệu trên đĩa. Dùng mạng tin cậy cho bài demo.
- Tối đa 254 phòng, gói dưới 1400 byte; nội dung có nhiều escape có thể chạm giới hạn gói trước 600 byte và sẽ báo lỗi. Lịch sử hiện nạp RAM; chưa tối ưu hàng triệu tin.
- Phát hiện mất kết nối không tức thì, phụ thuộc heartbeat/gửi lại (thường khoảng 15–20 giây). Tự nối lại cần IP/cổng/credentials còn đúng; phòng đã bị xóa không được khôi phục. Đổi cổng multicast cần khởi động lại Client.
- Chưa kiểm tra tương tác F5/Run File trực tiếp trong VS Code/NetBeans, hay DPI/phóng to chữ trên các máy khác. Đã kiểm tra cấu hình, biên dịch, mạng thật trong SelfTest và PNG của component Swing.

## Cấu trúc và tài liệu chỉ code

31 file Java ứng dụng + 3 file test ở mốc này, mỗi file dưới 400 dòng. Giữ bốn gói common/client/server/ui; UI không có Socket, logic không import Swing (ngoại lệ ServerMain). Quyền phòng gộp trong QuanLyPhong, lưu trữ gộp trong KhoLuuTru, DTO thành viên gộp trong Phong. Dialog tách khỏi DanhSachPhongPanel sang HopThoai để dùng chung và giữ file ngắn. Màu/font/nút/vẽ icon gom vào Theme, Avatar/BongTin còn trong ThanhPhan. Các phần M3–M5 sẽ ưu tiên mở rộng các lớp theo nhiệm vụ hiện có.

Xem [docs/GIAI_THICH.md](docs/GIAI_THICH.md) để tra bảng chức năng → file → hàm và đối chiếu UDP/Multicast chương 6.
