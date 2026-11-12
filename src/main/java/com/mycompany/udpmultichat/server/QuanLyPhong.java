package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.BaoMat;
import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** CRUD, quyen chu phong, dat cho vao phong va lich su; duoc ServerCore goi tuan tu. */
public final class QuanLyPhong {
    private final KhoLuuTru kho;
    private final Supplier<List<Phien>> cacPhien;
    private final Map<String, NoiBo> phong = new LinkedHashMap<>();
    private final Map<String, List<TinNhan>> lichSu = new LinkedHashMap<>();
    private final SecureRandom ngauNhien = new SecureRandom();
    private final int congGoc;

    public QuanLyPhong(KhoLuuTru kho, Supplier<List<Phien>> cacPhien) throws IOException {
        this.kho=kho; this.cacPhien=cacPhien;
        try {
            congGoc=Integer.parseInt(kho.cauHinh("multicast.portBase"));
            if (congGoc<1024 || congGoc>65000) { throw new IllegalArgumentException(); }
            for (String[] dong : kho.docPhong()) {
                if (dong.length!=15) { throw new IllegalArgumentException("rooms.csv sai số cột"); }
                Phong p=Phong.tuTruong(dong,0);
                if (!p.id.matches("sanh|[a-f0-9]{24}") || !p.maPhong.matches("[A-Z0-9]{6}")
                        || phong.containsKey(p.id) || phong.values().stream().anyMatch(n ->
                            n.phong.maPhong.equals(p.maPhong) || n.phong.nhom.equals(p.nhom))) {
                    throw new IllegalArgumentException("rooms.csv trùng/sai mã phòng hoặc nhóm");
                }
                int so=Integer.parseInt(p.nhom.substring(p.nhom.lastIndexOf('.')+1));
                if (so<1 || so>254 || !p.nhom.equals("239.255.42."+so)) { throw new IllegalArgumentException(); }
                Phong nap=new Phong(p.id,p.ten,p.moTa,p.maPhong,p.chuId,p.nhom,congGoc+so,
                        p.gioiHan,p.congKhai,p.coMatKhau,p.dangMo,0,p.ngayTao);
                phong.put(p.id,new NoiBo(nap,dong[13],dong[14]));
            }
        } catch (RuntimeException e) { throw new IOException("Không nạp được cấu hình/phòng: "+e.getMessage(),e); }
        if (!phong.containsKey(Protocol.SANH_ID)) {
            if (phong.values().stream().anyMatch(n -> n.phong.nhom.equals(Protocol.SANH_NHOM))) {
                throw new IOException("Nhóm của Sảnh chung đang bị phòng khác sử dụng");
            }
            Phong sanh=new Phong(Protocol.SANH_ID,"Sảnh chung","Phòng chung cho mọi người","SANH01",
                    "HE_THONG",Protocol.SANH_NHOM,congGoc+1,1000,true,false,true,0,Instant.now().toString());
            luuThayDoi(sanh.id,new NoiBo(sanh,"",""));
        }
        Phong sanh=lay(Protocol.SANH_ID);
        if (!sanh.congKhai || !sanh.dangMo || sanh.coMatKhau) { throw new IOException("Sảnh chung phải công khai và luôn mở"); }
        for (String id : phong.keySet()) { lichSu.put(id,kho.docTin(id)); }
    }

    // ===== DANH SACH VA QUYEN =====
    public Phong lay(String id) {
        NoiBo n=phong.get(id);
        if (n==null) { throw new IllegalArgumentException("Phòng không tồn tại hoặc đã bị xóa"); }
        return n.phong.voiSoThanhVien(thanhVien(id).size());
    }

    public List<Phong> danhSachCho(Phien p) {
        return phong.values().stream().map(n -> lay(n.phong.id)).filter(r -> r.congKhai
                || duocQuanLy(p.nguoi,r) || p.phong.contains(r.id)).toList();
    }

    public List<Phien> thanhVien(String id) {
        return cacPhien.get().stream().filter(p -> p.sanSang && p.phong.contains(id)).toList();
    }

    public static boolean duocQuanLy(NguoiDung nguoi, Phong p) {
        return nguoi.vaiTro==VaiTro.ADMIN || nguoi.id.equals(p.chuId);
    }

    private void kiemTraQuyen(Phien p, Phong r) {
        if (!duocQuanLy(p.nguoi,r)) { throw new IllegalArgumentException("Chỉ chủ phòng hoặc admin được quản lý phòng"); }
    }

    public void kiemTraThanhVien(Phien p, String id) {
        lay(id);
        if (!p.phong.contains(id)) { throw new IllegalArgumentException("Bạn chưa vào phòng này"); }
    }

