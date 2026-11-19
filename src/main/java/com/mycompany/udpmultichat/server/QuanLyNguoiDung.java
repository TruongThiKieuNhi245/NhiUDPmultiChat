package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.BaoMat;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.KhoLuuTru.TaiKhoan;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Vai tro: Gop tai khoan va phien; Server la noi quyet dinh quyen va online. Ai goi: ServerCore. Goi: KhoLuuTru, BaoMat, NguoiDung. */
public final class QuanLyNguoiDung {
    private final KhoLuuTru kho;
    private final Map<String, TaiKhoan> taiKhoan;
    private final java.util.Properties trangThai;
    private final Map<String, Phien> phien = new LinkedHashMap<>();

    // Khoi tao lop va noi cac thanh phan can thiet.
    public QuanLyNguoiDung(KhoLuuTru kho) throws IOException {
        this.kho = kho;
        taiKhoan = kho.docNguoiDung(); trangThai=kho.docThuocTinh("accounts");
        String tenAdmin = chuanHoaTen(kho.cauHinh("admin.username"));
        TaiKhoan cu = taiKhoan.get(tenAdmin);
        if (cu==null && taiKhoan.values().stream().noneMatch(t -> t.nguoi.vaiTro==VaiTro.ADMIN)) {
            NguoiDung admin=new NguoiDung(BaoMat.ngauNhien(12),tenAdmin,"Admin",VaiTro.ADMIN,Instant.now().toString());
            taiKhoan.put(tenAdmin,new TaiKhoan(admin,kho.cauHinh("admin.salt"),kho.cauHinh("admin.passwordHash")));
        }
        kho.ghiNguoiDung(taiKhoan.values());
    }

    // ===== DANG KY / DANG NHAP =====
    public synchronized NguoiDung dangKy(String ten, String matKhau) throws IOException {
        return giaoDich("",() -> dangKyNoi(ten,matKhau));
    }
    // Tao tai khoan trong giao dich cua caller.
    private NguoiDung dangKyNoi(String ten,String matKhau) throws IOException {
        ten = chuanHoaTen(ten);
        kiemTraMatKhau(matKhau);
        if (taiKhoan.containsKey(ten)) { throw new IllegalArgumentException("Tên đăng nhập đã tồn tại"); }
        String salt = BaoMat.ngauNhien(16);
        NguoiDung nguoi = new NguoiDung(BaoMat.ngauNhien(12), ten, ten, VaiTro.THANH_VIEN,
                Instant.now().toString());
        taiKhoan.put(ten, new TaiKhoan(nguoi, salt, BaoMat.bam(salt, matKhau)));
        try { kho.ghiNguoiDung(taiKhoan.values()); }
        catch (IOException e) { taiKhoan.remove(ten); throw e; }
        return nguoi;
    }

    // Dang nhap.
    public synchronized Phien dangNhap(String ten, String matKhau, InetSocketAddress diaChi) {
        try { return giaoDich("",() -> dangNhapNoi(ten,matKhau,diaChi)); }
        catch (IOException e) { throw new IllegalArgumentException("Không lưu được lần đăng nhập.",e); }
    }
    // Chi cong bo phien sau khi da ghi trang thai mat khau tam va lan dang nhap.
    private Phien dangNhapNoi(String ten,String matKhau,InetSocketAddress diaChi) {
        try { ten=chuanHoaTen(ten); kiemTraMatKhau(matKhau); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng."); }
        TaiKhoan tk = taiKhoan.get(ten);
        if (tk == null || !BaoMat.dungMatKhau(tk.salt, tk.hash, matKhau)) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }
        if (biKhoa(tk.nguoi.id)) { throw new IllegalArgumentException("Tài khoản đã bị khóa. Hãy liên hệ quản trị viên."); }
        if (Boolean.parseBoolean(trangThai.getProperty(tk.nguoi.id+".tempUsed","false"))) { throw new IllegalArgumentException("Mật khẩu tạm đã được sử dụng. Hãy nhờ admin đặt lại nếu chưa đổi được mật khẩu."); }
        for (Phien p : phien.values()) {
            if (p.nguoi.id.equals(tk.nguoi.id)) {
                throw new IllegalArgumentException("Tài khoản đang đăng nhập ở nơi khác; chờ hết phiên nếu mất mạng");
            }
        }
        Phien moi = new Phien(BaoMat.ngauNhien(24), congKhai(tk), diaChi);
        if (Boolean.parseBoolean(trangThai.getProperty(tk.nguoi.id+".force","false"))) { trangThai.setProperty(tk.nguoi.id+".tempUsed","true"); }
        trangThai.setProperty(tk.nguoi.id+".last",Instant.now().toString());
        try { kho.ghiThuocTinh("accounts",trangThai); } catch (IOException e) { throw new IllegalArgumentException("Không lưu được lần đăng nhập.",e); }
        phien.put(moi.token, moi);
        return moi;
    }

