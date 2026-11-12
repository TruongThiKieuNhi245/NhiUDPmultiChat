package com.mycompany.udpmultichat.common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

/** Vai tro: Chon IPv4 va tao MulticastSocket; tai su dung cach chon card cua ban goc. Ai goi: ClientMain, ServerMain, KenhPhong, ServerCore. Goi: NetworkInterface, MulticastSocket. */
public final class MangUtil {
    // Khoi tao lop va noi cac thanh phan can thiet.
    private MangUtil() { }

    // Goi truoc khi tao bat ky socket nao, ke ca khi chay SelfTest.
    public static void dungIPv4() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }
    // Dia chi LAN cung card multicast de Client may khac nhap, khong mo socket.
    public static String diaChiLan() throws SocketException {
        Enumeration<InetAddress> ds=timGiaoDienMang().getInetAddresses();
        while (ds.hasMoreElements()) {
            InetAddress ip=ds.nextElement();
            if (ip instanceof Inet4Address && !ip.isLoopbackAddress()) { return ip.getHostAddress(); }
        }
        return "127.0.0.1";
    }

    // ===== CHON CARD MANG =====
    // Uu tien card that co IPv4, du phong card ao hoac loopback ho tro multicast.
    public static NetworkInterface timGiaoDienMang() throws SocketException {
        NetworkInterface duPhong = null;
        Enumeration<NetworkInterface> dsCard = NetworkInterface.getNetworkInterfaces();
        while (dsCard.hasMoreElements()) {
            NetworkInterface ni = dsCard.nextElement();
            if (!ni.isUp() || !ni.supportsMulticast()) { continue; }
            boolean coIPv4 = false;
            Enumeration<InetAddress> dsDiaChi = ni.getInetAddresses();
            while (dsDiaChi.hasMoreElements()) {
                if (dsDiaChi.nextElement() instanceof Inet4Address) { coIPv4 = true; }
            }
            if (!coIPv4) { continue; }
            String tenCard = ni.getDisplayName().toLowerCase(Locale.ROOT);
            boolean laCardAo = tenCard.contains("virtual") || tenCard.contains("vmware")
                    || tenCard.contains("vpn") || tenCard.contains("hyper-v")
                    || tenCard.contains("vethernet") || tenCard.contains("tap-windows");
            if (!ni.isLoopback() && !laCardAo) { return ni; }
            if (duPhong == null) { duPhong = ni; }
        }
        if (duPhong == null) { throw new SocketException("Không tìm thấy card multicast IPv4"); }
        return duPhong;
    }

    // Tao socket nhan nhom, cho phep nhieu Client tren cung may.
    public static MulticastSocket vaoNhom(String diaChi, int cong,
                                          NetworkInterface giaoDien) throws java.io.IOException {
        MulticastSocket socket = new MulticastSocket(null);
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(cong));
            socket.setNetworkInterface(giaoDien);
            socket.setTimeToLive(1);
            socket.joinGroup(new InetSocketAddress(diaChi, cong), giaoDien);
            return socket;
        } catch (java.io.IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
    }
}
