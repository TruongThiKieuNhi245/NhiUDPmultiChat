# UDPmultiChat — mốc M1

Java 21 + Swing, Maven, không có thư viện ứng dụng bên ngoài. Hai điểm chạy:

- `com.mycompany.udpmultichat.server.ServerMain`: cửa sổ giám sát Server.
- `com.mycompany.udpmultichat.ClientMain`: cửa sổ Client; chạy nhiều lần để mở nhiều Client.

## Chức năng đã có

Đăng ký, đăng nhập (SHA-256 + salt), chặn trùng tên/đăng nhập hai nơi, token gắn với địa chỉ UDP của Client, heartbeat, danh sách online, chat tiếng Việt trong **Sảnh chung** qua multicast. Giữ bong bóng, avatar chữ cái, gradient và nút bo tròn từ bản gốc. Server có nút Bắt đầu/Dừng, log và thống kê tài khoản/online/tin.

M1 chỉ có một phòng mặc định. CRUD phòng, lịch sử/RESYNC thuộc M2; hồ sơ/sticker M3; file M4; quản trị/báo cáo M5; hoàn thiện M6. Chưa có các chức năng đó trong bản M1.

## Chạy bằng VS Code

1. Mở thư mục chứa `pom.xml`, dùng Extension Pack for Java và Maven for Java đang có trên máy.
2. `.vscode/settings.json` đã đặt JDK 21 và Maven NetBeans theo đường dẫn thực tế. Máy khác cần đổi hai đường dẫn này.
3. Run and Debug → **Server** → F5. Trong cửa sổ Server bấm **Bắt đầu**.
4. Chọn **Client** → F5, sau đó **Client (cua so thu 2/3)** để mở thêm cửa sổ. Có thể chạy lại cấu hình này.
5. IP mặc định `localhost`, cổng 5000. Đăng ký tên `a-z`, số, `_`, dài 3–20 ký tự (không phân biệt hoa/thường); mật khẩu 6–128 ký tự. Đăng ký xong bấm Đăng nhập.

Admin mặc định: **admin / admin123**. Server in cảnh báo khi còn dùng mật khẩu này. M1 mới xác định vai trò, Dashboard sẽ có ở M5.

## Chạy bằng PowerShell / Maven

Chạy từ thư mục chứa `pom.xml`. Mỗi Client cần một terminal riêng. Maven chưa có trong PATH nên dùng Maven sẵn của NetBeans:

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.server.ServerMain'
```

Client (lệnh mặc định cũng là ClientMain):

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ClientMain'
```

Nếu đã thêm Maven vào PATH, thay `& $mvnChat` bằng `mvn`. NetBeans: Open Project → chọn JDK 21 → mở `ServerMain.java`/`ClientMain.java` → Run File. Main mặc định của project là ClientMain.

## SelfTest không giao diện

