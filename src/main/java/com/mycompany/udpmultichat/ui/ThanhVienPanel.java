package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/** Vai tro: Thanh vien ben phai: nhom quyen, online truoc offline, the Ban va menu quan tri cho M5. Ai goi: ChatMulticastUI. Goi: Theme, ThanhPhan, Phong.ThanhVien. */
@SuppressWarnings("serial")
final class ThanhVienPanel extends JPanel {
    // ===== VAI TRO VA CAC THAO TAC =====
    private java.util.function.Function<String,java.awt.image.BufferedImage> anh=id -> null;
    // Gan bo doc cache; khong tai anh trong renderer.
    void datAnh(java.util.function.Function<String,java.awt.image.BufferedImage> anh) { this.anh=anh; }
    private java.util.function.BiConsumer<String,Phong.ThanhVien> xuLy=(l,n) -> { };
    // Gan callback de cot thanh vien khong can biet socket hay Core.
    void datKiemDuyet(java.util.function.BiConsumer<String,Phong.ThanhVien> callback) { xuLy=callback; }
    private Runnable onMoiThanhVien = () -> { };
    void datHanhDongMoi(Runnable r) { this.onMoiThanhVien = r; }
    private java.util.function.BiConsumer<String,Phong.ThanhVien> onDatBietDanh = (nick,tv) -> { };
    void datHanhDongBietDanh(java.util.function.BiConsumer<String,Phong.ThanhVien> r) { this.onDatBietDanh = r; }
    private final JPanel ds=new JPanel();
    private final JLabel tong=Theme.nhan("Thành viên",true);
    private final javax.swing.JButton nutMoi=Theme.nutNhe("","plus");
    // Khoi tao lop va noi cac thanh phan can thiet.
    ThanhVienPanel() {
        super(new BorderLayout()); setPreferredSize(new Dimension(238,0)); setBorder(Theme.le(Theme.S12));
        JPanel header=new JPanel(new BorderLayout()); header.setOpaque(false);
        tong.setBorder(Theme.le(Theme.S8)); header.add(tong,BorderLayout.CENTER);
        nutMoi.setToolTipText("Mời thành viên vào phòng");
        nutMoi.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        nutMoi.addActionListener(e -> onMoiThanhVien.run());
        header.add(nutMoi,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);

        ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS));
        JPanel nen=new JPanel(new BorderLayout()); nen.add(ds,BorderLayout.NORTH);
        JScrollPane cuon=new JScrollPane(nen); cuon.setBorder(null); add(cuon,BorderLayout.CENTER);
    }
    // Cap nhat.
    void capNhat(List<Phong.ThanhVien> danhSach,String toi,boolean quanLy) {
        ds.removeAll(); long online=danhSach.stream().filter(Phong.ThanhVien::online).count();
        tong.setText("Thành viên · "+danhSach.size()+" · "+online+" online");
        nutMoi.setVisible(quanLy);
        String[] vai={"ADMIN","CHU_PHONG","THANH_VIEN"},nhan={"Quản trị viên","Chủ phòng","Thành viên"};
        for (int i=0;i<vai.length;i++) {
            final String quyen=vai[i];
            var nhom=danhSach.stream().filter(n -> n.vaiTro().equals(quyen))
                .sorted(Comparator.comparing(Phong.ThanhVien::online).reversed().thenComparing(Phong.ThanhVien::ten)).toList();
            if (nhom.isEmpty()) { continue; }
            JLabel t=Theme.nhan(nhan[i].toUpperCase(java.util.Locale.ROOT),true); t.setFont(Theme.NHO); t.setForeground(Theme.PHU);
            t.setBorder(Theme.le(Theme.S8)); t.setAlignmentX(LEFT_ALIGNMENT); t.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); ds.add(t);
            boolean admin=danhSach.stream().anyMatch(n -> n.laToi(toi) && n.vaiTro().equals("ADMIN"));
            for (var n:nhom) { ds.add(hang(n,toi,quanLy && (admin || !n.vaiTro().equals("ADMIN")),admin)); }
        }
        ds.revalidate(); ds.repaint();
    }
    // Hang thanh vien co biet danh va menu thao tac.
    private JPanel hang(Phong.ThanhVien n,String toi,boolean quanLy,boolean toiLaAdmin) {
        JPanel hang=new JPanel(new BorderLayout(Theme.S8,0)); hang.setBorder(Theme.le(Theme.S8)); hang.setMaximumSize(new Dimension(1000,56));
        hang.setAlignmentX(LEFT_ALIGNMENT); hang.setName(n.tenDangNhap());
        hang.add(new ThanhPhan.Avatar(n.tenHienThi(),n.online(),() -> anh.apply(n.id())),BorderLayout.WEST);

        JPanel chu=new JPanel(); chu.setLayout(new BoxLayout(chu,BoxLayout.Y_AXIS)); chu.setOpaque(false);
        JLabel ten=Theme.nhan(n.tenHienThi(),true);
        ten.setForeground(n.online()?Theme.CHU:Theme.PHU);
        chu.add(ten);

        if (n.coBietDanh()) {
            JLabel phu=Theme.nhan("(" + n.ten() + ")",false); phu.setFont(Theme.NHO); phu.setForeground(Theme.PHU);
            chu.add(phu);
            hang.setToolTipText("Biệt danh: "+n.bietDanh()+" · Tên thật: "+n.ten()+" (@"+n.tenDangNhap()+")");
        } else {
            hang.setToolTipText("@"+n.tenDangNhap()+" ("+(n.online()?"Đang online":"Đang offline")+")");
        }

        JPanel phai=new JPanel(new BorderLayout()); phai.setOpaque(false);
        if (n.laToi(toi)) { JLabel ban=Theme.nhan("Bạn",false); ban.setFont(Theme.NHO); ban.setForeground(Theme.CHINH); phai.add(ban,BorderLayout.WEST); }

        JPopupMenu menu=taoMenuThanhVien(n,n.laToi(toi),quanLy,toiLaAdmin);
        if (menu.getComponentCount()>0) {
            var nut=Theme.nutNhe("⋮",""); nut.addActionListener(e -> menu.show(nut,0,nut.getHeight())); phai.add(nut,BorderLayout.EAST);
            ganMenu(hang,menu);
        }

        hang.add(chu,BorderLayout.CENTER);
        hang.add(phai,BorderLayout.EAST);
        return hang;
    }
    // Tao menu thao tac thanh vien gom dat biet danh va kiem duyet.
    private JPopupMenu taoMenuThanhVien(Phong.ThanhVien n,boolean laToi,boolean quanLy,boolean toiLaAdmin) {
        JPopupMenu menu=new JPopupMenu();
        boolean duocDoiBietDanh = laToi || (quanLy && (toiLaAdmin || !n.vaiTro().equals("ADMIN")));
        if (duocDoiBietDanh) {
            JMenuItem mnNick = new JMenuItem(n.coBietDanh() ? "Đổi biệt danh..." : "Đặt biệt danh...");
            mnNick.addActionListener(e -> {
                Object res = JOptionPane.showInputDialog(
                    this,
                    "Nhập biệt danh cho @" + n.tenDangNhap() + " trong phòng này\n(để trống để xóa biệt danh):",
                    "Đặt biệt danh trong phòng",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    n.bietDanh() == null ? "" : n.bietDanh()
                );
                if (res != null) {
                    String nhap = res.toString().strip();
                    onDatBietDanh.accept(nhap, n);
                }
            });
            menu.add(mnNick);
        }
        if (quanLy && !laToi) {
            if (menu.getComponentCount() > 0) { menu.addSeparator(); }
            JPopupMenu menuKd = KiemDuyetUI.menu(l -> xuLy.accept(l, n));
            for (var c : menuKd.getComponents()) {
                if (c instanceof JMenuItem mi) {
                    JMenuItem moi = new JMenuItem(mi.getText());
                    for (var al : mi.getActionListeners()) { moi.addActionListener(al); }
                    menu.add(moi);
                } else if (c instanceof JPopupMenu.Separator) {
                    menu.addSeparator();
                }
            }
        }
        return menu;
    }
    // Gan menu.
    private void ganMenu(javax.swing.JComponent c,JPopupMenu m) {
        c.setComponentPopupMenu(m);
        for (var con:c.getComponents()) { if (con instanceof javax.swing.JComponent j) { ganMenu(j,m); } }
    }

}
