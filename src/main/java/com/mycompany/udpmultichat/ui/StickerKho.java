package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Vai tro: Muoi hai sticker 128px ve bang Java2D, popup ba nhom. Ai goi: SoanTinPanel, HangTin, UiPreview. Goi: Theme, Protocol. */
final class StickerKho {
    private static final String[] TEN={"Vui vẻ","Yêu thương","Đồng ý","Vỗ tay","Cười ra nước mắt","Ngạc nhiên","Buồn","Giận","Ngủ ngon","Chúc mừng","Pizza","Cà phê"};
    private static final Map<String,BufferedImage> ANH=new HashMap<>();
    // Kho chi chua so luong ma co dinh, khong nhan du lieu anh tu mang.
    private StickerKho() { }
    // ===== KHO ANH VA POPUP =====
    static String ten(String ma) { int i=Arrays.asList(Protocol.MA_STICKER).indexOf(ma); return i<0?"Sticker không hỗ trợ":TEN[i]; }
    // Anh goc 128x128 chi ve mot lan moi ma.
    static synchronized BufferedImage anh(String ma) { return ANH.computeIfAbsent(Protocol.stickerHopLe(ma)?ma:"?",StickerKho::ve); }
    // Thu nho 112px/56px de dung trong chat va picker, khong dung font emoji.
    static Icon icon(String ma,int canh) { return new ImageIcon(anh(ma).getScaledInstance(canh,canh,Image.SCALE_SMOOTH)); }
    // Moi nhom bon sticker, ten hien tren nut va tooltip.
    static JPopupMenu bangChon(Consumer<String> chon) {
        JPopupMenu m=new JPopupMenu(); JTabbedPane tab=new JTabbedPane(); String[] nhom={"Cảm xúc","Tâm trạng","Mỗi ngày"};
        for (int j=0;j<3;j++) {
            JPanel luoi=new JPanel(new GridLayout(2,2,Theme.S8,Theme.S8)); luoi.setBorder(Theme.le(Theme.S8));
            for (int i=j*4;i<j*4+4;i++) {
                String ma=Protocol.MA_STICKER[i]; JButton b=Theme.nut(ten(ma),false); b.setIcon(icon(ma,56));
                b.setHorizontalTextPosition(JButton.CENTER); b.setVerticalTextPosition(JButton.BOTTOM); b.setToolTipText(ten(ma));
                b.addActionListener(e -> { m.setVisible(false); chon.accept(ma); }); luoi.add(b);
            }
            tab.addTab(nhom[j],luoi);
        }
        m.add(tab); return m;
    }
    // ===== VE HINH CO BAN, NEN TRONG SUOT =====
    private static BufferedImage ve(String ma) {
        BufferedImage h=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB); Graphics2D g=h.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch (ma) {
            case "tim": tim(g,18,16,92); break;
            case "like": tay(g,24,22,false); break;
            case "votay": tay(g,8,26,true); break;
            case "phao": phao(g); break;
            case "pizza": pizza(g); break;
            case "cafe": cafe(g); break;
            case "?": g.setColor(Theme.VIEN); g.fillRoundRect(10,10,108,108,20,20); g.setColor(Theme.PHU); g.setFont(Theme.DAM.deriveFont(48f)); g.drawString("?",50,82); break;
            default: mat(g,ma); break;
        }
        g.dispose(); return h;
    }
    // Mat tron va bieu cam, khong chen bat ky ky tu emoji nao.
    private static void mat(Graphics2D g,String ma) {
        g.setColor(ma.equals("gian")?Theme.STICKER_DO:Theme.STICKER_VANG); g.fillOval(12,12,104,104); g.setColor(Theme.CHU);
        if (ma.equals("ngu")) { g.drawLine(35,52,48,52); g.drawLine(79,52,92,52); g.drawOval(55,76,17,12); g.setFont(Theme.DAM.deriveFont(22f)); g.drawString("Z",97,20); return; }
        g.fillOval(35,42,10,14); g.fillOval(82,42,10,14);
        switch (ma) {
            case "nga": g.drawOval(51,69,26,30); break;
            case "buon": g.drawArc(43,78,42,23,0,180); giot(g,91,60); break;
            case "gian": g.drawLine(30,30,48,37); g.drawLine(80,37,98,30); g.drawLine(47,83,81,83); break;
            case "cuoi": g.fillArc(34,65,60,38,180,180); giot(g,18,57); giot(g,104,57); break;
            default: g.drawArc(35,53,58,43,180,180); break;
        }
    }
    // Giot nuoc mat lam tu duong cong.
    private static void giot(Graphics2D g,int x,int y) {
        Path2D p=new Path2D.Double(); p.moveTo(x,y); p.curveTo(x-17,y+27,x+17,y+27,x,y); g.setColor(Theme.STICKER_XANH); g.fill(p); g.setColor(Theme.CHU);
    }
    // Trai tim hai cung va mot day nhon.
    private static void tim(Graphics2D g,int x,int y,int n) {
        Path2D p=new Path2D.Double(); p.moveTo(x+n/2.0,y+n); p.curveTo(x-n/3.0,y+n/2.0,x,y-n/4.0,x+n/2.0,y+n/5.0);
        p.curveTo(x+n,y-n/4.0,x+n*4/3.0,y+n/2.0,x+n/2.0,y+n); g.setColor(Theme.STICKER_DO); g.fill(p);
    }
    // Ngon cai/hai ban tay dang vo, dung cac khoi bo tron.
    private static void tay(Graphics2D g,int x,int y,boolean vo) {
        g.setColor(Theme.STICKER_DA); g.fillRoundRect(x+24,y+30,50,59,18,18); g.fillRoundRect(x+27,y,18,60,12,12); g.fillRoundRect(x+5,y+42,30,30,14,14);
        g.setColor(Theme.CHINH); g.fillRoundRect(x+23,y+82,54,18,6,6);
        if (vo) { g.setColor(Theme.STICKER_VANG); g.fillRoundRect(79,42,24,60,14,14); g.drawLine(90,15,100,28); g.drawLine(107,25,116,23); }
    }
    // Phao hoa bang tia va confetti tron.
    private static void phao(Graphics2D g) {
        Color[] mau={Theme.CHINH,Theme.STICKER_DO,Theme.STICKER_VANG};
        for (int i=0;i<12;i++) {
            double a=i*Math.PI/6; g.setColor(mau[i%3]); g.drawLine(64+(int)(22*Math.cos(a)),64+(int)(22*Math.sin(a)),64+(int)(48*Math.cos(a)),64+(int)(48*Math.sin(a)));
        }
        g.setColor(Theme.STICKER_VANG); g.fillOval(54,54,20,20);
    }
    // Mieng pizza tam giac, vien banh va ba mieng topping.
    private static void pizza(Graphics2D g) {
        g.setColor(Theme.STICKER_VANG); g.fillPolygon(new int[]{17,110,64},new int[]{26,26,115},3); g.setColor(Theme.STICKER_DA); g.setStroke(new BasicStroke(14,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.drawLine(20,24,107,24);
        g.setColor(Theme.STICKER_DO); g.fillOval(38,41,18,18); g.fillOval(72,45,17,17); g.fillOval(56,73,16,16);
    }
    // Coc cafe va hoi nong.
    private static void cafe(Graphics2D g) {
        g.setColor(Theme.CHINH); g.drawOval(90,48,27,32); g.fillRoundRect(20,42,80,64,12,12);
        g.setColor(Theme.CANH_BAO); g.fillOval(24,44,72,18); g.setColor(Theme.VIEN); g.drawLine(12,113,115,113);
        g.setColor(Theme.PHU); g.drawArc(36,10,14,24,-90,180); g.drawArc(65,10,14,24,-90,180);
    }
}
