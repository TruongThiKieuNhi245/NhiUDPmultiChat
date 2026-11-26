# Đối chiếu host và cấu hình triển khai — 26/11/2026

Đã đọc nguồn chính thức trong ngày; chưa tạo tài khoản, cấp máy, push hoặc public. Các đánh giá phù hợp dưới đây là suy luận kiến trúc từ giới hạn nhà cung cấp, chưa phải phép thử trên host. Không coi credit dùng thử là ngân sách 0 đồng lâu dài.

## Các phương án đã đối chiếu

| Phương án | Thông tin xác minh từ nguồn chính thức | Đánh giá cho source hiện tại |
|---|---|---|
| Oracle OCI Always Free A1 | 1.500 OCPU-giờ và 9.000 GB-giờ/tháng, tương đương **2 OCPU/12 GB**, 200 GB boot/block volume; outbound 10 TB/tháng. Home region có thể hết capacity; VM nhàn rỗi có thể bị thu hồi. [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) | Ứng viên ưu tiên để thử Java một instance, UDP loopback và kho file trên đĩa bền. Chưa cấp máy nên chưa chứng minh UDP/WSS/restart trên OCI. Không dùng con số 4 OCPU/24 GB từ hướng dẫn cũ. |
| Render Free Web Service | Free compute 0,1 CPU/512 MB. [Compute plans](https://render.com/docs/compute-plans). 750 giờ/tháng/workspace; idle 15 phút thì sleep, cold start khoảng một phút; đĩa tạm, Free không gắn persistent disk, Free Postgres hết hạn sau 30 ngày. [Free services](https://render.com/docs/free) | Có thể cân nhắc demo tạm, **không đạt yêu cầu giữ dữ liệu** với kho file hiện tại. Không gọi đây là phương án vận hành đầy đủ 0 đồng. Chưa thử UDP nội bộ trên Render. |
| Google Compute Engine Free Tier | Một e2-micro theo giờ tháng ở us-west1/us-central1/us-east1; 30 GB-tháng standard persistent disk, 1 GB outbound/tháng. [Free Tier](https://docs.cloud.google.com/free/docs/free-cloud-features#compute). e2-micro 1 GB RAM, 0,25 vCPU chia sẻ. [Machine types](https://docs.cloud.google.com/compute/docs/general-purpose-machines#e2_shared-core) | Có đĩa bền nhưng RAM nhỏ; cần giảm heap và đo trên đích. **Không chốt 0 đồng khi dùng IPv4 public**: bảng mạng hiện ghi IPv4 in-use standard VM 0,005 USD/giờ, mức miễn phí một giờ/tháng. [Network pricing](https://cloud.google.com/vpc/network-pricing#ipaddress). Không dùng trial credit để che phí này. |

OCI yêu cầu thông tin tài khoản/thẻ hợp lệ để xác minh, có thể giữ tạm hạn mức; thông tin phải chính xác và có sự đồng ý của chủ sở hữu. [OCI FAQ](https://www.oracle.com/cloud/free/faq/). Không tự nâng Pay As You Go, đăng ký thẻ hoặc tạo nhiều tài khoản.

Render Hobby hiện có 5 GB outbound/tháng; vượt mức có thể tính tiền khi có phương thức thanh toán, hoặc dừng dịch vụ khi không có. [Outbound bandwidth](https://render.com/docs/outbound-bandwidth). Render có URL `onrender.com`/TLS quản lý và hỗ trợ WebSocket. [Web services](https://render.com/docs/web-services), [WebSockets](https://render.com/docs/websocket). Quota upload nội bộ của ứng dụng không thay thế quota lưu lượng nhà cung cấp.

Không phương án nào đã được nghiệm thu trên đích trong lượt này. Để tiếp tục cần người dùng giao host/hostname và quyền rõ ràng. Tên miền riêng không mặc định miễn phí; chưa chọn dịch vụ DNS/domain thay người dùng. Không có thay đổi gì trong tài khoản cloud hoặc Git remote.

## Mẫu cấu hình đã chuẩn bị

- `application-proxy.properties`: backend buộc loopback, Secure cookie và `server.forward-headers-strategy=framework`. Chỉ bật sau proxy tin cậy cùng máy; không mở trực tiếp cổng backend ra Internet. [Spring Boot 3.5 proxy guidance](https://docs.spring.io/spring-boot/3.5/how-to/webserver.html#howto.webserver.use-behind-a-proxy-server).
- `deploy/Caddyfile.example`: thay `chat.example.invalid` bằng hostname đã được giao; reverse proxy cùng máy, bỏ Forwarded/port/prefix/ssl do client đưa lên. Caddy xử lý WebSocket/forwarded headers theo [tài liệu reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy). Chưa chạy binary Caddy hoặc validate mẫu trên Linux.
- `deploy/udpmultichat.service`: Java 21, user riêng, một instance, `/var/lib/udpchat` có quyền ghi; jar ở `/opt/udpchat/current.jar`; heap tối đa 768 MB cho máy đủ RAM. Mẫu này chưa được thử systemd trên host thật; không áp nguyên heap này cho VM 1 GB.
- `deploy/udpchat.env.example`: mật khẩu bootstrap riêng 12–128 ký tự cho kho mới. Giá trị mẫu để trống có chủ ý; phải cấp secret hợp lệ trước khởi tạo. Sau khi kiểm đăng nhập thì bỏ biến bootstrap. Kho đã tồn tại không bị reset bởi biến này.

Trên host được giao: giữ Java/backend/UDP ở nội bộ; chỉ proxy HTTPS/WSS ra biên. Kiểm quyền, CSRF, Origin và session vẫn do backend xử lý. Không bỏ Origin check để làm proxy chạy được. Theo dõi quota, đĩa và backup riêng; không đưa archive vào thư mục static hoặc Git.

## Phân biệt các bằng chứng mạng

Đã đối chiếu lại hai PDF trong `../tailieu`: `Chap_6_Socket_UDP_2.pdf` có 40 trang (trang 25 mô tả DatagramSocket.send/receive); `Chap_6_1_MultiCast.pdf` có 14 trang (trang 11 mô tả MulticastSocket/joinGroup/leaveGroup). Web dùng DatagramSocket **unicast loopback** thật trong JVM; browser chỉ HTTP(S)/WS(S). Desktop còn multicast nhóm 239.255.42.*, cần kiểm LAN/card/firewall khi trình diễn. Không gọi luồng loopback web là multicast.

UDP send/ACK không chứng minh đã lưu; xác nhận thành công vẫn sau receive → kiểm quyền/chống trùng → ghi → WS. QA HTTPS local dùng proxy TLS bằng aiohttp với certificate fixture tự ký và Edge `ignore_https_errors` chỉ trong test, không cài CA và không thay kiểm certificate của môi trường triển khai.

## Nghiệm thu trên đích còn phải chạy

1. DNS/certificate hợp lệ, hai phiên thiết bị độc lập đăng nhập qua HTTPS và WSS, không dùng bỏ qua lỗi TLS.
2. Tin văn bản/sticker/tệp qua UDP nội bộ thật; dừng receiver nhưng WS còn mở không được commit, retry cùng mã chỉ lưu một bản.
3. Upload/download 5 MB khớp byte, quyền phòng và PDF/CSV đúng; kiểm quota/heap/số kết nối trên đúng gói host.
4. Restart/redeploy giữ dữ liệu tại mount bền; backup/verify/restore vào thư mục mới rồi thử rollback với cả binary.
5. Điện thoại vật lý với bàn phím ảo, tablet/desktop, focus/keyboard/screen reader và zoom trình duyệt thực tế. Emulation 360 px và CSS zoom trong browser QA không thay thế toàn bộ các phép thử này.
