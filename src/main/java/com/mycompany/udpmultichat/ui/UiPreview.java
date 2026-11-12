package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.ServerMain;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Vai tro: Xuat PNG tu COMPONENT THAT, khong Server/socket; du lieu mau chi ton tai trong RAM. Ai goi: Maven, lap trinh vien. Goi: cac component UI that, ImageIO. */
public final class UiPreview {
    // ===== VAI TRO VA CAC THAO TAC =====
    // Khoi tao lop va noi cac thanh phan can thiet.
    private UiPreview() { }
    private static final Path THU_MUC=Path.of("docs","screenshots");
    private static int soAnh;

    // Maven: exec:java -Dexec.mainClass=com.mycompany.udpmultichat.ui.UiPreview
    public static void main(String[] args) throws Exception {
        Theme.caiDat(); Files.createDirectories(THU_MUC);
        SwingUtilities.invokeAndWait(() -> {
            try { dungVaXuat(); }
            catch (IOException e) { throw new IllegalStateException("Không xuất được PNG",e); }
        });
        System.out.println("UiPreview: "+soAnh+" PNG → "+THU_MUC.toAbsolutePath());
    }
    // Dung va xuat.
    private static void dungVaXuat() throws IOException {
        DangNhapPanel login=new DangNhapPanel(t -> { },ip -> { });
        login.xemTruocDangNhap();
        xuat(login,"01-dang-nhap.png",1120,800);
        login.xemTruocDangKy(); xuat(login,"02-dang-ky.png",1120,800);
        login.loiServer("Tên đăng nhập đã tồn tại"); xuat(login,"03-loi-form.png",1120,800);
        xuatLoiForm();
        xuatChat();
        xuatVaoCuoi();
        SoanTinPanel soan=new SoanTinPanel(text -> { }); soan.xemTruoc("ắ".repeat(334));
        xuat(soan,"13-tin-qua-dai.png",760,220);
        DangNhapPanel settings=new DangNhapPanel(t -> { },ip -> { });
        settings.xemTruocDangNhap(); settings.xemTruocServer();
        xuat(settings,"14-cai-dat-server.png",1120,800);
        ServerMain server=new ServerMain();
        try { server.duLieuMau(); xuat(server.getRootPane(),"12-server.png",900,620); } finally { server.dispose(); }
    }
    // Loi that cua login/signup va room form phai giu chieu cao moi o nhap.
    private static void xuatLoiForm() throws IOException {
        DangNhapPanel login=new DangNhapPanel(t -> { },ip -> { }); login.xemTruocDangNhap();
        login.loiServer("Tên đăng nhập đã tồn tại"); login.loiServer("Mật khẩu cần 6–128 ký tự.");
        login.ketQuaThu("Server chưa phản hồi.",false); xuat(login,"20-dang-nhap-nhieu-loi.png",1120,900);
        DangNhapPanel signup=new DangNhapPanel(t -> { },ip -> { }); signup.xemTruocDangKy();
        java.util.List<javax.swing.JPasswordField> mk=new java.util.ArrayList<>(); timMatKhau(signup,mk);
        mk.get(0).setText("abcdef123"); mk.get(1).setText("khongkhop"); bamNut(signup,"Đăng ký");
        signup.loiServer("Tên đăng nhập đã tồn tại"); signup.loiServer("Mật khẩu cần 6–128 ký tự.");
        xuat(signup,"21-dang-ky-nhieu-loi.png",1120,900);
        Phong p=new Phong("mau","Phòng mẫu","Mô tả mẫu","MAU123","nhi","239.255.42.2",9002,20,true,false,true,2,"2026-11-12");
        xuatLoiPhong(new HopThoai(null,false),"22-tao-phong-co-loi.png");
        xuatLoiPhong(new HopThoai(p,false),"23-sua-phong-co-loi.png");
        HopThoai vao=new HopThoai(null,true); vao.datMa("SAI123");
        vao.loiServer("Không tìm thấy mã phòng."); vao.loiServer("Mật khẩu phòng không đúng.");
        xuatForm(vao,"24-vao-phong-co-loi.png");
        System.out.println("PASS UiPreview M2.7: input login/signup/room giu chieu cao khi hien loi");
    }
    // Tat ca truong text cua form phong deu co dong loi cung luc.
    private static void xuatLoiPhong(HopThoai f,String ten) throws IOException {
        f.loiServer("Tên phòng cần 3–30 ký tự."); f.loiServer("Mô tả tối đa 80 ký tự.");
        f.loiServer("Giới hạn cần 2–500 người."); f.loiServer("Mật khẩu tối đa 128 ký tự."); xuatForm(f,ten);
    }
    // Duyet component that de dien nhap lai mat khau, khong dung reflection.
    private static void timMatKhau(Container cha,java.util.List<javax.swing.JPasswordField> ds) {
        for (var con:cha.getComponents()) {
            if (con instanceof javax.swing.JPasswordField o) { ds.add(o); }
            if (con instanceof Container n) { timMatKhau(n,ds); }
        }
    }
    // Goi dung nut submit cua form de hien loi kiem tra nhap lai.
    private static void bamNut(Container cha,String ten) {
        for (var con:cha.getComponents()) {
            if (con instanceof javax.swing.JButton b && ten.equals(b.getText())) { b.doClick(0); return; }
            if (con instanceof Container n) { bamNut(n,ten); }
        }
    }
    // Chat, toast va hop thoai dung component that cua cua so chinh.
    private static void xuatChat() throws IOException {
        ChatMulticastUI frame=new ChatMulticastUI();
        try {
            NguoiDung n=new NguoiDung("nhi","nhikieu","Kiều Nhi",VaiTro.ADMIN,"2026-11-12");
            Phong sanh=new Phong("sanh","Sảnh chung","Kênh chung của lớp","SANH01","HE_THONG","239.255.42.1",9001,500,true,false,true,2,"2026-11-12").voiThongTin(true,"Hệ thống",3);
            Phong nhom=new Phong("demo","Bài tập mạng","Nhóm chuẩn bị demo UDP","NET246","nhi","239.255.42.2",9002,20,false,true,true,2,"2026-11-12").voiThongTin(true,"Kiều Nhi (@nhikieu)",2);
            Phong congKhai=new Phong("hoc","Lập trình Java","Cùng giải bài tập tuần này","JAVA26","an","239.255.42.3",9003,30,true,false,true,1,"2026-11-12");
            frame.duLieuMau(n,List.of(sanh,nhom,congKhai));
            frame.onThanhVienChiTiet(sanh.id,List.of(new Phong.ThanhVien("nhi","nhikieu","Kiều Nhi","ADMIN",true),
                new Phong.ThanhVien("an","minhan","Minh An","THANH_VIEN",true),new Phong.ThanhVien("ha","thuha","Thu Hà","THANH_VIEN",false)));
            frame.onThanhVienChiTiet(nhom.id,List.of(new Phong.ThanhVien("nhi","nhikieu","Kiều Nhi","ADMIN",true),new Phong.ThanhVien("an","minhan","Minh An","THANH_VIEN",true)));
            frame.onHeThong(sanh.id,"Minh An đã vào Sảnh chung.");
            long gio=java.time.Instant.parse("2026-11-12T05:30:00Z").toEpochMilli();
            frame.onTinNhan(new TinNhan(1,sanh.id,"an","Minh An","Chào Nhi, mình đã kiểm tra chat ở hai phòng.\nMọi thứ sẵn sàng cho buổi demo.",gio-120000),false,false);
            frame.onTinNhan(new TinNhan(2,sanh.id,"nhi","Kiều Nhi","Cảm ơn An! Nhớ thử mã phòng và trường hợp mất kết nối nữa nhé.",gio-60000),true,false);
            frame.onTinNhan(new TinNhan(3,sanh.id,"an","Minh An","Mình sẽ chạy đủ 3 Client để kiểm tra.",gio),false,false);
            frame.onTinNhan(new TinNhan(1,nhom.id,"an","Minh An","Mình đã gửi kịch bản demo vào nhóm.",gio),false,false);
            xuat(frame.getRootPane(),"04-chat.png",1240,800);
            kiemTraBadge(frame,"demo");
            frame.onKetNoi(false,"Mất kết nối, đang thử kết nối lại…"); xuat(frame.getRootPane(),"05-mat-ket-noi.png",1240,800);
            frame.onKetNoi(true,"Đã kết nối lại và đồng bộ tin nhắn.");
            frame.toast(ThongBao.Muc.THANH_CONG,"Đã tạo phòng «Bài tập mạng».");
            frame.toast(ThongBao.Muc.THONG_TIN,"Mã phòng đã được sao chép.");
            frame.toast(ThongBao.Muc.CANH_BAO,"Phòng đang đóng. Bạn vẫn xem được lịch sử.");
            xuat(frame.getRootPane(),"06-toast-ba-muc.png",1240,800);
            frame.toast(ThongBao.Muc.LOI,"Phòng đã đầy. Hãy liên hệ chủ phòng.");
            xuat(frame.getRootPane(),"07-toast-loi.png",1240,800);
            xuat(frame.menuAvatar(),"08-menu-nguoi-dung.png",220,50);
            HopThoai tao=new HopThoai(null,false); xuatForm(tao,"09-tao-phong.png");
            xuatForm(new HopThoai(nhom,false),"10-sua-phong.png");
            HopThoai vao=new HopThoai(null,true); vao.coMatKhau(); xuatForm(vao,"11-vao-phong.png");
            xuat(HopThoai.thongTin(nhom,List.of()),"15-thong-tin-phong.png",490,540);
        } finally { frame.dispose(); }
    }
    // Kiem thu ve lai danh tinh muon va hai nguoi trung ten, khong can Server.
    private static void xuatVaoCuoi() throws IOException {
        ChatMulticastUI frame=new ChatMulticastUI();
        try {
            Phong p=new Phong("sanh","Sảnh chung","","SANH01","HE_THONG","239.255.42.1",9001,500,true,false,true,3,"2026-11-12").voiThongTin(true,"Hệ thống",3);
            frame.duLieuMau(new NguoiDung("ad","admin","Admin",VaiTro.ADMIN,"2026-11-12"),List.of(p));
            if (demNhan(frame.getContentPane(),"PHÒNG CÔNG KHAI")!=0) { throw new AssertionError("Nhom phong cong khai rong phai an"); }
            frame.onThanhVienChiTiet(p.id,List.of(new Phong.ThanhVien("ad","admin","Admin","ADMIN",true),
                new Phong.ThanhVien("a","aaa","aaa","THANH_VIEN",true),new Phong.ThanhVien("b","bbb","bbb","THANH_VIEN",true)));
            frame.onNguoiDung(new NguoiDung("b","bbb","bbb",VaiTro.THANH_VIEN,"2026-11-12"));
            if (demNhan(frame.getContentPane(),"Bạn")!=1) { throw new AssertionError("Client vao cuoi phai co dung 1 nhan Ban"); }
            frame.onTinNhan(new TinNhan(1,p.id,"b","bbb","111",java.time.Instant.parse("2026-11-12T05:30:00Z").toEpochMilli()),true,false);
            xuat(frame.getRootPane(),"16-client-vao-cuoi.png",1240,800);
            xuat(frame.getRootPane(),"17-chat-960x600.png",960,600);
            frame.onThanhVienChiTiet(p.id,List.of(new Phong.ThanhVien("a","aaa","Trùng tên","THANH_VIEN",true),new Phong.ThanhVien("b","bbb","Trùng tên","THANH_VIEN",true)));
            frame.onNguoiDung(new NguoiDung("b","bbb","Trùng tên",VaiTro.THANH_VIEN,"2026-11-12"));
            if (demNhan(frame.getContentPane(),"Bạn")!=1 || !banDungTaiKhoan(frame.getContentPane(),"bbb")) {
                throw new AssertionError("Hai nguoi trung ten van phai gan Ban dung tai khoan bbb");
            }

        } finally { frame.dispose(); }
        SoanTinPanel soan=new SoanTinPanel(t -> { }); soan.xemTruoc("Dòng thứ nhất\nDòng thứ hai\nDòng thứ ba\nDòng thứ tư\nDòng thứ năm");
        xuat(soan,"18-soan-nhieu-dong.png",760,220);
        javax.swing.JPanel so=new javax.swing.JPanel(new java.awt.FlowLayout());
        so.add(new ThanhPhan.BongTin("aaa","111","12:30",false)); so.add(new ThanhPhan.BongTin("bbb","111","12:30",true));
        xuat(so,"19-chu-so.png",420,130);
        System.out.println("PASS UiPreview: profile doi -> ve lai dung 1 Ban, hai ten hien thi trung nhau");
    }
    // Click dong phong that phai chuyen card va xoa badge ngay, khong mo socket.
    private static void kiemTraBadge(ChatMulticastUI frame,String id) {
        var dong=timTheoTen(frame.getContentPane(),"phong-"+id);
        if (dong==null) { throw new AssertionError("Khong tim thay phong de thu chuyen card"); }
        dong.dispatchEvent(new java.awt.event.MouseEvent(dong,java.awt.event.MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),0,8,8,1,false,java.awt.event.MouseEvent.BUTTON1));
        var badge=timTheoTen(frame.getContentPane(),"badge-"+id);
        if (!(badge instanceof javax.swing.JLabel l) || !l.getText().isEmpty()) { throw new AssertionError("Badge khong xoa sau khi chon phong"); }
        var sanh=timTheoTen(frame.getContentPane(),"phong-sanh");
        sanh.dispatchEvent(new java.awt.event.MouseEvent(sanh,java.awt.event.MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),0,8,8,1,false,java.awt.event.MouseEvent.BUTTON1));
        System.out.println("PASS UiPreview: chuyen phong o sidebar xoa badge ngay, khong can mang");
    }
    // Tim component theo ten nghiep vu, khong truy cap field bang reflection.
    private static java.awt.Component timTheoTen(Container cha,String ten) {
        for (var con:cha.getComponents()) {
            if (ten.equals(con.getName())) { return con; }
            if (con instanceof Container n) { var kq=timTheoTen(n,ten); if (kq!=null) { return kq; } }
        }
        return null;
    }
    // Kiem tra nhan Ban o dung dong tai khoan, khong chi dem tong nhan.
    private static boolean banDungTaiKhoan(Container cha,String taiKhoan) {
        for (var con:cha.getComponents()) {
            if (!(con instanceof Container n)) { continue; }
            if (taiKhoan.equals(n.getName()) && demNhan(n,"Bạn")==1) { return true; }
            if (banDungTaiKhoan(n,taiKhoan)) { return true; }
        }
        return false;
    }
    // Doc nhan Swing that, khong reflection hay so sanh anh gia.
    private static int demNhan(Container c,String text) {
        int so=0;
        for (var con:c.getComponents()) {
            if (con instanceof javax.swing.JLabel l && l.getText().equals(text)) { so++; }
            if (con instanceof Container n) { so+=demNhan(n,text); }
        }
        return so;
    }
    // Ve ca cay component len BufferedImage; khong chup desktop hoac gui du lieu ra ngoai.
    private static void xuat(JComponent c,String ten,int rong,int cao) throws IOException {
        c.setSize(rong,cao); boCuc(c);
        kiemTraONhap(c);
        kiemTraFontNoiDung(c);
        kiemTraToast(c,c);
        kiemTraNutTao(c);
        BufferedImage anh=new BufferedImage(rong,cao,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=anh.createGraphics(); g.setColor(Theme.NEN); g.fillRect(0,0,rong,cao); c.printAll(g); g.dispose();
        ImageIO.write(anh,"png",THU_MUC.resolve(ten).toFile()); soAnh++;
        if (ten.equals("19-chu-so.png")) { kiemTraMauChu(anh,c); }
        if (anh.getWidth()!=rong || anh.getHeight()!=cao) { throw new IOException("Sai kích thước PNG"); }
    }
    // Toast vua noi dung, giu khoang 12px tren vien soan va khong che o nhap.
    private static void kiemTraToast(Container goc,Container cha) {
        var soan=timTheoTen(goc,"khung-soan");
        for (var con:cha.getComponents()) {
            if (!con.isVisible()) { continue; }
            if ("toast".equals(con.getName()) && soan!=null) {
                var r=SwingUtilities.convertRectangle(con.getParent(),con.getBounds(),goc);
                var s=SwingUtilities.convertRectangle(soan.getParent(),soan.getBounds(),goc);
                if (r.width>360 || r.height<44 || r.y+r.height>s.y-12) { throw new AssertionError("Toast sai vi tri/kich thuoc"); }
            }
            if (con instanceof Container n) { kiemTraToast(goc,n); }
        }
    }
    // Noi dung chat/soan tin va so 111 deu dung PLAIN; chi tieu de/ten duoc dam.
    private static void kiemTraFontNoiDung(Container cha) {
        for (var con:cha.getComponents()) {
            if (!con.isVisible()) { continue; }
            if (con instanceof javax.swing.JTextArea t && t.getFont().getStyle()!=java.awt.Font.PLAIN) {
                throw new AssertionError("Noi dung JTextArea khong dung Font.PLAIN");
            }
            if (con instanceof Container n) { kiemTraFontNoiDung(n); }
        }
    }
    // Bat loi o nhap bi ep thanh vach khi JLabel bao loi hien ra.
    private static void kiemTraONhap(Container cha) {
        for (var con:cha.getComponents()) {
            if (!con.isVisible()) { continue; }
            if (con instanceof javax.swing.JTextField t && t.getHeight()<chieuCaoToiThieu(t)) {
                throw new AssertionError("O nhap bi nen: cao="+t.getHeight()+", text="+t.getText());
            }
            if (con instanceof Container n) { kiemTraONhap(n); }
        }
    }
    // Editor trong Spinner tru di vien cua Spinner; input thuong phai du 28px.
    private static int chieuCaoToiThieu(javax.swing.JTextField t) {
        return SwingUtilities.getAncestorOfClass(javax.swing.JSpinner.class,t)==null?28:22;
    }
    // Anh form dung kich thuoc tu nhien sau khi bat/tat dong loi.
    private static void xuatForm(JComponent c,String ten) throws IOException {
        var d=c.getPreferredSize(); xuat(c,ten,d.width,d.height);
    }
    // Nut Tao phong phai du be rong cho chu, ke ca o cua so 960x600.
    private static void kiemTraNutTao(Container cha) {
        for (var con:cha.getComponents()) {
            if (!con.isVisible()) { continue; }
            if (con instanceof javax.swing.JButton b && "+ Tạo phòng".equals(b.getText())) {
                int can=b.getFontMetrics(b.getFont()).stringWidth(b.getText())+b.getInsets().left+b.getInsets().right;
                if (b.getWidth()<can) { throw new AssertionError("Nut Tao phong bi cat chu"); }
            }
            if (con instanceof Container n) { kiemTraNutTao(n); }
        }
    }
    // Grayscale AA chi pha cung mot ti le RGB giua chu trang va nen xanh.
    private static void kiemTraMauChu(BufferedImage anh,Container cha) {
        for (var con:cha.getComponents()) {
            if (con instanceof javax.swing.JTextArea t && t.getForeground().equals(Theme.THE)) {
                java.awt.Point goc=SwingUtilities.convertPoint(t,0,0,cha);
                kiemTraPixel(anh,new java.awt.Rectangle(goc.x,goc.y,t.getWidth(),t.getHeight()));
            }
            if (con instanceof Container n) { kiemTraMauChuCon(anh,cha,n); }
        }
        System.out.println("PASS UiPreview: chu 111 khong co vien RGB do LCD antialiasing");
    }
    // Tim textarea trong bong tin va doi toa do ve goc anh.
    private static void kiemTraMauChuCon(BufferedImage anh,Container goc,Container cha) {
        for (var con:cha.getComponents()) {
            if (con instanceof javax.swing.JTextArea t && t.getForeground().equals(Theme.THE)) {
                java.awt.Point p=SwingUtilities.convertPoint(t,0,0,goc); kiemTraPixel(anh,new java.awt.Rectangle(p.x,p.y,t.getWidth(),t.getHeight()));
            }
            if (con instanceof Container n) { kiemTraMauChuCon(anh,goc,n); }
        }
    }
    // Dung sai lam tron 8-bit, khong chap nhan lech mau LCD subpixel.
    private static void kiemTraPixel(BufferedImage anh,java.awt.Rectangle r) {
        for (int y=r.y;y<r.y+r.height;y++) {
            for (int x=r.x;x<r.x+r.width;x++) {
                int rgb=anh.getRGB(x,y);
                double a=(((rgb>>16)&255)-Theme.CHINH.getRed())/(255.0-Theme.CHINH.getRed());
                double b=(((rgb>>8)&255)-Theme.CHINH.getGreen())/(255.0-Theme.CHINH.getGreen());
                double c=((rgb&255)-Theme.CHINH.getBlue())/(255.0-Theme.CHINH.getBlue());
                if (Math.max(a,Math.max(b,c))-Math.min(a,Math.min(b,c))>0.12) { throw new AssertionError("Chu so con vien RGB"); }
            }
        }
    }
    // Bo cuc.
    private static void boCuc(Container c) { c.doLayout(); for (var con:c.getComponents()) { if (con instanceof Container n) { boCuc(n); } } }
}
