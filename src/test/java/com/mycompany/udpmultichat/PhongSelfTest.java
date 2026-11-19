package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.KenhPhong;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.ServerCore;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Kiem thu M2: CRUD, bao ve phong, nhieu nhom, lich su va RESYNC nhieu lo. Ai goi: SelfTest/main. Goi: ServerCore, ClientCore va cac ham kiem thu. */
final class PhongSelfTest {
    // Khoi tao lop va noi cac thanh phan can thiet.
    private PhongSelfTest() { }

    // Moi ham test ngan dung chung du lieu phien, khong lap code khoi tao.
    static void chayTatCa() throws Exception {
        chay("Cung cong: socket nhom B bo qua roomId A",PhongSelfTest::testCungCong);
        try (PhienThu t=new PhienThu()) {
            t.dangNhap();
            chay("Tao phong: ma 6 ky tu, nhom/cong rieng, danh sach realtime",t::taoPhong);
            chay("Vao bang danh sach/ma, mat khau, gioi han nguoi va tieng Viet UI",t::vaoPhong);
            chay("Cach ly: tin A khong sang B, tin B khong sang A tren cung may",t::cachLy);
            chay("RESYNC: mat tin giua va tin CUOI, van du thu tu va khong trung",t::resync);
            chay("RESYNC nhieu lo: bo 70 tin lien tiep, tu bu qua heartbeat",t::resyncNhieuLo);
            chay("Lich su: nguoi vao sau nhan dung 50 tin gan nhat",t::lichSu);
            chay("Sua phong/so nguoi realtime va chan nguoi khong phai chu/admin",t::suaPhong);
            chay("Dong/mo: chan gui va vao moi; admin mo lai phong cu",t::dongMo);
            chay("Vao phong dang co tin moi: buffer truoc READY khong lam loi vao phong",t::vaoDongThoi);
            chay("Sanh chung luon ton tai, khong xoa/doi loai/dong",t::baoVeSanh);
            chay("Xoa phong dang co thanh vien: dong tab/kenh va cap nhat realtime",t::xoaPhong);
            chay("Khoi dong lai: phong/ma/trang thai/lich su con, phong da xoa khong tro lai",t::khoiDongLai);
        }
    }

