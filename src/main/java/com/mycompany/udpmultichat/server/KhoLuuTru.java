package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.BaoMat;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.io.IOException;
import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/** Vai tro: Luu tai khoan va config UTF-8; ghi file tam roi thay the de tranh file do dang. Ai goi: ServerCore, QuanLyNguoiDung, QuanLyPhong. Goi: Files, BaoMat, Goi. */
public final class KhoLuuTru {
    private final Path thuMuc;
    private final Consumer<String> log;
    private final Properties cauHinh = new Properties();

    // Khoi tao lop va noi cac thanh phan can thiet.
    public KhoLuuTru(Path thuMuc, Consumer<String> log) throws IOException {
        this.thuMuc = thuMuc.toAbsolutePath().normalize();
        this.log = log;
        Files.createDirectories(this.thuMuc);
        napCauHinh();
    }

    // ===== CAU HINH ADMIN =====
    // admin.password la dau vao mot lan; chuyen ngay sang salt + hash tren dia.
    private void napCauHinh() throws IOException {
        Path tep = thuMuc.resolve("config.properties");
        if (Files.exists(tep)) {
            try (Reader doc = Files.newBufferedReader(tep, StandardCharsets.UTF_8)) { cauHinh.load(doc); }
        }
        cauHinh.putIfAbsent("admin.username", "admin");
        cauHinh.putIfAbsent("upload.maxBytes", "10485760");
        cauHinh.putIfAbsent("upload.maxRetries", "8");
        cauHinh.putIfAbsent("spam.burst", "5"); cauHinh.putIfAbsent("spam.minute", "20");
        cauHinh.putIfAbsent("multicast.portBase", "9000");
        if (cauHinh.containsKey("admin.password") || !cauHinh.containsKey("admin.passwordHash")) {
            String matKhau = cauHinh.getProperty("admin.password", "admin123");
            if (matKhau.length() < 6 || matKhau.length() > 128) {
                throw new IOException("Mật khẩu admin cần 6–128 ký tự");
            }
            String salt = BaoMat.ngauNhien(16);
            cauHinh.setProperty("admin.salt", salt);
            cauHinh.setProperty("admin.passwordHash", BaoMat.bam(salt, matKhau));
            cauHinh.remove("admin.password");
        }
        if (!cauHinh.containsKey("admin.salt")) { throw new IOException("Config thiếu admin.salt"); }
        if (BaoMat.dungMatKhau(cauHinh.getProperty("admin.salt"),
                cauHinh.getProperty("admin.passwordHash"), "admin123")) {
            log.accept("CẢNH BÁO: admin đang dùng admin123. Client phải đổi mật khẩu mặc định trước khi vào phòng.");
        }
        StringWriter ghi = new StringWriter();
        cauHinh.store(ghi, "UDPmultiChat - UTF-8; admin.password chi dung mot lan khi khoi dong");
        ghiAnToan(tep, ghi.toString());
    }

    // Cau hinh.
    public String cauHinh(String khoa) { return cauHinh.getProperty(khoa); }

    // Chup cac tep cua mot giao dich tai khoan truoc khi thay doi RAM/dia.
    synchronized BanLuu saoLuuTaiKhoan(String login) throws IOException {
        Map<Path,byte[]> tep=new LinkedHashMap<>();
        for (String ten:List.of("users.csv","accounts.properties","config.properties")) {
            Path p=thuMuc.resolve(ten); tep.put(p,Files.exists(p)?Files.readAllBytes(p):null);
        }
        if (!login.isEmpty()) {
            Path p=tepAnh(login); tep.put(p,Files.exists(p)?Files.readAllBytes(p):null);
        }
        return new BanLuu(tep,(Properties)cauHinh.clone());
    }
    // Chi khoi phuc tep da thay doi; loi phuc hoi duoc gan vao loi goc, khong nuot.
    synchronized void khoiPhucTaiKhoan(BanLuu ban,Throwable loi) {
        cauHinh.clear(); cauHinh.putAll(ban.cauHinh);
        for (var e:ban.tep.entrySet()) {
            try {
                byte[] hien=Files.exists(e.getKey())?Files.readAllBytes(e.getKey()):null;
                if (java.util.Arrays.equals(hien,e.getValue())) { continue; }
                if (e.getValue()==null) { Files.deleteIfExists(e.getKey()); }
                else {
                    Path tam=Files.createTempFile(thuMuc,"rollback-",".tmp");
                    try { Files.write(tam,e.getValue()); Files.move(tam,e.getKey(),StandardCopyOption.REPLACE_EXISTING); }
                    finally { Files.deleteIfExists(tam); }
                }
            } catch (IOException phucHoi) { loi.addSuppressed(phucHoi); }
        }
    }
    record BanLuu(Map<Path,byte[]> tep,Properties cauHinh) { }

