# UDPmultiChat — mốc M2.7

Java **21**, Swing, Maven. Hai điểm chạy rõ ràng: `com.mycompany.udpmultichat.ClientMain` và `com.mycompany.udpmultichat.server.ServerMain`. Giao diện dùng **FlatLaf 3.7.2**, giấy phép Apache 2.0 ([trang chính thức](https://www.formdev.com/flatlaf/)); đây là thư viện duy nhất của ứng dụng, được bổ sung theo yêu cầu M2.5.

## Đã hoàn thành

- M1: tài khoản SHA-256 + salt, token gắn endpoint UDP, chặn đăng nhập hai nơi, điều khiển có seq/ACK/gửi lại; heartbeat 5 giây, phiên quá 15 giây không PING bị coi là offline.
- M2: CRUD phòng, mã 6 ký tự, công khai/riêng tư, mật khẩu phòng, nhiều phòng, cập nhật realtime, lịch sử 50 tin, badge chưa đọc, RESYNC theo từng lô 50 tin.
- M2.5: đăng nhập/đăng ký riêng, xác nhận mật khẩu, hiện/ẩn mật khẩu, lỗi ngay dưới ô, kiểm tra Server, vòng chờ. Chat ba cột, danh sách phòng hai dòng, menu ⋮, thẻ tài khoản, nhóm thành viên online/offline, toast bốn mức, ô soạn nhiều dòng và bộ đếm byte. Mọi màu/font dùng chung `Theme.java`.
- Vá M2.5: lưu đúng loại phòng; giấu phòng riêng tư với người ngoài; lưu tư cách thành viên khi offline; chuyển chủ phòng trước khi rời; kiểm tra dữ liệu ở Client và Server; đóng cửa sổ gửi đăng xuất; tự nối lại và RESYNC sau mất kết nối.

- M2.6: danh tính phát trước sự kiện phòng, nhận diện nhãn **Bạn** bằng tên đăng nhập; sửa Client vào cuối thiếu nhãn. Kiểm tra lại tin của mình và quyền chủ/admin.
- M2.6: chọn phòng chỉ ở sidebar, **không còn tab giữa**; mỗi phòng giữ khung chat/vị trí cuộn riêng. Sidebar chia **PHÒNG CỦA TÔI / PHÒNG CÔNG KHAI**, lọc cả hai, có nút Tham gia và badge chỉ ở sidebar.
- M2.6: hộp thông tin phòng dùng HopThoai, thẻ tài khoản hai dòng, nút header nhẹ, toast giữa dưới khung chat, nút Gửi tròn cố định, form chỉ dành chỗ lỗi khi cần. Server hiện IP LAN/copy, bốn thẻ thống kê, log màu/font đơn cách và Xóa log. Giới hạn cửa sổ Client 960×600.
- M2.6: giới hạn nội dung **1000 byte UTF-8**, kiểm tra thêm header/escape để gói cuối không vượt 1399 byte. Bộ đếm UI hiển thị ký tự; xem phần giới hạn bên dưới.

- M2.7: sửa ô nhập bị ép còn 12px khi hiện lỗi; dùng hàng form chung giữ input 32px và tăng chiều cao theo lỗi. Toast gọn (một dòng 48px), giữa dọc, tối đa 360px, cách viền ô soạn 12px. Nút Tham gia có nền/viền, ẩn nhóm công khai rỗng, vai trò xám 12px, ngày dd/MM/yyyy/copy sát mã, nền soạn xám khi khóa, log Consolas 13 và header thẳng hàng.

**Sảnh chung** luôn công khai, mở, không mật khẩu; không được Sửa/Xóa/Rời/Đóng. Phòng tạo hoặc sửa dùng tên 3–30 ký tự, mô tả tối đa 240 byte UTF-8, giới hạn 2–500 thành viên. Sảnh hệ thống giữ giới hạn cũ 1000 nếu dữ liệu được tạo từ M2.

Mốc tiếp theo, mỗi mốc đều commit và dừng chờ xác nhận. Chỉ bắt đầu M3a sau khi người dùng duyệt M2.7:

| Mốc | Phần việc |
|---|---|
| M3a | Hồ sơ: tách tên đăng nhập cố định/tên hiển thị 2–30 ký tự, giới thiệu 100 ký tự; avatar PNG/JPG ≤2 MB, cắt giữa/96×96 PNG, mảnh ~600 byte/ACK/CRC32, phiên bản/cache có giới hạn, cập nhật realtime; đổi mật khẩu và buộc admin đổi admin123 |
| M3b | Sticker bằng hình vẽ/PNG (không emoji font), typing, tìm kiếm, thu hồi, trạng thái gửi/gửi lại, ngày/gộp tin, trả lời trích dẫn, sao chép/cuộn có điều kiện; tắt thông báo theo phòng |
| M4 | File UDP tối đa 10 MB cấu hình được, ACK/CRC32/tiến trình/hủy, chống path traversal, tải xuống, xem trước ảnh, chỉ thành viên phòng được tải |
| M5 | Dashboard đầy đủ bảng user (vai trò/online/phòng/ngày tạo/khóa-cấm, thêm/sửa/xóa/khóa/mở/đặt lại mật khẩu/đổi vai trò), bảng phòng (thành viên/tin/file, CRUD/đóng/mở), kick/mute có hạn/ban/gỡ cấm, broadcast, chống spam, thống kê, nhật ký thao tác, báo cáo CSV BOM + HTML (tổng user, user theo phòng, top chat) |
| M6 | Sáng/Tối, co giãn, phím tắt, xuất lịch sử một phòng ra .txt, hoàn thiện launch Server/Client 1/2/3 và tài liệu |
| M7 | Rà soát toàn bộ, sửa lỗi, chạy lại SelfTest/UiPreview, báo cáo đối chiếu yêu cầu |

Hồ sơ/đổi mật khẩu/Sáng-Tối chưa hoạt động nên đã ẩn khỏi menu tài khoản. Nút emoji/+ là vị trí chờ ở ô soạn, hiện vô hiệu. Quản trị thành viên thực hiện ở M5. Discovery tự tìm Server chưa triển khai; hiện nhập IP thủ công.

## Chạy bằng VS Code / NetBeans

1. Mở thư mục chứa `pom.xml`, dùng JDK 21 và Extension Pack for Java/Maven for Java. `.vscode/settings.json` trỏ JDK/Maven của máy hiện tại; máy khác cần sửa đường dẫn.
2. Đóng **toàn bộ Server/Client bản cũ**: snapshot thành viên/phòng M2.6 có thêm trường, cần chạy cùng phiên bản. Nhấn **Ctrl+Shift+D**, chọn **Server**, F5; bấm **Bắt đầu** trong cửa sổ giám sát.
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

Bốn file test ở `src/test/java`: SelfTest, PhongSelfTest, GiaoDienSelfTest, DanhTinhSelfTest. Dữ liệu riêng `.selftest/`, cấu hình Client test cũng tách riêng; không dùng data thật. UDP điều khiển dùng cổng tạm, multicast test dùng gốc 19000/21000/23000/25000. Chạy một phiên SelfTest mỗi lần.

Kết quả chạy lại ở M2.7 trên máy hiện tại: **28 PASS, 0 FAIL**, gồm:

- Tài khoản, timeout, mất ngẫu nhiên 20% DATA/ACK với seed `20261112/20261113` (thứ tự lập lịch hệ điều hành vẫn có thể khác).
- CRUD/quyền/mật khẩu/phòng đầy, phòng riêng không lộ, lưu đúng loại, chuyển quyền và nạp lại sau restart.
- Multicast thật qua card Wi-Fi: cách ly phòng, cả hai nhóm dùng cùng cổng; lịch sử đúng 50; RESYNC tin giữa/tin cuối và hơn 50 tin liên tiếp.
- Tin Việt quá 1000 byte bị chặn ở Client và Server; biên 1000 byte được nhận. Thử nội dung 1000 byte qua multicast thật và kiểm tra gói RESYNC bọc ReliableUdp dài **1280 byte** với ID/tên tối đa hiện hành; nhiều escape vượt gói bị chặn.
- Client vào sau cùng có đúng một nhãn Bạn ngay snapshot đầu; profile phát trước các sự kiện phòng, laToi đúng, chủ phòng sửa được còn người ngoài không được Sửa/Xóa; cờ membership chia nhóm đúng theo từng tài khoản.
- HELLO, thành viên offline, Server dừng/bật lại, Client tự nối lại và bù 75 tin đã thiếu, không trùng/sai thứ tự; cấu hình Client không có mật khẩu/token.

Tên GiaoDienSelfTest chỉ nhóm các lỗi phát hiện khi thử giao diện; test này gọi logic mạng/quản lý, không giả lập thao tác chuột Swing. Việc bấm F5, hover, kéo cửa sổ cần kiểm tra tay.

## UiPreview — xem giao diện không cần Server

```powershell
$mvnChat = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'
& $mvnChat -q compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview'
```

Xuất **24 PNG** vào [docs/screenshots](docs/screenshots/README.md): đăng nhập/đăng ký/lỗi inline, chat, mất kết nối, toast, menu tài khoản, hộp thoại phòng, Server và giới hạn tin, thông tin phòng, Client bbb vào cuối có Bạn, cửa sổ 960×600, ô soạn 5 dòng và số 111 và năm ảnh lỗi form bổ sung (20–24). Công cụ vẽ chính component Swing đang dùng lên BufferedImage với dữ liệu mẫu; không mở socket, không phải ảnh chụp thao tác desktop. Cần môi trường đồ họa để tạo JFrame. Đã xem PNG và chạy kiểm tra component thật: đổi profile vẽ lại đúng Bạn kể cả trùng tên hiển thị, chuyển phòng xóa badge không cần mạng, nút Tạo phòng không cắt chữ, chữ số không có viền RGB do LCD AA; mọi input vẫn đủ cao khi có lỗi, nội dung JTextArea là Font.PLAIN, toast không che ô soạn và nhóm công khai rỗng được ẩn. Đây không thay thế thử tương tác thực tế.

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

31 file Java ứng dụng + 4 file test ở mốc này, mỗi file dưới 400 dòng (dài nhất 368); các hàm được tách dưới khoảng 40 dòng. Comment đầu file ghi Vai trò/Ai gọi/Gọi ai; từng hàm có comment không dấu. Giữ bốn gói common/client/server/ui; UI không có Socket, logic không import Swing (ngoại lệ ServerMain). Quyền phòng gộp trong QuanLyPhong, lưu trữ gộp trong KhoLuuTru, DTO thành viên gộp trong Phong. Dialog tách khỏi DanhSachPhongPanel sang HopThoai để dùng chung và giữ file ngắn. Màu/font/nút/vẽ icon gom vào Theme, Avatar/BongTin còn trong ThanhPhan. Các phần M3–M5 sẽ ưu tiên mở rộng các lớp theo nhiệm vụ hiện có.

Xem [docs/GIAI_THICH.md](docs/GIAI_THICH.md) để tra bảng chức năng → file → hàm và đối chiếu UDP/Multicast chương 6.

Trong tài khoản admin, phòng riêng chưa tham gia vẫn hiển thị để quản lý ở nhóm PHÒNG CỦA TÔI; đây là quyền giám sát, không tự cấp membership hay quyền chat. Cờ membership do Server gửi, không suy từ tên hay việc phòng xuất hiện trong danh sách.

Đốm góc thẻ đăng nhập trong ảnh cũ chưa tái hiện riêng được ở lần thử này. Đã sửa điểm vẽ có thể để lại pixel cũ: card bo góc trước đây khai báo opaque nhưng không tô kín các góc; nay transparent, nền cha vẽ đầy đủ. PNG mới không có đốm. Chữ số viền màu đã xác nhận là LCD subpixel AA trong PNG, đổi grayscale AA tập trung ở Theme và kiểm tra pixel.

M2.7 chỉ thay bố cục/thành phần giao diện; không đổi định dạng gói hay dữ liệu M2.6. Hộp Đổi mật khẩu chưa tồn tại ở mốc này: các JPasswordField hiện có đã được kiểm tra, còn ba ô của hộp Đổi mật khẩu thật sẽ được kiểm tra và chụp ở M3a, cùng Hồ sơ và hộp bắt buộc đổi admin.
