package com.mycompany.udpmultichat;

import com.mycompany.udpmultichat.client.*;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static com.mycompany.udpmultichat.SelfTest.*;

/** Vai tro: Kiem thu M5 tren UDP that va data tam. Ai goi: SelfTest/main. Goi: ServerCore, ClientCore, XuatBaoCao, NhatKy. */
public final class AdminSelfTest {
    // Khong khoi tao lop tien ich.
    private AdminSelfTest() { }
    // Chay rieng nhom M5.
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(".selftest")); MangUtil.dungIPv4();
        System.setProperty("udpmultichat.clientConfig",Files.createTempDirectory(Path.of(".selftest"),"prefs-admin-").resolve("client.properties").toString());
        chayTatCa(); ketLuan();
    }
    // ===== CAC NHOM KIEM THU =====
    static void chayTatCa() throws Exception {
        chay("M5 last-active-admin guard",com.mycompany.udpmultichat.server.QuanTriBoSungSelfTest::baoVeAdmin);
        chay("M5 rollback create/reset/lock va persist",com.mycompany.udpmultichat.server.QuanTriBoSungSelfTest::rollback);
        chay("M5 20 tin/phut, audit auto mute va rollback moderation",com.mycompany.udpmultichat.server.QuanTriBoSungSelfTest::spamPhut);
        chay("M5 ZIP whitelist, zip slip, duplicate, thieu tep va cleanup",AdminSelfTest::zipAnToan);
        chay("M5 lap request CRUD/DENIED/broadcast partial khong trung tin/audit",com.mycompany.udpmultichat.server.QuanTriBoSungSelfTest::lenhTrung);
        try (BoTest t=new BoTest(false)) {
            t.batDau();
            chay("M5 moi lenh admin bi tu choi cho thanh vien va ghi DENIED",t::tuChoi);
            chay("M5 CRUD user, mat khau tam mot lan, doi bat buoc, khoa ngat phien",t::taiKhoan);
            chay("M5 bao ve admin chinh minh, Sanh chung va CRUD phong",t::rangBuoc);
            chay("M5 kick/mute het han/ban/go cam/chuyen chu va quyen chu phong",t::kiemDuyet);
            chay("M5 xoa user giu tin cu, chuyen chu va thong bao realtime",t::xoaUser);
            chay("M5 snapshot 5 user, 3 phong, 40 tin, 2 tep va CSV/HTML dung so lieu",t::baoCao);
            chay("M5 nhieu admin dong thoi, xuat nen khong chan chat",t::dongThoi);
            chay("M5 restart giu mute/ban/lock, vai tro va mat khau",t::khoiDongLai);
        }
        try (BoTest t=new BoTest(true)) { t.batDau(); chay("M5 spam 5/3s, lap lai tu mute 1 phut va audit",t::spam); }
        chay("M5 CSV BOM/RFC4180/injection, HTML escape va khong ghi de",AdminSelfTest::escape);
        chay("M5 audit mot dong/su kien, escape xuong dong va xoay vong",AdminSelfTest::nhatKy);
    }
    /** Vai tro: Ba Client that va Server rieng. Ai goi: chayTatCa. Goi: Core va Files. */
    private static final class BoTest implements AutoCloseable {
        final Path data; final ServerCore server; final BanNghe na=new BanNghe(),nb=new BanNghe(),nad=new BanNghe();
        final ClientCore a=new ClientCore(na),b=new ClientCore(nb),ad=new ClientCore(nad);
        Phong room; String aid,bid;
        // Cong va du lieu rieng, spam chi bat trong nhom chong spam.
        BoTest(boolean spam) throws Exception {
            data=Files.createTempDirectory(Path.of(".selftest"),"m5-");
            Files.writeString(data.resolve("config.properties"),"admin.password=adminTest123\nmulticast.portBase=34000\n"+(spam?"":"spam.burst=10000\nspam.minute=10000\n"));
            server=new ServerCore(data,new ServerCore.Listener() {
                // Log giup truy vet loi nghiep vu.
                public void onLog(String s) { System.out.println("  M5: "+s); }
                // Khong tao Swing trong test.
                public void onThongKe(int u,int o,long t) { }
            });
        }
        // Ba phien va mot phong do A lam chu.
        void batDau() throws Exception {
            server.batDau(0); int c=server.cong(); cho(a.dangKy("localhost",c,"aaa","abcdef123")); cho(b.dangKy("localhost",c,"bbb","abcdef123"));
            aid=cho(a.dangNhap("localhost",c,"aaa","abcdef123")).id; bid=cho(b.dangNhap("localhost",c,"bbb","abcdef123")).id; cho(ad.dangNhap("localhost",c,"admin","adminTest123"));
            room=cho(a.taoPhong("Phòng kiểm duyệt","",true,"",20)); cho(a.vaoPhong(room.id,"")); cho(b.vaoPhong(room.id,""));
        }
        // Ca API du lieu, export, job va action deu phai qua guard ADMIN.
        void tuChoi() throws Exception {
            long truoc=audit("DENIED");
            for (String l:List.of(Protocol.AD_SNAPSHOT,Protocol.AD_READ,Protocol.AD_EXPORT,Protocol.AD_JOB)) { biChan(a.yeuCauHoSo(l,"x","x","0")); }
            var lenh=List.of("USER_ADD","USER_EDIT","USER_LOCK","USER_RESET","USER_DELETE","ROOM_CREATE","ROOM_EDIT","ROOM_STATE","ROOM_DELETE","ROOM_CLEAR","BROADCAST");
            for (String l:lenh) { biChan(a.quanTri().lenh(l,bid,"true","x")); }
            can(audit("DENIED")==truoc+4+lenh.size(),"Thieu audit lenh AD gia mao");
            biChan(a.quanTri().kiemDuyet("MUTE",room.id,ad.nguoiDung().id,60,""));
            biChan(b.quanTri().kiemDuyet("KICK",room.id,aid,0,""));
            biChan(b.quanTri().thanhVien(room.id));
            can(audit("DENIED")==truoc+7+lenh.size(),"Moderation bi tu choi khong ghi dung mot audit");
        }
        // Dang nhap tam duoc mot lan, doi mat khau moi su dung nghiep vu duoc.
        void taiKhoan() throws Exception {
            String id=cho(ad.quanTri().lenh("USER_ADD","tam","Tên tạm","THANH_VIEN","tam123456"));
            biChan(ad.quanTri().lenh("USER_ADD","sai","x","ADMIN","abcdef123"));
            can(cho(ad.quanTri().lay()).lay("USERS").stream().noneMatch(r -> r.get(1).equals("sai")),"Tao loi van de lai tai khoan");
            try (ClientCore c=new ClientCore(new BanNghe())) {
                can(cho(c.dangNhap("localhost",server.cong(),"tam","tam123456")).buocDoiMatKhau,"Khong buoc doi");
                biChan(c.guiTin("Chua doi")); cho(c.doiMatKhau("tam123456","moi123456")); cho(c.vaoPhong("sanh","")); cho(c.guiTin("Da doi"));
                cho(ad.quanTri().lenh("USER_EDIT",id,"Tên đã sửa","Giới thiệu","ADMIN"));
                doi(() -> c.nguoiDung()!=null && c.nguoiDung().vaiTro==VaiTro.ADMIN,4000);
                String mk=cho(ad.quanTri().lenh("USER_RESET",id)); can(mk.length()==10,"Mat khau tam khong 10 ky tu");
                doi(() -> c.nguoiDung()==null,4000); can(cho(c.dangNhap("localhost",server.cong(),"tam",mk)).buocDoiMatKhau,"Reset khong force");
                cho(c.dangXuat()); biChan(c.dangNhap("localhost",server.cong(),"tam",mk));
                String moi=cho(ad.quanTri().lenh("USER_RESET",id)); cho(c.dangNhap("localhost",server.cong(),"tam",moi)); cho(c.doiMatKhau(moi,"moi456789"));
                cho(ad.quanTri().lenh("USER_LOCK",id,"true","Kiểm thử")); doi(() -> c.nguoiDung()==null,4000); biChan(c.dangNhap("localhost",server.cong(),"tam","moi456789"));
                cho(ad.quanTri().lenh("USER_LOCK",id,"false","")); cho(c.dangNhap("localhost",server.cong(),"tam","moi456789")); cho(c.dangXuat());
            }
            cho(ad.quanTri().lenh("USER_DELETE",id,"tam"));
            can(audit("USER_ADD")==1 && audit("USER_RESET")==2,"Audit lap hoac thieu hanh dong");
            can(!Files.readString(data.resolve("audit.log")).contains("tam123456"),"Audit lo mat khau");
        }
        // Tu khoa/xoa/ha quyen bi chan. Admin tao phong chon chu tu dau.
        void rangBuoc() throws Exception {
            String me=ad.nguoiDung().id;
            biChan(ad.quanTri().lenh("USER_LOCK",me,"true","")); biChan(ad.quanTri().lenh("USER_DELETE",me,"admin")); biChan(ad.quanTri().lenh("USER_EDIT",me,"Admin","","THANH_VIEN"));
            biChan(ad.quanTri().lenh("ROOM_DELETE","sanh"));
            String id=cho(ad.quanTri().lenh("ROOM_CREATE",aid,"Phòng admin","Mô tả","false","mk123456","7"));
            cho(ad.quanTri().lenh("ROOM_EDIT",id,"Phòng sửa","Mới","false","KEEP","","9"));
            var r=cho(ad.quanTri().lay()).lay("ROOMS").stream().filter(x -> x.get(0).equals(id)).findFirst().orElseThrow(); can(r.get(12).equals(aid) && r.get(13).equals("9"),"Sai chu/gioi han");
            cho(ad.quanTri().lenh("ROOM_STATE",id,"false")); biChan(b.vaoPhong(id,"mk123456")); cho(ad.quanTri().lenh("ROOM_STATE",id,"true")); cho(ad.quanTri().lenh("ROOM_DELETE",id));
        }
        // Qua UDP that, bao gom mute sticker/file/recall va het han tu dong.
        void kiemDuyet() throws Exception {
            cho(b.guiTin(room.id,"Tin trước mute")); doi(() -> nb.tin.stream().anyMatch(t -> t.noiDung.equals("Tin trước mute")),4000);
            long msg=nb.tin.stream().filter(t -> t.noiDung.equals("Tin trước mute")).findFirst().orElseThrow().msgId;
            cho(a.quanTri().kiemDuyet("MUTE",room.id,bid,1,"Thử hết hạn"));
            biChan(b.guiTin(room.id,"Bi chan")); biChan(b.troChuyen().gui(room.id,"2764","S")); biChan(b.troChuyen().thuHoi(room.id,msg));
            Path f=data.resolve("mute.txt"); Files.writeString(f,"x"); biChan(b.tep().gui(room.id,f)); Thread.sleep(1200); cho(b.guiTin(room.id,"Hết mute"));
            cho(a.quanTri().kiemDuyet("KICK",room.id,bid,0,"")); doi(() -> nb.roi.contains(room.id),4000); cho(b.vaoPhong(room.maPhong,""));
            cho(a.quanTri().kiemDuyet("BAN",room.id,bid,0,"")); biChan(b.vaoPhong(room.maPhong,"")); cho(a.quanTri().kiemDuyet("BAN",room.id,bid,-1,"")); cho(b.vaoPhong(room.maPhong,""));
            cho(a.quanTri().kiemDuyet("TRANSFER",room.id,bid,0,"")); biChan(a.quanTri().kiemDuyet("MUTE",room.id,bid,60,"")); cho(ad.quanTri().kiemDuyet("TRANSFER",room.id,aid,0,""));
            cho(ad.quanTri().lenh("BROADCAST","Chào tất cả")); doi(() -> nb.thongBao.contains("Chào tất cả"),4000);
            can(new KhoLuuTru(data,s -> { }).docTin(room.id).stream().anyMatch(t -> t.loai.equals("Y")),"Broadcast khong luu");
        }
        // Xoa chu phong: chon thanh vien cu, avatar bi xoa, nhan tin cu duoc thay.
        void xoaUser() throws Exception {
            try (ClientCore c=new ClientCore(new BanNghe())) {
                cho(c.dangKy("localhost",server.cong(),"xoa","abcdef123")); String id=cho(c.dangNhap("localhost",server.cong(),"xoa","abcdef123")).id;
                Phong r=cho(c.taoPhong("Phòng giữ lại","",true,"",10)); cho(c.vaoPhong(r.id,"")); cho(a.vaoPhong(r.id,"")); cho(c.guiTin(r.id,"Nội dung giữ lại"));
                cho(ad.quanTri().lenh("USER_DELETE",id,"xoa")); doi(() -> c.nguoiDung()==null,4000);
                var snap=cho(ad.quanTri().lay()); can(snap.lay("ROOMS").stream().anyMatch(v -> v.get(0).equals(r.id) && v.get(12).equals(aid)),"Khong chuyen chu");
                can(new KhoLuuTru(data,s -> { }).docTin(r.id).stream().anyMatch(t -> t.ten.equals("Người dùng đã xóa") && t.noiDung.equals("Nội dung giữ lại")),"Mat tin cu/nhan chua xoa");
                cho(ad.quanTri().lenh("ROOM_DELETE",r.id));
            }
        }
        // Du lieu biet truoc: dem theo snapshot va ca CSV/HTML, tai ZIP qua cua so M4.
        void baoCao() throws Exception {
            try (ClientCore c=new ClientCore(new BanNghe())) {
                cho(c.dangKy("localhost",server.cong(),"ccc","abcdef123")); cho(c.dangKy("localhost",server.cong(),"ddd","abcdef123"));
                cho(c.dangNhap("localhost",server.cong(),"ccc","abcdef123")); cho(c.vaoPhong(room.id,"")); cho(c.dangXuat());
            }
            String id=cho(ad.quanTri().lenh("ROOM_CREATE",bid,"Phòng báo cáo","",true+"","","20"));
            cho(ad.quanTri().lenh("ROOM_CLEAR",room.id));
            AnhChupQuanTri truoc=cho(ad.quanTri().lay()); long so=Long.parseLong(stat(truoc,"Tổng tin nhắn"));
            for (int i=0;i<40;i++) { cho(a.guiTin(room.id,"Báo cáo "+i)); }
            Path f=data.resolve("Tệp mẫu.txt"); Files.writeString(f,"Tiếng Việt có dấu"); cho(a.tep().gui(room.id,f)); cho(b.tep().gui(room.id,f));
            AnhChupQuanTri snap=cho(ad.quanTri().lay()); can(stat(snap,"Tổng tài khoản").equals("5") && stat(snap,"Tổng phòng").equals("3"),"Sai tong user/phong");
            can(Long.parseLong(stat(snap,"Tổng tin nhắn"))==so+42 && stat(snap,"Tổng tệp").equals("2"),"Sai tong tin/tep");
            long bytes=Files.size(f); can(Long.parseLong(stat(snap,"Dung lượng tệp"))==2*bytes,"Sai tong bytes");
            var userA=snap.lay("USERS").stream().filter(r -> r.get(1).equals("aaa")).findFirst().orElseThrow();
            can(userA.get(9).equals("1") && userA.get(10).equals(""+bytes),"Sai tep/bytes theo user");
            var roomRow=snap.lay("ROOMS").stream().filter(r -> r.get(0).equals(room.id)).findFirst().orElseThrow();
            can(roomRow.get(6).equals("3") && roomRow.get(8).equals("2") && roomRow.get(9).equals(""+(2*bytes)),"Sai thanh vien offline/tep/bytes phong");
            can(snap.lay("MEMBERS").stream().anyMatch(r -> r.get(0).equals(room.id) && r.get(3).equals("ccc") && r.get(6).equals("false")),"Mat member offline");
            Path out=cho(ad.quanTri().xuat(data.resolve("tai-bao-cao"),false));
            String html=Files.readString(out.resolve("tong_hop.html")); can(html.contains("Số người dùng theo từng phòng"),"Thieu bang yeu cau thay");
            for (var r:snap.lay("ROOMS")) { can(html.contains("<td>"+XuatBaoCao.e(r.get(1))+"</td><td>"+XuatBaoCao.e(r.get(2))+"</td><td>"+r.get(6)+"</td>"),"Sai user theo phong"); }
            byte[] csv=Files.readAllBytes(out.resolve("nguoidung.csv")); can(csv[0]==(byte)0xef && csv[1]==(byte)0xbb && csv[2]==(byte)0xbf,"Thieu BOM");
            String phong=Files.readString(out.resolve("phong.csv")); for (var r:snap.lay("ROOMS")) { can(phong.contains(XuatBaoCao.oCsv(r.get(1))+","+XuatBaoCao.oCsv(r.get(2))),"CSV thieu phong"); }
            can(Files.readAllLines(out.resolve("nguoidung.csv"),StandardCharsets.UTF_8).size()==6,"CSV so user sai");
            can(Files.readString(out.resolve("nguoidung.csv")).contains("\"aaa\",\"aaa\",\"THANH_VIEN\",\"Online\""),"CSV sai dinh danh/vai/trang thai");
            can(Files.readString(out.resolve("thanhvien_theophong.csv")).contains("\"ccc\",\"THANH_VIEN\",\"false\""),"CSV mat member offline");
            can(Files.readString(out.resolve("tin_theo_ngay.csv")).contains("\"2\",\"0\""),"CSV theo ngay thieu hai tep");
            Path audit=cho(ad.quanTri().xuat(data.resolve("tai-bao-cao"),true)); can(Files.exists(audit.resolve("nhatky.csv")),"Khong tai duoc audit");
            Path report=server.xuatBaoCao().get(12,TimeUnit.SECONDS); can(Files.exists(report.resolve("tong_hop.html")),"Nut Server khong xuat");
            cho(ad.quanTri().lenh("ROOM_DELETE",id));
        }
        // Hai admin cap nhat khac user, xuat du lieu trong khi chat van duoc dap ung.
        void dongThoi() throws Exception {
            String id=cho(ad.quanTri().lenh("USER_ADD","admin2","Admin hai","ADMIN","abcdef123"));
            try (ClientCore c=new ClientCore(new BanNghe())) {
                cho(c.dangNhap("localhost",server.cong(),"admin2","abcdef123")); cho(c.doiMatKhau("abcdef123","newabcdef"));
                var f1=ad.quanTri().lenh("USER_EDIT",aid,"An","","THANH_VIEN"); var f2=c.quanTri().lenh("USER_EDIT",bid,"Bình","","THANH_VIEN"); cho(CompletableFuture.allOf(f1,f2));
                var report=c.quanTri().xuat(data.resolve("dong-thoi"),false); long start=System.nanoTime(); cho(a.guiTin(room.id,"Chat khi xuất báo cáo"));
                doi(() -> nb.tin.stream().anyMatch(t -> t.noiDung.equals("Chat khi xuất báo cáo")),4000); long ms=(System.nanoTime()-start)/1000000;
                can(ms<3000,"Chat bi cham khi xuat"); System.out.println("  Do tre chat khi xuat: "+ms+" ms"); cho(report);
            }
            cho(ad.quanTri().lenh("USER_DELETE",id,"admin2"));
        }
        // Kiem duyet va khoa phai ton tai khi Server khoi dong lai.
        void khoiDongLai() throws Exception {
            cho(ad.quanTri().kiemDuyet("MUTE",room.id,aid,0,"")); cho(ad.quanTri().kiemDuyet("BAN",room.id,bid,0,""));
            for (ClientCore c:List.of(a,b,ad)) { cho(c.dangXuat()); } server.close(); server.batDau(0);
            cho(ad.dangNhap("localhost",server.cong(),"admin","adminTest123")); cho(a.dangNhap("localhost",server.cong(),"aaa","abcdef123")); cho(b.dangNhap("localhost",server.cong(),"bbb","abcdef123"));
            cho(a.vaoPhong(room.maPhong,"")); biChan(a.guiTin(room.id,"Mute còn")); biChan(b.vaoPhong(room.maPhong,""));
            cho(ad.quanTri().kiemDuyet("MUTE",room.id,aid,-1,"")); cho(a.guiTin(room.id,"Đã gỡ cấm"));
        }
        // Ba lan vuot burst dan den tu mute toan he thong.
        void spam() throws Exception {
            for (int i=0;i<5;i++) { cho(a.guiTin(room.id,"Spam "+i)); }
            for (int i=0;i<3;i++) { biChan(a.guiTin(room.id,"Quá nhanh "+i)); }
            doi(() -> a.quanTri().camChat(room.id)>System.currentTimeMillis(),4000); can(audit("AUTO_MUTE")==1,"Spam khong ghi audit"); biChan(a.guiTin("Sảnh cũng bị mute"));
        }
        // Dem lenh dung cot, khong tim chuoi tu do trong chi tiet.
        long audit(String action) throws Exception { return new NhatKy(data,5*1024*1024).doc().stream().filter(r -> r.get(2).equals(action)).count(); }
        // Dong socket va luong ngay ca khi assertion that bai.
        public void close() { a.close(); b.close(); ad.close(); server.close(); }
    }
    // Lay thong ke theo ten thay vi chi so co the thay doi.
    private static String stat(AnhChupQuanTri a,String ten) { return a.lay("STATS").stream().filter(r -> r.get(0).equals(ten)).findFirst().orElseThrow().get(1); }
    // ===== AN TOAN DINH DANG =====
    private static void escape() throws Exception {
        Path thu=Files.createTempDirectory(Path.of(".selftest"),"csv-"); Path p=thu.resolve("test.csv");
        XuatBaoCao.csv(p,List.of("Tên"),List.of(List.of("=cmd|..."),List.of("An, \"Nhi\"\nDòng 2")));
        String s=Files.readString(p); can(s.startsWith("\ufeff") && s.contains("\"'=cmd|...\"") && s.contains("An, \"\"Nhi\"\"\nDòng 2"),"CSV escape sai");
        for (String v:List.of("=1","+1","-1","@cmd","  =1","\t=1")) { can(XuatBaoCao.oCsv(v).startsWith("\"'"),"CSV injection: "+v); }
        can(XuatBaoCao.e("<script>&\"'").equals("&lt;script&gt;&amp;&quot;&#39;"),"HTML escape sai");
        AnhChupQuanTri a=new AnhChupQuanTri(System.currentTimeMillis(),Map.of());
        Path x=XuatBaoCao.xuat(a,thu,false),y=XuatBaoCao.xuat(a,thu,false); can(!x.equals(y),"Ghi de bao cao");
        can(!XuatBaoCao.html(a).contains("<script"),"HTML co script");
    }
    // Moi su kien co mot dong vat ly du chuoi dau vao co ky tu dac biet.
    private static void nhatKy() throws Exception {
        Path thu=Files.createTempDirectory(Path.of(".selftest"),"audit-"); NhatKy n=new NhatKy(thu,250);
        n.ghi("admin","TEST","user","Hai\ndòng|dấu"); can(Files.readAllLines(thu.resolve("audit.log")).size()==1,"Audit tach thanh nhieu dong");
        can(n.doc().get(0).get(4).equals("Hai\ndòng|dấu"),"Audit mat escape");
        for (int i=0;i<20;i++) { n.ghi("admin","TEST","user","x".repeat(60)); } can(Files.exists(thu.resolve("audit.1.log")),"Chua rotate");
    }
    // ZIP doc hai hoac dang do khong duoc de lai thu muc giai nen nua chung.
    private static void zipAnToan() throws Exception {
        Path thu=Files.createTempDirectory(Path.of(".selftest"),"zip-admin-");
        for (var entries:List.of(List.of("../ngoai.txt"),List.of("nhatky.csv","khong-hop-le.txt"),List.of("nguoidung.csv"),List.<String>of())) {
            Path zip=Files.createTempFile(thu,"test-",".zip");
            try (var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
                for (String ten:entries) { out.putNextEntry(new java.util.zip.ZipEntry(ten)); out.write("Dữ liệu".getBytes(StandardCharsets.UTF_8)); out.closeEntry(); }
            }
            try { QuanTriClient.giaiNen(zip,thu); throw new AssertionError("Chap nhan ZIP loi"); }
            catch (java.io.IOException dung) { }
            try (var ds=Files.list(thu)) { can(ds.noneMatch(Files::isDirectory),"ZIP loi de lai thu muc/tam"); }
        }
        Path zip=thu.resolve("duplicate.zip");
        try (var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            for (String ten:List.of("nhatky.csv","phongx.csv")) { out.putNextEntry(new java.util.zip.ZipEntry(ten)); out.write(1); out.closeEntry(); }
        }
        // Hai ten co cung do dai de sua raw ZIP thanh duplicate ma JDK writer thuong chan.
        byte[] raw=Files.readAllBytes(zip),tu="phongx.csv".getBytes(StandardCharsets.US_ASCII),den="nhatky.csv".getBytes(StandardCharsets.US_ASCII);
        for (int i=0;i<=raw.length-tu.length;i++) { if (Arrays.equals(Arrays.copyOfRange(raw,i,i+tu.length),tu)) { System.arraycopy(den,0,raw,i,den.length); } }
        Files.write(zip,raw);
        try { QuanTriClient.giaiNen(zip,thu); throw new AssertionError("Chap nhan duplicate ZIP"); }
        catch (java.io.IOException dung) { }
        try (var ds=Files.list(thu)) { can(ds.noneMatch(Files::isDirectory),"Duplicate de lai tam"); }
    }
    /** Vai tro: Thu su kien thread-safe. Ai goi: ClientCore. Goi: collection RAM. */
    private static final class BanNghe implements Listener {
        final List<TinNhan> tin=new CopyOnWriteArrayList<>(); final Set<String> roi=ConcurrentHashMap.newKeySet(),thongBao=ConcurrentHashMap.newKeySet();
        // Ghi tin multicast de do do tre va thong bao.
        public void onTinNhan(TinNhan t,boolean toi) { tin.add(t); }
        // Danh dau room da bi kick/ban.
        public void onRoiPhong(String r,String lydo) { roi.add(r); }
        // Thong bao admin den moi Client online.
        public void onThongBaoQuanTri(String text) { thongBao.add(text); }
        // Khong tao UI.
        public void onHeThong(String s) { }
        // Khong tao UI.
        public void onDanhSachOnline(List<String> ds) { }
        // Loi du kien duoc kiem tra boi future.
        public void onLoi(String s) { }
        // Ngat cuong che duoc kiem tra qua Core.
        public void onMatKetNoi(String s) { }
    }
}
