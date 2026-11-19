# Huong dan Agent cho UDPmultiChat

## Huong da chot

- Desktop Java 21 + Swing/FlatLaf; Maven chuan, giu groupId/artifactId. Khong chuyen web/JSP/Spring/WebSocket.
- Dieu khien va tep: UDP unicast. Chat phong: Server gui UDP multicast, Client truc tiep join/leave group. Khong gia lap multicast bang vong unicast.
- Ban giao portable Windows kem runtime, uu tien chat hai may LAN that. Khong tu push/public/mua cloud.

## Doc theo thu tu

1. `docs/BAN_GIAO_AGENT.md`: hien trang source, baseline, cac muc can xac minh.
2. `docs/PROMPT_NANG_CAP_VA_TRIEN_KHAI.md`: prompt day du dang ap dung.
3. `docs/MA_TRAN_CHUC_NANG_DESKTOP.md`: thong ke/bao cao, nhom/invite/nickname/sticker.
4. `docs/KE_HOACH_HOAN_THIEN_DESKTOP.md`: moc va tieu chi nghiem thu.
5. `docs/GIAI_THICH.md`, README, source/test va tai lieu UDP/Multicast ben thu muc tailieu.

README/GIAI_THICH co phan lich su M4: khong lay trang thai cu de ket luan source M5 chua ton tai. Yeu cau moi nhat cua nguoi dung uu tien hon tai lieu.

## Cach bat dau

- Neu nguoi dung giao thuc hien du an: kiem tra git status, doc cac file tren, chay baseline neu can, sau do hoan thien M5 dang do. Khong lap lai vong chi viet ke hoach va cho xac nhan cong nghe.
- Khong reset/checkout de mat cac thay doi M5 chua commit. Khong ghi commit `M5 xong` khi chi moi compile hoac con tieu chi bat buoc chua dat.
- Thu tu M5 -> M6 -> M3c -> M7. Moi moc compile/test/UiPreview/xem anh/docs, commit local dung ten, bao 3-5 buoc thu tay va gioi han, roi DUNG cho xac nhan truoc moc sau.
- Yeu cau chi ra soat/tai lieu khong tu dong cho phep lam xong mot moc ung dung.

## Chat luong code

- UI khong socket; client/server/common khong Swing, tru ServerMain. Noi bang Listener; mang/I-O nen, Swing cap nhat EDT.
- Comment tieng Viet khong dau, chuoi UI co dau, file UTF-8. Class ghi Vai tro/Ai goi/Goi ai, ham co comment ngan, khoi `// ===== ... =====`.
- Ten ham/bien tieng Viet khong dau; ham khoang <=40 dong, file khoang <=400 dong, <=3 tang long. Khong nen nhieu statement mot dong de lach gioi han.
- Giu MangUtil.timGiaoDienMang, preferIPv4Stack; ACK/seq/retry/RESYNC. File UDP window32, retry8 cau hinh; control ReliableUdp retry rieng.
- Theme chung, Noto PNG co LICENSE/NOTICE/hash; khong tu ve sticker. Khong them dependency ngoai FlatLaf neu chua co ly do/chap thuan.
- Khong xoa data/, client.properties, reports that, .git, source/test/anh tham chieu/giay phep khi don rac. Khong dung git clean/reset --hard.

## Lenh hien tai (PowerShell, chay tu project)

```powershell
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q compile
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q test-compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.SelfTest' '-Dexec.classpathScope=test'
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q test-compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.AdminSelfTest' '-Dexec.classpathScope=test'
& 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' -q compile exec:java '-Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview'
```

SelfTest la main, khong co JUnit/Surefire test case: `mvn test` rieng khong chung minh SelfTest da chay. Neu exec:java giu JVM lau, dung java truc tiep voi classpath test/classes + dependency; giu exit code/log. UI preview la component that nhung khong thay thu chuot/phim, hai may LAN hoac portable.
