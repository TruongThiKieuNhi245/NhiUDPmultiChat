package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.ClientCore;
import com.mycompany.udpmultichat.client.Listener;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Kiem thu ho so, avatar va mat khau khong Swing. Ai goi: SelfTest/main. Goi: Core that, PhanManh, KhoLuuTru. */
public final class HoSoSelfTest {
    // Lop chi cung cap diem chay kiem thu.
    private HoSoSelfTest() { }
    // Diem chay nhanh rieng M3a.
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(".selftest"));
        System.setProperty("udpmultichat.clientConfig",Files.createTempDirectory(Path.of(".selftest"),"prefs-ho-so-").resolve("client.properties").toString());
        chayTatCa(); ketLuan();
    }
    // ===== KIEM THU CAC DUONG NGHIEP VU =====
    static void chayTatCa() throws Exception {
        chay("Avatar het ACK: thu lai cung ma, loi quyen khong lap, gioi han bon dot",com.mycompany.udpmultichat.client.YeuCauAnhSelfTest::kiemTra);
        chay("Avatar SAVE/BEGIN trung, huy va rollback khong tao giao dich moi",HoSoSelfTest::kiemTraGiaoDich);
        chay("M3a anh PNG/JPG, cat giua 96x96, tu choi tep gia/qua 2MB",HoSoSelfTest::kiemTraAnh);
        chay("M3a Server tu choi avatar khong phai PNG 96x96/qua lon va CRC sai",HoSoSelfTest::kiemTraServer);
        chay("M3a nang cap users.csv 7 cot cu khong mat tai khoan",HoSoSelfTest::kiemTraCsvCu);
        Path data=Files.createTempDirectory(Path.of(".selftest"),"m3a-");
        Files.writeString(data.resolve("config.properties"),"spam.burst=10000\nspam.minute=10000\nmulticast.portBase=27000\n");
        ServerCore server=new ServerCore(data,new ServerCore.Listener() {
            // Log cua Server test.
            public void onLog(String s) { System.out.println("  M3a: "+s); }
            // Khong tao Swing trong test.
            public void onThongKe(int u,int o,long t) { }
        });
        BanNghe nghe=new BanNghe();
        try (ClientCore admin=new ClientCore(new BanNghe()); ClientCore a=new ClientCore(new BanNghe()); ClientCore b=new ClientCore(nghe)) {
            server.batDau(0); int cong=server.cong();
            chay("M3a admin mac dinh bi chan truoc doi; doi hash/config va vao Sanh",() -> kiemTraAdmin(admin,cong));
            cho(a.dangKy("localhost",cong,"nguoi_a","abcdef123")); cho(b.dangKy("localhost",cong,"nguoi_b","abcdef123"));
            cho(a.dangNhap("localhost",cong,"nguoi_a","abcdef123")); cho(b.dangNhap("localhost",cong,"nguoi_b","abcdef123"));
            chay("M3a doi ten/gioi thieu phat sang Client khac, dinh danh khong doi",() -> kiemTraHoSo(a,b,nghe));
            chay("M3a avatar luu/tai CRC dung, Huy va Xoa khong lam sai ho so",() -> kiemTraAvatar(a,b,data));
            chay("M3a mat khau cu sai/dung, 6-128 ky tu, mat khau cu het hieu luc",() -> kiemTraMatKhau(a,cong));
            cho(admin.dangXuat()); cho(a.dangXuat()); cho(b.dangXuat());
        } finally { server.close(); }
        chay("M3a khoi dong lai giu ho so va mat khau admin moi",() -> kiemTraNapLai(data));
        chay("M3a avatar 20% mat DATA/ACK, seed 20261113, CRC trung khop",HoSoSelfTest::kiemTraMatGoi);
    }
    // Default admin chua duoc READY hay tao phong; bat buoc doi phia Server.
    private static void kiemTraAdmin(ClientCore c,int cong) throws Exception {
        NguoiDung n=cho(c.dangNhap("localhost",cong,"admin","admin123")); can(n.buocDoiMatKhau,"Thieu co bat buoc");
        can(c.phongDaVao().isEmpty(),"Default admin da vao phong truoc khi doi"); biChan(c.vaoSanh());
        biChan(c.taoPhong("Không được","",true,"",5)); biChan(c.doiMatKhau("saimatkhau","abcdef123"));
        biChan(c.doiMatKhau("admin123","admin123"));
        can(!cho(c.doiMatKhau("admin123","adminMoi123")).buocDoiMatKhau,"Doi xong van bi buoc"); cho(c.vaoSanh());
        biChan(c.doiMatKhau("adminMoi123","admin123"));
    }
    // Moi snapshot co ten hien thi moi nhung ten dang nhap cu.
    private static void kiemTraHoSo(ClientCore a,ClientCore b,BanNghe nghe) throws Exception {
        String id=a.nguoiDung().id;
        var n=cho(a.hoSo().luu("Nguyễn An","Sinh viên mạng",null,false,new AtomicBoolean(),i -> { }));
        doi(() -> nghe.hoSo.stream().anyMatch(u -> u.id.equals(id) && u.tenHienThi.equals("Nguyễn An")),5000);
        can(n.tenDangNhap.equals("nguoi_a") && n.id.equals(id),"Doi ten lam doi danh tinh");
        doi(() -> b.hoSo().lay(id)!=null && b.hoSo().lay(id).gioiThieu.equals("Sinh viên mạng"),4000);
        biChan(a.hoSo().luu("x","",null,false,new AtomicBoolean(),i -> { }));
        biChan(a.hoSo().luu("Tên "+new String(Character.toChars(0x1F600)),"",null,false,new AtomicBoolean(),i -> { }));
        biChan(a.hoSo().luu("Hợp lệ","x".repeat(101),null,false,new AtomicBoolean(),i -> { }));
    }
    // Upload chi commit khi SAVE; Huy truoc SAVE khong doi avatar.
    private static void kiemTraAvatar(ClientCore a,ClientCore b,Path data) throws Exception {
        byte[] png=anhMau();
        NguoiDung n=a.hoSo().luu("Nguyễn An","Sinh viên mạng",png,false,new AtomicBoolean(),i -> { }).get(60,TimeUnit.SECONDS);
        byte[] tai=b.hoSo().taiDuLieu(n).get(60,TimeUnit.SECONDS);
        can(java.util.Arrays.equals(png,tai) && PhanManh.crc(png).equals(PhanManh.crc(tai)),"CRC download sai");
        doi(() -> b.hoSo().lay(n.id)!=null && b.hoSo().lay(n.id).anhHash.equals(n.anhHash) && b.hoSo().anh(n.id)!=null,10000);
        can(java.util.Arrays.equals(png,Files.readAllBytes(data.resolve("avatars/nguoi_a.png"))),"Anh tren dia sai");
        AtomicBoolean huy=new AtomicBoolean();
        biChan(a.hoSo().luu("Nguyễn An","",png,false,huy,i -> { if (i>=20) { huy.set(true); } }));
        can(a.nguoiDung().anhHash.equals(n.anhHash),"Huy lam mat anh cu");
        NguoiDung xoa=cho(a.hoSo().luu("Nguyễn An","Sinh viên mạng",null,true,new AtomicBoolean(),i -> { }));
        can(xoa.anhHash.isEmpty() && !Files.exists(data.resolve("avatars/nguoi_a.png")),"Xoa anh khong dung");
        a.hoSo().luu("Nguyễn An","Sinh viên mạng",png,false,new AtomicBoolean(),i -> { }).get(60,TimeUnit.SECONDS);
    }
    // Kiem tra password Server, sau do login bang mat khau moi.
    private static void kiemTraMatKhau(ClientCore a,int cong) throws Exception {
        biChan(a.doiMatKhau("saimatkhau","abcdef456")); biChan(a.doiMatKhau("abcdef123","12345"));
        biChan(a.doiMatKhau("abcdef123","x".repeat(129))); cho(a.doiMatKhau("abcdef123","abcdef456")); cho(a.dangXuat());
        biChan(a.dangNhap("localhost",cong,"nguoi_a","abcdef123")); cho(a.dangNhap("localhost",cong,"nguoi_a","abcdef456"));
    }
    // Ho so, hash admin moi van con sau khi nap lai kho.
    private static void kiemTraNapLai(Path data) throws Exception {
        KhoLuuTru kho=new KhoLuuTru(data,s -> { }); QuanLyNguoiDung q=new QuanLyNguoiDung(kho);
        var phien=q.dangNhap("admin","adminMoi123",new java.net.InetSocketAddress("localhost",33001));
        can(!q.lay(q.tatCa().stream().filter(n -> n.tenDangNhap.equals("admin")).findFirst().orElseThrow().id).buocDoiMatKhau,"Restart bat buoc doi lai");
        can(q.tatCa().stream().anyMatch(n -> n.tenHienThi.equals("Nguyễn An") && n.gioiThieu.equals("Sinh viên mạng")),"Mat ho so sau restart"); q.dangXuat(phien);
        NguoiDung an=q.tatCa().stream().filter(n -> n.tenDangNhap.equals("nguoi_a")).findFirst().orElseThrow();
        can(an.anhHash.equals(PhanManh.crc(Files.readAllBytes(data.resolve("avatars/nguoi_a.png")))),"Restart mat anh/hash");
        can(!Files.readString(data.resolve("users.csv")).contains("abcdef456") && !Files.readString(data.resolve("config.properties")).contains("adminMoi123"),"Luu mat khau tho");
    }
    // Chuyen users.csv cua M2.x sang schema co gioi thieu va avatar.
    private static void kiemTraCsvCu() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"csv-cu-");
        String salt=BaoMat.ngauNhien(16),hash=BaoMat.bam(salt,"abcdef123");
        Files.writeString(d.resolve("users.csv"),"id,tenDangNhap,tenHienThi,vaiTro,ngayTao,salt,hash\n"+
            "idcu,nguoi_cu,Tên cũ,THANH_VIEN,2026-11-12,"+salt+","+hash+"\n");
        KhoLuuTru kho=new KhoLuuTru(d,t -> { }); QuanLyNguoiDung q=new QuanLyNguoiDung(kho);
        q.dangNhap("nguoi_cu","abcdef123",new java.net.InetSocketAddress("localhost",33003));
        can(q.lay("idcu").tenHienThi.equals("Tên cũ") && q.lay("idcu").anhHash.isEmpty(),"Migration lam mat ho so cu");
        can(new KhoLuuTru(d,t -> { }).docNguoiDung().size()==2,"Schema moi khong nap lai duoc");
    }
    // Tep dau vao phai dung loai, kich thuoc va anh thuc su.
    private static void kiemTraAnh() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"anh-");
        Path png=d.resolve("mau.png"); Files.write(png,anhMau());
        can(PhanManh.docAnh(PhanManh.chuanHoa(png),true).getWidth()==96,"Khong chuan hoa 96");
        Path gia=d.resolve("gia.jpg"); Files.writeString(gia,"khong phai anh"); chanAnh(gia);
        Path sai=d.resolve("mau.gif"); Files.write(sai,anhMau()); chanAnh(sai);
        Path lon=d.resolve("lon.png"); Files.write(lon,new byte[Protocol.ANH_DAU_VAO+1]); chanAnh(lon);
        BufferedImage ngang=new BufferedImage(192,96,BufferedImage.TYPE_INT_RGB);
        var g=ngang.createGraphics(); g.setColor(java.awt.Color.RED); g.fillRect(0,0,192,96); g.setColor(java.awt.Color.BLUE); g.fillRect(48,0,96,96); g.dispose();
        Path jpg=d.resolve("ngang.jpg"); javax.imageio.ImageIO.write(ngang,"jpg",jpg.toFile());
        BufferedImage daCat=PhanManh.docAnh(PhanManh.chuanHoa(jpg),true);
        can((daCat.getRGB(48,48)&255)>200,"Khong cat giua JPG");
    }
    // Server truc tiep: bo qua validator Client de chung minh khong tin dau vao.
    private static void kiemTraServer() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"anh-server-"); KhoLuuTru k=new KhoLuuTru(d,s -> { });
        QuanLyNguoiDung q=new QuanLyNguoiDung(k); q.dangKy("nguoi_a","abcdef123");
        var p=q.dangNhap("nguoi_a","abcdef123",new java.net.InetSocketAddress("localhost",33002)); QuanLyHoSo h=new QuanLyHoSo(k,q);
        String id=java.util.UUID.randomUUID().toString(); byte[] gia={1,2,3};
        h.xuLy(p,new String[]{Protocol.ANH_BAT_DAU,"req","token",id,"3",PhanManh.crc(gia)});
        h.xuLy(p,new String[]{Protocol.ANH_MANH,"req","token",id,"0",PhanManh.manh(gia,0)});
        chanLenh(() -> h.xuLy(p,new String[]{Protocol.LUU_HO_SO,"req","token","Tên hợp lệ","",id}));
        chanLenh(() -> h.xuLy(p,new String[]{Protocol.ANH_BAT_DAU,"req","token",id,"65537","1"}));
        BufferedImage lon=new BufferedImage(97,96,BufferedImage.TYPE_INT_RGB); var out=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(lon,"png",out);
        byte[] saiCo=out.toByteArray(); h.xuLy(p,new String[]{Protocol.ANH_BAT_DAU,"req","token",id,Integer.toString(saiCo.length),PhanManh.crc(saiCo)});
        for (int i=0;i<(saiCo.length+Protocol.MANH_ANH-1)/Protocol.MANH_ANH;i++) { h.xuLy(p,new String[]{Protocol.ANH_MANH,"req","token",id,Integer.toString(i),PhanManh.manh(saiCo,i)}); }
        chanLenh(() -> h.xuLy(p,new String[]{Protocol.LUU_HO_SO,"req","token","Tên hợp lệ","",id}));
        var ghep=new PhanManh.BoGhep(3,"0"); chanLenh(() -> ghep.nhan(Integer.MAX_VALUE,"AQID")); ghep.nhan(0,PhanManh.manh(gia,0)); chanLenh(ghep::ketQua);
    }
    // Mat goi o Client anh huong ca upload DATA va ACK cua download.
    private static void kiemTraMatGoi() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"anh-loss-"); Files.writeString(d.resolve("config.properties"),"spam.burst=10000\nspam.minute=10000\nmulticast.portBase=29000\n");
        ServerCore s=new ServerCore(d,new ServerCore.Listener() {
            // Test mat goi chi can ket qua, khong lap log Server.
            public void onLog(String t) { }
            // Khong tao Swing trong SelfTest.
            public void onThongKe(int u,int o,long t) { }
        });
        try (ClientCore c=new ClientCore(new BanNghe())) {
            c.moPhongMatGoi(0.20,20261113L); s.batDau(0); cho(c.dangKy("localhost",s.cong(),"mat_goi","abcdef123")); cho(c.dangNhap("localhost",s.cong(),"mat_goi","abcdef123"));
            byte[] png=anhMau(); NguoiDung n=c.hoSo().luu("Mất gói","",png,false,new AtomicBoolean(),i -> { }).get(120,TimeUnit.SECONDS);
            byte[] manh=new byte[600]; java.util.Arrays.fill(manh,(byte)255);
            Goi.thanhByte(Goi.ghep("R","x".repeat(80),Goi.ghep(Protocol.ANH_MANH,"x".repeat(36),"x".repeat(48),"x".repeat(36),"109",PhanManh.manh(manh,0))));
            byte[] tai=c.hoSo().taiDuLieu(n).get(120,TimeUnit.SECONDS); can(java.util.Arrays.equals(png,tai),"Avatar mat goi CRC khong trung"); cho(c.dangXuat());
        } finally { s.close(); }
    }
    // Mat phan hoi SAVE duoc tai hien bang cach phat lai nguyen request sau khi da commit.
    private static void kiemTraGiaoDich() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"anh-giao-dich-");
        KhoLuuTru k=new KhoLuuTru(d,s -> { }); QuanLyNguoiDung q=new QuanLyNguoiDung(k);
        q.dangKy("anh_test","abcdef123");
        var p=q.dangNhap("anh_test","abcdef123",new java.net.InetSocketAddress("localhost",33009));
        QuanLyHoSo h=new QuanLyHoSo(k,q); byte[] png=anhMau(); String upload=java.util.UUID.randomUUID().toString();
        String[] begin={Protocol.ANH_BAT_DAU,java.util.UUID.randomUUID().toString(),"token",upload,""+png.length,PhanManh.crc(png)};
        h.xuLy(p,begin);
        for (int i=0;i<(png.length+599)/600;i++) {
            h.xuLy(p,new String[]{Protocol.ANH_MANH,java.util.UUID.randomUUID().toString(),"token",upload,""+i,PhanManh.manh(png,i)});
            if (i==0) { h.xuLy(p,begin); }
        }
        String[] save={Protocol.LUU_HO_SO,java.util.UUID.randomUUID().toString(),"token","Ảnh thử","",upload};
        String[] lanDau=h.xuLy(p,save); can(java.util.Arrays.equals(lanDau,h.xuLy(p,save)),"SAVE trung bi commit lai");
        can(java.util.Arrays.equals(png,Files.readAllBytes(d.resolve("avatars/anh_test.png"))),"BEGIN trung da xoa manh");
        String[] gia=save.clone(); gia[3]="Giả mạo"; chanLenh(() -> h.xuLy(p,gia));
        String huy=java.util.UUID.randomUUID().toString();
        h.xuLy(p,new String[]{Protocol.ANH_BAT_DAU,java.util.UUID.randomUUID().toString(),"token",huy,"3","0"});
        h.xuLy(p,new String[]{Protocol.ANH_HUY,java.util.UUID.randomUUID().toString(),"token",huy});
        chanLenh(() -> h.xuLy(p,new String[]{Protocol.LUU_HO_SO,java.util.UUID.randomUUID().toString(),"token","Ảnh thử","",huy}));
        can(java.util.Arrays.equals(png,Files.readAllBytes(d.resolve("avatars/anh_test.png"))),"Huy lam mat anh cu");
        Files.createDirectory(d.resolve("users.csv.tmp"));
        chanLenh(() -> h.xuLy(p,new String[]{Protocol.LUU_HO_SO,java.util.UUID.randomUUID().toString(),"token","Đổi bị lỗi","","DELETE"}));
        can(q.tatCa().stream().anyMatch(n -> n.tenDangNhap.equals("anh_test") && n.tenHienThi.equals("Ảnh thử")),"Loi luu profile da doi RAM");
        can(java.util.Arrays.equals(png,Files.readAllBytes(d.resolve("avatars/anh_test.png"))),"Loi luu profile khong rollback avatar");
        Files.delete(d.resolve("users.csv.tmp"));
    }
    // Anh nhieu mau co du nhieu manh de thuc su thu ACK/gui lai.
    private static byte[] anhMau() throws Exception {
        BufferedImage h=new BufferedImage(96,96,BufferedImage.TYPE_INT_RGB); java.util.Random r=new java.util.Random(77);
        for (int y=0;y<96;y++) { for (int x=0;x<96;x++) { h.setRGB(x,y,r.nextInt(0x1000000)); } }
        var out=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(h,"png",out); return out.toByteArray();
    }
    // Tep sai phai bi tu choi.
    private static void chanAnh(Path p) throws Exception { chanLenh(() -> PhanManh.chuanHoa(p)); }
    // Ham kiem tra ngoai le, khong nuot AssertionError.
    private static void chanLenh(ViecTest v) throws Exception {
        try { v.chay(); throw new AssertionError("Chap nhan dau vao sai"); } catch (java.io.IOException | IllegalArgumentException dung) { }
    }
    /** Vai tro: thu snapshot tu ClientCore khong Swing. Ai goi: Core. Goi: khong. */
    private static final class BanNghe implements Listener {
        final List<NguoiDung> hoSo=new CopyOnWriteArrayList<>();
        // Thu snapshot de doi su kien tu Client khac.
        public void onHoSo(NguoiDung n) { hoSo.add(n); }
        // Test nay chi kiem tra ho so.
        public void onTinNhan(TinNhan t,boolean laToi) { }
        // Khong can hien thong bao chat.
        public void onHeThong(String s) { }
        // Danh sach thanh vien da co test hoi quy.
        public void onDanhSachOnline(List<String> ds) { }
        // Ghi loi nen de khong che loi mang.
        public void onLoi(String s) { System.err.println("  M3a Client: "+s); }
        // Khong tao cua so khi het phien.
        public void onMatKetNoi(String s) { }
    }
}
