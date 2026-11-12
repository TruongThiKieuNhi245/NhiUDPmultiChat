package com.mycompany.udpmultichat.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import static com.mycompany.udpmultichat.ui.ThanhPhan.*;

/** Chi gom o nhap va su kien Swing; khong tao socket hay xu ly dang nhap. */
@SuppressWarnings("serial")
public final class DangNhapPanel extends JPanel {
    private final JTextField ip = new JTextField("localhost", 11);
    private final JTextField ten = new JTextField(10);
    private final JPasswordField matKhau = new JPasswordField(10);
    private final NutBo dangNhap = new NutBo("Đăng nhập", new Color(0x4F6BED), new Color(0x3B54C9));
    private final NutBo dangKy = new NutBo("Đăng ký", new Color(0x3DB88B), new Color(0x28936D));
    private final Consumer<ThongTin> khiGui;

    public DangNhapPanel(Consumer<ThongTin> khiGui) {
        this.khiGui = khiGui;
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBorder(new EmptyBorder(4, 8, 4, 8));
        setBackground(Color.WHITE);
        add(new JLabel("Server :5000")); add(ip);
        add(new JLabel("Tài khoản")); add(ten);
        add(new JLabel("Mật khẩu")); add(matKhau);
        add(dangNhap); add(dangKy);
        for (JTextField o : new JTextField[] { ip, ten, matKhau }) {
            o.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
        dangNhap.addActionListener(e -> gui(false));
        dangKy.addActionListener(e -> gui(true));
        matKhau.addActionListener(e -> gui(false));
    }

    // Doc truong tren EDT, sau do chuyen ban sao du lieu cho ClientCore.
    private void gui(boolean laDangKy) {
        if (!dangNhap.isEnabled()) { return; }
        char[] kyTu = matKhau.getPassword();
        String banSao = new String(kyTu);
        Arrays.fill(kyTu, '\0');
        khiGui.accept(new ThongTin(ip.getText().trim(), ten.getText().trim(), banSao, laDangKy));
    }

    public void choPhep(boolean bat) {
        ip.setEnabled(bat); ten.setEnabled(bat); matKhau.setEnabled(bat);
        dangNhap.setEnabled(bat); dangKy.setEnabled(bat);
    }

    public void xoaMatKhau() { matKhau.setText(""); }

    /** Ban sao dau vao, chi ton tai trong thao tac dang nhap/dang ky. */
    public record ThongTin(String ip, String ten, String matKhau, boolean dangKy) { }
}
