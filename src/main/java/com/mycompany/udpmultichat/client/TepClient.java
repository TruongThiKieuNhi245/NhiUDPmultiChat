package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Vai tro: Hang doi hai upload, tai/thumbnail, huy va tien do. Ai goi: TepGiaoDien, SelfTest. Goi: ClientCore, TruyenTep, AnhTep. */
public final class TepClient {
    private final ClientCore core;
    private final Listener nghe;
    private final ThreadPoolExecutor guiNen, taiNen;
    private final Map<String,Viec> viec=new ConcurrentHashMap<>();
    private final Map<String,BufferedImage> anh=Collections.synchronizedMap(new LinkedHashMap<>());
    private final Set<String> choAnh=ConcurrentHashMap.newKeySet();
    private volatile TruyenTep truyen;
    private long max=Protocol.TEP_TOI_DA; private double mat; private long seed;
    // Hai upload duoc chay song song; toi da 16 tep cho, tranh hang doi vo han.
    TepClient(ClientCore core,Listener nghe) { this.core=core; this.nghe=nghe; guiNen=taoNhomLuong(2,"tep-upload"); taiNen=taoNhomLuong(2,"tep-download"); }
    // Cau hinh mat goi co seed chi phuc vu kiem thu.
    public void moPhongMatGoi(double mat,long seed) { if (truyen!=null) { throw new IllegalStateException("Đã mở kênh tệp"); } this.mat=mat; this.seed=seed; }
    // Khoi tao engine sau khi hoi gioi han Server; khong bao gio goi tren EDT.
    private synchronized TruyenTep kenh() throws Exception {
        if (truyen==null) {
            String[] p=cho(core.yeuCauHoSo(Protocol.TEP_INFO)); max=Long.parseLong(p[3]);
            truyen=new TruyenTep(0,mat,seed,Integer.parseInt(p[5]));
        }
        return truyen;
    }
    // ===== GUI VA HANG DOI =====
    public CompletableFuture<TepTin> gui(String room,Path nguon) { return guiCoMa(room,nguon,taoMa()); }
    // Gui lai giu ma nghiep vu, con UUID cua luot DATA duoc Server cap rieng.
    private CompletableFuture<TepTin> guiCoMa(String room,Path nguon,String ma) {
        var n=core.nguoiDung(); if (n==null || !core.sanSang()) { return CompletableFuture.failedFuture(new IOException("Chưa kết nối Server.")); }
        Viec v=new Viec(ma,room,nguon,true);
        try {
            long size=Files.size(nguon); TepTin.kiemTra(size,truyen==null?100*1024*1024L:max);
            v.t=new TepTin(UUID.randomUUID().toString(),room,n.id,TepTin.tenAnToan(nguon.getFileName().toString()),size,0,System.currentTimeMillis());
            if (viec.size()>=100) { viec.entrySet().removeIf(e -> e.getValue().ketQua.isDone()); }
            if (viec.size()>=100) { throw new IOException("Đang có quá nhiều lượt truyền."); }
            dangKy(v); bao(v,"QUEUE",0,""); guiNen.execute(() -> guiThat(v));
        } catch (IOException | RuntimeException e) { viec.remove(v.ma); v.ketQua.completeExceptionally(e); }
        return v.ketQua;
    }
    // CRC va thumbnail deu tinh tren luong nen; giu nguyen ma Client khi thu lai.
    private void guiThat(Viec v) {
        Path nho=null;
        try {
            TruyenTep k=kenh(); kiemTra(v); TepTin.kiemTra(Files.size(v.duong),max);
            long crc=TruyenTep.crc(v.duong); Path tam=Files.createTempFile("udp-thumb-",".jpg");
            nho=AnhTep.tao(v.duong,tam); if (nho==null) { Files.deleteIfExists(tam); }
            long nhoSize=nho==null?0:Files.size(nho),nhoCrc=nho==null?0:TruyenTep.crc(nho);
            String[] p=cho(core.yeuCauHoSo(Protocol.TEP_INIT,v.room,v.t.ten(),Long.toString(v.t.kichThuoc()),Long.toString(crc),Integer.toString(k.cong()),Long.toString(nhoSize),Long.toString(nhoCrc),v.ma));
            v.id=UUID.fromString(p[3]); v.t=new TepTin(p[3],v.room,v.t.user(),v.t.ten(),v.t.kichThuoc(),crc,v.t.luc());
            if (p.length>6 && p[6].equals("DONE")) { bao(v,"DONE",v.t.kichThuoc(),""); v.ketQua.complete(v.t); return; }
            InetSocketAddress ban=new InetSocketAddress(core.diaChiServer().getAddress(),Integer.parseInt(p[5])); kiemTra(v); v.batDau=System.nanoTime();
            CompletableFuture<Path> anhGui=CompletableFuture.completedFuture(null);
            if (nho!=null) { v.anhId=UUID.fromString(p[4]); anhGui=k.gui(v.anhId,ban,nho,nhoSize,nhoCrc,n -> { }); }
            bao(v,"SEND",0,""); kiemTra(v); choTep(k.gui(v.id,ban,v.duong,v.t.kichThuoc(),crc,n -> bao(v,"SEND",n,""))); choTep(anhGui); kiemTra(v);
            cho(core.yeuCauHoSo(Protocol.TEP_DONE,v.id.toString())); bao(v,"DONE",v.t.kichThuoc(),""); v.ketQua.complete(v.t);
        } catch (Exception e) { thatBai(v,e); }
        finally { if (nho!=null) { try { Files.deleteIfExists(nho); } catch (IOException ignored) { } } }
    }
    // ===== TAI XUONG VA ANH XEM TRUOC =====
    public CompletableFuture<Path> tai(TepTin t,Path dich) {
        Viec v=new Viec(taoMa(),t.room(),dich,false); v.t=t; CompletableFuture<Path> kq=new CompletableFuture<>();
        try { dangKy(v); taiNen.execute(() -> {
            try { Path p=taiThat(v,false); bao(v,"DONE",t.kichThuoc(),""); v.ketQua.complete(t); kq.complete(p); }
            catch (Exception e) { thatBai(v,e); kq.completeExceptionally(e); }
        }); } catch (RejectedExecutionException e) { viec.remove(v.ma); kq.completeExceptionally(new IOException("Đang có quá nhiều lượt tải.")); }
        return kq;
    }
    // Client dang ky receiver truoc READY, tep dich doi ten sau khi CRC dung.
    private Path taiThat(Viec v,boolean thumbnail) throws Exception {
        TruyenTep k=kenh(); kiemTra(v);
        String[] p=cho(core.yeuCauHoSo(Protocol.TEP_GET,v.t.id(),Integer.toString(k.cong()),Boolean.toString(thumbnail)));
        v.id=UUID.fromString(p[3]); long size=Long.parseLong(p[4]),crc=Long.parseLong(p[5]);
        if (size<1 || size>(thumbnail?65536:max)) { throw new IOException("Kích thước tệp nhận không hợp lệ."); }
        Files.createDirectories(v.duong.toAbsolutePath().getParent());
        Path tam=v.duong.toAbsolutePath().resolveSibling("."+v.id+".part"); v.tam=tam; v.batDau=System.nanoTime();
        InetSocketAddress ban=new InetSocketAddress(core.diaChiServer().getAddress(),Integer.parseInt(p[6]));
        CompletableFuture<Path> nhan=k.nhan(v.id,ban,tam,size,crc,n -> { if (!thumbnail) { bao(v,"DOWNLOAD",n,""); } });
        cho(core.yeuCauHoSo(Protocol.TEP_READY,v.id.toString())); choTep(nhan); kiemTra(v);
        Path dich=v.duong.toAbsolutePath(); if (Files.exists(dich)) { dich=TepTin.tenChuaCo(dich.getParent(),dich.getFileName().toString()); }
        Files.move(tam,dich); // Khong REPLACE_EXISTING: tuyet doi khong ghi de tep da co.
        v.duong=dich; return dich;
    }
    // Cache anh toi da 100 muc; loi/anh gia chi hien the tep thuong.
    public void napAnh(TepTin t) {
        if (anh.containsKey(t.id()) || choAnh.size()>=32 || !choAnh.add(t.id())) { return; }
        try { taiNen.execute(() -> {
            Viec v=null;
            try {
                Path tmp=Files.createTempDirectory("udp-preview-").resolve("preview.jpg"); v=new Viec(taoMa(),t.room(),tmp,false); v.t=t;
                Path p=taiThat(v,true); BufferedImage h=ImageIO.read(p.toFile());
                if (h!=null && h.getWidth()<=240 && h.getHeight()<=240) {
                    synchronized (anh) { if (anh.size()>=100) { anh.remove(anh.keySet().iterator().next()); } anh.put(t.id(),h); }
                    nghe.onAnhTep(t.room(),t.id());
                }
            } catch (Exception ignored) { /* Khong co thumbnail: hien the tep thuong. */ }
            finally {
                choAnh.remove(t.id()); if (v!=null) { try { if (v.tam!=null) { Files.deleteIfExists(v.tam); } Files.deleteIfExists(v.duong); Files.deleteIfExists(v.duong.getParent()); } catch (IOException ignored) { } }
            }
        }); } catch (RejectedExecutionException e) { choAnh.remove(t.id()); }
    }
    // Doc cache RAM khong chan giao dien.
    public BufferedImage anh(String id) { return anh.get(id); }
    // ===== HUY, GUI LAI VA DON DEP =====
    public void huy(String ma) {
        Viec v=viec.get(ma); if (v==null) { return; } v.huy=true;
        if (v.id!=null && truyen!=null) { truyen.huy(v.id); if (v.anhId!=null) { truyen.huy(v.anhId); } core.yeuCauHoSo(Protocol.TEP_CANCEL,v.id.toString()); }
        bao(v,"CANCEL",0,"Đã hủy.");
    }
    // Giu nguon that de thu lai sau loi, sinh mot luot truyen moi.
    public CompletableFuture<TepTin> thuLai(String ma) {
        Viec v=viec.get(ma); if (v==null || !v.gui) { return CompletableFuture.failedFuture(new IOException("Không tìm thấy tệp nguồn.")); }
        return guiCoMa(v.room,v.duong,v.ma);
    }
    // So viec cuc bo co gioi han, cac snapshot ket thuc cu duoc giai phong.
    private synchronized void dangKy(Viec v) {
        if (viec.size()>=100) { viec.entrySet().removeIf(e -> e.getValue().ketQua.isDone()); }
        if (viec.size()>=100) { throw new RejectedExecutionException("Đang có quá nhiều lượt truyền."); } viec.put(v.ma,v);
    }
    // Mat ket noi huy moi luot, khong tu dong tiep tuc tep do dang.
    public synchronized void xoa() {
        for (Viec v:viec.values()) { v.huy=true; v.ketQua.completeExceptionally(new IOException("Mất kết nối khi truyền tệp.")); }
        if (truyen!=null) { truyen.close(); truyen=null; } guiNen.getQueue().clear(); taiNen.getQueue().clear(); viec.clear(); anh.clear(); choAnh.clear();
    }
    // Cap nhat snapshot, tinh toc do trung binh tu luc bat dau.
    private void bao(Viec v,String state,long n,String loi) {
        if (v.t==null) { return; } double giay=Math.max(.001,(System.nanoTime()-v.batDau)/1_000_000_000.0);
        nghe.onTep(new TepTienDo(v.ma,v.t,state,n,n/giay,v.duong,loi,v.gui));
    }
    // Khong de tep tam sot lai khi timeout/huy/CRC sai.
    private void thatBai(Viec v,Exception e) {
        if (v.id!=null && truyen!=null) { truyen.huy(v.id); if (v.anhId!=null) { truyen.huy(v.anhId); } core.yeuCauHoSo(Protocol.TEP_CANCEL,v.id.toString()); }
        if (v.tam!=null) { try { Files.deleteIfExists(v.tam); } catch (IOException ignored) { } }
        bao(v,v.huy?"CANCEL":"ERROR",0,ClientCore.loiDeHieu(e)); v.ketQua.completeExceptionally(e);
        if (viec.size()>100) { viec.entrySet().removeIf(x -> x.getValue().ketQua.isDone()); }
    }
    // Kiem tra huy o moi buoc truoc khi tao du lieu tiep theo.
    private void kiemTra(Viec v) throws IOException { if (v.huy || !core.sanSang()) { throw new IOException("Đã hủy hoặc mất kết nối truyền tệp."); } }
    // Cho control co han; luong nay chi la worker tep.
    private static String[] cho(CompletableFuture<String[]> f) throws Exception { return f.get(10,TimeUnit.SECONDS); }
    // Truyen co tien do va timeout rieng 60s; gioi han tong 10 phut tren mang rat yeu.
    private static Path choTep(CompletableFuture<Path> f) throws Exception { return f.get(10,TimeUnit.MINUTES); }
    // Tao id cuc bo nhu tin van ban de ghep dong tien do voi multicast.
    private static String taoMa() { return com.mycompany.udpmultichat.common.model.TinNhan.taoMa(); }
    // Pool co hang doi gioi han, khong tao luong theo moi manh.
    private static ThreadPoolExecutor taoNhomLuong(int so,String ten) {
        ThreadPoolExecutor p=new ThreadPoolExecutor(so,so,5,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r -> { Thread t=new Thread(r,ten); t.setDaemon(true); return t; });
        p.allowCoreThreadTimeOut(true); return p;
    }
    /** Mot viec cuc bo, tep nguon khong nap RAM. */
    private static final class Viec {
        final String ma,room; final boolean gui; final CompletableFuture<TepTin> ketQua=new CompletableFuture<>();
        volatile boolean huy; Path duong,tam; TepTin t; UUID id,anhId; long batDau=System.nanoTime();
        // Thong tin du de huy va bao tien do ma khong can Swing.
        Viec(String ma,String room,Path duong,boolean gui) { this.ma=ma; this.room=room; this.duong=duong; this.gui=gui; }
    }
}
