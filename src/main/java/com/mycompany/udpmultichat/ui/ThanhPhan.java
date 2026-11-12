package com.mycompany.udpmultichat.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;

/** Cac thanh phan ve tay tach tu UI goc: bong tin, nut bo, vien va avatar chu cai. */
@SuppressWarnings("serial")
final class ThanhPhan {
    private ThanhPhan() { }
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

    // Chon mau theo ten (cung ten luon ra cung mau)
    private static Color mauTheoTen(String ten) {
        return MAU_AVATAR[(ten.hashCode() & 0x7fffffff) % MAU_AVATAR.length];
    }

    // ================== CAC THANH PHAN VE TAY ==================

    // Bong tin nhan: nen bo tron, tu xuong dong khi qua dai
    static class BongTin extends JPanel {
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
    static class NutBo extends JButton {
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
    static class VienTron extends AbstractBorder {
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
    static class OnlineRenderer extends JPanel implements ListCellRenderer<String> {
        private String ten = "";
        private final java.util.function.Supplier<String> tenToi;
        OnlineRenderer(java.util.function.Supplier<String> tenToi) { this.tenToi = tenToi; }

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
            String hienThi = (ten.equals(tenToi.get()) || (!tenToi.get().isEmpty() && ten.startsWith(tenToi.get()+" ("))) ? ten + " (ban)" : ten;
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(hienThi, 54, 8 + (30 - fm2.getHeight()) / 2 + fm2.getAscent());
            g2.dispose();
        }
    }

}
