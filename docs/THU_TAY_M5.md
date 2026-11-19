# Mở bộ thử M5 bằng một lệnh

Bộ thử dùng **Server và ba Client thật**, cùng source hiện tại; không phải UiPreview. Mỗi lần chạy tạo một thư mục mới trong `.selftest/thu-tay-m5-.../`, không dùng `data/`, `client.properties` hoặc báo cáo thật của project. Không tự thao tác nút hoặc đăng nhập. Chỉ coi tương tác đạt sau khi người thử xác nhận kết quả.

Từ terminal ở thư mục project:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\thu-tay-m5.ps1
```

Windows hiện chặn chạy file `.ps1` theo chính sách mặc định của máy. `-ExecutionPolicy Bypass` ở lệnh trên chỉ áp dụng cho **tiến trình PowerShell mới đó**, không gọi `Set-ExecutionPolicy`, không thay chính sách hệ thống. Có thể đọc [script](../scripts/thu-tay-m5.ps1) trước khi chạy. Cần Java21 và Maven đang dùng của project; nếu máy có đường dẫn khác, truyền `-Java '.../bin/java.exe' -Maven '.../mvn.cmd'`.

Để chỉ compile và tạo dữ liệu, chưa mở cửa sổ:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\thu-tay-m5.ps1 -ChiChuanBi
```

Script không tự đóng Server khác. Nếu UDP5000 đang bị dùng, script dừng phần mở ứng dụng và in đường dẫn fixture vừa chuẩn bị. Hãy tự đóng Server cũ trước khi chạy lại; không tắt firewall hoặc đổi cổng của dữ liệu thật để thử.

## Chạy và kiểm thử trong NetBeans 31

Project có `nbactions.xml` với các mục chạy Maven riêng:

- `M5 - Prepare isolated fixture`: tạo dữ liệu kiểm thử trong `.selftest/netbeans-run/data`.
- `M5 - Run isolated Server`, `M5 - Run Client 1/2/3`: mở các cửa sổ dùng đúng fixture và profile riêng; Server không dùng thư mục `data/` thật.
- `M5 - Full SelfTest`, `M5 - AdminSelfTest`, `M5 - Swing handler test`: chạy các main test với classpath test trong Output của NetBeans.
- `M5 - UiPreview (isolated output)`: xuất ảnh vào `.selftest/netbeans-preview/docs/screenshots`, giữ ảnh tham chiếu trong `docs/screenshots`.

Trong cửa sổ Projects, bấm phải project `UDPmultiChat` → `Reload Project` nếu các action chưa hiện → `Run Maven` → chọn action cần chạy. Chạy Prepare một lần trước Server/Client; sau đó bấm **Bắt đầu** trên Server và chạy từng Client. Tài khoản/mật khẩu mẫu và năm bước thao tác nằm bên dưới. SelfTest là các main Java, không được Maven Surefire tự chạy khi bấm Test Project; dùng action M5 tương ứng. Không dùng Run Project mặc định cho phiên thử vì cấu hình mặc định có thể ghi `client.properties` và `data/` ở project root. Các action M5 đặt cấu hình và dữ liệu trong `.selftest` riêng.

