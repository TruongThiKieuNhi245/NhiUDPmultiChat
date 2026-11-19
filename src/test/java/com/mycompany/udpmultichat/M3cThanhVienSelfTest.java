package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.KhoLuuTru;
import com.mycompany.udpmultichat.server.QuanLyLoiMoi;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import com.mycompany.udpmultichat.server.QuanLyPhong;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Kiểm thử tự động M3c.2 (Thành viên & Biệt danh phòng):
 * 1. Đặt biệt danh riêng theo phòng (Room Nicknames):
 *    - Người dùng tự đổi biệt danh của mình.
 *    - Chủ phòng / admin đổi biệt danh cho thành viên.
 *    - Chủ phòng không được đổi biệt danh của admin.
 *    - Thành viên thường không được đổi biệt danh của người khác.
 *    - Xóa biệt danh khôi phục tên hiển thị gốc.
 *    - Biệt danh độc lập giữa các phòng khác nhau của cùng một user.
 *    - Lưu trữ bền vững (Persistence) qua restart.
 * 2. Lời mời thành viên tham gia phòng (Room Invitations):
 *    - Chủ phòng / admin mời user hợp lệ vào phòng.
 *    - Chặn mời người đã là thành viên, chặn mời trùng lời mời đang chờ.
 *    - Chặn thành viên thường tự ý gửi lời mời nếu không có quyền.
 *    - Người nhận Chấp nhận lời mời -> Trở thành thành viên chính thức.
 *    - Người nhận Từ chối lời mời -> Bị hủy, không vào phòng.
 *    - Hủy lời mời đang chờ bởi chủ phòng/admin.
 *    - Lưu trữ bền vững qua restart.
 * 3. Tắt / Bật thông báo phòng (Client-side Notification Muting).
 */
public final class M3cThanhVienSelfTest {
    public static void main(String[] args) throws Exception {
        testBietDanhPhong();
        testLoiMoiPhong();
        testTatThongBao();
        System.out.println("KET QUA M3c.2 Thanh Vien & Biet Danh: ALL PASS, 0 FAIL");
    }

    private static void testBietDanhPhong() throws Exception {
        Path dir = Files.createTempDirectory("test-m3c-nick");
        try {
            KhoLuuTru kho = new KhoLuuTru(dir, s -> {});
            QuanLyNguoiDung qlnd = new QuanLyNguoiDung(kho);
            QuanLyPhong qlp = new QuanLyPhong(kho, qlnd::danhSach);

            // Đăng ký tài khoản (admin đã có sẵn)
            qlnd.dangKy("user1", "pass123");
            qlnd.dangKy("user2", "pass123");

            Phien pAdmin = qlnd.dangNhap("admin", "admin123", new InetSocketAddress("127.0.0.1", 1001));
            Phien pUser1 = qlnd.dangNhap("user1", "pass123", new InetSocketAddress("127.0.0.1", 1002));
            Phien pUser2 = qlnd.dangNhap("user2", "pass123", new InetSocketAddress("127.0.0.1", 1003));

            // User1 tạo phòng P1
            Phong phong1 = qlp.tao(pUser1, "Phòng Dự Án 1", "Mô tả dự án", true, "", 50);
            qlp.datCho(pUser1, phong1.id, "");
            qlp.xacNhanVao(pUser1, phong1.id);

            // User2 và Admin vào phòng P1
            qlp.datCho(pUser2, phong1.id, "");
            qlp.xacNhanVao(pUser2, phong1.id);
            qlp.datCho(pAdmin, phong1.id, "");
            qlp.xacNhanVao(pAdmin, phong1.id);

            // 1. User1 tự đổi biệt danh của chính mình
            String nick1 = qlp.datBietDanh(pUser1, phong1.id, pUser1.nguoi.id, "Trưởng Ban", qlnd);
            can("Trưởng Ban".equals(nick1), "Lỗi đặt biệt danh cho chính mình");
            can("Trưởng Ban".equals(qlp.layBietDanh(phong1.id, pUser1.nguoi.id)), "Sai biệt danh đã lưu");

            // 2. User1 (chủ phòng) đổi biệt danh cho User2
            String nick2 = qlp.datBietDanh(pUser1, phong1.id, pUser2.nguoi.id, "Thư Ký Dự Án", qlnd);
            can("Thư Ký Dự Án".equals(nick2), "Lỗi chủ phòng đổi biệt danh cho thành viên");
            can("Thư Ký Dự Án".equals(qlp.layBietDanh(phong1.id, pUser2.nguoi.id)), "Sai biệt danh User2");

            // 3. User1 (chủ phòng) KHÔNG được đổi biệt danh của Admin
            boolean chanAdmin = false;
            try {
                qlp.datBietDanh(pUser1, phong1.id, pAdmin.nguoi.id, "Không Được Đổi", qlnd);
            } catch (IllegalArgumentException e) {
                chanAdmin = true;
            }
            can(chanAdmin, "Không chặn chủ phòng đổi biệt danh của admin");

            // 4. User2 (thành viên thường) KHÔNG được đổi biệt danh của User1 (chủ phòng)
            boolean chanUser2 = false;
            try {
                qlp.datBietDanh(pUser2, phong1.id, pUser1.nguoi.id, "Đổi Trộm", qlnd);
            } catch (IllegalArgumentException e) {
                chanUser2 = true;
            }
            can(chanUser2, "Không chặn thành viên thường đổi biệt danh người khác");

            // 5. Xóa biệt danh (truyền rỗng "")
            String nickXoa = qlp.datBietDanh(pUser1, phong1.id, pUser1.nguoi.id, "", qlnd);
            can(nickXoa.isEmpty(), "Lỗi xóa biệt danh");
            can(qlp.layBietDanh(phong1.id, pUser1.nguoi.id).isEmpty(), "Biệt danh chưa bị xóa");

            // 6. Tính độc lập biệt danh giữa các phòng khác nhau
            Phong phong2 = qlp.tao(pUser2, "Phòng Gia Đình", "Gia đình", true, "", 20);
            qlp.datCho(pUser2, phong2.id, "");
            qlp.xacNhanVao(pUser2, phong2.id);
            qlp.datCho(pUser1, phong2.id, "");
            qlp.xacNhanVao(pUser1, phong2.id);

            // Ở phòng 2, User1 đặt biệt danh là "Con Út"
            qlp.datBietDanh(pUser1, phong2.id, pUser1.nguoi.id, "Con Út", qlnd);
            can("Con Út".equals(qlp.layBietDanh(phong2.id, pUser1.nguoi.id)), "Sai biệt danh phòng 2");
            can(qlp.layBietDanh(phong1.id, pUser1.nguoi.id).isEmpty(), "Phòng 1 bị ảnh hưởng biệt danh phòng 2");

            // 7. Lưu trữ bền vững (Persistence) qua restart
            QuanLyPhong qlpMoi = new QuanLyPhong(kho, qlnd::danhSach);
            can("Con Út".equals(qlpMoi.layBietDanh(phong2.id, pUser1.nguoi.id)), "Mất biệt danh User1 sau restart");
            can("Thư Ký Dự Án".equals(qlpMoi.layBietDanh(phong1.id, pUser2.nguoi.id)), "Mất biệt danh User2 sau restart");

            System.out.println("PASS: testBietDanhPhong");
        } finally {
            xoaThuMuc(dir);
        }
    }

