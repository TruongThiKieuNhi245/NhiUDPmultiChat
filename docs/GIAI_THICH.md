# Giải thích M1 / M2 / M2.5 / M2.6 / M2.7 — Socket UDP và Multicast

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
| Vai trò xám 12px, không đậm theo nút cha | ui/ChatMulticastUI, ui/Theme | ganNguoiDung / mauHtml |
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

**UDP không đảm bảo**. **Multicast cần chung LAN và cho Java qua Windows Firewall**. **Token/mật khẩu đi qua UDP không mã hóa**, multicast chưa xác thực nguồn; salt/hash chỉ bảo vệ trên đĩa. Tự nối lại cần credentials/IP/cổng còn đúng và lịch sử Server còn. Lịch sử hiện nạp RAM, chưa tối ưu quy mô lớn. Discovery và các chức năng M3–M7 chưa triển khai trong mốc này.

## M2.8

Vai trò thẻ tài khoản dùng JLabel riêng: tên 14px đậm, vai trò 12px thường màu xám (không dùng kích thước HTML). Ô soạn chia vùng nhập toàn chiều rộng và hàng công cụ bên dưới; nền khóa vẽ bo tròn, không còn góc xám vuông. Bộ đếm gần ngưỡng hiển thị số ký tự / giới hạn ước tính từ tỷ lệ ký tự so với dung lượng nội dung hiện tại; giới hạn thật vẫn kiểm tra UTF-8 và gói UDP. Mô tả phòng giới hạn 80 ký tự ở cả Client và Server, thông báo bỏ thuật ngữ byte. Theme.vietHoaDau chuẩn hóa chữ đầu lỗi. Kiểm tra Font.PLAIN của JTextArea trong UiPreview vẫn được giữ; tên người gửi là nhãn riêng.

File/hàm: ChatMulticastUI.theTaiKhoan/ganNguoiDung; SoanTinPanel constructor/capNhat/paintComponent; Theme.vietHoaDau; HopThoai.kiemTra; QuanLyPhong.kiemTraNoiDung. Ảnh 05/18 là offline/nhiều dòng được xuất lại.
