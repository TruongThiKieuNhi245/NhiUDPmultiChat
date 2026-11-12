# Giải thích M1 / M2 / M2.5 — Socket UDP và Multicast

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
| Nhiều tab, đóng khi xóa phòng | ui/ChatMulticastUI | onVaoPhong, onRoiPhong |
| Badge, không tính lịch sử | ui/ChatMulticastUI | onTinNhan, chonTab, tieuDeTab |
| Kiểm thử phòng/cách ly/RESYNC | src/test/.../PhongSelfTest | chayTatCa, testCungCong |

## Chức năng → file → hàm M2.5

| Chức năng | File | Hàm |
|---|---|---|
| FlatLaf + design tokens, không rải màu/font | ui/Theme | caiDat, nut, nhan, le, boTron, icon |
| Cài LAF trước tạo Swing | ClientMain / server/ServerMain | main gọi Theme.caiDat |
| Card đăng nhập/chat, ba cột, bỏ banner | ui/ChatMulticastUI | constructor, taiKhoan, tieuDePhong |
| Tab đăng ký, nhập lại, lỗi inline, spinner | ui/DangNhapPanel | constructor, gui, loiServer, ban |
| Hiện/ẩn mật khẩu dùng chung | ui/HopThoai | oMatKhau |
| Kiểm tra Server trước đăng nhập | ui/ChatMulticastUI / client/ClientCore / server/ServerCore | thuServer / kiemTraServer / xuLyGoi với HELLO |
| Nhớ tên và Server, không lưu bí mật | client/CauHinhClient / client/ClientCore | doc, ghi, tepCauHinh / dangNhap |
| Toast tối đa ba, bốn mức, hover dừng, nút đóng | ui/components/ThongBao | hien, sapXep, xoa, Toast.ganHover, close |
| Dải mất mạng/nối lại, khóa ô soạn | ui/ChatMulticastUI | onKetNoi, capNhat |
| Tự đăng nhập lại, giữ mốc tin, RESYNC | client/ClientCore | matKetNoi, khoiPhuc, dongBoPhong |
| Xác thực lại cùng endpoint, giữ chặn hai nơi | server/QuanLyNguoiDung / server/ServerCore | ketNoiLai / xuLyGoi với RECONNECT |
| Ô nhiều dòng, Enter gửi/Shift+Enter xuống dòng, byte counter | ui/SoanTinPanel | constructor, capNhat, vien, choPhep |
| Chặn >600 byte UTF-8 ở cả hai phía | client/ClientCore / server/QuanLyPhong | guiTin / themTin; common/Protocol.TIN_TOI_DA |
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

## Multicast nằm ở đâu, theo từng phòng?

`QuanLyPhong.tao` cấp **239.255.42.X:9000+X**, X từ 1 đến 254; Sảnh dùng X=1. `ServerCore.phatMulticast(roomId, duLieu)` lấy nhóm/cổng của đúng phòng và gọi `MulticastSocket.send(DatagramPacket)`. Socket phát chọn card bằng `MangUtil.timGiaoDienMang`, TTL=1.

`ClientCore.cacKenh` lưu mỗi roomId một KenhPhong. Constructor KenhPhong gọi `MangUtil.vaoNhom` tạo MulticastSocket, reuseAddress/bind, chọn card, joinGroup. `KenhPhong.vongNhan` receive, giải mã UTF-8, **lọc roomId rồi mới xử lý**. Windows có thể giao gói nhóm khác cho socket cùng cổng, vì vậy lọc này vẫn bắt buộc dù các phòng có cổng riêng.

`KenhPhong.close` leaveGroup/close khi rời/xóa phòng hoặc đóng phiên. **M2.5 thay đổi lúc mất mạng tạm thời:** giữ socket và bộ đếm msgId của các tab để có thể nối lại, tự RESYNC; rời/xóa/đăng xuất mới đóng tài nguyên. Khi Server bật lại, Client dùng RECONNECT → PREPARE → READY cho từng tab → RESYNC → PING. Nếu phòng đã bị xóa thì đóng tab đó.

Tin trực tiếp đi qua multicast; lịch sử/tin bù đi qua UDP unicast có ACK. Cả hai đi qua DongBoTinNhan để sắp thứ tự và tránh hiển thị hai lần. Không dùng multicast chung để truyền chéo tất cả phòng.

## Định dạng gói và quá trình vào phòng

