package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.TepTin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Vai tro: Tep nhi phan va sieu du lieu ben vung theo phong. Ai goi: QuanLyTep. Goi: Files, Properties, TepTin. */
final class KhoTep {
    private final Path goc;
    private final Map<String,TepTin> muc=new ConcurrentHashMap<>();
    // Quet tep tam mo coi truoc khi Server nhan ket noi.
    KhoTep(Path data) throws IOException {
        goc=data.toAbsolutePath().normalize().resolve("uploads"); Files.createDirectories(goc);
        try (var ds=Files.walk(goc,2)) {
            for (Path p:ds.filter(Files::isRegularFile).toList()) {
                if (p.getFileName().toString().endsWith(".part")) { Files.deleteIfExists(p); }
                else if (p.getFileName().toString().endsWith(".properties")) { nap(p); }
            }
        }
        try (var ds=Files.walk(goc,2)) {
            for (Path p:ds.filter(Files::isRegularFile).toList()) {
                String ten=p.getFileName().toString();
                if ((ten.endsWith(".bin") || ten.endsWith(".jpg")) && !muc.containsKey(ten.substring(0,ten.lastIndexOf('.')))) { Files.deleteIfExists(p); }
            }
        }
    }
    // ===== DUONG DAN NOI BO =====
    Path duong(String room,String id,String duoi) throws IOException {
        if (!room.matches("[a-z0-9]{1,40}") || !id.matches("[a-f0-9-]{36}") || !Set.of(".bin",".jpg",".part",".properties").contains(duoi)) { throw new IOException("Mã tệp không hợp lệ."); }
        Path p=goc.resolve(room).resolve(id+duoi).normalize(); if (!p.startsWith(goc)) { throw new IOException("Đường dẫn tệp không hợp lệ."); }
        Files.createDirectories(p.getParent());
        if (!p.getParent().toRealPath().startsWith(goc.toRealPath())) { throw new IOException("Thư mục tệp trỏ ra ngoài uploads."); } return p;
    }
    // Ghi metadata tam roi doi ten; chi so thong ke cap nhat sau khi ghi thanh cong.
    synchronized void luu(TepTin t) throws IOException {
        Properties p=new Properties(); p.setProperty("id",t.id()); p.setProperty("room",t.room()); p.setProperty("user",t.user());
        p.setProperty("name",t.ten()); p.setProperty("size",Long.toString(t.kichThuoc())); p.setProperty("crc",Long.toString(t.crc())); p.setProperty("time",Long.toString(t.luc()));
        Path dich=duong(t.room(),t.id(),".properties"),tam=duong(t.room(),t.id(),".part");
        try (Writer w=Files.newBufferedWriter(tam,StandardCharsets.UTF_8)) { p.store(w,"UDP file metadata"); }
        doiTen(tam,dich); muc.put(t.id(),t);
    }
    // Nap tung muc, khong dung ten goc lam duong dan.
    private void nap(Path p) throws IOException {
        Properties d=new Properties(); try (Reader r=Files.newBufferedReader(p,StandardCharsets.UTF_8)) { d.load(r); }
        try {
            TepTin t=new TepTin(d.getProperty("id"),d.getProperty("room"),d.getProperty("user"),d.getProperty("name"),Long.parseLong(d.getProperty("size")),Long.parseLong(d.getProperty("crc")),Long.parseLong(d.getProperty("time")));
            Path bin=duong(t.room(),t.id(),".bin"); if (Files.isRegularFile(bin) && Files.size(bin)==t.kichThuoc()) { muc.put(t.id(),t); }
        } catch (RuntimeException e) { throw new IOException("Siêu dữ liệu tệp bị hỏng: "+p.getFileName(),e); }
    }
    // Tra metadata da duoc luu, khong tra nguoi dung noi bo hay token.
    TepTin lay(String id) { TepTin t=muc.get(id); if (t==null) { throw new IllegalArgumentException("Tệp không còn tồn tại."); } return t; }
    // Xoa ca anh xem truoc va metadata khi thu hoi.
    synchronized void xoa(String id) throws IOException {
        TepTin t=muc.get(id); if (t==null) { return; }
        for (String duoi:List.of(".bin",".jpg",".properties")) { Files.deleteIfExists(duong(t.room(),id,duoi)); } muc.remove(id);
    }
    // Chi xoa truc tiep cac file da kiem tra nam trong thu muc phong, khong goi shell.
    synchronized void xoaPhong(String room) throws IOException {
        for (TepTin t:List.copyOf(muc.values())) { if (t.room().equals(room)) { xoa(t.id()); } }
        Path thu=goc.resolve(room).normalize(); if (!thu.startsWith(goc) || thu.equals(goc)) { throw new IOException("Mã phòng không hợp lệ."); }
        if (!Files.exists(thu)) { return; }
        if (!thu.toRealPath().startsWith(goc.toRealPath())) { throw new IOException("Thư mục phòng trỏ ra ngoài uploads."); }
        try (var ds=Files.list(thu)) { for (Path p:ds.toList()) { Files.deleteIfExists(p); } } Files.deleteIfExists(thu);
    }
    // Xoa metadata/tep khong con tin F sau crash hoac thu hoi bi gian doan.
    synchronized void donMoCoi(Set<String> dangDung) throws IOException {
        for (String id:List.copyOf(muc.keySet())) { if (!dangDung.contains(id)) { xoa(id); } }
    }
    // Thong ke de M5 su dung, khong phai dem lai tep tren dia.
    long[] thongKe(String room,String user) {
        var ds=muc.values().stream().filter(t -> room==null || t.room().equals(room)).filter(t -> user==null || t.user().equals(user)).toList();
        return new long[]{ds.size(),ds.stream().mapToLong(TepTin::kichThuoc).sum()};
    }
    List<TepTin> entries() { return List.copyOf(muc.values()); }
    // Doi ten nguyen tu neu he thong tep ho tro; khong ghi de tep dich.
    static void doiTen(Path tam,Path dich) throws IOException {
        try { Files.move(tam,dich,StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tam,dich); }
    }
}
