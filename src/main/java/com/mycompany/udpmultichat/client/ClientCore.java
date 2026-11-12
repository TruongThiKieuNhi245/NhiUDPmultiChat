package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.ReliableUdp;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** API client khong Swing: UDP dieu khien, phien dang nhap va kenh multicast. */
public final class ClientCore implements AutoCloseable {
    private final Listener listener;
    private final boolean tuDongPing;
    private final Map<String, CompletableFuture<String[]>> dangCho = new ConcurrentHashMap<>();
    private final Map<Integer, String> online = new HashMap<>();
    private ReliableUdp udp;
    private volatile InetSocketAddress server;
    private volatile String token = "";
    private volatile NguoiDung toi;
    private KenhPhong kenh;
    private ScheduledExecutorService dongHo;
    private volatile long pingThanhCong;
    private long phienBanOnline = -1;
    private volatile boolean choPing;

    public ClientCore(Listener listener) { this(listener, true); }

    // Tat tuDongPing chi de SelfTest mo phong Client mat heartbeat.
    public ClientCore(Listener listener, boolean tuDongPing) {
        this.listener = listener; this.tuDongPing = tuDongPing;
    }

    // ===== KET NOI VA TAI KHOAN =====
    // DNS va tao socket can duoc goi tu luong nen cua giao dien.
    private synchronized void ketNoi(String ip, int cong) throws IOException {
        MangUtil.dungIPv4();
        InetSocketAddress dich = new InetSocketAddress(InetAddress.getByName(ip.trim()), cong);
        if (!token.isEmpty()) { throw new IllegalStateException("Hay dang xuat truoc"); }
        if (udp != null && dich.equals(server)) { return; }
        close();
        server = dich;
        udp = new ReliableUdp(new DatagramSocket(), this::nhanUnicast, listener::onLoi);
    }

    public CompletableFuture<Void> dangKy(String ip, int cong, String ten, String matKhau) {
        try {
            ketNoi(ip, cong);
            return yeuCau(Protocol.DANG_KY, ten, matKhau).thenApply(p -> null);
        } catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
    }

    public CompletableFuture<NguoiDung> dangNhap(String ip, int cong, String ten, String matKhau) {
        try { ketNoi(ip, cong); }
        catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
        return yeuCau(Protocol.DANG_NHAP, ten, matKhau).thenCompose(p -> {
            try {
                if (p.length != 12) { throw new IOException("Phan hoi dang nhap sai dinh dang"); }
                token = p[3];
                toi = NguoiDung.tuTruong(p, 4);
                kenh = new KenhPhong(p[9], p[10], Integer.parseInt(p[11]),
                        tin -> listener.onTinNhan(tin, toi != null && tin.userId.equals(toi.id)),
                        listener::onHeThong, listener::onLoi);
                return yeuCau("READY", token).thenApply(ketQua -> {
                    batHeartbeat();
                    return toi;
                });
            } catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
        }).whenComplete((v, e) -> {
            if (e != null && !token.isEmpty()) {
                yeuCau(Protocol.DANG_XUAT, token).whenComplete((boQua, loi) -> close());
            }
        });
    }

    public CompletableFuture<Void> guiTin(String noiDung) {
        if (toi == null) { return CompletableFuture.failedFuture(new IOException("Ban chua dang nhap")); }
        return yeuCau(Protocol.GUI_TIN, token, Protocol.SANH_ID, noiDung.trim()).thenApply(p -> null);
    }

    public CompletableFuture<Void> dangXuat() {
        if (token.isEmpty()) { close(); return CompletableFuture.completedFuture(null); }
        return yeuCau(Protocol.DANG_XUAT, token).handle((p, e) -> {
            close();
            if (e != null) { listener.onHeThong("Da dong Client; Server se don phien sau 15 giay."); }
            return null;
        });
    }

    public NguoiDung nguoiDung() { return toi; }

