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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Vai tro: CRUD, quyen chu phong, dat cho vao phong va lich su; duoc ServerCore goi tuan tu. Ai goi: ServerCore. Goi: KhoLuuTru, BaoMat, Goi, Phong. */
public final class QuanLyPhong {
    private final KhoLuuTru kho;
    private java.util.function.BiConsumer<Phien,String> truocGui=(p,id) -> { },truocVao=(p,id) -> { };
    // QuanLyPhong khong phu thuoc UI va khong can biet cach luu moderation.
    void kiemDuyet(java.util.function.BiConsumer<Phien,String> gui,java.util.function.BiConsumer<Phien,String> vao) { truocGui=gui; truocVao=vao; }
    // Snapshot tat ca phong chi duoc goi sau khi xac thuc admin hoac tu Server.
    List<Phong> tatCa() { return phong.keySet().stream().map(this::lay).toList(); }
    private final Supplier<List<Phien>> cacPhien;
    private final Map<String, NoiBo> phong = new LinkedHashMap<>();
    private final QuanLyTinNhan tinNhan;
    private final SecureRandom ngauNhien = new SecureRandom();
    private final int congGoc;
    private final java.util.Properties thanhVien;
    private final java.util.Properties bietDanh;

    // Khoi tao lop va noi cac thanh phan can thiet.
    public QuanLyPhong(KhoLuuTru kho, Supplier<List<Phien>> cacPhien) throws IOException {
        this.kho=kho; tinNhan=new QuanLyTinNhan(kho); this.cacPhien=cacPhien;
        thanhVien=kho.docThanhVien(); bietDanh=kho.docBietDanh();
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
        for (String id : phong.keySet()) {
            tinNhan.nap(id);
            String chu=phong.get(id).phong.chuId;
            if (!Protocol.SANH_ID.equals(id) && !laThanhVien(chu,id)) { ghiThanhVien(chu,id,true); }
        }
    }

    // ===== DANH SACH VA QUYEN =====
    public Phong lay(String id) {
        NoiBo n=phong.get(id);
        if (n==null) { throw new IllegalArgumentException("Phòng không tồn tại hoặc đã bị xóa"); }
        return n.phong.voiSoThanhVien(thanhVien(id).size());
    }

    // Danh sach cho.
    public List<Phong> danhSachCho(Phien p) {
        return phong.values().stream().map(n -> lay(n.phong.id)).filter(r -> r.congKhai
                || duocQuanLy(p.nguoi,r) || laThanhVien(p.nguoi.id,r.id)).toList();
    }

    // Thanh vien.
    public List<Phien> thanhVien(String id) {
        return cacPhien.get().stream().filter(p -> p.sanSang && p.phong.contains(id)).toList();
    }

    // Duoc quan ly.
    public static boolean duocQuanLy(NguoiDung nguoi, Phong p) {
        return nguoi.vaiTro==VaiTro.ADMIN || nguoi.id.equals(p.chuId);
    }

    // Kiem tra quyen.
    private void kiemTraQuyen(Phien p, Phong r) {
        if (!duocQuanLy(p.nguoi,r)) { throw new IllegalArgumentException("Chỉ chủ phòng hoặc admin được quản lý phòng"); }
    }

