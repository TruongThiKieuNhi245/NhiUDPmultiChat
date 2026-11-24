package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;

/** Nghiep vu dung chung kho desktop; adapter web khong tu ghi tin. */
public final class WebChatService implements AutoCloseable {
    private final QuanLyNguoiDung users;
    private final QuanLyPhong rooms;
    private final KiemDuyet moderation;
    private final Map<String, Session> sessions = new HashMap<>();
    private final FileChannel lockChannel;
    private final FileLock dataLock;
    private static final InetSocketAddress INTERNAL = new InetSocketAddress("127.0.0.1", 0);

    public WebChatService(Path directory) throws IOException {
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(directory.resolve("web.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired = lockChannel.tryLock();
        if (acquired == null) { lockChannel.close(); throw new IOException("Thư mục web đang được sử dụng."); }
        dataLock = acquired;
        try {
            // Khong tao admin co mat khau mac dinh cho kho web moi.
            Path config = directory.resolve("config.properties");
            if (!Files.exists(config)) {
                Files.writeString(config, "admin.password=" + BaoMat.ngauNhien(32) + "\n", StandardOpenOption.CREATE_NEW);
            }
            KhoLuuTru store = new KhoLuuTru(directory, s -> { });
            users = new QuanLyNguoiDung(store, true);
            rooms = new QuanLyPhong(store, users::danhSach);
            moderation = new KiemDuyet(store, new NhatKy(directory, 5_000_000));
            rooms.kiemDuyet(moderation::kiemTraGui, moderation::kiemTraVao);
        } catch (IOException | RuntimeException e) { dataLock.release(); lockChannel.close(); throw e; }
    }

    public synchronized void register(String login, String password) throws IOException {
        if (users.tongNguoiDung() >= 1000) { throw new IllegalArgumentException("Đã đủ tài khoản cho bản Web 1."); }
        users.dangKy(login, password);
    }

    public synchronized Identity login(String login, String password) throws IOException {
        expire();
        if (sessions.size() >= 128) { throw new IllegalArgumentException("Đã đủ phiên. Hãy thử lại sau."); }
        var person = users.dangNhap(login, password, INTERNAL);
        try {
            users.kiemTraBatBuoc(person, Protocol.SAN_SANG);
            person.sanSang = true;
            rooms.datCho(person, Protocol.SANH_ID, "");
            rooms.xacNhanVao(person, Protocol.SANH_ID);
            String token = BaoMat.ngauNhien(24);
            sessions.put(token, new Session(person));
            return new Identity(token, person.nguoi.id, person.nguoi.tenDangNhap, person.nguoi.tenHienThi);
        } catch (IOException | RuntimeException e) { users.dangXuat(person); throw e; }
    }

    public synchronized Identity identity(String token) {
        var person = require(token);
        return new Identity(token, person.nguoi.id, person.nguoi.tenDangNhap, person.nguoi.tenHienThi);
    }

    private QuanLyNguoiDung.Phien require(String token) {
        Session s = sessions.get(token);
        if (s == null || System.currentTimeMillis() - s.lastSeen > 1_800_000 || users.biKhoa(s.person.nguoi.id)) {
            logout(token); throw new IllegalArgumentException("Phiên đã hết hạn. Hãy đăng nhập lại.");
        }
        s.lastSeen = System.currentTimeMillis(); users.ping(s.person);
        users.kiemTraBatBuoc(s.person, Protocol.GUI_TIN);
        return s.person;
    }

    public synchronized boolean allowed(String token, String room) {
        try { rooms.kiemTraGui(require(token), room); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    public synchronized List<TinNhan> history(String token, String room, long after) {
        rooms.kiemTraThanhVien(require(token), room);
        return rooms.layTin(room, false, after);
    }

    // Trang gan nhat hoac 50 tin truoc cursor, cung kiem quyen voi lich su dong bo.
    public synchronized List<TinNhan> historyPage(String token, String room, long before) {
        rooms.kiemTraThanhVien(require(token), room);
        long end = before == 0 ? rooms.mocTin(room) : Math.min(before - 1, rooms.mocTin(room));
        long start = Math.max(0, end - 50);
        return rooms.layTin(room, false, start).stream().filter(t -> t.msgId <= end).toList();
    }

    // Chi UdpChatTransport.receive duoc goi diem chap nhan tin moi nay.
    public synchronized Accepted receive(String token, String room, String clientMsgId, String text) throws IOException {
        var person = require(token);
        rooms.kiemTraGui(person, room);
        boolean duplicate = rooms.tinNhan().theoMa(room, person.nguoi.id, clientMsgId) != null;
        if (!duplicate && rooms.mocTin(room) >= 10000) { throw new IllegalArgumentException("Sảnh chung đã đạt giới hạn 10.000 tin của Web 1."); }
        if (!duplicate && !moderation.choTin(person, System.currentTimeMillis())) {
            throw new IllegalArgumentException("Bạn gửi quá nhanh. Hãy chờ một chút.");
        }
        TinNhan saved = rooms.themTin(person, room, text, clientMsgId, "T");
        return new Accepted(saved, duplicate);
    }

    public synchronized void logout(String token) {
        Session s = sessions.remove(token);
        if (s != null) { users.dangXuat(s.person); }
    }
    public synchronized void expire() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> {
            if (now - e.getValue().lastSeen <= 1_800_000) { return false; }
            users.dangXuat(e.getValue().person); return true;
        });
    }
    @Override public synchronized void close() throws IOException {
        sessions.values().forEach(s -> users.dangXuat(s.person)); sessions.clear();
        dataLock.release(); lockChannel.close();
    }
    public record Identity(String token, String id, String login, String name) { }
    public record Accepted(TinNhan message, boolean duplicate) { }
    private static final class Session {
        private final QuanLyNguoiDung.Phien person;
        private long lastSeen = System.currentTimeMillis();
        Session(QuanLyNguoiDung.Phien person) { this.person = person; }
    }
}