- Datagram UTF-8 dưới 1400 byte. `Goi.ghep/tach` escape `|`, dấu gạch chéo ngược, CR/LF; `Goi.noi` nối trường phẳng, tránh escape nhiều lớp.
- Bọc ngoài `R|seq|noiDung` / `A|seq`. ACK chỉ xác nhận vào hàng xử lý; `RESPONSE|requestId|OK/ERR|...` mới xác nhận nghiệp vụ. Gửi lần đầu và tối đa 3 lần gửi lại, cách 500 ms.
- Chat `MSG|roomId|msgId|userId|ten|thoiGian|noiDung`; Server gán danh tính/thời gian/msgId sau kiểm tra token/quyền.
- PREPARE kiểm tra mã/password/capacity và đặt chỗ 10 giây → Client joinGroup → READY lưu membership → HISTORY 50 tin. Vào tab đã có chỉ chuyển tab, không tạo socket trùng.
- Snapshot ROOMS/ONLINE có phiên bản, tổng số/chỉ số; Client ghép đủ mới cập nhật, bỏ phiên cũ. ONLINE ở M2.5 có cả thành viên offline với cờ online, không đồng nhất tư cách thành viên với phiên đang chạy.
- SYNC_ITEM/SYNC_END ghép lô tối đa 50, trộn buffer multicast, phát tăng dần msgId. Mỗi PING trả HEAD của các phòng để phát hiện mất cả tin cuối.

## Quyền, lưu trữ và luồng

ADMIN là quyền hệ thống; chủ phòng là `Phong.chuId`, không thay quyền hệ thống khi tạo phòng. Phòng riêng chỉ hiện với chủ/admin/thành viên đã lưu. Biết mã cho phép tìm phòng; muốn bảo vệ thêm cần đặt mật khẩu. Sảnh chung không cho Sửa/Xóa/Rời/Đóng. Chủ phòng chỉ Xóa hoặc Chuyển quyền cho thành viên khác rồi Rời.

Membership ở `data/members.properties` tồn tại sau logout/restart; logout/timeout chỉ loại phiên/kênh đang online. Rời chủ động mới xóa membership. Số thành viên tính cả offline để capacity ổn định; danh sách thành viên hiển thị riêng tổng số và online. Dữ liệu M2 cũ chưa lưu membership nên người không phải chủ cần vào lại một lần.

KhoLuuTru dùng users.csv, rooms.csv, members.properties và rooms/<roomId>.log. CSV/properties ghi tạm rồi thay thế, thử lại có giới hạn nếu Windows tạm khóa file. Log append tuần tự và force xuống đĩa; lỗi ghi khôi phục chiều dài cũ. Không dùng tên phòng làm đường dẫn. Trạng thái Server được bảo vệ cùng khóa ServerCore; không chạy hai Server ghi chung data.

UI không import java.net; logic common/client/server không import Swing, ngoại lệ ServerMain chỉ giám sát qua Listener. Thao tác mạng chạy nền, cập nhật Swing trên EDT. Theme tập trung màu/font/khoảng cách/bo góc; HopThoai dùng chung form, ThongBao dùng layered pane. Phần mạng không phụ thuộc FlatLaf.

## Kiểm chứng và giới hạn

M1/M2 đã được người dùng xác nhận. M2.5: **26 PASS, 0 FAIL** trên Java 21, Maven NetBeans; multicast thật chạy qua card Wi-Fi. Có seed mất 20% DATA/ACK, cách ly hai nhóm cùng cổng, lịch sử 50 và RESYNC nhiều lô. Bổ sung lưu đúng loại/không lộ riêng tư, phòng đầy/password/chuyển chủ, biên byte UTF-8, offline và Server dừng/bật lại để Client bù 75 tin thiếu. Test riêng `.selftest/`, không dùng dữ liệu thật. Xem lệnh và kịch bản 3 Client trong README.

UiPreview xuất 14 PNG bằng các component Swing thật, không socket; đã kiểm tra bố cục ảnh, không khẳng định đã thử chuột/phím toàn bộ. F5/Run File và tương tác ba cửa sổ cần người dùng kiểm tra; nhiều máy LAN, DPI cao chưa thử. 31 file ứng dụng + 3 file test, file dài nhất dưới 400 dòng; compiler bật Xlint:all/failOnWarning.

**UDP không đảm bảo**. **Multicast cần chung LAN và cho Java qua Windows Firewall**. **Token/mật khẩu đi qua UDP không mã hóa**, multicast chưa xác thực nguồn; salt/hash chỉ bảo vệ trên đĩa. Tự nối lại cần credentials/IP/cổng còn đúng và lịch sử Server còn. Lịch sử hiện nạp RAM, chưa tối ưu quy mô lớn. Discovery và các chức năng M3–M7 chưa triển khai trong mốc này.
