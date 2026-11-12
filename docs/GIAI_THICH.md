# Giải thích M1/M2 — Socket UDP và Multicast

## Luồng chạy

ClientMain mở Swing. ServerMain mở giám sát; nút Bắt đầu gọi ServerCore trên luồng nền. Server giữ trạng thái thật và kiểm tra mọi quyền, Client không tự quyết định vai trò.

```text
UI → ClientCore → ReliableUdp → UDP unicast :5000 → ServerCore
                                                     ↓
                                       QuanLyPhong kiểm tra, lưu tin
                                                     ↓
                 MulticastSocket.send → nhóm/cổng của phòng A hoặc B
                                                     ↓
           KenhPhong lọc roomId → DongBoTinNhan → Listener → Swing EDT
```

## Chức năng → file → hàm (M1)

| Chức năng | File | Hàm |
|---|---|---|
| Mở chương trình | ClientMain / server.ServerMain | main |
| DatagramSocket, DatagramPacket | ReliableUdp | vongNhan, guiRaw |
| Seq, ACK, gửi lại 500 ms, loại trùng | ReliableUdp | gui, thuGui, nhanTinCay |
| Chọn card IPv4 thật | MangUtil | dungIPv4, timGiaoDienMang |
| Đăng ký/đăng nhập, chặn hai nơi | QuanLyNguoiDung | dangKy, dangNhap |
| Salt + SHA-256 | BaoMat | ngauNhien, bam, dungMatKhau |
| Token + địa chỉ UDP | QuanLyNguoiDung | kiemTra |
| Heartbeat, hết phiên | ClientCore / ServerCore | batHeartbeat, ping / quetPhien |
| Tách mạng khỏi Swing | Listener / ChatMulticastUI | on... / thucHien |
| Lưu tài khoản/config | KhoLuuTru | docNguoiDung, ghiNguoiDung, napCauHinh, ghiAnToan |

## Chức năng → file → hàm (M2)

| Chức năng | File | Hàm |
|---|---|---|
| DTO phòng không lộ mật khẩu/hash | common/model/Phong | cacTruong, tuTruong |
| Tạo phòng + nhóm/cổng riêng + mã 6 ký tự | server/QuanLyPhong | tao, taoMa |
| Sửa, đổi mật khẩu/loại/giới hạn | server/QuanLyPhong | sua |
| Xóa, đóng/mở | server/QuanLyPhong | xoa, dongMo |
| Quyền chủ phòng theo từng phòng, admin toàn hệ thống | server/QuanLyPhong | duocQuanLy, kiemTraQuyen |
| Bảo vệ Sảnh chung | server/QuanLyPhong | constructor, sua, xoa, dongMo |
| Vào bằng id công khai hoặc mã riêng tư | server/QuanLyPhong | datCho |
| Đặt chỗ 10 giây, không vượt capacity | server/QuanLyPhong | datCho, xacNhanVao, donDatCho, soChoDaDung |
| Nhiều phòng trên một tài khoản | QuanLyNguoiDung.Phien / ClientCore | phong, choVao / vaoPhong, moKenh, roiPhong |
| Gửi tin đúng nhóm/cổng phòng | server/ServerCore | guiTin, phatMulticast |
| Lưu tin trước khi phát, msgId riêng mỗi phòng | server/QuanLyPhong | themTin, mocTin |
| Danh sách phòng realtime, lọc phòng riêng | ServerCore / QuanLyPhong | capNhatPhong, guiDanhSachPhong / danhSachCho |
| Thành viên/role riêng từng phòng | ServerCore / ClientCore | capNhatThanhVien / nhanThanhVien |
| Mỗi phòng một MulticastSocket nhận | client/KenhPhong | constructor, vongNhan, close |
| Chặn nhận lẫn nhóm trên Windows | client/KenhPhong | vongNhan: so sánh roomId trước khi xử lý MSG/SYSTEM |
| Lịch sử 50 tin | QuanLyPhong / ServerCore | layTin / guiLichSu (HISTORY) |
| Bù theo lô 50 khi thiếu msgId | ClientCore / ServerCore | hoiDongBo, dongBoPhong / guiLichSu (RESYNC) |
| Phát hiện mất tin cuối | ServerCore / DongBoTinNhan | guiMocTin / nhanMoc |
| Sắp thứ tự, loại trùng giữa multicast và unicast bù | client/DongBoTinNhan | nhan, nhanMuc, nhanKetThuc, phatLienTiep |
| CSV phòng, log tin UTF-8, nạp lại sau restart | server/KhoLuuTru | docPhong, ghiPhong, docTin, ghiTin |
| + Tạo phòng, nhập mã, menu Sửa/Xóa/Đóng-mở | ui/DanhSachPhongPanel | nhap, vaoBangMa, vaoPhong, menu |
| Nhiều tab, đóng tab khi phòng bị xóa | ui/ChatMulticastUI | onVaoPhong, onRoiPhong |
| Badge tin chưa đọc; lịch sử không tăng badge | ui/ChatMulticastUI | onTinNhan, capNhatDangChon, capNhatTieuDeTab |
| Thông báo có dấu, IP chỉ tooltip | ServerCore / KhungChatPanel | sanSang, roiPhong / heThong, capNhat |
| Kiểm thử phòng/cách ly/lịch sử/RESYNC | src/test/.../PhongSelfTest | chayTatCa, testCungCong |

