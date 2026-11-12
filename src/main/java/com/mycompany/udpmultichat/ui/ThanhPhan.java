package com.mycompany.udpmultichat.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Thanh phan ve tay chi lay token tu Theme: avatar tron va bong tin 12px. */
final class ThanhPhan {
    private ThanhPhan() { }
    @SuppressWarnings("serial")
    static final class Avatar extends JPanel {
        private final String ten; private final boolean online;
        Avatar(String ten,boolean online) { this.ten=ten; this.online=online; setOpaque(false); setPreferredSize(new Dimension(32,36)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.AVATAR); g2.fillOval(0,0,32,32); g2.setColor(Theme.THE); g2.setFont(Theme.DAM);
            String chu=ten.isBlank()?"?":ten.substring(0,1).toUpperCase(java.util.Locale.ROOT); g2.drawString(chu,11,21);
            g2.setColor(Theme.THE); g2.fillOval(22,22,11,11); g2.setColor(online?Theme.THANH_CONG:Theme.OFFLINE); g2.fillOval(24,24,7,7); g2.dispose();
        }
    }
    @SuppressWarnings("serial")
    static final class BongTin extends JPanel {
        private final boolean laToi;
        BongTin(String ten,String noiDung,String gio,boolean laToi) {
            super(new BorderLayout(0,Theme.S4)); this.laToi=laToi; setOpaque(false); setBorder(Theme.le(Theme.S12));
            if (!laToi) { JLabel nhan=Theme.nhan(ten,true); nhan.setFont(Theme.NHO); add(nhan,BorderLayout.NORTH); }
            JTextArea nd=new JTextArea(noiDung); nd.setFont(Theme.FONT); nd.setOpaque(false); nd.setEditable(false);
            nd.setLineWrap(true); nd.setWrapStyleWord(true); nd.setForeground(laToi?Theme.THE:Theme.CHU); nd.setBorder(null);
            int rong=Math.min(380,Math.max(48,nd.getFontMetrics(Theme.FONT).stringWidth(noiDung.length()>50?noiDung.substring(0,50):noiDung)+8));
            nd.setSize(rong,Short.MAX_VALUE); nd.setPreferredSize(new Dimension(rong,nd.getPreferredSize().height)); add(nd,BorderLayout.CENTER);
            JLabel thoiGian=Theme.nhan(gio,false); thoiGian.setFont(Theme.NHO); thoiGian.setForeground(laToi?Theme.THE:Theme.PHU); add(thoiGian,BorderLayout.SOUTH);
        }
        @Override protected void paintComponent(Graphics g) { Theme.boTron(g,getWidth(),getHeight(),laToi?Theme.CHINH:Theme.CHON,Theme.BONG); super.paintComponent(g); }
    }
}
