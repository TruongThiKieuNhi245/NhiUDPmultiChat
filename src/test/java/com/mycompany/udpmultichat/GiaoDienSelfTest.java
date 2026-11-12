package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.KhoLuuTru;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung;
import com.mycompany.udpmultichat.server.QuanLyPhong;
import com.mycompany.udpmultichat.server.ServerCore;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Hoi quy M2.5: validate hai phia, loai phong, thanh vien offline, reconnect va RESYNC sau restart. Ai goi: SelfTest/main. Goi: ServerCore, ClientCore va cac ham kiem thu. */
final class GiaoDienSelfTest {
    // ===== VAI TRO VA CAC THAO TAC =====
    // Khoi tao lop va noi cac thanh phan can thiet.
    private GiaoDienSelfTest() { }
    // Chay tat ca.
    static void chayTatCa() throws Exception {
        chay("M2.5 Server: 3-30 ky tu, 2-500 nguoi, byte UTF-8 va loai phong luu dung",GiaoDienSelfTest::testServer);
        chay("M2.5 Client: HELLO, qua 1000 byte bi chan, offline va tu khoi phuc/RESYNC",GiaoDienSelfTest::testKetNoiLai);
    }
    // Kiem tra quy tac Server doc lap voi Swing.
    private static void testServer() throws Exception {
        Path data=Files.createTempDirectory(Path.of(".selftest"),"m25-manager-");
        KhoLuuTru kho=new KhoLuuTru(data,s -> { }); QuanLyNguoiDung user=new QuanLyNguoiDung(kho);
        var userA=user.dangKy("chu_a","abcdef123"); var userB=user.dangKy("ban_b","abcdef456");
        var a=user.dangNhap("chu_a","abcdef123",new InetSocketAddress("127.0.0.1",31001));
        var b=user.dangNhap("ban_b","abcdef456",new InetSocketAddress("127.0.0.1",31002));
        QuanLyPhong q=new QuanLyPhong(kho,user::danhSach);
        chan(() -> q.tao(a,"ab","",false,"",2)); chan(() -> q.tao(a,"x".repeat(31),"",true,"",2));
        chan(() -> q.tao(a,"Phòng","",false,"",1)); chan(() -> q.tao(a,"Phòng","",true,"",501));
        Phong p=q.tao(a,"Phòng riêng","Kiểm tra | UTF-8",false,"matkhau",2);
        can(!p.congKhai,"CREATE mat loai rieng tu");
        q.xacNhanVao(a,q.datCho(a,p.id,"").id);
        can(q.danhSachCho(b).stream().noneMatch(r -> r.id.equals(p.id)),"Phong rieng lo cho nguoi ngoai");
        chan(() -> q.datCho(b,p.maPhong,"sai")); q.xacNhanVao(b,q.datCho(b,p.maPhong,"matkhau").id);
        chan(() -> q.roiChuDong(a,p.id)); q.chuyenChu(a,p.id,userB.id);
        q.roiChuDong(a,p.id); can(q.lay(p.id).chuId.equals(userB.id),"Khong chuyen chu");
        String dai="ắ".repeat(334); can(dai.length()<1000 && dai.getBytes(StandardCharsets.UTF_8).length>1000,"Du lieu test byte sai");
        chan(() -> q.themTin(b,p.id,dai)); can(q.mocTin(p.id)==0,"Tin qua dai van duoc luu");
        q.themTin(b,p.id,"ắ".repeat(333)+"a"); can(q.mocTin(p.id)==1,"Tin dung 1000 byte bi chan");
        QuanLyPhong nap=new QuanLyPhong(kho,user::danhSach);
        can(!nap.lay(p.id).congKhai && nap.lay(p.id).chuId.equals(userB.id),"Khoi dong lai mat loai/quyen");
        can(nap.laThanhVien(userB.id,p.id) && !nap.laThanhVien(userA.id,p.id),"Tu cach thanh vien khong luu dung");
        can(nap.danhSachCho(a).stream().noneMatch(r -> r.id.equals(p.id)),"Chu cu roi roi van thay phong rieng");
    }
    // Kiem tra ngat/ket noi lai va du lieu duoc gui bu.
    private static void testKetNoiLai() throws Exception {
        Path data=Files.createTempDirectory(Path.of(".selftest"),"m25-reconnect-");
        Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nmulticast.portBase=23000\n",StandardCharsets.UTF_8);
        BanNghe nghe=new BanNghe(),ngheB=new BanNghe(); AtomicBoolean boMulticast=new AtomicBoolean();
        ServerCore server=new ServerCore(data,new ServerCore.Listener() {
            // Nhan log qua Listener, chuyen ve luong phu hop.
            public void onLog(String s) { System.out.println("  M2.5: "+s); }
            // Nhan snapshot thong ke tu ServerCore.
            public void onThongKe(int u,int o,long t) { }
        });
        try (ClientCore a=new ClientCore(nghe,true,t -> !boMulticast.get()); ClientCore b=new ClientCore(ngheB)) {
            server.batDau(0); int cong=server.cong(); cho(a.kiemTraServer("127.0.0.1",cong));
            cho(a.dangKy("127.0.0.1",cong,"ketnoi_a","abcdef123"));
            cho(a.dangNhap("127.0.0.1",cong,"ketnoi_a","abcdef123"));
            cho(b.dangNhap("127.0.0.1",cong,"admin","adminTest123"));
            Phong p=cho(a.taoPhong("Khôi phục","",false,"matkhau",2)); cho(a.vaoPhong(p.id,"")); cho(b.vaoPhong(p.id,""));
            cho(a.guiTin(p.id,"Tin trước restart")); doi(() -> nghe.tin.stream().filter(t -> t.roomId.equals(p.id)).count()==1,3000);
            biChan(a.guiTin(p.id,"ắ".repeat(334)));
            cho(b.dangXuat());
            doi(() -> nghe.thanhVien.stream().anyMatch(n -> n.ten().equals("Admin") && !n.online()),3000);
            server.close(); boMulticast.set(true);
            doi(() -> !a.sanSang(),210000); biChan(a.guiTin(p.id,"không được gửi lúc offline"));
            server.batDau(cong); cho(b.dangNhap("127.0.0.1",cong,"admin","adminTest123")); cho(b.vaoPhong(p.id,""));
            for (int i=2;i<=76;i++) { cho(b.guiTin(p.id,"Tin khi A mất mạng "+i)); }
            doi(() -> a.sanSang(),20000);
            doi(() -> nghe.tin.stream().filter(t -> t.roomId.equals(p.id)).count()==76,18000);
            boMulticast.set(false);
            var ds=nghe.tin.stream().filter(t -> t.roomId.equals(p.id)).toList();
            for (int i=0;i<ds.size();i++) { can(ds.get(i).msgId==i+1,"RESYNC reconnect bi trung/thieu"); }
            can(nghe.daKhoiPhuc,"Khong phat su kien khoi phuc ket noi");
            cho(a.dangXuat()); cho(b.dangXuat());
            can(a.nguoiDung()==null,"Dang xuat khong don client");
            String prefs=Files.readString(com.mycompany.udpmultichat.client.CauHinhClient.tepCauHinh(),StandardCharsets.UTF_8);
            can(!prefs.contains("abcdef123") && !prefs.contains("adminTest123") && !prefs.contains("token"),"Prefs luu thong tin bi mat");
        } finally { server.close(); }
    }
    // Chan.
    private static void chan(ViecTest viec) throws Exception {
        try { viec.chay(); throw new AssertionError("Server phai tu choi"); } catch (IllegalArgumentException dung) { }
    }
    /** Thu tin va danh sach chi tiet de so sanh sau ket noi lai. */
    private static final class BanNghe implements Listener {
        final List<TinNhan> tin=new CopyOnWriteArrayList<>();
        volatile List<Phong.ThanhVien> thanhVien=List.of(); volatile boolean daKhoiPhuc;
        // Nhan tin da duoc kiem tra danh tinh va phong.
        public void onTinNhan(TinNhan t,boolean toi) { tin.add(t); }
        // On thanh vien chi tiet.
        public void onThanhVienChiTiet(String id,List<Phong.ThanhVien> ds) { thanhVien=List.copyOf(ds); }
        // On ket noi.
        public void onKetNoi(boolean ok,String s) { if (ok) { daKhoiPhuc=true; } }
        // Nhan thong bao he thong qua Listener.
        public void onHeThong(String s) { }
        // Nhan danh sach online theo Listener.
        public void onDanhSachOnline(List<String> ds) { }
        // Nhan loi de hien thi hoac ghi nhan.
        public void onLoi(String s) { System.err.println("  M2.5 CLIENT: "+s); }
        // Nhan trang thai phien da ngat.
        public void onMatKetNoi(String s) { }
    }
}
