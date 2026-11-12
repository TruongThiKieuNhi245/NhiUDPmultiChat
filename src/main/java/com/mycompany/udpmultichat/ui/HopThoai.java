package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

/** Vai tro: Form va vo dialog chung: loi ngay duoi truong, Enter gui, Esc huy; dung lai boi UiPreview. Ai goi: ChatMulticastUI, UiPreview. Goi: Theme, callback UI. */
@SuppressWarnings("serial")
final class HopThoai extends JPanel {
    // ===== VAI TRO VA CAC THAO TAC =====
    record DuLieu(String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) { }
    private final Map<String,JLabel> loi=new LinkedHashMap<>();
    private final JPanel truong=new JPanel();
    private final JButton chinh,huy=Theme.nut("Hủy",false);
    private final JTextField ten=new JTextField(),moTa=new JTextField(),ma=new JTextField();
    private final JPasswordField matKhau=new JPasswordField();
    private final JComboBox<String> loai=new JComboBox<>(new String[]{"Công khai","Riêng tư"});
    private final JComboBox<String> cheDo=new JComboBox<>(new String[]{"Giữ nguyên","Đổi mật khẩu","Xóa mật khẩu"});
    private final JSpinner gioiHan=new JSpinner(new SpinnerNumberModel(20,2,500,1));
    private final boolean vao,sua;
    private JDialog dialog;
    private boolean chiDoc;
    private Consumer<HopThoai> gui=f -> { };