    // Doc trang thai tai khoan/kiem duyet rieng, khong doi dinh dang CSV cu.
    synchronized Properties docThuocTinh(String ten) throws IOException {
        if (!ten.matches("accounts|moderation")) { throw new IOException("Tên kho không hợp lệ."); }
        Properties p=new Properties(); Path f=thuMuc.resolve(ten+".properties");
        if (Files.exists(f)) { try (Reader d=Files.newBufferedReader(f,StandardCharsets.UTF_8)) { p.load(d); } } return p;
    }
    // Ghi nguyen tu tren khoa cua kho.
    synchronized void ghiThuocTinh(String ten,Properties p) throws IOException {
        if (!ten.matches("accounts|moderation")) { throw new IOException("Tên kho không hợp lệ."); }
        StringWriter w=new StringWriter(); p.store(w,"UDP Chat UTF-8"); ghiAnToan(thuMuc.resolve(ten+".properties"),w.toString());
    }
    // Cap nhat config admin sau khi doi mat khau; khong luu mat khau tho.
    public synchronized void doiMatKhauAdmin(String salt,String hash) throws IOException {
        String cuSalt=cauHinh.getProperty("admin.salt"),cuHash=cauHinh.getProperty("admin.passwordHash");
        cauHinh.setProperty("admin.salt",salt); cauHinh.setProperty("admin.passwordHash",hash);
        try { StringWriter w=new StringWriter(); cauHinh.store(w,"UDPmultiChat - UTF-8"); ghiAnToan(thuMuc.resolve("config.properties"),w.toString()); }
        catch (IOException e) { cauHinh.setProperty("admin.salt",cuSalt); cauHinh.setProperty("admin.passwordHash",cuHash); throw e; }
    }
    // Ten tep anh chi lay tu ten tai khoan da chuan hoa, khong nhan duong dan Client.
    private Path tepAnh(String ten) throws IOException {
        if (!ten.matches("[a-z0-9_]{3,20}")) { throw new IOException("Tên tài khoản không hợp lệ."); }
        Path p=thuMuc.resolve("avatars"); Files.createDirectories(p); return p.resolve(ten+".png");
    }
    // Doc anh da luu; khong ton tai thi tra mang rong.
    public synchronized byte[] docAnh(String ten) throws IOException {
        Path p=tepAnh(ten); if (!Files.exists(p)) { return new byte[0]; }
        if (Files.size(p)>com.mycompany.udpmultichat.common.Protocol.ANH_PNG_TOI_DA) { throw new IOException("Avatar lưu trữ quá lớn."); }
        return Files.readAllBytes(p);
    }
    // Ghi anh qua tep tam cung thu muc roi thay the.
    public synchronized void ghiAnh(String ten,byte[] data) throws IOException {
        Path p=tepAnh(ten);
        if (data.length==0) { Files.deleteIfExists(p); return; }
        Path tam=p.resolveSibling(p.getFileName()+".tmp"); Files.write(tam,data);
        try { Files.move(tam,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tam,p,StandardCopyOption.REPLACE_EXISTING); }
    }

    // ===== CSV TAI KHOAN =====
    public synchronized Map<String, TaiKhoan> docNguoiDung() throws IOException {
        Map<String, TaiKhoan> ketQua = new LinkedHashMap<>();
        Path tep = thuMuc.resolve("users.csv");
        if (!Files.exists(tep)) { return ketQua; }
        List<String> dong = Files.readAllLines(tep, StandardCharsets.UTF_8);
        for (int i = 1; i < dong.size(); i++) {
            if (dong.get(i).isBlank()) { continue; }
            try {
                String[] p = tachCsv(dong.get(i));
                if ((p.length != 7 && p.length != 9) || ketQua.containsKey(p[1])) { throw new IllegalArgumentException(); }
                NguoiDung nguoi = new NguoiDung(p[0], p[1], p[2], VaiTro.valueOf(p[3]), p[4],p.length==9?p[7]:"",p.length==9?p[8]:"",false);
                ketQua.put(p[1], new TaiKhoan(nguoi, p[5], p[6]));
            } catch (RuntimeException e) { throw new IOException("users.csv sai ở dòng " + (i + 1), e); }
        }
        return ketQua;
    }

