package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.BaoMat;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.util.*;

/**
 * Vai trò: Quản lý vòng đời lời mời tham gia phòng (M3c.2):
 * - Tạo lời mời, lưu trữ bền vững (persist) qua restart.
 * - Kiểm soát quyền mời (chủ phòng / admin), sức chứa phòng, trạng thái tài khoản.
 * - Người được mời có quyền Chấp nhận hoặc Từ chối lời mời.
 * - Hạn chế mời trùng, tự động dọn lời mời hết hạn (7 ngày).
 */
public final class QuanLyLoiMoi {
    public static final long THOI_HAN_MS = 7L * 24 * 3600 * 1000; // 7 ngày

    /** DTO thông tin một lời mời phòng. */
    public record BanGhiLoiMoi(
            String id,
            String roomId,
            String nguoiMoiId,
            String nguoiDuocMoiId,
            long thoiGianTao
    ) {
        public boolean hetHan() {
            return System.currentTimeMillis() - thoiGianTao > THOI_HAN_MS;
        }

        public String dongGoi() {
            return roomId + "|" + nguoiMoiId + "|" + nguoiDuocMoiId + "|" + thoiGianTao;
        }

        public static BanGhiLoiMoi giaiMa(String id, String chuoi) {
            String[] p = chuoi.split("\\|", -1);
            if (p.length < 4) { return null; }
            try {
                return new BanGhiLoiMoi(id, p[0], p[1], p[2], Long.parseLong(p[3]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /** DTO hiển thị lời mời cho giao diện Client. */
    public record LoiMoiHienThi(
            String inviteId,
            String roomId,
            String tenPhong,
            String nguoiMoiId,
            String tenNguoiMoi,
            long thoiGianTao
    ) {}

    private final KhoLuuTru kho;
    private final Properties duLieuLoiMoi;

    public QuanLyLoiMoi(KhoLuuTru kho) throws IOException {
        this.kho = kho;
        this.duLieuLoiMoi = kho.docLoiMoi();
        donDepHetHan();
    }

    /**
     * Tự động dọn dẹp các lời mời đã hết hạn 7 ngày.
     */
    public synchronized void donDepHetHan() {
        long hienTai = System.currentTimeMillis();
        List<String> canXoa = new ArrayList<>();
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            String val = duLieuLoiMoi.getProperty(id);
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, val);
            if (bg == null || (hienTai - bg.thoiGianTao > THOI_HAN_MS)) {
                canXoa.add(id);
            }
        }
        if (!canXoa.isEmpty()) {
            canXoa.forEach(duLieuLoiMoi::remove);
            try { kho.ghiLoiMoi(duLieuLoiMoi); } catch (IOException ignored) {}
        }
    }

    /**
     * Gửi lời mời người dùng vào phòng.
     * Kiểm tra quyền người mời (chủ phòng / admin), kiểm tra phòng mở, chưa đầy,
     * người được mời hợp lệ, chưa là thành viên và chưa có lời mời đang chờ.
     */
    public synchronized BanGhiLoiMoi guiLoiMoi(Phien actor, String roomId, String targetUserId,
                                             QuanLyPhong quanLyPhong, QuanLyNguoiDung quanLyNguoiDung) throws IOException {
        Phong phong = quanLyPhong.lay(roomId);
        if (!phong.dangMo) {
            throw new IllegalArgumentException("Phòng đang đóng, không thể gửi lời mời.");
        }
        // Kiểm tra quyền: chỉ chủ phòng hoặc admin mới có quyền mời
        if (actor.nguoi.vaiTro != VaiTro.ADMIN && !actor.nguoi.id.equals(phong.chuId)) {
            throw new IllegalArgumentException("Chỉ chủ phòng hoặc quản trị viên mới có quyền mời thành viên.");
        }
        // Kiểm tra tài khoản được mời
        NguoiDung target = quanLyNguoiDung.timTheoId(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("Người dùng được mời không tồn tại.");
        }
        if (quanLyNguoiDung.biKhoa(target.id)) {
            throw new IllegalArgumentException("Tài khoản người dùng này đang bị khóa.");
        }
        // Đã là thành viên phòng chưa
        if (quanLyPhong.laThanhVien(target.id, roomId)) {
            throw new IllegalArgumentException(target.tenHienThi + " đã là thành viên của phòng này rồi.");
        }
        // Kiểm tra giới hạn thành viên phòng
        if (quanLyPhong.soThanhVienBenVung(roomId) >= phong.gioiHan) {
            throw new IllegalArgumentException("Phòng đã đạt giới hạn tối đa (" + phong.gioiHan + " người).");
        }
        // Kiểm tra trùng lời mời đang chờ
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, duLieuLoiMoi.getProperty(id));
            if (bg != null && !bg.hetHan() && bg.roomId.equals(roomId) && bg.nguoiDuocMoiId.equals(targetUserId)) {
                throw new IllegalArgumentException("Đã gửi lời mời đến " + target.tenHienThi + " rồi, đang chờ phản hồi.");
            }
        }

        // Tạo mã lời mời ngẫu nhiên
        String inviteId = "inv_" + BaoMat.ngauNhien(10);
        BanGhiLoiMoi moi = new BanGhiLoiMoi(inviteId, roomId, actor.nguoi.id, targetUserId, System.currentTimeMillis());
        duLieuLoiMoi.setProperty(inviteId, moi.dongGoi());
        kho.ghiLoiMoi(duLieuLoiMoi);
        return moi;
    }

    /**
     * Trả lời lời mời: Đồng ý (chấp nhận) hoặc Từ chối.
     */
    public synchronized BanGhiLoiMoi traLoiLoiMoi(Phien target, String inviteId, boolean dongY,
                                                  QuanLyPhong quanLyPhong) throws IOException {
        String val = duLieuLoiMoi.getProperty(inviteId);
        if (val == null) {
            throw new IllegalArgumentException("Lời mời không tồn tại hoặc đã hết hạn.");
        }
        BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(inviteId, val);
        if (bg == null || bg.hetHan()) {
            duLieuLoiMoi.remove(inviteId);
            kho.ghiLoiMoi(duLieuLoiMoi);
            throw new IllegalArgumentException("Lời mời đã hết hạn.");
        }
        if (!bg.nguoiDuocMoiId.equals(target.nguoi.id)) {
            throw new IllegalArgumentException("Lời mời này không dành cho bạn.");
        }

        // Luôn xóa lời mời sau khi trả lời
        duLieuLoiMoi.remove(inviteId);
        kho.ghiLoiMoi(duLieuLoiMoi);

        if (dongY) {
            Phong phong = quanLyPhong.lay(bg.roomId);
            if (!phong.dangMo) {
                throw new IllegalArgumentException("Phòng đã đóng, không thể tham gia.");
            }
            if (quanLyPhong.soThanhVienBenVung(bg.roomId) >= phong.gioiHan) {
                throw new IllegalArgumentException("Phòng đã đầy (" + phong.gioiHan + " người), không thể tham gia.");
            }
            // Thêm người dùng vào danh sách thành viên bền vững của phòng
            quanLyPhong.themThanhVienTrucTiep(target.nguoi.id, bg.roomId);
        }
        return bg;
    }

    /**
     * Hủy lời mời đang chờ: Chủ phòng, admin hoặc chính người được mời hủy.
     */
    public synchronized void huyLoiMoi(Phien actor, String inviteId, QuanLyPhong quanLyPhong) throws IOException {
        String val = duLieuLoiMoi.getProperty(inviteId);
        if (val == null) { return; }
        BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(inviteId, val);
        if (bg == null) {
            duLieuLoiMoi.remove(inviteId);
            kho.ghiLoiMoi(duLieuLoiMoi);
            return;
        }
        Phong phong = quanLyPhong.lay(bg.roomId);
        boolean laChu = phong != null && phong.chuId.equals(actor.nguoi.id);
        boolean laAdmin = actor.nguoi.vaiTro == VaiTro.ADMIN;
        boolean laNguoiNhan = bg.nguoiDuocMoiId.equals(actor.nguoi.id);
        boolean laNguoiMoi = bg.nguoiMoiId.equals(actor.nguoi.id);

        if (!laChu && !laAdmin && !laNguoiNhan && !laNguoiMoi) {
            throw new IllegalArgumentException("Bạn không có quyền hủy lời mời này.");
        }
        duLieuLoiMoi.remove(inviteId);
        kho.ghiLoiMoi(duLieuLoiMoi);
    }

    /**
     * Lấy danh sách các lời mời đang chờ gửi đến một người dùng cụ thể.
     */
    public synchronized List<LoiMoiHienThi> danhSachLoiMoiCho(String userId, QuanLyPhong quanLyPhong, QuanLyNguoiDung quanLyNguoiDung) {
        List<LoiMoiHienThi> ketQua = new ArrayList<>();
        long hienTai = System.currentTimeMillis();
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, duLieuLoiMoi.getProperty(id));
            if (bg != null && !bg.hetHan() && bg.nguoiDuocMoiId.equals(userId)) {
                try {
                    Phong r = quanLyPhong.lay(bg.roomId);
                    NguoiDung nguoiMoi = quanLyNguoiDung.timTheoId(bg.nguoiMoiId);
                    String tenMoi = (nguoiMoi != null) ? nguoiMoi.tenHienThi : "Thành viên";
                    ketQua.add(new LoiMoiHienThi(bg.id, bg.roomId, r.ten, bg.nguoiMoiId, tenMoi, bg.thoiGianTao));
                } catch (Exception ignored) {
                    // Phòng có thể đã bị xóa
                }
            }
        }
        ketQua.sort(Comparator.comparingLong(LoiMoiHienThi::thoiGianTao).reversed());
        return ketQua;
    }

    /**
     * Lấy danh sách lời mời đang chờ trong một phòng (cho chủ phòng xem/hủy).
     */
    public synchronized List<BanGhiLoiMoi> danhSachTheoPhong(String roomId) {
        List<BanGhiLoiMoi> ketQua = new ArrayList<>();
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, duLieuLoiMoi.getProperty(id));
            if (bg != null && !bg.hetHan() && bg.roomId.equals(roomId)) {
                ketQua.add(bg);
            }
        }
        return ketQua;
    }

    /** Xóa các lời mời liên quan khi một phòng bị xóa. */
    public synchronized void xoaTheoPhong(String roomId) throws IOException {
        List<String> canXoa = new ArrayList<>();
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, duLieuLoiMoi.getProperty(id));
            if (bg != null && bg.roomId.equals(roomId)) {
                canXoa.add(id);
            }
        }
        if (!canXoa.isEmpty()) {
            canXoa.forEach(duLieuLoiMoi::remove);
            kho.ghiLoiMoi(duLieuLoiMoi);
        }
    }

    /** Xóa các lời mời liên quan khi một tài khoản bị xóa. */
    public synchronized void xoaTheoUser(String userId) throws IOException {
        List<String> canXoa = new ArrayList<>();
        for (String id : duLieuLoiMoi.stringPropertyNames()) {
            BanGhiLoiMoi bg = BanGhiLoiMoi.giaiMa(id, duLieuLoiMoi.getProperty(id));
            if (bg != null && (bg.nguoiMoiId.equals(userId) || bg.nguoiDuocMoiId.equals(userId))) {
                canXoa.add(id);
            }
        }
        if (!canXoa.isEmpty()) {
            canXoa.forEach(duLieuLoiMoi::remove);
            kho.ghiLoiMoi(duLieuLoiMoi);
        }
    }
}
