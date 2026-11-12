# UDPmultiChat — mốc M2

Java **21**, Swing, Maven; không thêm thư viện chạy ứng dụng. Hai điểm chạy:

- `com.mycompany.udpmultichat.server.ServerMain`: giám sát, nút Bắt đầu/Dừng, log và thống kê.
- `com.mycompany.udpmultichat.ClientMain`: Client; mỗi lần chạy là một cửa sổ riêng.

## Đã có ở M1/M2

- Đăng ký/đăng nhập, SHA-256 + salt, token gắn endpoint UDP, chặn trùng tên/đăng nhập hai nơi; PING mỗi 5 giây, hết phiên khi quá 15 giây.
- CRUD phòng; mã 6 ký tự; công khai/riêng tư, mật khẩu tùy chọn, giới hạn 1–1000 người; chủ phòng/admin sửa/xóa/đóng-mở. Mật khẩu phòng cũng lưu salt/hash.
- **Sảnh chung** luôn tồn tại, công khai, mở và không có mật khẩu. Không được xóa, đổi tên/loại hay đóng; admin có thể sửa mô tả/giới hạn.
- Mỗi phòng một nhóm `239.255.42.X`, một cổng `9000 + X` (X=1..254). Client lọc roomId cho mọi gói multicast.
- Nhiều phòng = nhiều tab; danh sách realtime; thành viên/role theo phòng; badge chưa đọc trên danh sách và tab. Tin lịch sử không tăng badge.
- Lịch sử **50 tin gần nhất** khi vào phòng; RESYNC tự gửi bù theo lô tối đa 50, sắp thứ tự và loại trùng. Heartbeat kèm mốc tin từng phòng để phát hiện cả tin cuối bị mất.
- Thông báo UI có dấu; địa chỉ multicast chỉ ở tooltip thông tin phòng/log Server, không chèn vào khung chat.
- Lưu/nạp tài khoản, phòng và lịch sử UTF-8. Không thay đổi schema tài khoản M1.

M3: hồ sơ/avatar/sticker/typing; M4: file; M5: Dashboard/kick/mute/ban/báo cáo; M6: hoàn thiện UI/tài liệu. Các chức năng này chưa có trong M2. Discovery, thu hồi, tìm kiếm, Sáng/Tối là ưu tiên sau cùng.

## Run/Debug trong VS Code

1. Mở đúng thư mục chứa `pom.xml`. Máy cần Extension Pack for Java và Maven for Java.
2. `.vscode/settings.json` đặt Java 21 và Maven đi kèm NetBeans theo đường dẫn trên máy hiện tại. Máy khác cần sửa các đường dẫn này.
3. Nhấn **Ctrl+Shift+D** → chọn **Server** trong danh sách cấu hình ở phía trên → F5. Trong cửa sổ giám sát, bấm **Bắt đầu**.
4. Chọn lần lượt **Client 1**, **Client 2**, **Client 3** rồi F5 để mở ba tiến trình Client. Chọn đúng cấu hình mới trước mỗi lần F5, giữ các phiên trước đang chạy.
5. IP mặc định `localhost`, cổng 5000. Nút Run trên `ClientMain.java`/`ServerMain.java` cũng dùng được; file UI cũ ở package gốc đã được thay thế.

Admin mặc định: **admin / admin123**. Server cảnh báo khi chưa đổi mật khẩu. Tài khoản thường: tên 3–20 ký tự `a-z`, số, `_`, không phân biệt hoa/thường; mật khẩu 6–128 ký tự. Đăng ký xong bấm Đăng nhập.

## Maven / NetBeans

Chạy tại thư mục chứa POM. Maven chưa có trong PATH, dùng Maven NetBeans:

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.server.ServerMain'
```

Mỗi Client chạy trong một terminal khác:

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ClientMain'
```

Đã có Maven trong PATH thì thay `& $mvnChat` bằng `mvn`. NetBeans: Open Project, dùng JDK 21, mở `server/ServerMain.java` hoặc `ClientMain.java` rồi Run File. Main mặc định trong POM là ClientMain. Dừng các tiến trình cũ rồi chạy lại để nạp mã M2.

