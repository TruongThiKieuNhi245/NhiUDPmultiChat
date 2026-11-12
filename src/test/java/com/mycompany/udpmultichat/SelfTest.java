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

/** Kiem thu M1 khong Swing, dung data rieng; M2-M5 se bo sung phong, file va bao cao. */
public final class SelfTest {
    private static int pass, fail;
    private SelfTest() { }

    // ===== DIEM CHAY =====
    public static void main(String[] args) throws Exception {
        MangUtil.dungIPv4();
        chay("UTF-8, escape va gioi han 1399 byte", SelfTest::testGoi);
        chay("ACK, gui lai, loai trung voi mat goi 20% (seed co dinh)", SelfTest::testReliable);
        chay("Mat 100%: dung sau 3 lan gui lai, khong treo", SelfTest::testHetLuot);
        testServerClient();
        System.out.println("KET QUA M1: " + pass + " PASS, " + fail + " FAIL");
        if (fail > 0) { throw new IllegalStateException("Co muc kiem thu FAIL; xem log o tren"); }
    }

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

    private static void testHetLuot() throws Exception {
        try (ReliableUdp nhan = new ReliableUdp(new DatagramSocket(0), (n, s) -> { }, SelfTest::loiNen);
             ReliableUdp gui = new ReliableUdp(new DatagramSocket(0), (n, s) -> { }, SelfTest::loiNen, 1, 7L)) {
            biChan(gui.gui(new InetSocketAddress("127.0.0.1", nhan.cong()), "TEST"));
            can(gui.soLanGuiLai() == 3, "So lan gui lai khong bang 3");
        }
    }

