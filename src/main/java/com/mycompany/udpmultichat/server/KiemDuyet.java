package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.util.*;

/** Vai tro: Mute/ban ben vung, quyen kiem duyet va chong spam. Ai goi: ServerCore, QuanLyPhong. Goi: KhoLuuTru, NhatKy. */
final class KiemDuyet {
    private final KhoLuuTru kho; private final Properties p; private final NhatKy log;
    private final Map<String,ArrayDeque<Long>> tin=new HashMap<>(); private final Map<String,Integer> viPham=new HashMap<>();
    private final int burst,minute;
    // Nap lai mute/ban, thoi gian dung wall clock de giu duoc sau restart.
    KiemDuyet(KhoLuuTru kho,NhatKy log) throws IOException {
        this.kho=kho; this.log=log; p=kho.docThuocTinh("moderation"); burst=Integer.parseInt(kho.cauHinh("spam.burst")); minute=Integer.parseInt(kho.cauHinh("spam.minute"));
        if (burst<1 || minute<1) { throw new IOException("Giới hạn chống spam không hợp lệ."); }
    }
    // ===== QUYEN VA LENH KIEM DUYET =====
    void quyen(Phien actor,NguoiDung dich,Phong room) {
        if (actor.nguoi.id.equals(dich.id)) { throw new IllegalArgumentException("Không kiểm duyệt chính mình."); }
        if (actor.nguoi.vaiTro==VaiTro.ADMIN) { return; }
        if (room==null || !room.chuId.equals(actor.nguoi.id) || dich.vaiTro==VaiTro.ADMIN || room.chuId.equals(dich.id)) { throw new IllegalArgumentException("Bạn không có quyền kiểm duyệt người này."); }
    }
    // Thoi han 0 la vinh vien, so am la go lenh.
    void dat(String scope,String user,String loai,long giay,String lyDo) throws IOException {
        if (!Set.of("MUTE","BAN").contains(loai) || giay>31536000) { throw new IllegalArgumentException("Thời hạn không hợp lệ."); }
        Properties cu=(Properties)p.clone();
        String k=scope+":"+user+":"+loai;
        if (giay<0) { p.remove(k); p.remove(k+":reason"); }
        else { p.setProperty(k,Long.toString(giay==0?Long.MAX_VALUE:System.currentTimeMillis()+giay*1000)); p.setProperty(k+":reason",lyDo); }
        try { kho.ghiThuocTinh("moderation",p); }
        catch (IOException e) { p.clear(); p.putAll(cu); throw e; }
    }
    // Lay thoi han con hieu luc, khong phu thuoc timer UI.
    long den(String scope,String user,String loai) {
        long n=Long.parseLong(p.getProperty(scope+":"+user+":"+loai,"0")); return n>System.currentTimeMillis()?n:0;
    }
    // Mute toan he thong va mute phong cung duoc ap dung cho text/sticker/file/thu hoi.
    long mute(String room,String user) { return Math.max(den("*",user,"MUTE"),den(room,user,"MUTE")); }
    // Server khong tin viec Client da khoa o soan.
    void kiemTraGui(Phien user,String room) {
        if (mute(room,user.nguoi.id)>0) { throw new IllegalArgumentException("Bạn đang bị cấm chat. Hãy chờ hết thời hạn."); }
        kiemTraVao(user,room);
    }
    // Ban van chan thanh vien cu, khong cho dung ma phong de vuot.
    void kiemTraVao(Phien user,String room) {
        if (den(room,user.nguoi.id,"BAN")>0) { throw new IllegalArgumentException("Bạn bị cấm vào phòng này."); }
    }
    // ===== CHONG SPAM =====
    boolean choTin(Phien user,long luc) throws IOException {
        ArrayDeque<Long> ds=tin.computeIfAbsent(user.nguoi.id,k -> new ArrayDeque<>()); while (!ds.isEmpty() && ds.peekFirst()<=luc-60000) { ds.removeFirst(); }
        long nhanh=ds.stream().filter(t -> t>luc-3000).count();
        if (nhanh>=burst || ds.size()>=minute) {
            int lan=viPham.merge(user.nguoi.id,1,Integer::sum);
            if (lan==3) { dat("*",user.nguoi.id,"MUTE",60,"Tự động chống spam"); log.ghi("HE_THONG","AUTO_MUTE",user.nguoi.tenDangNhap,"60 giây do gửi quá nhanh"); }
            return false;
        }
        ds.addLast(luc); return true;
    }
    // Dinh ky xoa muc het han va dem spam cu, khong de cache tang vo han.
    void quet() throws IOException {
        long luc=System.currentTimeMillis(); boolean doi=false;
        for (String k:List.copyOf(p.stringPropertyNames())) {
            if (k.endsWith(":reason")) { continue; }
            if (Long.parseLong(p.getProperty(k))<=luc) { p.remove(k); p.remove(k+":reason"); doi=true; }
        }
        tin.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast()<luc-60000); viPham.keySet().retainAll(tin.keySet());
        if (doi) { kho.ghiThuocTinh("moderation",p); }
    }
}
