package com.mycompany.udpmultichat.ui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** Mot he token chung cho moi man hinh; khong dat ma mau rieng trong cac UI khac. */
public final class Theme {
    public static final int S4=4,S8=8,S12=12,S16=16,S24=24,BO=8,BONG=12;
    public static final Color CHINH=new Color(0x4F6BED),CHU=new Color(0x202635),PHU=new Color(0x596273);
    public static final Color NEN=new Color(0xF5F6FA),THE=new Color(0xFFFFFF),VIEN=new Color(0xC9CED9);
    public static final Color VIEN_SOAN=new Color(0xB8BFCC),CHON=new Color(0xE9EDFF),CHU_TREN_MAU=THE;
    public static final Color THANH_CONG=new Color(0x217347),LOI=new Color(0xBC303A),CANH_BAO=new Color(0x8A5B00),THONG_TIN=new Color(0x245FB3);
    public static final Color NEN_XANH=new Color(0xEDF8F0),NEN_DO=new Color(0xFFF0F1),NEN_VANG=new Color(0xFFF6DC),NEN_TIN=new Color(0xEDF4FF);
    public static final Color OFFLINE=new Color(0x777F8D),AVATAR=new Color(0x4261B9);
    public static final Font FONT=new Font("Segoe UI",Font.PLAIN,14),DAM=FONT.deriveFont(Font.BOLD),NHO=FONT.deriveFont(12f);
    private Theme() { }

    // Cai LAF truoc khi tao bat ky thanh phan Swing nao.
    public static void caiDat() {
        FlatLightLaf.setup();
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
    public static JButton nut(String ten,boolean chinh) {
        JButton b=new JButton(ten); b.setFont(DAM);
        if (chinh) { b.setBackground(CHINH); b.setForeground(THE); }
        return b;
    }
    public static JLabel nhan(String ten,boolean dam) {
        JLabel l=new JLabel(ten); l.setFont(dam?DAM:FONT); l.setForeground(CHU); return l;
    }
    public static EmptyBorder le(int n) { return new EmptyBorder(n,n,n,n); }
    public static void vien(JComponent c) { c.setBorder(BorderFactory.createLineBorder(VIEN)); }
    public static void boTron(Graphics g,int w,int h,Color mau,int bo) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(mau); g2.fillRoundRect(0,0,w,h,bo*2,bo*2); g2.dispose();
    }
    // Icon ve bang Java2D de khong thanh o vuong khi font Windows thieu glyph.
    public static javax.swing.Icon icon(String loai) {
        return new javax.swing.Icon() {
            public int getIconWidth() { return 18; } public int getIconHeight() { return 18; }
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.translate(x,y); a.setColor(c.getForeground());
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setStroke(new java.awt.BasicStroke(1.6f));
                switch(loai) {
                    case "more": for (int i=0;i<3;i++) { a.fillOval(8,3+i*5,3,3); } break;
                    case "lock": a.drawRoundRect(4,8,11,8,2,2); a.drawArc(6,2,7,10,0,180); break;
                    case "smile": a.drawOval(2,2,14,14); a.fillOval(6,6,2,2); a.fillOval(11,6,2,2); a.drawArc(6,8,7,5,180,180); break;
                    case "ok": a.drawPolyline(new int[]{3,7,15},new int[]{9,13,4},3); break;
                    case "down": a.drawPolyline(new int[]{4,9,14},new int[]{6,11,6},3); break;
                    default: a.drawOval(2,2,14,14); a.drawLine(9,7,9,12); a.fillOval(8,4,2,2); break;
                }
                a.dispose();
            }
        };
    }
    public static javax.swing.Icon vongCho(java.util.function.IntSupplier goc) {
        return new javax.swing.Icon() {
            public int getIconWidth() { return 20; } public int getIconHeight() { return 20; }
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.setColor(CHINH);
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setStroke(new java.awt.BasicStroke(2)); a.drawArc(x+3,y+3,13,13,goc.getAsInt(),260); a.dispose();
            }
        };
    }
    public static javax.swing.Icon avatar(String ten) {
        return new javax.swing.Icon() {
            public int getIconWidth() { return 36; } public int getIconHeight() { return 36; }
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setColor(AVATAR); a.fillOval(x,y,34,34); a.setColor(THE); a.setFont(DAM);
                a.drawString(ten.isEmpty()?"?":ten.substring(0,1).toUpperCase(java.util.Locale.ROOT),x+12,y+23); a.dispose();
            }
        };
    }
    // Card trung tinh dung cho dang nhap, dialog va xem truoc.
    @SuppressWarnings("serial")
    public static JPanel the() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) { boTron(g,getWidth(),getHeight(),THE,BO); }
        };
    }
}
