package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.*;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Vai tro: Cua so admin nam tab, moi tac vu mang chay nen.
 * Ai goi: ChatMulticastUI, QuanTriPreview. Goi: QuanTriClient, BangQuanTri, FormQuanTri, BaoCaoPanel.
 */
@SuppressWarnings("serial")
final class QuanTriFrame extends JFrame {
    private final ClientCore core;
    private final JTabbedPane tabs = new JTabbedPane();
    private final TongQuanPanel tong = new TongQuanPanel(this::thongBao);
    private final BangQuanTri users = new BangQuanTri(
            new String[] { "Tài khoản", "Tên hiển thị", "Vai trò", "Trạng thái", "Số phòng", "Số tin", "Ngày tạo", "Đăng nhập cuối" },
            new int[] { 1, 2, 3, 4, 5, 6, 7, 8 },
            new String[] { "Thêm", "Sửa", "Đặt lại mật khẩu", "Khóa/Mở khóa", "Xóa", "Kick", "Mute toàn hệ thống", "Gỡ cấm chat toàn hệ thống", "Xuất danh sách" },
            this::user, true);
    private final BangQuanTri rooms = new BangQuanTri(
            new String[] { "Mã", "Tên", "Loại", "Trạng thái", "Chủ phòng", "Thành viên", "Tin", "Tệp", "Dung lượng", "Ngày tạo" },
            new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
            new String[] { "Tạo phòng", "Sửa", "Đóng/Mở", "Xóa", "Thành viên", "Xóa lịch sử", "Xuất danh sách" },
            this::room, false);
    private final BangQuanTri audit = new BangQuanTri(
            new String[] { "Thời gian", "Người thực hiện", "Hành động", "Đối tượng", "Chi tiết" },
            new int[] { 0, 1, 2, 3, 4 },
            new String[] { "Xuất nhật ký" },
            this::auditAction, false);
    private final JLabel trangThai = Theme.nhan("Chưa tải dữ liệu", false);
    private final ThongBao toast = new ThongBao(getLayeredPane());
    private final javax.swing.Timer timer = new javax.swing.Timer(4000, e -> nap());
    private AnhChupQuanTri snapshot;
    private boolean dangTai;

    // Giao dien chi duoc mo cho admin; Server van kiem tra moi request.
    QuanTriFrame(ClientCore core, boolean preview) {
        this.core = core;
        setTitle("UDP Chat — Quản trị");
        setIconImage(Theme.iconUngDung());
        setSize(1280, 860);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        tabs.addTab("Tổng quan", tong);
        tabs.addTab("Người dùng", users);
        tabs.addTab("Phòng", rooms);
        tabs.addTab("Nhật ký", audit);
        tabs.addTab("Báo cáo", new BaoCaoPanel(() -> snapshot, this::loi, toast, this::nap));
        JPanel goc = new JPanel(new BorderLayout(0, Theme.S8));
        JPanel tren = new JPanel(new BorderLayout());
        tren.setBorder(Theme.le(Theme.S12));
        tren.add(trangThai);
        JButton fresh = Theme.nut("Làm mới", false);
        fresh.addActionListener(e -> nap());
        tren.add(fresh, BorderLayout.EAST);
        goc.add(tren, BorderLayout.NORTH);
        goc.add(tabs);
        setContentPane(goc);
        toast.datVung(goc);
        if (!preview) {
            core.quanTri().khiDoi(() -> SwingUtilities.invokeLater(this::nap));
            timer.start();
            nap();
        }
    }

    // ===== TAI SNAPSHOT VA CAC TRANG THAI =====
    void nap() {
        if (dangTai || core == null || !isDisplayable()) return;
        if (core.nguoiDung() == null || core.nguoiDung().vaiTro != VaiTro.ADMIN) {
            dispose();
            return;
        }
        dangTai = true;
        trangThai.setText("Đang tải dữ liệu...");
        users.trangThai("Đang tải...", false);
        rooms.trangThai("Đang tải...", false);
        audit.trangThai("Đang tải...", false);
        core.quanTri().lay().whenComplete((a, e) -> SwingUtilities.invokeLater(() -> {
            dangTai = false;
            if (!isDisplayable()) return;
            if (e != null) {
                String loi = "Không tải được, thử lại: " + ClientCore.loiDeHieu(e);
                trangThai.setText(loi);
                users.trangThai(loi, true);
                rooms.trangThai(loi, true);
                audit.trangThai(loi, true);
            } else {
                capNhat(a);
            }
        }));
    }