    /** Vai tro: du lieu chung M2. Ai goi: chayTatCa. Goi: ServerCore va ba ClientCore. */
    private static final class PhienThu implements AutoCloseable {
        final AtomicInteger resync=new AtomicInteger(),daBo=new AtomicInteger();
        final AtomicBoolean boHet=new AtomicBoolean();
        final BanNghe na=new BanNghe(),nb=new BanNghe(),nc=new BanNghe();
        final AtomicReference<Phong> ra=new AtomicReference<>(),rb=new AtomicReference<>();
        final ClientCore a=new ClientCore(na),b=new ClientCore(nb,true,this::giuTin),c=new ClientCore(nc);
        final Path data;
        final ServerCore server;
        // Cau hinh multicast rieng va ghi nhan so lo gui bu.
        PhienThu() throws Exception {
            data=Files.createTempDirectory(Path.of(".selftest"),"m2-");
            Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nspam.burst=10000\nspam.minute=10000\nmulticast.portBase=21000\n",StandardCharsets.UTF_8);
            server=new ServerCore(data,new ServerCore.Listener() {
                // Nhan log qua Listener, chuyen ve luong phu hop.
                public void onLog(String s) { if (s.startsWith("RESYNC")) { resync.incrementAndGet(); } System.out.println("  M2: "+s); }
                // Nhan snapshot thong ke tu ServerCore.
                public void onThongKe(int tong,int online,long tin) { }
            });
        }
        // Bo chu dong mot so multicast de buoc Client xin RESYNC.
        private boolean giuTin(TinNhan tin) {
            boolean bo=boHet.get() || tin.msgId==2 || tin.msgId==7 || tin.msgId==55;
            if (bo) { daBo.incrementAndGet(); } return !bo;
        }
        // Dang nhap chu phong, admin va thanh vien theo thu tu.
        void dangNhap() throws Exception {
            server.batDau(0); int cong=server.cong();
            cho(a.dangKy("127.0.0.1",cong,"chu_phong","abcdef123")); cho(c.dangKy("127.0.0.1",cong,"thanh_vien","abcdef456"));
            cho(a.dangNhap("127.0.0.1",cong,"chu_phong","abcdef123")); cho(b.dangNhap("127.0.0.1",cong,"admin","adminTest123"));
            cho(c.dangNhap("127.0.0.1",cong,"thanh_vien","abcdef456"));
        }
        // Kiem tra taoPhong, ten PASS/FAIL duoc dat o ham dieu phoi.
        void taoPhong() throws Exception {
            ra.set(cho(a.taoPhong("Nhóm A","Học UDP | multicast",true,"",3)));
            rb.set(cho(a.taoPhong("Bí mật B","Phòng riêng tư",false,"phong123",2)));
            Phong x=ra.get(),y=rb.get();
            can(x.maPhong.matches("[A-Z0-9]{6}") && !x.maPhong.equals(y.maPhong),"Ma phong khong duy nhat");
            can(!x.nhom.equals(y.nhom) && x.cong!=y.cong,"Tai su dung nhom/cong giua hai phong");
            doi(() -> coPhong(nb,x.id) && coPhong(nc,x.id) && coPhong(nb,y.id),3000);
            can(!coPhong(nc,y.id),"Nguoi ngoai thay phong rieng tu");
            String csv=Files.readString(data.resolve("rooms.csv"),StandardCharsets.UTF_8);
            can(!csv.contains("phong123"),"Luu mat khau phong dang tho");
        }
        // Kiem tra vaoPhong, ten PASS/FAIL duoc dat o ham dieu phoi.
        void vaoPhong() throws Exception {
            Phong x=ra.get(),y=rb.get();
            cho(a.vaoPhong(x.id,"")); cho(c.vaoPhong(x.maPhong,"")); cho(b.vaoPhong(x.id,""));
            cho(a.vaoPhong(y.id,"")); cho(b.vaoPhong(y.id,""));
            tuChoi(c.vaoPhong(y.maPhong,"sai"),"Mật khẩu");
            tuChoi(c.vaoPhong(y.maPhong,"phong123"),"đầy");
            tuChoi(a.roiPhong(y.id),"Chủ phòng");
            cho(a.chuyenChu(y.id,b.nguoiDung().id)); cho(a.roiPhong(y.id));
            cho(c.vaoPhong(y.maPhong,"phong123"));
            cho(b.chuyenChu(y.id,c.nguoiDung().id)); cho(b.roiPhong(y.id));
            doi(() -> na.heThong.stream().anyMatch(s -> s.contains("đã vào")),2000);
            doi(() -> nc.thanhVien.getOrDefault(y.id,List.of()).size()==1,2000);
            doi(() -> coPhong(nc,y.id),3000);
            can(!tim(nc,y.id).congKhai && !tim(nb,y.id).congKhai,"Phong rieng bi doi thanh cong khai");
            tuChoi(a.xoaPhong(y.id),"chủ phòng");
        }
        // Kiem tra cachLy, ten PASS/FAIL duoc dat o ham dieu phoi.
        void cachLy() throws Exception {
            Phong x=ra.get(),y=rb.get(); cho(c.roiPhong(x.id));
            cho(a.guiTin(x.id,"Xin chào | nhóm A\nDòng thứ hai \\ UDP"));
            doi(() -> na.tin(x.id).size()==1 && nb.tin(x.id).size()==1,4000);
            cho(c.guiTin(y.id,"Chỉ phòng B nhìn thấy"));
            doi(() -> nc.tin(y.id).size()==1,3000);
            Thread.sleep(500);
            can(nc.tin(x.id).isEmpty(),"Tin A lot sang Client chi o B");
            can(na.tin(y.id).isEmpty() && nb.tin(y.id).isEmpty(),"Tin B lot sang Client chi o A");
            tuChoi(c.guiTin(x.id,"khong co quyen"),"chưa vào");
        }
        // Kiem tra resync, ten PASS/FAIL duoc dat o ham dieu phoi.
        void resync() throws Exception {
            Phong x=ra.get();
            for (int i=2;i<=55;i++) { cho(a.guiTin(x.id,"Tin số "+i)); }
            doi(() -> nb.tin(x.id).size()==55,12000);
            can(daBo.get()>=3 && resync.get()>0,"Chua mo phong mat goi/gui bu");
            kiemTraThuTu(nb.tin(x.id),1,55);
            can(nb.tin(x.id).get(0).noiDung.contains("Xin chào | nhóm A"),"UTF-8 hoac escape bi hong");
        }
        // Kiem tra resyncNhieuLo, ten PASS/FAIL duoc dat o ham dieu phoi.
        void resyncNhieuLo() throws Exception {
            Phong x=ra.get(); int soLoCu=resync.get(); boHet.set(true);
            try { for (int i=56;i<=125;i++) { cho(a.guiTin(x.id,"Tin số "+i)); } }
            finally { boHet.set(false); }
            doi(() -> nb.tin(x.id).size()==125,16000);
            kiemTraThuTu(nb.tin(x.id),1,125);
            can(resync.get()>=soLoCu+2,"Khong chia it nhat hai lo RESYNC");
        }
        // Kiem tra lichSu, ten PASS/FAIL duoc dat o ham dieu phoi.
        void lichSu() throws Exception {
            Phong x=ra.get(); cho(c.vaoPhong(x.maPhong,""));
            List<TinNhan> ds=nc.tin(x.id); kiemTraThuTu(ds,76,125);
            can(nc.lichSu.getOrDefault(x.id,new CopyOnWriteArrayList<>()).size()==50,"Tin lich su bi tinh thanh tin truc tiep");
            doi(() -> na.thanhVien.getOrDefault(x.id,List.of()).size()==3,3000);
        }
        // Kiem tra suaPhong, ten PASS/FAIL duoc dat o ham dieu phoi.
        void suaPhong() throws Exception {
            Phong x=ra.get();
            tuChoi(c.suaPhong(x.id,"Đổi lén","",true,null,3),"chủ phòng");
            tuChoi(c.xoaPhong(x.id),"chủ phòng"); tuChoi(c.dongMoPhong(x.id,false),"chủ phòng");
            tuChoi(a.suaPhong(x.id,x.ten,x.moTa,true,null,2),"Giới hạn");
            cho(a.suaPhong(x.id,"Nhóm A đã sửa","Mô tả, có \"dấu nháy\" | UTF-8",true,null,3));
            doi(() -> tim(nb,x.id).ten.equals("Nhóm A đã sửa") && tim(nc,x.id).soThanhVien==3,3000);
        }
        // Kiem tra dongMo, ten PASS/FAIL duoc dat o ham dieu phoi.
        void dongMo() throws Exception {
            Phong x=ra.get(); cho(a.dongMoPhong(x.id,false));
            doi(() -> !tim(nc,x.id).dangMo,3000);
            tuChoi(a.guiTin(x.id,"khong gui khi dong"),"đóng");
            cho(c.roiPhong(x.id)); tuChoi(c.vaoPhong(x.maPhong,""),"đóng");
            cho(b.dongMoPhong(x.id,true)); cho(c.vaoPhong(x.maPhong,""));
            doi(() -> tim(nc,x.id).dangMo,3000);
        }
        // Kiem tra vaoDongThoi, ten PASS/FAIL duoc dat o ham dieu phoi.
        void vaoDongThoi() throws Exception {
            Phong nhanh=cho(a.taoPhong("Vào đồng thời","Kiểm tra tin đến lúc join",true,"",3));
            cho(a.vaoPhong(nhanh.id,""));
            CompletableFuture<Phong> vao=c.vaoPhong(nhanh.id,"");
            for (int i=1;i<=10;i++) { cho(a.guiTin(nhanh.id,"Tin đang vào "+i)); }
            cho(vao); doi(() -> nc.tin(nhanh.id).size()==10,6000);
            kiemTraThuTu(nc.tin(nhanh.id),1,10); cho(a.xoaPhong(nhanh.id));
        }
        // Kiem tra baoVeSanh, ten PASS/FAIL duoc dat o ham dieu phoi.
        void baoVeSanh() throws Exception {
            tuChoi(b.xoaPhong(Protocol.SANH_ID),"Sảnh chung");
            tuChoi(b.dongMoPhong(Protocol.SANH_ID,false),"Sảnh chung");
            tuChoi(b.suaPhong(Protocol.SANH_ID,"Sảnh chung","",false,null,1000),"Sảnh chung");
            can(tim(na,Protocol.SANH_ID).congKhai,"Sanh bi doi loai");
        }
        // Kiem tra xoaPhong, ten PASS/FAIL duoc dat o ham dieu phoi.
        void xoaPhong() throws Exception {
            Phong y=rb.get(); cho(c.xoaPhong(y.id));
            doi(() -> nc.daRoi.contains(y.id) && !coPhong(nb,y.id) && !coPhong(nc,y.id),3000);
            can(c.phongDaVao().stream().noneMatch(r -> r.id.equals(y.id)),"Client chua dong kenh phong bi xoa");
            tuChoi(c.vaoPhong(y.maPhong,"phong123"),"Không tìm thấy");
        }
        // Kiem tra khoiDongLai, ten PASS/FAIL duoc dat o ham dieu phoi.
        void khoiDongLai() throws Exception {
            Phong x=ra.get(),y=rb.get(); cho(a.dongMoPhong(x.id,false));
            cho(a.dangXuat()); cho(b.dangXuat()); cho(c.dangXuat()); server.close(); server.batDau(0);
            cho(a.dangNhap("127.0.0.1",server.cong(),"chu_phong","abcdef123"));
            doi(() -> coPhong(na,x.id),3000); Phong nap=tim(na,x.id);
            can(nap.maPhong.equals(x.maPhong) && !nap.dangMo && nap.soThanhVien==0,"Mat ma/trang thai hoac giu thanh vien cu");
            can(nap.ten.equals("Nhóm A đã sửa") && nap.moTa.contains("\"dấu nháy\""),"CSV/UTF-8 khong nap dung");
            can(!coPhong(na,y.id),"Phong da xoa xuat hien lai");
            cho(a.dongMoPhong(x.id,true)); cho(a.vaoPhong(x.id,"")); kiemTraThuTu(na.tin(x.id),76,125);
            cho(a.dangXuat());
        }
        // Dong du ba Client va Server, khong de lai tai nguyen test.
        public void close() { a.close(); b.close(); c.close(); server.close(); }
    }