    // ===== REQUEST / RESPONSE =====
    private CompletableFuture<String[]> yeuCau(String lenh, String... thamSo) {
        ReliableUdp ketNoi = udp;
        if (ketNoi == null) { return CompletableFuture.failedFuture(new IOException("Chua ket noi Server")); }
        String ma = UUID.randomUUID().toString();
        List<String> p = new ArrayList<>(List.of(lenh, ma));
        p.addAll(Arrays.asList(thamSo));
        CompletableFuture<String[]> ketQua = new CompletableFuture<>();
        dangCho.put(ma, ketQua);
        ketQua.orTimeout(7, TimeUnit.SECONDS).whenComplete((v, e) -> dangCho.remove(ma));
        ketNoi.gui(server, Goi.ghep(p.toArray(String[]::new))).whenComplete((v, e) -> {
            if (e != null) { ketQua.completeExceptionally(e); }
        });
        return ketQua;
    }

    private void nhanUnicast(InetSocketAddress nguon, String duLieu) {
        if (!nguon.equals(server)) { return; }
        String[] p = Goi.tach(duLieu);
        if (p.length >= 4 && Protocol.TRA_LOI.equals(p[0])) {
            CompletableFuture<String[]> cho = dangCho.get(p[1]);
            if (cho == null) { return; }
            if ("OK".equals(p[2])) { cho.complete(p); }
            else { cho.completeExceptionally(new IOException(p[3])); }
        } else if (p.length == 7 && Protocol.ONLINE.equals(p[0]) && Protocol.SANH_ID.equals(p[1])) {
            nhanOnline(p);
        } else if (p.length == 2 && Protocol.HET_PHIEN.equals(p[0])) {
            matKetNoi(p[1]);
        }
    }

    // Ghep snapshot online theo phien ban; khong bi dao nguoc boi goi cu.
    private synchronized void nhanOnline(String[] p) {
        long phienBan = Long.parseLong(p[2]);
        int tong = Integer.parseInt(p[3]), viTri = Integer.parseInt(p[4]);
        if (phienBan < phienBanOnline || tong < 1 || tong > 10000 || viTri < 0 || viTri >= tong) { return; }
        if (phienBan > phienBanOnline) { online.clear(); phienBanOnline = phienBan; }
        online.put(viTri, p[6]);
        if (online.size() == tong) {
            List<String> ten = new ArrayList<>();
            for (int i = 0; i < tong; i++) { ten.add(online.get(i)); }
            listener.onDanhSachOnline(ten);
        }
    }

    // ===== HEARTBEAT =====
    private synchronized void batHeartbeat() {
        pingThanhCong = System.nanoTime();
        if (!tuDongPing) { return; }
        dongHo = Executors.newSingleThreadScheduledExecutor(viec -> {
            Thread t = new Thread(viec, "client-heartbeat"); t.setDaemon(true); return t;
        });
        dongHo.scheduleAtFixedRate(this::ping, Protocol.PING_MS, Protocol.PING_MS, TimeUnit.MILLISECONDS);
    }

    private void ping() {
        if (token.isEmpty() || choPing) { return; }
        choPing = true;
        yeuCau(Protocol.PING, token).whenComplete((p, e) -> {
            choPing = false;
            if (e == null) { pingThanhCong = System.nanoTime(); }
            else if (System.nanoTime() - pingThanhCong > Protocol.HET_PHIEN_MS * 1_000_000L) {
                matKetNoi("Khong lien lac duoc Server. Kiem tra IP, Server va Windows Firewall.");
            }
        });
    }

    private void matKetNoi(String lyDo) {
        if (toi == null) { return; }
        close();
        listener.onMatKetNoi(lyDo);
    }

    // Dong moi tai nguyen; goi tu luong nen, ke ca khi dong cua so.
    @Override public synchronized void close() {
        toi = null; token = "";
        if (dongHo != null) { dongHo.shutdownNow(); dongHo = null; }
        if (kenh != null) { kenh.close(); kenh = null; }
        if (udp != null) { udp.close(); udp = null; }
        dangCho.values().forEach(f -> f.completeExceptionally(new IOException("Ket noi da dong")));
        dangCho.clear(); online.clear(); phienBanOnline = -1; choPing = false;
    }
}
