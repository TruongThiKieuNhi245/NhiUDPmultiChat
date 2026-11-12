package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import static com.mycompany.udpmultichat.ui.ThanhPhan.*;

/** Cua so Client Swing; moi thao tac mang chay nen, su kien cap nhat qua EDT. */
@SuppressWarnings("serial")
public final class ChatMulticastUI extends JFrame implements Listener {
    private static final Color CHINH = new Color(0x4F6BED), NEN = new Color(0xF3F5FB);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private final ClientCore client = new ClientCore(this);
    private final ExecutorService viecNen = Executors.newSingleThreadExecutor(viec -> {
        Thread t = new Thread(viec, "ui-client-task"); t.setDaemon(true); return t;
    });
    private final JPanel dsTin = new JPanel();
    private final JScrollPane cuon = new JScrollPane();
    private final DefaultListModel<String> dsOnline = new DefaultListModel<>();
    private final JTextField nhap = new JTextField();
    private final NutBo gui = new NutBo("Gửi", CHINH, new Color(0x3B54C9));
    private final NutBo roi = new NutBo("Đăng xuất", new Color(0xEF6C6C), new Color(0xD65454));
    private final JLabel trangThai = new JLabel("Chưa đăng nhập"), soOnline = new JLabel("Thành viên: 0");
    private final DangNhapPanel dangNhap = new DangNhapPanel(this::taiKhoan);
    private String tenToi = "";
    private boolean dangGui, dangDong;

