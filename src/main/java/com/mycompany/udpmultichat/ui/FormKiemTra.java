package com.mycompany.udpmultichat.ui;

import java.awt.Container;
import java.util.Map;
import javax.swing.*;
import com.mycompany.udpmultichat.common.model.Phong;

/** Vai tro: Hoi quy form Swing tren component that, khong mang. Ai goi: UiPreview. Goi: HopThoai, SoanTinPanel. */
final class FormKiemTra {
    // Lop chi cung cap assertions khong reflection.
    private FormKiemTra() { }
    // ===== KIEM TRA LOI THEO KHOA =====
    static void chay() {
        Phong p=new Phong("demo","Phòng mẫu","","ABC123","chu","239.255.42.2",9002,20,true,false,true,1,"2026-11-13");
        for (Phong phong:new Phong[]{null,p}) { kiemTraPhong(phong); }
        HopThoai vao=new HopThoai(null,true); vao.coMatKhau();
        vao.datMa("A"); vao.kiemTra(); can(vao.cacLoi().keySet().equals(java.util.Set.of("ma")),"Ma sai o");
        vao.datMa("ABC123"); ((JPasswordField)tim(vao,"mk",JPasswordField.class)).setText("a".repeat(129));
        vao.kiemTra(); can(vao.cacLoi().equals(Map.of("mk","Mật khẩu tối đa 128 ký tự.")),"Mat khau vao sai o");
        HopThoai loiServer=new HopThoai(null,true); loiServer.loiServer("Mật khẩu phòng không đúng.");
        can(loiServer.cacLoi().equals(Map.of("mk","Mật khẩu phòng không đúng.")),"Loi Server vao sai o");
        System.out.println("PASS M3a.1: tung loi ten/mo ta/gioi han/mat khau dung o tao/sua/vao");
    }
    // Dien tung gia tri sai vao form that, bam validator that.
    private static void kiemTraPhong(Phong phong) {
        String[] khoa={"ten","mota","gioihan","mk"};
        String[] text={"Tên phòng cần 3-30 ký tự.","Mô tả tối đa 80 ký tự.","Giới hạn cần 2-500 người.","Mật khẩu tối đa 128 ký tự."};
        for (int i=0;i<khoa.length;i++) {
            HopThoai f=new HopThoai(phong,false);
            ((JTextField)tim(f,"ten",JTextField.class)).setText("Phòng hợp lệ");
            if (phong!=null) { ((JComboBox<?>)tim(f,"chedo",JComboBox.class)).setSelectedIndex(1); }
            switch (khoa[i]) {
                case "ten": ((JTextField)tim(f,"ten",JTextField.class)).setText("a"); break;
                case "mota": ((JTextField)tim(f,"mota",JTextField.class)).setText("a".repeat(81)); break;
                case "gioihan": ((JSpinner)tim(f,"gioihan",JSpinner.class)).setValue(1); break;
                case "mk": ((JPasswordField)tim(f,"mk",JPasswordField.class)).setText("a".repeat(129)); break;
                default: throw new AssertionError();
            }
            can(!f.kiemTra() && f.cacLoi().equals(Map.of(khoa[i],text[i])),"Validator sai o "+khoa[i]+": "+f.cacLoi());
            HopThoai server=new HopThoai(phong,false); server.loiServer(text[i]);
            can(server.cacLoi().equals(Map.of(khoa[i],text[i])),"Server sai o "+khoa[i]);
        }
    }
    // Anh loi dung gia tri sai that cho tung truong, khong hien loi tren du lieu hop le.
    static void dienLoi(HopThoai f) {
        ((JTextField)tim(f,"ten",JTextField.class)).setText("a");
        ((JTextField)tim(f,"mota",JTextField.class)).setText("Mô tả "+"a".repeat(75));
        ((JSpinner)tim(f,"gioihan",JSpinner.class)).setValue(1);
        var cheDo=tim(f,"chedo",JComboBox.class); if (cheDo!=null) { ((JComboBox<?>)cheDo).setSelectedIndex(1); }
        ((JPasswordField)tim(f,"mk",JPasswordField.class)).setText("a".repeat(129));
    }
    // Tim component theo ten va kieu, khong phu thuoc vi tri.
    private static java.awt.Component tim(Container cha,String ten,Class<?> loai) {
        if (ten.equals(cha.getName()) && loai.isInstance(cha)) { return cha; }
        for (var c:cha.getComponents()) {
            if (c instanceof Container n) { var k=tim(n,ten,loai); if (k!=null) { return k; } }
        }
        return null;
    }
    // Chua qua nam dong thi scroll pane khong duoc lo thanh cuon.
    static void kiemTraCuon(Container cha) {
        for (var c:cha.getComponents()) {
            if (c instanceof JScrollPane p && "cuon-soan".equals(p.getName())) {
                JTextArea o=(JTextArea)p.getViewport().getView();
                if (o.getLineCount()<=1 && o.getFontMetrics(o.getFont()).stringWidth(o.getText())<Math.max(150,o.getWidth()-8)) { can(p.getVerticalScrollBarPolicy()==JScrollPane.VERTICAL_SCROLLBAR_NEVER && !p.getVerticalScrollBar().isVisible(),"Lo thanh cuon mot dong"); }
            }
            if (c instanceof Container n) { kiemTraCuon(n); }
        }
    }
    // Tin den cham khong duoc khoi phuc noi dung tren EDT sau thu hoi.
    static void kiemTraThuHoi(Phong p) {
        var k=new KhungChatPanel(p); var t=new com.mycompany.udpmultichat.common.model.TinNhan(1,p.id,"a","An","Bí mật",1);
        k.thuHoi(1); k.themTin(t,false);
        can(k.locTin(t).thuHoi && k.locTin(t).noiDung.isEmpty(),"Tin cu song lai sau thu hoi");
        String ma="x".repeat(22); k.themTin(new com.mycompany.udpmultichat.common.model.TinNhan(2,p.id,"a","An","A",1,ma,"T"),false);
        k.themTin(new com.mycompany.udpmultichat.common.model.TinNhan(3,p.id,"b","Bình","B",1,ma,"T"),false);
        can(soHang(k)==3,"Hai nguoi dung cung ma Client lam ghi de hang tin");
    }
    // Dem hang tin tren component that, khong duyet qua field bang reflection.
    private static int soHang(Container cha) {
        int so=0; for (var c:cha.getComponents()) { if (c instanceof HangTin) { so++; } else if (c instanceof Container n) { so+=soHang(n); } } return so;
    }
    // Assertion luon bat ke ca khi JVM khong co -ea.
    private static void can(boolean dung,String loi) { if (!dung) { throw new AssertionError(loi); } }
}
