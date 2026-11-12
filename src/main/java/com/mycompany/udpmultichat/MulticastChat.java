package com.mycompany.udpmultichat;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FILE CHUC NANG: xu ly UDP MULTICAST CHAT .
 *
 * Khong can Server. Moi chuong trinh la 1 thanh vien cua "nhom multicast":
 *   1. Tao MulticastSocket        4. receive() : nhan goi tin cua nhom
 *   2. joinGroup()  : vao nhom    5. leaveGroup(): roi nhom
 *   3. send() : gui toi dia chi nhom -> moi thanh vien deu nhan   6. close()
 *
 * Giao dien (ChatMulticastUI) chi viec goi cac ham o day va lang nghe ket qua
 * qua interface Listener.
 *
 * Quy uoc goi tin: "LENH|id|ten|noiDung"
 *   JOIN  : vua vao nhom          HERE : toi dang online (tra loi nguoi moi)
 *   MSG   : tin nhan              LEAVE: roi nhom
 * "id" la ma ngau nhien cua moi nguoi -> phan biet duoc 2 nguoi trung ten.
 */
public class MulticastChat {

    // Cac su kien gui len cho giao dien. Chu y: duoc goi tu LUONG NHAN,
    // nen giao dien phai tu chuyen sang luong Swing (invokeLater).
    public interface Listener {
        void onTinNhan(String ten, String noiDung, boolean laToi);
        void onHeThong(String thongBao);
        void onDanhSachOnline(List<String> dsTen);
        void onLoi(String loi);
    }

    public static final String DIA_CHI_NHOM = "224.0.0.3";   // dia chi nhom (lop D)
    public static final int CONG = 8888;
    private static final int KICH_THUOC_DEM = 1024;
    private static final int DO_DAI_TEN_TOI_DA = 20;

    private final Listener listener;

    private MulticastSocket socket;
    private InetAddress nhom;
    private NetworkInterface giaoDien;

    private String id = "";
    private String ten = "";
    private volatile boolean daVao = false;

    // Danh sach online: id -> ten (truy cap qua synchronized vi nhieu luong)
    private final Map<String, String> online = new LinkedHashMap<>();

    public MulticastChat(Listener listener) {
        this.listener = listener;
    }

    public boolean isDaVao() {
        return daVao;
    }

    public String getTen() {
        return ten;
    }

    // ================== VAO / ROI NHOM ==================
    public void vaoNhom(String tenNhap) throws Exception {
        if (daVao) {
            return;
        }
        // Bo dau | vi ky tu nay dung de phan tach du lieu
        String tenMoi = tenNhap.replace("|", "").trim();
        if (tenMoi.isEmpty()) {
            throw new Exception("Hay nhap ten truoc!");
        }
        if (tenMoi.length() > DO_DAI_TEN_TOI_DA) {
            tenMoi = tenMoi.substring(0, DO_DAI_TEN_TOI_DA);
        }

        giaoDien = timGiaoDienMang();
        if (giaoDien == null) {
            throw new Exception("Khong tim thay card mang ho tro multicast IPv4");
        }

        ten = tenMoi;
        id = UUID.randomUUID().toString().substring(0, 6);
        nhom = InetAddress.getByName(DIA_CHI_NHOM);

        socket = new MulticastSocket(CONG);                  // 1. tao socket
        socket.setTimeToLive(1);                             //    chi di trong LAN
        socket.setNetworkInterface(giaoDien);                //    gui ra dung card mang
        socket.joinGroup(new InetSocketAddress(nhom, CONG), giaoDien);   // 2. vao nhom
        daVao = true;

        Thread luongNhan = new Thread(() -> vongLapNhan());
        luongNhan.setDaemon(true);
        luongNhan.start();

        guiGoi("JOIN", "");   // bao cho ca nhom biet minh vua vao
    }

    public void roiNhom() {
        if (!daVao) {
            return;
        }
        try {
            guiGoi("LEAVE", "");   // bao cho ca nhom biet minh di
            daVao = false;
            socket.leaveGroup(new InetSocketAddress(nhom, CONG), giaoDien);   // 5. roi nhom
            socket.close();                                                   // 6. dong socket
        } catch (Exception e) {
            listener.onLoi("Loi khi roi nhom: " + e.getMessage());
        }
        synchronized (online) {
            online.clear();
        }
    }

    // ================== GUI ==================
    public void guiTin(String noiDung) {
        String tin = noiDung.trim();
        if (daVao && !tin.isEmpty()) {
            guiGoi("MSG", tin);   // tin cua minh se hien khi nhan lai tu nhom
        }
    }

