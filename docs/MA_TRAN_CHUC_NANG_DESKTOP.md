# Ma trận chức năng bắt buộc — Desktop UDP Multi-room Chat

Cập nhật 17/11/2026. Đây là checklist yêu cầu/kiểm chứng, KHÔNG phải tuyên bố tất cả đã cài đặt. Trạng thái source và test phải xác minh khi làm từng mốc. “Nhóm” và “phòng” là cùng một thực thể nghiệp vụ, không xây hai hệ quản lý trùng nhau.

## 1. Thống kê và xuất báo cáo — yêu cầu của thầy, không được bỏ

### Thống kê

- Tổng tài khoản; đang online/offline; bị khóa; admin và thành viên theo vai trò khi cần tra cứu.
- Tổng phòng, phòng đang mở/đóng; số thành viên bền vững của mỗi phòng, tách số online.
- Tổng tin nhắn và theo phòng/ngày/người; phân biệt tin văn bản, sticker, tệp và tin thu hồi theo định nghĩa thống nhất.
- Tổng tệp và dung lượng; theo phòng và người gửi. Phân biệt dữ liệu còn lưu với dữ liệu đã xóa/thu hồi, ghi rõ cách tính.
- Top 10 người chat nhiều, top phòng sôi nổi; biểu đồ thành viên top 8 phòng, tin 7 ngày.
- Server có số phiên đang hoạt động và tệp đang truyền; dashboard cập nhật 3–5 giây/theo sự kiện.

### Bộ báo cáo bắt buộc

| File | Nội dung tối thiểu |
|---|---|
| nguoidung.csv | Login, tên hiển thị, vai trò, trạng thái, ngày tạo/login cuối, số phòng/tin/tệp/bytes |
| phong.csv | Mã, tên, loại, mở/đóng, chủ, số thành viên/tin/tệp/bytes, ngày tạo |
| thanhvien_theophong.csv | Mỗi dòng một cặp phòng–user, vai trò trong phòng, online, ngày vào, số tin |
| tin_theo_ngay.csv | Ngày, phòng, số tin/tệp/sticker/thu hồi |
| tong_hop.html | Thời điểm, tổng quan, **bảng số người dùng theo từng phòng**, top 10 người chat, top phòng, biểu đồ ngày, user bị khóa |

Tạo thư mục reports/baocao_yyyyMMdd_HHmmss duy nhất; không ghi đè. CSV UTF-8 BOM, RFC4180, chống CSV injection; HTML escape mọi dữ liệu người dùng, CSS nhúng, mở offline/in được. Snapshot nhất quán và xuất nền. Admin Client yêu cầu và tải ZIP qua UDP, Server có nút xuất local; mở thư mục/HTML bằng thao tác người dùng. Xuất nhật ký riêng nhatky.csv là chức năng bổ sung, không thay bốn CSV nghiệp vụ.

Kiểm thử bằng fixture biết trước số user/phòng/tin/tệp; so từng bảng/row, đặc biệt thành viên offline vẫn thuộc phòng. Kiểm BOM, nháy/phẩy/xuống dòng, công thức, script, hai lần xuất không ghi đè, xuất khi đang chat và quyền non-admin bị từ chối.

## 2. Nhóm và thành viên — làm rõ phần vừa bổ sung

