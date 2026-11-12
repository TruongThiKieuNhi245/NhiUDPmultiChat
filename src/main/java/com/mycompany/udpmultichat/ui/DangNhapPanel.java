package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.CauHinhClient;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/** Vai tro: Man hinh dang nhap rieng; validate inline, hien/an mat khau, cai dat Server mo rong. Ai goi: ChatMulticastUI, UiPreview. Goi: CauHinhClient, HopThoai, Theme, callback UI. */
@SuppressWarnings("serial")
final class DangNhapPanel extends JPanel {
    // ===== VAI TRO VA CAC THAO TAC =====
    record ThongTin(String ip,String ten,String matKhau,boolean dangKy) { }
    private final JTextField ten=new JTextField(),ip=new JTextField("localhost");
    private final JPasswordField matKhau=new JPasswordField(),nhapLai=new JPasswordField();
    private final JLabel loiTen=loi(),loiMk=loi(),loiLai=loi(),loiIp=loi();
    private final JTabbedPane tab=new JTabbedPane();
    private final JPanel form=new JPanel(),hangLai=new JPanel(new BorderLayout(0,4)),server=new JPanel(new BorderLayout(0,4));
    private final JButton gui=Theme.nut("Đăng nhập",true),caiDat=Theme.nut("Cài đặt máy chủ",false),thu=Theme.nut("Kiểm tra kết nối",false);
    private final JLabel cho=Theme.nhan("Đang kết nối…",false);
    private final JPanel the=Theme.the();
    private int goc;
    private final javax.swing.Timer vongQuay=new javax.swing.Timer(80,e -> { goc=(goc+30)%360; cho.repaint(); });
    private final Consumer<ThongTin> khiGui;
    private final Consumer<String> khiThu;

