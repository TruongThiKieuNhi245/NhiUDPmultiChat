package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Mot socket nhan multicast cho mot phong; luon loc roomId truoc khi hien thi. */
public final class KenhPhong implements AutoCloseable {
    private final String roomId, nhom;
    private final int cong;
    private final MulticastSocket socket;
    private final NetworkInterface giaoDien;
    private final Consumer<TinNhan> nhanTin;
    private final Consumer<String> heThong, baoLoi;
    private volatile boolean dangChay = true;

    public KenhPhong(String roomId, String nhom, int cong, Consumer<TinNhan> nhanTin,
                     Consumer<String> heThong, Consumer<String> baoLoi) throws IOException {
        this.roomId = roomId; this.nhom = nhom; this.cong = cong;
        this.nhanTin = nhanTin; this.heThong = heThong; this.baoLoi = baoLoi;
        giaoDien = MangUtil.timGiaoDienMang();
        socket = MangUtil.vaoNhom(nhom, cong, giaoDien);
        Thread luong = new Thread(this::vongNhan, "multicast-" + roomId);
        luong.setDaemon(true);
        luong.start();
    }

    // ===== NHAN MULTICAST =====
    private void vongNhan() {
        while (dangChay) {
            try {
                byte[] dem = new byte[Protocol.GOI_TOI_DA + 1];
                DatagramPacket goi = new DatagramPacket(dem, dem.length);
                socket.receive(goi);
                if (goi.getLength() > Protocol.GOI_TOI_DA) { continue; }
                String[] p = Goi.tach(new String(goi.getData(), 0, goi.getLength(), StandardCharsets.UTF_8));
                if (p.length < 2 || !roomId.equals(p[1])) { continue; }
                if (Protocol.TIN.equals(p[0])) {
                    TinNhan tin = TinNhan.tuTruong(p);
                    nhanTin.accept(tin);
                } else if (Protocol.HE_THONG.equals(p[0]) && p.length == 3) {
                    heThong.accept(p[2]);
                }
            } catch (IllegalArgumentException e) {
                // Goi la hoac khong dung dinh dang thi bo qua.
            } catch (IOException e) {
                if (dangChay) { baoLoi.accept("Lỗi nhận multicast: " + e.getMessage()); }
            }
        }
    }

    // Roi dung nhom va card da su dung, sau do dong receive.
    @Override public void close() {
        dangChay = false;
        try { socket.leaveGroup(new InetSocketAddress(nhom, cong), giaoDien); }
        catch (IOException e) { baoLoi.accept("Lỗi rời phòng: " + e.getMessage()); }
        finally { socket.close(); }
    }
}