    // Kiem tra token kem endpoint, khong tin ten hay vai tro do Client gui len.
    public synchronized Phien kiemTra(String token, InetSocketAddress diaChi) {
        Phien p = phien.get(token);
        if (p == null || !p.diaChi.equals(diaChi)) {
            throw new IllegalArgumentException("Phiên không hợp lệ. Hãy đăng nhập lại");
        }
        if (System.nanoTime() - p.pingCuoi > Protocol.HET_PHIEN_MS * 1_000_000L) {
            throw new IllegalArgumentException("Phiên đã hết hạn. Hãy đăng nhập lại");
        }
        return p;
    }

    // Chi cho phep khoi phuc cung endpoint va token; khong da nguoi dang nhap o noi khac.
    public synchronized Phien ketNoiLai(String ten,String matKhau,String token,InetSocketAddress diaChi) {
        Phien cu=phien.get(token);
        if (cu!=null && cu.diaChi.equals(diaChi) && cu.nguoi.tenDangNhap.equals(chuanHoaTen(ten))) {
            TaiKhoan tk=taiKhoan.get(cu.nguoi.tenDangNhap);
            if (!BaoMat.dungMatKhau(tk.salt,tk.hash,matKhau)) { throw new IllegalArgumentException("Sai mật khẩu"); }
            cu.pingCuoi=System.nanoTime(); return cu;
        }
        return dangNhap(ten,matKhau,diaChi);
    }
    // Tat ca.
    public synchronized List<NguoiDung> tatCa() { return taiKhoan.values().stream().map(t -> t.nguoi).toList(); }

