package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.ServerCore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: hoi quy thu tu danh tinh. Ai goi: SelfTest. Goi: ServerCore, ba ClientCore that. */
public final class DanhTinhSelfTest {
    // ===== VAI TRO VA CAC THAO TAC =====
    // Khoi tao lop va noi cac thanh phan can thiet.
    private DanhTinhSelfTest() { }
    // Chay rieng de tai hien loi truoc khi sua, khong tao Swing.
    public static void main(String[] args) throws Exception {
        MangUtil.dungIPv4(); Files.createDirectories(Path.of(".selftest"));
        System.setProperty("udpmultichat.clientConfig",Files.createTempDirectory(Path.of(".selftest"),"prefs-id-").resolve("client.properties").toString());
        kiemTra(); kiemTraGoi();
    }
    // Dang nhap sau cung phai biet danh tinh truoc ban danh sach dau tien.
    static void kiemTra() throws Exception {
        Path data=Files.createTempDirectory(Path.of(".selftest"),"m26-identity-");
        Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nspam.burst=10000\nspam.minute=10000\nmulticast.portBase=25000\n");
        BanNghe an=new BanNghe(),binh=new BanNghe(),quanTri=new BanNghe();
        ServerCore server=new ServerCore(data,new ServerCore.Listener() {
            // Nhan log qua Listener, chuyen ve luong phu hop.
            public void onLog(String s) { System.out.println("  M2.6: "+s); }
            // Nhan snapshot thong ke tu ServerCore.
            public void onThongKe(int u,int o,long t) { }
        });
        try (ClientCore a=new ClientCore(an); ClientCore b=new ClientCore(binh); ClientCore ad=new ClientCore(quanTri)) {
            server.batDau(0); int cong=server.cong();
            cho(a.dangKy("localhost",cong,"aaa","abcdef123")); cho(b.dangKy("localhost",cong,"bbb","abcdef123"));
            quanTri.ganSauFuture(cho(ad.dangNhap("localhost",cong,"admin","adminTest123")));
            an.ganSauFuture(cho(a.dangNhap("localhost",cong,"aaa","abcdef123")));
            binh.ganSauFuture(cho(b.dangNhap("localhost",cong,"bbb","abcdef123")));
            doi(() -> binh.thanhVien.size()==3,4000);
            can(!binh.suKienTruocDanhTinh,"TAI HIEN: danh sach thanh vien den truoc khi UI biet minh la ai");
            can(binh.soDongBanLucNhan==1,"Client vao cuoi khong co dung mot dong Ban ngay lan ve dau");
            cho(b.guiTin("111")); doi(() -> binh.tinCuaMinh.contains("111") && an.tinNguoiKhac.contains("111"),4000);
            String tinDai="ắ".repeat(333)+"a"; cho(b.guiTin(tinDai));
            doi(() -> binh.tinCuaMinh.contains(tinDai) && an.tinNguoiKhac.contains(tinDai),4000);
            Phong p=cho(b.taoPhong("Phòng của B","",true,"",3)); cho(b.vaoPhong(p.id,""));
            can(p.chuId.equals(b.nguoiDung().id),"Chu phong khong khop danh tinh B");
            doi(() -> b.danhSachPhong().stream().anyMatch(r -> r.id.equals(p.id) && r.daThamGia),3000);
            doi(() -> a.danhSachPhong().stream().anyMatch(r -> r.id.equals(p.id) && !r.daThamGia),3000);
            biChan(a.suaPhong(p.id,"Đổi trái phép","",true,null,3)); biChan(a.xoaPhong(p.id));
            cho(b.suaPhong(p.id,"B sửa được","",true,null,3));
            cho(b.dangXuat()); cho(a.dangXuat()); cho(ad.dangXuat());
        } finally { server.close(); }
    }
    // Tin 1000 byte duoc chap nhan ca trong goi RESYNC boc UDP tin cay.
    static void kiemTraGoi() {
        String id=com.mycompany.udpmultichat.common.BaoMat.ngauNhien(12),user=com.mycompany.udpmultichat.common.BaoMat.ngauNhien(12);
        String text="ắ".repeat(333)+"a";
        TinNhan tin=new TinNhan(Long.MAX_VALUE,id,user,new String(Character.toChars(0x1F642)).repeat(30),text,Long.MAX_VALUE);
        com.mycompany.udpmultichat.common.Goi.kiemTraTin(tin);
        String sync=com.mycompany.udpmultichat.common.Goi.ghep(com.mycompany.udpmultichat.common.Goi.noi(
            new String[]{Protocol.MUC_DONG_BO,id,"x".repeat(36)},com.mycompany.udpmultichat.common.Goi.tach(tin.dongGoi())));
        int bytes=com.mycompany.udpmultichat.common.Goi.thanhByte(com.mycompany.udpmultichat.common.Goi.ghep("R","x".repeat(80),sync)).length;
        System.out.println("  1000 byte noi dung + header RESYNC/R: "+bytes+" byte");
        can(bytes<1400,"Goi 1000 byte khong vua MTU");
        try { com.mycompany.udpmultichat.common.Goi.kiemTraTin(new TinNhan(1,id,user,"aaa","|".repeat(1000),1)); throw new AssertionError("Escape vuot MTU van duoc nhan"); }
        catch (IllegalArgumentException dung) { }
        var ds=List.of(new Phong.ThanhVien("a","aaa","Trùng tên","THANH_VIEN",true),new Phong.ThanhVien("b","bbb","Trùng tên","THANH_VIEN",true));
        can(ds.stream().filter(n -> n.laToi("BBB")).count()==1,"Trung ten hien thi lam sai nhan Ban");
    }
    /** Vai tro: mo phong UI gan ten qua Future cu. Ai goi: ClientCore. Goi: khong. */
    private static final class BanNghe implements Listener {
        volatile NguoiDung toi;
        volatile boolean suKienTruocDanhTinh;
        volatile long soDongBanLucNhan;
        volatile List<Phong.ThanhVien> thanhVien=List.of();
        final List<String> tinCuaMinh=new CopyOnWriteArrayList<>(),tinNguoiKhac=new CopyOnWriteArrayList<>();
        // Ban sua phai phat su kien nay truoc khi vao nhom.
        public void onNguoiDung(NguoiDung n) { toi=n; }
        // Tai hien cach UI cu gan danh tinh sau khi READY/HISTORY da xong.
        void ganSauFuture(NguoiDung n) { toi=n; }
        // Ghi lai ket qua ngay thoi diem nhan, khong tinh lai de che loi thu tu.
        public void onThanhVienChiTiet(String id,List<Phong.ThanhVien> ds) {
            if (!Protocol.SANH_ID.equals(id)) { return; }
            if (toi==null) { suKienTruocDanhTinh=true; }
            soDongBanLucNhan=ds.stream().filter(n -> toi!=null && n.laToi(toi.tenDangNhap)).count(); thanhVien=List.copyOf(ds);
        }
        // Kiem tra bong ben phai cua minh va ben trai cua nguoi khac.
        public void onTinNhan(TinNhan t,boolean laToi) { (laToi?tinCuaMinh:tinNguoiKhac).add(t.noiDung); }
        // Nhan thong bao he thong qua Listener.
        public void onHeThong(String s) { }
        // Nhan danh sach online theo Listener.
        public void onDanhSachOnline(List<String> ds) { }
        // Nhan loi de hien thi hoac ghi nhan.
        public void onLoi(String s) { System.err.println(s); }
        // Nhan trang thai phien da ngat.
        public void onMatKetNoi(String s) { }
    }
}
