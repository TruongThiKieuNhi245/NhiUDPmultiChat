# Giải thích đến M4 — Socket UDP và Multicast

## Luồng chạy và đối chiếu chương 6

```text
ClientMain → Swing → ClientCore → ReliableUdp → UDP unicast :5000 → ServerCore
                                                                      ↓
                                                        kiểm tra quyền, lưu tin
                                                                      ↓
                                      MulticastSocket.send → nhóm/cổng của phòng
                                                                      ↓
              Swing EDT ← Listener ← DongBoTinNhan ← KenhPhong: receive + lọc roomId
```

UDP ở chương 6 dùng **IP + port, DatagramSocket, DatagramPacket, send/receive**. Trong project, `common/ReliableUdp` sở hữu DatagramSocket và tạo DatagramPacket; ClientCore/ServerCore dùng lớp này cho đăng ký, đăng nhập, điều khiển phòng, PING, lịch sử và RESYNC. `guiRaw` gửi; `vongNhan` nhận. Seq/ACK/loại trùng/gửi lại là cơ chế tự viết ở tầng ứng dụng, không chuyển sang TCP.

Multicast dùng **địa chỉ nhóm lớp D, MulticastSocket, joinGroup/send/receive/leaveGroup/close**. Server gửi một datagram vào nhóm của phòng; nhiều Client trong nhóm nhận cùng tin. Không có TCP, ServerSocket, WebSocket. `MangUtil.dungIPv4` đặt preferIPv4Stack trước mạng, `timGiaoDienMang` giữ cách chọn card IPv4 thật để tránh lỗi Windows.

## Chức năng → file → hàm M1

Tên file dưới đây tương đối với `src/main/java/com/mycompany/udpmultichat/`, trừ file test.

| Chức năng | File | Hàm |
|---|---|---|
| Điểm chạy Client / giám sát Server | ClientMain / server/ServerMain | main |
| DatagramSocket / DatagramPacket | common/ReliableUdp | vongNhan, guiRaw |
| Seq, ACK, gửi lại cách 500 ms, loại trùng | common/ReliableUdp | gui, thuGui, nhanTinCay |
| IPv4, chọn card thật, mở nhóm | common/MangUtil | dungIPv4, timGiaoDienMang, vaoNhom |
| Ghép/tách gói UTF-8 có escape | common/Goi | ghep, tach, noi |
| Salt + SHA-256 | common/BaoMat | ngauNhien, bam, dungMatKhau |
| Đăng ký/đăng nhập, chặn hai nơi | server/QuanLyNguoiDung | dangKy, dangNhap |
| Token + endpoint UDP | server/QuanLyNguoiDung | kiemTra |
| PING 5 giây, hết phiên sau 15 giây | client/ClientCore / server/ServerCore | batHeartbeat, ping / quetPhien |
| Lưu tài khoản/config | server/KhoLuuTru | docNguoiDung, ghiNguoiDung, napCauHinh, ghiAnToan |
| Ranh giới mạng / Swing | client/Listener / ui/ChatMulticastUI | on... / chay, trenEdt |

## Chức năng → file → hàm M2

| Chức năng | File | Hàm |
|---|---|---|
| DTO phòng không lộ hash | common/model/Phong | cacTruong, tuTruong |
| Tạo phòng + nhóm/cổng riêng + mã 6 ký tự | server/QuanLyPhong | tao, taoMa |
| Sửa, mật khẩu/loại/giới hạn | server/QuanLyPhong | sua |
| Xóa, đóng/mở | server/QuanLyPhong | xoa, dongMo |
| Quyền chủ phòng, admin | server/QuanLyPhong | duocQuanLy, kiemTraQuyen |
| Bảo vệ Sảnh chung | server/QuanLyPhong | constructor, sua, xoa, dongMo, roiChuDong |
| Vào bằng id công khai hoặc mã riêng tư | server/QuanLyPhong | datCho |
| Đặt chỗ 10 giây, kiểm tra capacity | server/QuanLyPhong | datCho, xacNhanVao, donDatCho, soChoDaDung |
| Nhiều kênh trên một tài khoản | client/ClientCore | vaoPhong, moKenh, roiPhong |
| Gửi tin đúng phòng, lưu trước khi phát | server/ServerCore / server/QuanLyPhong | guiTin, phatMulticast / themTin, mocTin |
| Danh sách realtime, giấu phòng riêng | server/ServerCore / server/QuanLyPhong | capNhatPhong, guiDanhSachPhong / danhSachCho |
| Thành viên/role theo phòng | server/ServerCore / client/ClientCore | capNhatThanhVien / nhanThanhVien |
| Mỗi phòng một MulticastSocket | client/KenhPhong | constructor, vongNhan, close |
| Chặn nhận lẫn nhóm trên Windows | client/KenhPhong | vongNhan: so sánh roomId trước xử lý MSG/SYSTEM |
| Lịch sử 50 tin | server/QuanLyPhong / server/ServerCore | layTin / guiLichSu (HISTORY) |
| Bù theo lô 50 khi thiếu msgId | client/ClientCore / server/ServerCore | hoiDongBo, dongBoPhong / guiLichSu (RESYNC) |
| Phát hiện mất tin cuối bằng HEAD | server/ServerCore / client/DongBoTinNhan | guiMocTin / nhanMoc |
| Sắp thứ tự, loại trùng giữa multicast và tin bù | client/DongBoTinNhan | nhan, nhanMuc, nhanKetThuc, phatLienTiep |
| CSV phòng, log UTF-8, nạp lại | server/KhoLuuTru | docPhong, ghiPhong, docTin, ghiTin |
| Nhiều khung ở nền, đóng khi xóa phòng | ui/ChatMulticastUI | onVaoPhong, onRoiPhong, chonPhong |
| Badge, không tính lịch sử | ui/ChatMulticastUI | onTinNhan, chonPhong, capNhatPhongDangXem |
| Kiểm thử phòng/cách ly/RESYNC | src/test/.../PhongSelfTest | chayTatCa, testCungCong |

## Chức năng → file → hàm M2.5

| Chức năng | File | Hàm |
|---|---|---|
| FlatLaf + design tokens, không rải màu/font | ui/Theme | caiDat, nut, nhan, le, boTron, icon |
| Cài LAF trước tạo Swing | ClientMain / server/ServerMain | main gọi Theme.caiDat |
| Card đăng nhập/chat, ba cột, bỏ banner | ui/ChatMulticastUI | constructor, taiKhoan, tieuDePhong, chonPhong |
| Tab đăng ký, nhập lại, lỗi inline, spinner | ui/DangNhapPanel | constructor, gui, loiServer, ban |
| Hiện/ẩn mật khẩu dùng chung | ui/HopThoai | oMatKhau |
| Kiểm tra Server trước đăng nhập | ui/ChatMulticastUI / client/ClientCore / server/ServerCore | thuServer / kiemTraServer / xuLyGoi với HELLO |
| Nhớ tên và Server, không lưu bí mật | client/CauHinhClient / client/ClientCore | doc, ghi, tepCauHinh / dangNhap |
| Toast tối đa ba, bốn mức, hover dừng, nút đóng | ui/components/ThongBao | hien, sapXep, xoa, Toast.ganHover, close |
| Dải mất mạng/nối lại, khóa ô soạn | ui/ChatMulticastUI | onKetNoi, capNhat |
| Tự đăng nhập lại, giữ mốc tin, RESYNC | client/ClientCore | matKetNoi, khoiPhuc, dongBoPhong |
| Xác thực lại cùng endpoint, giữ chặn hai nơi | server/QuanLyNguoiDung / server/ServerCore | ketNoiLai / xuLyGoi với RECONNECT |
| Ô nhiều dòng, Enter gửi/Shift+Enter xuống dòng, byte counter | ui/SoanTinPanel | constructor, capNhat, vien, choPhep |
| Chặn >1000 byte UTF-8 và kiểm tra header/escape ở cả hai phía | client/ClientCore / server/QuanLyPhong | guiTin / themTin; Goi.kiemTraTin, Protocol.TIN_TOI_DA |
| Lọc phòng, hai dòng, tin cuối/thời gian, ⋮/chuột phải | ui/DanhSachPhongPanel | constructor, capNhat, tinCuoi, menu |
| Sao chép mã, ẩn/hiện thành viên | ui/ChatMulticastUI / ui/DanhSachPhongPanel | constructor / saoChep |
| Thẻ tài khoản, menu duy nhất có Đăng xuất | ui/ChatMulticastUI | menuAvatar, veDangNhap |
| Thành viên theo nhóm quyền, online/offline, thẻ Bạn | ui/ThanhVienPanel | capNhat |
| DTO thành viên có id/role/online | common/model/Phong | record ThanhVien |
| Lưu thành viên riêng với phiên online | server/QuanLyPhong / server/KhoLuuTru | ghiThanhVien, laThanhVien, soThanhVienBenVung / docThanhVien, ghiThanhVien |
| Không cho chủ phòng tự rời, chuyển quyền | server/QuanLyPhong / ui/ChatMulticastUI | roiChuDong, chuyenChu / chuyenChu |
| Hộp tạo/sửa/vào, inline validation, Enter/Esc | ui/HopThoai | constructor, hien, kiemTra, loiServer, duLieu |
| Sửa mật khẩu Giữ/Đổi/Xóa | ui/HopThoai / server/QuanLyPhong | duLieu / sua |
| Đóng cửa sổ gửi đăng xuất rồi đóng socket | ui/ChatMulticastUI / client/ClientCore | dongCuaSo / dangXuat, close |
| Log sửa/đóng/mở/xóa/chuyển chủ/đăng xuất/timeout | server/ServerCore | xuLyPhong, xoaPhong, roiTatCa, quetPhien |
| Chỉ tự cuộn khi đang ở đáy | ui/KhungChatPanel | themTin |
| Xuất ảnh component thật, dữ liệu mẫu không mạng | ui/UiPreview | main, dungVaXuat, xuat, boCuc |
| Test các lỗi M2.5 và nối lại/RESYNC | src/test/.../GiaoDienSelfTest | chayTatCa |