    // Khoi tao lop va noi cac thanh phan can thiet.
    DangNhapPanel(Consumer<ThongTin> khiGui,Consumer<String> khiThu) {
        super(new GridBagLayout()); this.khiGui=khiGui; this.khiThu=khiThu; setBackground(Theme.NEN);
        var daLuu=CauHinhClient.doc(); ten.setText(daLuu.getProperty("username","")); ip.setText(daLuu.getProperty("server","localhost"));
        the.setLayout(new BorderLayout(0,Theme.S24)); the.setBorder(Theme.le(Theme.S24));

        JPanel dau=new JPanel(new GridLayout(2,1,0,Theme.S8)); dau.setOpaque(false);
        JLabel t=Theme.nhan("UDP Chat",true); t.setFont(Theme.DAM.deriveFont(26f)); dau.add(t);
        var phu=Theme.nhan("Kết nối và trò chuyện cùng nhóm của bạn",false); phu.setForeground(Theme.PHU); dau.add(phu); the.add(dau,BorderLayout.NORTH);
        form.setLayout(new javax.swing.BoxLayout(form,javax.swing.BoxLayout.Y_AXIS));
        tab.setAlignmentX(LEFT_ALIGNMENT); caiDat.setIcon(Theme.icon("down"));
        tab.addTab("Đăng nhập",new JPanel()); tab.addTab("Đăng ký",new JPanel()); tab.setPreferredSize(new Dimension(380,40)); tab.setMaximumSize(new Dimension(600,40)); form.add(tab);
        them(form,"Tên đăng nhập",ten,loiTen); them(form,"Mật khẩu",HopThoai.oMatKhau(matKhau),loiMk);
        them(hangLai,"Nhập lại mật khẩu",HopThoai.oMatKhau(nhapLai),loiLai); hangLai.setVisible(false); form.add(hangLai);
        caiDat.putClientProperty("JButton.buttonType","toolBarButton"); caiDat.setHorizontalAlignment(JButton.LEFT); caiDat.setMargin(new java.awt.Insets(8,0,8,0)); caiDat.setAlignmentX(LEFT_ALIGNMENT); form.add(caiDat);
        them(server,"Địa chỉ Server · cổng 5000",ip,loiIp); server.add(thu,BorderLayout.SOUTH); server.setVisible(false); form.add(server);
        caiDat.addActionListener(e -> { server.setVisible(!server.isVisible()); boCuc(); });
        thu.addActionListener(e -> { if (!ip.getText().isBlank()) { Theme.datLoi(loiIp," "); boCuc(); khiThu.accept(ip.getText().strip()); } else { Theme.datLoi(loiIp,"Nhập địa chỉ Server."); boCuc(); } });
        the.add(form,BorderLayout.CENTER);
        JPanel cuoi=new JPanel(new BorderLayout(0,Theme.S8)); cho.setIcon(Theme.vongCho(() -> goc)); cho.setVisible(false); cho.setPreferredSize(new Dimension(100,24));
        cuoi.add(cho,BorderLayout.NORTH); gui.setPreferredSize(new Dimension(100,42)); cuoi.add(gui,BorderLayout.CENTER); the.add(cuoi,BorderLayout.SOUTH); add(the);
        tab.addChangeListener(e -> { hangLai.setVisible(tab.getSelectedIndex()==1); gui.setText(tab.getSelectedIndex()==1?"Đăng ký":"Đăng nhập"); xoaLoi(); boCuc(); });
        for (var c:form.getComponents()) { if (c instanceof javax.swing.JComponent j) { j.setAlignmentX(LEFT_ALIGNMENT); } }
        gui.addActionListener(e -> gui()); matKhau.addActionListener(e -> gui()); nhapLai.addActionListener(e -> gui()); ten.addActionListener(e -> gui()); boCuc();
    }
    // Loi.
    private static JLabel loi() { JLabel l=Theme.nhan(" ",false); l.setForeground(Theme.LOI); l.setFont(Theme.NHO); l.setVisible(false); return l; }
    // Them.
    private static void them(JPanel cha,String ten,javax.swing.JComponent o,JLabel loi) {
        JPanel hang=Theme.hangNhap(ten,o,loi);
        if (cha.getLayout() instanceof BorderLayout) { cha.add(hang,BorderLayout.CENTER); } else { cha.add(hang); }
    }
    // Gui.
    private void gui() {
        if (!gui.isEnabled()) { return; } xoaLoi();
        if (!ten.getText().strip().matches("[A-Za-z0-9_]{3,20}")) { Theme.datLoi(loiTen,"Tên cần 3–20 chữ a-z, số hoặc _."); boCuc(); return; }
        char[] mk=matKhau.getPassword(),lai=nhapLai.getPassword();
        try {
            if (mk.length<6 || mk.length>128) { Theme.datLoi(loiMk,"Mật khẩu cần 6–128 ký tự."); boCuc(); return; }
            if (tab.getSelectedIndex()==1 && !Arrays.equals(mk,lai)) { Theme.datLoi(loiLai,"Mật khẩu nhập lại chưa khớp."); boCuc(); return; }
            if (ip.getText().isBlank()) { server.setVisible(true); Theme.datLoi(loiIp,"Nhập địa chỉ Server."); boCuc(); return; }
            khiGui.accept(new ThongTin(ip.getText().strip(),ten.getText().strip(),new String(mk),tab.getSelectedIndex()==1));
        } finally { Arrays.fill(mk,'\0'); Arrays.fill(lai,'\0'); }
    }
    // Xoa loi.
    private void xoaLoi() { for (JLabel l:new JLabel[]{loiTen,loiMk,loiLai,loiIp}) { Theme.datLoi(l,""); l.setForeground(Theme.LOI); } }
    // Ban.
    void ban(boolean ban) {
        gui.setEnabled(!ban); thu.setEnabled(!ban); cho.setVisible(ban); tab.setEnabled(!ban);
        if (ban) { vongQuay.start(); } else { vongQuay.stop(); } boCuc();
    }
    // Bo cuc.
    private void boCuc() {
        form.invalidate(); hangLai.invalidate(); server.invalidate(); the.invalidate();
        the.setPreferredSize(null);
        the.setPreferredSize(new Dimension(440,the.getLayout().preferredLayoutSize(the).height)); revalidate(); repaint();
    }
    // Remove notify.
    @Override public void removeNotify() { vongQuay.stop(); super.removeNotify(); }
    // Loi server.
    void loiServer(String noiDung) {
        ban(false);
        if (noiDung.contains("tồn tại") || noiDung.contains("nơi khác")) { Theme.datLoi(loiTen,noiDung); boCuc(); }
        else if (noiDung.contains("mật khẩu") || noiDung.contains("Mật khẩu")) { Theme.datLoi(loiMk,noiDung); boCuc(); }
        else { server.setVisible(true); Theme.datLoi(loiIp,noiDung); boCuc(); }
        boCuc();
    }
    // Ket qua thu.
    void ketQuaThu(String thongBao,boolean ok) { ban(false); server.setVisible(true); loiIp.setForeground(ok?Theme.THANH_CONG:Theme.LOI); Theme.datLoi(loiIp,thongBao); boCuc(); }
    // Dang ky xong.
    void dangKyXong() { tab.setSelectedIndex(0); loiTen.setForeground(Theme.THANH_CONG); Theme.datLoi(loiTen,"Đăng ký thành công. Hãy đăng nhập."); boCuc(); }
    // UiPreview hien tab dang ky voi du lieu gia, khong gui form.
    void xemTruocServer() { server.setVisible(true); boCuc(); }
    // Xem truoc dang nhap.
    void xemTruocDangNhap() { ten.setText("nhikieu"); ip.setText("localhost"); }
    // Xem truoc dang ky.
    void xemTruocDangKy() { tab.setSelectedIndex(1); ten.setText("nhikieu"); }
    // Xoa mat khau.
    void xoaMatKhau() { matKhau.setText(""); nhapLai.setText(""); }
}