## Multicast nằm ở đâu, theo từng phòng?

`QuanLyPhong.tao` cấp địa chỉ **239.255.42.X** và cổng **9000+X**. Sảnh chung dùng X=1. Không có một nhóm cố định để phát toàn bộ tin các phòng. `ServerCore.phatMulticast(roomId, duLieu)` lấy đúng Phong, rồi gọi `MulticastSocket.send(DatagramPacket)` tới nhóm/cổng đó. Socket phát được chọn card IPv4 bằng `MangUtil.timGiaoDienMang`, TTL=1.

Client có bản đồ `ClientCore.cacKenh`, mỗi roomId một `KenhPhong`. Constructor gọi `MangUtil.vaoNhom` để tạo MulticastSocket, setReuseAddress/bind, setNetworkInterface và `joinGroup`. `KenhPhong.vongNhan` gọi receive, giải mã UTF-8, so sánh roomId rồi mới chuyển tin. `KenhPhong.close` gọi leaveGroup/close khi rời phòng, xóa phòng, đăng xuất hoặc mất kết nối. Windows có thể đưa gói nhóm khác vào socket cùng cổng; kiểm tra roomId vẫn bắt buộc dù địa chỉ/cổng riêng.

Lịch sử và RESYNC dùng **UDP unicast có ACK** để gửi bù đáng tin cậy. Tin chat trực tiếp dùng multicast. Các tin bù và tin trực tiếp đi chung DongBoTinNhan để tránh hiển thị hai lần hoặc sai thứ tự.

## Gói và luồng vào phòng

