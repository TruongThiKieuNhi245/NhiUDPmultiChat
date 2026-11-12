# Ảnh UiPreview — M2.7

Chạy từ project:

```powershell
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview'
```

Công cụ vẽ **chính component Swing của ứng dụng** lên BufferedImage với dữ liệu mẫu, không kết nối Server. Ảnh là kiểm tra bố cục, không thay thế thử chuột/phím, hover, F5 và DPI trên máy thật. Có thể ghi đè PNG khi chạy lại; không chứa tài khoản/mật khẩu thật.

| Ảnh | Nội dung |
|---|---|
| [01-dang-nhap.png](01-dang-nhap.png) | Card đăng nhập căn giữa |
| [02-dang-ky.png](02-dang-ky.png) | Đăng ký có nhập lại và nút hiện mật khẩu |
| [03-loi-form.png](03-loi-form.png) | Lỗi ngay dưới trường tên |
| [04-chat.png](04-chat.png) | Chat ba cột, mẫu tin, phòng riêng, badge, thành viên offline |
| [05-mat-ket-noi.png](05-mat-ket-noi.png) | Dải cảnh báo và khóa soạn tin |
| [06-toast-ba-muc.png](06-toast-ba-muc.png) | Thành công, thông tin, cảnh báo cùng lúc |
| [07-toast-loi.png](07-toast-loi.png) | Toast lỗi, tối đa ba thẻ |
| [08-menu-nguoi-dung.png](08-menu-nguoi-dung.png) | Menu tài khoản và Đăng xuất |
| [09-tao-phong.png](09-tao-phong.png) | Form tạo phòng |
| [10-sua-phong.png](10-sua-phong.png) | Form sửa, Giữ/Đổi/Xóa mật khẩu |
| [11-vao-phong.png](11-vao-phong.png) | Vào bằng mã kèm mật khẩu |
| [12-server.png](12-server.png) | Cửa sổ giám sát Server |
| [13-tin-qua-dai.png](13-tin-qua-dai.png) | 334 ký tự ắ = 1002 byte, khóa Gửi |
| [14-cai-dat-server.png](14-cai-dat-server.png) | Ô IP Server và kiểm tra kết nối |
| [15-thong-tin-phong.png](15-thong-tin-phong.png) | HopThoai thông tin phòng đầy đủ, có sao chép mã |
| [16-client-vao-cuoi.png](16-client-vao-cuoi.png) | bbb vào sau admin/aaa, nhãn Bạn đúng cạnh bbb |
| [17-chat-960x600.png](17-chat-960x600.png) | Kiểm tra kích thước cửa sổ tối thiểu |
| [18-soan-nhieu-dong.png](18-soan-nhieu-dong.png) | Năm dòng, nút Gửi tròn và emoji/+ căn đáy |
| [19-chu-so.png](19-chu-so.png) | Số 111 trong hai bong bóng, grayscale AA không viền RGB |
| [20-dang-nhap-nhieu-loi.png](20-dang-nhap-nhieu-loi.png) | Tên/mật khẩu/Server cùng có lỗi, input không bị nén |
| [21-dang-ky-nhieu-loi.png](21-dang-ky-nhieu-loi.png) | Ba ô tài khoản/mật khẩu/nhập lại giữ chiều cao khi lỗi |
| [22-tao-phong-co-loi.png](22-tao-phong-co-loi.png) | Tạo phòng có nhiều dòng lỗi, ô nhập/nút vẫn đủ chỗ |
| [23-sua-phong-co-loi.png](23-sua-phong-co-loi.png) | Sửa phòng còn nguyên chữ đã nhập khi lỗi |
| [24-vao-phong-co-loi.png](24-vao-phong-co-loi.png) | Mã phòng và mật khẩu cùng có lỗi |


24 PNG đã được xuất lại bằng UI M2.7. UiPreview còn xác nhận trên component thật: đổi profile vẽ lại đúng một Bạn, hai tên hiển thị trùng nhau không nhầm tài khoản, chuyển phòng xóa badge ngay, nút Tạo phòng không bị cắt và kiểm tra pixel chữ trắng trên nền xanh không có viền RGB.

M2.7 tái hiện bản cũ bằng assertion (ô tên còn 12px), sau sửa toàn bộ ảnh qua kiểm tra chiều cao input (32px cho hàng form; editor bên trong spinner trừ viền), font nội dung PLAIN và vị trí toast. Ảnh 03, 06/07, 15 và các ảnh khác đã xuất lại. Toast một dòng khoảng 48px, nhiều dòng tự cao lên, rộng tối đa 360px. Ảnh hộp Đổi mật khẩu thật thuộc mốc M3a, chưa có tại M2.7.

## M2.8

Vai trò thẻ tài khoản dùng JLabel riêng: tên 14px đậm, vai trò 12px thường màu xám (không dùng kích thước HTML). Ô soạn chia vùng nhập toàn chiều rộng và hàng công cụ bên dưới; nền khóa vẽ bo tròn, không còn góc xám vuông. Bộ đếm gần ngưỡng hiển thị số ký tự / giới hạn ước tính từ tỷ lệ ký tự so với dung lượng nội dung hiện tại; giới hạn thật vẫn kiểm tra UTF-8 và gói UDP. Mô tả phòng giới hạn 80 ký tự ở cả Client và Server, thông báo bỏ thuật ngữ byte. Theme.vietHoaDau chuẩn hóa chữ đầu lỗi. Kiểm tra Font.PLAIN của JTextArea trong UiPreview vẫn được giữ; tên người gửi là nhãn riêng.

File/hàm: ChatMulticastUI.theTaiKhoan/ganNguoiDung; SoanTinPanel constructor/capNhat/paintComponent; Theme.vietHoaDau; HopThoai.kiemTra; QuanLyPhong.kiemTraNoiDung. Ảnh 05/18 là offline/nhiều dòng được xuất lại.


## M3a

UiPreview xuất lại 30 ảnh từ component thật; ảnh 08 là menu ba mục Hồ sơ/Đổi mật khẩu/Đăng xuất. Ảnh 25–30:

| Ảnh | Nội dung |
|---|---|
| [25-ho-so-chua-anh.png](25-ho-so-chua-anh.png) | Hồ sơ, avatar chữ cái 96px |
| [26-ho-so-co-anh.png](26-ho-so-co-anh.png) | Avatar ảnh mẫu vẽ bằng Java2D, Đổi/Xóa và tên/giới thiệu |
| [27-doi-mat-khau.png](27-doi-mat-khau.png) | Ba ô và nút Hiện riêng |
| [28-doi-mat-khau-co-loi.png](28-doi-mat-khau-co-loi.png) | Ba lỗi inline, không nén ô |
| [29-buoc-doi-mat-khau.png](29-buoc-doi-mat-khau.png) | Lý do bắt buộc, chỉ một nút hành động, không Hủy |
| [30-avatar-trong-chat.png](30-avatar-trong-chat.png) | Cùng ảnh trong thành viên/bong bóng, nhãn Bạn và chấm online chỉ ở thành viên |

Các ảnh form dùng chính HoSoDialog đang chạy, không dựng bản giả. Ảnh 29 chụp nội dung dialog; cấm nút X và Esc được cài trong HoSoDialog.hien, cần thử tay cửa sổ thật. Công cụ không mở kết nối mạng. Các kiểm tra chiều cao input, font PLAIN, Bạn/badge và toast vẫn chạy.
