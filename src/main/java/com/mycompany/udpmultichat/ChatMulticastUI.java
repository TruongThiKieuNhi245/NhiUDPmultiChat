package com.mycompany.udpmultichat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;

/**
 * FILE GIAO DIEN: cua so chat dep (bong tin nhan, danh sach online co avatar).
 *
 * File nay KHONG xu ly mang. Moi viec gui/nhan nam trong MulticastChat.
 * Giao dien chi: goi ham cua MulticastChat va hien thi ket qua nhan duoc
 * (implements Listener).
 *
 * Cach thu: chay file nay nhieu lan, moi cua so nhap 1 ten roi bam "Vao nhom".
 */
public class ChatMulticastUI extends JFrame implements MulticastChat.Listener {

    // ===== Bang mau + font =====
    private static final Color MAU_CHINH = new Color(0x4F6BED);
    private static final Color MAU_CHINH_HOVER = new Color(0x3B54C9);
    private static final Color MAU_NEN = new Color(0xF3F5FB);
    private static final Color MAU_CHU = new Color(0x2B2F3A);
    private static final Color MAU_XAM = new Color(0x8A90A6);
    private static final Color MAU_VIEN = new Color(0xE3E6EF);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final int RONG_BONG_TOI_DA = 340;   // chieu rong toi da cua 1 bong tin

    // Mau avatar, chon theo ten de moi nguoi co 1 mau co dinh
    private static final Color[] MAU_AVATAR = {
        new Color(0xEF6C6C), new Color(0xF2A03D), new Color(0x3DB88B),
        new Color(0x3D9BE9), new Color(0x8F6BE8), new Color(0xE967A8)
    };

    // ===== Thanh phan giao dien =====
    private JTextField txtTen;
    private NutBo btVao;
    private NutBo btRoi;
    private JTextField txtNhap;
    private NutBo btGui;
    private JPanel dsTin;                       // noi chua cac bong tin
    private JScrollPane khungCuon;
    private DefaultListModel<String> dsOnline;
    private JLabel lbTrangThai;

    private final MulticastChat chat;           // phan xu ly mang

    public ChatMulticastUI() {
        chat = new MulticastChat(this);
        taoGiaoDien();
        capNhatTrangThai(false);
        themHeThong("Nhap ten roi bam \"Vao nhom\" de bat dau chat.");
    }

