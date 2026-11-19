package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.*;
import javax.swing.*;

/** Vai tro: Mot tin co thanh thao tac hover, menu va trang thai gui. Ai goi: KhungChatPanel, UiPreview. Goi: ThanhPhan, StickerKho, Theme. */
@SuppressWarnings("serial")
final class HangTin extends JPanel {
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private TinNhan tin;
    private final boolean toi;
    private final Supplier<BufferedImage> anh;
    private final Supplier<String> ten;
    private final Consumer<String> hanhDong;
    private final BooleanSupplier quanLy;
    private String trangThai="",lyDo="";
    private boolean hienDaGui=true;
    private com.mycompany.udpmultichat.common.TepTienDo tepTienDo;
    private Supplier<BufferedImage> anhTep=() -> null;
    // Tien do va thumbnail chi la snapshot, khong truy cap dia/mang.
    void datTep(com.mycompany.udpmultichat.common.TepTienDo td,Supplier<BufferedImage> anh) { tepTienDo=td; anhTep=anh; veLai(); }
    private final JPanel the=new JPanel(new BorderLayout(0,Theme.S4));
    private final JPanel barHanhDong=taoBarHanhDong();

    // Renderer doc cache RAM; callback chi gui lenh qua UI/Core.
    HangTin(TinNhan t,boolean toi,Supplier<BufferedImage> anh,Supplier<String> ten,BooleanSupplier quanLy,Consumer<String> hanhDong) {
        super(new FlowLayout(toi?FlowLayout.RIGHT:FlowLayout.LEFT,Theme.S16,Theme.S8));
        tin=t; this.toi=toi; this.anh=anh; this.ten=ten; this.quanLy=quanLy; this.hanhDong=hanhDong;
        setOpaque(false); the.setOpaque(false); veLai();
    }
    // Tao thanh thao tac hover an mac dinh, chi hien khi chuot vao vung tin.
    private JPanel taoBarHanhDong() {
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.CENTER,2,0)) {
            @Override protected void paintComponent(Graphics g) {
                Theme.boTron(g,getWidth(),getHeight(),Theme.THE,Theme.BO); super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(Theme.VIEN,1,true),Theme.le(2)));
        bar.setVisible(false);
        return bar;
    }
    // Tin Server thay tin cuc bo, khong tao them dong khi ma Client trung.
    void capNhat(TinNhan t) { tin=t; veLai(); }
    // Doc tin hien tai de menu luon dung msgId Server gan.
    TinNhan tin() { return tin; }
    // Trang thai chi duoc Core phat, renderer khong tu doan ACK.
    void trangThai(String ma,String loi) { trangThai=ma; lyDo=loi; veLai(); }
    // Chi tin moi nhat cua minh duoc hien nhan Da gui.
    void hienDaGui(boolean hien) { if (hienDaGui!=hien) { hienDaGui=hien; veLai(); } }
    // Doc vai tro cua hang de chon tin moi nhat trong phong.
    boolean laToi() { return toi; }
    // Ve lai giu JPanel ngoai de khong doi vi tri trong khung chat.
    void veLai() {
        removeAll(); the.removeAll();
        if (!toi) {
            JPanel tren=new JPanel(new BorderLayout()); tren.setOpaque(false); tren.add(new ThanhPhan.Avatar(ten.get(),null,anh),BorderLayout.NORTH); add(tren);
        }
        JComponent nd=noiDung(); the.add(nd);
        JPanel duoi=new JPanel(new BorderLayout(Theme.S8,0)); duoi.setOpaque(false);
        JLabel gio=Theme.nhan(GIO.format(Instant.ofEpochMilli(tin.thoiGian)),false); gio.setFont(Theme.NHO); gio.setForeground(Theme.PHU);
        duoi.add(gio,BorderLayout.WEST); if (toi && !tin.loai.equals("F") && !trangThai.isEmpty() && (!trangThai.equals("SENT") || hienDaGui)) { duoi.add(trangThaiGui()); }
        the.add(duoi,BorderLayout.SOUTH);
        napBarHanhDong();
        if (toi) { add(barHanhDong); add(the); } else { add(the); add(barHanhDong); }
        if (!toi && getComponentCount()>2) { getComponent(0).setPreferredSize(new Dimension(40,the.getPreferredSize().height)); }
        ganMenu(this); ganHover(this);
        revalidate(); repaint();
    }
    // Nap cac nut vao thanh hover: sao chep, thu hoi va menu tuy chon.
    private void napBarHanhDong() {
        barHanhDong.removeAll(); barHanhDong.setVisible(false);
        if (tin.thuHoi || tin.loai.equals("Y")) { barHanhDong.putClientProperty("co",false); return; }
        if (!tin.loai.equals("F") && !tin.loai.equals("S")) {
            JButton cp=Theme.nutNhe("","copy"); cp.setToolTipText("Sao chép");
            cp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cp.addActionListener(e -> hanhDong.accept("Sao chép")); barHanhDong.add(cp);
        }
        if (toi && tin.msgId>0 && (quanLy.getAsBoolean() || System.currentTimeMillis()-tin.thoiGian<=Protocol.THU_HOI_MS)) {
            JButton th=Theme.nutNhe("","undo"); th.setForeground(Theme.LOI); th.setToolTipText("Thu hồi tin nhắn");
            th.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            th.addActionListener(e -> hanhDong.accept("Thu hồi")); barHanhDong.add(th);
        }
        JButton more=Theme.nutNhe("","more"); more.setToolTipText("Tuỳ chọn khác");
        more.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        more.addActionListener(e -> menu().show(more,0,more.getHeight())); barHanhDong.add(more);
        barHanhDong.putClientProperty("co",true);
    }
    // Hover lan xuong moi component con: vao hien bar, ra khoi hang an bar.
    private void ganHover(JComponent c) {
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (Boolean.TRUE.equals(barHanhDong.getClientProperty("co"))) { barHanhDong.setVisible(true); }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                Point p=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),HangTin.this);
                if (!HangTin.this.contains(p)) { barHanhDong.setVisible(false); }
            }
        });
        for (var con:c.getComponents()) {
            if (con instanceof JComponent j && j!=barHanhDong) { ganHover(j); }
        }
    }
    // Sticker khong bong nen; thu hoi khong giu noi dung trong component hay tooltip.
    private JComponent noiDung() {
        if (tin.loai.equals("Y")) {
            JLabel l=Theme.nhan("Quản trị viên: "+tin.noiDung,false); l.setForeground(Theme.THONG_TIN); l.setOpaque(true); l.setBackground(Theme.NEN_TIN); l.setBorder(Theme.le(Theme.S12)); return l;
        }
        if (tin.thuHoi) {
            JLabel l=Theme.nhan("Tin nhắn đã được thu hồi",false); l.setFont(Theme.FONT.deriveFont(Font.ITALIC)); l.setForeground(Theme.PHU); return l;
        }
        if (tin.loai.equals("F")) {
            try {
                var t=com.mycompany.udpmultichat.common.TepTin.doc(tin.roomId,tin.userId,tin.noiDung,tin.thoiGian);
                TheTep p=new TheTep(t,toi,tepTienDo,anhTep.get(),hanhDong); p.setForeground(toi?Theme.CHINH:Theme.NEN);
                if (toi) { return p; }
                JPanel boc=new JPanel(new BorderLayout(0,Theme.S4)); boc.setOpaque(false);
                JLabel nguoi=Theme.nhan(ten.get(),true); nguoi.setFont(Theme.NHO.deriveFont(Font.BOLD)); boc.add(nguoi,BorderLayout.NORTH); boc.add(p); return boc;
            } catch (IllegalArgumentException e) { return Theme.nhan("Tệp không hỗ trợ",false); }
        }
        if (tin.loai.equals("S")) {
            JPanel p=new JPanel(new BorderLayout(0,Theme.S4)); p.setOpaque(false); JLabel n=Theme.nhan(ten.get(),true); n.setFont(Theme.NHO.deriveFont(Font.BOLD)); p.add(n,BorderLayout.NORTH);
            JLabel h=Theme.nhan(StickerKho.ten(tin.noiDung),false);
            if (StickerKho.anh(tin.noiDung)!=null) { h.setText(""); h.setIcon(StickerKho.icon(tin.noiDung,112)); }
            h.setToolTipText(StickerKho.ten(tin.noiDung)); p.add(h); return p;
        }
        return new ThanhPhan.BongTin(ten.get(),tin.noiDung,"",toi);
    }
    // Loi co Gui lai/Xoa; nut khong lam doi ma tin.
    private JPanel trangThaiGui() {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S4,0)); p.setOpaque(false);
        String text=switch (trangThai) { case "WAIT" -> "Đang gửi"; case "ERROR" -> "Lỗi"; default -> "Đã gửi"; };
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(trangThai.equals("ERROR")?Theme.LOI:Theme.PHU); l.setToolTipText(lyDo); p.add(l);
        if (trangThai.equals("ERROR")) {
            for (String lenh:new String[]{"Gửi lại","Xóa"}) { JButton b=Theme.nutNhe(lenh,""); b.setFont(Theme.NHO); b.setForeground(Theme.CHINH); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { b.setFont(Theme.NHO.deriveFont(java.util.Map.of(java.awt.font.TextAttribute.UNDERLINE,java.awt.font.TextAttribute.UNDERLINE_ON))); }
                    public void mouseExited(java.awt.event.MouseEvent e) { b.setFont(Theme.NHO); }
                }); b.addActionListener(e -> hanhDong.accept(lenh)); p.add(b); }
        }
        return p;
    }
    // Chuot phai tren ca noi dung, avatar va trang thai deu mo cung menu.
    private void ganMenu(JComponent c) {
        c.setComponentPopupMenu(menu());
        for (var con:c.getComponents()) { if (con instanceof JComponent j && j!=barHanhDong) { ganMenu(j); } }
    }
    // Server kiem tra lai quyen va 10 phut, khong tin viec an menu tren Client.
    JPopupMenu menu() {
        JPopupMenu m=new JPopupMenu(); m.setBorder(javax.swing.BorderFactory.createCompoundBorder(m.getBorder(),Theme.le(Theme.S4)));
        if (!tin.thuHoi) {
            JMenuItem copy=new JMenuItem("Sao chép"); copy.addActionListener(e -> DanhSachPhongPanel.saoChep(tin.loai.equals("S")?StickerKho.ten(tin.noiDung):tin.noiDung)); m.add(copy);
            if (tin.msgId>0 && (quanLy.getAsBoolean() || (toi && System.currentTimeMillis()-tin.thoiGian<=Protocol.THU_HOI_MS))) {
                m.addSeparator(); JMenuItem thu=new JMenuItem("Thu hồi"); thu.setForeground(Theme.LOI); thu.addActionListener(e -> hanhDong.accept("Thu hồi")); m.add(thu);
            }
        }
        return m;
    }
}