## Dùng phòng

- **+ Tạo phòng**: nhập tên, mô tả một dòng, loại, mật khẩu tùy chọn và giới hạn. Chủ phòng tự vào tab mới sau khi tạo; mã phòng hiện ở đầu tab.
- Bấm một phòng trong danh sách để vào/chuyển tab. Phòng có mật khẩu yêu cầu nhập mật khẩu, trừ chủ/admin.
- **Nhập mã phòng → Vào**: nhập mã 6 ký tự, mật khẩu nếu có (không có thì để trống). Phòng riêng tư chưa vào không hiện với người ngoài; chủ/admin vẫn thấy để quản lý.
- Chuột phải phòng mình sở hữu hoặc admin: Sửa, Đóng/Mở lại, Xóa. Khi sửa, không chọn đổi mật khẩu thì giữ nguyên; chọn đổi và để trống thì gỡ mật khẩu.
- **Đóng**: giữ tab/lịch sử nhưng tạm chặn vào mới và gửi tin. **Rời phòng**: rời tab đang chọn, tài khoản vẫn online ở những phòng khác.
- Mã phòng là thông tin để chia sẻ quyền tìm/vào phòng riêng tư; nếu cần thêm bảo vệ, đặt mật khẩu phòng.

## SelfTest không giao diện

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q test-compile
java '-Djava.net.preferIPv4Stack=true' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'target/test-classes;target/classes' com.mycompany.udpmultichat.SelfTest
```

Hoặc `& $mvnChat -q exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.SelfTest' '-Dexec.classpathScope=test'` sau test-compile. Nếu terminal hiển thị log không đúng dấu, dùng terminal UTF-8; UI Swing vẫn nhận chuỗi Unicode.

`SelfTest` và `PhongSelfTest` ở `src/test/java`. Dữ liệu riêng `.selftest/m1-*`, `.selftest/m2-*`; UDP Server dùng cổng tạm, multicast dùng cổng gốc 19000/21000 để không trộn với chat thật trên 9000. Một test tạo hai KenhPhong khác nhóm nhưng cùng cổng để xác nhận lọc roomId. Bộ test gồm M1, CRUD/quyền/mật khẩu/capacity, realtime, cách ly phòng, lịch sử đúng 50, RESYNC tin giữa/tin cuối và mất 70 tin liên tiếp, dữ liệu sau khởi động lại. Mất ngẫu nhiên 20% DATA/ACK dùng seed `20261112/20261113`; thứ tự lập lịch của hệ điều hành vẫn có thể khác. In PASS/FAIL, exit lỗi khi có FAIL. Kết quả M2 hiện tại: **24 PASS, 0 FAIL**. Chạy một phiên SelfTest mỗi lần.

## Dữ liệu

```text
data/
├── config.properties    # Admin, gioi han upload, multicast.portBase=9000
├── users.csv            # Ho so, salt/hash mat khau tai khoan
├── rooms.csv            # Phong, ma, salt/hash mat khau phong
└── rooms/<roomId>.log    # Moi dong mot goi MSG da escape UTF-8
```

Chỉ chạy một Server cho mỗi thư mục data; luôn chạy từ thư mục project. Phòng/người online và token chỉ trong RAM, không khôi phục sau restart. Server nạp phòng và lịch sử trước khi nhận Client. CSV/config ghi file tạm rồi thay thế; log tin được ghi tuần tự và force xuống đĩa. Nếu dữ liệu không hợp lệ, Server báo lỗi thay vì tự ghi đè bỏ dữ liệu.

Đổi admin ở M2: dừng Server, thêm `admin.password=MatKhauMoiDu6KyTu` vào config rồi bật lại. Server chuyển giá trị thành salt/hash và xóa trường mật khẩu thô sau khi đọc. Chức năng đổi mật khẩu qua Dashboard ở M5.

