package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.*;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Kiem thu tep tren UDP that, du lieu/seed rieng. Ai goi: Maven, SelfTest. Goi: ServerCore, ClientCore, TruyenTep. */
public final class TepSelfTest {
    // Khong khoi tao lop nhom test.
    private TepSelfTest() { }
    // Chay rieng khong UI, khong cham data that.
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(".selftest")); MangUtil.dungIPv4();
        System.setProperty("udpmultichat.clientConfig",Files.createTempDirectory(Path.of(".selftest"),"prefs-tep-").resolve("client.properties").toString());
        if (args.length>0 && args[0].equals("ackcu")) {
            chay("M4 ACK bitmap cu lap",TepMangSelfTest::ackCuLap); ketLuan(); return;
        }
        if (args.length>0 && args[0].equals("matgoi")) {
            try (BoTest t=new BoTest(.2)) { t.batDau(); chay("M4 mat goi rieng",t::matGoi); } ketLuan(); return;
        }
        chayTatCa(); ketLuan();
    }
    // ===== CAC NHOM KIEM THU =====
    static void chayTatCa() throws Exception {
        chay("M4 ten tep doc hai, tieng Viet, gioi han Client/Server",TepSelfTest::tenVaAnh);
        chay("M4 ACK bitmap cu lap khong dot het retry cua mot manh",TepMangSelfTest::ackCuLap);
        chay("M4 cua so: dao thu tu, trung goi, CRC sai, huy va timeout",TepMangSelfTest::chay);
        try (BoTest t=new BoTest(0)) {
            t.batDau();
            chay("M4 1 byte, 1 manh, 1MB, 5MB: byte/CRC va toc do 0%",t::kichThuoc);
            chay("M4 quyen ngoai phong, phong dong, INIT qua lon/rong",t::quyen);
            chay("M4 ten doc hai qua INIT/DATA/DONE van nam trong uploads",t::tenTepServer);
            chay("M4 hang doi, quota va gui lai khong trung",t::hangDoi);
            chay("M4 2 gui + 2 tai dong thoi, chat khong cham",t::dongThoi);
            chay("M4 thumbnail va anh hong, tep trong HISTORY/RESYNC",t::anhVaLichSu);
            chay("M4 huy gui/tai don tam, khong tao tin tep",t::huy);
            chay("M4 tat Client/Server giua chung khong treo va don tep tam",t::matKetNoi);
            chay("M4 recall, xoa phong, restart/orphan va tai tep cu",t::luuTru);
        }
        try (BoTest t=new BoTest(.2)) {
            t.batDau(); chay("M4 20% DATA/ACK ca upload/download seed 20261113: 5MB CRC",t::matGoi);
        }
    }
    // Kiem tra quy tac don gian khong can socket.
    private static void tenVaAnh() throws Exception {
        for (String s:List.of("../../x.txt","C:\\Windows\\a.exe","CON","NUL.txt","x".repeat(500),"Bài tập có dấu và khoảng trắng.txt")) {
            String n=TepTin.tenAnToan(s); can(!n.contains("..") && !n.matches(".*[\\\\/:*?\"<>|].*") && n.codePointCount(0,n.length())<=100,"Ten khong an toan: "+n);
            can(!n.equalsIgnoreCase("CON") && !n.equalsIgnoreCase("NUL.txt"),"Ten Windows dac biet");
        }
        for (long n:new long[]{0,Protocol.TEP_TOI_DA+1}) { try { TepTin.kiemTra(n,Protocol.TEP_TOI_DA); throw new AssertionError("Cho kich thuoc sai"); } catch (IllegalArgumentException dung) { } }
        Path tmp=Files.createTempDirectory(Path.of(".selftest"),"anh-tep-"); Path gia=tmp.resolve("gia.png"); Files.writeString(gia,"khong phai anh");
        can(AnhTep.tao(gia,tmp.resolve("thumb.jpg"))==null,"Anh gia duoc chap nhan");
        var h=new java.awt.image.BufferedImage(800,400,java.awt.image.BufferedImage.TYPE_INT_RGB); javax.imageio.ImageIO.write(h,"png",tmp.resolve("anh.png").toFile());
        Path nho=AnhTep.tao(tmp.resolve("anh.png"),tmp.resolve("thumb.jpg")); var a=javax.imageio.ImageIO.read(nho.toFile()); can(a.getWidth()==240 && a.getHeight()==120,"Sai thumbnail");
    }
    /** Vai tro: Ba Client va Server that, thu muc/cong test rieng. Ai goi: nhom test. Goi: Core. */
    private static final class BoTest implements AutoCloseable {
        final Path data,nguon; final ServerCore server; final Nghe na=new Nghe(),nb=new Nghe(),nc=new Nghe();
        final ClientCore a=new ClientCore(na),b=new ClientCore(nb),c=new ClientCore(nc); final double mat;
        Phong room; TepTin tepCu; Path tep5;
        // Seed tai lap o ca ben gui va ben ACK.
        BoTest(double mat) throws Exception {
            this.mat=mat; data=Files.createTempDirectory(Path.of(".selftest"),"m4-"); nguon=Files.createDirectories(data.resolve("nguon"));
            Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nspam.burst=10000\nspam.minute=10000\nmulticast.portBase="+(mat==0?33000:35000)+"\nupload.maxRetries=8\n");
            server=new ServerCore(data,new ServerCore.Listener() {
                public void onLog(String s) { System.out.println("  M4: "+s); }
                public void onThongKe(int u,int o,long t) { }
            });
            server.moPhongMatTep(mat,20261113); a.tep().moPhongMatGoi(mat,20261114); b.tep().moPhongMatGoi(mat,20261115);
        }
        // Dang ky/vao phong bang UDP; C khong phai thanh vien de kiem tra quyen.
        void batDau() throws Exception {
            server.batDau(0); int port=server.cong();
            for (var e:Map.of(a,"tep_a",b,"tep_b",c,"tep_c").entrySet()) { cho(e.getKey().dangKy("localhost",port,e.getValue(),"abcdef")); cho(e.getKey().dangNhap("localhost",port,e.getValue(),"abcdef")); }
            room=cho(a.taoPhong("Truyền tệp","",true,"",10)); cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.id,""));
        }
        // Ghi du lieu gia lap theo khoi, khong cap phat ca tep trong RAM.
        Path tao(String ten,int size) throws Exception {
            Path p=nguon.resolve(ten); byte[] dem=new byte[8192]; Random r=new Random(1234+size);
            try (var out=Files.newOutputStream(p)) { for (int con=size;con>0;) { r.nextBytes(dem); int n=Math.min(con,dem.length); out.write(dem,0,n); con-=n; } } return p;
        }
        // Ghi ket qua toc do thuc te cua ca luong INIT/CRC/DONE, khong chi toc do socket.
        TepTin guiTai(Path p) throws Exception {
            long luc=System.nanoTime(); TepTin t=a.tep().gui(room.id,p).get(180,TimeUnit.SECONDS); long giua=System.nanoTime();
            Path dich=b.tep().tai(t,data.resolve("tai").resolve(p.getFileName())).get(180,TimeUnit.SECONDS); long xong=System.nanoTime();
            can(Files.mismatch(p,dich)==-1 && TruyenTep.crc(p)==TruyenTep.crc(dich),"Tep tai khac ban goc");
            System.out.printf(java.util.Locale.ROOT,"BENCH loss=%.0f%% size=%d upload=%.3fs %.2f MB/s download=%.3fs %.2f MB/s%n",mat*100,Files.size(p),(giua-luc)/1e9,Files.size(p)/1048576.0/((giua-luc)/1e9),(xong-giua)/1e9,Files.size(p)/1048576.0/((xong-giua)/1e9));
            return t;
        }
        // Bien 1 byte va manh vua du khong duoc mat manh cuoi.
        void kichThuoc() throws Exception {
            for (int n:new int[]{1,TruyenTep.MANH,1024*1024,5*1024*1024}) { Path p=tao("tep-"+n+".bin",n); TepTin t=guiTai(p); if (n==5*1024*1024) { tepCu=t; tep5=p; } }
        }
        // Auth phai duoc Server kiem tra, ke ca Client bo qua preflight kich thuoc.
        void quyen() throws Exception {
            Path p=tao("mot.bin",1); biChan(c.tep().gui(room.id,p)); biChan(c.tep().tai(tepCu,data.resolve("cam.bin")));
            cho(a.dongMoPhong(room.id,false)); biChan(b.tep().gui(room.id,p)); cho(a.dongMoPhong(room.id,true));
            for (long n:new long[]{0,Protocol.TEP_TOI_DA+1}) {
                Path x=tao("loi-"+n,n==0?0:(int)n); biChan(a.tep().gui(room.id,x));
                biChan(a.yeuCauHoSo(Protocol.TEP_INIT,room.id,"x",Long.toString(n),"0","12345","0","0",TinNhan.taoMa()));
            }
        }
        // Goi INIT truc tiep de kiem tra Server khong tin ten ma Client gui len.
        void tenTepServer() throws Exception {
            Path goc=tao("ten-test.bin",1); long crc=TruyenTep.crc(goc);
            try (TruyenTep k=new TruyenTep(0,0,1,8)) {
                for (String ten:List.of("../../x.txt","C:\\Windows\\a.exe","CON","x".repeat(500),"Bài tập tiếng Việt có dấu.txt")) {
                    String[] p=cho(a.yeuCauHoSo(Protocol.TEP_INIT,room.id,ten,"1",Long.toString(crc),Integer.toString(k.cong()),"0","0",TinNhan.taoMa()));
                    k.gui(UUID.fromString(p[3]),new InetSocketAddress(InetAddress.getByName("127.0.0.1"),Integer.parseInt(p[5])),goc,1,crc,n -> { }).get(10,TimeUnit.SECONDS);
                    String[] done=cho(a.yeuCauHoSo(Protocol.TEP_DONE,p[3])); TepTin t=TepTin.doc(room.id,a.nguoiDung().id,done[4],System.currentTimeMillis());
                    Path bin=data.resolve("uploads").resolve(room.id).resolve(p[3]+".bin");
                    can(Files.isRegularFile(bin) && TruyenTep.crc(bin)==crc && t.ten().equals(TepTin.tenAnToan(ten)),"Server dung ten goc lam duong dan");
                }
            }
        }
        // Hai upload toi da, cac tep sau vao hang; retry giu ma va khong tao tin trung.
        void hangDoi() throws Exception {
            AtomicReference<String> ma=new AtomicReference<>(); na.huy=td -> { if (td.trangThai().equals("DONE")) { ma.set(td.maClient()); } };
            TepTin t=a.tep().gui(room.id,tep5).get(60,TimeUnit.SECONDS); na.huy=null;
            int truoc=new KhoLuuTru(data,s -> { }).docTin(room.id).size(); TepTin lap=a.tep().thuLai(ma.get()).get(10,TimeUnit.SECONDS);
            can(lap.id().equals(t.id()) && new KhoLuuTru(data,s -> { }).docTin(room.id).size()==truoc,"Retry tao tin/file trung");
            var ds=new ArrayList<CompletableFuture<TepTin>>();
            for (int i=0;i<4;i++) { ds.add(a.tep().gui(room.id,tao("hang-"+i+".bin",1024*1024))); }
            CompletableFuture.allOf(ds.toArray(CompletableFuture[]::new)).get(60,TimeUnit.SECONDS);
            String[] mot=cho(b.yeuCauHoSo(Protocol.TEP_INIT,room.id,"../../CON", "1","0","12345","0","0",TinNhan.taoMa()));
            String[] hai=cho(b.yeuCauHoSo(Protocol.TEP_INIT,room.id,"x.bin", "1","0","12345","0","0",TinNhan.taoMa()));
            biChan(b.yeuCauHoSo(Protocol.TEP_INIT,room.id,"x.bin", "1","0","12345","0","0",TinNhan.taoMa()));
            can(Files.exists(data.resolve("uploads").resolve(room.id).resolve(mot[3]+".part")),"INIT khong dung ten noi bo");
            cho(b.yeuCauHoSo(Protocol.TEP_CANCEL,mot[3])); cho(b.yeuCauHoSo(Protocol.TEP_CANCEL,hai[3]));
            Path co=data.resolve("khong-ghi-de.bin"); Files.writeString(co,"ban cu"); Path moi=b.tep().tai(t,co).get(60,TimeUnit.SECONDS);
            can(!moi.equals(co.toAbsolutePath()) && Files.readString(co).equals("ban cu") && Files.mismatch(tep5,moi)==-1,"Tai ghi de file cu");
            long[] stats=server.thongKeTep(room.id,a.nguoiDung().id); can(stats[0]>=5 && stats[1]>=5*1024*1024,"Thong ke file sai");
        }
        // Hai nguoi gui va hai nguoi tai, dong thoi do do tre chat trong khi truyen.
        void dongThoi() throws Exception {
            Path p=tao("song-song-a.bin",5*1024*1024),q=tao("song-song-b.bin",1024*1024);
            var x=a.tep().gui(room.id,p); var y=b.tep().gui(room.id,q); long luc=System.nanoTime();
            cho(a.guiTin(room.id,"CHAT_DANG_TRUYEN")); doi(() -> nb.tin.stream().anyMatch(t -> t.noiDung.equals("CHAT_DANG_TRUYEN")),4000);
            System.out.printf("CHAT latency=%.1f ms%n",(System.nanoTime()-luc)/1e6);
            TepTin t=x.get(60,TimeUnit.SECONDS); y.get(60,TimeUnit.SECONDS);
            var d1=a.tep().tai(t,data.resolve("tai-a.bin")); var d2=b.tep().tai(t,data.resolve("tai-b.bin"));
            can(Files.mismatch(p,d1.get(60,TimeUnit.SECONDS))==-1 && Files.mismatch(p,d2.get(60,TimeUnit.SECONDS))==-1,"Lan du lieu dong thoi");
        }
        // Filecard nam trong log 7 truong va RESYNC nhu tin van ban.
        void anhVaLichSu() throws Exception {
            var h=new java.awt.image.BufferedImage(800,400,java.awt.image.BufferedImage.TYPE_INT_RGB); Path p=nguon.resolve("ảnh mẫu.png"); javax.imageio.ImageIO.write(h,"png",p.toFile());
            TepTin t=guiTai(p); b.tep().napAnh(t); doi(() -> b.tep().anh(t.id())!=null,5000);
            can(b.tep().anh(t.id()).getWidth()==240,"Cache thumbnail sai");
            Path gia=nguon.resolve("gia.png"); Files.writeString(gia,"khong phai png"); TepTin g=guiTai(gia); b.tep().napAnh(g);
            cho(b.dongBoPhong(room.id)); var lich=new KhoLuuTru(data,s -> { }).docTin(room.id);
            can(lich.stream().anyMatch(tin -> tin.loai.equals("F") && tin.noiDung.contains(t.id())),"Mat the F trong log");
        }
        // Huy o su kien dau tien, Server khong tao tin F cho tep do.
        void huy() throws Exception {
            int truoc=new KhoLuuTru(data,s -> { }).docTin(room.id).size();
            AtomicBoolean da=new AtomicBoolean(); na.huy=td -> { if (td.trangThai().equals("SEND") && da.compareAndSet(false,true)) { a.tep().huy(td.maClient()); } };
            biChan(a.tep().gui(room.id,tep5)); na.huy=null;
            can(new KhoLuuTru(data,s -> { }).docTin(room.id).size()==truoc,"Huy van tao tin");
            AtomicBoolean tai=new AtomicBoolean(); nb.huy=td -> { if (td.trangThai().equals("DOWNLOAD") && tai.compareAndSet(false,true)) { b.tep().huy(td.maClient()); } };
            biChan(b.tep().tai(tepCu,data.resolve("huy-tai.bin"))); nb.huy=null;
            doi(() -> { try (var ds=Files.walk(data)) { return ds.noneMatch(p -> p.toString().endsWith(".part")); } catch (Exception e) { return false; } },5000);
        }
        // Tat Client/Server that giua truyen, heartbeat tu ket thuc Future va don tam.
        void matKetNoi() throws Exception {
            AtomicBoolean da=new AtomicBoolean(); na.huy=td -> { if (td.trangThai().equals("SEND") && td.daNhan()>0 && da.compareAndSet(false,true)) { a.close(); } };
            biChan(a.tep().gui(room.id,tep5)); na.huy=null;
            doi(() -> { try (var ds=Files.walk(data.resolve("uploads"))) { return ds.noneMatch(p -> p.toString().endsWith(".part")); } catch (Exception e) { return false; } },22000);
            int port=server.cong(); cho(a.dangNhap("localhost",port,"tep_a","abcdef")); cho(a.vaoPhong(room.id,""));
            AtomicBoolean tat=new AtomicBoolean(); nb.huy=td -> { if (td.trangThai().equals("DOWNLOAD") && td.daNhan()>0 && tat.compareAndSet(false,true)) { server.close(); } };
            var f=b.tep().tai(tepCu,data.resolve("mat-server.bin"));
            try { f.get(30,TimeUnit.SECONDS); throw new AssertionError("Tat Server van thanh cong"); }
            catch (ExecutionException dung) { can(ClientCore.loiDeHieu(dung).length()>0,"Loi mat ket noi rong"); }
            nb.huy=null; a.close(); b.close(); c.close(); server.batDau(port);
            cho(a.dangNhap("localhost",port,"tep_a","abcdef")); cho(b.dangNhap("localhost",port,"tep_b","abcdef")); cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.id,""));
            can(!Files.exists(data.resolve("mat-server.bin")),"Mat Server de lai tep dich dang do");
        }
        // Tep cu van tai duoc sau restart; orphan bi don, recall/xoa phong xoa file that.
        void luuTru() throws Exception {
            Path orphan=data.resolve("uploads").resolve(room.id).resolve(UUID.randomUUID()+".part"); Files.writeString(orphan,"mo coi");
            int port=server.cong(); a.close(); b.close(); c.close(); server.close(); server.batDau(port); can(!Files.exists(orphan),"Con orphan");
            cho(a.dangNhap("localhost",port,"tep_a","abcdef")); cho(b.dangNhap("localhost",port,"tep_b","abcdef")); cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.id,""));
            Path d=b.tep().tai(tepCu,data.resolve("restart.bin")).get(60,TimeUnit.SECONDS); can(Files.mismatch(tep5,d)==-1,"Restart tai sai");
            TinNhan tin=new KhoLuuTru(data,s -> { }).docTin(room.id).stream().filter(t -> t.loai.equals("F") && t.noiDung.contains(tepCu.id())).findFirst().orElseThrow();
            cho(a.troChuyen().thuHoi(room.id,tin.msgId)); can(!Files.exists(data.resolve("uploads").resolve(room.id).resolve(tepCu.id()+".bin")),"Recall con file");
            biChan(b.tep().tai(tepCu,data.resolve("da-xoa.bin"))); cho(a.xoaPhong(room.id)); can(!Files.exists(data.resolve("uploads").resolve(room.id)),"Xoa phong con folder");
        }
        // 5MB gom hang nghin manh, mat 20% o ca hai dau ACK/DATA.
        void matGoi() throws Exception { guiTai(tao("mat-goi-5MB.bin",5*1024*1024)); }
        // Luon dong socket ke ca khi test that bai.
        public void close() { a.close(); b.close(); c.close(); server.close(); }
    }
    /** Vai tro: Thu su kien tin/tien do khong Swing. Ai goi: ClientCore. Goi: collection thread-safe. */
    private static final class Nghe implements Listener {
        final List<TinNhan> tin=new CopyOnWriteArrayList<>(); volatile java.util.function.Consumer<TepTienDo> huy;
        public void onTinNhan(TinNhan t,boolean toi) { tin.add(t); }
        public void onTep(TepTienDo td) { var c=huy; if (c!=null) { c.accept(td); } }
        public void onHeThong(String s) { }
        public void onDanhSachOnline(List<String> ds) { }
        public void onLoi(String s) { System.err.println(" M4 Client: "+s); }
        public void onMatKetNoi(String s) { }
    }
}