| Chức năng | Quy tắc và bằng chứng |
|---|---|
| Tạo nhóm | Tên/mô tả, công khai/riêng tư, password tùy chọn, capacity, mã 6 ký tự; người tạo là chủ |
| Sửa nhóm | Đổi tên/mô tả/loại/password/giới hạn đúng quyền; Sảnh giữ giới hạn đặc biệt |
| Mời/thêm thành viên | Chủ/admin tìm tài khoản đã đăng ký và gửi lời mời trong app; người được mời có Chấp nhận/Từ chối. Sau chấp nhận mới trở thành thành viên và join multicast khi online; đây là luồng Thêm thành viên rõ ràng, không chỉ hướng dẫn gửi mã ngoài app |
| Kiểm soát lời mời | Server kiểm quyền, nhóm mở, capacity cả lúc mời và chấp nhận, ban/locked, không mời trùng/người đã là thành viên. Có hủy lời mời, timeout và dọn khi xóa nhóm/user; giới hạn số lượng, không lộ private cho người không liên quan |
| Offline | Lưu lời mời có hạn qua restart, hiển thị khi user đăng nhập; lời mời không tự chiếm một chỗ thành viên. Nhận lời từ nhiều người đồng thời phải kiểm capacity nguyên tử |
| Tham gia bằng mã/danh sách | Giữ cả hai lối cũ; lời mời cá nhân hợp lệ là một quyền vào bổ sung, không làm lộ mật khẩu phòng. Ban/locked/closed/full vẫn phải được kiểm tra |
| Danh sách thành viên | Vai trò, online/offline, chính mình là Bạn; số người không đồng nghĩa số online |
| Kick | Chủ/admin xác nhận và lý do; rời multicast, đóng phòng, cập nhật mọi Client; khác Ban vì có thể vào lại |
| Mute/Ban/Gỡ cấm | Mute có hạn/vĩnh viễn; Ban chặn vào lại bằng mọi lối kể cả lời mời; lưu restart/expire/quyền Server |
| Chuyển chủ/Rời/Xóa | Chủ chuyển trước khi rời, admin xử lý ngoại lệ; xóa phòng hủy invite/moderation và xóa tệp đúng chính sách; Sảnh không xóa/rời |

Không tạo user mới khi “thêm thành viên”; tạo tài khoản là chức năng riêng của Đăng ký/admin. Không thêm ép người dùng vào nhóm mà không thông báo/đồng ý. Nếu cần thay đổi hành vi này phải hỏi người dùng trước.

## 3. Tên hiển thị và biệt danh — hai khái niệm khác nhau

- **Tên đăng nhập**: cố định, dùng xác thực và nhận diện; không đổi vì chỉnh biệt danh.
- **Tên hiển thị hồ sơ**: đổi toàn hệ thống qua Hồ sơ của tôi, đã có phạm vi riêng.
- **Biệt danh trong nhóm**: bổ sung riêng theo roomId + userId; người dùng đổi biệt danh của mình, chủ/admin có thể đặt/xóa biệt danh thành viên trong phạm vi quyền. Chủ không đổi cho admin; không dùng nickname để kiểm quyền.
- Biệt danh 2–30 ký tự, bỏ khoảng trắng đầu/cuối; không control/newline/ký tự ngoài phạm vi font hỗ trợ theo quy tắc hồ sơ. Xóa biệt danh trả về tên hiển thị toàn hệ thống; không cho nickname làm thay đổi accountId.
- Trong phòng: hiện nickname nếu có, tooltip tên hiển thị và @login để phân biệt người trùng tên. Cập nhật realtime tên/typing/member/message renderer; các phòng khác và nhãn Bạn không bị ảnh hưởng. Không thay tác giả hay nội dung của bản ghi lịch sử, báo cáo định danh bằng login/id ổn định.
- Persist nickname qua restart; lời mời/ban/quyền vẫn dựa id. Báo cáo thành viên có thể thêm cột biệt danh nhưng giữ đủ cột bắt buộc. Test hai phòng cùng user có nickname khác nhau, clear/restart/realtime và không giả mạo danh tính.

## 4. Sticker đẹp — dùng ảnh có sẵn, không tự vẽ

Hiện đã có **36 PNG Noto Emoji 2D 128×128**, Google và cộng tác viên, Apache2.0, nguồn chính thức được pin commit. Xem docs/NGUON_TAI_NGUYEN.md, common/StickerDanhMuc.java và resources/stickers với LICENSE/NOTICE/SHA256SUMS. Không cần người dùng tải lại bộ này nếu file còn đủ.