Mọi datagram text UTF-8 dưới 1400 byte. `Goi.ghep/tach` escape `|`, `\`, CR/LF; `Goi.noi` nối mảng trường phẳng để tránh nhiều lớp escape không cần thiết.

- Ngoài cùng: `R|seq|noiDung` hoặc `A|seq`. ACK chỉ báo đã nhận vào hàng xử lý; `RESPONSE|requestId|OK/ERR|...` mới là kết quả nghiệp vụ.
- Chat: `MSG|roomId|msgId|userId|ten|thoiGian|noiDung`. Server tự cấp danh tính/thời gian/msgId sau kiểm tra token + membership.
- Vào: `PREPARE` kiểm tra mã/mật khẩu/capacity và đặt chỗ 10 giây → Client joinGroup → `READY` xác nhận membership → `HISTORY` lấy 50 tin. Socket phải vào nhóm trước READY để hạn chế bỏ lỡ tin mới. Vào lại phòng đã có tab chỉ chuyển tab, không nhân đôi socket.
- Snapshot `ROOMS`/`ONLINE` có phiên bản, tổng số, chỉ số. Ghép đủ mới cập nhật UI, bỏ phiên bản cũ; chỉ gửi các phòng người đó được thấy. Lô dữ liệu đi từng gói chờ ACK, không chặn luồng nhận.
- `SYNC_ITEM` kèm roomId/mã đồng bộ và trường MSG; `SYNC_END` kèm mốc đầu/cuối/số lượng/head. Client ghép đủ lô, trộn với buffer multicast, phát tăng dần msgId; lô cũ và msgId trùng bị bỏ. Nếu còn thiếu, xin lô sau.
- Mỗi PING, Server gửi `HEAD|roomId|lastMsgId` cho từng phòng đang ở; vì thế không cần có tin tiếp theo mới phát hiện tin cuối bị mất.

## Phân quyền, lưu trữ và ranh giới

ADMIN là quyền toàn hệ thống. Chủ phòng được xác định bằng `Phong.chuId == user.id`, không đổi vai trò toàn hệ thống của user khi tạo phòng. Đóng phòng giữ membership/lịch sử nhưng chặn gửi và vào mới; xóa phòng gửi ROOM_LEFT tin cậy để Client đóng tab/socket. Sảnh chung không được xóa/đổi loại/đóng. Log phòng đã xóa giữ trên đĩa, id mới không tái sử dụng; nhóm/cổng cũ có thể được cấp lại và roomId ngăn lẫn gói.

KhoLuuTru giữ schema users.csv M1, thêm rooms.csv và rooms/<roomId>.log. CSV hỗ trợ dấu phẩy/dấu nháy; nội dung một dòng, tin xuống dòng dùng escape. Config/CSV ghi file tạm + thay thế; log append qua FileChannel rồi force, lỗi ghi trả lại chiều dài cũ. Không dùng tên phòng làm đường dẫn. Mọi truy cập trạng thái Server từ luồng nhận/heartbeat được bảo vệ cùng khóa ServerCore.

UI không import java.net; client/common và ServerCore/manager không import javax.swing. ServerMain là giao diện giám sát nối ServerCore.Listener. Mọi thao tác mạng được đẩy ra luồng nền, cập nhật Swing bằng invokeLater. KhungChatPanel chỉ vẽ một phòng; DanhSachPhongPanel chứa cả dialog để giảm số file.

## Đối chiếu chương 6

UDP dùng IP + port và DatagramSocket/DatagramPacket, không thiết lập kết nối TCP. Multicast dùng địa chỉ nhóm lớp D, MulticastSocket và chuỗi joinGroup/send/receive/leaveGroup/close. ReliableUdp bổ sung độ tin cậy ở tầng ứng dụng, không đổi sang TCP. Không có TCP, ServerSocket hay WebSocket.

## Kiểm chứng và giới hạn

M1 đã được người dùng xác nhận chat hai Client qua ảnh chụp. M2 kiểm tra tự động bằng SelfTest theo lệnh README: tài khoản, mất 20% DATA/ACK có seed, cách ly cả hai socket cùng cổng, CRUD/quyền/mật khẩu/capacity, lịch sử 50, RESYNC tin cuối và hơn 50 tin mất liên tiếp, restart. Cổng test 19000/21000 tách khỏi cổng chat thật; không dùng data thật.

UDP không đảm bảo tuyệt đối; RESYNC cần Server còn truy cập được. Multicast cần LAN và Java được phép qua Windows Firewall. Mật khẩu/token qua UDP không mã hóa; multicast chưa xác thực nguồn phát. Lịch sử hiện giữ trong RAM, chưa tối ưu quy mô lớn. VS Code có cấu hình Server/Client 1/2/3; việc bấm F5/Run File trên IDE cần thử tay. Kết quả M2: **24 PASS, 0 FAIL** trên Java 21/Maven NetBeans. Đã chạy multicast thật trên card Wi-Fi: cách ly phòng/cùng cổng, quyền/mật khẩu/capacity, lịch sử 50, mất tin giữa/tin cuối, mất 70 tin cần hai lô RESYNC và vào phòng trong lúc tin mới đến. Kiểm tra nguồn: 24 file Java ứng dụng + 2 file test, dài nhất 308 dòng; comment dòng không dấu và ranh giới UI/mạng đã qua. Maven bật Xlint:all/failOnWarning, test-compile thành công. JSON launch có đúng Server/Client 1/Client 2/Client 3 và mọi class đích tồn tại. Chưa thao tác trực tiếp F5/Run File hoặc kiểm tra trực quan giao diện M2; cần thử tay theo README.
