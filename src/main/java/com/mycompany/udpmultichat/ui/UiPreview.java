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

/** Xuat PNG tu COMPONENT THAT, khong Server/socket; du lieu mau chi ton tai trong RAM. */
public final class UiPreview {
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
    private static void dungVaXuat() throws IOException {
        DangNhapPanel login=new DangNhapPanel(t -> { },ip -> { });
        login.xemTruocDangNhap();
        xuat(login,"01-dang-nhap.png",1120,800);
        login.xemTruocDangKy(); xuat(login,"02-dang-ky.png",1120,800);
        login.loiServer("Tên đăng nhập đã tồn tại"); xuat(login,"03-loi-form.png",1120,800);
        ChatMulticastUI frame=new ChatMulticastUI();
        try {
            NguoiDung n=new NguoiDung("nhi","nhikieu","Kiều Nhi",VaiTro.ADMIN,"2026-11-12");
            Phong sanh=new Phong("sanh","Sảnh chung","Kênh chung của lớp","SANH01","HE_THONG","239.255.42.1",9001,500,true,false,true,3,"2026-11-12");
            Phong nhom=new Phong("demo","Bài tập mạng","Nhóm chuẩn bị demo UDP","NET246","nhi","239.255.42.2",9002,20,false,true,true,2,"2026-11-12");
            frame.duLieuMau(n,List.of(sanh,nhom));
            frame.onThanhVienChiTiet(sanh.id,List.of(new Phong.ThanhVien("nhi","Kiều Nhi","ADMIN",true),
                new Phong.ThanhVien("an","Minh An","THANH_VIEN",true),new Phong.ThanhVien("ha","Thu Hà","THANH_VIEN",false)));
            frame.onThanhVienChiTiet(nhom.id,List.of(new Phong.ThanhVien("nhi","Kiều Nhi","ADMIN",true),new Phong.ThanhVien("an","Minh An","THANH_VIEN",true)));
            frame.onHeThong(sanh.id,"Minh An đã vào Sảnh chung.");
            long gio=java.time.Instant.parse("2026-11-12T05:30:00Z").toEpochMilli();
            frame.onTinNhan(new TinNhan(1,sanh.id,"an","Minh An","Chào Nhi, mình đã kiểm tra chat ở hai phòng.\nMọi thứ sẵn sàng cho buổi demo.",gio-120000),false,false);
            frame.onTinNhan(new TinNhan(2,sanh.id,"nhi","Kiều Nhi","Cảm ơn An! Nhớ thử mã phòng và trường hợp mất kết nối nữa nhé.",gio-60000),true,false);
            frame.onTinNhan(new TinNhan(3,sanh.id,"an","Minh An","Mình sẽ chạy đủ 3 Client để kiểm tra.",gio),false,false);
            frame.onTinNhan(new TinNhan(1,nhom.id,"an","Minh An","Mình đã gửi kịch bản demo vào nhóm.",gio),false,false);
            xuat(frame.getRootPane(),"04-chat.png",1240,800);
            frame.onKetNoi(false,"Mất kết nối, đang thử kết nối lại…"); xuat(frame.getRootPane(),"05-mat-ket-noi.png",1240,800);
            frame.onKetNoi(true,"Đã kết nối lại và đồng bộ tin nhắn.");
            frame.toast(ThongBao.Muc.THANH_CONG,"Đã tạo phòng «Bài tập mạng».");
            frame.toast(ThongBao.Muc.THONG_TIN,"Mã phòng đã được sao chép.");
            frame.toast(ThongBao.Muc.CANH_BAO,"Phòng đang đóng. Bạn vẫn xem được lịch sử.");
            xuat(frame.getRootPane(),"06-toast-ba-muc.png",1240,800);
            frame.toast(ThongBao.Muc.LOI,"Phòng đã đầy. Hãy liên hệ chủ phòng.");
            xuat(frame.getRootPane(),"07-toast-loi.png",1240,800);
            xuat(frame.menuAvatar(),"08-menu-nguoi-dung.png",300,190);
            HopThoai tao=new HopThoai(null,false); xuat(tao,"09-tao-phong.png",470,560);
            xuat(new HopThoai(nhom,false),"10-sua-phong.png",470,640);
            HopThoai vao=new HopThoai(null,true); vao.coMatKhau(); xuat(vao,"11-vao-phong.png",470,300);
        } finally { frame.dispose(); }
        SoanTinPanel soan=new SoanTinPanel(text -> { }); soan.xemTruoc("ắ".repeat(201));
        xuat(soan,"13-tin-qua-dai.png",760,220);
        DangNhapPanel settings=new DangNhapPanel(t -> { },ip -> { });
        settings.xemTruocDangNhap(); settings.xemTruocServer();
        xuat(settings,"14-cai-dat-server.png",1120,800);
        ServerMain server=new ServerMain();
        try { xuat(server.getRootPane(),"12-server.png",820,520); } finally { server.dispose(); }
    }
    // Ve ca cay component len BufferedImage; khong chup desktop hoac gui du lieu ra ngoai.
    private static void xuat(JComponent c,String ten,int rong,int cao) throws IOException {
        c.setSize(rong,cao); boCuc(c);
        BufferedImage anh=new BufferedImage(rong,cao,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=anh.createGraphics(); g.setColor(Theme.NEN); g.fillRect(0,0,rong,cao); c.printAll(g); g.dispose();
        ImageIO.write(anh,"png",THU_MUC.resolve(ten).toFile()); soAnh++;
        if (anh.getWidth()!=rong || anh.getHeight()!=cao) { throw new IOException("Sai kích thước PNG"); }
    }
    private static void boCuc(Container c) { c.doLayout(); for (var con:c.getComponents()) { if (con instanceof Container n) { boCuc(n); } } }
}