- Dùng ảnh nguồn, không Java2D tự vẽ sticker, không ký tự emoji phụ thuộc font. Thu phóng bicubic giữ tỷ lệ; chat112–120px, picker64px; tooltip, nhóm và tìm nhanh theo tên.
- Gửi mã sticker ngắn qua UDP, mọi Client nạp resource, mã lạ hiện placeholder không lỗi. Cache có giới hạn, ảnh đọc được và đóng gói đủ trong portable.
- Muốn thêm bộ đẹp khác: ưu tiên PNG/WebP đọc được bằng stack được duyệt, giấy phép phân phối rõ. Không lấy tùy tiện bộ của Zalo/Apple hoặc ảnh từ Google Images không nguồn. GIF/sticker động là mở rộng phải đánh giá riêng, không tự thêm thư viện.
- Có mạng thì tải từ upstream chính thức và kiểm license/hash. Không tải được hoặc cần bộ người dùng thích thì hỏi, liệt kê cụ thể tên bộ/link nguồn/tên file/định dạng/kích thước/license và nơi chép; không để người dùng tự đoán. Có thể xin ảnh mẫu để chọn phong cách trước.
- Giữ LICENSE/NOTICE và mục Giới thiệu trong menu tài khoản; test toàn bộ mã có file đọc được, hình không méo/mờ, picker và chat hiển thị thật.

## 5. Các tiện ích chat phải có trong phạm vi đã duyệt

- Văn bản UTF-8, sticker ảnh, tệp/ảnh kéo thả, thumbnail, tải/hủy/thử lại/CRC, tiến độ; trạng thái đang gửi/đã gửi/lỗi và chống trùng.
- Reply/trích dẫn, copy, thu hồi đúng quyền, tìm kiếm với phạm vi rõ, phân cách ngày, gộp tin, chỉ cuộn khi đang ở đáy, nút về tin mới và unread.
- Typing, online/offline, mute thông báo từng phòng, export lịch sử .txt, shortcut, sáng/tối, resize/HiDPI.
- Hồ sơ/ảnh đại diện/giới thiệu, đổi mật khẩu, buộc đổi mặc định/tạm, kết nối lại và RESYNC, thông báo mất mạng dễ hiểu.
- Nhóm CRUD/mời/thành viên/biệt danh/quyền và toàn bộ M5 thống kê/báo cáo ở trên.

Các tiện ích có thể đề xuất tiếp sau khảo sát: ghim tin, reaction bằng ảnh, @mention và bảng ảnh/tệp đã chia sẻ. Chưa coi những mục này là đã duyệt bắt buộc hoặc đã cài; phải trình bày lợi ích/công sức/ảnh hưởng protocol và hỏi trước khi thêm. “Mới nhất” không có nghĩa làm mọi tính năng của mọi sản phẩm; không tự thêm gọi thoại/video, AI, chia sẻ màn hình hoặc mạng xã hội vào bài UDP này.

## 6. Mốc và nghĩa vụ cập nhật

- M5: thống kê, 5 tab quản trị, kiểm duyệt, báo cáo, audit theo yêu cầu gốc.
- M6: chất lượng giao diện, bộ ảnh sticker nguồn mở, chọn/tìm sticker, theme, kết nối tiện dụng và portable smoke; không thay ảnh đã có bằng hình vẽ.
- M3c: giữ search/reply/group/day/mute notification/export, **bổ sung mời/thêm thành viên và biệt danh nhóm**. Nếu cần tách mốc phụ cho phần mới, đề xuất rõ trước khi triển khai và dừng xác nhận; không âm thầm bỏ.
- M7: test end-to-end đủ checklist, quyền/lời mời/nickname sau restart, ZIP tài nguyên và hai máy thật; ghi từng chức năng file/hàm/test/trạng thái.

Không đánh dấu “đầy đủ” chỉ vì đã viết trong prompt; phải có source, test và UI/thao tác tương ứng.
