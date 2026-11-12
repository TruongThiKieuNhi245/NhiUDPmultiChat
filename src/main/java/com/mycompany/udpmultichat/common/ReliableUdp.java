package com.mycompany.udpmultichat.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** UDP tin cay cho dieu khien: ACK, gui lai 3 lan, loai trung theo endpoint + seq. */
public final class ReliableUdp implements AutoCloseable {
    private final DatagramSocket socket;
    private final BiConsumer<InetSocketAddress, String> xuLy;
    private final Consumer<String> baoLoi;
    private final Map<String, ChoAck> dangCho = new ConcurrentHashMap<>();
    private final Map<String, Long> daNhan = new ConcurrentHashMap<>();
    private final ScheduledExecutorService dongHo;
    private final ThreadPoolExecutor xuLyNen;
    private final String phien = UUID.randomUUID().toString();
    private final AtomicLong seq = new AtomicLong();
    private final AtomicLong soGuiLai = new AtomicLong(), soTrung = new AtomicLong();
    private final Random ngauNhien;
    private final double tyLeMat;
    private volatile boolean dangChay = true;

    public ReliableUdp(DatagramSocket socket, BiConsumer<InetSocketAddress, String> xuLy,
                       Consumer<String> baoLoi) {
        this(socket, xuLy, baoLoi, 0, 0);
    }

