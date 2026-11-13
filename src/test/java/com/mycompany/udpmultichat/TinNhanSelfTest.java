package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.*;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Hoi quy sticker, thu hoi, retry, typing tren UDP/multicast that. Ai goi: SelfTest/main. Goi: Core, QuanLyTinNhan. */
public final class TinNhanSelfTest {
    // Lop chi chua nhom kiem thu.
    private TinNhanSelfTest() { }
    // Chay rieng, cau hinh va du lieu khong cham du lieu that.
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(".selftest"));
        System.setProperty("udpmultichat.clientConfig",Files.createTempDirectory(Path.of(".selftest"),"prefs-tin-").resolve("client.properties").toString());
        chayTatCa(); ketLuan();
    }
    // ===== CAC TRUONG HOP M3B =====
    static void chayTatCa() throws Exception {
        chay("M3b quyen thu hoi 10 phut, chu/admin va tuong thich tin cu",TinNhanSelfTest::kiemTraQuyen);
        try (BoTest t=new BoTest()) {
            t.batDau();
            chay("M3b sticker hop le/khong hop le, UTF-8 va cach ly phong",t::sticker);
            chay("M3b thu hoi quyen dung/sai, xoa noi dung dia, HISTORY/RESYNC",t::thuHoi);
            chay("M3b 8 giay thieu multicast -> Loi, retry cung ma khong trung",t::trangThai);
            chay("M3b TYPING khong luu tin, gioi han Client/Server 3 giay",t::typing);
            chay("M3b khoi dong lai: tombstone va ma gui lai con ben vung",t::napLai);
        }
    }
    // Kiem tra bien thoi gian khong doi may hay cho muoi phut that.
    private static void kiemTraQuyen() {
        NguoiDung chu=new NguoiDung("chu","chuphong","Chủ",VaiTro.THANH_VIEN,"2026-11-13");
        NguoiDung a=new NguoiDung("a","aaa","An",VaiTro.THANH_VIEN,"2026-11-13"),ad=new NguoiDung("ad","admin","Admin",VaiTro.ADMIN,"2026-11-13");
        Phong r=new Phong("r","Phòng","","ABC123","chu","239.255.42.2",9002,20,true,false,true,3,"2026-11-13");
        TinNhan t=new TinNhan(1,"r","a","An","bí mật",1000,TinNhan.taoMa(),"T");
        can(QuanLyTinNhan.duocThuHoi(a,r,t,1000+Protocol.THU_HOI_MS),"Chan dung 10 phut");
        can(!QuanLyTinNhan.duocThuHoi(a,r,t,1001+Protocol.THU_HOI_MS),"Cho qua 10 phut");
        can(QuanLyTinNhan.duocThuHoi(chu,r,t,90000000) && QuanLyTinNhan.duocThuHoi(ad,r,t,90000000),"Chu/admin bi han che");
        can(!t.daThuHoi().dongGoi().contains("bí mật"),"Goi thu hoi lo noi dung");
        can(TinNhan.tuTruong(Goi.tach(t.dongGoi())).maClient.equals(t.maClient),"Mat ma Client");
        TinNhan cu=new TinNhan(2,"r","a","An","~abc~T~ky tu |\n",1001);
        can(TinNhan.tuTruong(Goi.tach(cu.dongGoi())).noiDung.equals(cu.noiDung),"Tin cu bi hieu nham");
    }
    /** Vai tro: bo ba Client that dung Server va data test rieng. Ai goi: nhom test. Goi: ServerCore, ClientCore. */
    private static final class BoTest implements AutoCloseable {
        final Path data;
        final BanNghe ngheA=new BanNghe(),ngheB=new BanNghe(),ngheAd=new BanNghe();
        final AtomicBoolean boMulticast=new AtomicBoolean();
        final ClientCore a=new ClientCore(ngheA,true,t -> !boMulticast.get()),b=new ClientCore(ngheB),ad=new ClientCore(ngheAd);
        final ServerCore server;
        Phong room;
        long idSticker,idThuHoi;
        String maGuiLai;
        // Khoi tao data tam, port multicast khac nhom cu.
        BoTest() throws Exception {
            data=Files.createTempDirectory(Path.of(".selftest"),"m3b-"); Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nmulticast.portBase=31000\n");
            server=new ServerCore(data,new ServerCore.Listener() {
                // Log Server test de truy vet loi.
                public void onLog(String s) { System.out.println("  M3b: "+s); }
                // Khong tao Swing trong SelfTest.
                public void onThongKe(int u,int o,long t) { }
            });
        }
        // Tao ba phien va phong rieng chi A/B tham gia.
        void batDau() throws Exception {
            server.batDau(0); int c=server.cong(); cho(a.dangKy("localhost",c,"aaa","abcdef123")); cho(b.dangKy("localhost",c,"bbb","abcdef123"));
            cho(a.dangNhap("localhost",c,"aaa","abcdef123")); cho(b.dangNhap("localhost",c,"bbb","abcdef123")); cho(ad.dangNhap("localhost",c,"admin","adminTest123"));
            room=cho(a.taoPhong("Phòng sticker","",false,"",10)); cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.maPhong,""));
        }
        // Ma hop le di qua multicast; Server tu choi ma la, so tin khong tang.
        void sticker() throws Exception {
            String ma=TinNhan.taoMa(); cho(a.troChuyen().guiCoMa(room.id,"2764","S",ma));
            doi(() -> ngheB.tin.stream().anyMatch(t -> t.maClient.equals(ma) && t.loai.equals("S") && t.noiDung.equals("2764")),4000);
            can(ngheAd.tin.stream().noneMatch(t -> t.roomId.equals(room.id)),"Sticker lot sang phong khac");
            idSticker=ngheB.tin.stream().filter(t -> t.maClient.equals(ma)).findFirst().orElseThrow().msgId;
            biChan(a.troChuyen().gui(room.id,"MA_LA","S")); can(Protocol.stickerHopLe("2764") && !Protocol.stickerHopLe("MA_LA"),"Registry sai");
            cho(a.guiTin(room.id,"Sau sticker")); doi(() -> ngheB.tin.stream().anyMatch(t -> t.roomId.equals(room.id) && t.msgId==idSticker+1),4000);
            cho(b.troChuyen().guiCoMa(room.id,"1f44d","S",ma));
            doi(() -> ngheA.tin.stream().filter(t -> t.maClient.equals(ma)).count()==2,4000);
        }
        // Thu hoi nguoi khac bi chan; chu/admin duoc phep, RESYNC van mang tombstone.
        void thuHoi() throws Exception {
            biChan(b.troChuyen().thuHoi(room.id,idSticker)); cho(a.troChuyen().thuHoi(room.id,idSticker));
            doi(() -> ngheB.thuHoi.contains(room.id+":"+idSticker),4000);
            cho(b.guiTin(room.id,"Noi dung phai xoa")); doi(() -> ngheA.tin.stream().anyMatch(t -> t.noiDung.equals("Noi dung phai xoa")),4000);
            idThuHoi=ngheA.tin.stream().filter(t -> t.noiDung.equals("Noi dung phai xoa")).findFirst().orElseThrow().msgId;
            cho(a.troChuyen().thuHoi(room.id,idThuHoi));
            cho(b.guiTin(room.id,"Tin tự thu hồi")); doi(() -> ngheB.tin.stream().anyMatch(t -> t.noiDung.equals("Tin tự thu hồi")),4000);
            long cuaB=ngheB.tin.stream().filter(t -> t.noiDung.equals("Tin tự thu hồi")).findFirst().orElseThrow().msgId;
            cho(b.troChuyen().thuHoi(room.id,cuaB));
            cho(b.guiTin("Admin sẽ thu hồi")); doi(() -> ngheAd.tin.stream().anyMatch(t -> t.noiDung.equals("Admin sẽ thu hồi")),4000);
            long id=ngheAd.tin.stream().filter(t -> t.noiDung.equals("Admin sẽ thu hồi")).findFirst().orElseThrow().msgId;
            cho(ad.troChuyen().thuHoi("sanh",id));
            ngheB.thuHoi.clear(); cho(b.dongBoPhong(room.id)); can(ngheB.thuHoi.contains(room.id+":"+idThuHoi),"RESYNC khong bu tombstone cu");
            can(!Files.readString(data.resolve("rooms").resolve(room.id+".log")).contains("Noi dung phai xoa"),"Lich su tren dia lo noi dung");
            cho(b.dangXuat()); ngheB.tin.clear(); cho(b.dangNhap("localhost",server.cong(),"bbb","abcdef123")); cho(b.vaoPhong(room.maPhong,""));
            doi(() -> ngheB.tin.stream().anyMatch(t -> t.roomId.equals(room.id) && t.msgId==idThuHoi && t.thuHoi && t.noiDung.isEmpty()),4000);
        }
        // Server da luu nhung multicast mat: loi 8s va gui lai khong them dong tren Server.
        void trangThai() throws Exception {
            boMulticast.set(true); maGuiLai=TinNhan.taoMa();
            cho(a.troChuyen().guiCoMa(room.id,"Gửi lại không trùng","T",maGuiLai));
            doi(() -> "ERROR".equals(ngheA.trangThai.get(maGuiLai)),11000);
            can(!"SENT".equals(ngheA.trangThai.get(maGuiLai)),"Da gui du chua co multicast");
            boMulticast.set(false); cho(a.troChuyen().guiLai(room.id,maGuiLai));
            doi(() -> "SENT".equals(ngheA.trangThai.get(maGuiLai)),4000);
            can(ngheB.tin.stream().filter(t -> t.maClient.equals(maGuiLai)).count()==1,"Retry hien tin trung");
            var kho=new KhoLuuTru(data,s -> { }); can(kho.docTin(room.id).stream().filter(t -> t.maClient.equals(maGuiLai)).count()==1,"Retry luu trung");
            biChan(a.troChuyen().guiCoMa(room.id,"Nội dung bị đổi","T",maGuiLai));
        }
        // Ca hai phia deu co limiter, noi dung rong khong phat; khong ghi log.
        void typing() throws Exception {
            long soTin=new KhoLuuTru(data,s -> { }).docTin(room.id).size();
            a.troChuyen().dangGo(room.id,""); a.troChuyen().dangGo(room.id,"đang gõ"); a.troChuyen().dangGo(room.id,"gõ tiếp");
            doi(() -> ngheB.dangGo.get()>0,4000); Thread.sleep(200); can(ngheB.dangGo.get()==1,"Typing phat qua nhieu");
            can(new KhoLuuTru(data,s -> { }).docTin(room.id).size()==soTin,"Typing duoc luu thanh tin");
            var q=new QuanLyTinNhan(new KhoLuuTru(data,s -> { }));
            can(q.choTyping("r","u",0) && !q.choTyping("r","u",2999) && q.choTyping("r","u",3000),"Limiter Server sai bien");
            can(a.troChuyen().choTyping("kiemtra",0) && !a.troChuyen().choTyping("kiemtra",2999) && a.troChuyen().choTyping("kiemtra",3000),"Limiter Client sai bien");
        }
        // Nap ban ghi luu, retry ma cu khong tao tin moi sau restart.
        void napLai() throws Exception {
            cho(a.dangXuat()); cho(b.dangXuat()); cho(ad.dangXuat()); server.close(); server.batDau(0);
            cho(a.dangNhap("localhost",server.cong(),"aaa","abcdef123")); cho(a.vaoPhong(room.maPhong,""));
            cho(a.troChuyen().guiCoMa(room.id,"Gửi lại không trùng","T",maGuiLai));
            var ds=new KhoLuuTru(data,s -> { }).docTin(room.id);
            can(ds.stream().filter(t -> t.maClient.equals(maGuiLai)).count()==1,"Restart mat dedup");
            can(ds.stream().anyMatch(t -> t.msgId==idThuHoi && t.thuHoi && t.noiDung.isEmpty()),"Restart mat recall");
            cho(a.dangXuat());
        }
        // Dong moi socket, luong va Server ke ca khi test that bai.
        public void close() { a.close(); b.close(); ad.close(); server.close(); }
    }
    /** Vai tro: ghi nhan su kien an toan luong. Ai goi: ClientCore. Goi: collection RAM. */
    private static final class BanNghe implements Listener {
        final List<TinNhan> tin=new CopyOnWriteArrayList<>(); final Set<String> thuHoi=ConcurrentHashMap.newKeySet();
        final Map<String,String> trangThai=new ConcurrentHashMap<>(); final AtomicInteger dangGo=new AtomicInteger();
        // Tin duoc phat theo msgId sau DongBoTinNhan.
        public void onTinNhan(TinNhan t,boolean laToi) { tin.add(t); }
        // Thu hoi chi ghi id, khong noi dung goc.
        public void onThuHoi(String room,long id) { thuHoi.add(room+":"+id); }
        // Theo doi trang thai tin cuc bo theo ma bat bien.
        public void onTrangThaiGui(TinNhan t,String ma,String loi) { trangThai.put(t.maClient,ma); }
        // Dem typing nhan qua multicast that.
        public void onDangGo(String r,String u,String ten) { dangGo.incrementAndGet(); }
        // Khong dung thong bao chat trong test nay.
        public void onHeThong(String s) { }
        // Danh sach da co test hoi quy rieng.
        public void onDanhSachOnline(List<String> ds) { }
        // Ghi loi nen de khong che loi mang.
        public void onLoi(String s) { System.err.println(" M3b Client: "+s); }
        // Khong tao UI khi mat ket noi.
        public void onMatKetNoi(String s) { }
    }
}