    private static void testLoiMoiPhong() throws Exception {
        Path dir = Files.createTempDirectory("test-m3c-invite");
        try {
            KhoLuuTru kho = new KhoLuuTru(dir, s -> {});
            QuanLyNguoiDung qlnd = new QuanLyNguoiDung(kho);
            QuanLyPhong qlp = new QuanLyPhong(kho, qlnd::danhSach);
            QuanLyLoiMoi qllm = new QuanLyLoiMoi(kho);

            qlnd.dangKy("chu", "pass123");
            qlnd.dangKy("tv_moi", "pass123");
            qlnd.dangKy("tv_khac", "pass123");

            Phien pChu = qlnd.dangNhap("chu", "pass123", new InetSocketAddress("127.0.0.1", 2001));
            Phien pMoi = qlnd.dangNhap("tv_moi", "pass123", new InetSocketAddress("127.0.0.1", 2002));
            Phien pKhac = qlnd.dangNhap("tv_khac", "pass123", new InetSocketAddress("127.0.0.1", 2003));

            Phong phong = qlp.tao(pChu, "Nhóm Học Tập", "Học tập", true, "", 10);
            qlp.datCho(pChu, phong.id, "");
            qlp.xacNhanVao(pChu, phong.id);

            // 1. Chủ phòng gửi lời mời tới pMoi
            var lm1 = qllm.guiLoiMoi(pChu, phong.id, pMoi.nguoi.id, qlp, qlnd);
            can(lm1 != null && lm1.id() != null, "Khong tao duoc loi moi");
            can(lm1.roomId().equals(phong.id), "Sai roomId loi moi");

            // 2. Chặn mời trùng khi đang có lời mời chờ
            boolean chanTrung = false;
            try {
                qllm.guiLoiMoi(pChu, phong.id, pMoi.nguoi.id, qlp, qlnd);
            } catch (IllegalArgumentException e) {
                chanTrung = true;
            }
            can(chanTrung, "Khong chan moi trung nguoi dung");

            // 3. Chặn thành viên không có quyền mời
            boolean chanQuyen = false;
            try {
                qllm.guiLoiMoi(pKhac, phong.id, pMoi.nguoi.id, qlp, qlnd);
            } catch (IllegalArgumentException e) {
                chanQuyen = true;
            }
            can(chanQuyen, "Khong chan nguoi ngoai moi thanh vien");

            // 4. Kiểm tra danh sách lời mời của pMoi
            var dsMoi = qllm.danhSachLoiMoiCho(pMoi.nguoi.id, qlp, qlnd);
            can(dsMoi.size() == 1, "Sai so luong loi moi cho pMoi");
            can(dsMoi.get(0).tenPhong().equals("Nhóm Học Tập"), "Sai ten phong loi moi");

            // 5. pMoi từ chối lời mời
            var bgTuChoi = qllm.traLoiLoiMoi(pMoi, lm1.id(), false, qlp);
            can(bgTuChoi != null, "Loi tu choi loi moi");
            can(!qlp.laThanhVien(pMoi.nguoi.id, phong.id), "User da vao phong du da tu choi");
            can(qllm.danhSachLoiMoiCho(pMoi.nguoi.id, qlp, qlnd).isEmpty(), "Loi moi van con sau khi tu choi");

            // 6. Gửi lại lời mời và pMoi Chấp nhận
            var lm2 = qllm.guiLoiMoi(pChu, phong.id, pMoi.nguoi.id, qlp, qlnd);
            var bgDongY = qllm.traLoiLoiMoi(pMoi, lm2.id(), true, qlp);
            can(bgDongY != null, "Loi chap nhan loi moi");
            can(qlp.laThanhVien(pMoi.nguoi.id, phong.id), "User chua tro thanh thanh vien sau khi chap nhan");

            // 7. Chặn mời người đã là thành viên
            boolean chanDaVao = false;
            try {
                qllm.guiLoiMoi(pChu, phong.id, pMoi.nguoi.id, qlp, qlnd);
            } catch (IllegalArgumentException e) {
                chanDaVao = true;
            }
            can(chanDaVao, "Khong chan moi nguoi da la thanh vien");

            // 8. Hủy lời mời
            var lm3 = qllm.guiLoiMoi(pChu, phong.id, pKhac.nguoi.id, qlp, qlnd);
            can(qllm.danhSachLoiMoiCho(pKhac.nguoi.id, qlp, qlnd).size() == 1, "Khong co loi moi cho pKhac");
            qllm.huyLoiMoi(pChu, lm3.id(), qlp);
            can(qllm.danhSachLoiMoiCho(pKhac.nguoi.id, qlp, qlnd).isEmpty(), "Loi moi pKhac chua bi huy");

            // 9. Lưu trữ bền vững (Persistence) qua restart
            var lm4 = qllm.guiLoiMoi(pChu, phong.id, pKhac.nguoi.id, qlp, qlnd);
            QuanLyLoiMoi qllmMoi = new QuanLyLoiMoi(kho);
            var dsRestart = qllmMoi.danhSachLoiMoiCho(pKhac.nguoi.id, qlp, qlnd);
            can(dsRestart.size() == 1, "Mat loi moi pKhac sau restart");
            can(dsRestart.get(0).inviteId().equals(lm4.id()), "Sai id loi moi sau restart");

            System.out.println("PASS: testLoiMoiPhong");
        } finally {
            xoaThuMuc(dir);
        }
    }