    // Cac tab deu dung mot snapshot, khong tron nhieu moc thoi gian.
    void capNhat(AnhChupQuanTri a) {
        snapshot = a;
        tong.capNhat(a);
        users.duLieu(a.lay("USERS"));
        rooms.duLieu(a.lay("ROOMS"));
        audit.duLieu(a.lay("AUDIT"));
        trangThai.setText("Dữ liệu tự cập nhật · " + java.time.Instant.ofEpochMilli(a.luc())
                .atZone(java.time.ZoneId.systemDefault()).toLocalTime().withNano(0));
    }

    // Preview doi tab, khong khoi tao socket hay Server.
    void xemTab(int i) {
        tabs.setSelectedIndex(i);
    }

    // Fixture preview dung chinh trang thai bang ma callback mang hien trong ung dung.
    void trangThaiMau(String text, boolean rong, boolean loi) {
        if (rong) users.duLieu(java.util.List.of());
        users.trangThai(text, loi);
        trangThai.setText(text);
        xemTab(1);
    }

    // Goi lenh nen va chi dong form khi Server xac nhan.
    private void lenh(FormQuanTri form, String action, String... args) {
        if (core == null) return;
        core.quanTri().lenh(action, args).whenComplete((k, e) -> SwingUtilities.invokeLater(() -> {
            if (e != null) {
                if (form != null) form.loi(ClientCore.loiDeHieu(e));
                else loi(ClientCore.loiDeHieu(e));
                return;
            }
            if (form != null) form.dong();
            if (action.equals("USER_RESET")) {
                matKhauTam(k);
            } else if (action.equals("BROADCAST")) {
                toast.hien(k.contains("lỗi lưu") ? ThongBao.Muc.CANH_BAO : ThongBao.Muc.THANH_CONG, k);
            } else {
                toast.hien(ThongBao.Muc.THANH_CONG, "Đã cập nhật.");
            }
            nap();
        }));
    }

    // ===== NGUOI DUNG =====
    private void user(String l) {
        var n = users.chon();
        if (!l.equals("Thêm") && !l.equals("Xuất danh sách") && n.isEmpty()) {
            loi("Hãy chọn người dùng.");
            return;
        }
        switch (l) {
            case "Thêm":
            case "Sửa":
                boolean them = l.equals("Thêm");
                FormQuanTri f = FormQuanTri.nguoiDung(them, n);
                f.hien(this, l + " người dùng", m -> {
                    if (them) lenh(f, "USER_ADD", m.get("login"), m.get("ten"), m.get("vai"), m.get("mk"));
                    else lenh(f, "USER_EDIT", n.get(0), m.get("ten"), m.get("gt"), m.get("vai"));
                });
                break;
            case "Đặt lại mật khẩu":
                xacNhan("Đặt lại mật khẩu", n.get(1),
                        "Phiên hiện tại bị ngắt. Người dùng phải đổi mật khẩu tạm ở lần đăng nhập kế.",
                        f2 -> lenh(f2, "USER_RESET", n.get(0)));
                break;
            case "Khóa/Mở khóa":
                boolean khoa = !n.get(4).equals("Bị khóa");
                FormQuanTri f3 = new FormQuanTri(khoa ? "Khóa" : "Mở khóa", khoa)
                        .moTa(n.get(1), khoa ? "Tài khoản sẽ bị ngắt phiên ngay và không đăng nhập được." : "Cho phép tài khoản đăng nhập lại.")
                        .text("lydo", "Lý do", "");
                f3.hien(this, khoa ? "Khóa tài khoản" : "Mở khóa", m -> lenh(f3, "USER_LOCK", n.get(0), "" + khoa, m.get("lydo")));
                break;
            case "Xóa":
                FormQuanTri xoa = new FormQuanTri("Xóa tài khoản", true)
                        .moTa(n.get(1), "Ngắt phiên, gỡ khỏi phòng, xóa avatar. Tin cũ được giữ với tên Người dùng đã xóa.")
                        .text("ten", "Gõ lại tên đăng nhập", "")
                        .kiemTra(m -> m.get("ten").equals(n.get(1)) ? "" : "ten|Tên xác nhận chưa khớp.");
                xoa.hien(this, "Xóa tài khoản", m -> lenh(xoa, "USER_DELETE", n.get(0), m.get("ten")));
                break;
            case "Mute toàn hệ thống":
                KiemDuyetUI.thaoTac(this, core, "*", n.get(0), n.get(2), "Cấm chat", this::loi);
                break;
            case "Gỡ cấm chat toàn hệ thống":
                KiemDuyetUI.thaoTac(this, core, "*", n.get(0), n.get(2), "Gỡ cấm chat", this::loi);
                break;
            case "Kick":
                chonPhongKick(n);
                break;
            case "Xuất danh sách":
                xuatBangDon("Người dùng", true, false, false);
                break;
            default:
                break;
        }
    }