NetBeans ánh xạ action Maven tới các goal trong project; xem [Best Practices for Apache Maven in NetBeans](https://netbeans.apache.org/wiki/main/wiki/MavenBestPractices/). Lượt hiện tại chưa được chạy bằng cú nhấp trong IDE: NetBeans đang mở nhưng helper điều khiển Windows lỗi khởi tạo (`windows sandbox failed: helper_unknown_error: setup refresh had errors`). Cấu hình action đã được thêm; cần chạy chúng trong NetBeans để xác nhận Output và tương tác cửa sổ trên máy này.
## Tài khoản và tệp mẫu

| Cửa sổ | Tài khoản | Mật khẩu lúc mới tạo |
|---|---|---|
| Client1 | admin | admin123; phải đổi trước khi vào Sảnh |
| Client2 | m5_a | abcdef123 |
| Client3 | m5_b | abcdef123 |

Đây là mật khẩu mẫu công khai cho fixture, không dùng cho dữ liệu thật. Ba file `client-1.properties` đến `client-3.properties` chỉ nhớ username và `127.0.0.1`, không lưu mật khẩu. Cùng một localhost vì bộ này thử trên một máy; không phải phép thử hai máy LAN.

Trong thư mục vừa tạo có:

- `HUONG_DAN.txt`: tài khoản và bước mở nhanh.
- `data/`: dữ liệu Server riêng; `reports/` sẽ nằm cạnh nó khi xuất báo cáo.
- `tep-mau.txt` và `tep-mau-5MiB.bin`: tệp gửi thử; tệp lớn 5.242.880 byte, CRC32 **150838231**. `tep-mau.properties` lưu số đo để đối chiếu.
- `compile.log`, `fixture.log`, `server.log/.err`, `client-*.log/.err`: log của bộ thử.
- `tien-trinh.json`: PID của đúng bốn tiến trình vừa khởi chạy để nhận biết; script không dùng PID này để tự đóng ứng dụng.

## Năm bước thao tác

1. Trên Server bấm **Bắt đầu**. Ở Client1 đăng nhập admin, đổi mật khẩu mặc định bằng một mật khẩu riêng cho fixture. Ở hai Client còn lại đăng nhập `m5_a`/`m5_b`; username và Server đã được điền trước.
2. Ở admin mở Quản trị, đi qua cả5 tab. Thử tìm/lọc/sort/Làm mới; thử thêm user tạm, buộc đổi, reset rồi login lại, khóa/mở. Hai Client thường không có menu Quản trị. Thử xác nhận xóa sai login và nhập dữ liệu sai để xem lỗi inline.
3. Tạo phòng, cho hai thành viên vào; thử mute/unmute, kick, ban/unban, chuyển chủ. Kiểm đúng phòng bị đóng khi kick; mute khóa text/sticker/file/thu hồi. User offline vẫn xuất hiện trong thành viên và thống kê.
4. Gửi broadcast và hai tệp mẫu; Client kia tải về, so byte/CRC. Xuất báo cáo tại admin và Server, mở4 CSV/HTML, kiểm số người/tệp/bytes. Xuất nhật ký riêng, kiểm nút mở HTML vẫn trỏ báo cáo nghiệp vụ.
5. Trên cùng cửa sổ Server bấm **Dừng**, rồi **Bắt đầu** để nạp lại cùng thư mục data. Kiểm mật khẩu, lock/mute/ban và dữ liệu vẫn giữ; thử resize, Enter/Esc, menu chuột. Ghi lỗi và bước tái hiện nếu có.

Sau khi thử, đóng các cửa sổ. Fixture và log được giữ nguyên; script không xóa chúng. Chạy script lần khác tạo bộ dữ liệu mới, nên dùng **Dừng/Bắt đầu trên cùng cửa sổ Server** khi thử persist. Các ảnh tham chiếu nằm ở [screenshots/README.md](screenshots/README.md), kết quả tự động ở [BAO_CAO_RA_SOAT.md](BAO_CAO_RA_SOAT.md).

Nếu source vừa được sửa/compile trong lúc bộ thử đang mở, đóng bộ cũ và chạy lại script để mọi cửa sổ dùng cùng bản mới. Không suy rằng JVM đang chạy đã tự nạp các class vừa compile.

## Kiểm thử sự kiện Swing tự động bổ sung

Đã chạy `com.mycompany.udpmultichat.ui.QuanTriTuongTacSelfTest`: **5 PASS,0 FAIL**, exit0. Bài này mở một QuanTriFrame thật cùng Server/ba ClientCore UDP thật ở data/cổng riêng, gọi handler trên EDT qua `doClick`, default-button, binding Esc và `JFileChooser.approveSelection`. Nó không điều khiển chuột/phím hệ điều hành, không đọc clipboard hoặc mở viewer ngoài.

```powershell
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q test-compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.QuanTriTuongTacSelfTest' '-Dexec.classpathScope=test'
```

Đây là main riêng cần AWT headful, không tự chạy bởi Surefire hay nằm trong số75 PASS của SelfTest mạng. Log lượt cuối: `.selftest/m5-swing-events-3.log/.err`. Các nhóm đã thử:

1. Form lỗi, default button Enter và binding Esc; tạo/sửa user từ handler thật.
2. Reset ngắt phiên, đổi mật khẩu tạm, khóa/mở, mute/gỡ mute toàn hệ thống, xác nhận xóa và guard tự khóa.
3. Mute/unmute phòng, kick, ban/unban và chuyển chủ từ menu thành viên.
4. Broadcast từ form đến hai Client qua UDP thật.
5. Chọn thư mục trong JFileChooser, tạo/tải/giải nén báo cáo và nhật ký; trạng thái nút mở HTML.

Nhờ bài này đã sửa thêm lỗi refresh làm mất dòng đang chọn và bổ sung menu gỡ cấm chat toàn hệ thống. Vẫn chưa xác nhận cảm giác chuột/phím, clipboard/viewer, thao tác trên màn hình/DPI khác hoặc mạng LAN hai máy.

## Phạm vi đã kiểm chứng

Chế độ chuẩn bị đã được chạy: compile/test-compile thành công, tạo đúng3 tài khoản, không có mật khẩu thô trong users.csv, ba profile riêng và CRC tệp mẫu đúng. Chế độ mở ứng dụng cũng đã chạy exit0: bốn tiến trình javaw của fixture `thu-tay-m5-20261118-095909-5cdee4ea` còn hoạt động khi kiểm tra, các file stderr rỗng. Việc tiến trình được khởi chạy hoặc dữ liệu được tạo **không** chứng minh nút/phím đã thao tác thành công. Công cụ computer-use hiện lỗi khởi tạo sandbox; vẫn chờ kết quả thử tay để chốt M5 theo prompt.