    // ===== HAI MULTICAST SOCKET CUNG CONG =====
    private static void testCungCong() throws Exception {
        int cong;
        try (DatagramSocket tam=new DatagramSocket(0)) { cong=tam.getLocalPort(); }
        CountDownLatch daNhan=new CountDownLatch(1); AtomicInteger lot=new AtomicInteger();
        KenhPhong a=new KenhPhong("phong-a","239.255.42.250",cong,t -> daNhan.countDown(),s -> { },s -> { });
        try {
            KenhPhong b=new KenhPhong("phong-b","239.255.42.251",cong,t -> lot.incrementAndGet(),s -> { },s -> { });
            try (MulticastSocket gui=new MulticastSocket()) {
            gui.setNetworkInterface(MangUtil.timGiaoDienMang()); gui.setTimeToLive(1);
            byte[] data=Goi.thanhByte(new TinNhan(1,"phong-a","a","A","Chỉ A",1).dongGoi());
            gui.send(new DatagramPacket(data,data.length,InetAddress.getByName("239.255.42.250"),cong));
            can(daNhan.await(3,TimeUnit.SECONDS),"Socket A khong nhan multicast");
                Thread.sleep(300); can(lot.get()==0,"Socket B nhan lan roomId A khi cung cong");
            } finally { b.close(); }
        } finally { a.close(); }
    }

