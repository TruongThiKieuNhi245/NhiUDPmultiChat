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
    public static final Color GRADIENT_START=new Color(0x4F,0x6B,0xED),GRADIENT_END=new Color(0x25,0x63,0xEB);
    public static final Color GRADIENT_HOVER_START=new Color(0x43,0x58,0xD0),GRADIENT_HOVER_END=new Color(0x1D,0x4E,0xD8);
    public static final Color GRADIENT_ACTIVE_START=new Color(0x38,0x48,0xB0),GRADIENT_ACTIVE_END=new Color(0x1A,0x3F,0xB0);
    public static final Color GRADIENT_DO_START=new Color(0xEF,0x44,0x44),GRADIENT_DO_END=new Color(0xDC,0x26,0x26);
    public static final Color VIEN_CHON=new Color(0xC7,0xD2,0xFE);

    // Mau nen Pastel nhe nhang, khong dam khong choi cho phong chat
    public static final Color PASTEL_MAC_DINH = new Color(0xF8, 0xFA, 0xFC);
    public static final Color PASTEL_MAY = new Color(0xF0, 0xF7, 0xFF);
    public static final Color PASTEL_BAC_HA = new Color(0xF0, 0xFD, 0xF4);
    public static final Color PASTEL_OAI_HUONG = new Color(0xF5, 0xF3, 0xFF);
    public static final Color PASTEL_DAO = new Color(0xFF, 0xF7, 0xED);
    public static final Color PASTEL_HONG = new Color(0xFF, 0xF1, 0xF2);
    public static final Color PASTEL_SAGE = new Color(0xF7, 0xFE, 0xE7);
    public static final Color PASTEL_CAT = new Color(0xFA, 0xF6, 0xF0);

    public record MauPastel(String ten, Color mau) {}
    public static final java.util.List<MauPastel> DANH_SACH_PASTEL = java.util.List.of(
        new MauPastel("Mặc định", PASTEL_MAC_DINH),
        new MauPastel("Xanh mây", PASTEL_MAY),
        new MauPastel("Bạc hà", PASTEL_BAC_HA),
        new MauPastel("Oải hương", PASTEL_OAI_HUONG),
        new MauPastel("Cam đào", PASTEL_DAO),
        new MauPastel("Hồng phấn", PASTEL_HONG),
        new MauPastel("Cốm non", PASTEL_SAGE),
        new MauPastel("Cát ấm", PASTEL_CAT)
    );
    public static final Font FONT=new Font("Segoe UI",Font.PLAIN,14),DAM=FONT.deriveFont(Font.BOLD),NHO=FONT.deriveFont(12f);
    public static final Font DON_CACH=new Font(tenFontLog(),Font.PLAIN,13);
    public static final int NUT_GUI=36,TOAST_CAO=48,TOAST_RONG=360,O_NHAP_CAO=32;
    public static final int TOAST_MS=4500,TOAST_LOI_MS=6500;
    // Khoi tao lop va noi cac thanh phan can thiet.
    private Theme() { }
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
    // Nut bam: nut chinh co gradient chuyen sac hien dai, nut phu co vien nhe.
    public static JButton nut(String ten,boolean chinh) {
        if (!chinh) {
            JButton b=new JButton(ten); b.setFont(DAM); return b;
        }
        @SuppressWarnings("serial")
        JButton b=new JButton(ten) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(),h=getHeight();
                boolean nhan=getModel().isPressed(),hover=getModel().isRollover();
                Color c1=nhan?GRADIENT_ACTIVE_START:hover?GRADIENT_HOVER_START:GRADIENT_START;
                Color c2=nhan?GRADIENT_ACTIVE_END:hover?GRADIENT_HOVER_END:GRADIENT_END;
                if (!isEnabled()) { c1=new Color(0xC7,0xCE,0xDC); c2=new Color(0xAE,0xB6,0xC8); }
                g2.setPaint(new java.awt.GradientPaint(0,0,c1,w,h,c2));
                g2.fillRoundRect(0,0,w,h,BO*2,BO*2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(DAM); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setForeground(THE);
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return b;
    }
    // Nut nhe chi hien nen khi hover, dung cho toolbar va icon.
    public static JButton nutNhe(String ten,String icon) {
        JButton b=new JButton(ten); b.setFont(DAM); b.putClientProperty("JButton.buttonType","toolBarButton");
        if (!icon.isEmpty()) { b.setIcon(Theme.icon(icon)); } return b;
    }
    // Nut bat/tat hien mat khau dung icon mat Java2D, khong dung chu.
    public static JButton nutMatKhau(javax.swing.JPasswordField o) {
        JButton b=nutNhe("","eye-off");
        b.setToolTipText("Hiện mật khẩu");
        b.setFocusable(false);
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        char echo=o.getEchoChar();
        b.addActionListener(e -> {
            boolean an=o.getEchoChar()!=0;
            o.setEchoChar(an?(char)0:echo);
            b.setIcon(icon(an?"eye":"eye-off"));
            b.setToolTipText(an?"Ẩn mật khẩu":"Hiện mật khẩu");
        });
        return b;
    }
    // Nut gui hinh tron voi gradient hien dai va icon trang.
    @SuppressWarnings("serial")
    public static JButton nutGui() {
        JButton b=new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(),h=getHeight();
                boolean nhan=getModel().isPressed(),hover=getModel().isRollover();
                Color c1=nhan?GRADIENT_ACTIVE_START:hover?GRADIENT_HOVER_START:GRADIENT_START;
                Color c2=nhan?GRADIENT_ACTIVE_END:hover?GRADIENT_HOVER_END:GRADIENT_END;
                if (!isEnabled()) { c1=new Color(0xC7,0xCE,0xDC); c2=new Color(0xAE,0xB6,0xC8); }
                g2.setPaint(new java.awt.GradientPaint(0,0,c1,w,h,c2));
                g2.fillOval(0,0,w,h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setIcon(icon("send")); b.setToolTipText("Gửi tin nhắn");
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setForeground(THE); b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        b.setPreferredSize(new java.awt.Dimension(NUT_GUI,NUT_GUI));
        return b;
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
    // Bo tron gradient chuyen sac.
    public static void boTronGradient(Graphics g,int w,int h,Color c1,Color c2,int bo) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new java.awt.GradientPaint(0,0,c1,w,h,c2));
        g2.fillRoundRect(0,0,w,h,bo*2,bo*2); g2.dispose();
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
                a.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
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
            case "warning": a.drawPolygon(new int[]{9,1,17},new int[]{1,16,16},3); a.drawLine(9,6,9,10); a.fillOval(8,12,2,2); break;
            case "smile": a.drawOval(2,2,14,14); a.fillOval(6,6,2,2); a.fillOval(11,6,2,2); a.drawArc(6,8,7,5,180,180); break;
            case "ok": a.drawPolyline(new int[]{3,7,15},new int[]{9,13,4},3); break;
            case "down": a.drawPolyline(new int[]{4,9,14},new int[]{6,11,6},3); break;
            case "up": a.drawPolyline(new int[]{4,9,14},new int[]{11,6,11},3); break;
            case "send": a.drawLine(9,15,9,3); a.drawPolyline(new int[]{4,9,14},new int[]{8,3,8},3); break;
            case "plus": a.drawLine(9,3,9,15); a.drawLine(3,9,15,9); break;
            case "copy": a.drawRoundRect(3,3,8,8,2,2); a.drawRoundRect(6,6,8,8,2,2); break;
            case "undo": {
                a.drawPolyline(new int[]{6,3,6},new int[]{3,6,9},3);
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(4,6); p.quadTo(12,3,14,9); p.quadTo(15,14,10,15); p.quadTo(6,15,6,12);
                a.draw(p); break;
            }
            case "reply": {
                a.drawPolyline(new int[]{7,3,7},new int[]{4,7,10},3);
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(4,7); p.quadTo(10,7,13,14);
                a.draw(p); break;
            }
            case "search": {
                a.drawOval(3,3,8,8);
                a.drawLine(9,9,14,14);
                break;
            }
            case "close": {
                a.drawLine(4,4,13,13);
                a.drawLine(13,4,4,13);
                break;
            }
            case "eye": {
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(2,9); p.quadTo(9,3,16,9); p.quadTo(9,15,2,9);
                a.draw(p); a.drawOval(7,7,4,4); a.fillOval(8,8,2,2);
                break;
            }
            case "eye-off": {
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(2,9); p.quadTo(9,3,16,9); p.quadTo(9,15,2,9);
                a.draw(p); a.drawLine(3,3,15,15);
                break;
            }
            case "bell-off": {
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(6,8); p.quadTo(6,4,9,4); p.quadTo(12,4,12,8);
                p.lineTo(14,12); p.lineTo(4,12); p.closePath();
                a.draw(p);
                a.drawArc(8,12,2,2,0,180);
                a.drawLine(3,3,15,15);
                break;
            }
            case "palette": {
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(9,2); p.quadTo(16,2,16,8); p.quadTo(16,13,11,13);
                p.quadTo(9,13,9,15); p.quadTo(9,16,7,16); p.quadTo(2,16,2,9);
                p.quadTo(2,2,9,2); p.closePath();
                a.draw(p);
                a.fillOval(5,5,2,2); a.fillOval(9,4,2,2); a.fillOval(12,7,2,2);
                break;
            }
            case "mail": {
                a.drawRoundRect(2,4,14,10,2,2);
                a.drawPolyline(new int[]{2,9,16},new int[]{5,10,5},3);
                break;
            }
            case "bell": {
                java.awt.geom.Path2D p=new java.awt.geom.Path2D.Float();
                p.moveTo(6,8); p.quadTo(6,4,9,4); p.quadTo(12,4,12,8);
                p.lineTo(14,12); p.lineTo(4,12); p.closePath();
                a.draw(p); a.drawArc(8,12,2,2,0,180);
                break;
            }
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
    // The hien dai voi dai mau gradient diem nhan o mep tren.
    @SuppressWarnings("serial")
    public static JPanel theGradient() {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(THE); g2.fillRoundRect(0,0,getWidth(),getHeight(),BO*2,BO*2);
                g2.setPaint(new java.awt.GradientPaint(0,0,GRADIENT_START,getWidth(),0,GRADIENT_END));
                g2.fillRoundRect(0,0,getWidth(),3,BO*2,BO*2);
                g2.setColor(VIEN); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,BO*2,BO*2);
                g2.dispose();
            }
        };
    }
    // Icon ung dung ve trong code, khong can tep anh hoac thu vien moi.
    public static java.awt.Image iconUngDung() {
        var anh=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=anh.createGraphics(); boTron(g,32,32,CHINH,BO); g.setColor(THE); g.setFont(DAM);
        g.drawString("U",10,22); g.dispose(); return anh;
    }
    // Anh tron Java2D dung chung cho ho so, the tai khoan va danh sach/tin nhan.
    public static javax.swing.Icon avatar(String ten,java.awt.image.BufferedImage anh,int canh) {
        return new javax.swing.Icon() {
            // Kich thuoc icon theo noi su dung.
            public int getIconWidth() { return canh; }
            // Giu avatar vuong de cat tron.
            public int getIconHeight() { return canh; }
            // Renderer chi doc anh da san sang trong RAM.
            public void paintIcon(java.awt.Component c,Graphics g,int x,int y) {
                Graphics2D a=(Graphics2D)g.create(); a.translate(x,y);
                a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                a.setClip(new java.awt.geom.Ellipse2D.Double(0,0,canh,canh));
                if (anh!=null) { a.drawImage(anh,0,0,canh,canh,null); }
                else {
                    a.setColor(AVATAR); a.fillOval(0,0,canh,canh); a.setColor(THE); a.setFont(DAM.deriveFont(canh*0.42f));
                    String t=ten.isBlank()?"?":ten.substring(0,ten.offsetByCodePoints(0,1)).toUpperCase(java.util.Locale.ROOT);
                    var fm=a.getFontMetrics(); a.drawString(t,(canh-fm.stringWidth(t))/2,(canh-fm.getHeight())/2+fm.getAscent());
                }
                a.dispose();
            }
        };
    }

}
