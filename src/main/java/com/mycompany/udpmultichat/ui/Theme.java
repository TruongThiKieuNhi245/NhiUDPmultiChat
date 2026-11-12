package com.mycompany.udpmultichat.ui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** Vai tro: Mot he token chung cho moi man hinh; khong dat ma mau rieng trong cac UI khac. Ai goi: cac lop ui, ClientMain, ServerMain. Goi: FlatLightLaf, UIManager, Java2D. */
public final class Theme {
    // ===== VAI TRO VA CAC THAO TAC =====
    public static final int S4=4,S8=8,S12=12,S16=16,S24=24,BO=8,BONG=12;
    public static final Color CHINH=new Color(0x4F6BED),CHU=new Color(0x202635),PHU=new Color(0x596273);
    public static final Color NEN=new Color(0xF5F6FA),THE=new Color(0xFFFFFF),VIEN=new Color(0xC9CED9);
    public static final Color VIEN_SOAN=new Color(0xB8BFCC),CHON=new Color(0xE9EDFF),CHU_TREN_MAU=THE;
    public static final Color THANH_CONG=new Color(0x217347),LOI=new Color(0xBC303A),CANH_BAO=new Color(0x8A5B00),THONG_TIN=new Color(0x245FB3);
    public static final Color NEN_XANH=new Color(0xEDF8F0),NEN_DO=new Color(0xFFF0F1),NEN_VANG=new Color(0xFFF6DC),NEN_TIN=new Color(0xEDF4FF);
    public static final Color OFFLINE=new Color(0x777F8D),AVATAR=new Color(0x4261B9);
    public static final Color NEN_KHOA=new Color(0xECEEF2);
    public static final Font FONT=new Font("Segoe UI",Font.PLAIN,14),DAM=FONT.deriveFont(Font.BOLD),NHO=FONT.deriveFont(12f);
    public static final Font DON_CACH=new Font(tenFontLog(),Font.PLAIN,13);
    public static final int NUT_GUI=36,TOAST_CAO=48,TOAST_RONG=360,O_NHAP_CAO=32;
    public static final int TOAST_MS=4500,TOAST_LOI_MS=6500;
    // Khoi tao lop va noi cac thanh phan can thiet.
    private Theme() { }
    // Chuoi mau cho JLabel HTML van lay tu token, khong lap ma mau trong UI.
    public static String mauHtml(Color mau) { return String.format("#%06x",mau.getRGB()&0xffffff); }
    // Consolas ho tro font don cach tren Windows; fallback tren may khac.
    private static String tenFontLog() {
        for (String ten:java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (ten.equalsIgnoreCase("Consolas")) { return ten; }
        }
        return Font.MONOSPACED;
    }

