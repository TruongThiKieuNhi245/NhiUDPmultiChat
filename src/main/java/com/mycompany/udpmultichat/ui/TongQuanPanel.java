package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.*;
import java.awt.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

/** Vai tro: The thong ke va hai bieu do cot Java2D. Ai goi: QuanTriFrame, QuanTriPreview. Goi: Theme, AnhChupQuanTri. */
@SuppressWarnings("serial")
final class TongQuanPanel extends JPanel {
    private final JPanel cards=new JPanel(new GridLayout(3,3,Theme.S12,Theme.S12));
    private final Cot phong=new Cot("Thành viên theo phòng (top 8)"),ngay=new Cot("Tin nhắn 7 ngày gần nhất");
    // Bieu do va the co cung token voi cac cua so khac.
    TongQuanPanel(Runnable thongBao) {
        super(new BorderLayout(Theme.S16,Theme.S16)); setBorder(Theme.le(Theme.S16)); add(cards,BorderLayout.NORTH);
        JPanel bieuDo=new JPanel(new GridLayout(1,2,Theme.S16,0)); bieuDo.add(phong); bieuDo.add(ngay); add(bieuDo);
        JButton gui=Theme.nut("Gửi thông báo toàn hệ thống",true); gui.addActionListener(e -> thongBao.run()); JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT)); nut.add(gui); add(nut,BorderLayout.SOUTH);
    }
    // Snapshot chi tinh tren Server, UI khong tu dem lai file hay user.
    void capNhat(AnhChupQuanTri a) {
        cards.removeAll();
        for (var r:a.lay("STATS")) {
            JPanel p=Theme.theGradient(); p.setLayout(new BorderLayout(0,Theme.S8)); p.setBorder(Theme.le(Theme.S12));
            JLabel l=Theme.nhan(r.get(0),false); l.setForeground(Theme.PHU); p.add(l,BorderLayout.NORTH);
            JLabel n=Theme.nhan(r.get(0).equals("Dung lượng tệp")?TepTin.dungLuong(Long.parseLong(r.get(1))):r.get(1),true);
            n.setFont(Theme.DAM.deriveFont(24f)); n.setForeground(Theme.CHINH); p.add(n); cards.add(p);
        }
        Map<String,Long> p=new LinkedHashMap<>(); a.lay("ROOMS").stream().sorted(Comparator.comparingLong((java.util.List<String> r) -> Long.parseLong(r.get(6))).reversed()).limit(8).forEach(r -> p.put(r.get(2)+" · "+r.get(1),Long.parseLong(r.get(6)))); phong.dat(p);
        Map<String,Long> ds=new LinkedHashMap<>(); LocalDate now=Instant.ofEpochMilli(a.luc()).atZone(ZoneId.systemDefault()).toLocalDate(); for (int i=6;i>=0;i--) { ds.put(now.minusDays(i).toString(),0L); }
        for (var r:a.lay("DAYS")) { if (ds.containsKey(r.get(0))) { ds.merge(r.get(0),Long.parseLong(r.get(2)),Long::sum); } } ngay.dat(ds); revalidate(); repaint();
    }
    /** Bieu do don gian co nhan truc va gia tri; dung gradient token Theme. */
    private static final class Cot extends JPanel {
        private final String ten; private Map<String,Long> ds=Map.of();
        Cot(String ten) { this.ten=ten; setPreferredSize(new Dimension(430,280)); setBackground(Theme.THE); setBorder(BorderFactory.createLineBorder(Theme.VIEN,1,true)); }
        void dat(Map<String,Long> d) { ds=new LinkedHashMap<>(d); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D d=(Graphics2D)g.create(); d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            d.setFont(Theme.DAM); d.setColor(Theme.CHU); d.drawString(ten,16,24);
            long max=Math.max(1,ds.values().stream().mapToLong(Long::longValue).max().orElse(1));
            int step=(getWidth()-32)/Math.max(1,ds.size()),i=0,base=getHeight()-48,h=Math.max(40,getHeight()-100);
            d.setFont(Theme.NHO);
            for (var e:ds.entrySet()) {
                int x=16+i++*step,cao=(int)(h*e.getValue()/max),cotW=Math.max(10,step-12);
                d.setPaint(new java.awt.GradientPaint(x,base-cao,Theme.GRADIENT_START,x,base,Theme.GRADIENT_END));
                d.fillRoundRect(x,base-cao,cotW,cao,6,6);
                d.setColor(Theme.CHU); d.drawString(""+e.getValue(),x+Math.max(0,(cotW-d.getFontMetrics().stringWidth(""+e.getValue()))/2),base-cao-6);
                String n=e.getKey(); while (n.length()>1 && d.getFontMetrics().stringWidth(n)>step-4) { n=n.substring(0,n.length()-1); }
                d.setColor(Theme.PHU); d.drawString(n,x,base+20);
            } d.dispose();
        }
    }
}
