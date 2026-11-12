package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Vai tro: Noi dung mot tab: bong tin va dong he thong; tieu de/chuc nang phong o cua so chinh. Ai goi: ChatMulticastUI. Goi: ThanhPhan, Theme. */
@SuppressWarnings("serial")
final class KhungChatPanel extends JPanel {
    // ===== VAI TRO VA CAC THAO TAC =====
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private final JPanel ds=new JPanel(); private final JScrollPane cuon;
    private Phong phong;
    private java.util.function.Function<String,java.awt.image.BufferedImage> anh=id -> null;
    private java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo=id -> null;
    private final java.util.Map<String,java.util.List<ThanhPhan.BongTin>> bong=new java.util.HashMap<>();
    private final java.util.Map<String,java.util.List<ThanhPhan.Avatar>> avatar=new java.util.HashMap<>();
    // Cache duoc doc truc tiep tren EDT, khong chan mang.
    void datHoSo(java.util.function.Function<String,java.awt.image.BufferedImage> anh,java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo) {
        this.anh=anh; this.hoSo=hoSo;
    }
    // Ve lai ten va avatar, giu nguyen thu tu tin va vi tri cuon.
    void hoSoMoi(com.mycompany.udpmultichat.common.model.NguoiDung n) {
        bong.getOrDefault(n.id,java.util.List.of()).forEach(b -> b.tenMoi(n.tenHienThi));
        avatar.getOrDefault(n.id,java.util.List.of()).forEach(a -> a.tenMoi(n.tenHienThi)); ds.revalidate(); ds.repaint();
    }
    // Khoi tao lop va noi cac thanh phan can thiet.
    KhungChatPanel(Phong phong) {
        super(new BorderLayout()); this.phong=phong; ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS)); ds.setBackground(Theme.THE);
        JPanel nen=new JPanel(new BorderLayout()); nen.setBackground(Theme.THE); nen.add(ds,BorderLayout.NORTH);
        cuon=new JScrollPane(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16); add(cuon,BorderLayout.CENTER);
    }
    // Tra thong tin phong cua khung chat dang giu.
    Phong phong() { return phong; }
    // Cap nhat.
    void capNhat(Phong p) { phong=p; }
    // Them tin.
    void themTin(TinNhan t,boolean toi) {
        JPanel hang=new JPanel(new FlowLayout(toi?FlowLayout.RIGHT:FlowLayout.LEFT,Theme.S16,Theme.S8)); hang.setOpaque(false);
        var n=hoSo.apply(t.userId); String ten=n==null?t.ten:n.tenHienThi;
        if (!toi) {
            ThanhPhan.Avatar a=new ThanhPhan.Avatar(ten,null,() -> anh.apply(t.userId));
            avatar.computeIfAbsent(t.userId,id -> new java.util.ArrayList<>()).add(a); hang.add(a);
        }
        ThanhPhan.BongTin b=new ThanhPhan.BongTin(ten,t.noiDung,GIO.format(Instant.ofEpochMilli(t.thoiGian)),toi);
        bong.computeIfAbsent(t.userId,id -> new java.util.ArrayList<>()).add(b); hang.add(b); them(hang);
    }
    // He thong.
    void heThong(String text) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(Theme.PHU); hang.add(l); them(hang);
    }
    // Them.
    private void them(JPanel hang) {
        var bar=cuon.getVerticalScrollBar(); boolean oDay=bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-36;
        ds.add(hang); ds.revalidate(); ds.repaint();
        if (oDay) { SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum())); }
    }
}
