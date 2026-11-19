package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.common.AnhChupQuanTri;
import com.mycompany.udpmultichat.server.XuatBaoCao;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

/**
 * Tab bao cao quan tri: chon nhom du lieu, xem truoc truc quan va xuat PDF/HTML/CSV.
 */
@SuppressWarnings("serial")
final class BaoCaoPanel extends JPanel {
    private final Supplier<AnhChupQuanTri> nguonSnapshot;
    private final Consumer<String> baoLoi;
    private final ThongBao toast;
    private final Runnable khiXong;
    private final JCheckBox cbUsers = new JCheckBox("Tài khoản người dùng (users.csv)", true);
    private final JCheckBox cbRooms = new JCheckBox("Phòng trò chuyện (phong.csv)", true);
    private final JCheckBox cbAudit = new JCheckBox("Nhật ký kiểm toán & Hệ thống (nhatky.csv)", false);
    private final JCheckBox cbStats = new JCheckBox("Thống kê tổng quan & Biểu đồ 7 ngày", true);
    private final JCheckBox cbHtml = new JCheckBox("Trang web HTML tự chứa & In/Lưu PDF", true);
    private final JCheckBox cbCsv = new JCheckBox("Tệp bảng tính CSV (Excel UTF-8 BOM)", true);
    private final JLabel lblBaoCao = Theme.nhan("Báo cáo CSV UTF-8 BOM và HTML tự chứa", false);
    private final JButton moThuMuc = Theme.nut("Mở thư mục", false), moHtml = Theme.nut("Mở báo cáo HTML", false);
    private Path thuBaoCao;
    private boolean dangXuat;

    BaoCaoPanel(Supplier<AnhChupQuanTri> nguonSnapshot, Consumer<String> baoLoi, ThongBao toast, Runnable khiXong) {
        super(new BorderLayout(0, Theme.S16));
        this.nguonSnapshot = nguonSnapshot;
        this.baoLoi = baoLoi;
        this.toast = toast;
        this.khiXong = khiXong;
        setBorder(Theme.le(Theme.S24));
        add(taoDau(), BorderLayout.NORTH);
        add(taoGiua(), BorderLayout.CENTER);
        add(taoDuoi(), BorderLayout.SOUTH);
    }

    private JPanel taoDau() {
        JPanel dau = new JPanel(new GridLayout(2, 1, 0, Theme.S4));
        dau.setOpaque(false);
        JLabel tieuDe = Theme.nhan("Trung tâm Báo cáo & Thống kê", true);
        tieuDe.setFont(Theme.DAM.deriveFont(20f));
        JLabel phu = Theme.nhan("Tùy biến nhóm dữ liệu cần xuất, xem trước trực quan và in ấn / xuất PDF, HTML, CSV", false);
        phu.setForeground(Theme.PHU);
        dau.add(tieuDe);
        dau.add(phu);
        return dau;
    }

    private JPanel taoGiua() {
        JPanel giua = new JPanel(new GridLayout(1, 2, Theme.S16, 0));
        giua.setOpaque(false);
        giua.add(taoTheNhom());
        giua.add(taoTheDinhDang());
        return giua;
    }

