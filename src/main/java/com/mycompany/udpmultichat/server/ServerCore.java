package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.ReliableUdp;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Server dieu phoi UDP; xac thuc truoc khi phat tin vao multicast Sanh chung. */
public final class ServerCore implements AutoCloseable {
    /** Giao dien hoac SelfTest nhan log/thong ke, khong phu thuoc Swing. */
    public interface Listener {
        void onLog(String noiDung);
        void onThongKe(int tongUser, int online, long soTin);
    }

    private final Path thuMuc;
    private final Listener listener;
    private QuanLyNguoiDung nguoiDung;
    private ReliableUdp udp;
    private MulticastSocket multicast;
    private InetAddress nhom;
    private ScheduledExecutorService dongHo;
    private long soTin, phienBanOnline;
    private volatile boolean dangChay;

    public ServerCore(Path thuMuc, Listener listener) {
        this.thuMuc = thuMuc;
        this.listener = listener;
    }

    // ===== BAT DAU / DUNG =====
    public synchronized void batDau(int cong) throws IOException {
        if (dangChay) { throw new IllegalStateException("Server dang chay"); }
        MangUtil.dungIPv4();
        try {
            NetworkInterface giaoDien = MangUtil.timGiaoDienMang();
            nguoiDung = new QuanLyNguoiDung(new KhoLuuTru(thuMuc, listener::onLog));
            nhom = InetAddress.getByName(Protocol.SANH_NHOM);
            multicast = new MulticastSocket();
            multicast.setNetworkInterface(giaoDien);
            multicast.setTimeToLive(1);
            DatagramSocket socket = new DatagramSocket(null);
            try { socket.bind(new InetSocketAddress(cong)); }
            catch (IOException e) { socket.close(); throw e; }
            udp = new ReliableUdp(socket, this::xuLyGoi, listener::onLog);
            dongHo = Executors.newSingleThreadScheduledExecutor(viec -> {
                Thread t = new Thread(viec, "server-heartbeat"); t.setDaemon(true); return t;
            });
            soTin = 0;
            dangChay = true;
            dongHo.scheduleAtFixedRate(this::quetPhien, 1, 1, TimeUnit.SECONDS);
            listener.onLog("Server UDP cong " + udp.cong() + "; card " + giaoDien.getDisplayName());
            listener.onLog("Sanh chung: " + Protocol.SANH_NHOM + ":" + Protocol.SANH_CONG);
            baoThongKe();
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    public int cong() { return udp == null ? -1 : udp.cong(); }
    public boolean dangChay() { return dangChay; }

    @Override public synchronized void close() {
        dangChay = false;
        if (dongHo != null) { dongHo.shutdownNow(); }
        if (udp != null) { udp.close(); }
        if (multicast != null) { multicast.close(); }
        listener.onLog("Server da dung; cac Client se phat hien qua heartbeat.");
        listener.onThongKe(nguoiDung == null ? 0 : nguoiDung.tongNguoiDung(), 0, soTin);
    }

    // ===== PHAN LENH UNICAST =====
    // Cung mot khoa bao ve trang thai voi luong quet heartbeat.
    private synchronized void xuLyGoi(InetSocketAddress nguon, String duLieu) {
        if (!dangChay) { return; }
        String maYeuCau = "";
        try {
            String[] p = Goi.tach(duLieu);
            if (p.length < 2 || p[1].length() > 64) { return; }
            maYeuCau = p[1];
            switch (p[0]) {
                case Protocol.DANG_KY:
                    duTruong(p, 4);
                    nguoiDung.dangKy(p[2], p[3]);
                    traLoi(nguon, maYeuCau, true, "Dang ky thanh cong. Hay dang nhap.");
                    listener.onLog("Dang ky: " + p[2]);
                    baoThongKe();
                    break;
                case Protocol.DANG_NHAP:
                    duTruong(p, 4);
                    quetPhien();
                    Phien phien = nguoiDung.dangNhap(p[2], p[3], nguon);
                    List<String> thongTin = new ArrayList<>();
                    thongTin.add(phien.token);
                    thongTin.addAll(List.of(phien.nguoi.cacTruong()));
                    thongTin.addAll(List.of(Protocol.SANH_ID, Protocol.SANH_NHOM,
                            Integer.toString(Protocol.SANH_CONG)));
                    traLoi(nguon, maYeuCau, true, thongTin.toArray(String[]::new));
                    listener.onLog("Dang nhap: " + phien.nguoi.tenDangNhap);
                    baoThongKe();
                    break;
                default: xuLyDaDangNhap(nguon, p);
            }
        } catch (IllegalArgumentException e) {
            traLoi(nguon, maYeuCau, false, e.getMessage());
        } catch (IOException e) {
            listener.onLog("Loi luu/gui: " + e.getMessage());
            traLoi(nguon, maYeuCau, false, "Server khong luu/gui duoc du lieu. Hay thu lai.");
        }
    }

    private void xuLyDaDangNhap(InetSocketAddress nguon, String[] p) throws IOException {
        if (p.length < 3) { throw new IllegalArgumentException("Thieu token phien"); }
        Phien phien = nguoiDung.kiemTra(p[2], nguon);
        switch (p[0]) {
            case "READY":
                duTruong(p, 3);
                if (!phien.sanSang) {
                    phien.sanSang = true;
                    thongBaoPhong(phien.nguoi.tenHienThi + " da vao Sanh chung.");
                    capNhatOnline();
                    listener.onLog(phien.nguoi.tenDangNhap + " vao Sanh chung");
                }
                traLoi(nguon, p[1], true, "Da vao Sanh chung");
                break;
            case Protocol.PING:
                duTruong(p, 3);
                nguoiDung.ping(phien);
                traLoi(nguon, p[1], true, Long.toString(soTin));
                break;
            case Protocol.DANG_XUAT:
                duTruong(p, 3);
                nguoiDung.dangXuat(phien);
                traLoi(nguon, p[1], true, "Da dang xuat");
                roiSanh(phien, "da roi Sanh chung.");
                capNhatOnline();
                break;
            case Protocol.GUI_TIN:
                duTruong(p, 5);
                guiTin(phien, p[3], p[4]);
                traLoi(nguon, p[1], true, Long.toString(soTin));
                break;
            default: throw new IllegalArgumentException("Lenh chua ho tro o M1");
        }
    }

    // ===== CHAT MULTICAST =====
    private void guiTin(Phien phien, String roomId, String noiDung) throws IOException {
        if (!phien.sanSang || !Protocol.SANH_ID.equals(roomId)) {
            throw new IllegalArgumentException("Ban chua vao phong nay");
        }
        if (noiDung.isBlank() || noiDung.getBytes(StandardCharsets.UTF_8).length > 600) {
            throw new IllegalArgumentException("Tin can 1-600 byte UTF-8 o M1");
        }
        TinNhan tin = new TinNhan(soTin + 1, roomId, phien.nguoi.id,
                phien.nguoi.tenHienThi, noiDung, System.currentTimeMillis());
        phatMulticast(tin.dongGoi());
        soTin++;
        baoThongKe();
    }

    private void thongBaoPhong(String noiDung) {
        try { phatMulticast(Goi.ghep(Protocol.HE_THONG, Protocol.SANH_ID, noiDung)); }
        catch (IOException e) { listener.onLog("Loi phat thong bao: " + e.getMessage()); }
    }

    // Chi Server phat tin hop le den nhom; Client SEND bang unicast.
    private void phatMulticast(String duLieu) throws IOException {
        byte[] data = Goi.thanhByte(duLieu);
        multicast.send(new DatagramPacket(data, data.length, nhom, Protocol.SANH_CONG));
    }

    // ===== DANH SACH ONLINE VA HEARTBEAT =====
    private void capNhatOnline() {
        List<Phien> danhSach = nguoiDung.danhSach().stream().filter(p -> p.sanSang).toList();
        String phienBan = Long.toString(++phienBanOnline);
        // Gui tung muc de danh sach dai khong vuot kich thuoc mot datagram.
        for (Phien dich : danhSach) {
            for (int i = 0; i < danhSach.size(); i++) {
                Phien thanhVien = danhSach.get(i);
                guiSuKien(dich, Goi.ghep(Protocol.ONLINE, Protocol.SANH_ID, phienBan,
                        Integer.toString(danhSach.size()), Integer.toString(i),
                        thanhVien.nguoi.id, thanhVien.nguoi.tenHienThi));
            }
        }
        baoThongKe();
    }

    private synchronized void quetPhien() {
        if (!dangChay) { return; }
        List<Phien> hetHan = nguoiDung.xoaHetHan();
        for (Phien phien : hetHan) {
            guiSuKien(phien, Goi.ghep(Protocol.HET_PHIEN, "Khong co heartbeat qua 15 giay. Hay dang nhap lai."));
            roiSanh(phien, "da mat ket noi (het heartbeat).");
        }
        if (!hetHan.isEmpty()) { capNhatOnline(); }
    }

    private void roiSanh(Phien phien, String lyDo) {
        listener.onLog(phien.nguoi.tenDangNhap + " " + lyDo);
        if (phien.sanSang) { thongBaoPhong(phien.nguoi.tenHienThi + " " + lyDo); }
    }

    private void baoThongKe() {
        listener.onThongKe(nguoiDung.tongNguoiDung(), nguoiDung.danhSach().size(), soTin);
    }

    private void guiSuKien(Phien phien, String duLieu) {
        udp.gui(phien.diaChi, duLieu).exceptionally(e -> {
            if (dangChay) { listener.onLog("Khong gui duoc cap nhat cho " + phien.nguoi.tenDangNhap); }
            return null;
        });
    }

    private void traLoi(InetSocketAddress dich, String ma, boolean thanhCong, String... noiDung) {
        if (ma.isEmpty() || udp == null) { return; }
        List<String> p = new ArrayList<>(List.of(Protocol.TRA_LOI, ma, thanhCong ? "OK" : "ERR"));
        p.addAll(List.of(noiDung));
        udp.gui(dich, Goi.ghep(p.toArray(String[]::new))).exceptionally(e -> {
            if (dangChay) { listener.onLog("Khong nhan ACK phan hoi tu " + dich); }
            return null;
        });
    }

    private static void duTruong(String[] p, int soTruong) {
        if (p.length != soTruong) { throw new IllegalArgumentException("So truong cua goi khong hop le"); }
    }
}
