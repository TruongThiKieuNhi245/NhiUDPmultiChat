package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.common.Protocol;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.List;

/** Vai tro: Guard va loi luu tru kho tai khoan/kiem duyet. Ai goi: AdminSelfTest. Goi: manager that va fixture tam. */
public final class QuanTriBoSungSelfTest {
    // Khong khoi tao lop tien ich.
    private QuanTriBoSungSelfTest() { }

    // Last-active-admin phai duoc bao ve ngay ca khi caller khong phai admin cuoi.
    public static void baoVeAdmin() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"admin-guard-");
        QuanLyNguoiDung q=new QuanLyNguoiDung(new KhoLuuTru(d,s -> { }));
        q.dangKy("nguoi_test","abcdef123");
        var actor=q.dangNhap("nguoi_test","abcdef123",new InetSocketAddress("localhost",39101));
        String admin=q.tatCa().stream().filter(n -> n.vaiTro==VaiTro.ADMIN).findFirst().orElseThrow().id;
        chan(() -> q.khoa(actor,admin,true,""));
        chan(() -> q.xoaQuanTri(actor,admin));
        chan(() -> q.suaQuanTri(actor,admin,"Admin","","THANH_VIEN"));
        can(q.lay(admin).vaiTro==VaiTro.ADMIN && !q.biKhoa(admin),"Mat admin hoat dong cuoi");
    }

    // Lam hong file tam cua fixture, quan sat RAM va tep that truoc/sau loi.
    public static void rollback() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"admin-rollback-");
        KhoLuuTru k=new KhoLuuTru(d,s -> { }); QuanLyNguoiDung q=new QuanLyNguoiDung(k);
        var ad=q.dangNhap("admin","admin123",new InetSocketAddress("localhost",39102));
        var n=q.dangKy("nguoi_test","abcdef123");
        Path chan=d.resolve("accounts.properties.tmp"); Files.createDirectory(chan);
        byte[] users=Files.readAllBytes(d.resolve("users.csv"));
        chan(() -> q.khoa(ad,n.id,true,"Lỗi đĩa"));
        can(!q.biKhoa(n.id),"Khoa loi van doi RAM");
        chan(() -> q.taoQuanTri(ad,"loi_test","Tên thử","ADMIN","abcdef123"));
        can(q.tatCa().stream().noneMatch(u -> u.tenDangNhap.equals("loi_test")),"Tao loi de lai user");
        can(java.util.Arrays.equals(users,Files.readAllBytes(d.resolve("users.csv"))),"Tao loi khong rollback CSV");
        Files.delete(chan);
        Path chanUser=d.resolve("users.csv.tmp"); Files.createDirectory(chanUser);
        byte[] accounts=Files.readAllBytes(d.resolve("accounts.properties"));
        chan(() -> q.datLai(ad,n.id));
        can(!q.lay(n.id).buocDoiMatKhau,"Reset loi van force");
        can(java.util.Arrays.equals(accounts,Files.readAllBytes(d.resolve("accounts.properties"))),"Reset loi khong rollback flags");
        Files.delete(chanUser);
        var p=q.dangNhap("nguoi_test","abcdef123",new InetSocketAddress("localhost",39103)); q.dangXuat(p);
        String mk=q.datLai(ad,n.id);
        QuanLyNguoiDung nap=new QuanLyNguoiDung(new KhoLuuTru(d,s -> { }));
        can(nap.dangNhap("nguoi_test",mk,new InetSocketAddress("localhost",39104)).nguoi.buocDoiMatKhau,"Reset khong persist");
        q.khoa(ad,n.id,true,"Khóa sau test");
        can(new QuanLyNguoiDung(new KhoLuuTru(d,s -> { })).biKhoa(n.id),"Lock khong persist");
    }

    // Dung moc thoi gian truyen vao de test 20/phut khong phai doi mot phut.
    public static void spamPhut() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"spam-phut-");
        KhoLuuTru k=new KhoLuuTru(d,s -> { }); QuanLyNguoiDung q=new QuanLyNguoiDung(k);
        q.dangKy("nguoi_test","abcdef123");
        var p=q.dangNhap("nguoi_test","abcdef123",new InetSocketAddress("localhost",39105));
        NhatKy audit=new NhatKy(d,5242880); KiemDuyet m=new KiemDuyet(k,audit);
        long luc=System.currentTimeMillis();
        for (int i=0;i<20;i++) { can(m.choTin(p,luc+i*2500),"Chan truoc 20 tin/phut"); }
        for (int i=0;i<3;i++) { can(!m.choTin(p,luc+50000+i),"Cho vuot 20 tin/phut"); }
        can(m.mute("sanh",p.nguoi.id)>luc,"Khong auto mute");
        can(audit.doc().stream().filter(r -> r.get(2).equals("AUTO_MUTE")).count()==1,"Audit auto mute lap");
        Files.createDirectory(d.resolve("moderation.properties.tmp"));
        chan(() -> m.dat("sanh",p.nguoi.id,"BAN",0,""));
        can(m.den("sanh",p.nguoi.id,"BAN")==0,"Ban ghi loi van ton tai RAM");
        Files.delete(d.resolve("moderation.properties.tmp"));
    }
    // Phat lai requestId nhu mat RESPONSE: CRUD/audit/broadcast khong duoc lap.
    public static void lenhTrung() throws Exception {
        Path d=Files.createTempDirectory(Path.of(".selftest"),"admin-trung-");
        Files.writeString(d.resolve("config.properties"),"admin.password=adminTest123\nmulticast.portBase=36000\n");
        ServerCore s=new ServerCore(d,new ServerCore.Listener() {
            public void onLog(String text) { }
            public void onThongKe(int u,int o,long t) { }
        });
        try {
            s.batDau(0);
            var ad=s.nguoiDung.dangNhap("admin","adminTest123",new InetSocketAddress("localhost",39111));
            String[] add={Protocol.AD_ACTION,java.util.UUID.randomUUID().toString(),ad.token,"USER_ADD","nguoi_test","Tên thử","THANH_VIEN","abcdef123"};
            String id=s.quanTri.xuLy(ad,add)[0]; can(s.quanTri.xuLy(ad,add)[0].equals(id),"Lap CRUD tao user moi");
            can(s.quanTri.audit.doc().stream().filter(r -> r.get(2).equals("USER_ADD")).count()==1,"Lap audit CRUD");
            var user=s.nguoiDung.dangNhap("nguoi_test","abcdef123",new InetSocketAddress("localhost",39112));
            String[] deny={Protocol.AD_ACTION,java.util.UUID.randomUUID().toString(),user.token,"USER_LOCK",ad.nguoi.id,"true",""};
            chan(() -> s.quanTri.xuLy(user,deny)); chan(() -> s.quanTri.xuLy(user,deny));
            can(s.quanTri.audit.doc().stream().filter(r -> r.get(2).equals("DENIED")).count()==1,"Lap audit DENIED cung request");
            var r=s.phong.tao(ad,"Phòng lỗi","",true,"",20);
            Files.createDirectory(d.resolve("rooms").resolve(r.id+".log"));
            String[] bc={Protocol.AD_ACTION,java.util.UUID.randomUUID().toString(),ad.token,"BROADCAST","Thông báo thử"};
            String k=s.quanTri.xuLy(ad,bc)[0]; long tin=s.phong.tongTin();
            can(k.contains("1 phòng lỗi lưu") && tin==1,"Broadcast bao sai partial");
            can(s.quanTri.xuLy(ad,bc)[0].equals(k) && s.phong.tongTin()==tin,"Broadcast retry tao tin trung");
        } finally { s.close(); }
    }
    // Chap nhan loi nghiep vu hoac I/O nhung khong nuot assertion.
    private static void chan(Viec v) throws Exception {
        try { v.chay(); } catch (IllegalArgumentException | IOException dung) { return; }
        throw new AssertionError("Chap nhan thao tac phai bi chan");
    }
    // Assertion gon, co thong bao ro.
    private static void can(boolean dung,String loi) { if (!dung) { throw new AssertionError(loi); } }
    @FunctionalInterface private interface Viec { void chay() throws Exception; }

}
