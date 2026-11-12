# Giải thích M1 — Socket UDP và Multicast

## Luồng chạy

`ClientMain.main` mở Swing. `ServerMain.main` mở giám sát; nút Bắt đầu gọi `ServerCore.batDau` trên luồng nền.

```text
Giao diện → ClientCore → ReliableUdp (UDP unicast :5000)
                               ↓
                     ServerCore kiểm tra phiên
                               ↓
                 MulticastSocket.send → 239.255.42.1:9001
                               ↓
                     KenhPhong.receive → Listener → Swing EDT
```

## Chỉ code khi thầy hỏi

| Nội dung | File / hàm |
|---|---|
| Tạo DatagramSocket, nhận DatagramPacket | `ServerCore.batDau`, `ClientCore.ketNoi`, `ReliableUdp.vongNhan` |
| Gửi UDP unicast | `ReliableUdp.guiRaw` |
| ACK, seq, gửi lại 500 ms, loại trùng | `ReliableUdp.gui`, `thuGui`, `nhanTinCay` |
| Tạo socket, chọn card, vào nhóm multicast | `MangUtil.timGiaoDienMang`, `MangUtil.vaoNhom`, `KenhPhong` |
| Server phát tin cho cả nhóm | `ServerCore.guiTin`, `phatMulticast` |
| Client nhận và lọc roomId | `KenhPhong.vongNhan` |
| Rời nhóm và đóng socket | `KenhPhong.close` |
| Escape và UTF-8 | `Goi.ghep`, `tach`, `thanhByte` và hàm nhận hai loại socket |
| Đăng ký / kiểm tra mật khẩu | `QuanLyNguoiDung.dangKy`, `dangNhap`, `BaoMat.bam` |
| Chặn hai nơi, kiểm tra token + địa chỉ | `QuanLyNguoiDung.dangNhap`, `kiemTra` |
| Heartbeat, offline sau 15 giây | `ClientCore.batHeartbeat`, `ping`, `ServerCore.quetPhien` |
| Lưu/nạp tài khoản, ghi file an toàn | `KhoLuuTru.docNguoiDung`, `ghiNguoiDung`, `ghiAnToan` |
| Giao diện không bị chặn bởi mạng | `ChatMulticastUI.thucHien`, `ServerMain.doiTrangThai` |
| Chuyển sự kiện mạng sang EDT | Các hàm `on...` trong `ChatMulticastUI` và `ServerMain` |

## Gói điều khiển

Lớp ngoài: `R|seq|noiDung` hoặc `A|seq`. Seq có UUID phiên vận chuyển + bộ đếm; bên nhận loại trùng theo địa chỉ/port gửi + seq. Phần nội dung lại là các trường được escape, ví dụ `LOGIN|requestId|ten|matKhau`. Gói trả lời: `RESPONSE|requestId|OK/ERR|...`.

ACK chỉ báo gói đã vào hàng xử lý. Quyền, thành công hay lỗi do RESPONSE quyết định. Mỗi gói gửi lần đầu và được gửi lại tối đa ba lần, 500 ms/lần. Socket nhận chạy riêng nên chờ ACK không chặn nhận hay chặn Swing. Bộ nhớ chờ ACK và hàng xử lý có giới hạn.

Đăng nhập trả token, hồ sơ công khai và địa chỉ Sảnh chung. Client vào nhóm trước, sau đó gửi READY; Server mới thông báo vào phòng. Mỗi snapshot online có phiên bản và chỉ số để ghép lại khi các gói đến khác thứ tự.

## Gói chat multicast

`MSG|roomId|msgId|userId|ten|thoiGian|noiDung`. Server tự cấp msgId, danh tính và thời gian sau khi kiểm tra token. Client chỉ gửi SEND tới Server, không tự phát tin chat multicast. Mọi gói hệ thống cũng có roomId. Tin bị nhận hai lần được loại theo msgId; sắp xếp/RESYNC hoàn thiện ở M2.

## Đối chiếu bài học

UDP dùng địa chỉ IP + port, gửi/nhận từng datagram qua DatagramSocket/DatagramPacket; không thiết lập kết nối TCP. Multicast dùng địa chỉ nhóm lớp D, MulticastSocket, chọn giao diện mạng, joinGroup/send/receive/leaveGroup/close; TTL=1 để giới hạn phạm vi mạng cục bộ. ReliableUdp bổ sung độ tin cậy ở tầng ứng dụng, không biến UDP thành TCP.

## Ranh giới và giới hạn M1

UI chỉ Swing/AWT; ClientCore, KenhPhong, ServerCore và common không import Swing. Riêng ServerMain là cửa sổ giám sát, nối với ServerCore qua Listener. Chỉ có Sảnh chung ở M1; CRUD phòng, lịch sử và RESYNC sẽ ở M2. Không có TCP/ServerSocket/WebSocket.

UDP vẫn có thể mất gói; multicast cần LAN và Windows Firewall cho Java. Mật khẩu/token qua UDP chưa mã hóa. Chạy SelfTest bằng lệnh trong README; dùng thư mục dữ liệu kiểm thử riêng, không dùng data thật.

## K?t qu? x?c minh M1

- Maven NetBeans 3.9.16 + Temurin 21.0.12.1: compile/test-compile th?nh c?ng.
- To?n b? source qua `javac -Xlint:all -Werror --release 21`; Maven b?t failOnWarning.
- SelfTest: **11 PASS, 0 FAIL**, g?m multicast th?t tr?n card Wi-Fi, m?t 20% DATA/ACK (10 l?n g?i l?i, 4 g?i tr?ng ???c lo?i), h?t heartbeat, kh?i ??ng l?i v? Client ph?t hi?n Server d?ng.
- C? 19 file Java ?ng d?ng + 1 SelfTest; file d?i nh?t 252 d?ng. JSON VS Code h?p l?; ki?m tra t? ??ng ranh gi?i Swing/m?ng v? comment d?ng kh?ng d?u ?? qua.
- Hai c?a s? Client/Server ?? kh?i ??ng v? kh?ng ghi l?i stderr, sau ?? ???c ??ng. C?ng c? Windows ki?m tra tr?c quan l?i kh?i t?o, n?n ch?a x?c nh?n b? c?c b?ng ?nh hay thao t?c n?t b?m. Ch?a ch?y tr?c ti?p F5 trong VS Code ho?c Run File trong NetBeans; c?n th? tay theo README.