    private void chonPhongKick(java.util.List<String> n) {
        if (snapshot == null) return;
        var ds = snapshot.lay("ROOMS");
        String[] ten = ds.stream().map(r -> r.get(1) + " · " + r.get(2)).toArray(String[]::new);
        if (ten.length == 0) return;
        FormQuanTri f = new FormQuanTri("Tiếp tục", false).chon("room", "Phòng", ten[0], ten);
        f.hien(this, "Chọn phòng để Kick", m -> {
            int i = Arrays.asList(ten).indexOf(m.get("room"));
            f.dong();
            KiemDuyetUI.thaoTac(this, core, ds.get(i).get(0), n.get(0), n.get(2), "Kick", this::loi);
        });
    }

    private void matKhauTam(String mk) {
        JDialog d = new JDialog(this, "Mật khẩu tạm", false);
        d.setContentPane(matKhauTamPanel(mk));
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    static JPanel matKhauTamPanel(String mk) {
        JPanel p = new JPanel(new BorderLayout(Theme.S12, Theme.S12));
        p.setBorder(Theme.le(Theme.S24));
        p.add(Theme.nhan("Mật khẩu tạm — chỉ hiển thị lần này", true), BorderLayout.NORTH);
        JTextField f = new JTextField(mk, 18);
        f.setEditable(false);
        f.setFont(Theme.DAM);
        p.add(f);
        JButton copy = Theme.nut("Sao chép", true);
        copy.addActionListener(e -> DanhSachPhongPanel.saoChep(mk));
        p.add(copy, BorderLayout.EAST);
        p.add(Theme.nhan("Người dùng bắt buộc đổi khi đăng nhập.", false), BorderLayout.SOUTH);
        return p;
    }

    // ===== PHONG =====
    private void room(String l) {
        var r = rooms.chon();
        if (!l.equals("Tạo phòng") && !l.equals("Xuất danh sách") && r.isEmpty()) {
            loi("Hãy chọn phòng.");
            return;
        }
        switch (l) {
            case "Tạo phòng":
            case "Sửa":
                formPhong(l.equals("Tạo phòng"), r);
                break;
            case "Đóng/Mở":
                lenh(null, "ROOM_STATE", r.get(0), "" + !r.get(4).equals("Mở"));
                break;
            case "Xóa":
                xacNhan("Xóa phòng", r.get(2), "Xóa phòng và toàn bộ tệp của phòng. Các thành viên sẽ rời phòng ngay.",
                        f -> lenh(f, "ROOM_DELETE", r.get(0)));
                break;
            case "Xóa lịch sử":
                xacNhan("Xóa lịch sử", r.get(2), "Nội dung tin nhắn và các tệp sẽ bị xóa. Thao tác không thể hoàn tác.",
                        f -> lenh(f, "ROOM_CLEAR", r.get(0)));
                break;
            case "Thành viên":
                KiemDuyetUI.thanhVien(this, core, r.get(0), this::loi);
                break;
            case "Xuất danh sách":
                xuatBangDon("Phòng trò chuyện", false, true, false);
                break;
            default:
                break;
        }
    }

    private void auditAction(String l) {
        xuatBangDon("Nhật ký kiểm toán", false, false, true);
    }

    private void xuatBangDon(String ten, boolean u, boolean r, boolean a) {
        if (snapshot == null) {
            loi("Chưa có dữ liệu snapshot để xuất.");
            return;
        }
        String html = com.mycompany.udpmultichat.server.XuatBaoCao.htmlTuyChon(snapshot, u, r, a, false);
        new BaoCaoPreviewDialog(this, html, ten).setVisible(true);
    }

    private void formPhong(boolean them, java.util.List<String> r) {
        FormQuanTri f = new FormQuanTri(them ? "Tạo phòng" : "Lưu", false)
                .text("ten", "Tên phòng", them ? "" : r.get(2)).text("mota", "Mô tả", them ? "" : r.get(11))
                .chon("loai", "Loại", them ? "Công khai" : r.get(3), "Công khai", "Riêng tư")
                .text("max", "Giới hạn 2–500", them ? "20" : r.get(13));
        if (them) {
            String[] u = snapshot.lay("USERS").stream().map(n -> n.get(1)).toArray(String[]::new);
            f.chon("chu", "Chủ phòng", u[0], u);
        } else {
            f.chon("chedo", "Mật khẩu", "Giữ nguyên", "Giữ nguyên", "Đổi", "Xóa");
        }
        f.matKhau("mk", "Mật khẩu phòng (nếu có)").kiemTra(m -> {
            if (m.get("ten").length() < 3 || m.get("ten").length() > 30) return "ten|Tên phòng cần 3–30 ký tự.";
            if (m.get("mota").length() > 80) return "mota|Mô tả tối đa 80 ký tự.";
            try {
                int n = Integer.parseInt(m.get("max"));
                if (n < 2 || n > 500) return "max|Giới hạn cần 2–500 người.";
            } catch (NumberFormatException e) {
                return "max|Hãy nhập số người.";
            }
            return "";
        });
        f.hien(this, them ? "Tạo phòng" : "Sửa phòng", m -> {
            if (them) {
                String id = snapshot.lay("USERS").stream().filter(n -> n.get(1).equals(m.get("chu"))).findFirst()
                        .orElseThrow().get(0);
                lenh(f, "ROOM_CREATE", id, m.get("ten"), m.get("mota"), "" + m.get("loai").equals("Công khai"),
                        m.get("mk"), m.get("max"));
            } else {
                lenh(f, "ROOM_EDIT", r.get(0), m.get("ten"), m.get("mota"), "" + m.get("loai").equals("Công khai"),
                        m.get("chedo").equals("Giữ nguyên") ? "KEEP" : m.get("chedo").equals("Đổi") ? "SET" : "REMOVE",
                        m.get("mk"), m.get("max"));
            }
        });
    }

    private void thongBao() {
        FormQuanTri f = new FormQuanTri("Gửi thông báo", true).text("text", "Nội dung (tối đa 200 ký tự)", "").kiemTra(
                m -> m.get("text").isBlank() || m.get("text").length() > 200 ? "text|Thông báo cần 1–200 ký tự." : "");
        f.hien(this, "Thông báo toàn hệ thống", m -> lenh(f, "BROADCAST", m.get("text")));
    }

    private void xacNhan(String lenh, String ten, String hauQua, java.util.function.Consumer<FormQuanTri> ok) {
        FormQuanTri f = new FormQuanTri(lenh, true).moTa(ten, hauQua);
        f.hien(this, lenh, m -> ok.accept(f));
    }

    private void loi(String text) {
        toast.hien(ThongBao.Muc.LOI, text);
    }

    @Override
    public void dispose() {
        timer.stop();
        toast.close();
        if (core != null) core.quanTri().khiDoi(() -> {});
        super.dispose();
    }
}
