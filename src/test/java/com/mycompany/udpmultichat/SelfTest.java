package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.ReliableUdp;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.ServerCore;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Vai tro: Kiem thu M1/M2 khong Swing, data va cong multicast rieng; cac moc sau bo sung tiep. Ai goi: SelfTest/main. Goi: ServerCore, ClientCore va cac ham kiem thu. */
public final class SelfTest {
    private static int pass, fail;
    // Khoi tao lop va noi cac thanh phan can thiet.
    private SelfTest() { }

    // ===== DIEM CHAY =====
    public static void main(String[] args) throws Exception {
        MangUtil.dungIPv4();
        Files.createDirectories(Path.of(".selftest"));
        Path prefs=Files.createTempDirectory(Path.of(".selftest"),"prefs-").resolve("client.properties");
        System.setProperty("udpmultichat.clientConfig",prefs.toAbsolutePath().toString());
        chay("UTF-8, escape va gioi han 1399 byte", SelfTest::testGoi);
        chay("ACK, gui lai, loai trung voi mat goi 20% (seed co dinh)", SelfTest::testReliable);
        chay("Mat 100%: dung sau 3 lan gui lai, khong treo", SelfTest::testHetLuot);
        testServerClient();
        PhongSelfTest.chayTatCa();
        GiaoDienSelfTest.chayTatCa();
        chay("M2.6 danh tinh truoc su kien, Client cuoi co mot Ban, laToi va quyen",DanhTinhSelfTest::kiemTra);
        chay("M2.6 goi 1000 byte va escape nam trong MTU",DanhTinhSelfTest::kiemTraGoi);
        HoSoSelfTest.chayTatCa();
        TinNhanSelfTest.chayTatCa();
        ketLuan();
    }
    // Bao loi exit code ca khi chay rieng nhom M3a.
    static void ketLuan() {
        System.out.println("KET QUA den M3b: " + pass + " PASS, " + fail + " FAIL");
        if (fail > 0) { throw new IllegalStateException("Co muc kiem thu FAIL; xem log o tren"); }
    }

    // Test goi.
    private static void testGoi() {
        String[] dauVao = { "MSG", "Xin chào | lớp mạng", "\\p\\n\nDòng 2\r", "" };
        can(Arrays.equals(dauVao, Goi.tach(Goi.ghep(dauVao))), "Escape khong khoi phuc dung");
        byte[] utf8 = Goi.thanhByte(Goi.ghep(dauVao));
        can(new String(utf8, StandardCharsets.UTF_8).equals(Goi.ghep(dauVao)), "Sai UTF-8");
        try { Goi.thanhByte("a".repeat(1400)); throw new AssertionError("Chap nhan goi qua dai"); }
        catch (IllegalArgumentException dung) { /* Can bi chan. */ }
    }

    // Mat goi o chieu DATA va ACK; dem so lan xu ly nghiep vu de phat hien goi trung.
    private static void testReliable() throws Exception {
        AtomicInteger xuLy = new AtomicInteger();
        try (ReliableUdp nhan = new ReliableUdp(new DatagramSocket(0), (n, s) -> xuLy.incrementAndGet(),
                SelfTest::loiNen, 0.20, 20261113L);
             ReliableUdp gui = new ReliableUdp(new DatagramSocket(0), (n, s) -> { },
                     SelfTest::loiNen, 0.20, 20261112L)) {
            InetSocketAddress dich = new InetSocketAddress("127.0.0.1", nhan.cong());
            for (int i = 0; i < 16; i++) { cho(gui.gui(dich, Goi.ghep("TEST", Integer.toString(i)))); }
            doi(() -> xuLy.get() == 16, 2000);
            can(xuLy.get() == 16, "Nghiep vu chay thua hoac thieu");
            can(gui.soLanGuiLai() > 0, "Chua kich hoat gui lai");
            can(nhan.soGoiTrung() > 0, "Chua kich hoat loai trung do mat ACK");
            System.out.println("  gui lai=" + gui.soLanGuiLai() + ", goi trung=" + nhan.soGoiTrung());
        }
    }

    // Test het luot.
    private static void testHetLuot() throws Exception {
        try (ReliableUdp nhan = new ReliableUdp(new DatagramSocket(0), (n, s) -> { }, SelfTest::loiNen);
             ReliableUdp gui = new ReliableUdp(new DatagramSocket(0), (n, s) -> { }, SelfTest::loiNen, 1, 7L)) {
            biChan(gui.gui(new InetSocketAddress("127.0.0.1", nhan.cong()), "TEST"));
            can(gui.soLanGuiLai() == 3, "So lan gui lai khong bang 3");
        }
    }

