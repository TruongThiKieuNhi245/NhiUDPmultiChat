# Ảnh UiPreview — M2.6

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

19 PNG đã được xuất lại bằng UI M2.6. UiPreview còn xác nhận trên component thật: đổi profile vẽ lại đúng một Bạn, hai tên hiển thị trùng nhau không nhầm tài khoản, chuyển phòng xóa badge ngay, nút Tạo phòng không bị cắt và kiểm tra pixel chữ trắng trên nền xanh không có viền RGB.
