package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.client.*;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.ServerCore;
import java.awt.*;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import static com.mycompany.udpmultichat.ui.SwingThu.*;

/** Vai tro: Test handler Swing that voi Server/ba Client UDP that, khong phai test chuot/phim desktop. Ai goi: main rieng. Goi: QuanTriFrame, FormQuanTri, Core. */
public final class QuanTriTuongTacSelfTest {
    // Khong khoi tao lop tien ich.
    private QuanTriTuongTacSelfTest() { }

    // Chay rieng vi can AWT headful; khong cong vao SelfTest mang khong Swing.
    public static void main(String[] args) throws Exception {
        MangUtil.dungIPv4(); Theme.caiDat(); Files.createDirectories(Path.of(".selftest"));
        try (BoTest t=new BoTest()) {
            t.mo();
            t.formVaCrud();
            System.out.println("PASS Swing M5 form loi/Enter/Esc, tao va sua user qua handler that");
            t.resetKhoaXoa();
            System.out.println("PASS Swing M5 reset ngat phien, khoa/mo, xoa xac nhan va guard chinh minh");
            t.kiemDuyet();
            System.out.println("PASS Swing M5 mute/unmute, kick, ban/unban va transfer qua menu that");
            t.broadcast();
            System.out.println("PASS Swing M5 broadcast tu form den hai Client UDP va lich su");
            t.baoCao();
            System.out.println("PASS Swing M5 JFileChooser, tai ZIP UDP, bao cao/nhat ky va nut mo");
        }
        System.out.println("KET QUA Swing M5: 5 PASS, 0 FAIL; su kien component, chua phai chuot/phim he dieu han");
    }

    /** Vai tro: Fixture co data/cong rieng va mot frame admin that. Ai goi: main. Goi: Core/SwingThu. */
    private static final class BoTest implements AutoCloseable {
        final Path data;
        final Nghe ngheA=new Nghe(),ngheB=new Nghe();
        final ClientCore ad=new ClientCore(new Nghe()),a=new ClientCore(ngheA),b=new ClientCore(ngheB);
        final ServerCore server;
        QuanTriFrame frame;
        Phong room;

        // Du lieu va multicast port rieng, khong cham bo thu tay dang mo.
        BoTest() throws Exception {
            Path thu=Files.createTempDirectory(Path.of(".selftest"),"m5-swing-");
            data=thu.resolve("data"); Files.createDirectory(data);
            System.setProperty("udpmultichat.clientConfig",thu.resolve("client.properties").toString());
            Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nspam.burst=10000\nspam.minute=10000\nmulticast.portBase=37000\n");
            server=new ServerCore(data,new ServerCore.Listener() {
                public void onLog(String text) { System.out.println("  Swing Server: "+text); }
                public void onThongKe(int u,int o,long t) { }
            });
        }