    // Cai LAF truoc khi tao bat ky thanh phan Swing nao.
    public static void caiDat() {
        FlatLightLaf.setup();
        // Grayscale AA thay LCD subpixel de chu so khong co vien RGB tren PNG/nen mau.
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        UIManager.put("defaultFont",FONT);
        UIManager.put("Component.arc",BO); UIManager.put("Button.arc",BO);
        UIManager.put("TextComponent.arc",BO); UIManager.put("Component.focusColor",CHINH);
        UIManager.put("Component.borderColor",VIEN); UIManager.put("Component.focusWidth",1);
        UIManager.put("Component.innerFocusWidth",1); UIManager.put("Button.default.background",CHINH);
        UIManager.put("Button.default.foreground",THE); UIManager.put("Panel.background",THE);
        UIManager.put("Label.foreground",CHU); UIManager.put("TextField.foreground",CHU);
        UIManager.put("List.selectionBackground",CHON); UIManager.put("List.selectionForeground",CHU);
        UIManager.put("ScrollBar.width",10); UIManager.put("TabbedPane.tabSelectionHeight",2);
    }
    // Nut.
    public static JButton nut(String ten,boolean chinh) {
        JButton b=new JButton(ten); b.setFont(DAM);
        if (chinh) { b.setBackground(CHINH); b.setForeground(THE); }
        return b;
    }
    // Nut nhe chi hien nen khi hover, dung cho toolbar va icon.
    public static JButton nutNhe(String ten,String icon) {
        JButton b=nut(ten,false); b.putClientProperty("JButton.buttonType","toolBarButton");
        if (!icon.isEmpty()) { b.setIcon(Theme.icon(icon)); } return b;
    }
    // Nut gui vuong kich thuoc co dinh, LAF bo tron thanh hinh tron.
    public static JButton nutGui() {
        JButton b=nut("",true); b.setIcon(icon("send")); b.setToolTipText("Gửi tin nhắn");
        b.putClientProperty("JButton.buttonType","roundRect"); b.setPreferredSize(new java.awt.Dimension(NUT_GUI,NUT_GUI)); return b;
    }
    // Loi form chi chiem cho khi co noi dung.
    public static void datLoi(JLabel l,String text) {
        l.setText(vietHoaDau(text)); l.setVisible(!text.isBlank());
        if (l.getParent()!=null) { l.getParent().revalidate(); l.getParent().repaint(); }
    }
    // Hang form tinh lai chieu cao tu input + loi, khong luu maximum size cu.
    @SuppressWarnings("serial")
    public static JPanel hangNhap(String nhan,javax.swing.JComponent o,JLabel loi) {
        JPanel hang=new JPanel(new java.awt.BorderLayout(0,S4)) {
            // BoxLayout khong duoc ep input de danh cho JLabel loi.
            public java.awt.Dimension getMinimumSize() { return getPreferredSize(); }
            // Be rong co gian, chieu cao luon theo noi dung hien tai.
            public java.awt.Dimension getMaximumSize() { return new java.awt.Dimension(Integer.MAX_VALUE,getPreferredSize().height); }
        };
        JPanel boc=new JPanel(new java.awt.BorderLayout()); boc.add(o);
        boc.setPreferredSize(new java.awt.Dimension(200,O_NHAP_CAO));
        boc.setMinimumSize(new java.awt.Dimension(100,O_NHAP_CAO));
        hang.setAlignmentX(JPanel.LEFT_ALIGNMENT); hang.setBorder(new EmptyBorder(S8,0,S8,0));
        hang.add(nhan(nhan,false),java.awt.BorderLayout.NORTH); hang.add(boc,java.awt.BorderLayout.CENTER);
        hang.add(loi,java.awt.BorderLayout.SOUTH); return hang;
    }
    // Chuan hoa chu dau thong bao, khong doi noi dung ten/tin nhan.
    public static String vietHoaDau(String text) {
        return text.isEmpty()?text:text.substring(0,1).toUpperCase(java.util.Locale.ROOT)+text.substring(1);
    }
    // Nhan.
    public static JLabel nhan(String ten,boolean dam) {
        JLabel l=new JLabel(ten); l.setFont(dam?DAM:FONT); l.setForeground(CHU); return l;
    }
    // Le.
    public static EmptyBorder le(int n) { return new EmptyBorder(n,n,n,n); }
    // Bo tron.
    public static void boTron(Graphics g,int w,int h,Color mau,int bo) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(mau); g2.fillRoundRect(0,0,w,h,bo*2,bo*2); g2.dispose();
    }
    // Icon ve bang Java2D de khong thanh o vuong khi font Windows thieu glyph.
    public static javax.swing.Icon icon(String loai) {
        return new javax.swing.Icon() {
            // Tra be rong icon.
            public int getIconWidth() { return 18; }
            // Tra chieu cao icon.
            public int getIconHeight() { return 18; }
            // Ve icon Java2D, khong phu thuoc anh ngoai.
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.translate(x,y); a.setColor(c.getForeground());
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setStroke(new java.awt.BasicStroke(1.6f));
                veIcon(a,loai);
                a.dispose();
            }
        };
    }
    // Net ve tung icon tach rieng de tranh long lambda/class/switch nhieu tang.
    private static void veIcon(Graphics2D a,String loai) {
        switch(loai) {
            case "more": a.fillOval(8,3,3,3); a.fillOval(8,8,3,3); a.fillOval(8,13,3,3); break;
            case "lock": a.drawRoundRect(4,8,11,8,2,2); a.drawArc(6,2,7,10,0,180); break;
            case "smile": a.drawOval(2,2,14,14); a.fillOval(6,6,2,2); a.fillOval(11,6,2,2); a.drawArc(6,8,7,5,180,180); break;
            case "ok": a.drawPolyline(new int[]{3,7,15},new int[]{9,13,4},3); break;
            case "down": a.drawPolyline(new int[]{4,9,14},new int[]{6,11,6},3); break;
            case "up": a.drawPolyline(new int[]{4,9,14},new int[]{11,6,11},3); break;
            case "send": a.drawLine(9,15,9,3); a.drawPolyline(new int[]{4,9,14},new int[]{8,3,8},3); break;
            case "plus": a.drawLine(9,3,9,15); a.drawLine(3,9,15,9); break;
            default: a.drawOval(2,2,14,14); a.drawLine(9,7,9,12); a.fillOval(8,4,2,2); break;
        }
    }
    // Vong cho.
    public static javax.swing.Icon vongCho(java.util.function.IntSupplier goc) {
        return new javax.swing.Icon() {
            // Tra be rong icon.
            public int getIconWidth() { return 20; }
            // Tra chieu cao icon.
            public int getIconHeight() { return 20; }
            // Ve icon Java2D, khong phu thuoc anh ngoai.
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.setColor(CHINH);
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setStroke(new java.awt.BasicStroke(2)); a.drawArc(x+3,y+3,13,13,goc.getAsInt(),260); a.dispose();
            }
        };
    }
    // Avatar.
    public static javax.swing.Icon avatar(String ten) {
        return new javax.swing.Icon() {
            // Tra be rong icon.
            public int getIconWidth() { return 36; }
            // Tra chieu cao icon.
            public int getIconHeight() { return 36; }
            // Ve icon Java2D, khong phu thuoc anh ngoai.
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setColor(AVATAR); a.fillOval(x,y,34,34); a.setColor(THE); a.setFont(DAM);
                a.drawString(ten.isEmpty()?"?":ten.substring(0,1).toUpperCase(java.util.Locale.ROOT),x+12,y+23); a.dispose();
            }
        };
    }
    // Card trung tinh dung cho dang nhap, dialog va xem truoc.
    @SuppressWarnings("serial")
    // The.
    public static JPanel the() {
        return new JPanel() {
            { setOpaque(false); }
            // Ve thanh phan bang token chung cua Theme.
            @Override protected void paintComponent(Graphics g) { boTron(g,getWidth(),getHeight(),THE,BO); }
        };
    }
    // Icon ung dung ve trong code, khong can tep anh hoac thu vien moi.
    public static java.awt.Image iconUngDung() {
        var anh=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=anh.createGraphics(); boTron(g,32,32,CHINH,BO); g.setColor(THE); g.setFont(DAM);
        g.drawString("U",10,22); g.dispose(); return anh;
    }
}