    // ===== TAO / SUA / XOA / DONG MO =====
    public Phong tao(Phien p,String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) throws IOException {
        kiemTraNoiDung(ten,moTa,gioiHan); kiemTraMatKhau(matKhau);
        int so=2;
        while (so<=254) {
            String nhom="239.255.42."+so;
            if (phong.values().stream().noneMatch(n -> n.phong.nhom.equals(nhom))) { break; }
            so++;
        }
        if (so>254) { throw new IllegalArgumentException("Đã đủ 254 phòng hoạt động"); }
        String salt=matKhau.isEmpty()?"":BaoMat.ngauNhien(16);
        Phong moi=new Phong(BaoMat.ngauNhien(12),ten.strip(),moTa.strip(),taoMa(),p.nguoi.id,
                "239.255.42."+so,congGoc+so,gioiHan,congKhai,!matKhau.isEmpty(),true,0,Instant.now().toString());
        luuThayDoi(moi.id,new NoiBo(moi,salt,matKhau.isEmpty()?"":BaoMat.bam(salt,matKhau)));
        lichSu.put(moi.id,new ArrayList<>());
        return moi;
    }

    // null giu mat khau cu; chuoi rong xoa; chuoi khac dat mat khau moi.
    public Phong sua(Phien p,String id,String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu); kiemTraNoiDung(ten,moTa,gioiHan);
        if (matKhau!=null) { kiemTraMatKhau(matKhau); }
        if (Protocol.SANH_ID.equals(id) && (!congKhai || (matKhau!=null && !matKhau.isEmpty())
                || !ten.strip().equals("Sảnh chung"))) {
            throw new IllegalArgumentException("Sảnh chung giữ nguyên tên/loại công khai và không đặt mật khẩu");
        }
        if (gioiHan<soChoDaDung(id)) { throw new IllegalArgumentException("Giới hạn mới nhỏ hơn số người/đặt chỗ hiện tại"); }
        NoiBo n=phong.get(id);
        String salt=matKhau==null?n.salt:(matKhau.isEmpty()?"":BaoMat.ngauNhien(16));
        String hash=matKhau==null?n.hash:(matKhau.isEmpty()?"":BaoMat.bam(salt,matKhau));
        Phong moi=new Phong(id,ten.strip(),moTa.strip(),cu.maPhong,cu.chuId,cu.nhom,cu.cong,gioiHan,
                congKhai,!hash.isEmpty(),cu.dangMo,0,cu.ngayTao);
        luuThayDoi(id,new NoiBo(moi,salt,hash));
        return lay(id);
    }

    public void dongMo(Phien p,String id,boolean mo) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu);
        if (Protocol.SANH_ID.equals(id) && !mo) { throw new IllegalArgumentException("Sảnh chung luôn mở"); }
        NoiBo n=phong.get(id);
        Phong moi=new Phong(id,cu.ten,cu.moTa,cu.maPhong,cu.chuId,cu.nhom,cu.cong,cu.gioiHan,
                cu.congKhai,cu.coMatKhau,mo,0,cu.ngayTao);
        luuThayDoi(id,new NoiBo(moi,n.salt,n.hash));
    }

    public void xoa(Phien p,String id) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu);
        if (Protocol.SANH_ID.equals(id)) { throw new IllegalArgumentException("Không được xóa Sảnh chung"); }
        luuThayDoi(id,null); lichSu.remove(id);
        // Giu file log cu de khong mat du lieu; id phong moi khong tai su dung.
    }

    // ===== VAO / ROI PHONG =====
    public Phong datCho(Phien p,String khoa,String matKhau) {
        String ma=khoa.strip().toUpperCase(Locale.ROOT);
        Phong r=phong.containsKey(khoa)?lay(khoa):phong.values().stream()
                .map(n -> n.phong).filter(n -> n.maPhong.equals(ma)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã phòng"));
        if (p.phong.contains(r.id)) { return lay(r.id); }
        if (phong.containsKey(khoa) && !r.congKhai && !duocQuanLy(p.nguoi,r)) {
            throw new IllegalArgumentException("Hãy nhập mã để vào phòng riêng tư");
        }
        if (!r.dangMo) { throw new IllegalArgumentException("Phòng đang đóng"); }
        NoiBo n=phong.get(r.id);
        if (r.coMatKhau && !duocQuanLy(p.nguoi,r) && !BaoMat.dungMatKhau(n.salt,n.hash,matKhau)) {
            throw new IllegalArgumentException("Mật khẩu phòng không đúng");
        }
        p.choVao.remove(r.id);
        if (soChoDaDung(r.id)>=r.gioiHan) { throw new IllegalArgumentException("Phòng đã đầy"); }
        p.choVao.put(r.id,System.nanoTime()+10_000_000_000L);
        return lay(r.id);
    }

    public void xacNhanVao(Phien p,String id) {
        if (p.phong.contains(id)) { return; }
        Long hetHan=p.choVao.remove(id);
        if (hetHan==null || hetHan<System.nanoTime()) { throw new IllegalArgumentException("Đặt chỗ đã hết hạn; hãy vào lại phòng"); }
        if (!lay(id).dangMo) { throw new IllegalArgumentException("Phòng vừa đóng"); }
        p.phong.add(id);
    }

    public void roi(Phien p,String id) { p.phong.remove(id); p.choVao.remove(id); }

    public void donDatCho() {
        long nay=System.nanoTime();
        cacPhien.get().forEach(p -> p.choVao.entrySet().removeIf(e -> e.getValue()<nay));
    }

    private int soChoDaDung(String id) {
        long nay=System.nanoTime();
        return (int)cacPhien.get().stream().filter(p -> p.phong.contains(id)
                || p.choVao.getOrDefault(id,0L)>nay).count();
    }

    // ===== LICH SU VA MSG ID THEO PHONG =====
    public long mocTin(String id) { lay(id); return lichSu.get(id).size(); }
    public long tongTin() { return lichSu.values().stream().mapToLong(List::size).sum(); }

    public TinNhan themTin(Phien p,String id,String noiDung) throws IOException {
        kiemTraThanhVien(p,id); Phong r=lay(id);
        if (!r.dangMo) { throw new IllegalArgumentException("Phòng đang đóng, chưa thể gửi tin"); }
        if (noiDung.isBlank() || noiDung.getBytes(StandardCharsets.UTF_8).length>600) {
            throw new IllegalArgumentException("Tin nhắn cần 1–600 byte UTF-8");
        }
        TinNhan tin=new TinNhan(mocTin(id)+1,id,p.nguoi.id,p.nguoi.tenHienThi,noiDung,System.currentTimeMillis());
        // Tin phai vua ca goi gui bu (them envelope va ma dong bo).
        String sync=Goi.ghep(Goi.noi(new String[]{Protocol.MUC_DONG_BO,id,"x".repeat(36)},Goi.tach(tin.dongGoi())));
        Goi.thanhByte(Goi.ghep("R","x".repeat(80),sync));
        kho.ghiTin(tin); lichSu.get(id).add(tin);
        return tin;
    }

    public List<TinNhan> layTin(String id,boolean ganNhat,long sauId) {
        List<TinNhan> ds=lichSu.get(id); lay(id);
        int batDau=ganNhat?Math.max(0,ds.size()-50):(int)Math.min(Math.max(0,sauId),ds.size());
        int ketThuc=ganNhat?ds.size():Math.min(ds.size(),batDau+50);
        return List.copyOf(ds.subList(batDau,ketThuc));
    }

    // ===== GHI AN TOAN VA KIEM TRA DAU VAO =====
    private void luuThayDoi(String id,NoiBo moi) throws IOException {
        if (moi!=null) {
            String[] dau={"RESPONSE","x".repeat(36),"OK"};
            Goi.thanhByte(Goi.ghep("R","x".repeat(80),Goi.ghep(Goi.noi(dau,moi.phong.cacTruong()))));
        }
        NoiBo cu=phong.get(id);
        if (moi==null) { phong.remove(id); } else { phong.put(id,moi); }
        try {
            List<String[]> dong=phong.values().stream().map(n -> Goi.noi(
                    n.phong.voiSoThanhVien(0).cacTruong(),new String[]{n.salt,n.hash})).toList();
            kho.ghiPhong(dong);
        } catch (IOException e) { if (cu==null) { phong.remove(id); } else { phong.put(id,cu); } throw e; }
    }

    private String taoMa() {
        String bang="23456789ABCDEFGHJKLMNPQRSTUVWXYZ", ma;
        do {
            StringBuilder s=new StringBuilder();
            for (int i=0;i<6;i++) { s.append(bang.charAt(ngauNhien.nextInt(bang.length()))); }
            ma=s.toString();
        } while (trungMa(ma));
        return ma;
    }
    private boolean trungMa(String ma) { return phong.values().stream().anyMatch(n -> n.phong.maPhong.equals(ma)); }

    private static void kiemTraNoiDung(String ten,String moTa,int gioiHan) {
        if (ten.isBlank() || ten.strip().getBytes(StandardCharsets.UTF_8).length>80
                || moTa.getBytes(StandardCharsets.UTF_8).length>240 || ten.contains("\n") || ten.contains("\r")
                || moTa.contains("\n") || moTa.contains("\r") || gioiHan<1 || gioiHan>1000) {
            throw new IllegalArgumentException("Tên tối đa 80 byte, mô tả 240 byte, một dòng; giới hạn 1–1000 người");
        }
    }
    private static void kiemTraMatKhau(String matKhau) {
        if (matKhau.length()>128) { throw new IllegalArgumentException("Mật khẩu phòng tối đa 128 ký tự"); }
    }

    /** Du lieu rieng cua Server; DTO Phong khong chua bi mat. */
    private record NoiBo(Phong phong,String salt,String hash) { }
}
