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

/** Toast tren layered pane, toi da ba the; hover dung dem gio, xoa ca Timer khi dong. */
public final class ThongBao implements AutoCloseable {
    public enum Muc { THANH_CONG, LOI, CANH_BAO, THONG_TIN }
    private final JLayeredPane lop;
    private final List<Toast> ds=new ArrayList<>();
    public ThongBao(JLayeredPane lop) {
        this.lop=lop;
        lop.addComponentListener(new ComponentAdapter() { @Override public void componentResized(ComponentEvent e) { sapXep(); } });
    }
    // Ham nay phai duoc goi tren EDT.
    public void hien(Muc muc,String noiDung) {
        if (ds.size()==3) { xoa(ds.get(0)); }
        Toast t=new Toast(muc,noiDung); ds.add(t); lop.add(t,JLayeredPane.POPUP_LAYER); sapXep();
    }
    private void sapXep() {
        int y=Theme.S16,w=Math.min(370,Math.max(220,lop.getWidth()-Theme.S24*2));
        for (Toast t:ds) { t.setBounds(Math.max(8,lop.getWidth()-w-Theme.S16),y,w,86); y+=98; }
        lop.repaint();
    }
    private void xoa(Toast t) { t.timer.stop(); ds.remove(t); lop.remove(t); lop.repaint(); }
    @Override public void close() { for (Toast t:List.copyOf(ds)) { xoa(t); } }

    /** Mot the toast: bieu tuong, thanh mau, noi dung va nut dong. */
    @SuppressWarnings("serial")
    private final class Toast extends JPanel {
        private final Timer timer;
        private int con;
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
                @Override public void mouseEntered(MouseEvent e) { timer.stop(); }
                @Override public void mouseExited(MouseEvent e) {
                    java.awt.Point p=javax.swing.SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),Toast.this);
                    if (!contains(p)) { timer.start(); }
                }
            };
            ganHover(this,hover);
        }
        private void ganHover(Component c,MouseAdapter m) {
            c.addMouseListener(m);
            if (c instanceof Container n) { for (Component con:n.getComponents()) { ganHover(con,m); } }
        }
    }
}