    // Kiem tra thanh vien.
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
        tinNhan.tao(moi.id);
        ghiThanhVien(p.nguoi.id,moi.id,true);
        return moi;
    }

    // null giu mat khau cu; chuoi rong xoa; chuoi khac dat mat khau moi.
    public Phong sua(Phien p,String id,String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu);
        if (Protocol.SANH_ID.equals(id)) { throw new IllegalArgumentException("Sảnh chung không được sửa"); }
        kiemTraNoiDung(ten,moTa,gioiHan);
        if (matKhau!=null) { kiemTraMatKhau(matKhau); }
        if (Protocol.SANH_ID.equals(id) && (!congKhai || (matKhau!=null && !matKhau.isEmpty())
                || !ten.strip().equals("Sảnh chung"))) {
            throw new IllegalArgumentException("Sảnh chung giữ nguyên tên/loại công khai và không đặt mật khẩu");
        }
        if (gioiHan<Math.max(soChoDaDung(id),soThanhVienBenVung(id))) { throw new IllegalArgumentException("Giới hạn mới nhỏ hơn số người/đặt chỗ hiện tại"); }
        NoiBo n=phong.get(id);
        String salt=matKhau==null?n.salt:(matKhau.isEmpty()?"":BaoMat.ngauNhien(16));
        String hash=matKhau==null?n.hash:(matKhau.isEmpty()?"":BaoMat.bam(salt,matKhau));
        Phong moi=new Phong(id,ten.strip(),moTa.strip(),cu.maPhong,cu.chuId,cu.nhom,cu.cong,gioiHan,
                congKhai,!hash.isEmpty(),cu.dangMo,0,cu.ngayTao);
        luuThayDoi(id,new NoiBo(moi,salt,hash));
        return lay(id);
    }

    // Dong mo.
    public void dongMo(Phien p,String id,boolean mo) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu);
        if (Protocol.SANH_ID.equals(id) && !mo) { throw new IllegalArgumentException("Sảnh chung luôn mở"); }
        NoiBo n=phong.get(id);
        Phong moi=new Phong(id,cu.ten,cu.moTa,cu.maPhong,cu.chuId,cu.nhom,cu.cong,cu.gioiHan,
                cu.congKhai,cu.coMatKhau,mo,0,cu.ngayTao);
        luuThayDoi(id,new NoiBo(moi,n.salt,n.hash));
    }

    // Xoa.
    public void xoa(Phien p,String id) throws IOException {
        Phong cu=lay(id); kiemTraQuyen(p,cu);
        if (Protocol.SANH_ID.equals(id)) { throw new IllegalArgumentException("Không được xóa Sảnh chung"); }
        luuThayDoi(id,null); tinNhan.xoa(id);
        // Xoa biet danh phong
        bietDanh.stringPropertyNames().stream().filter(k -> k.startsWith(id+".")).toList().forEach(bietDanh::remove);
        try { kho.ghiBietDanh(bietDanh); } catch (IOException ignored) {}
        // Giu file log cu de khong mat du lieu; id phong moi khong tai su dung.
    }

    // ===== VAO / ROI PHONG =====
    public Phong datCho(Phien p,String khoa,String matKhau) {
        String ma=khoa.strip().toUpperCase(Locale.ROOT);
        Phong r=phong.containsKey(khoa)?lay(khoa):phong.values().stream()
                .map(n -> n.phong).filter(n -> n.maPhong.equals(ma)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã phòng"));
        truocVao.accept(p,r.id);
        if (p.phong.contains(r.id)) { return lay(r.id); }
        if (laThanhVien(p.nguoi.id,r.id)) {
            p.choVao.put(r.id,System.nanoTime()+10_000_000_000L); return lay(r.id);
        }
        if (phong.containsKey(khoa) && !r.congKhai && !duocQuanLy(p.nguoi,r)) {
            throw new IllegalArgumentException("Hãy nhập mã để vào phòng riêng tư");
        }
        if (!r.dangMo) { throw new IllegalArgumentException("Phòng đang đóng"); }
        NoiBo n=phong.get(r.id);
        if (r.coMatKhau && !duocQuanLy(p.nguoi,r) && !BaoMat.dungMatKhau(n.salt,n.hash,matKhau)) {
            throw new IllegalArgumentException("Mật khẩu phòng không đúng");
        }
        p.choVao.remove(r.id);
        if (soThanhVienBenVung(r.id)>=r.gioiHan || soChoDaDung(r.id)>=r.gioiHan) { throw new IllegalArgumentException("Phòng đã đầy"); }
        p.choVao.put(r.id,System.nanoTime()+10_000_000_000L);
        return lay(r.id);
    }

    // Xac nhan vao.
    public void xacNhanVao(Phien p,String id) throws IOException {
        truocVao.accept(p,id);
        if (p.phong.contains(id)) { return; }
        Long hetHan=p.choVao.remove(id);
        if (hetHan==null || hetHan<System.nanoTime()) { throw new IllegalArgumentException("Đặt chỗ đã hết hạn; hãy vào lại phòng"); }
        if (!lay(id).dangMo && !laThanhVien(p.nguoi.id,id)) { throw new IllegalArgumentException("Phòng vừa đóng"); }
        ghiThanhVien(p.nguoi.id,id,true); p.phong.add(id);
    }

    // Roi.
    public void roi(Phien p,String id) { p.phong.remove(id); p.choVao.remove(id); }

    // Roi phong chu dong khac voi dang xuat: xoa tu cach thanh vien.
    public void roiChuDong(Phien p,String id) throws IOException {
        Phong r=lay(id);
        if (Protocol.SANH_ID.equals(id)) { throw new IllegalArgumentException("Không được rời Sảnh chung"); }
        if (r.chuId.equals(p.nguoi.id)) { throw new IllegalArgumentException("Chủ phòng phải chuyển quyền hoặc xóa phòng trước khi rời"); }
        ghiThanhVien(p.nguoi.id,id,false); roi(p,id);
    }

    // Chuyen chu.
    public void chuyenChu(Phien p,String id,String nguoiMoi) throws IOException {
        Phong r=lay(id); kiemTraQuyen(p,r);
        if (Protocol.SANH_ID.equals(id)) { throw new IllegalArgumentException("Không chuyển chủ Sảnh chung"); }
        if (nguoiMoi.equals(r.chuId) || !laThanhVien(nguoiMoi,id)) { throw new IllegalArgumentException("Hãy chọn thành viên khác trong phòng"); }
        NoiBo n=phong.get(id);
        Phong moi=new Phong(id,r.ten,r.moTa,r.maPhong,nguoiMoi,r.nhom,r.cong,r.gioiHan,r.congKhai,r.coMatKhau,r.dangMo,0,r.ngayTao);
        luuThayDoi(id,new NoiBo(moi,n.salt,n.hash));
    }
    // La thanh vien.
    public boolean laThanhVien(String user,String id) { return thanhVien.containsKey(id+"."+user); }
    // So thanh vien ben vung.
    public int soThanhVienBenVung(String id) { return (int)thanhVien.stringPropertyNames().stream().filter(k -> k.startsWith(id+".")).count(); }
    // Ghi thanh vien.
    private void ghiThanhVien(String user,String id,boolean them) throws IOException {
        String khoa=id+"."+user, cu=thanhVien.getProperty(khoa);
        if (them) { thanhVien.setProperty(khoa,cu==null?Instant.now().toString():cu); } else { thanhVien.remove(khoa); }
        try { kho.ghiThanhVien(thanhVien); }
        catch (IOException e) { if (cu==null) { thanhVien.remove(khoa); } else { thanhVien.setProperty(khoa,cu); } throw e; }
    }

    // Goi tu kick/ban/xoa user, khong ap dung rang buoc roi chu dong.
    void goThanhVien(String room,String user) throws IOException {
        ghiThanhVien(user,room,false);
        bietDanh.remove(room+"."+user);
        try { kho.ghiBietDanh(bietDanh); } catch (IOException ignored) {}
    }
    // Them thanh vien truc tiep tu chap nhan loi moi.
    public void themThanhVienTrucTiep(String user,String room) throws IOException {
        ghiThanhVien(user,room,true);
    }
    // Lay biet danh thanh vien theo phong.
    public String layBietDanh(String roomId,String userId) {
        return bietDanh.getProperty(roomId+"."+userId,"");
    }
    // Dat hoac xoa biet danh thanh vien trong phong (M3c.2).
    public String datBietDanh(Phien actor,String roomId,String targetUserId,String nickname,
                              QuanLyNguoiDung quanLyNguoiDung) throws IOException {
        Phong r=lay(roomId);
        kiemTraThanhVien(actor,roomId);
        if (!laThanhVien(targetUserId,roomId)) {
            throw new IllegalArgumentException("Người dùng chưa phải là thành viên trong phòng này.");
        }
        NguoiDung target=quanLyNguoiDung.timTheoId(targetUserId);
        if (target==null) { throw new IllegalArgumentException("Không tìm thấy người dùng."); }

        boolean laChinhMinh=actor.nguoi.id.equals(targetUserId);
        boolean laChu=r.chuId.equals(actor.nguoi.id);
        boolean laAdmin=actor.nguoi.vaiTro==VaiTro.ADMIN;

        if (!laChinhMinh && !laChu && !laAdmin) {
            throw new IllegalArgumentException("Chỉ chủ phòng hoặc quản trị viên mới được đặt biệt danh cho thành viên khác.");
        }
        if (!laChinhMinh && !laAdmin && target.vaiTro==VaiTro.ADMIN) {
            throw new IllegalArgumentException("Chủ phòng không được đổi biệt danh của quản trị viên.");
        }

        String khoa=roomId+"."+targetUserId;
        String nickMoi=(nickname==null)?"":nickname.strip();
        if (nickMoi.isEmpty()) {
            bietDanh.remove(khoa);
        } else {
            int doDai=nickMoi.codePointCount(0,nickMoi.length());
            if (doDai<2 || doDai>30 || nickMoi.contains("\n") || nickMoi.contains("\r")) {
                throw new IllegalArgumentException("Biệt danh cần từ 2–30 ký tự, không xuống dòng.");
            }
            bietDanh.setProperty(khoa,nickMoi);
        }
        kho.ghiBietDanh(bietDanh);
        return nickMoi;
    }
    // Ngay vao duoc luu lan dau; du lieu cu dung ngay tao phong lam du phong.
    String ngayVao(String room,String user) { String n=thanhVien.getProperty(room+"."+user,""); return n.equals("1")?lay(room).ngayTao:n; }
    // Admin co the gan chu la tai khoan offline; tu cach thanh vien duoc ghi ben vung.
    void ganChu(Phien actor,String room,String user) throws IOException { ghiThanhVien(user,room,true); chuyenChu(actor,room,user); }
    // Don dat cho.
    public void donDatCho() {
        long nay=System.nanoTime();
        cacPhien.get().forEach(p -> p.choVao.entrySet().removeIf(e -> e.getValue()<nay));
    }

    // So cho da dung.
    private int soChoDaDung(String id) {
        long nay=System.nanoTime();
        java.util.Set<String> user=new java.util.HashSet<>();
        thanhVien.stringPropertyNames().stream().filter(k -> k.startsWith(id+".")).forEach(k -> user.add(k.substring(id.length()+1)));
        cacPhien.get().stream().filter(p -> p.phong.contains(id) || p.choVao.getOrDefault(id,0L)>nay)
                .forEach(p -> user.add(p.nguoi.id));
        return user.size();
    }

    // ===== LICH SU VA MSG ID THEO PHONG =====
    public QuanLyTinNhan tinNhan() { return tinNhan; }
    // Moc tin de heartbeat phat hien mat goi cuoi.
    public long mocTin(String id) { lay(id); return tinNhan.moc(id); }
    // Tong tin gom ca tin thu hoi.
    public long tongTin() { return tinNhan.tong(); }
    // Tong phong hien co.
    public int tongPhong() { return phong.size(); }
    // API cu cho test va tin van ban khong co ma gui lai.
    public TinNhan themTin(Phien p,String id,String nd) throws IOException { return themTin(p,id,nd,"","T"); }
    // Kiem tra quyen/phong truoc khi loai trung va luu tin.
    public TinNhan themTin(Phien p,String id,String nd,String ma,String loai) throws IOException {
        kiemTraGui(p,id);
        return tinNhan.them(p,id,nd,ma,loai);
    }
    // Diem chung cho tin va tep; M5 bo sung mute/ban tai day de khong bo sot quyen.
    public void kiemTraGui(Phien p,String id) {
        kiemTraThanhVien(p,id); truocGui.accept(p,id);
        if (!lay(id).dangMo) { throw new IllegalArgumentException("Phòng đang đóng, chưa thể gửi tin hoặc tệp."); }
    }
    // Lay lich su 50 tin hoac lo dong bo tiep theo.
    public List<TinNhan> layTin(String id,boolean ganNhat,long sauId) { lay(id); return tinNhan.lay(id,ganNhat,sauId); }

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

    // Tao ma.
    private String taoMa() {
        String bang="23456789ABCDEFGHJKLMNPQRSTUVWXYZ", ma;
        do {
            StringBuilder s=new StringBuilder();
            for (int i=0;i<6;i++) { s.append(bang.charAt(ngauNhien.nextInt(bang.length()))); }
            ma=s.toString();
        } while (trungMa(ma));
        return ma;
    }
    // Trung ma.
    private boolean trungMa(String ma) { return phong.values().stream().anyMatch(n -> n.phong.maPhong.equals(ma)); }

    // Kiem tra noi dung.
    private static void kiemTraNoiDung(String ten,String moTa,int gioiHan) {
        int n=ten.strip().codePointCount(0,ten.strip().length());
        if (n<3 || n>30 || ten.contains("\n") || ten.contains("\r")) { throw new IllegalArgumentException("Tên phòng cần 3-30 ký tự, không xuống dòng."); }
        if (moTa.codePointCount(0,moTa.length())>80 || moTa.contains("\n") || moTa.contains("\r")) { throw new IllegalArgumentException("Mô tả tối đa 80 ký tự, không xuống dòng."); }
        if (gioiHan<2 || gioiHan>500) { throw new IllegalArgumentException("Giới hạn cần 2-500 người."); }
    }
    // Kiem tra mat khau.
    private static void kiemTraMatKhau(String matKhau) {
        if (matKhau.length()>128) { throw new IllegalArgumentException("Mật khẩu phòng tối đa 128 ký tự"); }
    }

    /** Du lieu rieng cua Server; DTO Phong khong chua bi mat. */
    private record NoiBo(Phong phong,String salt,String hash) { }
}