        // Login bang API trong fixture; cac thao tac admin sau do deu tu handler UI.
        void mo() throws Exception {
            server.batDau(0); int cong=server.cong();
            cho(a.dangKy("localhost",cong,"ui_a","abcdef123"));
            cho(b.dangKy("localhost",cong,"ui_b","abcdef123"));
            cho(ad.dangNhap("localhost",cong,"admin","adminTest123"));
            cho(a.dangNhap("localhost",cong,"ui_a","abcdef123"));
            cho(b.dangNhap("localhost",cong,"ui_b","abcdef123"));
            room=cho(a.taoPhong("Phòng Swing","Fixture tương tác",true,"",20));
            cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.id,""));
            frame=lay(() -> { QuanTriFrame f=new QuanTriFrame(ad,false); f.setVisible(true); f.nap(); return f; });
            Container users=tab(frame,1);
            doi(() -> lay(() -> tim(users,JTable.class).get(0).getRowCount()==3),"Khong tai duoc user vao JTable");
            for (int i=0;i<5;i++) { tab(frame,i); }
        }

        // Validator, default button va Esc phai noi vao form dang chay.
        void formVaCrud() throws Exception {
            Container users=tab(frame,1);
            bam(users,"Thêm"); esc(hopThoai("Thêm người dùng"));
            bam(users,"Thêm"); JDialog d=hopThoai("Thêm người dùng");
            dien(d,"Tên đăng nhập","x");
            trenEdt(() -> d.getRootPane().getDefaultButton().doClick());
            can(lay(() -> d.isShowing() && tim(d,JLabel.class).stream().anyMatch(l -> l.isVisible() && l.getText().startsWith("Tên cần"))),"Enter khong hien loi validator");
            dien(d,"Tên đăng nhập","ui_tam"); dien(d,"Tên hiển thị","Tạm tương tác");
            dien(d,"Mật khẩu tạm (buộc đổi lần đầu)","tam123456");
            trenEdt(() -> d.getRootPane().getDefaultButton().doClick());
            doi(() -> lay(() -> !d.isDisplayable()),"Tao user khong dong form sau ACK nghiep vu");
            chonDong(users,"ui_tam"); bam(users,"Sửa");
            JDialog sua=hopThoai("Sửa người dùng");
            dien(sua,"Tên hiển thị","Tên đã sửa"); dien(sua,"Giới thiệu","Giới thiệu kiểm thử");
            bam(sua,"Lưu"); doi(() -> lay(() -> !sua.isDisplayable()),"Sua user khong thanh cong");
            doi(() -> lay(() -> tim(users,JTable.class).get(0).getValueAt(0,1).equals("Tên đã sửa")),"JTable khong cap nhat ten moi");
        }

        // Nguoi dung dang online bi reset/khoa phai that su mat phien.
        void resetKhoaXoa() throws Exception {
            cho(b.dangXuat());
            can(cho(b.dangNhap("localhost",server.cong(),"ui_tam","tam123456")).buocDoiMatKhau,"Tao user tam khong force");
            cho(b.doiMatKhau("tam123456","moi123456")); cho(b.vaoSanh());
            Container users=tab(frame,1); chonDong(users,"ui_tam");
            menu(users,"Đặt lại mật khẩu"); bam(hopThoai("Đặt lại mật khẩu"),"Đặt lại mật khẩu");
            JDialog mk=hopThoai("Mật khẩu tạm");
            String tam=lay(() -> tim(mk,JTextField.class).get(0).getText());
            can(tam.length()==10,"Mat khau reset khong 10 ky tu");
            doi(() -> b.nguoiDung()==null,"Reset UI khong ngat phien");
            trenEdt(mk::dispose);
            can(cho(b.dangNhap("localhost",server.cong(),"ui_tam",tam)).buocDoiMatKhau,"Reset khong force doi");
            cho(b.doiMatKhau(tam,"moi456789")); cho(b.vaoSanh());
            khoa(users,true); doi(() -> b.nguoiDung()==null,"Khoa UI khong ngat phien");
            biChan(b.dangNhap("localhost",server.cong(),"ui_tam","moi456789"));
            khoa(users,false); cho(b.dangNhap("localhost",server.cong(),"ui_tam","moi456789"));
            menu(users,"Mute toàn hệ thống"); xacNhan("Cấm chat");
            doi(() -> b.quanTri().camChat("sanh")>0,"Mute toan he thong khong den Client");
            menu(users,"Gỡ cấm chat toàn hệ thống"); xacNhan("Gỡ cấm chat");
            doi(() -> b.quanTri().camChat("sanh")==0,"Khong go duoc mute toan he thong tu UI");
            menu(users,"Xóa"); JDialog xoa=hopThoai("Xóa tài khoản");
            dien(xoa,"Gõ lại tên đăng nhập","sai"); bam(xoa,"Xóa tài khoản");
            can(lay(xoa::isShowing),"Xoa sai login van dong form");
            dien(xoa,"Gõ lại tên đăng nhập","ui_tam"); bam(xoa,"Xóa tài khoản");
            doi(() -> b.nguoiDung()==null,"Xoa UI khong ngat phien");
            doi(() -> lay(() -> tim(users,JTable.class).get(0).getRowCount()==0),"UI van con user da xoa");
            chonDong(users,"admin"); menu(users,"Khóa/Mở khóa");
            JDialog tuKhoa=hopThoai("Khóa tài khoản"); bam(tuKhoa,"Khóa");
            doi(() -> lay(() -> tim(tuKhoa,JLabel.class).stream().anyMatch(l -> l.isVisible() && l.getText().contains("chính mình"))),"UI khong hien loi guard tu khoa");
            bam(tuKhoa,"Hủy");
            cho(b.dangNhap("localhost",server.cong(),"ui_b","abcdef123")); cho(b.vaoPhong(room.id,""));
        }

        // Cho JTable status moi roi moi bam toggle de khong dung snapshot cu.
        void khoa(Container users,boolean bat) throws Exception {
            String truoc=bat?"Online":"Bị khóa";
            doi(() -> lay(() -> tim(users,JTable.class).get(0).getValueAt(0,3).equals(truoc)),"Status user chua dong bo truoc toggle");
            menu(users,"Khóa/Mở khóa");
            JDialog d=hopThoai(bat?"Khóa tài khoản":"Mở khóa");
            dien(d,"Lý do","Kiểm thử Swing"); bam(d,bat?"Khóa":"Mở khóa");
            doi(() -> lay(() -> !d.isDisplayable()),"Toggle khoa khong dong form");
        }

        // Di qua menu/thoi han/confirm that va kiem tra ket qua tren Core thu hai.
        void kiemDuyet() throws Exception {
            Container rooms=tab(frame,2); chonDong(rooms,room.maPhong); menu(rooms,"Thành viên");
            JDialog ds=hopThoai("Thành viên và kiểm duyệt"); chonDong(ds,"ui_b");
            bam(ds,"Cấm chat"); xacNhan("Cấm chat");
            doi(() -> b.quanTri().camChat(room.id)>0,"Mute UI khong den Client");
            biChan(b.guiTin(room.id,"Không được gửi"));
            chonDong(ds,"ui_b"); menu(ds,"Gỡ cấm chat"); xacNhan("Gỡ cấm chat");
            doi(() -> b.quanTri().camChat(room.id)==0,"Unmute UI chua dong bo");
            cho(b.guiTin(room.id,"Đã gỡ mute qua UI"));
            chonDong(ds,"ui_b"); bam(ds,"Kick"); xacNhan("Kick");
            doi(() -> b.phongDaVao().stream().noneMatch(r -> r.id.equals(room.id)),"Kick UI khong dong kenh");
            cho(b.vaoPhong(room.maPhong,""));
            chonDong(ds,"ui_b"); menu(ds,"Cấm vào phòng"); xacNhan("Cấm vào phòng");
            doi(() -> b.phongDaVao().stream().noneMatch(r -> r.id.equals(room.id)),"Ban UI khong kick");
            biChan(b.vaoPhong(room.maPhong,""));
            chonDong(ds,"ui_b"); menu(ds,"Gỡ cấm vào"); xacNhan("Gỡ cấm vào");
            cho(b.vaoPhong(room.maPhong,""));
            chonDong(ds,"ui_b"); menu(ds,"Chuyển chủ phòng"); xacNhan("Chuyển chủ phòng");
            String id=b.nguoiDung().id;
            can(cho(ad.quanTri().lay()).lay("ROOMS").stream().anyMatch(r -> r.get(0).equals(room.id) && r.get(12).equals(id)),"Transfer UI khong luu chu");
            trenEdt(ds::dispose);
        }

        // Confirm moderation doi Server xac nhan moi dong form.
        void xacNhan(String ten) throws Exception {
            JDialog d=hopThoai(ten); bam(d,"Xác nhận");
            doi(() -> lay(() -> !d.isDisplayable()),"Khong hoan tat "+ten);
        }

        // Thong bao tao tu form that, khong goi thang API broadcast trong test.
        void broadcast() throws Exception {
            Container tong=tab(frame,0); bam(tong,"Gửi thông báo toàn hệ thống");
            JDialog d=hopThoai("Thông báo toàn hệ thống");
            dien(d,"Nội dung (tối đa 200 ký tự)","Thông báo Swing tự động"); bam(d,"Gửi thông báo");
            doi(() -> ngheA.thongBao.contains("Thông báo Swing tự động") && ngheB.thongBao.contains("Thông báo Swing tự động"),"Broadcast UI khong den hai Client");
            doi(() -> lay(() -> !d.isDisplayable()),"Broadcast UI khong dong form");
        }

        // Approve JFileChooser bang su kien Swing; khong mo viewer/clipboard cua OS.
        void baoCao() throws Exception {
            Container report=tab(frame,4);
            can(lay(() -> !nut(report,"Mở báo cáo HTML").isEnabled()),"Nut HTML bat truoc khi xuat");
            Path thu=data.getParent().resolve("bao-cao-ui"); Files.createDirectory(thu);
            chonThuMuc(report,"Xuất báo cáo",thu);
            doi(() -> lay(() -> nut(report,"Mở báo cáo HTML").isEnabled()),"UI khong hoan tat tai ZIP bao cao");
            can(coTep(thu,"tong_hop.html") && coTep(thu,"thanhvien_theophong.csv"),"Thieu HTML/CSV sau xuat UI");
            Container audit=tab(frame,3);
            Path nhatKy=data.getParent().resolve("nhat-ky-ui"); Files.createDirectory(nhatKy);
            chonThuMuc(audit,"Xuất nhật ký",nhatKy);
            doi(() -> coTep(nhatKy,"nhatky.csv"),"UI khong xuat nhat ky");
            can(lay(() -> nut(report,"Mở báo cáo HTML").isEnabled()),"Xuat audit lam vo hieu HTML bao cao truoc");
        }

        // Doc chi fixture vua tao, khong quet bao cao/du lieu nguoi dung.
        boolean coTep(Path thu,String ten) throws Exception {
            try (var ds=Files.walk(thu)) { return ds.anyMatch(p -> p.getFileName().toString().equals(ten)); }
        }

        // Chi dong cua so trong JVM test, giu nguyen bo thu tay o JVM khac.
        @Override public void close() {
            try { trenEdt(() -> { for (Window w:Window.getWindows()) { w.dispose(); } }); }
            catch (Exception e) { System.err.println("Lỗi dọn cửa sổ test: "+e.getMessage()); }
            ad.close(); a.close(); b.close(); server.close();
        }
    }

    /** Vai tro: Nhan thong bao qua UDP that, khong Swing. Ai goi: ClientCore. Goi: collection thread-safe. */
    private static final class Nghe implements Listener {
        final Set<String> thongBao=ConcurrentHashMap.newKeySet();
        public void onThongBaoQuanTri(String text) { thongBao.add(text); }
        public void onTinNhan(TinNhan t,boolean toi) { }
        public void onHeThong(String text) { }
        public void onDanhSachOnline(List<String> ds) { }
        public void onLoi(String text) { System.err.println("Swing Client: "+text); }
        public void onMatKetNoi(String text) { }
    }
}
