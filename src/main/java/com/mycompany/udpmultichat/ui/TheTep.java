package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.*;

/** Vai tro: The tep/anh, tien do va nut hanh dong. Ai goi: HangTin, TepPreview. Goi: Theme, TepTin, callback UI. */
@SuppressWarnings("serial")
final class TheTep extends JPanel {
    // ===== VE THE TEP KHONG CO MANG =====
    TheTep(TepTin t,boolean toi,TepTienDo td,BufferedImage anh,Consumer<String> hanhDong) {
        super(new BorderLayout(0,Theme.S8)); setOpaque(false); setBorder(Theme.le(Theme.S12));
        Color chu=toi?Theme.THE:Theme.CHU;
        JLabel ten=Theme.nhan(t.ten(),true); ten.setForeground(chu); ten.setToolTipText(t.ten()); ten.setIcon(bieuTuong(t.ten(),chu)); ten.setPreferredSize(new Dimension(272,32)); add(ten,BorderLayout.NORTH);
        JPanel giua=new JPanel(new BorderLayout(0,Theme.S8)); giua.setOpaque(false);
        if (anh!=null) {
            JLabel xem=new JLabel(new ImageIcon(anh)); xem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); xem.setToolTipText("Tải bản gốc và mở ảnh");
            xem.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { hanhDong.accept("Mở ảnh"); } }); giua.add(xem,BorderLayout.NORTH);
        }
        JPanel moTa=new JPanel(new BorderLayout(0,Theme.S4)); moTa.setOpaque(false);
        JLabel dungLuong=Theme.nhan(TepTin.dungLuong(t.kichThuoc()),false); dungLuong.setFont(Theme.NHO); dungLuong.setForeground(chu); moTa.add(dungLuong,BorderLayout.NORTH);
        JPanel duoi=new JPanel(new BorderLayout(0,Theme.S4)); duoi.setOpaque(false);
        if (td!=null) { tienDo(td,duoi,chu); }
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); nut.setOpaque(false);
        String state=td==null?"":td.trangThai();
        if (state.equals("QUEUE") || state.equals("SEND") || state.equals("DOWNLOAD")) { nut.add(nut("Hủy",hanhDong)); }
        else if (state.equals("ERROR") || state.equals("CANCEL")) { nut.add(nut("Thử lại",hanhDong)); }
        else if (td!=null && !td.gui() && state.equals("DONE")) { nut.add(nut("Mở thư mục",hanhDong)); }
        else { nut.add(nut("Tải về",hanhDong)); }
        duoi.add(nut,BorderLayout.SOUTH); moTa.add(duoi); giua.add(moTa); add(giua); setPreferredSize(new Dimension(304,getPreferredSize().height));
    }
    // Nen bo tron dong nhat bong tin cua minh/nguoi khac.
    @Override protected void paintComponent(Graphics g) { Theme.boTron(g,getWidth(),getHeight(),getForeground(),12); }
    // Nut nho co nen ro rang tren ca the xanh va the nhat.
    private static JButton nut(String ten,Consumer<String> hd) { JButton b=Theme.nut(ten,false); b.setFont(Theme.NHO); b.addActionListener(e -> hd.accept(ten)); return b; }
    // Trang thai tien do gom phan tram/toc do/thoi gian con lai, khong doan ket qua CRC.
    private static void tienDo(TepTienDo td,JPanel p,Color chu) {
        long size=td.tep().kichThuoc(); int n=(int)Math.min(100,td.daNhan()*100/Math.max(1,size));
        String text=switch (td.trangThai()) {
            case "QUEUE" -> "Đang chờ"; case "SEND" -> "Đang gửi "+n+"%"; case "DOWNLOAD" -> "Đang tải "+n+"%";
            case "DONE" -> "Hoàn tất"; case "CANCEL" -> "Đã hủy"; default -> "Lỗi: "+td.loi();
        };
        if ((td.trangThai().equals("SEND") || td.trangThai().equals("DOWNLOAD")) && td.tocDo()>0) {
            text+=" · "+TepTin.dungLuong((long)td.tocDo())+"/s · còn "+(long)Math.ceil((size-td.daNhan())/td.tocDo())+" giây";
        }
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(chu); l.setToolTipText(text); p.add(l,BorderLayout.NORTH);
        if (td.trangThai().equals("SEND") || td.trangThai().equals("DOWNLOAD")) {
            JProgressBar bar=new JProgressBar(0,100); bar.setValue(n); bar.setPreferredSize(new Dimension(260,4)); p.add(bar);
        }
    }
    // Icon tep ve bang cac khoi don gian, khong dung emoji/font ky hieu.
    private static Icon bieuTuong(String ten,Color mau) {
        return new Icon() {
            // Kich thuoc icon doc lap font emoji.
            public int getIconWidth() { return 32; }
            // Chieu cao co dinh de can giua dong ten.
            public int getIconHeight() { return 32; }
            // Ve bieu tuong tai lieu va duoi tep rut gon.
            public void paintIcon(Component c,Graphics g,int x,int y) {
                Graphics2D d=(Graphics2D)g.create(); d.setColor(mau); d.setStroke(new BasicStroke(1.5f)); d.drawRoundRect(x+4,y+2,22,28,3,3);
                int i=ten.lastIndexOf('.'); String ext=i<0?"FILE":ten.substring(i+1).toUpperCase(java.util.Locale.ROOT); if (ext.length()>4) { ext=ext.substring(0,4); }
                d.setFont(Theme.NHO.deriveFont(8f)); d.drawString(ext,x+6,y+20); d.dispose();
            }
        };
    }
}
