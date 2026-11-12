package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/** Thanh vien ben phai: nhom quyen, online truoc offline, the Ban va menu quan tri cho M5. */
@SuppressWarnings("serial")
final class ThanhVienPanel extends JPanel {
    private final JPanel ds=new JPanel();
    private final JLabel tong=Theme.nhan("Thành viên",true);
    ThanhVienPanel() {
        super(new BorderLayout()); setPreferredSize(new Dimension(238,0)); setBorder(Theme.le(Theme.S12));
        tong.setBorder(Theme.le(Theme.S8)); add(tong,BorderLayout.NORTH);
        ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS));
        JPanel nen=new JPanel(new BorderLayout()); nen.add(ds,BorderLayout.NORTH);
        JScrollPane cuon=new JScrollPane(nen); cuon.setBorder(null); add(cuon,BorderLayout.CENTER);
    }
    void capNhat(List<Phong.ThanhVien> danhSach,String toi,boolean quanLy) {
        ds.removeAll(); long online=danhSach.stream().filter(Phong.ThanhVien::online).count();
        tong.setText("Thành viên · "+danhSach.size()+" · "+online+" online");
        String[] vai={"ADMIN","CHU_PHONG","THANH_VIEN"},nhan={"Quản trị viên","Chủ phòng","Thành viên"};
        for (int i=0;i<vai.length;i++) {
            final String quyen=vai[i];
            var nhom=danhSach.stream().filter(n -> n.vaiTro().equals(quyen))
                .sorted(Comparator.comparing(Phong.ThanhVien::online).reversed().thenComparing(Phong.ThanhVien::ten)).toList();
            if (nhom.isEmpty()) { continue; }
            JLabel t=Theme.nhan(nhan[i],true); t.setFont(Theme.NHO); t.setForeground(Theme.PHU); t.setBorder(Theme.le(Theme.S8)); ds.add(t);
            for (var n:nhom) { ds.add(hang(n,toi,quanLy)); }
        }
        ds.revalidate(); ds.repaint();
    }
    private JPanel hang(Phong.ThanhVien n,String toi,boolean quanLy) {
        JPanel hang=new JPanel(new BorderLayout(Theme.S8,0)); hang.setBorder(Theme.le(Theme.S8)); hang.setMaximumSize(new Dimension(1000,56));
        hang.add(new ThanhPhan.Avatar(n.ten(),n.online()),BorderLayout.WEST);
        JPanel chu=new JPanel(new BorderLayout()); JLabel ten=Theme.nhan(n.ten(),true);
        ten.setForeground(n.online()?Theme.CHU:Theme.PHU); chu.add(ten,BorderLayout.CENTER);
        if (n.id().equals(toi)) { JLabel ban=Theme.nhan("Bạn",false); ban.setFont(Theme.NHO); ban.setForeground(Theme.CHINH); chu.add(ban,BorderLayout.EAST); }
        hang.add(chu,BorderLayout.CENTER); hang.setToolTipText(n.online()?"Đang online":"Thành viên phòng · đang offline");
        if (quanLy && !n.id().equals(toi)) {
            JPopupMenu menu=new JPopupMenu();
            for (String lenh:new String[]{"Kick (M5)","Cấm chat (M5)","Cấm vào (M5)"}) { JMenuItem m=new JMenuItem(lenh); m.setEnabled(false); menu.add(m); }
            ganMenu(hang,menu);
        }
        return hang;
    }
    private void ganMenu(javax.swing.JComponent c,JPopupMenu m) {
        c.setComponentPopupMenu(m);
        for (var con:c.getComponents()) { if (con instanceof javax.swing.JComponent j) { ganMenu(j,m); } }
    }

}
