package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Vai tro: Quyen tep, INIT/GET/DONE, luu va don tep tam. Ai goi: ServerCore. Goi: KhoTep, TruyenTep, QuanLyPhong. */
final class QuanLyTep implements AutoCloseable {
    interface GhiTin { TinNhan ghi(Phien p,TepTin t,String ma) throws IOException; }
    private final KhoTep kho;
    private final QuanLyPhong phong;
    private final TruyenTep truyen;
    private final GhiTin ghiTin;
    private final long max;
    private final int retries;
    private final Map<String,Luot> luot=new HashMap<>();
    private final ScheduledExecutorService don;
    // Khoi tao cong du lieu rieng, toc do khong anh huong ReliableUdp dieu khien.
    QuanLyTep(Path data,KhoLuuTru config,QuanLyPhong phong,GhiTin ghiTin,double mat,long seed) throws IOException {
        this.phong=phong; this.ghiTin=ghiTin; kho=new KhoTep(data); kho.donMoCoi(phong.tinNhan().cacTep());
        max=Long.parseLong(config.cauHinh("upload.maxBytes"));
        retries=Integer.parseInt(config.cauHinh("upload.maxRetries"));
        if (max<1 || max>100*1024*1024L || retries<0 || retries>12) { throw new IOException("Cấu hình truyền tệp không hợp lệ."); }
        truyen=new TruyenTep(0,mat,seed,retries);
        don=Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"tep-don"); t.setDaemon(true); return t; });
        don.scheduleWithFixedDelay(this::quet,5,5,TimeUnit.SECONDS);
    }
    // ===== DIEU KHIEN DA XAC THUC =====
    synchronized String[] xuLy(Phien p,String[] g) throws IOException {
        return switch (g[0]) {
            case Protocol.TEP_INFO -> new String[]{Long.toString(max),Integer.toString(truyen.cong()),Integer.toString(retries)};
            case Protocol.TEP_INIT -> batDau(p,g);
            case Protocol.TEP_GET -> chuanBiTai(p,g);
            case Protocol.TEP_READY -> sanSang(p,g);
            case Protocol.TEP_DONE -> hoanTat(p,g);
            case Protocol.TEP_CANCEL -> huy(p,g);
            default -> throw new IllegalArgumentException("Lệnh tệp không hợp lệ.");
        };
    }
    // Quyen phong va gioi han 2 tep moi nguoi duoc Server kiem tra, khong tin Client.
    private String[] batDau(Phien p,String[] g) throws IOException {
        if (g.length!=11) { throw new IllegalArgumentException("Yêu cầu tệp thiếu thông tin."); }
        phong.kiemTraGui(p,g[3]);
        String[] cu=timLai(p,g); if (cu!=null) { return cu; }
        if (luot.size()>=64) { throw new IllegalArgumentException("Quá nhiều lượt truyền. Hãy thử lại sau."); }
        if (luot.values().stream().filter(l -> l.upload && l.tin==null && l.user.equals(p.nguoi.id)).count()>=2) { throw new IllegalArgumentException("Bạn đang gửi hai tệp. Hãy chờ hoàn tất."); }
        long size=Long.parseLong(g[5]),crc=Long.parseLong(g[6]),nho=Long.parseLong(g[8]),crcNho=Long.parseLong(g[9]); TepTin.kiemTra(size,max);
        if (crc<0 || crc>0xffffffffL || nho<0 || nho>65536 || !g[10].matches("[A-Za-z0-9_-]{22}")) { throw new IllegalArgumentException("Thông tin tệp không hợp lệ."); }
        InetSocketAddress ban=diaChi(p,g[7]); String id=UUID.randomUUID().toString();
        TepTin t=new TepTin(id,g[3],p.nguoi.id,TepTin.tenAnToan(g[4]),size,crc,System.currentTimeMillis());
        Path tam=kho.duong(t.room(),id,".part"); Luot l=new Luot(p,t,ban,true,tam,g[10]); luot.put(id,l);
        try {
            l.viec=truyen.nhan(UUID.fromString(id),ban,tam,size,crc,n -> { }); l.viec.whenComplete((x,e) -> l.cuoi=System.nanoTime());
            if (nho>0) {
                l.anhId=UUID.randomUUID(); l.anhTam=kho.duong(t.room(),l.anhId.toString(),".part");
                l.anh=truyen.nhan(l.anhId,ban,l.anhTam,nho,crcNho,n -> { });
            }
        } catch (IOException e) { donLuot(id,l); throw e; }
        return new String[]{id,l.anhId==null?"":l.anhId.toString(),Integer.toString(truyen.cong())};
    }
    // INIT gui lai voi cung ma da cong bo chi tra tep cu, khong tao them tin hay ban sao.
    private String[] timLai(Phien p,String[] g) throws IOException {
        TinNhan tin=phong.tinNhan().theoMa(g[3],p.nguoi.id,g[10]);
        if (tin!=null) {
            if (!tin.loai.equals("F")) { throw new IllegalArgumentException("Tệp đã thu hồi hoặc mã tin đã được dùng."); }
            TepTin t=kho.lay(TepTin.doc(tin.roomId,tin.userId,tin.noiDung,tin.thoiGian).id());
            if (t.kichThuoc()!=Long.parseLong(g[5]) || t.crc()!=Long.parseLong(g[6])) { throw new IllegalArgumentException("Tệp nguồn đã thay đổi."); }
            Luot l=new Luot(p,t,diaChi(p,g[7]),true,kho.duong(t.room(),t.id(),".part"),g[10]); l.tin=tin; luot.put(t.id(),l);
            return new String[]{t.id(),"",Integer.toString(truyen.cong()),"DONE"};
        }
        for (var e:List.copyOf(luot.entrySet())) { var l=e.getValue(); if (l.upload && l.user.equals(p.nguoi.id) && l.ma.equals(g[10])) { donLuot(e.getKey(),l); } }
        return null;
    }
    // DONE chi thanh cong sau CRC ca tep va thumbnail, tao tin F nhu lich su thuong.
    private String[] hoanTat(Phien p,String[] g) throws IOException {
        Luot l=tim(p,g); if (!l.upload) { throw new IllegalArgumentException("Không phải lượt gửi tệp."); }
        phong.kiemTraGui(p,l.t.room());
        if (l.tin!=null) { return new String[]{Long.toString(l.tin.msgId),l.t.dongGoi()}; }
        if (!l.viec.isDone() || (l.anh!=null && !l.anh.isDone())) { throw new IllegalArgumentException("Tệp chưa được nhận đủ."); }
        try { l.viec.join(); if (l.anh!=null) { l.anh.join(); } }
        catch (CompletionException e) { donLuot(l.t.id(),l); throw new IOException("Tệp nhận không toàn vẹn.",e); }
        if (!phong.lay(l.t.room()).dangMo) { donLuot(l.t.id(),l); throw new IllegalArgumentException("Phòng đã đóng trong khi gửi tệp."); }
        KhoTep.doiTen(l.tam,kho.duong(l.t.room(),l.t.id(),".bin"));
        if (l.anhTam!=null) {
            Path anh=kho.duong(l.t.room(),l.t.id(),".jpg"); Path doc=AnhTep.tao(l.anhTam,anh); Files.deleteIfExists(l.anhTam);
            if (doc==null) { Files.deleteIfExists(anh); }
        }
        try { kho.luu(l.t); l.tin=ghiTin.ghi(p,l.t,l.ma); l.cuoi=System.nanoTime(); }
        catch (IOException | RuntimeException e) { kho.xoa(l.t.id()); throw e; }
        return new String[]{Long.toString(l.tin.msgId),l.t.dongGoi()};
    }
    // GET chi tao luot; doi READY de Client mo kenh nhan truoc khi Server gui.
    private String[] chuanBiTai(Phien p,String[] g) throws IOException {
        if (g.length!=6 || luot.size()>=64) { throw new IllegalArgumentException("Quá nhiều lượt tải hoặc thiếu thông tin."); }
        TepTin t=kho.lay(g[3]); phong.kiemTraThanhVien(p,t.room());
        if (!phong.tinNhan().cacTep().contains(t.id())) { throw new IllegalArgumentException("Tệp đã được thu hồi."); }
        Path nguon=kho.duong(t.room(),t.id(),Boolean.parseBoolean(g[5])?".jpg":".bin");
        if (!Files.isRegularFile(nguon)) { throw new IllegalArgumentException("Tệp không có ảnh xem trước."); }
        UUID id=UUID.randomUUID(); Luot l=new Luot(p,t,diaChi(p,g[4]),false,nguon,""); luot.put(id.toString(),l);
        long size=Files.size(nguon),crc=Boolean.parseBoolean(g[5])?TruyenTep.crc(nguon):t.crc();
        l.size=size; l.crc=crc;
        return new String[]{id.toString(),Long.toString(size),Long.toString(crc),Integer.toString(truyen.cong())};
    }
    // READY lap lai khong tao luong gui thu hai.
    private String[] sanSang(Phien p,String[] g) throws IOException {
        Luot l=tim(p,g); if (l.upload) { throw new IllegalArgumentException("Lượt tải không hợp lệ."); }
        phong.kiemTraThanhVien(p,l.t.room());
        if (l.viec==null) { l.viec=truyen.gui(UUID.fromString(g[3]),l.ban,l.tam,l.size,l.crc,n -> { }); }
        return new String[]{"OK"};
    }
    // CANCEL control co ACK bo sung cho CANCEL nhi phan co the mat.
    private String[] huy(Phien p,String[] g) throws IOException { Luot l=tim(p,g); if (l.tin==null) { donLuot(g[3],l); } return new String[]{"OK"}; }
    // Tim bang token, khong cho user khac huy/tai luot truyen.
    private Luot tim(Phien p,String[] g) {
        if (g.length!=4) { throw new IllegalArgumentException("Thiếu mã lượt truyền."); }
        Luot l=luot.get(g[3]); if (l==null || !l.token.equals(p.token)) { throw new IllegalArgumentException("Lượt truyền không còn tồn tại."); } return l;
    }
    // Cong du lieu lay cung IP da xac thuc; khong cho Client gui den may thu ba.
    private static InetSocketAddress diaChi(Phien p,String cong) {
        int n=Integer.parseInt(cong); if (n<1 || n>65535) { throw new IllegalArgumentException("Cổng truyền tệp không hợp lệ."); }
        return new InetSocketAddress(p.diaChi.getAddress(),n);
    }
    // ===== DON DEP VA THONG KE =====
    synchronized void matPhien(String token) {
        for (var e:List.copyOf(luot.entrySet())) { if (e.getValue().token.equals(token)) { try { donLuot(e.getKey(),e.getValue()); } catch (IOException ignored) { } } }
    }
    // Tep dang gui dong boi engine sau 60s khong goi; luot cho READY/DONE cung co han.
    private synchronized void quet() {
        long luc=System.nanoTime();
        for (var e:List.copyOf(luot.entrySet())) {
            Luot l=e.getValue(); boolean loi=l.viec!=null && l.viec.isCompletedExceptionally();
            if (loi || (luc-l.cuoi>TimeUnit.SECONDS.toNanos(60) && (l.viec==null || l.viec.isDone()))) {
                try { donLuot(e.getKey(),l); } catch (IOException ignored) { }
            }
        }
    }
    // Khong xoa tep da cong bo khi mat phien, chi xoa tep tam cua luot dang do.
    private void donLuot(String id,Luot l) throws IOException {
        truyen.huy(UUID.fromString(id)); if (l.anhId!=null) { truyen.huy(l.anhId); }
        if (l.upload) { Files.deleteIfExists(l.tam); if (l.anhTam!=null) { Files.deleteIfExists(l.anhTam); } } luot.remove(id);
    }
    // Thu hoi tin chi xoa metadata tep thuoc dung room/msg; caller da kiem tra quyen.
    synchronized void xoaTin(TinNhan tin) throws IOException { if (tin.loai.equals("F")) { kho.xoa(TepTin.doc(tin.roomId,tin.userId,tin.noiDung,tin.thoiGian).id()); } }
    // Xoa phong huy moi luot dang gui/tai truoc khi xoa folder.
    synchronized void xoaPhong(String id) throws IOException {
        for (var e:List.copyOf(luot.entrySet())) { if (e.getValue().t.room().equals(id)) { donLuot(e.getKey(),e.getValue()); } } kho.xoaPhong(id);
    }
    // Thong ke co san cho dashboard M5.
    long[] thongKe(String room,String user) { return kho.thongKe(room,user); }
    // Dong socket va file dang mo truoc khi Server dung.
    @Override public synchronized void close() {
        don.shutdownNow(); truyen.close();
        for (var e:List.copyOf(luot.entrySet())) { try { donLuot(e.getKey(),e.getValue()); } catch (IOException ignored) { } }
    }
    /** Mot luot co han, so luong toi da 64. */
    private static final class Luot {
        final String token,user,ma; final TepTin t; final InetSocketAddress ban; final boolean upload; final Path tam;
        long size,crc; volatile long cuoi=System.nanoTime(); UUID anhId; Path anhTam; CompletableFuture<Path> viec,anh; TinNhan tin;
        // Gan token de huy chinh xac khi dang xuat/timeout.
        Luot(Phien p,TepTin t,InetSocketAddress ban,boolean upload,Path tam,String ma) { token=p.token; user=p.nguoi.id; this.t=t; this.ban=ban; this.upload=upload; this.tam=tam; this.ma=ma; }
    }
}
