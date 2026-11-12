package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.PhanManh;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Vai tro: Nhan avatar tam, ghep/CRC va luu ho so. Ai goi: ServerCore. Goi: QuanLyNguoiDung, KhoLuuTru, PhanManh. */
public final class QuanLyHoSo {
    private final KhoLuuTru kho;
    private final QuanLyNguoiDung nguoi;
    private final Map<String,AnhTam> dangNhan=new HashMap<>();
    // Gan kho va quan ly tai khoan cua Server dang chay.
    public QuanLyHoSo(KhoLuuTru kho,QuanLyNguoiDung nguoi) { this.kho=kho; this.nguoi=nguoi; }
    // ===== PHAN LENH DA XAC THUC =====
    public synchronized String[] xuLy(Phien p,String[] g) throws IOException {
        donDep();
        switch (g[0]) {
            case Protocol.HO_SO: du(g,4); return nguoi.lay(g[3]).cacTruong();
            case Protocol.DOI_MAT_KHAU: du(g,5); return nguoi.doiMatKhau(p,g[3],g[4]).cacTruong();
            case Protocol.ANH_BAT_DAU:
                du(g,6); if (!g[3].matches("[a-f0-9-]{36}")) { throw new IllegalArgumentException("Mã truyền ảnh không hợp lệ."); }
                if (dangNhan.size()>=128 && !dangNhan.containsKey(p.token)) { throw new IllegalArgumentException("Server đang bận nhận ảnh. Hãy thử lại."); }
                dangNhan.put(p.token,new AnhTam(g[3],new PhanManh.BoGhep(Integer.parseInt(g[4]),g[5]))); return new String[]{"OK"};
            case Protocol.ANH_MANH:
                du(g,6); AnhTam a=layTam(p,g[3]); a.bo.nhan(Integer.parseInt(g[4]),g[5]); return new String[]{"OK"};
            case Protocol.ANH_HUY:
                du(g,4); AnhTam cu=dangNhan.get(p.token); if (cu!=null && cu.id.equals(g[3])) { dangNhan.remove(p.token); } return new String[]{"OK"};
            case Protocol.LUU_HO_SO: du(g,6); return luu(p,g[3],g[4],g[5]).cacTruong();
            case Protocol.ANH_DOC: du(g,6); return doc(g[3],g[4],Integer.parseInt(g[5]));
            default: throw new IllegalArgumentException("Lệnh hồ sơ không hợp lệ.");
        }
    }
    // Chi luu anh vao tep chinh khi PROFILE_SAVE hop le; Huy khong doi ho so.
    private NguoiDung luu(Phien p,String ten,String gt,String ma) throws IOException {
        byte[] data=null;
        if (ma.equals("DELETE")) { data=new byte[0]; }
        else if (!ma.equals("KEEP")) { data=layTam(p,ma).bo.ketQua(); PhanManh.docAnh(data,true); }
        byte[] cu=data==null?null:kho.docAnh(p.nguoi.tenDangNhap);
        String hash=data==null?p.nguoi.anhHash:(data.length==0?"":PhanManh.crc(data));
        if (data!=null) { kho.ghiAnh(p.nguoi.tenDangNhap,data); }
        try { NguoiDung n=nguoi.luuHoSo(p,ten,gt,hash); dangNhan.remove(p.token); return n; }
        catch (IOException | RuntimeException e) { if (cu!=null) { kho.ghiAnh(p.nguoi.tenDangNhap,cu); } throw e; }
    }
    // Download theo id va phien ban; Client cu khong ghep lan hai avatar khac nhau.
    private String[] doc(String id,String hash,int i) throws IOException {
        NguoiDung n=nguoi.lay(id);
        if (hash.isEmpty() || !hash.equals(n.anhHash)) { throw new IllegalArgumentException("Ảnh đã thay đổi. Hãy tải lại hồ sơ."); }
        byte[] data=kho.docAnh(n.tenDangNhap);
        if (!PhanManh.crc(data).equals(hash)) { throw new IOException("Ảnh lưu trữ không khớp hồ sơ."); }
        return new String[]{Integer.toString(data.length),hash,PhanManh.manh(data,i)};
    }
    // Tim bo ghep chi thuoc token dang gui; khong truy cap avatar tam cua nguoi khac.
    private AnhTam layTam(Phien p,String id) {
        AnhTam a=dangNhan.get(p.token);
        if (a==null || !a.id.equals(id)) { throw new IllegalArgumentException("Lượt tải ảnh đã hết hạn. Hãy chọn và lưu lại."); }
        a.luc=System.nanoTime(); return a;
    }
    // Thu hoi bo dem sau 60 giay khong co manh moi.
    public synchronized void donDep() { dangNhan.values().removeIf(a -> System.nanoTime()-a.luc>60000000000L); }
    // Moi lenh co dung so truong, khong chap nhan goi thieu/du.
    private static void du(String[] g,int n) { if (g.length!=n) { throw new IllegalArgumentException("Gói hồ sơ không hợp lệ."); } }
    /** Anh tam chi ton tai trong RAM, toi da 64 KB moi phien. */
    private static final class AnhTam {
        final String id; final PhanManh.BoGhep bo; long luc=System.nanoTime();
        // Tao bo dem anh cho mot luot upload.
        AnhTam(String id,PhanManh.BoGhep bo) { this.id=id; this.bo=bo; }
    }
}