    // Tat ca du lieu tai khoan duoc ghi tren cung mot khoa.
    public synchronized void ghiNguoiDung(Collection<TaiKhoan> danhSach) throws IOException {
        StringBuilder noiDung = new StringBuilder("id,tenDangNhap,tenHienThi,vaiTro,ngayTao,salt,hash,gioiThieu,anhHash\n");
        for (TaiKhoan tk : danhSach) {
            List<String> truong = new ArrayList<>(List.of(tk.nguoi.id,tk.nguoi.tenDangNhap,tk.nguoi.tenHienThi,tk.nguoi.vaiTro.name(),tk.nguoi.ngayTao));
            truong.add(tk.salt);
            truong.add(tk.hash); truong.add(tk.nguoi.gioiThieu); truong.add(tk.nguoi.anhHash);
            noiDung.append(String.join(",", truong.stream().map(KhoLuuTru::csv).toList())).append('\n');
        }
        ghiAnToan(thuMuc.resolve("users.csv"), noiDung.toString());
    }

    // ===== PHONG VA LICH SU =====
    public synchronized List<String[]> docPhong() throws IOException {
        Path tep = thuMuc.resolve("rooms.csv");
        List<String[]> ketQua = new ArrayList<>();
        if (!Files.exists(tep)) { return ketQua; }
        List<String> dong = Files.readAllLines(tep, StandardCharsets.UTF_8);
        for (int i=1; i<dong.size(); i++) {
            if (!dong.get(i).isBlank()) { ketQua.add(tachCsv(dong.get(i))); }
        }
        return ketQua;
    }

    // Ghi phong.
    public synchronized void ghiPhong(List<String[]> danhSach) throws IOException {
        StringBuilder noiDung = new StringBuilder("id,ten,moTa,maPhong,chuId,nhom,cong,gioiHan,congKhai,coMatKhau,dangMo,soThanhVien,ngayTao,salt,hash,gioiThieu,anhHash\n");
        for (String[] p : danhSach) {
            noiDung.append(String.join(",", java.util.Arrays.stream(p).map(KhoLuuTru::csv).toList())).append('\n');
        }
        ghiAnToan(thuMuc.resolve("rooms.csv"), noiDung.toString());
    }

    // Chi nhan id do Server sinh, khong dung ten phong de tao duong dan.
    private Path tepTin(String roomId) throws IOException {
        if (!roomId.matches("sanh|[a-f0-9]{24}")) { throw new IOException("Mã lưu trữ phòng không hợp lệ"); }
        Path phong = thuMuc.resolve("rooms"); Files.createDirectories(phong);
        return phong.resolve(roomId + ".log");
    }

    // Doc tin.
    public synchronized List<TinNhan> docTin(String roomId) throws IOException {
        Path tep = tepTin(roomId);
        List<TinNhan> ketQua = new ArrayList<>();
        if (!Files.exists(tep)) { return ketQua; }
        try {
            for (String dong : Files.readAllLines(tep, StandardCharsets.UTF_8)) {
                if (dong.isBlank()) { continue; }
                TinNhan tin = TinNhan.tuTruong(Goi.tach(dong));
                if (!roomId.equals(tin.roomId) || tin.msgId != ketQua.size()+1L) {
                    throw new IllegalArgumentException("Sai roomId hoặc thứ tự tin");
                }
                ketQua.add(tin);
            }
        } catch (RuntimeException e) { throw new IOException("Lịch sử phòng bị lỗi: " + roomId, e); }
        return ketQua;
    }

    // Ghi mot ban tin, force xuong dia; neu ghi loi thi tra lai do dai cu.
    public synchronized void ghiTin(TinNhan tin) throws IOException {
        byte[] data = (tin.dongGoi()+"\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel kenh = FileChannel.open(tepTin(tin.roomId), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            long doDaiCu = kenh.size();
            try {
                ByteBuffer dem = ByteBuffer.wrap(data);
                while (dem.hasRemaining()) { kenh.write(dem); }
                kenh.force(false);
            } catch (IOException e) { kenh.truncate(doDaiCu); throw e; }
        }
    }

