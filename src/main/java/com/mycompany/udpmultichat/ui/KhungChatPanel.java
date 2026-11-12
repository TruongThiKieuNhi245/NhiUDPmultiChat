package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import static com.mycompany.udpmultichat.ui.ThanhPhan.*;

/** Noi dung MOT tab phong; chi ve Swing, khong xu ly mang hay dong bo. */
@SuppressWarnings("serial")
final class KhungChatPanel extends JPanel {
    private static final DateTimeFormatter GIO=DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private final JPanel dsTin=new JPanel();
    private final JScrollPane cuon=new JScrollPane();
    private final JLabel thongTin=new JLabel();
    private Phong phong;

    KhungChatPanel(Phong phong) {
        setLayout(new BorderLayout());
        thongTin.setBorder(new EmptyBorder(10,12,10,12));
        thongTin.setFont(new Font("Segoe UI",Font.PLAIN,12));
        add(thongTin,BorderLayout.NORTH);
        dsTin.setLayout(new BoxLayout(dsTin,BoxLayout.Y_AXIS)); dsTin.setOpaque(false);
        JPanel nen=new JPanel(new BorderLayout()); nen.setBackground(new Color(0xF3F5FB));
        nen.setBorder(new EmptyBorder(8,0,8,0)); nen.add(dsTin,BorderLayout.NORTH);
        cuon.setViewportView(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16);
        add(cuon,BorderLayout.CENTER); capNhat(phong);
    }

    void capNhat(Phong p) {
        phong=p;
        thongTin.setText("Mã phòng: "+p.maPhong+"  •  "+(p.congKhai?"Công khai":"Riêng tư")
                +"  •  "+p.soThanhVien+"/"+p.gioiHan+" người"+(p.dangMo?"":"  •  Đang đóng"));
        // Dia chi ky thuat chi o tooltip, khong chen vao noi dung chat.
        thongTin.setToolTipText(p.nhom+":"+p.cong);
    }
    Phong phong() { return phong; }

    // Tin lich su va truc tiep deu da duoc ClientCore sap thu tu/loai trung.
    void themTin(TinNhan tin,boolean laToi) {
        JPanel hang=new JPanel(new FlowLayout(laToi?FlowLayout.RIGHT:FlowLayout.LEFT,12,4));
        hang.setOpaque(false);
        hang.add(new BongTin(tin.ten,tin.noiDung,GIO.format(Instant.ofEpochMilli(tin.thoiGian)),laToi));
        themHang(hang);
    }
    void heThong(String noiDung) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel nhan=new JLabel(noiDung); nhan.setForeground(new Color(0x747B91));
        nhan.setFont(new Font("Segoe UI",Font.PLAIN,12)); hang.add(nhan); themHang(hang);
    }
    private void themHang(JPanel hang) {
        dsTin.add(hang); dsTin.revalidate(); dsTin.repaint();
        SwingUtilities.invokeLater(() -> cuon.getVerticalScrollBar().setValue(cuon.getVerticalScrollBar().getMaximum()));
    }
}