Đặt tại `src/test/java/com/mycompany/udpmultichat/SelfTest.java`. Chạy khi không có phiên chat khác để tránh trộn gói trong Sảnh chung:

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q test-compile
java '-Djava.net.preferIPv4Stack=true' -cp 'target/test-classes;target/classes' com.mycompany.udpmultichat.SelfTest
```

Hoặc dùng Maven (nhớ giữ dấu nháy quanh tham số `-D` trong PowerShell):

```powershell
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.SelfTest' '-Dexec.classpathScope=test'
```

Test sử dụng cổng UDP điều khiển tạm thời, nhóm multicast thật và dữ liệu riêng trong `.selftest/m1-*`. In PASS/FAIL; thoát lỗi nếu có FAIL. Kiểm tra escape/UTF-8, kích thước gói, mất 20% gói DATA/ACK với seed `20261112/20261113`, đúng 3 lần gửi lại khi mất 100%, tài khoản, multicast, roomId, heartbeat 15 giây, đăng xuất và nạp lại tài khoản. Mất gói có seed tái lập luồng ngẫu nhiên; thời điểm các luồng/ACK trên hệ điều hành vẫn có thể khác. Chưa kiểm thử chức năng thuộc M2–M5.

## Dữ liệu và đổi mật khẩu admin

Server tự tạo `data/config.properties` và `data/users.csv`, UTF-8. Tài khoản chỉ lưu salt/hash; phiên không lưu xuống đĩa. Ghi file tạm rồi thay thế file đích. Chạy từ cùng thư mục project để dùng đúng dữ liệu. Chỉ chạy **một Server cho một thư mục data**.

Đổi mật khẩu admin ở M1: dừng Server, thêm dòng `admin.password=MatKhauMoiDu6KyTu` vào `data/config.properties`, rồi khởi động Server. Server đọc giá trị một lần, chuyển sang `admin.salt` + `admin.passwordHash`, xóa trường mật khẩu thô khỏi config và cập nhật tài khoản admin. Không đăng config hay users.csv lên Git. Lịch sử tin và phòng sẽ được lưu từ M2; thống kê tin hiện tại chỉ tính trong lần bật Server.

## Thử tay M1 với 3 Client

1. Bật Server. Mở 3 Client: đăng ký `sinhvien_a`, `sinhvien_b`; cửa sổ cuối dùng admin.
2. Đăng nhập cả ba. Kiểm tra danh sách có 3 người và log Server ghi đăng nhập/vào Sảnh chung. Thử dùng tài khoản đã online ở cửa sổ khác, phải bị từ chối.
3. Gửi `Xin chào | lớp mạng` ở A, trả lời ở B. Cả ba thấy tin, tên, giờ do Server cấp; tin của mình nằm bên phải.
4. Đăng xuất B: A/admin cập nhật danh sách. Để kiểm tra hết heartbeat, kết thúc **tiến trình Java của B** thay vì đóng cửa sổ bình thường, chờ khoảng 16–20 giây. Dừng Server để xem Client báo mất kết nối sau heartbeat.
5. Dừng/bật Server, đăng nhập lại tài khoản đã tạo. Tài khoản còn; tin cũ chưa được khôi phục ở M1.

## Giới hạn đã biết

- UDP không đảm bảo giao hàng/thứ tự. ReliableUdp gửi lần đầu + tối đa 3 lần gửi lại, mỗi 500 ms, rồi báo lỗi; không đảm bảo tuyệt đối. ACK là xác nhận nhận vào hàng xử lý, RESPONSE là kết quả nghiệp vụ.
- M1 chưa có RESYNC/lịch sử; tin multicast bị mất chưa được gửi bù. Giới hạn nội dung tin 600 byte UTF-8 và toàn bộ datagram dưới 1400 byte; nhiều ký tự escape có thể làm chạm giới hạn sớm hơn.
- Multicast cần chung mạng LAN có hỗ trợ multicast và cho Java qua Windows Firewall (UDP 5000, 9001 trong M1). Wi-Fi có cô lập máy khách, VPN hoặc card ảo có thể ngăn multicast. Dùng địa chỉ LAN của Server khi chạy nhiều máy.
- Token/mật khẩu đi qua UDP **không mã hóa**. Hash + salt bảo vệ dữ liệu lưu trữ, không mã hóa đường truyền; chỉ demo trên mạng tin cậy. Multicast M1 cũng chưa xác thực nguồn phát.
- Khi mất mạng hoặc dừng Server, Client cần thời gian heartbeat để phát hiện; phiên trên Server hết hạn khi quá 15 giây không PING. Sau lỗi đăng nhập do mất phản hồi, có thể cần chờ hết phiên trước khi thử lại.
- Discovery, thu hồi, tìm kiếm, Sáng/Tối là ưu tiên sau cùng theo yêu cầu. Các mốc sau chưa được triển khai trong M1.

## Thay đổi từ bản gốc

- `MulticastChat.java` → `client/ClientCore.java`, `client/KenhPhong.java` và `common/MangUtil.java`.
- `ChatMulticastUI.java` → `ui/ChatMulticastUI.java`, `ui/DangNhapPanel.java`, `ui/ThanhPhan.java`; không giữ class cùng tên tại package gốc.
- Thêm ClientMain/ServerMain; sửa POM từ release 26 về 21 và bỏ main class không tồn tại.
- Gộp thành phần vẽ tay trong `ThanhPhan`, tài khoản + phiên trong `QuanLyNguoiDung`, config + CSV trong `KhoLuuTru`. Mục tiêu cuối khoảng 30–35 file Java, giữ bốn gói chức năng common/client/server/ui.