    private JPanel taoTheNhom() {
        JPanel the = Theme.theGradient();
        the.setLayout(new BorderLayout(0, Theme.S8));
        the.setBorder(Theme.le(Theme.S16));
        the.add(Theme.nhan("1. Chọn nhóm dữ liệu", true), BorderLayout.NORTH);
        JPanel ds = new JPanel(new GridLayout(4, 1, 0, Theme.S8));
        ds.setOpaque(false);
        for (var cb : new JCheckBox[] { cbUsers, cbRooms, cbAudit, cbStats }) {
            cb.setFont(Theme.FONT);
            cb.setOpaque(false);
            ds.add(cb);
        }
        the.add(ds, BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.S8, 0));
        nut.setOpaque(false);
        JButton bAll = Theme.nutNhe("Chọn tất cả", ""), bNone = Theme.nutNhe("Bỏ chọn", ""), bDef = Theme.nutNhe("Mặc định", "");
        bAll.addActionListener(e -> datNhom(true, true, true, true));
        bNone.addActionListener(e -> datNhom(false, false, false, false));
        bDef.addActionListener(e -> datNhom(true, true, false, true));
        nut.add(bAll);
        nut.add(bNone);
        nut.add(bDef);
        the.add(nut, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheDinhDang() {
        JPanel the = Theme.theGradient();
        the.setLayout(new BorderLayout(0, Theme.S8));
        the.setBorder(Theme.le(Theme.S16));
        the.add(Theme.nhan("2. Định dạng & Kết quả", true), BorderLayout.NORTH);
        JPanel ds = new JPanel(new GridLayout(2, 1, 0, Theme.S8));
        ds.setOpaque(false);
        for (var cb : new JCheckBox[] { cbHtml, cbCsv }) {
            cb.setFont(Theme.FONT);
            cb.setOpaque(false);
            ds.add(cb);
        }
        the.add(ds, BorderLayout.CENTER);
        JPanel duoi = new JPanel(new BorderLayout(0, Theme.S8));
        duoi.setOpaque(false);
        lblBaoCao.setFont(Theme.NHO);
        lblBaoCao.setForeground(Theme.PHU);
        duoi.add(lblBaoCao, BorderLayout.NORTH);
        JPanel nutMo = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.S8, 0));
        nutMo.setOpaque(false);
        moThuMuc.setEnabled(false);
        moHtml.setEnabled(false);
        moThuMuc.addActionListener(e -> mo(thuBaoCao));
        moHtml.addActionListener(e -> mo(thuBaoCao.resolve("tong_hop.html")));
        nutMo.add(moThuMuc);
        nutMo.add(moHtml);
        duoi.add(nutMo, BorderLayout.SOUTH);
        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoDuoi() {
        JPanel thanhNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S8, 0));
        thanhNut.setOpaque(false);
        JButton xemTruoc = Theme.nut("Xem trước báo cáo (In/PDF)", true);
        xemTruoc.setIcon(Theme.icon("eye"));
        JButton xuatChon = Theme.nut("Xuất báo cáo đã chọn", false);
        xemTruoc.addActionListener(e -> xemTruocBaoCao());
        xuatChon.addActionListener(e -> xuatTuyChon());
        thanhNut.add(xemTruoc);
        thanhNut.add(xuatChon);
        return thanhNut;
    }

    private void datNhom(boolean u, boolean r, boolean a, boolean s) {
        cbUsers.setSelected(u);
        cbRooms.setSelected(r);
        cbAudit.setSelected(a);
        cbStats.setSelected(s);
    }

    private void xemTruocBaoCao() {
        AnhChupQuanTri snap = nguonSnapshot.get();
        if (snap == null) {
            baoLoi.accept("Chưa có dữ liệu snapshot để xem trước.");
            return;
        }
        if (!cbUsers.isSelected() && !cbRooms.isSelected() && !cbAudit.isSelected() && !cbStats.isSelected()) {
            baoLoi.accept("Hãy chọn ít nhất một nhóm dữ liệu.");
            return;
        }
        String html = XuatBaoCao.htmlTuyChon(snap, cbUsers.isSelected(), cbRooms.isSelected(),
                cbAudit.isSelected(), cbStats.isSelected());
        Window w = SwingUtilities.getWindowAncestor(this);
        new BaoCaoPreviewDialog(w instanceof JFrame ? (JFrame) w : null, html, "Tùy chọn").setVisible(true);
    }

    private void xuatTuyChon() {
        AnhChupQuanTri snap = nguonSnapshot.get();
        if (snap == null || dangXuat) {
            return;
        }
        if (!cbUsers.isSelected() && !cbRooms.isSelected() && !cbAudit.isSelected() && !cbStats.isSelected()) {
            baoLoi.accept("Hãy chọn ít nhất một nhóm dữ liệu để xuất.");
            return;
        }
        if (!cbHtml.isSelected() && !cbCsv.isSelected()) {
            baoLoi.accept("Hãy chọn ít nhất một định dạng xuất (HTML hoặc CSV).");
            return;
        }
        JFileChooser chon = new JFileChooser();
        chon.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path dich = chon.getSelectedFile().toPath();
        dangXuat = true;
        lblBaoCao.setText("Đang tạo và lưu báo cáo...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return XuatBaoCao.xuatTuyChon(snap, dich, cbUsers.isSelected(), cbRooms.isSelected(),
                        cbAudit.isSelected(), cbHtml.isSelected(), cbCsv.isSelected());
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }).whenComplete((p, e) -> SwingUtilities.invokeLater(() -> {
            dangXuat = false;
            if (e != null) {
                lblBaoCao.setText("Không xuất được, thử lại.");
                baoLoi.accept(ClientCore.loiDeHieu(e));
            } else {
                thuBaoCao = p;
                moThuMuc.setEnabled(true);
                moHtml.setEnabled(cbHtml.isSelected() && Files.exists(p.resolve("tong_hop.html")));
                lblBaoCao.setText("Đã lưu tại " + p.getFileName());
                lblBaoCao.setToolTipText(p.toAbsolutePath().toString());
                toast.hien(ThongBao.Muc.THANH_CONG, "Đã xuất báo cáo tùy chọn.", "Mở thư mục", () -> mo(p));
            }
            if (khiXong != null) khiXong.run();
        }));
    }

    private void mo(Path p) {
        if (p == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Desktop.getDesktop().open(p.toFile());
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> baoLoi.accept("Không mở được báo cáo hoặc thư mục."));
            }
        });
    }
}
