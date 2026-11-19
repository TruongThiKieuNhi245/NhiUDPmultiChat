package com.mycompany.udpmultichat.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;

/** Vai tro: Form admin/mute va xac nhan nguy hiem co loi inline. Ai goi: QuanTriFrame, KiemDuyetUI, QuanTriPreview. Goi: Theme. */
@SuppressWarnings("serial")
final class FormQuanTri extends JPanel {
    private static final int RONG=480;
    private final Map<String,JComponent> o=new LinkedHashMap<>(); private final Map<String,JLabel> loi=new LinkedHashMap<>();
    private final JButton luu=Theme.nut("Lưu",true),huy=Theme.nut("Hủy",false); private final JLabel loiChung=Theme.nhan("",false);
    private final JPanel form=new JPanel(); private JDialog dialog; private Function<Map<String,String>,String> kiemTra=x -> "";
    // Cung hangNhap nhu form dang nhap de khong nen input khi co loi.
    FormQuanTri(String nut,boolean nguyHiem) {
        super(new BorderLayout(0,Theme.S12)); setBorder(Theme.le(Theme.S24)); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS)); add(form);
        JPanel duoi=new JPanel(new BorderLayout()); loiChung.setForeground(Theme.LOI); loiChung.setVisible(false); duoi.add(loiChung,BorderLayout.NORTH);
        JPanel nuts=new JPanel(new FlowLayout(FlowLayout.RIGHT)); luu.setText(nut); if (nguyHiem) { luu.setBackground(Theme.LOI); luu.setForeground(Theme.THE); } nuts.add(huy); nuts.add(luu); duoi.add(nuts); add(duoi,BorderLayout.SOUTH);
        huy.addActionListener(e -> dong());
    }
    // Tieu de mo ta hau qua, ten doi tuong duoc dam va khong chen HTML nguoi dung.
    FormQuanTri moTa(String ten,String hauQua) {
        JLabel t=Theme.nhan(ten,true); t.setFont(Theme.DAM); t.setAlignmentX(LEFT_ALIGNMENT); form.add(t);
        JTextArea m=new JTextArea(hauQua); m.setFont(Theme.FONT);
        m.setLineWrap(true); m.setWrapStyleWord(true); m.setEditable(false); m.setOpaque(false); m.setAlignmentX(LEFT_ALIGNMENT);
        int rong=RONG-2*Theme.S24;
        m.setSize(rong,Short.MAX_VALUE);
        int cao=m.getPreferredSize().height+Theme.S8;
        m.setPreferredSize(new Dimension(rong,cao)); m.setMaximumSize(new Dimension(Integer.MAX_VALUE,cao));
        form.add(m); return this;
    }
    // Chi co dinh chieu rong; chieu cao phai tinh lai khi hien loi, khong cache form cu.
    @Override public Dimension getPreferredSize() {
        Dimension d=super.getPreferredSize(); d.width=Math.max(RONG,d.width); return d;
    }
    // Them input thong thuong.
    FormQuanTri text(String key,String nhan,String value) { JTextField f=new JTextField(value); them(key,nhan,f); return this; }
    // Mat khau co nut Hien cung kieu cac form da co.
    FormQuanTri matKhau(String key,String nhan) {
        JPasswordField f=new JPasswordField(); JPanel hang=new JPanel(new BorderLayout(Theme.S4,0));
        hang.add(f,BorderLayout.CENTER); hang.add(Theme.nutMatKhau(f),BorderLayout.EAST); them(key,nhan,hang); o.put(key,f); return this;
    }
    // Combo quyen/loai/co thoi han.
    FormQuanTri chon(String key,String nhan,String value,String... ds) {
        JComboBox<String> f=new JComboBox<>(ds); f.setSelectedItem(value);
        if (key.equals("vai")) {
            f.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list,Object v,int i,boolean chon,boolean focus) {
                    super.getListCellRendererComponent(list,v,i,chon,focus);
                    setText("ADMIN".equals(v)?"Quản trị viên":"Thành viên"); return this;
                }
            });
        }
        them(key,nhan,f); return this;
    }
    // Mot diem them field, loi cua field khong bi gan nham chi so.
    private void them(String key,String nhan,JComponent f) { JLabel l=Theme.nhan("",false); l.setForeground(Theme.LOI); l.setVisible(false); o.put(key,f); loi.put(key,l); form.add(Theme.hangNhap(nhan,f,l)); }
    // Doc gia tri thanh map de callback khong can biet chi tiet Swing.
    Map<String,String> giaTri() {
        Map<String,String> m=new LinkedHashMap<>(); o.forEach((k,f) -> m.put(k,f instanceof JComboBox<?> c?String.valueOf(c.getSelectedItem()):f instanceof JPasswordField p?new String(p.getPassword()):((JTextField)f).getText().strip())); return m;
    }
    // Validator tra key|noi dung; chuoi rong la hop le.
    FormQuanTri kiemTra(Function<Map<String,String>,String> f) { kiemTra=f; return this; }
    // Loi server hien trong form; khong dong form khi chua thanh cong.
    void loi(String text) { loiChung.setText(text); loiChung.setVisible(true); luu.setEnabled(true); boCuc(); }
    // Gan loi theo key bat bien, co the dung boi UiPreview.
    void loiO(String key,String text) { Theme.datLoi(loi.get(key),text); boCuc(); }
    // BoxLayout can xoa cache ngay ca khi form chua displayable trong UiPreview.
    private void boCuc() {
        form.invalidate(); invalidate();
        if (dialog!=null) { dialog.pack(); }
        revalidate(); repaint();
    }
    // Tao dialog co Enter/Esc, mo khong chan luong mang.
    void hien(Window cha,String title,Consumer<Map<String,String>> xuLy) {
        dialog=new JDialog(cha,title,Dialog.ModalityType.MODELESS); dialog.setContentPane(this); dialog.getRootPane().setDefaultButton(luu);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().registerKeyboardAction(e -> dong(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        luu.addActionListener(e -> {
            loi.values().forEach(l -> Theme.datLoi(l,"")); loiChung.setVisible(false); Map<String,String> m=giaTri(); String kq=kiemTra.apply(m);
            if (!kq.isEmpty()) { String[] p=kq.split("\\|",2); if (p.length==2 && loi.containsKey(p[0])) { loiO(p[0],p[1]); } else { loi(kq); } return; }
            luu.setEnabled(false); xuLy.accept(m);
        });
        dialog.pack(); dialog.setLocationRelativeTo(cha); dialog.setVisible(true);
    }
    // Ket thuc sau khi Server xac nhan; Huy chi dong form, khong gui lenh.
    void dong() { if (dialog!=null) { dialog.dispose(); } }
    // Form tai khoan chung cho them va sua, khong cho doi ten dang nhap khi sua.
    static FormQuanTri nguoiDung(boolean them,java.util.List<String> n) {
        FormQuanTri f=new FormQuanTri(them?"Thêm người dùng":"Lưu",false);
        if (them) { f.text("login","Tên đăng nhập",""); }
        f.text("ten","Tên hiển thị",them?"":n.get(2)).chon("vai","Vai trò",them?"THANH_VIEN":n.get(3),"THANH_VIEN","ADMIN");
        if (!them) { f.text("gt","Giới thiệu",n.get(11)); }
        if (them) { f.matKhau("mk","Mật khẩu tạm (buộc đổi lần đầu)"); }
        return f.kiemTra(m -> {
            if (them && !m.get("login").matches("[a-z0-9_]{3,20}")) { return "login|Tên cần 3–20 chữ a-z, số hoặc gạch dưới."; }
            if (m.get("ten").length()<2 || m.get("ten").length()>30) { return "ten|Tên hiển thị cần 2–30 ký tự."; }
            if (!them && m.get("gt").length()>100) { return "gt|Giới thiệu tối đa 100 ký tự."; }
            if (them && (m.get("mk").length()<6 || m.get("mk").length()>128)) { return "mk|Mật khẩu cần 6–128 ký tự."; } return "";
        });
    }
}