Các tên hàm M2 về UI đã được cập nhật sau khi tách dialog sang HopThoai; không còn dialog mạng trong DanhSachPhongPanel.

## M2.6: nguyên nhân thật của lỗi Bạn và bằng chứng

Ở M2.5, ClientCore đã gán `toi` trước khi mở kênh, nhưng **UI chỉ gán bản sao `toi` khi Future đăng nhập hoàn tất**. Future đó chờ READY và HISTORY; Server đã gửi ONLINE trong READY. Khi vẽ snapshot đầu, UI còn `toi == null`, nên không gắn Bạn. Client vào sau cùng không còn lần cập nhật thành viên nào tiếp theo để tự sửa hình vẽ; Client vào trước vô tình được vẽ lại khi người khác vào.

`DanhTinhSelfTest` đã chạy trên bản trước vá với admin → aaa → bbb và thất bại bằng thông báo **“TAI HIEN: danh sach thanh vien den truoc khi UI biet minh la ai”**. Sau vá, cùng ca này PASS: danh tính có trước snapshot đầu, Client bbb có đúng một dòng Bạn ngay lúc nhận; tin 111/1000 byte và quyền Sửa/Xóa đúng.

Bản vá:

1. `ClientCore.dangNhap`: gán token/profile, phát `Listener.onNguoiDung(toi)` **trước** heartbeat, lỗi ghi cấu hình, onVaoPhong/READY/HISTORY. Khi RECONNECT nhận profile mới cũng phát sự kiện này trước khi khôi phục phòng.
2. ONLINE gửi thêm **tên đăng nhập** do Server lấy từ hồ sơ đã xác thực. `Phong.ThanhVien.laToi` so tên đăng nhập, không so tên hiển thị; không phân biệt hoa/thường theo quy tắc tài khoản.
3. `ChatMulticastUI.onNguoiDung → ganNguoiDung → capNhatPhongDangXem`: gán profile trên EDT và vẽ lại member/sidebar/quyền ngay. Không chờ snapshot tiếp theo.
4. Bong bóng `laToi` trước đó đã so `tin.userId` với ID bất biến của tài khoản đã xác thực trong Core, không dùng tên hiển thị. Quyền phòng cũng so ownerId/vai trò Server. Hai chỗ này không có lỗi gán profile muộn của UI; giữ ID ổn định để đổi tên vẫn nhận tin/quyền đúng. Test xác nhận tin của bbb nằm bên phải ở bbb, bên trái ở aaa và người ngoài không được Sửa/Xóa.

UiPreview dùng component thật để thử đổi profile sau khi danh sách đã có, rồi thử hai tài khoản trùng tên hiển thị: chỉ một Bạn và nằm đúng dòng có account bbb. [Ảnh Client vào cuối](screenshots/16-client-vao-cuoi.png).

## Chức năng → file → hàm M2.6

| Chức năng | File | Hàm |
|---|---|---|
| Danh tính trước mọi sự kiện phòng | client/ClientCore, client/Listener | dangNhap, khoiPhuc / onNguoiDung |
| Nhận Bạn bằng tài khoản, không tên hiển thị | common/model/Phong, server/ServerCore, ui/ThanhVienPanel | ThanhVien.laToi / capNhatThanhVien / hang |
| Vẽ lại khi profile thay đổi | ui/ChatMulticastUI | onNguoiDung, ganNguoiDung, capNhatPhongDangXem |
| Cờ tham gia riêng từng user, owner và tổng thành viên cho thông tin phòng | ServerCore, ClientCore, common/model/Phong | guiDanhSachPhong / nhanDanhSach / voiThongTin |
| Không tab giữa, giữ card và vị trí cuộn riêng | ui/ChatMulticastUI, ui/KhungChatPanel | cacPhong, noiDungPhong, chonPhong, onVaoPhong / cuon |
| Chọn phòng đã mở không phát sinh mạng | ui/ChatMulticastUI | vaoPhong: kiểm tra khung trước yêu cầu ClientCore |
| Hai nhóm sidebar, lọc cả hai, Tham gia, hover ⋮ | ui/DanhSachPhongPanel | loc, cuaToi, themNhom, hang, nutPhong, ganHover |
| Badge chỉ sidebar, xóa ngay khi mở | ui/ChatMulticastUI, ui/DanhSachPhongPanel | chonPhong, onTinNhan / chon, tinCuoi |
| Ẩn mục chưa có trong menu tài khoản | ui/ChatMulticastUI | menuAvatar |
| Thẻ tài khoản hai dòng, mũi tên, lề | ui/ChatMulticastUI | theTaiKhoan, ganNguoiDung |
| Thông tin phòng đủ tám trường, copy, Đóng | ui/HopThoai | thongTin, constructor chiDoc, thongTinDong |
| Header nhẹ, Thành viên (N), tối thiểu 960×600 | ui/ChatMulticastUI, ui/Theme | tieuDePhong / nutNhe |
| Nhóm thành viên trái/xám/in hoa | ui/ThanhVienPanel | capNhat, hang |
| Bỏ chấm online trên avatar tin nhắn | ui/ThanhPhan, ui/KhungChatPanel | Avatar: online null / themTin |
| Toast giữa dưới vùng tin, không che toolbar/ô soạn | ui/components/ThongBao | datVung, sapXep; close gỡ listener/timer |
| Gửi tròn cố định, icon nhẹ và căn đáy, bộ đếm ký tự | ui/SoanTinPanel, ui/Theme | constructor, nutDinhKem, capNhat / nutGui |
| 1000 byte + kiểm tra envelope/escape cả hai phía | common/Protocol, common/Goi, ClientCore, QuanLyPhong | TIN_TOI_DA, LOI_TIN_DAI / kiemTraTin / guiTin / themTin |
| Không dành chỗ lỗi trống, căn mũi tên, nhãn mật khẩu mới | ui/DangNhapPanel, ui/HopThoai, ui/Theme | boCuc, loiServer / them, loi / datLoi |
| Card bo góc không báo opaque sai | ui/Theme | the: setOpaque(false) |
| Khử viền màu LCD ở chữ số, áp dụng toàn bộ Swing | ui/Theme | caiDat: KEY_TEXT_ANTIALIASING = VALUE_TEXT_ANTIALIAS_ON |
| IP LAN cùng card multicast | common/MangUtil, server/ServerMain | diaChiLan / napDiaChi, ganDiaChi |
| Bốn thẻ, log trắng/đơn cách/màu, Xóa log, icon Server | server/ServerMain, server/ServerCore | theThongKe, themLog, thanhCongCu / baoThongKe |
| Kiểm thử danh tính, laToi, membership, quyền, MTU | src/test/.../DanhTinhSelfTest | kiemTra, kiemTraGoi |
| Kiểm tra UI thật và 19 PNG | ui/UiPreview | xuatVaoCuoi, banDungTaiKhoan, kiemTraBadge, kiemTraNutTao, kiemTraMauChu |