    // ================== DUNG GIAO DIEN ==================
    private void taoGiaoDien() {
        setTitle("Chat Multicast");
        setSize(820, 560);
        int lech = (int) (Math.random() * 6) * 30;   // lech cua so de khong chong khit
        setLocation(80 + lech, 40 + lech);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        add(taoPhanDau(), BorderLayout.NORTH);
        add(taoKhungChat(), BorderLayout.CENTER);
        add(taoKhungOnline(), BorderLayout.EAST);
        add(taoThanhNhap(), BorderLayout.SOUTH);

        // Su kien
        btVao.addActionListener(e -> vaoNhom());
        btRoi.addActionListener(e -> roiNhom());
        btGui.addActionListener(e -> guiTin());
        txtNhap.addActionListener(e -> guiTin());   // Enter cung gui
        txtTen.addActionListener(e -> {
            if (!chat.isDaVao()) {
                vaoNhom();
            }
        });

        // Dong cua so: roi nhom truoc roi moi thoat
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                chat.roiNhom();
                System.exit(0);
            }
        });
    }

    // Phan dau: thanh tieu de mau gradient + thanh nhap ten
    private JPanel taoPhanDau() {
        // Thanh tieu de
        JPanel tieuDe = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, MAU_CHINH, getWidth(), 0, new Color(0x7A5CF0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        tieuDe.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lbTieuDe = new JLabel("Chat Multicast");
        lbTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbTieuDe.setForeground(Color.WHITE);
        JLabel lbNhom = new JLabel("Nhom " + MulticastChat.DIA_CHI_NHOM + ":" + MulticastChat.CONG);
        lbNhom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbNhom.setForeground(new Color(0xDDE3FF));

        JPanel chuBenTrai = new JPanel();
        chuBenTrai.setOpaque(false);
        chuBenTrai.setLayout(new BoxLayout(chuBenTrai, BoxLayout.Y_AXIS));
        chuBenTrai.add(lbTieuDe);
        chuBenTrai.add(lbNhom);

        lbTrangThai = new JLabel("Chua vao nhom");
        lbTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbTrangThai.setForeground(Color.WHITE);

        tieuDe.add(chuBenTrai, BorderLayout.WEST);
        tieuDe.add(lbTrangThai, BorderLayout.EAST);

        // Thanh nhap ten + nut vao/roi
        JPanel thanhTen = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        thanhTen.setBackground(Color.WHITE);
        thanhTen.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MAU_VIEN));

        JLabel lbTen = new JLabel("Ten cua ban:");
        lbTen.setFont(FONT);
        lbTen.setForeground(MAU_CHU);
        txtTen = new JTextField(14);
        txtTen.setFont(FONT);
        txtTen.setBorder(new VienTron(MAU_VIEN, 6, 12));
        btVao = new NutBo("Vao nhom", MAU_CHINH, MAU_CHINH_HOVER);
        btRoi = new NutBo("Roi nhom", new Color(0xEF6C6C), new Color(0xD65454));
        thanhTen.add(lbTen);
        thanhTen.add(txtTen);
        thanhTen.add(btVao);
        thanhTen.add(btRoi);

        JPanel phanDau = new JPanel(new BorderLayout());
        phanDau.add(tieuDe, BorderLayout.NORTH);
        phanDau.add(thanhTen, BorderLayout.SOUTH);
        return phanDau;
    }

    // Khung chat o giua: cac bong tin xep doc, tin moi nhat o duoi
    private JScrollPane taoKhungChat() {
        dsTin = new JPanel();
        dsTin.setLayout(new BoxLayout(dsTin, BoxLayout.Y_AXIS));
        dsTin.setOpaque(false);

        // Bo dsTin vao NORTH de cac bong tin khong bi gian ra theo chieu doc
        JPanel nen = new JPanel(new BorderLayout());
        nen.setBackground(MAU_NEN);
        nen.setBorder(new EmptyBorder(8, 0, 8, 0));
        nen.add(dsTin, BorderLayout.NORTH);

        khungCuon = new JScrollPane(nen);
        khungCuon.setBorder(null);
        khungCuon.getVerticalScrollBar().setUnitIncrement(16);
        return khungCuon;
    }

    // Khung ben phai: danh sach nguoi online co avatar
    private JPanel taoKhungOnline() {
        dsOnline = new DefaultListModel<>();
        JList<String> lstOnline = new JList<>(dsOnline);
        lstOnline.setCellRenderer(new OnlineRenderer());
        lstOnline.setFocusable(false);

        JLabel lbTieuDe = new JLabel("Dang online");
        lbTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbTieuDe.setForeground(MAU_CHU);
        lbTieuDe.setBorder(new EmptyBorder(14, 16, 10, 16));

        JScrollPane cuon = new JScrollPane(lstOnline);
        cuon.setBorder(null);

        JPanel khung = new JPanel(new BorderLayout());
        khung.setBackground(Color.WHITE);
        khung.setPreferredSize(new Dimension(190, 0));
        khung.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, MAU_VIEN));
        khung.add(lbTieuDe, BorderLayout.NORTH);
        khung.add(cuon, BorderLayout.CENTER);
        return khung;
    }

    // Thanh nhap tin o duoi cung
    private JPanel taoThanhNhap() {
        txtNhap = new JTextField();
        txtNhap.setFont(FONT);
        txtNhap.setBorder(new VienTron(MAU_VIEN, 10, 16));
        btGui = new NutBo("Gui", MAU_CHINH, MAU_CHINH_HOVER);

        JPanel thanh = new JPanel(new BorderLayout(10, 0));
        thanh.setBackground(Color.WHITE);
        thanh.setBorder(new EmptyBorder(10, 14, 10, 14));
        thanh.add(txtNhap, BorderLayout.CENTER);
        thanh.add(btGui, BorderLayout.EAST);
        return thanh;
    }

    // ================== XU LY NUT BAM ==================
    private void vaoNhom() {
        try {
            chat.vaoNhom(txtTen.getText());
            setTitle("Chat Multicast - " + chat.getTen());
            capNhatTrangThai(true);
            txtNhap.requestFocusInWindow();
        } catch (Exception e) {
            themHeThong("Loi vao nhom: " + e.getMessage());
        }
    }

    private void roiNhom() {
        chat.roiNhom();
        capNhatTrangThai(false);
        setTitle("Chat Multicast");
        dsOnline.clear();
        lbTrangThai.setText("Chua vao nhom");
        themHeThong("Ban da roi nhom.");
    }

    private void guiTin() {
        chat.guiTin(txtNhap.getText());
        txtNhap.setText("");
        txtNhap.requestFocusInWindow();
    }

    // Bat/tat o nhap va nut theo trang thai
    private void capNhatTrangThai(boolean vao) {
        txtTen.setEnabled(!vao);
        btVao.setEnabled(!vao);
        btRoi.setEnabled(vao);
        btGui.setEnabled(vao);
        txtNhap.setEnabled(vao);
    }

    // ================== NHAN SU KIEN TU MulticastChat ==================
    // Cac ham nay chay tren luong nhan -> phai dung invokeLater de cap nhat Swing

    @Override
    public void onTinNhan(String ten, String noiDung, boolean laToi) {
        SwingUtilities.invokeLater(() -> themBongTin(ten, noiDung, laToi));
    }

    @Override
    public void onHeThong(String thongBao) {
        SwingUtilities.invokeLater(() -> themHeThong(thongBao));
    }

    @Override
    public void onDanhSachOnline(List<String> dsTen) {
        SwingUtilities.invokeLater(() -> {
            dsOnline.clear();
            for (String ten : dsTen) {
                dsOnline.addElement(ten);
            }
            lbTrangThai.setText("Online: " + dsTen.size() + " nguoi");
        });
    }

    @Override
    public void onLoi(String loi) {
        SwingUtilities.invokeLater(() -> themHeThong("Loi: " + loi));
    }

    // ================== THEM NOI DUNG VAO KHUNG CHAT ==================
    // Them 1 bong tin: tin cua minh ben phai, cua nguoi khac ben trai
    private void themBongTin(String ten, String noiDung, boolean laToi) {
        String gio = new SimpleDateFormat("HH:mm").format(new Date());
        JPanel hang = new JPanel(new FlowLayout(laToi ? FlowLayout.RIGHT : FlowLayout.LEFT, 12, 4));
        hang.setOpaque(false);
        hang.add(new BongTin(ten, noiDung, gio, laToi));
        themVaoKhung(hang);
    }

    // Them 1 thong bao he thong o giua (vi du: "An da vao nhom")
    private void themHeThong(String thongBao) {
        JLabel nhan = new JLabel(thongBao) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE4E7F2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        nhan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nhan.setForeground(MAU_XAM);
        nhan.setBorder(new EmptyBorder(3, 14, 3, 14));

        JPanel hang = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        hang.setOpaque(false);
        hang.add(nhan);
        themVaoKhung(hang);
    }

    private void themVaoKhung(JPanel hang) {
        dsTin.add(hang);
        dsTin.revalidate();
        dsTin.repaint();
        // Cuon xuong cuoi de thay tin moi nhat (sau khi giao dien tinh xong kich thuoc)
        SwingUtilities.invokeLater(() -> {
            JScrollBar thanhCuon = khungCuon.getVerticalScrollBar();
            thanhCuon.setValue(thanhCuon.getMaximum());
        });
    }

    // Chon mau theo ten (cung ten luon ra cung mau)
    private static Color mauTheoTen(String ten) {
        return MAU_AVATAR[(ten.hashCode() & 0x7fffffff) % MAU_AVATAR.length];
    }

    // ================== CAC THANH PHAN VE TAY ==================

    // Bong tin nhan: nen bo tron, tu xuong dong khi qua dai
    private static class BongTin extends JPanel {
        private final boolean laToi;

        BongTin(String ten, String noiDung, String gio, boolean laToi) {
            this.laToi = laToi;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(8, 14, 6, 14));

            // Tin cua nguoi khac thi co them ten (tin cua minh khong can)
            if (!laToi) {
                JLabel lbTen = new JLabel(ten);
                lbTen.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbTen.setForeground(mauTheoTen(ten));
                lbTen.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(lbTen);
                add(Box.createVerticalStrut(2));
            }

            // Noi dung: JTextArea de tu xuong dong
            JTextArea nd = new JTextArea(noiDung);
            nd.setEditable(false);
            nd.setOpaque(false);
            nd.setLineWrap(true);
            nd.setWrapStyleWord(true);
            nd.setFont(FONT);
            nd.setForeground(laToi ? Color.WHITE : MAU_CHU);
            nd.setBorder(null);
            nd.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Tinh chieu rong: vua du chu, nhung khong qua RONG_BONG_TOI_DA
            FontMetrics fm = nd.getFontMetrics(FONT);
            int rongChu = 0;
            for (String dong : noiDung.split("\n")) {
                rongChu = Math.max(rongChu, fm.stringWidth(dong));
            }
            int rong = Math.min(rongChu + 6, RONG_BONG_TOI_DA);
            nd.setSize(rong, Short.MAX_VALUE);   // dat rong truoc de JTextArea tinh duoc chieu cao
            nd.setPreferredSize(new Dimension(rong, nd.getPreferredSize().height));
            add(nd);

            // Gio gui
            JLabel lbGio = new JLabel(gio);
            lbGio.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lbGio.setForeground(laToi ? new Color(0xD5DCFF) : MAU_XAM);
            lbGio.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(Box.createVerticalStrut(3));
            add(lbGio);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(laToi ? MAU_CHINH : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            if (!laToi) {
                g2.setColor(MAU_VIEN);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Nut bam bo tron, doi mau khi re chuot vao
    private static class NutBo extends JButton {
        private final Color mau;
        private final Color mauHover;
        private boolean hover = false;

        NutBo(String chu, Color mau, Color mauHover) {
            super(chu);
            this.mau = mau;
            this.mauHover = mauHover;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);   // tu ve nen
            setFocusPainted(false);
            setBorder(new EmptyBorder(9, 20, 9, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(!isEnabled() ? new Color(0xC9CDDA) : (hover ? mauHover : mau));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Vien bo tron cho o nhap
    private static class VienTron extends AbstractBorder {
        private final Color mau;
        private final int doc;
        private final int ngang;

        VienTron(Color mau, int doc, int ngang) {
            this.mau = mau;
            this.doc = doc;
            this.ngang = ngang;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(mau);
            g2.drawRoundRect(x, y, w - 1, h - 1, h, h);
            g2.dispose();
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(doc, ngang, doc, ngang);
        }
    }

    // Moi dong trong danh sach online: avatar tron co chu cai dau + ten
    private class OnlineRenderer extends JPanel implements ListCellRenderer<String> {
        private String ten = "";

        public Component getListCellRendererComponent(JList<? extends String> list,
                String value, int index, boolean duocChon, boolean coFocus) {
            ten = value;
            return this;
        }

        public Dimension getPreferredSize() {
            return new Dimension(170, 46);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Avatar tron + chu cai dau
            g2.setColor(mauTheoTen(ten));
            g2.fillOval(14, 8, 30, 30);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            String chu = ten.isEmpty() ? "?" : ten.substring(0, 1).toUpperCase();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(chu, 14 + (30 - fm.stringWidth(chu)) / 2, 8 + (30 - fm.getHeight()) / 2 + fm.getAscent());

            // Cham xanh bao dang online
            g2.setColor(Color.WHITE);
            g2.fillOval(34, 30, 12, 12);
            g2.setColor(new Color(0x34C759));
            g2.fillOval(36, 32, 8, 8);

            // Ten (them "(ban)" neu la minh)
            g2.setColor(MAU_CHU);
            g2.setFont(FONT);
            String hienThi = ten.equals(chat.getTen()) ? ten + " (ban)" : ten;
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(hienThi, 54, 8 + (30 - fm2.getHeight()) / 2 + fm2.getAscent());
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");   // dung IPv4 cho on dinh
        SwingUtilities.invokeLater(() -> new ChatMulticastUI().setVisible(true));
    }
}
