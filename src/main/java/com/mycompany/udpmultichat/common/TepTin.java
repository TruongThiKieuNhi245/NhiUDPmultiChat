package com.mycompany.udpmultichat.common;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.Locale;

/** Vai tro: Sieu du lieu tep va quy tac ten an toan. Ai goi: TepClient, QuanLyTep, TheTep. Goi: Goi, Files. */
public record TepTin(String id,String room,String user,String ten,long kichThuoc,long crc,long luc) {
    // ===== MA HOA THE TEP =====
    public String dongGoi() { return Goi.ghep(id,ten,Long.toString(kichThuoc),Long.toString(crc)); }
    // Tin F chi chua thong tin hien thi, quyen tai luon kiem tra o Server.
    public static TepTin doc(String room,String user,String nd,long luc) {
        String[] p=Goi.tach(nd); if (p.length!=4 || !p[0].matches("[a-f0-9-]{36}")) { throw new IllegalArgumentException("Thẻ tệp không hợp lệ."); }
        return new TepTin(p[0],room,user,p[1],Long.parseLong(p[2]),Long.parseLong(p[3]),luc);
    }
    // ===== TEN VA DUONG DAN =====
    public static String tenAnToan(String ten) {
        String n=Normalizer.normalize(ten,Normalizer.Form.NFC).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");
        while (n.contains("..")) { n=n.replace("..","_"); }
        n=n.strip().replaceAll("[. ]+$","");
        if (n.isEmpty()) { n="tep"; }
        if (n.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) { n="_"+n; }
        if (n.codePointCount(0,n.length())>100) { n=n.substring(0,n.offsetByCodePoints(0,100)); }
        return n;
    }
    // Khong ghi de, tao ten khac neu da co tep cung ten.
    public static Path tenChuaCo(Path thuMuc,String ten) throws IOException {
        Files.createDirectories(thuMuc); String n=tenAnToan(ten); int dau=n.lastIndexOf('.');
        String goc=dau>0?n.substring(0,dau):n,duoi=dau>0?n.substring(dau):"";
        for (int i=0;i<10000;i++) { Path p=thuMuc.resolve(i==0?n:goc+" ("+i+")"+duoi); if (!Files.exists(p)) { return p; } }
        throw new IOException("Thư mục có quá nhiều tệp trùng tên.");
    }
    // Dung luong hien thi khong dung cac thuat ngu giao thuc.
    public static String dungLuong(long n) { return n<1024?n+" B":n<1024*1024?String.format(Locale.ROOT,"%.1f KB",n/1024.0):String.format(Locale.ROOT,"%.1f MB",n/1048576.0); }
    // Kiem tra som o ca Client va Server.
    public static void kiemTra(long n,long max) {
        if (n<1) { throw new IllegalArgumentException("Tệp rỗng không thể gửi."); }
        if (n>max) { throw new IllegalArgumentException("Tệp quá lớn (tối đa "+dungLuong(max)+")."); }
    }
}
