package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.common.PhanManh;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.*;

/** Vai tro: Form ho so va mat khau, chi goi API bat dong bo. Ai goi: ChatMulticastUI, UiPreview. Goi: HoSoClient, PhanManh, Theme. */
@SuppressWarnings("serial")
public final class HoSoDialog extends JPanel {
    private final JPanel form=new JPanel();
    private final ClientCore core;
    private final NguoiDung nguoi;
    private final boolean matKhau,batBuoc;
    private final JTextField ten=new JTextField(),gioiThieu=new JTextField();
    private final JPasswordField[] mk={new JPasswordField(),new JPasswordField(),new JPasswordField()};
    private final JLabel[] loi={loi(),loi(),loi()};
    private final JLabel trangThai=Theme.nhan("",false),loiChung=loi();
    private final JLabel anh=new JLabel();
    private final JButton luu,huy=Theme.nut("Hủy",false),doiAnh=Theme.nut("Đổi ảnh",false),xoaAnh=Theme.nut("Xóa ảnh",false);
    private final AtomicBoolean daHuy=new AtomicBoolean();
    private BufferedImage xemTruoc;
    private byte[] anhMoi;
    private boolean xoa,ban;
    private JDialog dialog;
    private Consumer<NguoiDung> xong=n -> { };
    // Dung dung component that cho UI va preview; core null chi khi chup anh.
    public HoSoDialog(ClientCore core,NguoiDung n,boolean matKhau,boolean batBuoc) {
        super(new BorderLayout(0,Theme.S16)); this.core=core; nguoi=n; this.matKhau=matKhau; this.batBuoc=batBuoc;
        setBorder(Theme.le(Theme.S24));
        luu=Theme.nut(matKhau?"Đổi mật khẩu":"Lưu",true);
        JLabel dau=Theme.nhan(matKhau?"Đổi mật khẩu":"Hồ sơ của tôi",true); dau.setFont(Theme.DAM.deriveFont(20f)); add(dau,BorderLayout.NORTH);
        form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        if (matKhau) { formMatKhau(form); } else { formHoSo(form); }
        add(form); JPanel duoi=new JPanel(new BorderLayout(0,Theme.S8));
        trangThai.setFont(Theme.NHO); trangThai.setForeground(Theme.PHU); duoi.add(trangThai,BorderLayout.NORTH); duoi.add(loiChung);
        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S8,0)); if (!batBuoc) { nut.add(huy); } nut.add(luu); duoi.add(nut,BorderLayout.SOUTH); add(duoi,BorderLayout.SOUTH);
        huy.addActionListener(e -> dong()); luu.addActionListener(e -> gui());
    }
    // Form tu cao theo dong loi, chi co dinh be rong.
    @Override public Dimension getPreferredSize() { Dimension d=super.getPreferredSize(); return new Dimension(540,d.height); }
    // ===== FORM HO SO =====
    private void formHoSo(JPanel form) {
        xemTruoc=core==null?null:core.hoSo().anh(nguoi.id); capNhatAnh();
        anh.setPreferredSize(new Dimension(96,96)); JPanel hang=new JPanel(new FlowLayout(FlowLayout.LEFT,Theme.S12,0));
        hang.setAlignmentX(LEFT_ALIGNMENT); hang.setMaximumSize(new Dimension(Integer.MAX_VALUE,108));
        hang.add(anh); hang.add(doiAnh); hang.add(xoaAnh); form.add(hang);
        doiAnh.addActionListener(e -> chonAnh()); xoaAnh.addActionListener(e -> { anhMoi=null; xoa=true; xemTruoc=null; capNhatAnh(); });
        ten.setText(nguoi.tenHienThi); gioiThieu.setText(nguoi.gioiThieu);
        form.add(Theme.hangNhap("Tên hiển thị",ten,loi[0])); form.add(Theme.hangNhap("Giới thiệu",gioiThieu,loi[1]));
        String vai=nguoi.vaiTro==com.mycompany.udpmultichat.common.model.VaiTro.ADMIN?"Quản trị viên":"Thành viên";
        form.add(doc("Tên đăng nhập",nguoi.tenDangNhap)); form.add(doc("Vai trò",vai)); form.add(doc("Ngày tạo",ngay(nguoi.ngayTao)));
    }
    // Truong chi doc khong cho doi dinh danh hay quyen tu Client.
    private JPanel doc(String nhan,String noiDung) {
        JPanel p=new JPanel(new BorderLayout(Theme.S12,0)); p.setBorder(Theme.le(Theme.S8));
        JLabel l=Theme.nhan(nhan,false); l.setForeground(Theme.PHU); p.add(l,BorderLayout.WEST); p.add(Theme.nhan(noiDung,false));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,40)); p.setAlignmentX(LEFT_ALIGNMENT); return p;
    }
    // Dinh dang ngay cong khai, dung cho ca du lieu ISO instant va ngay cu.
    private static String ngay(String s) {
        try { return DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(s)); }
        catch (java.time.format.DateTimeParseException e) { return s; }
    }
    // ===== FORM DOI MAT KHAU =====
    private void formMatKhau(JPanel form) {
        if (batBuoc) {
            JLabel lyDo=Theme.nhan("Bạn đang dùng mật khẩu mặc định. Hãy đổi để tiếp tục.",false);
            lyDo.setForeground(Theme.CANH_BAO); lyDo.setAlignmentX(LEFT_ALIGNMENT); form.add(lyDo);
        }
        String[] nhan={"Mật khẩu cũ","Mật khẩu mới","Nhập lại mật khẩu mới"};
        for (int i=0;i<mk.length;i++) { form.add(Theme.hangNhap(nhan[i],oMatKhau(mk[i]),loi[i])); }
        JLabel goiY=Theme.nhan("Mật khẩu mới cần 6–128 ký tự.",false); goiY.setFont(Theme.NHO); goiY.setForeground(Theme.PHU); form.add(goiY);
    }
    // Moi o co nut Hien rieng; giu echo char do LAF quy dinh.
    private JPanel oMatKhau(JPasswordField o) {
        JPanel p=new JPanel(new BorderLayout(Theme.S4,0)); JButton hien=Theme.nutNhe("Hiện",""); char kyTu=o.getEchoChar();
        hien.addActionListener(e -> { boolean an=o.getEchoChar()!=0; o.setEchoChar(an?'\0':kyTu); hien.setText(an?"Ẩn":"Hiện"); });
        p.add(o); p.add(hien,BorderLayout.EAST); return p;
    }
    // Loi chi chiem cho khi co noi dung, giu chieu cao input theo Theme.
    private static JLabel loi() { JLabel l=Theme.nhan("",false); l.setFont(Theme.NHO); l.setForeground(Theme.LOI); l.setVisible(false); return l; }
    // Kiem tra Client de nguoi dung sua ngay; Server van kiem tra lai.
    boolean kiemTra() {
        for (JLabel l:loi) { Theme.datLoi(l,""); } Theme.datLoi(loiChung,"");
        if (!matKhau) {
            int n=ten.getText().strip().codePointCount(0,ten.getText().strip().length());
            if (n<2 || n>30) { Theme.datLoi(loi[0],"Tên hiển thị cần 2–30 ký tự."); }
            if (gioiThieu.getText().codePointCount(0,gioiThieu.getText().length())>100) { Theme.datLoi(loi[1],"Giới thiệu tối đa 100 ký tự."); }
        } else {
            for (int i=0;i<mk.length;i++) {
                char[] s=mk[i].getPassword(); if (s.length<6 || s.length>128) { Theme.datLoi(loi[i],"Mật khẩu cần 6–128 ký tự."); } java.util.Arrays.fill(s,'\0');
            }
            char[] moi=mk[1].getPassword(),lai=mk[2].getPassword();
            if (!java.util.Arrays.equals(moi,lai)) { Theme.datLoi(loi[2],"Mật khẩu nhập lại chưa khớp."); }
            java.util.Arrays.fill(moi,'\0'); java.util.Arrays.fill(lai,'\0');
        }
        form.invalidate(); invalidate(); revalidate(); if (dialog!=null) { dialog.pack(); }
        for (JLabel l:loi) { if (l.isVisible()) { return false; } } return true;
    }
    // ===== CHON VA XEM TRUOC ANH, KHONG GUI MANG =====
    private void chonAnh() {
        JFileChooser chon=new JFileChooser(); chon.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ảnh PNG/JPG","png","jpg","jpeg")); chon.setAcceptAllFileFilterUsed(false);
        if (chon.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) { return; }
        docAnh(chon.getSelectedFile().toPath());
    }
    // ImageIO chay nen, EDT chi nhan ket qua de ve avatar 96px.
    private void docAnh(Path tep) {
        ban(true); trangThai.setText("Đang đọc ảnh...");
        CompletableFuture.supplyAsync(() -> {
            try { return PhanManh.chuanHoa(tep); } catch (java.io.IOException e) { throw new java.util.concurrent.CompletionException(e); }
        }).whenComplete((data,e) -> SwingUtilities.invokeLater(() -> {
            if (daHuy.get()) { return; } ban(false); trangThai.setText("");
            if (e!=null) { baoLoi(ClientCore.loiDeHieu(e)); return; }
            try { anhMoi=data; xemTruoc=PhanManh.docAnh(data,true); xoa=false; capNhatAnh(); }
            catch (java.io.IOException loiAnh) { baoLoi(loiAnh.getMessage()); }
        }));
    }
    // Icon tron dung cung renderer voi avatar trong chat.
    private void capNhatAnh() { anh.setIcon(Theme.avatar(nguoi.tenHienThi,xemTruoc,96)); }
    // ===== GUI BAT DONG BO =====
    private void gui() {
        if (ban || !kiemTra() || core==null) { return; }
        ban(true); trangThai.setText(matKhau?"Đang đổi mật khẩu...":anhMoi!=null?"Đang tải ảnh lên...":"Đang lưu hồ sơ...");
        CompletableFuture<NguoiDung> viec;
        if (matKhau) { viec=doiMatKhau(); }
        else { viec=core.hoSo().luu(ten.getText(),gioiThieu.getText(),anhMoi,xoa,daHuy,n -> SwingUtilities.invokeLater(() -> trangThai.setText("Đang tải ảnh lên... "+n+"%"))); }
        viec.whenComplete((n,e) -> SwingUtilities.invokeLater(() -> {
            if (daHuy.get()) { return; } ban(false); trangThai.setText("");
            if (e!=null) { baoLoi(ClientCore.loiDeHieu(e)); }
            else { xong.accept(n); dong(); }
        }));
    }
    // Xoa mang char sau khi API da sao chep; khong ghi mat khau vao Preferences.
    private CompletableFuture<NguoiDung> doiMatKhau() {
        char[] cu=mk[0].getPassword(),moi=mk[1].getPassword();
        try { return core.doiMatKhau(new String(cu),new String(moi)); }
        finally { java.util.Arrays.fill(cu,'\0'); java.util.Arrays.fill(moi,'\0'); }
    }
    // Gan loi Server vao dung o, loi mang vao dong chung.
    private void baoLoi(String text) {
        if (matKhau && text.contains("cũ")) { Theme.datLoi(loi[0],text); }
        else if (matKhau && text.contains("Mật khẩu")) { Theme.datLoi(loi[1],text); }
        else if (text.contains("Tên hiển thị")) { Theme.datLoi(loi[0],text); }
        else if (text.contains("Giới thiệu")) { Theme.datLoi(loi[1],text); }
        else { Theme.datLoi(loiChung,text); }
        form.invalidate(); invalidate(); revalidate(); if (dialog!=null) { dialog.pack(); }
    }
    // Chi khoa form dang luu, nut Huy van hoat dong; khong khoa cua so chat.
    private void ban(boolean b) {
        ban=b; luu.setEnabled(!b); doiAnh.setEnabled(!b); xoaAnh.setEnabled(!b); ten.setEnabled(!b); gioiThieu.setEnabled(!b);
        for (var o:mk) { o.setEnabled(!b); }
    }
    // Bat buoc khong co X/Huy/Esc; default button la hanh dong duy nhat.
    public void hien(JFrame cha,Consumer<NguoiDung> xong) {
        this.xong=xong; dialog=new JDialog(cha,matKhau?"Đổi mật khẩu":"Hồ sơ của tôi",batBuoc);
        dialog.setUndecorated(batBuoc);
        dialog.setDefaultCloseOperation(batBuoc?JDialog.DO_NOTHING_ON_CLOSE:JDialog.DISPOSE_ON_CLOSE);
        if (batBuoc) { dialog.getRootPane().setBorder(BorderFactory.createLineBorder(Theme.VIEN)); }
        dialog.setContentPane(this); dialog.getRootPane().setDefaultButton(luu);
        dialog.getRootPane().registerKeyboardAction(e -> { if (!batBuoc) { dong(); } },KeyStroke.getKeyStroke("ESCAPE"),WHEN_IN_FOCUSED_WINDOW);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosed(java.awt.event.WindowEvent e) { daHuy.set(true); for (var o:mk) { o.setText(""); } } });
        dialog.pack(); dialog.setResizable(false); dialog.setLocationRelativeTo(cha); dialog.setVisible(true);
    }
    // Huy chi tac dong anh tam, ho so cu giu nguyen neu chua gui lenh Luu.
    private void dong() { daHuy.set(true); for (var o:mk) { o.setText(""); } if (dialog!=null) { dialog.dispose(); } }
    // Preview dung form that va cung duong kiem tra loi.
    void xemTruoc(BufferedImage h,boolean coLoi) {
        xemTruoc=h; capNhatAnh(); if (coLoi) { if (!matKhau) { ten.setText(""); } kiemTra(); }
    }
}