    // Ty le mat va seed chi dung cho kiem thu; mac dinh khong bo goi.
    public ReliableUdp(DatagramSocket socket, BiConsumer<InetSocketAddress, String> xuLy,
                       Consumer<String> baoLoi, double tyLeMat, long seed) {
        if (tyLeMat < 0 || tyLeMat > 1) { throw new IllegalArgumentException("Tỷ lệ mất sai"); }
        this.socket = socket;
        this.xuLy = xuLy;
        this.baoLoi = baoLoi;
        this.tyLeMat = tyLeMat;
        this.ngauNhien = new Random(seed);
        dongHo = Executors.newSingleThreadScheduledExecutor(t -> luongNen(t, "udp-dong-ho"));
        xuLyNen = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024), t -> luongNen(t, "udp-xu-ly"));
        luongNen(this::vongNhan, "udp-nhan").start();
        dongHo.scheduleAtFixedRate(() -> daNhan.entrySet().removeIf(
                e -> System.nanoTime() - e.getValue() > TimeUnit.SECONDS.toNanos(60)),
                60, 60, TimeUnit.SECONDS);
    }

    // ===== GUI KHONG CHAN LUONG GOI =====
    // Hoan thanh khi co ACK; nghiep vu can goi RESPONSE rieng.
    public CompletableFuture<Void> gui(InetSocketAddress dich, String noiDung) {
        if (!dangChay) { return CompletableFuture.failedFuture(new IOException("UDP đã đóng")); }
        if (dangCho.size() >= 512) {
            return CompletableFuture.failedFuture(new IOException("Quá nhiều yêu cầu đang chờ"));
        }
        String ma = phien + "-" + seq.incrementAndGet();
        try {
            byte[] duLieu = Goi.thanhByte(Goi.ghep("R", ma, noiDung));
            ChoAck cho = new ChoAck(dich, duLieu);
            dangCho.put(ma, cho);
            cho.ketQua.whenComplete((v, e) -> dangCho.remove(ma, cho));
            dongHo.execute(() -> thuGui(ma, cho, 0));
            return cho.ketQua;
        } catch (IllegalArgumentException | RejectedExecutionException e) {
            ChoAck cho = dangCho.remove(ma);
            if (cho != null) { cho.ketQua.completeExceptionally(e); }
            return CompletableFuture.failedFuture(e);
        }
    }

    // Gui lan dau va toi da ba lan gui lai, cach nhau 500 ms.
    private void thuGui(String ma, ChoAck cho, int lan) {
        if (cho.ketQua.isDone()) { return; }
        if (!dangChay || lan > Protocol.GUI_LAI_TOI_DA) {
            cho.ketQua.completeExceptionally(new IOException("Không nhận được ACK sau 4 lần gửi"));
            return;
        }
        try {
            if (lan > 0) { soGuiLai.incrementAndGet(); }
            guiRaw(cho.duLieu, cho.dich);
            dongHo.schedule(() -> thuGui(ma, cho, lan + 1), Protocol.CHO_ACK_MS, TimeUnit.MILLISECONDS);
        } catch (IOException | RejectedExecutionException e) {
            cho.ketQua.completeExceptionally(e);
        }
    }

    // ===== NHAN VA ACK =====
    private void vongNhan() {
        while (dangChay) {
            try {
                byte[] dem = new byte[Protocol.GOI_TOI_DA + 1];
                DatagramPacket goi = new DatagramPacket(dem, dem.length);
                socket.receive(goi);
                if (goi.getLength() > Protocol.GOI_TOI_DA) { continue; }
                InetSocketAddress nguon = (InetSocketAddress) goi.getSocketAddress();
                String[] truong = Goi.tach(new String(goi.getData(), 0, goi.getLength(), StandardCharsets.UTF_8));
                if (truong.length == 2 && "A".equals(truong[0])) {
                    ChoAck cho = dangCho.get(truong[1]);
                    if (cho != null && cho.dich.equals(nguon)) { cho.ketQua.complete(null); }
                } else if (truong.length == 3 && "R".equals(truong[0]) && truong[1].length() <= 80) {
                    nhanTinCay(nguon, truong[1], truong[2]);
                }
            } catch (IOException | RuntimeException e) {
                if (dangChay) { baoLoi.accept("Lỗi UDP: " + e.getMessage()); }
            }
        }
    }

    // Chi ACK sau khi da nhan vao hang xu ly; goi trung duoc ACK lai.
    private void nhanTinCay(InetSocketAddress nguon, String ma, String noiDung) throws IOException {
        String khoa = nguon + "/" + ma;
        if (daNhan.containsKey(khoa)) {
            soTrung.incrementAndGet();
        } else {
            if (daNhan.size() >= 20000) { return; }
            try {
                xuLyNen.execute(() -> {
                    try { xuLy.accept(nguon, noiDung); }
                    catch (RuntimeException e) { baoLoi.accept("Lỗi xử lý gói: " + e.getMessage()); }
                });
                daNhan.put(khoa, System.nanoTime());
            } catch (RejectedExecutionException e) { return; }
        }
        guiRaw(Goi.thanhByte(Goi.ghep("A", ma)), nguon);
    }

    // Bo ngau nhien goi gui, gom ca ACK, khi kiem thu mat goi.
    private void guiRaw(byte[] duLieu, InetSocketAddress dich) throws IOException {
        synchronized (ngauNhien) {
            if (ngauNhien.nextDouble() < tyLeMat) { return; }
        }
        socket.send(new DatagramPacket(duLieu, duLieu.length, dich));
    }

    public int cong() { return socket.getLocalPort(); }
    public long soLanGuiLai() { return soGuiLai.get(); }
    public long soGoiTrung() { return soTrung.get(); }

    // Dong socket de danh thuc receive, huy cac Future dang cho.
    @Override public void close() {
        dangChay = false;
        socket.close();
        dongHo.shutdownNow();
        xuLyNen.shutdownNow();
        dangCho.values().forEach(c -> c.ketQua.completeExceptionally(new IOException("UDP đã đóng")));
        dangCho.clear();
        daNhan.clear();
    }

    private static Thread luongNen(Runnable viec, String ten) {
        Thread luong = new Thread(viec, ten);
        luong.setDaemon(true);
        return luong;
    }

    /** Trang thai cua mot goi dang doi ACK. */
    private static final class ChoAck {
        final InetSocketAddress dich;
        final byte[] duLieu;
        final CompletableFuture<Void> ketQua = new CompletableFuture<>();
        ChoAck(InetSocketAddress dich, byte[] duLieu) {
            this.dich = dich;
            this.duLieu = duLieu;
        }
    }
}