    public ChatMulticastUI() {
        setTitle("UDP Multi-room Chat — M1");
        setSize(1100, 700); setMinimumSize(new Dimension(1000, 580));
        setLocationByPlatform(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel dau = new JPanel(new BorderLayout());
        dau.add(taoTieuDe(), BorderLayout.NORTH);
        dau.add(dangNhap, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);
        add(taoDanhSachPhong(), BorderLayout.WEST);
        add(taoKhungChat(), BorderLayout.CENTER);
        add(taoThanhVien(), BorderLayout.EAST);
        add(taoThanhNhap(), BorderLayout.SOUTH);
        gui.addActionListener(e -> guiTin());
        nhap.addActionListener(e -> guiTin());
        roi.addActionListener(e -> thucHien(client::dangXuat, v -> daDangXuat("Đã đăng xuất.")));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dongCuaSo(); }
        });
        capNhat(false);
        themHeThong("Bật Server, đăng ký tài khoản rồi đăng nhập. Admin mặc định: admin / admin123.");
    }

    // ===== DUNG GIAO DIEN =====
    private JPanel taoTieuDe() {
        JPanel dau = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, CHINH, getWidth(), 0, new Color(0x7A5CF0)));
                g2.fillRect(0, 0, getWidth(), getHeight()); g2.dispose();
            }
        };
        dau.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel ten = new JLabel("UDP Multi-room Chat");
        ten.setFont(new Font("Segoe UI", Font.BOLD, 22)); ten.setForeground(Color.WHITE);
        trangThai.setForeground(Color.WHITE);
        dau.add(ten, BorderLayout.WEST); dau.add(trangThai, BorderLayout.EAST);
        return dau;
    }

    private JPanel taoDanhSachPhong() {
        JPanel trai = new JPanel(new BorderLayout(0, 12));
        trai.setBackground(Color.WHITE); trai.setBorder(new EmptyBorder(16, 12, 16, 12));
        trai.setPreferredSize(new Dimension(155, 0));
        trai.add(new JLabel("PHÒNG CHAT"), BorderLayout.NORTH);
        JList<String> phong = new JList<>(new String[] { "# Sảnh chung" });
        phong.setSelectedIndex(0); phong.setFixedCellHeight(42);
        trai.add(phong, BorderLayout.CENTER); trai.add(roi, BorderLayout.SOUTH);
        return trai;
    }

    private JTabbedPane taoKhungChat() {
        dsTin.setLayout(new BoxLayout(dsTin, BoxLayout.Y_AXIS)); dsTin.setOpaque(false);
        JPanel nen = new JPanel(new BorderLayout());
        nen.setBackground(NEN); nen.setBorder(new EmptyBorder(8, 0, 8, 0));
        nen.add(dsTin, BorderLayout.NORTH);
        cuon.setViewportView(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16);
        JTabbedPane tab = new JTabbedPane(); tab.addTab("Sảnh chung", cuon);
        return tab;
    }

    private JPanel taoThanhVien() {
        JList<String> online = new JList<>(dsOnline);
        online.setCellRenderer(new OnlineRenderer(() -> tenToi));
        JPanel phai = new JPanel(new BorderLayout()); phai.setBackground(Color.WHITE);
        phai.setPreferredSize(new Dimension(185, 0));
        soOnline.setBorder(new EmptyBorder(16, 12, 12, 12));
        phai.add(soOnline, BorderLayout.NORTH);
        JScrollPane cuonOnline = new JScrollPane(online); cuonOnline.setBorder(null);
        phai.add(cuonOnline, BorderLayout.CENTER);
        return phai;
    }

    private JPanel taoThanhNhap() {
        JPanel thanh = new JPanel(new BorderLayout(10, 0));
        thanh.setBackground(Color.WHITE); thanh.setBorder(new EmptyBorder(12, 16, 12, 16));
        nhap.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nhap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xE3E6EF)),
                new EmptyBorder(8, 12, 8, 12)));
        thanh.add(nhap, BorderLayout.CENTER); thanh.add(gui, BorderLayout.EAST);
        return thanh;
    }

    // ===== NUT BAM: CHI DOC SWING TREN EDT =====
    private void taiKhoan(DangNhapPanel.ThongTin thongTin) {
        dangNhap.choPhep(false);
        if (thongTin.dangKy()) {
            thucHien(() -> client.dangKy(thongTin.ip(), Protocol.CONG_SERVER, thongTin.ten(), thongTin.matKhau()),
                    v -> { dangNhap.choPhep(true); themHeThong("Đăng ký thành công. Bấm Đăng nhập."); });
        } else {
            thucHien(() -> client.dangNhap(thongTin.ip(), Protocol.CONG_SERVER, thongTin.ten(), thongTin.matKhau()),
                    this::daDangNhap);
        }
    }

    private void daDangNhap(NguoiDung toi) {
        dsTin.removeAll(); tenToi = toi.tenHienThi;
        dangNhap.xoaMatKhau(); capNhat(true);
        setTitle("UDP Chat — " + tenToi + " (" + toi.vaiTro + ")");
        trangThai.setText(tenToi + " • " + toi.vaiTro);
        themHeThong("Đã vào Sảnh chung — " + Protocol.SANH_NHOM + ":" + Protocol.SANH_CONG);
        nhap.requestFocusInWindow();
    }

    private void guiTin() {
        String noiDung = nhap.getText();
        if (dangGui || noiDung.isBlank() || client.nguoiDung() == null) { return; }
        dangGui = true; gui.setEnabled(false);
        thucHien(() -> client.guiTin(noiDung), v -> {
            if (nhap.getText().equals(noiDung)) { nhap.setText(""); }
            dangGui = false; gui.setEnabled(true); nhap.requestFocusInWindow();
        });
    }

    // Tao tac vu nen; ca thanh cong va loi deu quay lai EDT.
    private <T> void thucHien(Supplier<CompletableFuture<T>> viec, Consumer<T> xong) {
        roi.setEnabled(false);
        viecNen.execute(() -> {
            try {
                viec.get().whenComplete((v, e) -> SwingUtilities.invokeLater(() -> {
                    if (dangDong) { return; }
                    if (e == null) { xong.accept(v); }
                    else {
                        dangGui = false;
                        capNhat(client.nguoiDung() != null);
                        themHeThong("Lỗi: " + loiDeHieu(e));
                    }
                    roi.setEnabled(client.nguoiDung() != null);
                }));
            } catch (RuntimeException e) { onLoi(loiDeHieu(e)); }
        });
    }

    private void capNhat(boolean daVao) {
        dangNhap.choPhep(!daVao); roi.setEnabled(daVao);
        nhap.setEnabled(daVao); gui.setEnabled(daVao);
    }

    private void daDangXuat(String lyDo) {
        tenToi = ""; dsOnline.clear(); soOnline.setText("Thành viên: 0");
        trangThai.setText("Chưa đăng nhập"); capNhat(false); themHeThong(lyDo);
    }

    private void dongCuaSo() {
        if (dangDong) { return; }
        dangDong = true; setTitle("Đang thoát…");
        viecNen.execute(() -> client.dangXuat().whenComplete((v, e) -> {
            client.close(); viecNen.shutdown(); SwingUtilities.invokeLater(this::dispose);
        }));
    }

    private static String loiDeHieu(Throwable loi) {
        while (loi.getCause() != null) { loi = loi.getCause(); }
        if (loi instanceof java.util.concurrent.TimeoutException) {
            return "Server chưa phản hồi. Kiểm tra IP, Server và Windows Firewall.";
        }
        return loi.getMessage() == null ? "Không thực hiện được yêu cầu" : loi.getMessage();
    }

    // ===== SU KIEN TU CLIENTCORE =====
    @Override public void onTinNhan(TinNhan tin, boolean laToi) {
        SwingUtilities.invokeLater(() -> {
            JPanel hang = new JPanel(new FlowLayout(laToi ? FlowLayout.RIGHT : FlowLayout.LEFT, 12, 4));
            hang.setOpaque(false);
            hang.add(new BongTin(tin.ten, tin.noiDung, GIO.format(Instant.ofEpochMilli(tin.thoiGian)), laToi));
            themHang(hang);
        });
    }
    @Override public void onHeThong(String tin) { SwingUtilities.invokeLater(() -> themHeThong(tin)); }
    @Override public void onLoi(String loi) { SwingUtilities.invokeLater(() -> themHeThong("Lỗi: " + loi)); }
    @Override public void onMatKetNoi(String lyDo) { SwingUtilities.invokeLater(() -> daDangXuat(lyDo)); }
    @Override public void onDanhSachOnline(List<String> danhSach) {
        SwingUtilities.invokeLater(() -> {
            dsOnline.clear(); danhSach.forEach(dsOnline::addElement);
            soOnline.setText("Thành viên: " + danhSach.size());
        });
    }

    private void themHeThong(String tin) {
        JPanel hang = new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel nhan = new JLabel(tin); nhan.setForeground(new Color(0x747B91));
        nhan.setFont(new Font("Segoe UI", Font.PLAIN, 12)); hang.add(nhan); themHang(hang);
    }

    private void themHang(JPanel hang) {
        dsTin.add(hang); dsTin.revalidate(); dsTin.repaint();
        SwingUtilities.invokeLater(() -> cuon.getVerticalScrollBar().setValue(cuon.getVerticalScrollBar().getMaximum()));
    }
}
