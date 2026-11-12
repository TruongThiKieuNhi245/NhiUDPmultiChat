package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.VaiTro;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/** Cot trai va hop thoai CRUD phong; chi nhap du lieu, khong co socket hay phan quyen Server. */
@SuppressWarnings("serial")
final class DanhSachPhongPanel extends JPanel {
    /** UI chinh nhan hanh dong va goi ClientCore tren luong nen. */
    interface SuKien {
        void vao(String khoa,String matKhau);
        void tao(ThongTin thongTin);
        void sua(Phong phong,ThongTin thongTin);
        void xoa(Phong phong);
        void dongMo(Phong phong);
    }
    /** null matKhau nghia la giu mat khau cu khi sua. */
    record ThongTin(String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) { }
    private final DefaultListModel<Phong> model=new DefaultListModel<>();
    private final JList<Phong> ds=new JList<>(model);
    private final JTextField ma=new JTextField();
    private final JButton tao=new JButton("+ Tạo phòng"), vaoMa=new JButton("Vào");
    private final Supplier<NguoiDung> toi;
    private final Function<String,Integer> chuaDoc;
    private final SuKien suKien;
    private Set<String> daVao=Set.of();

    DanhSachPhongPanel(Supplier<NguoiDung> toi,Function<String,Integer> chuaDoc,SuKien suKien) {
        this.toi=toi; this.chuaDoc=chuaDoc; this.suKien=suKien;
        setLayout(new BorderLayout(0,10)); setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12,10,12,10));
        JPanel dau=new JPanel(new BorderLayout(0,10)); dau.setOpaque(false);
        dau.add(new JLabel("PHÒNG CHAT"),BorderLayout.NORTH); dau.add(tao,BorderLayout.CENTER);
        JPanel maPhong=new JPanel(new BorderLayout(4,4)); maPhong.setOpaque(false);
        maPhong.add(new JLabel("Nhập mã phòng"),BorderLayout.NORTH);
        maPhong.add(ma,BorderLayout.CENTER); maPhong.add(vaoMa,BorderLayout.EAST);
        dau.add(maPhong,BorderLayout.SOUTH); add(dau,BorderLayout.NORTH);
        ds.setCellRenderer(new DongPhong()); ds.setFixedCellHeight(62);
        JScrollPane cuon=new JScrollPane(ds); cuon.setBorder(null); add(cuon,BorderLayout.CENTER);
        tao.addActionListener(e -> { ThongTin t=nhap(null); if (t!=null) { suKien.tao(t); } });
        vaoMa.addActionListener(e -> vaoBangMa()); ma.addActionListener(e -> vaoBangMa());
        ds.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton()!=MouseEvent.BUTTON1 || toi.get()==null) { return; }
                Phong p=taiDiem(e);
                if (p!=null) { vaoPhong(p); }
            }
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) { menu(e); } }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) { menu(e); } }
        });
        choPhep(false);
    }

    void choPhep(boolean bat) { tao.setEnabled(bat); vaoMa.setEnabled(bat); ma.setEnabled(bat); ds.setEnabled(bat); }
    void capNhat(List<Phong> danhSach,Set<String> daVao) {
        String chon=ds.getSelectedValue()==null?"":ds.getSelectedValue().id;
        this.daVao=Set.copyOf(daVao); model.clear();
        for (Phong p : danhSach) { model.addElement(p); if (p.id.equals(chon)) { ds.setSelectedValue(p,true); } }
    }
    void chon(String id) { for (int i=0;i<model.size();i++) { if (model.get(i).id.equals(id)) { ds.setSelectedIndex(i); return; } } }

    // ===== VAO PHONG BANG CLICK HOAC MA =====
    private void vaoPhong(Phong p) {
        String matKhau="";
        if (p.coMatKhau && !daVao.contains(p.id) && !quanLy(p)) {
            matKhau=hoiMatKhau(); if (matKhau==null) { return; }
        }
        suKien.vao(p.id,matKhau);
    }
    private void vaoBangMa() {
        if (toi.get()==null || ma.getText().isBlank()) { return; }
        String matKhau=hoiMatKhau();
        if (matKhau!=null) { suKien.vao(ma.getText().strip(),matKhau); }
    }
    private String hoiMatKhau() {
        JPasswordField o=new JPasswordField(18);
        Object[] noiDung={"Mật khẩu phòng (để trống nếu không có):",o};
        if (JOptionPane.showConfirmDialog(this,noiDung,"Vào phòng",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) { return null; }
        char[] kyTu=o.getPassword(); String ketQua=new String(kyTu); Arrays.fill(kyTu,'\0'); return ketQua;
    }

    // ===== MENU CHU PHONG / ADMIN =====
    private boolean quanLy(Phong p) {
        NguoiDung n=toi.get(); return n!=null && (n.vaiTro==VaiTro.ADMIN || n.id.equals(p.chuId));
    }
    private Phong taiDiem(MouseEvent e) {
        int i=ds.locationToIndex(e.getPoint());
        return i>=0 && ds.getCellBounds(i,i).contains(e.getPoint())?model.get(i):null;
    }
    private void menu(MouseEvent e) {
        Phong p=taiDiem(e); if (p==null || !quanLy(p)) { return; }
        ds.setSelectedValue(p,true);
        JPopupMenu menu=new JPopupMenu();
        JMenuItem sua=new JMenuItem("Sửa phòng"), xoa=new JMenuItem("Xóa phòng");
        JMenuItem trangThai=new JMenuItem(p.dangMo?"Đóng phòng":"Mở lại phòng");
        sua.addActionListener(v -> { ThongTin t=nhap(p); if (t!=null) { suKien.sua(p,t); } });
        xoa.addActionListener(v -> {
            if (JOptionPane.showConfirmDialog(this,"Xóa phòng «"+p.ten+"»?","Xóa phòng",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) { suKien.xoa(p); }
        });
        trangThai.addActionListener(v -> suKien.dongMo(p));
        boolean sanh=Protocol.SANH_ID.equals(p.id); xoa.setEnabled(!sanh); trangThai.setEnabled(!sanh);
        menu.add(sua); menu.add(trangThai); menu.add(xoa); menu.show(ds,e.getX(),e.getY());
    }

    // ===== HOP THOAI TAO / SUA =====
    private ThongTin nhap(Phong p) {
        JTextField ten=new JTextField(p==null?"":p.ten,24), moTa=new JTextField(p==null?"":p.moTa,24);
        JComboBox<String> loai=new JComboBox<>(new String[]{"Công khai","Riêng tư"});
        loai.setSelectedIndex(p==null || p.congKhai?0:1);
        JSpinner gioiHan=new JSpinner(new SpinnerNumberModel(p==null?20:p.gioiHan,1,1000,1));
        JPasswordField matKhau=new JPasswordField(24);
        JCheckBox doi=new JCheckBox("Đổi/xóa mật khẩu (trống = xóa)",p==null);
        matKhau.setEnabled(p==null); doi.addActionListener(e -> matKhau.setEnabled(doi.isSelected()));
        JPanel form=new JPanel(new GridLayout(0,2,8,10));
        form.add(new JLabel("Tên phòng")); form.add(ten); form.add(new JLabel("Mô tả")); form.add(moTa);
        form.add(new JLabel("Loại phòng")); form.add(loai); form.add(new JLabel("Giới hạn người")); form.add(gioiHan);
        form.add(doi); form.add(matKhau);
        if (p!=null && Protocol.SANH_ID.equals(p.id)) {
            ten.setEnabled(false); loai.setEnabled(false); doi.setEnabled(false); matKhau.setEnabled(false);
        }
        int luaChon=JOptionPane.showConfirmDialog(this,form,p==null?"Tạo phòng":"Sửa phòng",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if (luaChon!=JOptionPane.OK_OPTION) { return null; }
        char[] kyTu=matKhau.getPassword(); String mk=(p==null || doi.isSelected())?new String(kyTu):null;
        Arrays.fill(kyTu,'\0');
        return new ThongTin(ten.getText().strip(),moTa.getText().strip(),loai.getSelectedIndex()==0,mk,(Integer)gioiHan.getValue());
    }

    /** Moi dong gom ten, loai, ma phong, so nguoi va badge chua doc. */
    private final class DongPhong extends JPanel implements ListCellRenderer<Phong> {
        private final JLabel ten=new JLabel(), chiTiet=new JLabel();
        DongPhong() { setLayout(new GridLayout(2,1,0,3)); setBorder(new EmptyBorder(8,8,8,8)); add(ten); add(chiTiet); }
        @Override public Component getListCellRendererComponent(JList<? extends Phong> list,Phong p,int index,boolean chon,boolean focus) {
            int so=chuaDoc.apply(p.id);
            ten.setText((quanLy(p)?"★ ":"# ")+p.ten+(so>0?"  ["+so+"]":""));
            chiTiet.setText((p.congKhai?"Công khai":"Riêng tư")+" · "+p.maPhong+" · "+p.soThanhVien+"/"+p.gioiHan+(p.dangMo?"":" · Đóng"));
            chiTiet.setForeground(new Color(0x747B91));
            setBackground(chon?new Color(0xE2E9FF):Color.WHITE);
            setToolTipText(p.moTa); return this;
        }
    }
}