Các trường bổ sung của snapshot chỉ dùng lúc chạy, **không đổi schema rooms.csv/users.csv/log cũ**. ONLINE hiện có 10 trường; ROOMS có thêm membership/nhãn chủ/tổng thành viên sau 13 trường phòng. Phải đóng và chạy lại toàn bộ Server/Client cùng bản M2.6. Admin thấy phòng riêng chưa tham gia ở nhóm PHÒNG CỦA TÔI để giám sát, nhưng cờ membership vẫn false và Server vẫn kiểm tra quyền vào/chat.

1000 byte nội dung Việt (333 ký tự ắ + a), tên/ID hiện hành, msgId/time dự phòng tối đa và envelope RESYNC/R dài **1280 byte** trong SelfTest, nhỏ hơn 1399. Nếu nhiều dấu `|`, dấu gạch chéo hay xuống dòng làm escape tăng gói, Client và Server từ chối trước khi lưu/phát với câu rút gọn/tách tin. UI chỉ hiện số ký tự, nội bộ vẫn đo byte và toàn gói.

Đã đo ảnh chữ 111: trước vá có pixel lệch tỷ lệ RGB do LCD subpixel; sau grayscale AA không còn. Cách đặt AA qua UIManager được đối chiếu [mã nguồn JComponent của OpenJDK 21](https://github.com/openjdk/jdk21u/blob/master/src/java.desktop/share/classes/javax/swing/JComponent.java). Đốm góc ảnh đăng nhập cũ chưa tái hiện riêng; đã sửa việc card khai báo opaque trong khi chỉ tô phần bo góc, tránh pixel nền cũ khi repaint. PNG mới không có đốm; vẫn cần người dùng kiểm tra trên màn hình/DPI thực tế.

## M2.7: sửa bố cục form và chi tiết giao diện

Nguyên nhân ảnh 03: BoxLayout giữ chiều cao hàng được tính lúc chưa có lỗi; khi JLabel lỗi hiện, phần CENTER của BorderLayout bị ép. Assertion mới của UiPreview chạy trên bản trước sửa báo **“O nhap bi nen: cao=12, text=nhikieu”**. `Theme.hangNhap` nay đặt vùng control cao 32px, minimum/maximum của hàng tính theo preferred hiện tại; DangNhapPanel bỏ preferred-size cache của card trước khi đo lại. HopThoai tự tính chiều cao/pack theo nội dung khi có lỗi. Không dành chỗ trắng cho lỗi đang ẩn.

| Chức năng | File | Hàm |
|---|---|---|
| Hàng input dùng chung, giữ 32px khi lỗi hiện | ui/Theme | hangNhap, O_NHAP_CAO |
| Bỏ chiều cao cache cũ của login/signup | ui/DangNhapPanel | them, boCuc, loiServer |
| Hộp tạo/sửa/vào tự cao theo lỗi | ui/HopThoai | them, loi, boCucForm, coMatKhau |
| Toast một dòng 48px, nhiều dòng tự cao, giữa dọc | ui/components/ThongBao | Toast constructor, chieuCao, sapXep |
| Toast tối đa 360px, trên viền ô soạn 12px | ui/Theme, ui/components/ThongBao | TOAST_CAO/TOAST_RONG / sapXep |
| Đủ thời gian đọc, hover tạm dừng như trước | ui/Theme, ui/components/ThongBao | TOAST_MS/TOAST_LOI_MS / Toast constructor, ganHover |
| Tham gia là nút nền nhạt/viền, giữa dọc; ẩn nhóm rỗng | ui/DanhSachPhongPanel | nutPhong, themNhom |
| Vai trò xám 12px, không đậm theo nút cha | ui/ChatMulticastUI, ui/Theme | theTaiKhoan, ganNguoiDung (JLabel riêng từ M2.8) |
| Ngày dd/MM/yyyy, copy sát mã | ui/HopThoai | dinhDangNgay, thongTinMa |
| Cả vùng control soạn cùng xám khi khóa | ui/SoanTinPanel, ui/Theme | capNhat / NEN_KHOA |
| Log Consolas 13, fallback Monospaced | ui/Theme, server/ServerMain | tenFontLog, DON_CACH / constructor |
| Header tên phòng và nút cùng một hàng | ui/ChatMulticastUI | tieuDePhong |
| Chứng minh form không nén, font PLAIN, toast không che input | ui/UiPreview | xuatLoiForm, xuatLoiPhong, kiemTraONhap, kiemTraFontNoiDung, kiemTraToast |

Thời gian toast ít nhất 4,5 giây; lỗi ít nhất 6,5 giây; nội dung dài tăng theo số ký tự, tối đa 12 giây; hover dừng đồng hồ. Vị trí neo tính thêm lề trên 16px của SoanTinPanel để mép dưới toast nằm trước viền control 12px, không che ô nhập.

UiPreview xuất **24 PNG**, bổ sung ảnh 20–24 cho các lỗi ở login/signup/tạo/sửa/vào phòng. Lỗi nhập lại mật khẩu được tạo bằng cách điền hai giá trị khác nhau rồi gọi nút Đăng ký thật; lỗi Server/form được đưa qua đường hiển thị lỗi đang dùng. Mỗi ảnh kiểm tra chiều cao JTextField/JPasswordField, font JTextArea là PLAIN; giữ kiểm tra Bạn/badge/MTU cũ. Nhóm công khai rỗng được assertion là không có tiêu đề. Chữ 111 và ô soạn đã dùng Theme.FONT (Font.PLAIN); không đổi sang font đậm. Cỡ/font log được gom trong Theme.

**Phạm vi Đổi mật khẩu:** hộp này chưa được triển khai ở M2.7. Các ô mật khẩu hiện có dùng chung HopThoai.oMatKhau/Theme.hangNhap đã được kiểm tra; hộp Đổi mật khẩu thật, lỗi ba trường và hộp bắt buộc đổi admin sẽ được kiểm tra tại M3a, không dùng ảnh mẫu giả chức năng ở mốc này.

Đã chạy lại SelfTest hồi quy **28 PASS, 0 FAIL**, Maven compile/test-compile thành công. UI test nằm trong UiPreview, tách khỏi SelfTest mạng không Swing. M2.7 không đổi giao thức UDP/multicast hay schema dữ liệu. Thử click/hover/F5 trên IDE và DPI khác vẫn cần kiểm tra tay; nhiều máy LAN chưa thử.

## Multicast nằm ở đâu, theo từng phòng?

`QuanLyPhong.tao` cấp **239.255.42.X:9000+X**, X từ 1 đến 254; Sảnh dùng X=1. `ServerCore.phatMulticast(roomId, duLieu)` lấy nhóm/cổng của đúng phòng và gọi `MulticastSocket.send(DatagramPacket)`. Socket phát chọn card bằng `MangUtil.timGiaoDienMang`, TTL=1.

`ClientCore.cacKenh` lưu mỗi roomId một KenhPhong. Constructor KenhPhong gọi `MangUtil.vaoNhom` tạo MulticastSocket, reuseAddress/bind, chọn card, joinGroup. `KenhPhong.vongNhan` receive, giải mã UTF-8, **lọc roomId rồi mới xử lý**. Windows có thể giao gói nhóm khác cho socket cùng cổng, vì vậy lọc này vẫn bắt buộc dù các phòng có cổng riêng.

`KenhPhong.close` leaveGroup/close khi rời/xóa phòng hoặc đóng phiên. **M2.5 thay đổi lúc mất mạng tạm thời:** giữ socket và bộ đếm msgId của các khung phòng để có thể nối lại, tự RESYNC; rời/xóa/đăng xuất mới đóng tài nguyên. Khi Server bật lại, Client dùng RECONNECT → PREPARE → READY cho từng phòng → RESYNC → PING. Nếu phòng đã bị xóa thì đóng khung đó.

Tin trực tiếp đi qua multicast; lịch sử/tin bù đi qua UDP unicast có ACK. Cả hai đi qua DongBoTinNhan để sắp thứ tự và tránh hiển thị hai lần. Không dùng multicast chung để truyền chéo tất cả phòng.

## Định dạng gói và quá trình vào phòng

- Datagram UTF-8 dưới 1400 byte. `Goi.ghep/tach` escape `|`, dấu gạch chéo ngược, CR/LF; `Goi.noi` nối trường phẳng, tránh escape nhiều lớp.
- Bọc ngoài `R|seq|noiDung` / `A|seq`. ACK chỉ xác nhận vào hàng xử lý; `RESPONSE|requestId|OK/ERR|...` mới xác nhận nghiệp vụ. Gửi lần đầu và tối đa 3 lần gửi lại, cách 500 ms.
- Chat `MSG|roomId|msgId|userId|ten|thoiGian|noiDung`; Server gán danh tính/thời gian/msgId sau kiểm tra token/quyền.
- PREPARE kiểm tra mã/password/capacity và đặt chỗ 10 giây → Client joinGroup → READY lưu membership → HISTORY 50 tin. Chọn phòng đã mở chỉ chuyển card, không tạo socket trùng.
- Snapshot ROOMS/ONLINE có phiên bản, tổng số/chỉ số; Client ghép đủ mới cập nhật, bỏ phiên cũ. ONLINE ở M2.6 có id/tên đăng nhập/tên hiển thị/vai trò/cờ online, gồm cả thành viên offline, không đồng nhất tư cách thành viên với phiên đang chạy.
- SYNC_ITEM/SYNC_END ghép lô tối đa 50, trộn buffer multicast, phát tăng dần msgId. Mỗi PING trả HEAD của các phòng để phát hiện mất cả tin cuối.

## Quyền, lưu trữ và luồng

ADMIN là quyền hệ thống; chủ phòng là `Phong.chuId`, không thay quyền hệ thống khi tạo phòng. Phòng riêng chỉ hiện với chủ/admin/thành viên đã lưu. Biết mã cho phép tìm phòng; muốn bảo vệ thêm cần đặt mật khẩu. Sảnh chung không cho Sửa/Xóa/Rời/Đóng. Chủ phòng chỉ Xóa hoặc Chuyển quyền cho thành viên khác rồi Rời.

Membership ở `data/members.properties` tồn tại sau logout/restart; logout/timeout chỉ loại phiên/kênh đang online. Rời chủ động mới xóa membership. Số thành viên tính cả offline để capacity ổn định; danh sách thành viên hiển thị riêng tổng số và online. Dữ liệu M2 cũ chưa lưu membership nên người không phải chủ cần vào lại một lần.

KhoLuuTru dùng users.csv, rooms.csv, members.properties và rooms/<roomId>.log. CSV/properties ghi tạm rồi thay thế, thử lại có giới hạn nếu Windows tạm khóa file. Log append tuần tự và force xuống đĩa; lỗi ghi khôi phục chiều dài cũ. Không dùng tên phòng làm đường dẫn. Trạng thái Server được bảo vệ cùng khóa ServerCore; không chạy hai Server ghi chung data.

UI không import java.net; logic common/client/server không import Swing, ngoại lệ ServerMain chỉ giám sát qua Listener. Thao tác mạng chạy nền, cập nhật Swing trên EDT. Theme tập trung màu/font/khoảng cách/bo góc; HopThoai dùng chung form, ThongBao dùng layered pane. Phần mạng không phụ thuộc FlatLaf.

## Kiểm chứng và giới hạn

M1/M2 đã được người dùng xác nhận. M2.6: **28 PASS, 0 FAIL** trên Java 21, Maven NetBeans; multicast thật chạy qua card Wi-Fi. Có seed mất 20% DATA/ACK, cách ly hai nhóm cùng cổng, lịch sử 50 và RESYNC nhiều lô. Bổ sung lưu đúng loại/không lộ riêng tư, phòng đầy/password/chuyển chủ, biên 1000 byte UTF-8, offline và Server dừng/bật lại để Client bù 75 tin thiếu. Test riêng `.selftest/`, không dùng dữ liệu thật. Xem lệnh và kịch bản 3 Client trong README.

UiPreview xuất 24 PNG bằng các component Swing thật, không socket; đã kiểm tra bố cục ảnh, không khẳng định đã thử chuột/phím toàn bộ. F5/Run File và tương tác ba cửa sổ cần người dùng kiểm tra; nhiều máy LAN, DPI cao chưa thử. 31 file ứng dụng + 4 file test, file dài nhất dưới 400 dòng; compiler bật Xlint:all/failOnWarning.

**UDP không đảm bảo**. **Multicast cần chung LAN và cho Java qua Windows Firewall**. **Token/mật khẩu đi qua UDP không mã hóa**, multicast chưa xác thực nguồn; salt/hash chỉ bảo vệ trên đĩa. Tự nối lại cần credentials/IP/cổng còn đúng và lịch sử Server còn. Lịch sử hiện nạp RAM, chưa tối ưu quy mô lớn. Discovery chưa triển khai. Các mốc tiếp theo của phần lịch sử này được cập nhật bên dưới.

## M2.8

Vai trò thẻ tài khoản dùng JLabel riêng: tên 14px đậm, vai trò 12px thường màu xám (không dùng kích thước HTML). Ô soạn chia vùng nhập toàn chiều rộng và hàng công cụ bên dưới; nền khóa vẽ bo tròn, không còn góc xám vuông. Bộ đếm gần ngưỡng hiển thị số ký tự / giới hạn ước tính từ tỷ lệ ký tự so với dung lượng nội dung hiện tại; giới hạn thật vẫn kiểm tra UTF-8 và gói UDP. Mô tả phòng giới hạn 80 ký tự ở cả Client và Server, thông báo bỏ thuật ngữ byte. Theme.vietHoaDau chuẩn hóa chữ đầu lỗi. Kiểm tra Font.PLAIN của JTextArea trong UiPreview vẫn được giữ; tên người gửi là nhãn riêng.

File/hàm: ChatMulticastUI.theTaiKhoan/ganNguoiDung; SoanTinPanel constructor/capNhat/paintComponent; Theme.vietHoaDau; HopThoai.kiemTra; QuanLyPhong.kiemTraNoiDung. Ảnh 05/18 là offline/nhiều dòng được xuất lại.


## M3a: chức năng → file → hàm

| Chức năng | File | Hàm |
|---|---|---|
| Tách tên đăng nhập bất biến và tên hiển thị/giới thiệu/hash avatar | common/model/NguoiDung | constructor, cacTruong, tuTruong, voiHoSo |
| Kiểm tra tên 2–30, giới thiệu ≤100, cấm ký tự điều khiển | server/QuanLyNguoiDung / ui/HoSoDialog | luuHoSo / kiemTra |
| Lưu hồ sơ bằng một lệnh, phát cho Client chung phòng | server/QuanLyHoSo / server/ServerCore | luu / xuLyHoSo |
| Nhận hồ sơ trước callback; định danh Bạn/quyền vẫn dùng tài khoản/id | client/ClientCore / ui/ChatMulticastUI | nhanHoSo / onHoSo, ganNguoiDung |
| Hồ sơ có avatar 96px, Đổi/Xóa, xem trước, Lưu/Hủy, trạng thái tải | ui/HoSoDialog | formHoSo, chonAnh, docAnh, gui, dong |
| PNG/JPG ≤2 MB, kiểm tra nội dung/diện tích trước giải nén, cắt giữa 96×96 PNG | common/PhanManh | chuanHoa, docAnh |
| Mảnh 600 byte, Base64, giới hạn index/bộ nhớ, CRC32 | common/PhanManh | manh, crc, BoGhep.nhan/ketQua |
| Gửi mảnh nền có ACK/gửi lại, dừng trước SAVE khi Hủy | client/HoSoClient / common/ReliableUdp | luu, guiAnh / gui, thuGui |
| Ảnh tạm theo token, tối đa 128 lượt × 64 KB, hết hạn 60 giây | server/QuanLyHoSo | xuLy, layTam, donDep |
| Server kiểm tra PNG 96×96 + CRC trước lưu chính thức | server/QuanLyHoSo | luu |
| Lưu data/avatars/username.png qua file tạm; rollback ảnh cũ nếu lưu hồ sơ lỗi | server/KhoLuuTru / server/QuanLyHoSo | tepAnh, ghiAnh / luu |
| Tải mảnh theo id + phiên bản, chống trộn ảnh cũ/mới | server/QuanLyHoSo / client/HoSoClient | doc / taiDuLieu |
| Lazy load và cache LRU tối đa 100 hồ sơ, 100 ảnh, 16 lượt tải/32 lượt hỏi | client/HoSoClient | gap, taiAnh, nhan, gioiHan, xoa |
| Avatar ảnh thật hoặc chữ cái, tên người gửi cập nhật tại chỗ | ui/Theme / ui/ThanhPhan / ui/KhungChatPanel | avatar / Avatar.paintComponent, BongTin.tenMoi / datHoSo, hoSoMoi |
| Avatar thành viên và thẻ tài khoản, không tải mạng trong paint | ui/ThanhVienPanel / ui/ChatMulticastUI | datAnh, hang / ganNguoiDung, onHoSo |
| Ba ô mật khẩu, Hiện/Ẩn, lỗi inline không nén input | ui/HoSoDialog / ui/Theme | formMatKhau, oMatKhau, kiemTra, baoLoi / hangNhap |
| Kiểm tra mật khẩu cũ, độ dài 6–128, salt/hash mới | server/QuanLyNguoiDung | doiMatKhau, thayTaiKhoan |
| Đồng bộ hash admin trong config; nạp CSV cũ 7 cột / mới 9 cột | server/KhoLuuTru | doiMatKhauAdmin, docNguoiDung, ghiNguoiDung |
| Mật khẩu mới trong RAM phục vụ reconnect, xóa char[] cũ | client/ClientCore | doiMatKhau, close |
| Buộc admin đổi mật khẩu mặc định ở cả Server lẫn UI | server/QuanLyNguoiDung / server/ServerCore / ui/HoSoDialog | congKhai, kiemTraBatBuoc / xuLyGoi / formMatKhau, hien |
| Chỉ mở Sảnh sau khi đổi mật khẩu bắt buộc | client/ClientCore / ui/ChatMulticastUI | dangNhap, vaoSanh / moMatKhau |
| Mất kết nối/nối lại lấy lại hồ sơ, không giữ cache cũ | client/ClientCore | khoiPhuc |
| Kiểm thử hồ sơ/ảnh/mật khẩu/admin, dữ liệu cũ, mất 20% gói | src/test/.../HoSoSelfTest | chayTatCa, kiemTraHoSo, kiemTraAvatar, kiemTraMatGoi, kiemTraMatKhau, kiemTraAdmin, kiemTraCsvCu |
| Ảnh hộp thật, lỗi ba ô và ảnh mẫu vẽ bằng code | ui/UiPreview | xuatHoSo, anhMau, kiemTraONhap, kiemTraFontNoiDung |

### Luồng UDP của hồ sơ/avatar/mật khẩu

LOGIN trả token + 8 trường hồ sơ công khai (không salt/hash mật khẩu). Client gán danh tính trước Listener. Admin dùng hash tương ứng admin123 nhận `buocDoiMatKhau=true`: Server chỉ cho PING/LOGOUT/PASSWORD_CHANGE, chưa cho PREPARE/READY/SEND. Sau đổi thành công mới vào Sảnh; một tài khoản chỉ có một phiên nên không có phiên khác cần hủy. Phiên hiện tại giữ đăng nhập, cập nhật mật khẩu mới chỉ trong RAM để reconnect.

Chọn ảnh chỉ xử lý tại máy Client. Bấm Lưu: AVATAR_BEGIN → AVATAR_PART từng mảnh 600 byte thô → PROFILE_SAVE (tên, giới thiệu, mã ảnh tạm/KEEP/DELETE). Mỗi request đi qua ReliableUdp, có seq/ACK/gửi lại và RESPONSE nghiệp vụ. Hủy trước PROFILE_SAVE không thay hồ sơ; nếu SAVE đã gửi, có thể đã lưu dù người dùng đóng hộp. Server ghép đủ + CRC và PNG 96×96 hợp lệ mới ghi, sau đó phát PROFILE_CHANGED tới các phiên chung phòng. Không đưa cả ảnh vào multicast hay vào một datagram lớn.

Client gặp user chưa biết gửi PROFILE_GET, có hash ảnh thì AVATAR_READ lần lượt từng mảnh; ghép CRC trùng mới ImageIO đọc PNG. Cache LRU theo id/hash, không dùng tên hiển thị làm khóa. Khi hash thay đổi, renderer chuyển sang bản mới sau khi tải; nếu chưa có thì hiện chữ cái. Ảnh tải nền, UI chỉ đọc RAM rồi repaint trên EDT. Callback ảnh tải xong lấy snapshot mới nhất để tránh đổi tên quay về giá trị cũ. Đăng xuất xóa cache và bỏ kết quả luồng tải thuộc phiên trước.

### Multicast ở M3a

Không thay đường chat: `ServerCore.phatMulticast` vẫn phát đúng nhóm/cổng phòng, `KenhPhong.vongNhan` vẫn lọc roomId. Hồ sơ/avatar/mật khẩu đi **UDP unicast có ACK**, vì cần kiểm tra quyền, truyền mảnh và trả kết quả riêng. Hồ sơ mới được Server gửi bằng UDP unicast tới những Client chung phòng; tin văn bản hiện tại vẫn multicast. Đây là hai nhiệm vụ khác nhau của Socket UDP và Multicast trong chương 6.

### Kiểm chứng, giới hạn và thay đổi file

Maven NetBeans/JDK 21 biên dịch với Xlint/failOnWarning. SelfTest đến M3a: **37 PASS, 0 FAIL**, gồm 9 mục M3a và 28 mục hồi quy. Ảnh nhiều màu truyền nhiều mảnh, so byte/CRC chính xác cả upload/download khi bỏ ngẫu nhiên 20% DATA/ACK với seed `20261113`; lịch trình luồng hệ điều hành có thể khác. Server được kiểm tra trực tiếp để không dựa vào validator Client: ảnh giả, PNG sai kích thước, quá giới hạn bộ đệm, index lớn, CRC sai đều bị từ chối. CSV cũ được viết theo schema 7 cột rồi kiểm tra nạp/nâng cấp thật.

UiPreview xuất **30 PNG**: 25/26 hồ sơ chưa/có avatar, 27/28 mật khẩu bình thường/có lỗi, 29 bắt buộc đổi, 30 thành viên và bong bóng có ảnh mẫu Java2D. Kiểm tra chiều cao input, font PLAIN vẫn chạy trên tất cả ảnh; không khẳng định đã thao tác chuột/phím qua 3 Client. Nhiều máy LAN, DPI khác và Run/Debug IDE cần thử tay.

Bốn file ứng dụng mới: PhanManh, HoSoClient, QuanLyHoSo, HoSoDialog; tổng **35 file ứng dụng + 5 file test**, giữ bốn gói, mỗi file dưới 400 dòng. Không đổi tên/xóa lớp cũ ở M3a; không thêm thư viện. NguoiDung bổ sung snapshot 8 trường, CSV giữ 7 cột cũ và nối 2 cột mới; đóng toàn bộ tiến trình cũ trước khi chạy cùng phiên bản.

**Giới hạn đã biết:** UDP không đảm bảo; hết 3 lần gửi lại vẫn có thể lỗi và cần thử Lưu lại. Multicast cần chung LAN, bật Java qua Windows Firewall. Token/mật khẩu đi qua UDP không mã hóa; CRC32 chỉ kiểm tra lỗi, không xác thực nguồn. Ảnh đầu vào tối đa 2 MB và 16 triệu pixel để giới hạn giải nén; ảnh chuẩn hóa tối đa 64 KB. Cache chỉ ở RAM, logout xóa. Ghi avatar và users.csv là hai file riêng: có rollback khi gặp lỗi I/O trong tiến trình, chưa phải giao dịch chống mất điện giữa hai bước. Phần mới hơn được cập nhật bên dưới.


## M3a.1 — sửa lỗi từ ảnh

Assertion đã tái hiện lỗi `Mô tả` bị gán khóa `ten`: hàm HopThoai.loiServer tìm chuỗi `mô tả` phân biệt hoa/thường. Đã chuẩn hóa khi phân loại lỗi và tách thông báo Server theo từng trường. FormKiemTra.chay nhập riêng tên 1 ký tự, mô tả 81 ký tự, giới hạn 1, mật khẩu 129 ký tự vào form thật để xác nhận chỉ đúng ô có lỗi; kiểm tra cả tạo/sửa/vào, cả validator và thông báo Server. Ảnh 22/23 dùng đúng lỗi tên và mô tả.

SoanTinPanel.capNhat chỉ cho thanh cuộn dọc hiện khi vượt 5 dòng kể cả dòng tự ngắt; UiPreview xác nhận ô một dòng không lộ thanh cuộn và thêm ảnh 31-soan-co-thanh-cuon.png. Nút Hiện mật khẩu dùng nút viền; lỗi cũ/mới/nhập lại phân biệt đúng, ẩn gợi ý khi ô mới có lỗi. Hồ sơ vô hiệu Xóa ảnh khi chưa có ảnh, bộ đếm giới thiệu n/100. Login thật trả cùng thông báo “Tên đăng nhập hoặc mật khẩu không đúng.”; thông báo tồn tại chỉ dùng khi đăng ký. Admin mặc định có khung cảnh báo vàng. Tên hiển thị ngoài BMP bị từ chối ở cả Client/Server với thông báo rõ để người dùng sửa, không âm thầm mất ký tự.

File/hàm: ui/HopThoai.loiServer/kiemTra; ui/FormKiemTra.chay/kiemTraPhong/kiemTraCuon; ui/SoanTinPanel.capNhat; ui/HoSoDialog.formHoSo/formMatKhau/kiemTra/oGioiThieu; server/QuanLyPhong.kiemTraNoiDung; server/QuanLyNguoiDung.dangNhap/luuHoSo; ui/UiPreview.xuatLoiForm. SelfTest hồi quy 37 PASS; UiPreview 31 PNG. Thao tác thật nhiều máy LAN và DPI khác vẫn cần thử tay.


## M3b rút gọn: chức năng → file → hàm

| Chức năng | File | Hàm |
|---|---|---|
| Mã sticker hợp lệ, hạn 10 phút/typing/8 giây | common/Protocol | stickerHopLe, MA_STICKER, THU_HOI_MS, TYPING_MS, CHO_TIN_MS |
| 36 PNG Noto Emoji 128px; popup 4 nhóm và hover tên | common/StickerDanhMuc; ui/StickerKho | tim; anh, icon, bangChon |
| Nút mặt cười mở picker, text gõ kích hoạt callback | ui/SoanTinPanel / ui/ChatMulticastUI | nutDinhKem, theoDoiNhap, datChucNang / guiSticker |
| Mã Client 128-bit, loại T/S/D, cờ thu hồi, nạp log cũ 7 trường | common/model/TinNhan | taoMa, constructor, dongGoi, tuTruong, daThuHoi |
| Server kiểm tra STK, room + user + mã để loại trùng | server/QuanLyTinNhan / server/QuanLyPhong | them, nap / themTin |
| Lưu trước multicast; retry phát lại cùng msgId | server/ServerCore | guiTin, phatMulticast |
| Hàng gửi WAIT/SENT/ERROR; RESPONSE và multicast cùng msgId mới SENT | client/TroChuyenClient | gui, guiCoMa, thuGui, nhanMulticast, hoanTat, thatBai |
| Timeout 8 giây, Gửi lại cùng mã, Xóa hàng lỗi cục bộ | client/TroChuyenClient / ui/HangTin | guiLai, xoaTinLoi / trangThaiGui |
| Hợp nhất tin local và Server, không tạo hàng trùng | ui/KhungChatPanel | trangThai, themTin, theoMa/theoId |
| Render sticker không nền, recall xám nghiêng, menu chuột phải | ui/HangTin | noiDung, menu, ganMenu |
| Quyền người gửi 10 phút, chủ/admin mọi lúc, phải ở phòng | server/QuanLyTinNhan / server/ServerCore | duocThuHoi, thuHoi / thuHoi |
| Xóa nội dung cũ khỏi file chính qua temp/atomic move | server/KhoLuuTru | ghiLaiTin, ghiAnToan |
| Sự kiện thu hồi tin cậy, revision count trong HEAD | server/ServerCore / server/QuanLyTinNhan | thuHoi, guiMocTin / phienBan, danhSachThuHoi |
| RESYNC cả thu hồi cũ khi msgId không tăng | client/TroChuyenClient / client/ClientCore | nhanRevision, dongBoThuHoi / dongBoPhong |
| Chặn tin cũ tới muộn làm sống lại nội dung, kể cả EDT/sidebar | client/TroChuyenClient / ui/KhungChatPanel / ui/ChatMulticastUI | loc, thuHoiMoi / locTin, thuHoi / onTinNhan, onThuHoi |
| Sidebar không giữ preview nội dung đã thu hồi | ui/DanhSachPhongPanel | thuHoi, nutPhong |
| UDP nhanh không ACK dùng cùng DatagramSocket | common/ReliableUdp | guiNhanh, nhanNhanh, vongNhan |
| Typing Client ≤1 lần/3 giây, không gửi text rỗng | client/TroChuyenClient / client/ClientCore | dangGo, choTyping / guiTyping |
| Typing Server kiểm tra token/phòng và rate, phát multicast | server/ServerCore / server/QuanLyTinNhan | typing / choTyping |
| Lọc roomId trước nhận TYPING multicast | client/KenhPhong / client/TroChuyenClient | vongNhan, suKien / nhanTyping |
| Nhiều người đang nhập, tự tắt sau 4 giây, riêng từng phòng | ui/ChatMulticastUI | onDangGo, capNhatDangGo, khungSoan |
| SelfTest M3b không Swing | src/test/.../TinNhanSelfTest | kiemTraQuyen, BoTest.sticker/thuHoi/trangThai/typing/napLai |
| PNG và assertions trạng thái/typing/menu/sticker lạ | ui/UiPreview / ui/FormKiemTra | xuatTroChuyen / kiemTraThuHoi |

### Luồng tin và chống trùng

Client tạo mã ngẫu nhiên 128-bit, biểu diễn Base64url 22 ký tự. Gửi `SEND|requestId|token|roomId|maClient|text` hoặc `STK|requestId|token|roomId|maClient|maSticker` qua ReliableUdp. Server xác thực thành viên/quyền/phòng mở và mã sticker. `QuanLyTinNhan.them` tra room + user + mã; có rồi trả lại cùng msgId và phát lại multicast, không ghi thêm. Khác nội dung mà dùng lại mã sẽ bị từ chối; tin đã thu hồi chỉ phát bản D không nội dung. Mã chống trùng được nạp từ log khi restart.

Tin multicast/log vẫn 7 trường. Với tin mới, phần nội dung có dấu `~maClient~T/S/D~noiDung`; phần nhị phân sticker không đi qua mạng. Client biết mã để hợp nhất với hàng local; msgId dùng sắp thứ tự/RESYNC. Giới hạn byte kiểm tra toàn gói có header/escape, không chỉ nội dung. Không đổi sang TCP.

Gửi thành công ở tầng ReliableUdp chưa đủ để báo Đã gửi. `TroChuyenClient` chờ RESPONSE đúng msgId và multicast thực tế của cùng mã; RESYNC có thể hiển thị tin nhưng không được giả làm multicast đã quay về. Hết khoảng 8 giây chuyển lỗi, giữ mã/nội dung để retry. Gửi lại có thể xác nhận một tin mà Server đã lưu từ lần trước. Nút Xóa chỉ bỏ hàng lỗi local, không thu hồi tin trên Server; lịch sử có thể hiện lại nếu tin đã tới Server. Tối đa 256 hàng chờ/lỗi, logout dọn.

### Thu hồi và đồng bộ

Server kiểm tra người gửi trong 10 phút hoặc chủ/admin; mọi trường hợp phải đang ở phòng. Tạo bản D với nội dung rỗng, ghi lại toàn bộ log qua file tạm rồi đổi tên, sau thành công mới thay RAM. Không giữ nội dung cũ trong log chính. Các phiên online nhận REVOKED qua UDP có ACK. Client ghi tombstone trước khi xử lý tin tới chậm; UI lọc lại trên EDT và cập nhật preview sidebar. Đương nhiên không thể xóa bản sao đã được người nhận lưu/chụp trước khi thu hồi.

HEAD mang thêm số lượng tin đã thu hồi (revision), không đổi msgId. Nếu hụt sự kiện thu hồi hoặc nối lại, Client xin RECALLS từng lô tối đa 50 ID, không mang nội dung gốc; HISTORY và RESYNC luôn lấy bản D hiện tại. Ngay RESYNC với mốc đã bằng Server vẫn đồng bộ tombstone. File cũ không có mã/loại vẫn nạp được; cần đóng toàn bộ Client/Server cũ trước khi chạy cùng phiên bản mới.

### Multicast và typing nằm ở đâu?

Tin văn bản và sticker sau kiểm tra ở Server đều qua `ServerCore.phatMulticast` → `KenhPhong.vongNhan`, nhóm/cổng riêng từng phòng và lọc roomId. TYPING đi Client → Server bằng UDP unicast nhanh cùng DatagramSocket, không envelope R/ACK. Server kiểm tra token + thành viên + phòng mở + limiter 3 giây, rồi dùng MulticastSocket phát TYPING vào đúng nhóm phòng. Không ghi lịch sử, cho phép mất gói. Client bỏ typing của chính mình; UI dùng đồng hồ đơn điệu để tắt sau 4 giây. Lệnh thu hồi, kết quả gửi, lịch sử và tombstone vẫn UDP tin cậy tự viết.

### Kiểm chứng và phần dời lại

Toàn bộ SelfTest: **43 PASS, 0 FAIL** = 37 hồi quy + 6 nhóm M3b. SelfTest mới dùng một Server/ba Client trên card mạng thật: mã sticker đúng/lạ; cách ly phòng; người thường không thu hồi người khác; chủ/admin được phép; biên đúng 10 phút/quá 1ms; log không còn nội dung bị thu hồi; HISTORY/RESYNC; thiếu multicast dù Server đã ACK gây lỗi sau 8 giây; retry không trùng cả RAM/log; restart còn tombstone/mã; TYPING không lưu và limiter Client/Server đúng 2999/3000ms. UiPreview **37 PNG**, ảnh 31–36 có thanh cuộn thật, picker, sticker/thu hồi/ba trạng thái/typing, menu, cập nhật không trùng và sticker không hỗ trợ; typing hết hạn được kiểm tra bằng đồng hồ truyền vào, không sleep EDT.

Không thêm thư viện. Thêm bốn file chức năng/renderer M3b (QuanLyTinNhan, TroChuyenClient, StickerKho, HangTin) và FormKiemTra từ M3a.1: **40 file ứng dụng + 6 file test**. Tách thêm để từng file dưới 400 dòng, hàm dưới khoảng 40 dòng, UI không Socket, logic không Swing. Không xóa chức năng cũ; dời logic lịch sử khỏi QuanLyPhong sang QuanLyTinNhan, API cũ gọi tiếp qua lớp này.

Chỉ bốn mục M3b đã làm. Thứ tự tiếp: **M4 → M5 → M6 → M3c → M7**. M3c gồm tìm kiếm, trả lời, gộp tin, phân cách ngày, tắt thông báo phòng, xuất lịch sử. Không tự thêm các mục đó vào M3b.

**Giới hạn đã biết:** UDP vẫn có thể hết lượt retry; multicast cần chung LAN và cho Java qua Windows Firewall; token/mật khẩu qua UDP không mã hóa, nguồn multicast chưa xác thực. TYPING có thể mất, không phải bằng chứng người dùng còn online. Thu hồi ghi lại log và tombstone/mã hiện giữ RAM theo lịch sử, chưa tối ưu dữ liệu rất lớn. Chưa kiểm tra tương tác IDE/ba cửa sổ đầy đủ, nhiều máy LAN hoặc DPI khác; PNG không thay thế kiểm tra tay.

Ảnh [37-kho-sticker-day-du.png](screenshots/37-kho-sticker-day-du.png) cho xem đầy đủ 36 PNG Noto Emoji, ảnh nguồn 128px, thu phóng bicubic.


## Sticker PNG nguồn mở

36 sticker Noto Emoji 2D (Google và cộng tác viên), ảnh Apache 2.0; xem [nguồn tài nguyên](NGUON_TAI_NGUYEN.md).
| Chức năng | File | Hàm |
|---|---|---|
| Danh mục hex, tên, 4 nhóm | common/StickerDanhMuc.java | tim, maCu |
| Kiểm tra mã ở Server | common/Protocol.java | stickerHopLe |
| Cache PNG, bicubic, tìm nhanh | ui/StickerKho.java | anh, icon, bangChon |
| Ghi nguồn trong ứng dụng | ui/ChatMulticastUI.java | menuAvatar |

Gói vẫn là `STK|<mã hex>` qua lệnh điều khiển; Server phát tin S qua multicast phòng.
Không truyền ảnh sticker qua mạng. Mã lịch sử cũ có ánh xạ để vẫn xem được; mã lạ hiện “Sticker không hỗ trợ”.


## M3b.1: Chỉnh chi tiết chat

| Chức năng | File | Hàm |
|---|---|---|
| Liên kết gửi lại/xóa, avatar căn đầu, menu có quyền | ui/HangTin.java | veLai, trangThaiGui, menu |
| Chỉ tin cuối của mình hiện Đã gửi | ui/KhungChatPanel.java | capNhatNhanGui |
| Xem trước sticker/thu hồi, tiền tố Bạn | ui/DanhSachPhongPanel.java | xemTruoc |
| Nút Xóa ảnh vô hiệu nếu chưa có ảnh | ui/HoSoDialog.java | capNhatAnh |

Popup sticker dùng tab gạch chân FlatLaf; hình Vỗ tay đã thay bằng PNG Noto Emoji.
Trạng thái Đang gửi và Lỗi luôn hiện; Đã gửi chỉ hiện khi đó là tin cuối của chính mình.


## M4: Gửi và nhận tệp bằng UDP

| Chức năng | File | Hàm |
|---|---|---|
| Header nhị phân, cửa sổ 32, ACK bitmap, gửi lại | common/TruyenTep.java | guiCuaSo, nhanAck, nhanManh |
| Ghi đúng vị trí, loại mảnh trùng, CRC32, timeout/hủy | common/TruyenTep.java | nhanManh, crc, huy, loi |
| Tên sạch, dung lượng, tránh ghi đè | common/TepTin.java | tenAnToan, kiemTra, tenChuaCo |
| Thumbnail tối đa 240 px, ảnh hỏng thành thẻ thường | common/AnhTep.java | tao, thuNho |
| Quyền thành viên, phòng đóng, tối đa hai lượt gửi | server/QuanLyTep.java | batDau, chuanBiTai, sanSang, hoanTat |
| INIT/DONE gửi lại không tạo tin trùng | server/QuanLyTep.java | timLai |
| Tệp `.bin`, metadata UTF-8, nạp/dọn orphan | server/KhoTep.java | duong, luu, nap, constructor |
| Thu hồi/xóa phòng xóa tệp, timeout/đăng xuất dọn tạm | server/QuanLyTep.java | xoaTin, xoaPhong, matPhien, quet |
| Số tệp và dung lượng theo phòng/người gửi | server/ServerCore.java | thongKeTep |
| Hàng đợi hai upload, tiến độ, tải CRC, hủy/thử lại | client/TepClient.java | gui, guiThat, taiThat, huy, thuLai |
| Cache thumbnail có giới hạn, tải nền | client/TepClient.java | napAnh, anh |
| Chọn tệp/ảnh, kéo thả, nơi lưu, mở thư mục | ui/TepGiaoDien.java | chon, datKeoTha, tai, moThuMuc |
| Thẻ tệp/ảnh và các trạng thái truyền | ui/TheTep.java | constructor, tienDo |
| File trong lịch sử/RESYNC/multicast | common/model/TinNhan.java; server/ServerCore.java | dongGoi, tuTruong; guiLichSu |
| Kiểm thử tích hợp và lỗi mạng | TepSelfTest.java; TepMangSelfTest.java (src/test/java) | chayTatCa; chay |

### UDP được dùng thế nào để truyền tệp?

1. Client gửi `FILE_INIT` qua **UDP unicast có ACK** (`ReliableUdp`). Server kiểm tra phiên,
   thành viên, phòng mở, dung lượng và số lượt gửi; cấp UUID tệp và cổng UDP dữ liệu.
2. `TruyenTep` dùng **DatagramSocket riêng**. Mỗi gói nhị phân có 23 byte header:
   loại (1), UUID (16), số mảnh (4), độ dài (2), sau đó tối đa 1.000 byte dữ liệu.
   Tổng 1.023 byte, nhỏ hơn giới hạn 1.399; không phải mã hóa Base64.
3. Bên gửi cho phép **32 mảnh đang chờ**. ACK chứa mốc các mảnh liên tiếp đã nhận và
   bitmap 32 mảnh tiếp theo. Chỉ gửi lại mảnh thiếu, chờ ban đầu 300 ms rồi tăng gấp đôi,
   tối đa 2.400 ms. Khi ACK xác nhận nhiều mảnh sau lỗ hổng, có thể gửi lại sớm.
   `upload.maxRetries=8` cấu hình số lần gửi lại (đã được người dùng duyệt).
4. Bên nhận ghi `offset = số mảnh × 1.000` bằng FileChannel, bỏ mảnh trùng.
   Luồng receive chỉ đưa gói vào hàng đợi tối đa 2.048; worker ghi đĩa và kiểm CRC32.
   Dữ liệu tệp không được giữ toàn bộ trong RAM. Mỗi nhịp tối đa 32 gói/2 ms để nhường chat.
5. ACK cuối chỉ gửi sau khi CRC32 đúng. Client gửi `FILE_DONE`; Server lưu metadata,
   tạo tin loại F rồi **phát tin qua multicast của phòng**. Nội dung file đi unicast;
   thông báo thẻ file đi multicast và có roomId như mọi tin khác.
6. Tải xuống dùng `FILE_GET` → Client chuẩn bị tệp tạm → `FILE_READY` → cửa sổ DATA/ACK
   theo chiều ngược lại → CRC32 → đổi tên. Không dùng REPLACE_EXISTING; nếu trùng tên
   tự thêm ` (1)`, ` (2)`... Hủy dùng cả CANCEL nhị phân và lệnh UDP có ACK.

ReliableUdp vẫn dùng cho điều khiển; PhanManh vẫn dùng cho avatar nhỏ.
Không dùng hai lớp đó để dừng-chờ từng mảnh tệp lớn vì sẽ chậm trên Wi-Fi.
Các lớp M4 mới tách phần cửa sổ UDP, lưu trữ, điều phối Client và Swing để mỗi file dễ đọc,
không gộp socket vào UI hoặc để ServerCore/ClientCore vượt khoảng 400 dòng.

### Lưu trữ và cấu hình

- `data/config.properties`: `upload.maxBytes=10485760`, `upload.maxRetries=8`.
- `data/uploads/<roomId>/<UUID>.bin`: bản gốc; `.jpg`: thumbnail; `.properties`: metadata
  tên gốc đã làm sạch, người gửi, phòng, kích thước, CRC32, thời gian. `.part` chỉ dùng tạm.
- UUID do Server cấp, đường dẫn được normalize/kiểm containment; không dùng tên gốc làm đường dẫn.
- Quá 60 giây không có gói, hủy hoặc đăng xuất: đóng kênh và xóa tệp tạm. Khởi động quét orphan.
- Tối đa hai upload/người, hàng đợi Client 16; engine tối đa 64 lượt, cache thumbnail 100 ảnh.
- Tệp cũ vẫn tải sau restart; thu hồi xóa bản gốc, thumbnail và metadata; xóa phòng dọn uploads.

### Giới hạn đã biết

UDP không đảm bảo giao nhận; CRC32 kiểm tra lỗi, không xác thực người gửi như chữ ký số.
Không tiếp tục một tệp đang dở sau mất kết nối: cần bấm Thử lại. Mạng Wi-Fi yếu có thể chậm
hoặc hết số lượt gửi lại. Multicast cần cùng mạng LAN và cho Java qua Windows Firewall;
cổng UDP dữ liệu được cấp động nên cần cho ứng dụng Java qua firewall, không chỉ mở cổng 5000.
Token, mật khẩu và tệp đi qua UDP **không mã hóa**. Ảnh động chỉ xem trước khung đầu.
Tệp tải về không tự thực thi; chỉ ảnh PNG/JPG/GIF có thao tác mở ảnh do người dùng chọn.

M5 sẽ bổ sung kiểm tra mute/ban vào `QuanLyPhong.kiemTraGui`: cả tin và FILE_INIT/FILE_DONE đều đi qua điểm kiểm tra chung này.


### Kết quả đo M4 trên máy phát triển (14/11/2026)

`TepSelfTest`: **12 PASS, 0 FAIL**, tiến trình exit code 0. Dữ liệu 5 MB được so từng byte
và CRC32 ở hai chiều. Tốc độ dưới đây tính cả INIT, CRC và DONE, đo localhost bằng UDP thật:

| Mất DATA/ACK tại mỗi đầu | Kích thước | Gửi lên | Tải xuống |
|---|---:|---:|---:|
| 0% | 1 MB | 0,768 giây — 1,30 MB/s | 0,749 giây — 1,34 MB/s |
| 0% | 5 MB | 3,015 giây — 1,66 MB/s | 2,657 giây — 1,88 MB/s |
| 20%, seed cố định | 5 MB | 28,130 giây — 0,18 MB/s | 39,886 giây — 0,13 MB/s |

Độ trễ tin chat khi hai người đang truyền: khoảng **14,3 ms** trong lượt này.
Số MB/s trong phép đo dùng 1 MB = 1.048.576 byte. Tốc độ thay đổi theo CPU, đĩa và lịch chạy luồng;
đây không phải cam kết tốc độ Wi-Fi, cũng chưa phải phép đo giữa hai máy LAN.

Kiểm thử dùng `127.0.0.1` cho endpoint dữ liệu giả lập thủ công để tránh trộn IPv4/IPv6;
ứng dụng thật lấy cùng địa chỉ Server đã xác thực, và gọi MangUtil.dungIPv4 trước khi mở socket.
Đã kiểm tra trực tiếp cổng 5000: HELLO/RESPONSE thành công khi Server đã Bắt đầu.
Lỗi trong ảnh Client "Không nhận được ACK" xảy ra khi chưa có Server lắng nghe localhost:5000.
Thông báo kết nối mới hướng dẫn chạy Server trước. KenhPhong.close đóng lặp lại an toàn và
xử lý lỗi drop-membership Windows/JDK được bọc trong AssertionError có SocketException bên trong.

Chưa kiểm tra: tốc độ giữa hai máy LAN/Wi-Fi, firewall máy khác, DPI khác, tương tác kéo thả/chọn
file/mở thư mục và mở ảnh bằng chuột trên ba cửa sổ thật. UiPreview là vẽ component Swing thật,
không thay thế kiểm thử thao tác người dùng. M5 (mute/ban/dashboard/báo cáo) chưa triển khai.


Lượt hồi quy cuối cùng `SelfTest`: **56 PASS, 0 FAIL**, exit code 0 (14/11/2026).
Riêng lượt này tệp 5 MB mất 20% gói: upload 44,552 giây (0,11 MB/s), download 28,659 giây
(0,17 MB/s); chat đang truyền 10,4 ms. Hai lượt có seed giống nhau nhưng thời gian chạy,
thứ tự lịch luồng và thứ tự DATA/ACK có thể khác, vì vậy tốc độ không hoàn toàn giống nhau.
`mvn -q compile` thành công với `-Xlint:all`, `failOnWarning=true`. UiPreview: 43 PNG và assertions PASS.
Không file Java ứng dụng nào quá 400 dòng; ui không import java.net, logic không import Swing
(ngoại trừ ServerMain theo yêu cầu). Không thêm thư viện Maven ở M4.
