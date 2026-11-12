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

/** Noi dung mot tab: bong tin va dong he thong; tieu de/chuc nang phong o cua so chinh. */
@SuppressWarnings("serial")
final class KhungChatPanel extends JPanel {
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private final JPanel ds=new JPanel(); private final JScrollPane cuon;
    private Phong phong;
    KhungChatPanel(Phong phong) {
        super(new BorderLayout()); this.phong=phong; ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS)); ds.setBackground(Theme.THE);
        JPanel nen=new JPanel(new BorderLayout()); nen.setBackground(Theme.THE); nen.add(ds,BorderLayout.NORTH);
        cuon=new JScrollPane(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16); add(cuon,BorderLayout.CENTER);
    }
    Phong phong() { return phong; }
    void capNhat(Phong p) { phong=p; }
    void themTin(TinNhan t,boolean toi) {
        JPanel hang=new JPanel(new FlowLayout(toi?FlowLayout.RIGHT:FlowLayout.LEFT,Theme.S16,Theme.S8)); hang.setOpaque(false);
        if (!toi) { hang.add(new ThanhPhan.Avatar(t.ten,true)); }
        hang.add(new ThanhPhan.BongTin(t.ten,t.noiDung,GIO.format(Instant.ofEpochMilli(t.thoiGian)),toi)); them(hang);
    }
    void heThong(String text) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(Theme.PHU); hang.add(l); them(hang);
    }
    private void them(JPanel hang) {
        var bar=cuon.getVerticalScrollBar(); boolean oDay=bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-36;
        ds.add(hang); ds.revalidate(); ds.repaint();
        if (oDay) { SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum())); }
    }
}