    // ===== SERVER THAT VA BA CLIENT KHONG GIAO DIEN =====
    private static void testServerClient() throws Exception {
        Path goc = Path.of(".selftest"); Files.createDirectories(goc);
        Path data = Files.createTempDirectory(goc, "m1-");
        System.out.println("Data kiem thu: " + data.toAbsolutePath());
        AtomicInteger soOnline = new AtomicInteger();
        ServerCore.Listener theoDoi = new ServerCore.Listener() {
            @Override public void onLog(String s) { System.out.println("  SERVER: " + s); }
            @Override public void onThongKe(int tong, int online, long tin) { soOnline.set(online); }
        };
        BanNghe ngheA = new BanNghe(), ngheB = new BanNghe(), ngheC = new BanNghe();
        ServerCore server = new ServerCore(data, theoDoi);
        try (ClientCore a = new ClientCore(ngheA);
             ClientCore b = new ClientCore(ngheB);
             ClientCore c = new ClientCore(ngheC, false)) {
            server.batDau(0);
            int cong = server.cong();
            chay("Dang ky, chan trung ten va mat khau khong hop le", () -> {
                cho(a.dangKy("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
                cho(c.dangKy("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
                biChan(b.dangKy("127.0.0.1", cong, "SINHVIEN_A", "abcdef123"));
                biChan(b.dangKy("127.0.0.1", cong, "khong_hople", "123"));
            });
            chay("Dang nhap, ADMIN, sai mat khau va chan dang nhap hai noi", () -> {
                biChan(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "saimatkhau"));
                cho(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
                biChan(c.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
                can(cho(b.dangNhap("127.0.0.1", cong, "admin", "admin123")).vaiTro == VaiTro.ADMIN, "Sai quyen admin");
                cho(c.dangNhap("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
                doi(() -> ngheA.online.size() == 3 && ngheB.online.size() == 3, 3000);
            });
            chay("Hai Client nhan tin tieng Viet qua multicast (ke ca tin cua minh)", () -> {
                String noiDung = "Xin chào | lớp mạng \\ UDP\nDòng thứ hai";
                cho(a.guiTin(noiDung));
                TinNhan tinA = ngheA.tin.poll(4, TimeUnit.SECONDS);
                TinNhan tinB = ngheB.tin.poll(4, TimeUnit.SECONDS);
                can(tinA != null && tinB != null, "Khong nhan multicast: kiem tra card mang va Firewall");
                can(noiDung.equals(tinB.noiDung) && tinA.msgId == tinB.msgId, "Tin bi sai noi dung hoac msgId");
                can(Protocol.SANH_ID.equals(tinB.roomId), "Thieu roomId");
                can(tinB.thoiGian > 0 && tinB.userId.equals(a.nguoiDung().id), "Sai danh tinh/thoi gian Server");
            });
            chay("Bo qua goi multicast co roomId khac", () -> {
                try (MulticastSocket gia = new MulticastSocket()) {
                    gia.setNetworkInterface(MangUtil.timGiaoDienMang()); gia.setTimeToLive(1);
                    byte[] dataSai = Goi.thanhByte(new TinNhan(99999, "phong_khac", "gia", "Gia", "KHONG_HIEN", 1).dongGoi());
                    gia.send(new DatagramPacket(dataSai, dataSai.length, InetAddress.getByName(Protocol.SANH_NHOM), Protocol.SANH_CONG));
                }
                can(ngheB.tin.poll(600, TimeUnit.MILLISECONDS) == null, "Nhan lan tin phong khac");
            });
            chay("Heartbeat: 2 Client van online, Client khong PING bi loai sau 15s", () -> {
                can(ngheC.matKetNoi.await(18, TimeUnit.SECONDS), "Khong het phien Client khong PING");
                doi(() -> soOnline.get() == 2 && ngheA.online.size() == 2, 3000);
                can(a.nguoiDung() != null && b.nguoiDung() != null, "Heartbeat khong giu duoc phien");
                cho(c.dangNhap("127.0.0.1", cong, "sinhvien_c", "abcdef456"));
                can(c.nguoiDung() != null, "Khong dang nhap lai sau het phien");
            });
            chay("Dang xuat, danh sach online va dang nhap lai", () -> {
                cho(a.dangXuat());
                doi(() -> ngheB.online.size() == 2, 3000);
                cho(a.dangNhap("127.0.0.1", cong, "sinhvien_a", "abcdef123"));
                doi(() -> ngheB.online.size() == 3, 3000);
            });
            chay("Salt/hash va du lieu tai khoan con sau khoi dong lai Server", () -> {
                cho(a.dangXuat()); cho(b.dangXuat()); cho(c.dangXuat());
                String tep = Files.readString(data.resolve("users.csv"), StandardCharsets.UTF_8);
                String config = Files.readString(data.resolve("config.properties"), StandardCharsets.UTF_8);
                can(!tep.contains("abcdef123") && !tep.contains("admin123"), "Luu mat khau tho trong users.csv");
                can(!config.contains("admin123") && !config.contains("admin.password="), "Luu mat khau tho trong config");
                server.close(); server.batDau(0);
                cho(a.dangNhap("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
                biChan(b.dangKy("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
                cho(a.dangXuat());
            });
            chay("Server dung: Client tu bao mat ket noi, khong treo", () -> {
                cho(a.dangNhap("127.0.0.1", server.cong(), "sinhvien_a", "abcdef123"));
                server.close();
                can(ngheA.matKetNoi.await(24, TimeUnit.SECONDS), "Client khong phat hien Server dung");
                can(a.nguoiDung() == null, "Client chua don phien sau mat Server");
            });
        } finally { server.close(); }
    }

    // ===== TIEN ICH KIEM THU =====
    private static <T> T cho(CompletableFuture<T> viec) throws Exception { return viec.get(9, TimeUnit.SECONDS); }

    private static void biChan(CompletableFuture<?> viec) throws Exception {
        try { cho(viec); throw new AssertionError("Yeu cau dang le phai bi tu choi"); }
        catch (java.util.concurrent.ExecutionException dung) { /* Server/transport da tu choi. */ }
    }

    private static void doi(BooleanSupplier dieuKien, long toiDaMs) throws Exception {
        long hetHan = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(toiDaMs);
        while (!dieuKien.getAsBoolean() && System.nanoTime() < hetHan) { Thread.sleep(25); }
        can(dieuKien.getAsBoolean(), "Het thoi gian cho dieu kien");
    }

    private static void can(boolean dieuKien, String lyDo) { if (!dieuKien) { throw new AssertionError(lyDo); } }
    private static void loiNen(String loi) { System.err.println("  UDP: " + loi); }

    private static void chay(String ten, ViecTest viec) {
        try { viec.chay(); pass++; System.out.println("PASS " + ten); }
        catch (Exception | AssertionError e) { fail++; System.err.println("FAIL " + ten + ": " + e); }
    }

    @FunctionalInterface private interface ViecTest { void chay() throws Exception; }

    /** Thu su kien Client vao hang doi an toan giua cac luong. */
    private static final class BanNghe implements Listener {
        final BlockingQueue<TinNhan> tin = new LinkedBlockingQueue<>();
        final CountDownLatch matKetNoi = new CountDownLatch(1);
        volatile List<String> online = List.of();
        @Override public void onTinNhan(TinNhan t, boolean laToi) { tin.add(t); }
        @Override public void onHeThong(String s) { }
        @Override public void onDanhSachOnline(List<String> ds) { online = List.copyOf(ds); }
        @Override public void onLoi(String s) { loiNen(s); }
        @Override public void onMatKetNoi(String s) { matKetNoi.countDown(); }
    }
}
