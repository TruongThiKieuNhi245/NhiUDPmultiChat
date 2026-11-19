package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Vai tro: API admin bat dong bo, mute va tai/giai nen bao cao qua UDP M4. Ai goi: QuanTriFrame, KiemDuyetUI. Goi: ClientCore, TepClient, Listener. */
public final class QuanTriClient {
    private final ClientCore core; private final Listener nghe;
    private final Map<String,Long> mute=new ConcurrentHashMap<>();
    private Runnable thayDoi=() -> { };
    // Core dung cung mot manager trong mot cua so Client.
    QuanTriClient(ClientCore core,Listener nghe) { this.core=core; this.nghe=nghe; }
    // ===== SNAPSHOT VA LENH =====
    public void khiDoi(Runnable viec) { thayDoi=viec; }
    // Khong chan EDT, lay cac trang tu cung mot snapshot.
    public CompletableFuture<AnhChupQuanTri> lay() { return CompletableFuture.supplyAsync(() -> {
        try {
            String[] p=cho(core.yeuCauHoSo(Protocol.AD_SNAPSHOT)); Map<String,List<List<String>>> bang=new LinkedHashMap<>();
            for (String ten:List.of("STATS","USERS","ROOMS","MEMBERS","DAYS","AUDIT")) { bang.put(ten,bang(p[3],ten)); }
            return new AnhChupQuanTri(Long.parseLong(p[4]),bang);
        } catch (Exception e) { throw new CompletionException(e); }
    }); }
    // Bang co gioi han, khong de Server la tra so dong vo han.
    private List<List<String>> bang(String id,String ten) throws Exception {
        List<List<String>> ds=new ArrayList<>(); int so=1;
        for (int i=0;i<so;i++) {
            String[] g=cho(core.yeuCauHoSo(Protocol.AD_READ,id,ten,""+i)); so=Integer.parseInt(g[3]);
            if (so>10000) { throw new IOException("Bảng quá lớn để mở trong Client."); }
            if (so>0) { ds.add(List.of(Arrays.copyOfRange(g,4,g.length))); }
        }
        return List.copyOf(ds);
    }
    // Server kiem tra quyen moi lenh, UI an menu chi de de su dung.
    public CompletableFuture<String> lenh(String ten,String... p) { return core.yeuCauHoSo(Protocol.AD_ACTION,Goi.noi(new String[]{ten},p)).thenApply(g -> g[3]); }
    // Cung API cho chu phong va admin, quyen cu the duoc kiem tra tren Server.
    public CompletableFuture<Void> kiemDuyet(String lenh,String room,String user,long giay,String lyDo) { return core.yeuCauHoSo(Protocol.MOD,lenh,room,user,""+giay,lyDo).thenApply(g -> null); }
    // Danh sach ca nguoi chua vao de go ban cho thanh vien da bi duoi.
    public CompletableFuture<List<List<String>>> thanhVien(String room) { return CompletableFuture.supplyAsync(() -> {
        try {
            List<List<String>> ds=new ArrayList<>(); int so=1;
            for (int i=0;i<so;i++) { String[] g=cho(core.yeuCauHoSo(Protocol.MOD_LIST,room,""+i)); so=Integer.parseInt(g[3]); if (so>10000) { throw new IOException("Danh sách quá lớn."); } if (so>0) { ds.add(List.of(Arrays.copyOfRange(g,4,g.length))); } } return ds;
        } catch (Exception e) { throw new CompletionException(e); }
    }); }
    // ===== MUTE VA SU KIEN REALTIME =====
    boolean nhan(String[] g) {
        switch (g[0]) {
            case Protocol.AD_CHANGED: thayDoi.run(); return true;
            case Protocol.MUTE_STATE: if (g.length==3) { long n=Long.parseLong(g[2]); mute.put(g[1],n); nghe.onCamChat(g[1],n); } return true;
            case Protocol.FORCE_OUT: if (g.length==2) { core.close(); nghe.onMatKetNoi(g[1]); } return true;
            case Protocol.ANNOUNCE: if (g.length==2) { nghe.onThongBaoQuanTri(g[1]); } return true;
            case Protocol.USER_DELETED:
                if (g.length==2) { var n=new NguoiDung(g[1],"da_xoa","Người dùng đã xóa",VaiTro.THANH_VIEN,""); core.hoSo().nhan(n); nghe.onHoSo(n); } return true;
            default: return false;
        }
    }
    // UI chi doc cache, Server van kiem tra lai.
    public long camChat(String room) { long n=mute.getOrDefault(room,0L); return n>System.currentTimeMillis()?n:0; }
    // Xoa du lieu phien, callback UI duoc giu cho lan dang nhap tiep.
    void xoa() { mute.clear(); }
    // ===== BAO CAO ZIP QUA CO CHE TEP =====
    public CompletableFuture<Path> xuat(Path reports,boolean audit) { return CompletableFuture.supplyAsync(() -> {
        try {
            String id=cho(core.yeuCauHoSo(Protocol.AD_EXPORT,audit?"AUDIT":"REPORT"))[3]; String[] g;
            long het=System.nanoTime()+TimeUnit.MINUTES.toNanos(2);
            do { g=cho(core.yeuCauHoSo(Protocol.AD_JOB,id)); if (g[3].equals("WAIT")) { Thread.sleep(300); } } while (g[3].equals("WAIT") && System.nanoTime()<het);
            if (!g[3].equals("DONE")) { throw new IOException("Tạo báo cáo quá thời gian chờ."); }
            TepTin t=TepTin.doc("bao_cao",core.nguoiDung().id,g[4],System.currentTimeMillis()); Files.createDirectories(reports);
            Path zip=core.tep().tai(t,reports.resolve(t.ten())).get(3,TimeUnit.MINUTES); return giaiNen(zip,reports);
        } catch (Exception e) { throw new CompletionException(e); }
    }); }
    // Zip chi duoc chua nam file bao cao hoac audit; chan zip slip va bom giai nen.
    public static Path giaiNen(Path zip,Path reports) throws IOException {
        Path thu=Files.createTempDirectory(reports,"baocao_").toAbsolutePath().normalize(); long tong=0; int so=0;
        Set<String> daCo=new HashSet<>();
        try (ZipInputStream in=new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e; byte[] dem=new byte[8192];
            while ((e=in.getNextEntry())!=null) {
                if (++so>6 || e.isDirectory() || !daCo.add(e.getName()) || !Set.of("nguoidung.csv","phong.csv","thanhvien_theophong.csv","tin_theo_ngay.csv","tong_hop.html","nhatky.csv").contains(e.getName())) { throw new IOException("Gói báo cáo không hợp lệ."); }
                Path dich=thu.resolve(e.getName()).normalize(); if (!dich.startsWith(thu)) { throw new IOException("Đường dẫn báo cáo không hợp lệ."); }
                Path tam=Files.createTempFile(thu,"giai-",".tmp");
                try (var out=Files.newOutputStream(tam)) { int n; while ((n=in.read(dem))!=-1) { tong+=n; if (tong>50*1024*1024) { throw new IOException("Báo cáo giải nén quá lớn."); } out.write(dem,0,n); } }
                Files.move(tam,dich);
            }
            if (!daCo.equals(Set.of("nhatky.csv")) && !daCo.equals(Set.of("nguoidung.csv","phong.csv","thanhvien_theophong.csv","tin_theo_ngay.csv","tong_hop.html"))) { throw new IOException("Gói báo cáo thiếu tệp."); }
        } catch (IOException | RuntimeException loi) {
            // Chi don thu muc vua tao, khong cham ZIP hay bao cao co san cua nguoi dung.
            try (var ds=Files.walk(thu)) {
                for (Path p:ds.sorted(Comparator.reverseOrder()).toList()) { Files.delete(p); }
            } catch (IOException don) { loi.addSuppressed(don); }
            throw loi;
        }
        return thu;
    }
    // Chi worker goi ham cho phan hoi.
    private static String[] cho(CompletableFuture<String[]> f) throws Exception { return f.get(12,TimeUnit.SECONDS); }
}