    // Khoi tao lop va noi cac thanh phan can thiet.
    HopThoai(Phong p,boolean vao) {
        super(new BorderLayout(0,Theme.S16)); this.vao=vao; sua=p!=null;
        setBorder(Theme.le(Theme.S24)); setPreferredSize(new Dimension(470,vao?280:(sua?570:490)));
        var tieuDe=Theme.nhan(vao?"Vào phòng bằng mã":(sua?"Sửa phòng":"Tạo phòng"),true);
        tieuDe.setFont(Theme.DAM.deriveFont(20f)); add(tieuDe,BorderLayout.NORTH);
        truong.setLayout(new javax.swing.BoxLayout(truong,javax.swing.BoxLayout.Y_AXIS));
        if (vao) {
            ma.putClientProperty("JTextField.placeholderText","Mã 6 ký tự"); them("ma","Mã phòng",ma);
            matKhau.setVisible(false);
            them("mk","Mật khẩu phòng (nếu có)",oMatKhau(matKhau));
            truong.getComponent(1).setVisible(false);
        } else {
            them("ten","Tên phòng *",ten); them("mota","Mô tả",moTa); them("loai","Loại phòng",loai);
            them("gioihan","Giới hạn người (2–500)",gioiHan);
            if (sua) { them("chedo","Mật khẩu phòng",cheDo); }
            them("mk",sua?"Mật khẩu mới":"Mật khẩu phòng (nếu có)",oMatKhau(matKhau));
            if (p!=null) {
                ten.setText(p.ten); moTa.setText(p.moTa); loai.setSelectedIndex(p.congKhai?0:1);
                gioiHan.setValue(Math.min(500,Math.max(2,p.gioiHan))); matKhau.setEnabled(false);
                cheDo.addActionListener(e -> matKhau.setEnabled(cheDo.getSelectedIndex()==1));
            }
        }
        add(truong,BorderLayout.CENTER);
        JPanel nut=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,Theme.S8,0));
        chinh=Theme.nut(vao?"Vào phòng":(sua?"Lưu":"Tạo phòng"),true); nut.add(huy); nut.add(chinh); add(nut,BorderLayout.SOUTH);
        chinh.addActionListener(e -> { if (kiemTra()) { gui.accept(this); } }); huy.addActionListener(e -> dong());
        boCucForm();
    }
    // Thong tin phong dung cung vo dialog; khong dung JOptionPane.
    static HopThoai thongTin(Phong p,java.util.List<Phong.ThanhVien> ds) { return new HopThoai(p,ds); }
    // Chi doc day du metadata, nut Dong o ben phai.
    private HopThoai(Phong p,java.util.List<Phong.ThanhVien> ds) {
        super(new BorderLayout(0,Theme.S16)); vao=false; sua=false; chiDoc=true; chinh=Theme.nut("Đóng",true);
        setBorder(Theme.le(Theme.S24)); setPreferredSize(new Dimension(490,540));
        JLabel t=Theme.nhan("Thông tin phòng",true); t.setFont(Theme.DAM.deriveFont(20f)); add(t,BorderLayout.NORTH);
        truong.setLayout(new javax.swing.BoxLayout(truong,javax.swing.BoxLayout.Y_AXIS));
        thongTinDong("Tên phòng",p.ten); thongTinDong("Mô tả",p.moTa.isBlank()?"Chưa có mô tả":p.moTa);
        thongTinMa(p.maPhong);
        thongTinDong("Loại",p.congKhai?"Công khai":"Riêng tư"); thongTinDong("Giới hạn",p.gioiHan+" người");
        thongTinDong("Chủ phòng",ds.stream().filter(n -> n.id().equals(p.chuId)).map(n -> n.ten()+" (@"+n.tenDangNhap()+")").findFirst().orElse(p.tenChu));
        thongTinDong("Ngày tạo",dinhDangNgay(p.ngayTao));
        thongTinDong("Thành viên",Math.max(p.tongThanhVien,ds.size())+" người · "+p.soThanhVien+" đang online");
        add(truong); JPanel nut=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT)); nut.add(chinh); add(nut,BorderLayout.SOUTH);
        chinh.addActionListener(e -> dong());
    }
    // Nut copy bam sat ma, khong bi day sang mep phai cua dialog.
    private void thongTinMa(String ma) {
        JPanel hang=thongTinDong("Mã phòng","");
        var cu=((BorderLayout)hang.getLayout()).getLayoutComponent(BorderLayout.CENTER); hang.remove(cu);
        JPanel maVaNut=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,Theme.S8,0));
        maVaNut.add(Theme.nhan(ma,false)); JButton copy=Theme.nutNhe("Sao chép","");
        copy.addActionListener(e -> DanhSachPhongPanel.saoChep(ma)); maVaNut.add(copy); hang.add(maVaNut);
    }
    // Dong thong tin doc duoc xuong dong, khong cat mo ta dai.
    private JPanel thongTinDong(String nhan,String noiDung) {
        JPanel hang=new JPanel(new BorderLayout(Theme.S12,0)); hang.setBorder(javax.swing.BorderFactory.createEmptyBorder(Theme.S8,0,Theme.S8,0));
        JLabel ten=Theme.nhan(nhan,true); ten.setPreferredSize(new Dimension(110,24)); hang.add(ten,BorderLayout.WEST);
        javax.swing.JTextArea text=new javax.swing.JTextArea(noiDung); text.setFont(Theme.FONT); text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true); text.setOpaque(false);
        text.setSize(270,200); hang.add(text); hang.setMaximumSize(new Dimension(Integer.MAX_VALUE,Math.max(44,text.getPreferredSize().height+16))); truong.add(hang); return hang;
    }
    // Ngay luu la ISO instant hoac ngay ISO khi xem truoc.
    private static String dinhDangNgay(String ngay) {
        var mau=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try { return java.time.Instant.parse(ngay).atZone(java.time.ZoneId.systemDefault()).format(mau); }
        catch (java.time.format.DateTimeParseException e) {
            try { return java.time.LocalDate.parse(ngay).format(mau); }
            catch (java.time.format.DateTimeParseException sai) { return ngay; }
        }
    }
    // Them.
    private void them(String khoa,String nhan,JComponent o) {
        JLabel l=Theme.nhan("",false); l.setForeground(Theme.LOI); l.setFont(Theme.NHO); l.setVisible(false); loi.put(khoa,l);
        truong.add(Theme.hangNhap(nhan,o,l));
    }
    // O mat khau.
    static JPanel oMatKhau(JPasswordField o) {
        JPanel hang=new JPanel(new BorderLayout(Theme.S4,0)); JButton hien=Theme.nut("Hiện",false);
        char echo=o.getEchoChar(); hien.setFocusable(false);
        hien.addActionListener(e -> { boolean dangAn=o.getEchoChar()!=0; o.setEchoChar(dangAn?0:echo); hien.setText(dangAn?"Ẩn":"Hiện"); });
        hang.add(o,BorderLayout.CENTER); hang.add(hien,BorderLayout.EAST); return hang;
    }
    // Hien.
    void hien(Window cha,Consumer<HopThoai> gui) {
        this.gui=gui; dialog=new JDialog(cha,chiDoc?"Thông tin phòng":vao?"Vào phòng":(sua?"Sửa phòng":"Tạo phòng"),Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(this); dialog.getRootPane().setDefaultButton(chinh);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"huy");
        dialog.getRootPane().getActionMap().put("huy",new AbstractAction() { public void actionPerformed(ActionEvent e) { dong(); } });
        dialog.pack(); dialog.setResizable(false); dialog.setLocationRelativeTo(cha); dialog.setVisible(true);
    }
    // Dong.
    void dong() { if (dialog!=null) { dialog.dispose(); } }
    // Ban.
    void ban(boolean ban) { chinh.setEnabled(!ban); huy.setEnabled(!ban); }
    // Loi server.
    void loiServer(String thongBao) {
        ban(false);
        if (vao && thongBao.contains("Mật khẩu")) {
            truong.getComponent(1).setVisible(true); matKhau.setVisible(true); matKhau.requestFocusInWindow(); revalidate();
        }
        String truong=vao?(thongBao.contains("Mật khẩu")?"mk":"ma"):
            (thongBao.contains("Mật khẩu")?"mk":(thongBao.contains("Giới hạn")?"gioihan":(thongBao.contains("mô tả")?"mota":"ten")));
        loi(truong,thongBao);
    }
    // Loi.
    private void loi(String khoa,String thongBao) {
        JLabel l=loi.get(khoa); Theme.datLoi(l,thongBao);
        truong.invalidate(); revalidate();
        boCucForm(); if (dialog!=null) { dialog.pack(); }
    }
    // Chieu cao cua so theo noi dung that, khong ep hang khi them dong loi.
    private void boCucForm() {
        truong.invalidate(); setPreferredSize(null);
        setPreferredSize(new Dimension(470,getLayout().preferredLayoutSize(this).height)); revalidate();
    }
    // Kiem tra.
    private boolean kiemTra() {
        for (String khoa:loi.keySet()) { loi(khoa,""); }
        if (vao) {
            if (!ma.getText().strip().matches("[A-Za-z0-9]{6}")) { loi("ma","Mã phòng cần đúng 6 ký tự chữ/số."); return false; }
            return true;
        }
        String t=ten.getText().strip(); int n=t.codePointCount(0,t.length());
        if (n<3 || n>30) { loi("ten","Tên phòng cần 3–30 ký tự."); return false; }
        if (moTa.getText().codePointCount(0,moTa.getText().length())>80) { loi("mota","Mô tả tối đa 80 ký tự."); return false; }
        if (matKhau.getPassword().length>128) { loi("mk","Mật khẩu tối đa 128 ký tự."); return false; }
        return true;
    }
    // Doc form va chuyen che do mat khau thanh KEEP/SET/xoa.
    DuLieu duLieu() {
        String mk=new String(matKhau.getPassword());
        if (sua) { mk=cheDo.getSelectedIndex()==0?null:(cheDo.getSelectedIndex()==2?"":mk); }
        return new DuLieu(ten.getText().strip(),moTa.getText().strip(),loai.getSelectedIndex()==0,mk,(Integer)gioiHan.getValue());
    }
    // Dat ma.
    void datMa(String giaTri) { ma.setText(giaTri); }
    // Ma.
    String ma() { return ma.getText().strip(); }
    // Mat khau.
    String matKhau() { return new String(matKhau.getPassword()); }
    // Co mat khau.
    void coMatKhau() { truong.getComponent(1).setVisible(true); matKhau.setVisible(true); boCucForm(); }
}