Phòng bị xóa không còn trong rooms.csv và không vào lại được; log cũ được giữ trên đĩa để tránh mất dữ liệu. Khi cấp lại nhóm/cổng của phòng đã xóa, phòng mới có roomId khác, nên Client bỏ gói cũ. Không commit data/reports/target/.selftest.

## Kịch bản thử tay M2 (3 Client)

1. Bật Server, mở Client 1/2/3; đăng nhập hai tài khoản thường và admin. Kiểm tra thông báo vào/ra có dấu, tiêu đề không có M1 và chat không hiện IP multicast.
2. A tạo phòng công khai (giới hạn 3), B bấm danh sách để vào. A tạo thêm phòng riêng có mật khẩu, gửi mã cho B; B vào qua mã. C thấy thay đổi phòng công khai ngay.
3. Chuyển giữa các tab, gửi tin ở từng phòng; chỉ người đã vào đúng phòng nhận. Để B đang xem Sảnh chung, A gửi ở phòng khác: badge tăng, mở tab thì badge về 0. Người vào sau nhận tối đa 50 tin cũ.
4. A chuột phải phòng: Sửa, Đóng, Mở lại. Thử gửi/vào khi đóng; thử quản lý bằng tài khoản không có quyền. Xóa phòng khi B còn ở trong, tab của B phải đóng và có thông báo. Thử xóa Sảnh chung bằng admin: không được.
5. Dừng/bật Server, đăng nhập lại, vào bằng mã cũ: phòng và lịch sử còn; phòng đã xóa không trở lại. Chạy SelfTest để xem các ca mất gói/RESYNC.

## Giới hạn đã biết

- **UDP không đảm bảo**. Điều khiển gửi lần đầu + tối đa 3 lần gửi lại, 500 ms/lần; hết lượt báo lỗi. ACK xác nhận nhận vào hàng xử lý, RESPONSE mới là kết quả nghiệp vụ. Multicast không có ACK; RESYNC cần Server còn truy cập được và lịch sử đã lưu.
- Multicast cần chung mạng LAN hỗ trợ multicast và **cho Java qua Windows Firewall**: UDP 5000 và các cổng phòng 9001–9254 mặc định. Wi-Fi cô lập thiết bị/VPN có thể ngăn nhận multicast.
- **Token/mật khẩu đi qua UDP không mã hóa**; multicast chưa xác thực nguồn phát. Salt/hash chỉ bảo vệ dữ liệu trên đĩa. Dùng trong mạng tin cậy để demo.
- Tối đa 254 phòng hoạt động (gồm Sảnh chung), mỗi phòng tối đa 1000 người. Tên tối đa 80 byte UTF-8, mô tả 240 byte, tin 600 byte; nhiều escape có thể chạm giới hạn gói 1399 byte sớm hơn.
- Vào phòng có bước đặt chỗ 10 giây để chờ mở socket. Mất mạng có thể cần chờ đặt chỗ/phiên hết hạn trước khi thử lại. Đổi cấu hình cổng multicast cần khởi động lại tất cả Client.
- Lịch sử hiện được nạp trong RAM để dễ đọc/thuyết trình; chưa tối ưu cho hàng triệu tin. Log phòng đã xóa được giữ để bảo toàn dữ liệu.
- F5/Run File cần thử trực tiếp trên IDE của bạn; cấu hình JSON và class đích đã được kiểm tra, Maven/SelfTest là kiểm chứng tự động.

## Thay đổi cấu trúc

M1 thay hai file gốc bằng ClientCore/KenhPhong/MangUtil và UI tách riêng; hai điểm chạy là ClientMain/ServerMain. M2 thêm `common/model/Phong`, `server/QuanLyPhong`, `client/DongBoTinNhan`, `ui/DanhSachPhongPanel`, `ui/KhungChatPanel` và `PhongSelfTest`. Gộp quyền chủ phòng vào QuanLyPhong; config/phòng/tài khoản/log cùng KhoLuuTru; dialog tạo/sửa nằm trong DanhSachPhongPanel để giữ mục tiêu khoảng 30–35 file Java cuối dự án.
