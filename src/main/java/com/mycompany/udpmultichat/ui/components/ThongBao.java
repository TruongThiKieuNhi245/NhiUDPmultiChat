package com.mycompany.udpmultichat.ui.components;

import com.mycompany.udpmultichat.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

/** Vai tro: Toast tren layered pane, toi da ba the; hover dung dem gio, xoa ca Timer khi dong. Ai goi: ChatMulticastUI. Goi: Theme, Swing Timer. */
public final class ThongBao implements AutoCloseable {
    // ===== VAI TRO VA CAC THAO TAC =====
    public enum Muc { THANH_CONG, LOI, CANH_BAO, THONG_TIN }
    private final JLayeredPane lop;
    private final List<Toast> ds=new ArrayList<>();
    private javax.swing.JComponent vung;
    private final ComponentAdapter doiCo=new ComponentAdapter() { public void componentResized(ComponentEvent e) { sapXep(); } };
    // Khoi tao lop va noi cac thanh phan can thiet.
    public ThongBao(JLayeredPane lop) {
        this.lop=lop;
        lop.addComponentListener(new ComponentAdapter() { @Override public void componentResized(ComponentEvent e) { sapXep(); } });
    }
    // Ham nay phai duoc goi tren EDT.
    public void hien(Muc muc,String noiDung) {
        if (ds.size()==3) { xoa(ds.get(0)); }
        Toast t=new Toast(muc,noiDung); ds.add(t); lop.add(t,JLayeredPane.POPUP_LAYER); sapXep();
    }
    // Dat vung chat de toast nam ngay tren o soan, khong che toolbar/header.
    public void datVung(javax.swing.JComponent vung) {
        if (this.vung!=null) { this.vung.removeComponentListener(doiCo); }
        this.vung=vung;
        if (vung!=null) { vung.addComponentListener(doiCo); }
        sapXep();
    }
    // Sap xep.
    private void sapXep() {
        java.awt.Rectangle r=new java.awt.Rectangle(0,0,lop.getWidth(),lop.getHeight()-Theme.S24);
        if (vung!=null && vung.getParent()!=null && vung.getWidth()>0) {
            r=javax.swing.SwingUtilities.convertRectangle(vung.getParent(),vung.getBounds(),lop);
        }
        int w=Math.min(Theme.TOAST_RONG,Math.max(220,r.width-Theme.S24)),y=r.y+r.height-Theme.S8;
        for (int i=ds.size()-1;i>=0;i--) {
            y-=Theme.TOAST_CAO; ds.get(i).setBounds(r.x+(r.width-w)/2,Math.max(r.y,y),w,Theme.TOAST_CAO); y-=Theme.S8;
        }
        lop.repaint();
    }
    // Xoa.
    private void xoa(Toast t) { t.timer.stop(); ds.remove(t); lop.remove(t); lop.repaint(); }
    // Dong tai nguyen cua lop, ke ca khi co loi.
    @Override public void close() { for (Toast t:List.copyOf(ds)) { xoa(t); } if (vung!=null) { vung.removeComponentListener(doiCo); } }

    /** Mot the toast: bieu tuong, thanh mau, noi dung va nut dong. */
    @SuppressWarnings("serial")
    private final class Toast extends JPanel {
        private final Timer timer;
        private int con;
        // Tao mot toast co icon, mau va thoi gian tu dong dong.
        Toast(Muc muc,String noiDung) {
            super(new BorderLayout(Theme.S8,0));
            Color mau=switch(muc) { case THANH_CONG -> Theme.THANH_CONG; case LOI -> Theme.LOI; case CANH_BAO -> Theme.CANH_BAO; case THONG_TIN -> Theme.THONG_TIN; };
            setBackground(Theme.THE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.VIEN),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,4,0,0,mau),Theme.le(Theme.S12))));
            var bieuTuong=Theme.nhan("",true); bieuTuong.setForeground(mau); bieuTuong.setIcon(Theme.icon(muc==Muc.THANH_CONG?"ok":"info")); add(bieuTuong,BorderLayout.WEST);
            JTextArea text=new JTextArea(noiDung); text.setFont(Theme.FONT); text.setForeground(Theme.CHU);
            text.setOpaque(false); text.setEditable(false); text.setFocusable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
            add(text,BorderLayout.CENTER);
            JButton dong=Theme.nut("×",false); dong.setBorder(Theme.le(0)); dong.setPreferredSize(new Dimension(22,22));
            dong.addActionListener(e -> xoa(this)); add(dong,BorderLayout.EAST);
            con=muc==Muc.LOI?6000:3800;
            timer=new Timer(100,e -> { con-=100; if (con<=0) { xoa(this); } }); timer.start();
            MouseAdapter hover=new MouseAdapter() {
                // Cap nhat khi chuot vao thanh phan.
                @Override public void mouseEntered(MouseEvent e) { timer.stop(); }
                // Cap nhat khi chuot roi thanh phan.
                @Override public void mouseExited(MouseEvent e) {
                    java.awt.Point p=javax.swing.SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),Toast.this);
                    if (!contains(p)) { timer.start(); }
                }
            };
            ganHover(this,hover);
        }
        // Gan hover.
        private void ganHover(Component c,MouseAdapter m) {
            c.addMouseListener(m);
            if (c instanceof Container n) { for (Component con:n.getComponents()) { ganHover(con,m); } }
        }
    }
}