    private static void testTatThongBao() {
        // Tạo listener dummy để kiểm tra ClientCore
        com.mycompany.udpmultichat.client.Listener dummy = new com.mycompany.udpmultichat.client.Listener() {
            @Override public void onTinNhan(com.mycompany.udpmultichat.common.model.TinNhan tin, boolean laToi) {}
            @Override public void onHeThong(String thongBao) {}
            @Override public void onDanhSachOnline(List<String> danhSach) {}
            @Override public void onLoi(String loi) {}
            @Override public void onMatKetNoi(String lyDo) {}
        };
        ClientCore core = new ClientCore(dummy);
        can(!core.daTatThongBao("room-1"), "Mac dinh phong chua tat thong bao");

        core.batTatThongBao("room-1", true);
        can(core.daTatThongBao("room-1"), "Phong chua duoc bat tat thong bao");

        core.batTatThongBao("room-1", false);
        can(!core.daTatThongBao("room-1"), "Phong chua duoc bo tat thong bao");

        core.close();
        System.out.println("PASS: testTatThongBao");
    }

    private static void can(boolean dk, String lyDo) {
        if (!dk) { throw new AssertionError(lyDo); }
    }

    private static void xoaThuMuc(Path p) {
        try {
            if (Files.exists(p)) {
                try (var s = Files.walk(p)) {
                    s.sorted(java.util.Comparator.reverseOrder()).forEach(f -> {
                        try { Files.deleteIfExists(f); } catch (IOException ignored) {}
                    });
                }
            }
        } catch (IOException ignored) {}
    }
}