    // Ghi lai lich su qua tep tam khi thu hoi, noi dung cu khong con tren tep chinh.
    public synchronized void ghiLaiTin(String roomId,List<TinNhan> ds) throws IOException {
        StringBuilder s=new StringBuilder(); for (TinNhan t:ds) { s.append(t.dongGoi()).append('\n'); }
        ghiAnToan(tepTin(roomId),s.toString());
    }
    // Danh sach thanh vien ben vung, khong luu token hoac mat khau.
    public synchronized Properties docThanhVien() throws IOException {
        Properties p=new Properties(); Path tep=thuMuc.resolve("members.properties");
        if (Files.exists(tep)) {
            try (Reader doc=Files.newBufferedReader(tep,StandardCharsets.UTF_8)) { p.load(doc); }
        }
        return p;
    }
    // Ghi thanh vien.
    public synchronized void ghiThanhVien(Properties p) throws IOException {
        StringWriter ghi=new StringWriter(); p.store(ghi,"Thanh vien phong - UTF-8");
        ghiAnToan(thuMuc.resolve("members.properties"),ghi.toString());
    }
    // Doc biet danh theo phong: roomId.userId=bietDanh.
    public synchronized Properties docBietDanh() throws IOException {
        Properties p=new Properties(); Path tep=thuMuc.resolve("nicknames.properties");
        if (Files.exists(tep)) {
            try (Reader doc=Files.newBufferedReader(tep,StandardCharsets.UTF_8)) { p.load(doc); }
        }
        return p;
    }
    // Ghi biet danh theo phong.
    public synchronized void ghiBietDanh(Properties p) throws IOException {
        StringWriter ghi=new StringWriter(); p.store(ghi,"Biet danh thanh vien theo phong - UTF-8");
        ghiAnToan(thuMuc.resolve("nicknames.properties"),ghi.toString());
    }
    // Doc danh sach loi moi phong: inviteId=roomId|nguoiMoiId|nguoiDuocMoiId|thoiGianTao.
    public synchronized Properties docLoiMoi() throws IOException {
        Properties p=new Properties(); Path tep=thuMuc.resolve("invites.properties");
        if (Files.exists(tep)) {
            try (Reader doc=Files.newBufferedReader(tep,StandardCharsets.UTF_8)) { p.load(doc); }
        }
        return p;
    }
    // Ghi danh sach loi moi phong.
    public synchronized void ghiLoiMoi(Properties p) throws IOException {
        StringWriter ghi=new StringWriter(); p.store(ghi,"Loi moi phong - UTF-8");
        ghiAnToan(thuMuc.resolve("invites.properties"),ghi.toString());
    }

    // Csv.
    private static String csv(String giaTri) { return "\"" + giaTri.replace("\"", "\"\"") + "\""; }

    // Doc CSV co dau phay va dau nhay kep trong truong.
    private static String[] tachCsv(String dong) {
        List<String> ketQua = new ArrayList<>();
        StringBuilder truong = new StringBuilder();
        boolean trongNhay = false;
        for (int i = 0; i < dong.length(); i++) {
            char c = dong.charAt(i);
            if (c == '"') {
                if (trongNhay && i + 1 < dong.length() && dong.charAt(i + 1) == '"') {
                    truong.append('"'); i++;
                } else { trongNhay = !trongNhay; }
            } else if (c == ',' && !trongNhay) {
                ketQua.add(truong.toString()); truong.setLength(0);
            } else { truong.append(c); }
        }
        if (trongNhay) { throw new IllegalArgumentException("CSV thiếu dấu nháy"); }
        ketQua.add(truong.toString());
        return ketQua.toArray(String[]::new);
    }

    // Thay the cung thu muc, uu tien thao tac atomic tren he thong tep.
    private static void ghiAnToan(Path tep, String noiDung) throws IOException {
        Path tam = tep.resolveSibling(tep.getFileName() + ".tmp");
        Files.writeString(tam, noiDung, StandardCharsets.UTF_8);
        IOException loiCuoi=null;
        for (int lan=0;lan<6;lan++) {
            try {
                try { Files.move(tam,tep,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(tam,tep,StandardCopyOption.REPLACE_EXISTING); }
                return;
            } catch (java.nio.file.AccessDeniedException e) {
                // Windows co the khoa file rat ngan khi index/quet; retry co gioi han, khong bo loi.
                loiCuoi=e;
                try { Thread.sleep(40L*(lan+1)); }
                catch (InterruptedException ngat) { Thread.currentThread().interrupt(); throw new IOException("Đã ngắt ghi file",ngat); }
            }
        }
        throw new IOException("Không thay được file "+tep.getFileName()+": "+loiCuoi,loiCuoi);
    }

    /** Ban ghi rieng cua Server; tuyet doi khong gui salt/hash cho Client. */
    static final class TaiKhoan {
        final NguoiDung nguoi;
        final String salt, hash;
        TaiKhoan(NguoiDung nguoi, String salt, String hash) {
            this.nguoi = nguoi; this.salt = salt; this.hash = hash;
        }
    }
}
