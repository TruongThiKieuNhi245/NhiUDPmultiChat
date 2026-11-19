package com.mycompany.udpmultichat.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Vai tro: Thanh phan ve tay chi lay token tu Theme: avatar tron va bong tin 12px. Ai goi: KhungChatPanel, ThanhVienPanel. Goi: Theme. */
final class ThanhPhan {
    // ===== VAI TRO VA CAC THAO TAC =====
    // Khoi tao lop va noi cac thanh phan can thiet.
    private ThanhPhan() { }
    @SuppressWarnings("serial")
    static final class Avatar extends JPanel {
        private String ten; private final Boolean online; private final java.util.function.Supplier<java.awt.image.BufferedImage> anh;
        // Avatar tin nhan khong co cham; thanh vien truyen co online.
        Avatar(String ten,Boolean online) { this(ten,online,() -> null); }
        // Anh lay tu cache RAM, khong cho mang trong paintComponent.
        Avatar(String ten,Boolean online,java.util.function.Supplier<java.awt.image.BufferedImage> anh) {
            this.ten=ten; this.online=online; this.anh=anh; setOpaque(false); setPreferredSize(new Dimension(32,36));
        }
        // Doi chu cai du phong khi ten hien thi thay doi.
        void tenMoi(String ten) { this.ten=ten; repaint(); }
        // Ve thanh phan bang token chung cua Theme.
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Theme.avatar(ten,anh.get(),32).paintIcon(this,g2,0,0);
            if (online!=null) { g2.setColor(Theme.THE); g2.fillOval(22,22,11,11); g2.setColor(online?Theme.THANH_CONG:Theme.OFFLINE); g2.fillOval(24,24,7,7); } g2.dispose();
        }
    }
    @SuppressWarnings("serial")
    static final class BongTin extends JPanel {
        private final boolean laToi;
        // Ve bong tin theo danh tinh da kiem tra, khong suy tu ten.
        BongTin(String ten,String noiDung,String gio,boolean laToi) {
            super(new BorderLayout(0,Theme.S4)); this.laToi=laToi; setOpaque(false); setBorder(Theme.le(Theme.S12));
            if (!laToi) { JLabel nhan=Theme.nhan(ten,true); nhan.setFont(Theme.NHO.deriveFont(java.awt.Font.BOLD)); nhan.setName("tenNguoiGui"); add(nhan,BorderLayout.NORTH); }
            JTextArea nd=new JTextArea(noiDung); nd.setFont(Theme.FONT); nd.setOpaque(false); nd.setEditable(false);
            nd.setLineWrap(true); nd.setWrapStyleWord(true); nd.setForeground(laToi?Theme.THE:Theme.CHU); nd.setBorder(null);
            int rong=Math.min(380,Math.max(48,nd.getFontMetrics(Theme.FONT).stringWidth(noiDung.length()>50?noiDung.substring(0,50):noiDung)+8));
            nd.setSize(rong,Short.MAX_VALUE); nd.setPreferredSize(new Dimension(rong,nd.getPreferredSize().height)); add(nd,BorderLayout.CENTER);
            JLabel thoiGian=Theme.nhan(gio,false); thoiGian.setFont(Theme.NHO); thoiGian.setForeground(laToi?Theme.THE:Theme.PHU); add(thoiGian,BorderLayout.SOUTH);
        }
        // Doi ten hien thi khong thay doi noi dung tin da luu.
        void tenMoi(String ten) {
            for (var c:getComponents()) { if (c instanceof JLabel l && "tenNguoiGui".equals(l.getName())) { l.setText(ten); } }
        }
        // Ve thanh phan bang token chung cua Theme.
        @Override protected void paintComponent(Graphics g) {
            if (laToi) {
                Theme.boTron(g,getWidth(),getHeight(),Theme.CHINH,Theme.BONG);
            } else {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int r=Theme.BONG*2;
                g2.setColor(Theme.THE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),r,r);
                g2.setColor(new java.awt.Color(0xE2,0xE8,0xF0));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,r,r);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
