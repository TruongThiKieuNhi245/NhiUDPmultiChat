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

/** Gop tai khoan va phien; Server la noi quyet dinh quyen va online. */
public final class QuanLyNguoiDung {
    private final KhoLuuTru kho;
    private final Map<String, TaiKhoan> taiKhoan;
    private final Map<String, Phien> phien = new LinkedHashMap<>();

    public QuanLyNguoiDung(KhoLuuTru kho) throws IOException {
        this.kho = kho;
        taiKhoan = kho.docNguoiDung();
        String tenAdmin = chuanHoaTen(kho.cauHinh("admin.username"));
        TaiKhoan cu = taiKhoan.get(tenAdmin);
        NguoiDung admin = cu == null ? new NguoiDung(BaoMat.ngauNhien(12), tenAdmin, "Admin",
                VaiTro.ADMIN, Instant.now().toString()) : new NguoiDung(cu.nguoi.id, tenAdmin,
                cu.nguoi.tenHienThi, VaiTro.ADMIN, cu.nguoi.ngayTao);
        taiKhoan.put(tenAdmin, new TaiKhoan(admin, kho.cauHinh("admin.salt"), kho.cauHinh("admin.passwordHash")));
        kho.ghiNguoiDung(taiKhoan.values());
    }

    // ===== DANG KY / DANG NHAP =====
    public synchronized NguoiDung dangKy(String ten, String matKhau) throws IOException {
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

    public synchronized Phien dangNhap(String ten, String matKhau, InetSocketAddress diaChi) {
        ten = chuanHoaTen(ten);
        kiemTraMatKhau(matKhau);
        TaiKhoan tk = taiKhoan.get(ten);
        if (tk == null || !BaoMat.dungMatKhau(tk.salt, tk.hash, matKhau)) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu");
        }
        for (Phien p : phien.values()) {
            if (p.nguoi.id.equals(tk.nguoi.id)) {
                throw new IllegalArgumentException("Tài khoản đang đăng nhập ở nơi khác; chờ hết phiên nếu mất mạng");
            }
        }
        Phien moi = new Phien(BaoMat.ngauNhien(24), tk.nguoi, diaChi);
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

    // ===== HEARTBEAT =====
    public synchronized void ping(Phien p) { p.pingCuoi = System.nanoTime(); }
    public synchronized void dangXuat(Phien p) { phien.remove(p.token); }
    public synchronized List<Phien> danhSach() { return new ArrayList<>(phien.values()); }
    public synchronized int tongNguoiDung() { return taiKhoan.size(); }

    public synchronized List<Phien> xoaHetHan() {
        long hienTai = System.nanoTime();
        List<Phien> hetHan = phien.values().stream()
                .filter(p -> hienTai - p.pingCuoi > Protocol.HET_PHIEN_MS * 1_000_000L).toList();
        hetHan.forEach(p -> phien.remove(p.token));
        return hetHan;
    }

    private static String chuanHoaTen(String ten) {
        String ketQua = ten.trim().toLowerCase(Locale.ROOT);
        if (!ketQua.matches("[a-z0-9_]{3,20}")) {
            throw new IllegalArgumentException("Tên cần 3–20 chữ cái a-z, số hoặc dấu gạch dưới");
        }
        return ketQua;
    }

    private static void kiemTraMatKhau(String matKhau) {
        if (matKhau.length() < 6 || matKhau.length() > 128) {
            throw new IllegalArgumentException("Mật khẩu cần 6–128 ký tự");
        }
    }

    /** Phien chi ton tai trong RAM, duoc xoa khi dang xuat hoac het heartbeat. */
    public static final class Phien {
        final String token;
        final NguoiDung nguoi;
        final InetSocketAddress diaChi;
        long pingCuoi = System.nanoTime();
        boolean sanSang;
        final java.util.Set<String> phong = new java.util.LinkedHashSet<>();
        final Map<String, Long> choVao = new LinkedHashMap<>();
        Phien(String token, NguoiDung nguoi, InetSocketAddress diaChi) {
            this.token = token; this.nguoi = nguoi; this.diaChi = diaChi;
        }
    }
}