    // ===== SERVER THAT VA BA CLIENT KHONG GIAO DIEN =====
    // Moi ca test dung cung ba Client, nhung moi ham chi kiem mot nhom hanh vi.
    private static void testServerClient() throws Exception {
        try (PhienThu t=new PhienThu()) {
            t.batDau();
            chay("Dang ky, chan trung ten va mat khau khong hop le",t::dangKy);
            chay("Dang nhap, ADMIN, sai mat khau va chan dang nhap hai noi",t::dangNhap);
            chay("Hai Client nhan tin tieng Viet qua multicast (ke ca tin cua minh)",t::multicast);
            chay("Bo qua goi multicast co roomId khac",t::locRoomId);
            chay("Heartbeat: 2 Client van online, Client khong PING bi loai sau 15s",t::heartbeat);
            chay("Dang xuat, danh sach online va dang nhap lai",t::dangXuat);
            chay("Salt/hash va du lieu tai khoan con sau khoi dong lai Server",t::luuTaiKhoan);
            chay("Server dung: Client tu bao mat ket noi, khong treo",t::serverDung);
        }
    }

    /** Vai tro: du lieu chung M1. Ai goi: testServerClient. Goi: ServerCore va ba ClientCore. */
    private static final class PhienThu implements AutoCloseable {
        final AtomicInteger soOnline=new AtomicInteger();
        final BanNghe ngheA=new BanNghe(),ngheB=new BanNghe(),ngheC=new BanNghe();
        final ClientCore a=new ClientCore(ngheA),b=new ClientCore(ngheB),c=new ClientCore(ngheC,false);
        final Path data;
        final ServerCore server;
        int cong;
        // Tao thu muc rieng, cac Client chua dang nhap.
        PhienThu() throws Exception {
            data=Files.createTempDirectory(Path.of(".selftest"),"m1-");
            Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nmulticast.portBase=19000\n",StandardCharsets.UTF_8);
            System.out.println("Data kiem thu: "+data.toAbsolutePath());
            server=new ServerCore(data,new ServerCore.Listener() {
                // Nhan log qua Listener, chuyen ve luong phu hop.
                public void onLog(String s) { System.out.println("  SERVER: "+s); }
                // Nhan snapshot thong ke tu ServerCore.
                public void onThongKe(int tong,int online,long tin) { soOnline.set(online); }
            });
        }
        // Chon cong tam, khong tranh cong Server demo.
        void batDau() throws Exception { server.batDau(0); cong=server.cong(); }
        // Kiem tra dangKy, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void dangKy() throws Exception {
            cho(a.dangKy("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
            cho(c.dangKy("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
            biChan(b.dangKy("127.0.0.1", cong, "SINHVIEN_A", "abcdef123"));
            biChan(b.dangKy("127.0.0.1", cong, "khong_hople", "123"));
        }
        // Kiem tra dangNhap, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void dangNhap() throws Exception {
            biChan(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "saimatkhau"));
            cho(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
            biChan(c.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
            try { cho(b.dangNhap("127.0.0.1",cong,"khong_ton_tai","saimatkhau")); throw new AssertionError("Login sai van duoc"); }
            catch (java.util.concurrent.ExecutionException e) { can(ClientCore.loiDeHieu(e).equals("Tên đăng nhập hoặc mật khẩu không đúng."),"Login tiet lo tai khoan"); }
            can(cho(b.dangNhap("127.0.0.1", cong, "admin", "adminTest123")).vaiTro == VaiTro.ADMIN, "Sai quyen admin");
            cho(c.dangNhap("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
            doi(() -> ngheA.online.size() == 3 && ngheB.online.size() == 3, 3000);
        }
        // Kiem tra multicast, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void multicast() throws Exception {
            String noiDung = "Xin chào | lớp mạng \\ UDP\nDòng thứ hai";
            cho(a.guiTin(noiDung));
            TinNhan tinA = ngheA.tin.poll(4, TimeUnit.SECONDS);
            TinNhan tinB = ngheB.tin.poll(4, TimeUnit.SECONDS);
            can(tinA != null && tinB != null, "Khong nhan multicast: kiem tra card mang va Firewall");
            can(noiDung.equals(tinB.noiDung) && tinA.msgId == tinB.msgId, "Tin bi sai noi dung hoac msgId");
            can(Protocol.SANH_ID.equals(tinB.roomId), "Thieu roomId");
            can(tinB.thoiGian > 0 && tinB.userId.equals(a.nguoiDung().id), "Sai danh tinh/thoi gian Server");
        }
        // Kiem tra locRoomId, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void locRoomId() throws Exception {
            try (MulticastSocket gia = new MulticastSocket()) {
                gia.setNetworkInterface(MangUtil.timGiaoDienMang()); gia.setTimeToLive(1);
                byte[] dataSai = Goi.thanhByte(new TinNhan(99999, "phong_khac", "gia", "Gia", "KHONG_HIEN", 1).dongGoi());
                var sanh=a.phongDaVao().stream().filter(r -> r.id.equals(Protocol.SANH_ID)).findFirst().orElseThrow();
                gia.send(new DatagramPacket(dataSai, dataSai.length, InetAddress.getByName(sanh.nhom), sanh.cong));
            }
            can(ngheB.tin.poll(600, TimeUnit.MILLISECONDS) == null, "Nhan lan tin phong khac");
        }
        // Kiem tra heartbeat, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void heartbeat() throws Exception {
            can(ngheC.matKetNoi.await(18, TimeUnit.SECONDS), "Khong het phien Client khong PING");
            doi(() -> soOnline.get() == 2 && ngheA.online.size() == 2, 3000);
            can(a.nguoiDung() != null && b.nguoiDung() != null, "Heartbeat khong giu duoc phien");
            cho(c.dangNhap("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
            can(c.nguoiDung() != null, "Khong dang nhap lai sau het phien");
        }
        // Kiem tra dangXuat, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void dangXuat() throws Exception {
            cho(a.dangXuat());
            doi(() -> ngheB.online.size() == 2, 3000);
            cho(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
            doi(() -> ngheB.online.size() == 3, 3000);
        }
        // Kiem tra luuTaiKhoan, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void luuTaiKhoan() throws Exception {
            cho(a.dangXuat()); cho(b.dangXuat()); cho(c.dangXuat());
            String tep = Files.readString(data.resolve("users.csv"), StandardCharsets.UTF_8);
            String config = Files.readString(data.resolve("config.properties"), StandardCharsets.UTF_8);
            can(!tep.contains("abcdef123") && !tep.contains("adminTest123"), "Luu mat khau tho trong users.csv");
            can(!config.contains("adminTest123") && !config.contains("admin.password="), "Luu mat khau tho trong config");
            server.close(); server.batDau(0);
            cho(a.dangNhap("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
            biChan(b.dangKy("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
            cho(a.dangXuat());
        }
        // Kiem tra serverDung, giu chi tiet PASS/FAIL tai ham dieu phoi.
        void serverDung() throws Exception {
            cho(a.dangNhap("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
            server.close();
            can(ngheA.matKetNoi.await(24, TimeUnit.SECONDS), "Client khong phat hien Server dung");
            can(!a.sanSang(), "Client khong khoa gui khi mat Server");
        }
        // Don het socket va luong ke ca khi test nem loi.
        public void close() { a.close(); b.close(); c.close(); server.close(); }
    }

    // ===== TIEN ICH KIEM THU =====
    static <T> T cho(CompletableFuture<T> viec) throws Exception { return viec.get(9, TimeUnit.SECONDS); }

    // Bi chan.
    static void biChan(CompletableFuture<?> viec) throws Exception {
        try { cho(viec); throw new AssertionError("Yeu cau dang le phai bi tu choi"); }
        catch (java.util.concurrent.ExecutionException dung) { /* Server/transport da tu choi. */ }
    }

    // Doi.
    static void doi(BooleanSupplier dieuKien, long toiDaMs) throws Exception {
        long hetHan = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(toiDaMs);
        while (!dieuKien.getAsBoolean() && System.nanoTime() < hetHan) { Thread.sleep(25); }
        can(dieuKien.getAsBoolean(), "Het thoi gian cho dieu kien");
    }

    // Can.
    static void can(boolean dieuKien, String lyDo) { if (!dieuKien) { throw new AssertionError(lyDo); } }
    // Loi nen.
    private static void loiNen(String loi) { System.err.println("  UDP: " + loi); }

    // Chay.
    static void chay(String ten, ViecTest viec) {
        try { viec.chay(); pass++; System.out.println("PASS " + ten); }
        catch (Exception | AssertionError e) { fail++; System.err.println("FAIL " + ten + ": " + e); }
    }

    @FunctionalInterface interface ViecTest { void chay() throws Exception; }

    /** Thu su kien Client vao hang doi an toan giua cac luong. */
    private static final class BanNghe implements Listener {
        final BlockingQueue<TinNhan> tin = new LinkedBlockingQueue<>();
        final CountDownLatch matKetNoi = new CountDownLatch(1);
        volatile List<String> online = List.of();
        // Nhan tin da duoc kiem tra danh tinh va phong.
        @Override public void onTinNhan(TinNhan t, boolean laToi) { tin.add(t); }
        // Nhan thong bao he thong qua Listener.
        @Override public void onHeThong(String s) { }
        // Nhan danh sach online theo Listener.
        @Override public void onDanhSachOnline(List<String> ds) { online = List.copyOf(ds); }
        // Nhan loi de hien thi hoac ghi nhan.
        @Override public void onLoi(String s) { loiNen(s); }
        // Nhan trang thai phien da ngat.
        @Override public void onMatKetNoi(String s) { matKetNoi.countDown(); }
        // On ket noi.
        @Override public void onKetNoi(boolean ok,String s) { if (!ok) { matKetNoi.countDown(); } }
    }
}
