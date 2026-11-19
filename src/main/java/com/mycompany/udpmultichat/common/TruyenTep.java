package com.mycompany.udpmultichat.common;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongConsumer;
import java.util.zip.CRC32;

/** Vai tro: Truyen tep UDP nhi phan cua so truot, ghi thang dia. Ai goi: QuanLyTep, TepClient. Goi: DatagramSocket, FileChannel. */
public final class TruyenTep implements AutoCloseable {
    public static final int MANH=1000, CUA_SO=32, HEADER=23;
    private static final byte DATA=1, ACK=2, CANCEL=3;
    private final DatagramSocket socket;
    private final Map<UUID,Phien> phien=new ConcurrentHashMap<>();
    private final ScheduledExecutorService dongHo;
    private final ThreadPoolExecutor ghi;
    private final Random random;
    private final double mat;
    private final int guiLai;
    private volatile boolean chay=true;
    // Ty le mat va seed chi phuc vu kiem thu; so lan gui lai do cau hinh quyet dinh.
    public TruyenTep(int cong,double mat,long seed,int guiLai) throws IOException {
        socket=new DatagramSocket(cong); socket.setReceiveBufferSize(4*1024*1024); socket.setSendBufferSize(1024*1024);
        this.mat=mat; this.guiLai=guiLai; random=new Random(seed);
        ghi=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(2048),r -> nen(r,"tep-ghi"));
        dongHo=Executors.newSingleThreadScheduledExecutor(r -> nen(r,"tep-cua-so"));
        nen(this::vongNhan,"tep-nhan").start(); dongHo.scheduleWithFixedDelay(this::nhip,2,2,TimeUnit.MILLISECONDS);
    }
    // Cong rieng cho DATA/ACK; dieu khien van qua ReliableUdp tren cong 5000.
    public int cong() { return socket.getLocalPort(); }
    // ===== DANG KY GUI / NHAN =====
    public CompletableFuture<Path> nhan(UUID id,InetSocketAddress ban,Path dich,long size,long crc,LongConsumer tienDo) throws IOException {
        return them(new Phien(id,ban,dich,size,crc,false,tienDo));
    }
    // Chi doc tung manh 1000 byte, khong dua ca tep vao RAM.
    public CompletableFuture<Path> gui(UUID id,InetSocketAddress ban,Path nguon,long size,long crc,LongConsumer tienDo) throws IOException {
        return them(new Phien(id,ban,nguon,size,crc,true,tienDo));
    }
    // Tran dung luong/ngan phien de tranh tran RAM va mo qua nhieu file.
    private CompletableFuture<Path> them(Phien p) throws IOException {
        if (!chay || phien.size()>=64 || phien.putIfAbsent(p.id,p)!=null) {
            p.kenh.close(); throw new IOException("Không thể mở thêm lượt truyền tệp.");
        }
        return p.ketQua;
    }
    // Huy cuc bo va bao ben kia ba lan; control CANCEL cung duoc ACK rieng.
    public void huy(UUID id) {
        Phien p=phien.get(id); if (p==null) { return; }
        synchronized (p) {
            for (int i=0;i<3;i++) { try { phat(goi(CANCEL,p.id,0,new byte[0]),p.ban); } catch (IOException ignored) { } }
            loi(p,"Đã hủy truyền tệp.");
        }
    }
    // ===== VONG NHAN NHE, HANG GHI CO GIOI HAN =====
    private void vongNhan() {
        while (chay) {
            try {
                byte[] dem=new byte[Protocol.GOI_TOI_DA+1]; DatagramPacket g=new DatagramPacket(dem,dem.length); socket.receive(g);
                if (g.getLength()<HEADER || g.getLength()>Protocol.GOI_TOI_DA) { continue; }
                byte[] data=Arrays.copyOf(dem,g.getLength()); InetSocketAddress ban=(InetSocketAddress)g.getSocketAddress();
                ghi.execute(() -> xuLy(ban,data));
            } catch (RejectedExecutionException e) { /* Hang day: bo goi, cua so se gui lai. */ }
            catch (IOException e) { if (chay) { phien.values().forEach(p -> loi(p,"Mất kết nối truyền tệp.")); } }
        }
    }
    // Doi chieu endpoint + UUID ngau nhien truoc khi ghi bat ky byte nao.
    private void xuLy(InetSocketAddress ban,byte[] data) {
        ByteBuffer b=ByteBuffer.wrap(data); byte loai=b.get(); UUID id=new UUID(b.getLong(),b.getLong()); int so=b.getInt(),dai=Short.toUnsignedInt(b.getShort());
        Phien p=phien.get(id); if (p==null || !p.ban.equals(ban) || dai!=b.remaining()) { return; }
        synchronized (p) {
            try {
                p.cuoi=System.nanoTime();
                if (loai==CANCEL) { loi(p,"Bên kia đã hủy truyền tệp."); return; }
                if (loai==ACK && p.gui && dai==8) { nhanAck(p,so,b.getLong()); }
                if (loai==DATA && !p.gui) { nhanManh(p,so,b); }
            } catch (IOException | RuntimeException e) { loi(p,"Không thể ghi/đọc tệp: "+e.getMessage()); }
        }
    }
    // Ghi offset cho manh dao thu tu, bo trung va ACK bitmap sau moi manh.
    private void nhanManh(Phien p,int so,ByteBuffer b) throws IOException {
        int can=(int)Math.min(MANH,p.size-(long)so*MANH);
        if (so<0 || so>=p.tong || b.remaining()!=can) { return; }
        if (!p.daNhan.get(so) && !p.ketQua.isDone()) {
            long offset=(long)so*MANH; while (b.hasRemaining()) { offset+=p.kenh.write(b,offset); }
            p.daNhan.set(so); p.xong+=can; baoTienDo(p);
        }
        while (p.dau<p.tong && p.daNhan.get(p.dau)) { p.dau++; }
        if (p.dau==p.tong && !p.ketQua.isDone()) {
            p.kenh.force(true); p.kenh.close();
            if (crc(p.duong)!=p.crc) { loi(p,"Tệp nhận không toàn vẹn. Hãy thử lại."); }
            else { p.ketQua.complete(p.duong); }
        }
        if (!p.ketQua.isCompletedExceptionally()) { ack(p); }
    }
    // ACK gom moc lien tuc va bitmap 32 manh tiep theo; goi trung khong tang tien do.
    private void ack(Phien p) throws IOException {
        long bits=0; for (int i=0;i<CUA_SO && p.dau+i<p.tong;i++) { if (p.daNhan.get(p.dau+i)) { bits|=1L<<i; } }
        phat(goi(ACK,p.id,p.dau,ByteBuffer.allocate(8).putLong(bits).array()),p.ban);
    }
    // Xac nhan chi nhung manh da gui, khong tin moc ACK vuot tep.
    private void nhanAck(Phien p,int dau,long bits) {
        if (dau<0 || dau>p.tong || dau>p.daGui.length()) { return; }
        for (int i=p.dau;i<Math.min(p.tong,dau+CUA_SO);i++) {
            if (p.daGui.get(i) && (i<dau || (bits&(1L<<(i-dau)))!=0)) { p.daNhan.set(i); }
        }
        // SACK da nhan it nhat ba manh sau lo hong: gui lai som, khong doi timeout.
        int cuoi=p.daNhan.length()-1,soAck=p.daNhan.cardinality();
        for (int i=p.dau;i<cuoi-2;i++) {
            if (!p.daNhan.get(i) && soAck-p.ackLucGui[i]>=3) { p.thieu.set(i); }
        }
        while (p.dau<p.tong && p.daNhan.get(p.dau)) { p.dau++; }
        p.xong=Math.min(p.size,(long)p.daNhan.cardinality()*MANH); baoTienDo(p);
        if (p.dau==p.tong && !p.ketQua.isDone()) { try { p.kenh.close(); p.ketQua.complete(p.duong); } catch (IOException | RuntimeException e) { loi(p,"Lỗi truyền tệp: "+e); } }
    }
    // ===== CUA SO TRUOT VA TIMEOUT =====
    private void nhip() {
        long luc=System.nanoTime();
        for (Phien p:phien.values()) {
            synchronized (p) {
                if (p.ketQua.isDone()) { if (luc-p.cuoi>TimeUnit.SECONDS.toNanos(60)) { phien.remove(p.id,p); } continue; }
                if (luc-p.cuoi>TimeUnit.SECONDS.toNanos(60)) { loi(p,"Mất kết nối: truyền tệp quá thời gian chờ."); continue; }
                if (p.gui) { try { guiCuaSo(p,luc); } catch (IOException | RuntimeException e) { loi(p,"Lỗi truyền tệp: "+e); } }
            }
        }
    }
    // Moi nhip gui toi da 32 goi, cua so 32: khong chiem het socket/CPU cua chat.
    private void guiCuaSo(Phien p,long luc) throws IOException {
        int daGui=0;
        for (int i=p.dau;i<Math.min(p.tong,p.dau+CUA_SO) && daGui<32;i++) {
            long cho=TimeUnit.MILLISECONDS.toNanos(Math.min(2400,300L<<Math.min(3,Math.max(0,p.lan[i]-1))));
            boolean som=p.thieu.get(i) && luc-p.lucGui[i]>=20_000_000L;
            if (p.daNhan.get(i) || (!som && p.lan[i]>0 && luc-p.lucGui[i]<cho)) { continue; }
            if (p.lan[i]>guiLai) { throw new IOException("Không nhận đủ mảnh tệp sau nhiều lần gửi lại. Hãy thử lại."); }
            int n=(int)Math.min(MANH,p.size-(long)i*MANH); ByteBuffer dem=ByteBuffer.allocate(n); long offset=(long)i*MANH;
            while (dem.hasRemaining()) { int doc=p.kenh.read(dem,offset); if (doc<0) { throw new IOException("Tệp nguồn đã thay đổi."); } offset+=doc; }
            p.thieu.clear(i); p.ackLucGui[i]=p.daNhan.cardinality();
            p.daGui.set(i); p.lan[i]++; p.lucGui[i]=luc; phat(goi(DATA,p.id,i,dem.array()),p.ban); daGui++;
        }
    }
    // Tien do toi da 10 lan/giay, luon bao 100%.
    private void baoTienDo(Phien p) {
        long luc=System.nanoTime(); if (p.xong==p.size || luc-p.bao>100_000_000L) { p.bao=luc; p.tienDo.accept(p.xong); }
    }
    // Goi nhi phan lon nhat 1023 byte, duoi MTU 1399.
    private static byte[] goi(byte loai,UUID id,int so,byte[] data) {
        return ByteBuffer.allocate(HEADER+data.length).put(loai).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).putInt(so).putShort((short)data.length).put(data).array();
    }
    // Mo phong mat ca DATA/ACK voi seed; moi cuoc truyen co endpoint rieng.
    private void phat(byte[] data,InetSocketAddress ban) throws IOException {
        synchronized (random) { if (random.nextDouble()<mat) { return; } }
        socket.send(new DatagramPacket(data,data.length,ban));
    }
    // Loi luon dong kenh va xoa tep tam ben nhan, khong xoa tep nguon.
    private void loi(Phien p,String thongBao) {
        synchronized (p) {
            if (p.ketQua.isDone()) { return; }
            try { p.kenh.close(); if (!p.gui) { Files.deleteIfExists(p.duong); } } catch (IOException ignored) { }
            p.ketQua.completeExceptionally(new IOException(thongBao)); phien.remove(p.id,p);
        }
    }
    // CRC32 doc theo khoi, khong nap ca tep vao bo nho.
    public static long crc(Path p) throws IOException {
        CRC32 crc=new CRC32(); byte[] dem=new byte[8192];
        try (InputStream in=Files.newInputStream(p)) { int n; while ((n=in.read(dem))!=-1) { crc.update(dem,0,n); } }
        return crc.getValue();
    }
    // Tat socket danh thuc receive va ket thuc moi Future.
    @Override public void close() {
        chay=false; socket.close(); dongHo.shutdownNow(); ghi.shutdownNow();
        phien.values().forEach(p -> loi(p,"Kết nối truyền tệp đã đóng.")); phien.clear();
    }
    // Luong nen khong giu ung dung song khi dong cua so.
    private static Thread nen(Runnable viec,String ten) { Thread t=new Thread(viec,ten); t.setDaemon(true); return t; }
    /** Trang thai gioi han bo nho theo so manh; du lieu nam tren dia. */
    private static final class Phien {
        final UUID id; final InetSocketAddress ban; final Path duong; final long size,crc; final boolean gui;
        final int tong; final BitSet daNhan=new BitSet(),daGui=new BitSet(),thieu=new BitSet(); final int[] lan,ackLucGui; final long[] lucGui;
        final FileChannel kenh; final LongConsumer tienDo; final CompletableFuture<Path> ketQua=new CompletableFuture<>();
        int dau; long xong,bao,cuoi=System.nanoTime();
        // Chi ben nhan duoc phep tao tep moi; khong bao gio ghi de tep dich.
        Phien(UUID id,InetSocketAddress ban,Path duong,long size,long crc,boolean gui,LongConsumer tienDo) throws IOException {
            if (size<1 || size>100*1024*1024L) { throw new IOException("Kích thước tệp không hợp lệ."); }
            this.id=id; this.ban=ban; this.duong=duong; this.size=size; this.crc=crc; this.gui=gui; this.tienDo=tienDo;
            tong=(int)((size+MANH-1)/MANH); lan=new int[tong]; ackLucGui=new int[tong]; lucGui=new long[tong];
            kenh=gui?FileChannel.open(duong,StandardOpenOption.READ):FileChannel.open(duong,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
        }
    }
}
