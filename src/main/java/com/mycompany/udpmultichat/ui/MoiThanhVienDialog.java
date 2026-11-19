package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.ClientCore.NguoiDungTimKiem;
import com.mycompany.udpmultichat.common.model.Phong;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Hộp thoại tìm kiếm và mời người dùng vào phòng (M3c.2).
 * Thiết kế giao diện hiện đại theo chuẩn FlatLaf và Theme tokens:
 * - Ô tìm kiếm nhanh theo tên hiển thị hoặc tài khoản @username.
 * - Danh sách người dùng trực quan với Avatar, tên, tài khoản và trạng thái Online/Offline.
 * - Tự động lọc bỏ các người dùng đã có mặt trong phòng.
 * - Nút "Gửi lời mời" có hiệu ứng gradient hiện đại.
 */
@SuppressWarnings("serial")
public final class MoiThanhVienDialog extends JDialog {
    private final ClientCore core;
    private final String roomId;
    private final String tenPhong;
    private final Set<String> thanhVienHienTai;
    private final DefaultListModel<NguoiDungTimKiem> modelDanhSach = new DefaultListModel<>();
    private final JList<NguoiDungTimKiem> lstNguoiDung = new JList<>(modelDanhSach);
    private final JTextField txtTim = new JTextField();
    private final JLabel lblTrangThai = Theme.nhan("", false);
    private final JButton btnMoi = Theme.nut("Gửi lời mời", true);

    public MoiThanhVienDialog(Window cha, ClientCore core, String roomId, String tenPhong, List<Phong.ThanhVien> danhSachHienTai) {
        super(cha, "Mời thành viên — " + tenPhong, ModalityType.APPLICATION_MODAL);
        this.core = core;
        this.roomId = roomId;
        this.tenPhong = tenPhong;
        this.thanhVienHienTai = danhSachHienTai.stream().map(Phong.ThanhVien::id).collect(Collectors.toSet());

        khoiTaoGiaoDien();
        setSize(420, 500);
        setLocationRelativeTo(cha);
        // Tải danh sách ban đầu
        timKiem("");
    }

    private void khoiTaoGiaoDien() {
        JPanel content = new JPanel(new BorderLayout(0, Theme.S12));
        content.setBorder(new EmptyBorder(Theme.S16, Theme.S16, Theme.S16, Theme.S16));
        content.setBackground(Theme.NEN);

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout(0, Theme.S4));
        pnlHeader.setOpaque(false);
        JLabel lblTieuDe = Theme.nhan("Mời thành viên vào phòng", true);
        lblTieuDe.setFont(Theme.DAM.deriveFont(16f));
        JLabel lblMoTa = Theme.nhan("Tìm kiếm theo tên đăng nhập hoặc tên hiển thị", false);
        lblMoTa.setForeground(Theme.PHU);
        pnlHeader.add(lblTieuDe, BorderLayout.NORTH);
        pnlHeader.add(lblMoTa, BorderLayout.SOUTH);
        content.add(pnlHeader, BorderLayout.NORTH);

        // Vùng giữa: Ô tìm kiếm + Danh sách
        JPanel pnlCenter = new JPanel(new BorderLayout(0, Theme.S8));
        pnlCenter.setOpaque(false);