    // Dong goi "LENH|id|ten|noiDung" va gui toi dia chi nhom
    private void guiGoi(String lenh, String noiDung) {
        try {
            byte[] data = (lenh + "|" + id + "|" + ten + "|" + noiDung).getBytes(StandardCharsets.UTF_8);
            if (data.length > KICH_THUOC_DEM) {
                listener.onLoi("Tin nhan qua dai (toi da " + KICH_THUOC_DEM + " byte)!");
                return;
            }
            socket.send(new DatagramPacket(data, data.length, nhom, CONG));   // 3. gui
        } catch (Exception e) {
            listener.onLoi("Loi gui: " + e.getMessage());
        }
    }

    // ================== NHAN ==================
    // Chay tren luong rieng: lien tuc nhan goi tin cua nhom
    private void vongLapNhan() {
        try {
            while (daVao) {
                byte[] vungDem = new byte[KICH_THUOC_DEM];
                DatagramPacket goi = new DatagramPacket(vungDem, vungDem.length);
                socket.receive(goi);   // 4. nhan
                xuLyGoiTin(new String(goi.getData(), 0, goi.getLength(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            if (daVao) {   // dang roi nhom thi socket dong -> loi la binh thuong
                listener.onLoi("Mat ket noi: " + e.getMessage());
            }
        }
    }

    // Tach "LENH|id|ten|noiDung" roi xu ly theo lenh
    private void xuLyGoiTin(String noiDung) {
        String[] phan = noiDung.split("\\|", 4);
        if (phan.length < 4) {
            return;   // sai dinh dang -> bo qua
        }
        String lenh = phan[0];
        String idNguoiGui = phan[1];
        String tenNguoiGui = phan[2];
        String tin = phan[3];
        boolean laToi = idNguoiGui.equals(id);

        switch (lenh) {
            case "JOIN":
                themOnline(idNguoiGui, tenNguoiGui);
                listener.onHeThong(laToi ? "Ban da vao nhom." : tenNguoiGui + " da vao nhom.");
                if (!laToi) {
                    guiGoi("HERE", "");   // cho nguoi moi biet minh dang online
                }
                break;
            case "HERE":
                themOnline(idNguoiGui, tenNguoiGui);
                break;
            case "MSG":
                listener.onTinNhan(tenNguoiGui, tin, laToi);
                break;
            case "LEAVE":
                synchronized (online) {
                    online.remove(idNguoiGui);
                }
                if (!laToi) {
                    listener.onHeThong(tenNguoiGui + " da roi nhom.");
                    baoDanhSach();
                }
                break;
            default:
                break;
        }
    }

    // ================== DANH SACH ONLINE ==================
    private void themOnline(String idNguoi, String tenNguoi) {
        synchronized (online) {
            online.put(idNguoi, tenNguoi);
        }
        baoDanhSach();
    }

    // Gui cho giao dien 1 ban sao cua danh sach ten dang online
    private void baoDanhSach() {
        List<String> ds;
        synchronized (online) {
            ds = new ArrayList<>(online.values());
        }
        listener.onDanhSachOnline(ds);
    }

    // ================== CHON CARD MANG ==================
    // Tim card dang bat, ho tro multicast, co IPv4. Uu tien card that (Wi-Fi/Ethernet),
    // bo qua card ao. Khong co card that thi dung tam loopback (chi chat trong 1 may).
    private NetworkInterface timGiaoDienMang() throws Exception {
        NetworkInterface duPhong = null;
        Enumeration<NetworkInterface> dsCard = NetworkInterface.getNetworkInterfaces();
        while (dsCard.hasMoreElements()) {
            NetworkInterface ni = dsCard.nextElement();
            if (!ni.isUp() || !ni.supportsMulticast()) {
                continue;
            }
            boolean coIPv4 = false;
            Enumeration<InetAddress> dsDiaChi = ni.getInetAddresses();
            while (dsDiaChi.hasMoreElements()) {
                if (dsDiaChi.nextElement() instanceof Inet4Address) {
                    coIPv4 = true;
                }
            }
            if (!coIPv4) {
                continue;
            }
            String tenCard = ni.getDisplayName().toLowerCase();
            boolean laCardAo = tenCard.contains("virtual") || tenCard.contains("vmware")
                    || tenCard.contains("vpn") || tenCard.contains("hyper-v")
                    || tenCard.contains("vethernet") || tenCard.contains("tap-windows");
            if (!ni.isLoopback() && !laCardAo) {
                return ni;
            }
            if (duPhong == null) {
                duPhong = ni;
            }
        }
        return duPhong;
    }
}