    // Co phong.
    private static boolean coPhong(BanNghe n,String id) { return n.phong.stream().anyMatch(p -> p.id.equals(id)); }
    // Tim.
    private static Phong tim(BanNghe n,String id) { return n.phong.stream().filter(p -> p.id.equals(id)).findFirst().orElseThrow(); }
    // Kiem tra thu tu.
    private static void kiemTraThuTu(List<TinNhan> ds,long tu,long den) {
        can(ds.size()==den-tu+1,"Sai so tin: "+ds.size()+", can "+(den-tu+1));
        for (int i=0;i<ds.size();i++) { can(ds.get(i).msgId==tu+i,"Sai thu tu/trung msgId o vi tri "+i); }
    }
    // Tu choi.
    private static void tuChoi(CompletableFuture<?> viec,String lyDo) throws Exception {
        try { cho(viec); throw new AssertionError("Yeu cau phai bi tu choi"); }
        catch (java.util.concurrent.ExecutionException e) {
            can(ClientCore.loiDeHieu(e).contains(lyDo),"Sai ly do tu choi: "+ClientCore.loiDeHieu(e));
        }
    }

    /** Thu su kien theo roomId, tach tin lich su de kiem tra badge khong tinh tin cu. */
    private static final class BanNghe implements Listener {
        final Map<String,CopyOnWriteArrayList<TinNhan>> cacTin=new ConcurrentHashMap<>(), lichSu=new ConcurrentHashMap<>();
        final Map<String,List<String>> thanhVien=new ConcurrentHashMap<>();
        final Set<String> daRoi=ConcurrentHashMap.newKeySet();
        final List<String> heThong=new CopyOnWriteArrayList<>();
        volatile List<Phong> phong=List.of();
        // Tin.
        List<TinNhan> tin(String id) { return List.copyOf(cacTin.getOrDefault(id,new CopyOnWriteArrayList<>())); }
        // On vao phong.
        @Override public void onVaoPhong(Phong p) { cacTin.put(p.id,new CopyOnWriteArrayList<>()); lichSu.put(p.id,new CopyOnWriteArrayList<>()); }
        // Nhan tin da duoc kiem tra danh tinh va phong.
        @Override public void onTinNhan(TinNhan t,boolean laToi) { onTinNhan(t,laToi,false); }
        // Nhan tin da duoc kiem tra danh tinh va phong.
        @Override public void onTinNhan(TinNhan t,boolean laToi,boolean cu) {
            cacTin.computeIfAbsent(t.roomId,id -> new CopyOnWriteArrayList<>()).add(t);
            if (cu) { lichSu.computeIfAbsent(t.roomId,id -> new CopyOnWriteArrayList<>()).add(t); }
        }
        // On thanh vien.
        @Override public void onThanhVien(String id,List<String> ds) { thanhVien.put(id,List.copyOf(ds)); }
        // On danh sach phong.
        @Override public void onDanhSachPhong(List<Phong> ds) { phong=List.copyOf(ds); }
        // On roi phong.
        @Override public void onRoiPhong(String id,String lyDo) { daRoi.add(id); }
        // Nhan thong bao he thong qua Listener.
        @Override public void onHeThong(String s) { heThong.add(s); }
        // Nhan danh sach online theo Listener.
        @Override public void onDanhSachOnline(List<String> ds) { }
        // Nhan loi de hien thi hoac ghi nhan.
        @Override public void onLoi(String s) { System.err.println("  CLIENT M2: "+s); }
        // Nhan trang thai phien da ngat.
        @Override public void onMatKetNoi(String s) { System.err.println("  CLIENT M2: "+s); }
    }
}
