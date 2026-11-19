package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.ClientCore.LoiMoiPhong;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Hộp thoại hiển thị và xử lý danh sách lời mời vào phòng đang chờ (M3c.2).
 * Cho phép người dùng Chấp nhận hoặc Từ chối từng lời mời.
 */
@SuppressWarnings("serial")
public final class LoiMoiPhongDialog extends JDialog {
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ClientCore core;
    private final Consumer<String> khiChapNhanVaoPhong;
    private final JPanel pnlDanhSach = new JPanel();
    private final JLabel lblTrangThai = Theme.nhan("", false);

    public LoiMoiPhongDialog(Window cha, ClientCore core, Consumer<String> khiChapNhanVaoPhong) {
        super(cha, "Lời mời tham gia phòng", ModalityType.APPLICATION_MODAL);
        this.core = core;
        this.khiChapNhanVaoPhong = khiChapNhanVaoPhong;

        khoiTaoGiaoDien();
        setSize(480, 420);
        setLocationRelativeTo(cha);
        taiDanhSach();
    }

    private void khoiTaoGiaoDien() {
        JPanel content = new JPanel(new BorderLayout(0, Theme.S12));
        content.setBorder(new EmptyBorder(Theme.S16, Theme.S16, Theme.S16, Theme.S16));
        content.setBackground(Theme.NEN);

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout(0, Theme.S4));
        pnlHeader.setOpaque(false);
        JLabel lblTieuDe = Theme.nhan("Lời mời tham gia phòng", true);
        lblTieuDe.setFont(Theme.DAM.deriveFont(16f));
        JLabel lblMoTa = Theme.nhan("Các lời mời vào phòng chat đang chờ bạn phản hồi", false);
        lblMoTa.setForeground(Theme.PHU);
        pnlHeader.add(lblTieuDe, BorderLayout.NORTH);
        pnlHeader.add(lblMoTa, BorderLayout.SOUTH);
        content.add(pnlHeader, BorderLayout.NORTH);

        // Danh sách cuộn
        pnlDanhSach.setLayout(new BoxLayout(pnlDanhSach, BoxLayout.Y_AXIS));
        pnlDanhSach.setOpaque(false);

        JPanel pnlBoc = new JPanel(new BorderLayout());
        pnlBoc.setOpaque(false);
        pnlBoc.add(pnlDanhSach, BorderLayout.NORTH);

        JScrollPane scr = new JScrollPane(pnlBoc);
        scr.setBorder(BorderFactory.createLineBorder(Theme.VIEN));
        scr.getViewport().setBackground(Theme.NEN);
        content.add(scr, BorderLayout.CENTER);

        // Footer
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        lblTrangThai.setFont(Theme.NHO);
        lblTrangThai.setForeground(Theme.PHU);
        pnlFooter.add(lblTrangThai, BorderLayout.CENTER);

        JButton btnDong = Theme.nut("Đóng", false);
        btnDong.addActionListener(e -> dispose());
        pnlFooter.add(btnDong, BorderLayout.EAST);
        content.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void taiDanhSach() {
        lblTrangThai.setText("Đang tải danh sách lời mời...");
        pnlDanhSach.removeAll();
        pnlDanhSach.revalidate();
        pnlDanhSach.repaint();

        core.layDanhSachLoiMoi().thenAccept(ds -> SwingUtilities.invokeLater(() -> {
            pnlDanhSach.removeAll();
            if (ds.isEmpty()) {
                JPanel empty = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
                empty.setOpaque(false);
                JLabel lblEmpty = Theme.nhan("Bạn hiện không có lời mời nào.", false);
                lblEmpty.setForeground(Theme.PHU);
                empty.add(lblEmpty);
                pnlDanhSach.add(empty);
                lblTrangThai.setText("Không có lời mời.");
            } else {
                for (var lm : ds) {
                    pnlDanhSach.add(taoHangLoiMoi(lm));
                    pnlDanhSach.add(Box.createVerticalStrut(Theme.S8));
                }
                lblTrangThai.setText("Có " + ds.size() + " lời mời đang chờ.");
            }
            pnlDanhSach.revalidate();
            pnlDanhSach.repaint();
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> lblTrangThai.setText("Lỗi: " + ClientCore.loiDeHieu(ex)));
            return null;
        });
    }

    private JPanel taoHangLoiMoi(LoiMoiPhong lm) {
        JPanel hang = new JPanel(new BorderLayout(Theme.S12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme.boTron(g, getWidth(), getHeight(), Theme.THE, 8);
                super.paintComponent(g);
            }
        };
        hang.setOpaque(false);
        hang.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.VIEN, 1, true),
            new EmptyBorder(Theme.S12, Theme.S12, Theme.S12, Theme.S12)
        ));
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        // Thông tin phòng và người mời
        JPanel pnlInfo = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlInfo.setOpaque(false);
        JLabel lblPhong = Theme.nhan(lm.tenPhong(), true);
        lblPhong.setFont(Theme.DAM);

        String thoiGian = DINH_DANG_NGAY.format(Instant.ofEpochMilli(lm.thoiGianTao()));
        JLabel lblNguoi = Theme.nhan("Mời bởi: " + lm.tenNguoiMoi() + " · " + thoiGian, false);
        lblNguoi.setFont(Theme.NHO);
        lblNguoi.setForeground(Theme.PHU);

        pnlInfo.add(lblPhong);
        pnlInfo.add(lblNguoi);
        hang.add(pnlInfo, BorderLayout.CENTER);

        // Nút hành động
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S8, 0));
        pnlButtons.setOpaque(false);

        JButton btnTuChoi = Theme.nut("Từ chối", false);
        btnTuChoi.setFont(Theme.NHO);
        btnTuChoi.addActionListener(e -> traLoi(lm.inviteId(), false, null));

        JButton btnDongY = Theme.nut("Chấp nhận", true);
        btnDongY.setFont(Theme.NHO);
        btnDongY.addActionListener(e -> traLoi(lm.inviteId(), true, lm.roomId()));

        pnlButtons.add(btnTuChoi);
        pnlButtons.add(btnDongY);
        hang.add(pnlButtons, BorderLayout.EAST);

        return hang;
    }

    private void traLoi(String inviteId, boolean dongY, String roomId) {
        lblTrangThai.setText("Đang xử lý phản hồi...");
        core.traLoiLoiMoi(inviteId, dongY).thenAccept(v -> SwingUtilities.invokeLater(() -> {
            taiDanhSach();
            if (dongY && roomId != null && khiChapNhanVaoPhong != null) {
                khiChapNhanVaoPhong.accept(roomId);
                dispose();
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                lblTrangThai.setText("Lỗi: " + ClientCore.loiDeHieu(ex));
                JOptionPane.showMessageDialog(this, ClientCore.loiDeHieu(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }
}
