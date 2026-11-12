package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.BaoMat;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.io.IOException;
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

/** Luu tai khoan va config UTF-8; ghi file tam roi thay the de tranh file do dang. */
public final class KhoLuuTru {
    private final Path thuMuc;
    private final Consumer<String> log;
    private final Properties cauHinh = new Properties();

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
        if (cauHinh.containsKey("admin.password") || !cauHinh.containsKey("admin.passwordHash")) {
            String matKhau = cauHinh.getProperty("admin.password", "admin123");
            if (matKhau.length() < 6 || matKhau.length() > 128) {
                throw new IOException("Mat khau admin can 6-128 ky tu");
            }
            String salt = BaoMat.ngauNhien(16);
            cauHinh.setProperty("admin.salt", salt);
            cauHinh.setProperty("admin.passwordHash", BaoMat.bam(salt, matKhau));
            cauHinh.remove("admin.password");
        }
        if (!cauHinh.containsKey("admin.salt")) { throw new IOException("Config thieu admin.salt"); }
        if (BaoMat.dungMatKhau(cauHinh.getProperty("admin.salt"),
                cauHinh.getProperty("admin.passwordHash"), "admin123")) {
            log.accept("CANH BAO: admin dang dung admin123. Hay doi mat khau trong config (xem README).");
        }
        StringWriter ghi = new StringWriter();
        cauHinh.store(ghi, "UDPmultiChat - UTF-8; admin.password chi dung mot lan khi khoi dong");
        ghiAnToan(tep, ghi.toString());
    }

    public String cauHinh(String khoa) { return cauHinh.getProperty(khoa); }

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
                if (p.length != 7 || ketQua.containsKey(p[1])) { throw new IllegalArgumentException(); }
                NguoiDung nguoi = new NguoiDung(p[0], p[1], p[2], VaiTro.valueOf(p[3]), p[4]);
                ketQua.put(p[1], new TaiKhoan(nguoi, p[5], p[6]));
            } catch (RuntimeException e) { throw new IOException("users.csv sai o dong " + (i + 1), e); }
        }
        return ketQua;
    }

    // Tat ca du lieu tai khoan duoc ghi tren cung mot khoa.
    public synchronized void ghiNguoiDung(Collection<TaiKhoan> danhSach) throws IOException {
        StringBuilder noiDung = new StringBuilder("id,tenDangNhap,tenHienThi,vaiTro,ngayTao,salt,hash\n");
        for (TaiKhoan tk : danhSach) {
            List<String> truong = new ArrayList<>(List.of(tk.nguoi.cacTruong()));
            truong.add(tk.salt);
            truong.add(tk.hash);
            noiDung.append(String.join(",", truong.stream().map(KhoLuuTru::csv).toList())).append('\n');
        }
        ghiAnToan(thuMuc.resolve("users.csv"), noiDung.toString());
    }

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
        if (trongNhay) { throw new IllegalArgumentException("CSV thieu dau nhay"); }
        ketQua.add(truong.toString());
        return ketQua.toArray(String[]::new);
    }

    // Thay the cung thu muc, uu tien thao tac atomic tren he thong tep.
    private static void ghiAnToan(Path tep, String noiDung) throws IOException {
        Path tam = tep.resolveSibling(tep.getFileName() + ".tmp");
        Files.writeString(tam, noiDung, StandardCharsets.UTF_8);
        try { Files.move(tam, tep, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) {
            Files.move(tam, tep, StandardCopyOption.REPLACE_EXISTING);
        }
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