        // Ô tìm kiếm
        JPanel pnlTim = new JPanel(new BorderLayout(Theme.S8, 0));
        pnlTim.setOpaque(false);
        txtTim.putClientProperty("JTextField.placeholderText", "Nhập tên hoặc @tài khoản để tìm...");
        JButton btnTim = Theme.nut("Tìm", false);
        btnTim.addActionListener(e -> timKiem(txtTim.getText()));
        txtTim.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    timKiem(txtTim.getText());
                }
            }
        });
        pnlTim.add(txtTim, BorderLayout.CENTER);
        pnlTim.add(btnTim, BorderLayout.EAST);
        pnlCenter.add(pnlTim, BorderLayout.NORTH);

        // Renderer cho danh sách
        lstNguoiDung.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstNguoiDung.setCellRenderer(new ListCellRenderer<NguoiDungTimKiem>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends NguoiDungTimKiem> list,
                                                          NguoiDungTimKiem val, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JPanel hang = new JPanel(new BorderLayout(Theme.S12, 0));
                hang.setBorder(new EmptyBorder(Theme.S8, Theme.S8, Theme.S8, Theme.S8));
                hang.setOpaque(true);
                hang.setBackground(isSelected ? Theme.CHON : Theme.THE);

                // Avatar tròn
                ThanhPhan.Avatar avt = new ThanhPhan.Avatar(val.tenHienThi(), val.online(), () -> null);
                hang.add(avt, BorderLayout.WEST);

                // Thông tin tên và @username
                JPanel pnlThongTin = new JPanel(new GridLayout(2, 1, 0, 2));
                pnlThongTin.setOpaque(false);
                JLabel lblTen = Theme.nhan(val.tenHienThi(), true);
                JLabel lblSub = Theme.nhan("@" + val.tenDangNhap() + (val.online() ? " · Online" : " · Offline"), false);
                lblSub.setFont(Theme.NHO);
                lblSub.setForeground(val.online() ? Theme.THANH_CONG : Theme.PHU);
                pnlThongTin.add(lblTen);
                pnlThongTin.add(lblSub);
                hang.add(pnlThongTin, BorderLayout.CENTER);

                return hang;
            }
        });

        lstNguoiDung.addListSelectionListener(e -> {
            btnMoi.setEnabled(lstNguoiDung.getSelectedValue() != null);
        });

        JScrollPane scr = new JScrollPane(lstNguoiDung);
        scr.setBorder(BorderFactory.createLineBorder(Theme.VIEN));
        pnlCenter.add(scr, BorderLayout.CENTER);

        lblTrangThai.setFont(Theme.NHO);
        lblTrangThai.setForeground(Theme.PHU);
        pnlCenter.add(lblTrangThai, BorderLayout.SOUTH);
        content.add(pnlCenter, BorderLayout.CENTER);

        // Vùng dưới: Nút Hủy và Nút Mời
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S8, 0));
        pnlFooter.setOpaque(false);
        JButton btnHuy = Theme.nut("Hủy", false);
        btnHuy.addActionListener(e -> dispose());

        btnMoi.setEnabled(false);
        btnMoi.addActionListener(e -> thucHienMoi());

        pnlFooter.add(btnHuy);
        pnlFooter.add(btnMoi);
        content.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void timKiem(String tuKhoa) {
        lblTrangThai.setText("Đang tìm kiếm...");
        core.timNguoiDung(tuKhoa).thenAccept(ds -> SwingUtilities.invokeLater(() -> {
            modelDanhSach.clear();
            // Lọc các user chưa ở trong phòng
            List<NguoiDungTimKiem> hopLe = ds.stream()
                .filter(u -> !thanhVienHienTai.contains(u.id()))
                .filter(u -> core.nguoiDung() == null || !core.nguoiDung().id.equals(u.id()))
                .toList();

            for (var u : hopLe) {
                modelDanhSach.addElement(u);
            }
            if (modelDanhSach.isEmpty()) {
                lblTrangThai.setText("Không tìm thấy người dùng phù hợp (hoặc đã ở trong phòng).");
            } else {
                lblTrangThai.setText("Tìm thấy " + modelDanhSach.size() + " người dùng có thể mời.");
            }
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> lblTrangThai.setText("Lỗi: " + ClientCore.loiDeHieu(ex)));
            return null;
        });
    }

    private void thucHienMoi() {
        NguoiDungTimKiem chon = lstNguoiDung.getSelectedValue();
        if (chon == null) { return; }

        btnMoi.setEnabled(false);
        lblTrangThai.setText("Đang gửi lời mời...");

        core.moiVaoPhong(roomId, chon.id()).thenAccept(v -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                "Đã gửi lời mời tham gia phòng «" + tenPhong + "» tới @" + chon.tenDangNhap() + " thành công!",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                btnMoi.setEnabled(true);
                lblTrangThai.setText("Lỗi: " + ClientCore.loiDeHieu(ex));
                JOptionPane.showMessageDialog(this, ClientCore.loiDeHieu(ex), "Lỗi gửi lời mời", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }
}