    // ===== HO SO VA DOI MAT KHAU =====
    // Server chan moi lenh nghiep vu khi admin con dung mat khau mac dinh.
    public synchronized void kiemTraBatBuoc(Phien p,String lenh) {
        if (p.nguoi.buocDoiMatKhau && !java.util.Set.of(Protocol.PING,Protocol.DANG_XUAT,Protocol.DOI_MAT_KHAU).contains(lenh)) {
            throw new IllegalArgumentException("Bạn đang dùng mật khẩu tạm hoặc mặc định. Hãy đổi mật khẩu trước.");
        }
    }
    // Co buoc doi duoc tinh tu hash hien tai, khong tin flag tu CSV.
    private NguoiDung congKhai(TaiKhoan tk) {
        boolean buoc=Boolean.parseBoolean(trangThai.getProperty(tk.nguoi.id+".force","false")) || tk.nguoi.tenDangNhap.equalsIgnoreCase(kho.cauHinh("admin.username").trim()) && BaoMat.dungMatKhau(tk.salt,tk.hash,"admin123");
        return tk.nguoi.voiHoSo(tk.nguoi.tenHienThi,tk.nguoi.gioiThieu,tk.nguoi.anhHash,buoc);
    }
    // Tim ho so theo id bat bien, khong so ten hien thi.
    public synchronized NguoiDung lay(String id) {
        return taiKhoan.values().stream().filter(t -> t.nguoi.id.equals(id)).map(this::congKhai).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }
    // Luu ho so va cap nhat snapshot cua phien dang hoat dong.
    public synchronized NguoiDung luuHoSo(Phien p,String ten,String gt,String anh) throws IOException {
        if (ten.codePoints().anyMatch(cp -> cp>Character.MAX_VALUE)) { throw new IllegalArgumentException("Tên hiển thị không hỗ trợ biểu tượng cảm xúc"); }
        ten=ten.strip(); gt=gt.strip();
        if (ten.codePointCount(0,ten.length())<2 || ten.codePointCount(0,ten.length())>30 || ten.chars().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Tên hiển thị cần 2–30 ký tự, không xuống dòng."); }
        if (gt.codePointCount(0,gt.length())>100 || gt.chars().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Giới thiệu tối đa 100 ký tự, không xuống dòng."); }
        TaiKhoan cu=taiKhoan.get(p.nguoi.tenDangNhap);
        NguoiDung n=cu.nguoi.voiHoSo(ten,gt,anh,p.nguoi.buocDoiMatKhau);
        thayTaiKhoan(new TaiKhoan(n,cu.salt,cu.hash)); return n;
    }
    // Kiem tra mat khau cu; tai khoan chi co mot phien nen khong con phien khac.
    public synchronized NguoiDung doiMatKhau(Phien p,String cu,String moi) throws IOException {
        return giaoDich("",() -> doiMatKhauNoi(p,cu,moi));
    }
    // Cap nhat hash/config/co bat buoc trong cung mot giao dich co rollback.
    private NguoiDung doiMatKhauNoi(Phien p,String cu,String moi) throws IOException {
        kiemTraMatKhau(moi); TaiKhoan tk=taiKhoan.get(p.nguoi.tenDangNhap);
        if (!BaoMat.dungMatKhau(tk.salt,tk.hash,cu)) { throw new IllegalArgumentException("Mật khẩu hiện tại không đúng."); }
        if (p.nguoi.tenDangNhap.equalsIgnoreCase(kho.cauHinh("admin.username").trim()) && moi.equals("admin123")) { throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu mặc định."); }
        String salt=BaoMat.ngauNhien(16),hash=BaoMat.bam(salt,moi);
        NguoiDung n=tk.nguoi.voiHoSo(tk.nguoi.tenHienThi,tk.nguoi.gioiThieu,tk.nguoi.anhHash,false);
        if (n.tenDangNhap.equalsIgnoreCase(kho.cauHinh("admin.username").trim())) { kho.doiMatKhauAdmin(salt,hash); }
        trangThai.remove(n.id+".force"); trangThai.remove(n.id+".tempUsed"); kho.ghiThuocTinh("accounts",trangThai);
        try { thayTaiKhoan(new TaiKhoan(n,salt,hash)); }
        catch (IOException e) { if (n.tenDangNhap.equalsIgnoreCase(kho.cauHinh("admin.username").trim())) { kho.doiMatKhauAdmin(tk.salt,tk.hash); } throw e; }
        return n;
    }
    // Ghi thanh cong moi thay snapshot phien; rollback RAM neu ghi that bai.
    private void thayTaiKhoan(TaiKhoan moi) throws IOException {
        TaiKhoan cu=taiKhoan.put(moi.nguoi.tenDangNhap,moi);
        try { kho.ghiNguoiDung(taiKhoan.values()); }
        catch (IOException e) { taiKhoan.put(cu.nguoi.tenDangNhap,cu); throw e; }
        phien.values().stream().filter(p -> p.nguoi.id.equals(moi.nguoi.id)).forEach(p -> p.nguoi=moi.nguoi);
    }

    // ===== QUAN TRI TAI KHOAN, CHI DUOC GOI SAU KIEM TRA ADMIN =====
    public synchronized boolean biKhoa(String id) { return Boolean.parseBoolean(trangThai.getProperty(id+".locked","false")); }
    // Lan dang nhap cuoi duoc luu qua restart.
    public synchronized String lanCuoi(String id) { return trangThai.getProperty(id+".last",""); }
    // Lay duoc ca tai khoan da khoa de hien dashboard.
    public synchronized NguoiDung taoQuanTri(Phien actor,String ten,String hien,String vai,String mk) throws IOException {
        return giaoDich("",() -> taoQuanTriNoi(actor,ten,hien,vai,mk));
    }
    // Tao ho so va co mat khau tam; neu bat ky buoc nao loi thi khong de user nua chung.
    private NguoiDung taoQuanTriNoi(Phien actor,String ten,String hien,String vai,String mk) throws IOException {
        VaiTro.valueOf(vai); kiemTraMatKhau(mk);
        if (hien.strip().length()<2 || hien.strip().length()>30 || hien.codePoints().anyMatch(c -> c>Character.MAX_VALUE || Character.isISOControl(c))) { throw new IllegalArgumentException("Tên hiển thị cần 2–30 ký tự và không chứa biểu tượng cảm xúc."); }
        NguoiDung n=dangKy(ten,mk); n=suaQuanTri(actor,n.id,hien,"",vai);
        trangThai.setProperty(n.id+".force","true"); kho.ghiThuocTinh("accounts",trangThai); return lay(n.id);
    }
    // Kiem tra vai tro va rang buoc admin truoc khi luu.
    public synchronized NguoiDung suaQuanTri(Phien nguoi,String id,String ten,String gt,String vai) throws IOException {
        return giaoDich("",() -> suaQuanTriNoi(nguoi,id,ten,gt,vai));
    }
    // Ten/gioi thieu/vai tro phai cung thanh cong hoac cung khoi phuc.
    private NguoiDung suaQuanTriNoi(Phien nguoi,String id,String ten,String gt,String vai) throws IOException {
        NguoiDung cu=lay(id); VaiTro moi=VaiTro.valueOf(vai); baoVe(nguoi,id,moi!=VaiTro.ADMIN);
        Phien gia=new Phien("",cu,null); NguoiDung n=luuHoSo(gia,ten,gt,cu.anhHash);
        n=new NguoiDung(n.id,n.tenDangNhap,n.tenHienThi,moi,n.ngayTao,n.gioiThieu,n.anhHash,n.buocDoiMatKhau);
        TaiKhoan tk=taiKhoan.get(n.tenDangNhap); thayTaiKhoan(new TaiKhoan(n,tk.salt,tk.hash)); return n;
    }
    // Khong khoa/xoa/ha quyen chinh minh; khong mat admin hoat dong cuoi.
    private void baoVe(Phien actor,String id,boolean nguyHiem) {
        if (!nguyHiem) { return; }
        if (actor.nguoi.id.equals(id)) { throw new IllegalArgumentException("Không thể khóa, xóa hoặc hạ quyền chính mình."); }
        NguoiDung n=lay(id);
        if (n.vaiTro==VaiTro.ADMIN && !biKhoa(id) && tatCa().stream().filter(u -> u.vaiTro==VaiTro.ADMIN && !biKhoa(u.id)).count()<=1) { throw new IllegalArgumentException("Phải còn ít nhất một quản trị viên hoạt động."); }
    }
    // Ly do khoa duoc luu rieng, khong luu mat khau tho.
    public synchronized void khoa(Phien actor,String id,boolean bat,String lyDo) throws IOException {
        giaoDich("",() -> { khoaNoi(actor,id,bat,lyDo); return null; });
    }
    // Ghi trang thai khoa va ly do truoc khi caller ngat phien.
    private void khoaNoi(Phien actor,String id,boolean bat,String lyDo) throws IOException {
        baoVe(actor,id,bat); lay(id); trangThai.setProperty(id+".locked",Boolean.toString(bat)); trangThai.setProperty(id+".reason",lyDo); kho.ghiThuocTinh("accounts",trangThai);
    }
    // Sinh mat khau tam 10 ky tu va chi tra trong phan hoi lenh nay.
    public synchronized String datLai(Phien actor,String id) throws IOException {
        return giaoDich("",() -> datLaiNoi(actor,id));
    }
    // Mat khau tam chi duoc tra ve khi hash va co bat buoc da luu du.
    private String datLaiNoi(Phien actor,String id) throws IOException {
        if (actor.nguoi.id.equals(id)) { throw new IllegalArgumentException("Hãy dùng Đổi mật khẩu cho chính mình."); }
        NguoiDung n=lay(id); String mk=BaoMat.ngauNhien(5),salt=BaoMat.ngauNhien(16),hash=BaoMat.bam(salt,mk);
        if (n.tenDangNhap.equals(kho.cauHinh("admin.username"))) { kho.doiMatKhauAdmin(salt,hash); }
        trangThai.remove(id+".tempUsed"); trangThai.setProperty(id+".force","true"); kho.ghiThuocTinh("accounts",trangThai);
        thayTaiKhoan(new TaiKhoan(n,salt,hash)); return mk;
    }
    // Xoa ho so va avatar; tin cu duoc doi nhan boi QuanLyTinNhan.
    public synchronized void xoaQuanTri(Phien actor,String id) throws IOException {
        giaoDich(lay(id).tenDangNhap,() -> { xoaQuanTriNoi(actor,id); return null; });
    }
    // Avatar va users.csv duoc chup cung giao dich de phuc hoi neu xoa loi.
    private void xoaQuanTriNoi(Phien actor,String id) throws IOException {
        baoVe(actor,id,true); NguoiDung n=lay(id); TaiKhoan cu=taiKhoan.remove(n.tenDangNhap);
        try { kho.ghiNguoiDung(taiKhoan.values()); kho.ghiAnh(n.tenDangNhap,new byte[0]); }
        catch (IOException e) { taiKhoan.put(n.tenDangNhap,cu); throw e; }
    }
    // Khoi phuc RAM, snapshot phien va tep neu thao tac nhieu buoc that bai.
    private <T> T giaoDich(String login,Viec<T> viec) throws IOException {
        Map<String,TaiKhoan> cu=new LinkedHashMap<>(taiKhoan);
        java.util.Properties trangCu=(java.util.Properties)trangThai.clone();
        KhoLuuTru.BanLuu ban=kho.saoLuuTaiKhoan(login);
        try { return viec.chay(); }
        catch (IOException | RuntimeException e) {
            taiKhoan.clear(); taiKhoan.putAll(cu); trangThai.clear(); trangThai.putAll(trangCu);
            phien.values().forEach(p -> { TaiKhoan tk=cu.get(p.nguoi.tenDangNhap); if (tk!=null) { p.nguoi=congKhai(tk); } });
            kho.khoiPhucTaiKhoan(ban,e); throw e;
        }
    }
    @FunctionalInterface private interface Viec<T> { T chay() throws IOException; }
    // ===== HEARTBEAT =====
    public synchronized void ping(Phien p) { p.pingCuoi = System.nanoTime(); }
    // Dang xuat.
    public synchronized void dangXuat(Phien p) { phien.remove(p.token); }
    // Danh sach.
    public synchronized List<Phien> danhSach() { return new ArrayList<>(phien.values()); }
    // Tong nguoi dung.
    public synchronized int tongNguoiDung() { return taiKhoan.size(); }

    // Xoa het han.
    public synchronized List<Phien> xoaHetHan() {
        long hienTai = System.nanoTime();
        List<Phien> hetHan = phien.values().stream()
                .filter(p -> hienTai - p.pingCuoi > Protocol.HET_PHIEN_MS * 1_000_000L).toList();
        hetHan.forEach(p -> phien.remove(p.token));
        return hetHan;
    }

    // Chuan hoa ten.
    private static String chuanHoaTen(String ten) {
        String ketQua = ten.trim().toLowerCase(Locale.ROOT);
        if (!ketQua.matches("[a-z0-9_]{3,20}")) {
            throw new IllegalArgumentException("Tên cần 3–20 chữ cái a-z, số hoặc dấu gạch dưới");
        }
        return ketQua;
    }

    // Kiem tra mat khau.
    private static void kiemTraMatKhau(String matKhau) {
        if (matKhau.length() < 6 || matKhau.length() > 128) {
            throw new IllegalArgumentException("Mật khẩu cần 6–128 ký tự");
        }
    }

    /** Phien chi ton tai trong RAM, duoc xoa khi dang xuat hoac het heartbeat. */
    public static final class Phien {
        final String token;
        NguoiDung nguoi;
        final InetSocketAddress diaChi;
        long pingCuoi = System.nanoTime();
        boolean sanSang;
        final java.util.Set<String> phong = new java.util.LinkedHashSet<>();
        final Map<String, Long> choVao = new LinkedHashMap<>();
        // Tao phien gan token, tai khoan va endpoint UDP.
        Phien(String token, NguoiDung nguoi, InetSocketAddress diaChi) {
            this.token = token; this.nguoi = nguoi; this.diaChi = diaChi;
        }
    }
}
