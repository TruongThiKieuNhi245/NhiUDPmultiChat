package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.PhanManh;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Vai tro: Upload/download avatar nen va cache LRU gioi han. Ai goi: ClientCore, HoSoDialog. Goi: PhanManh va API request cua ClientCore. */
public final class HoSoClient {
    private final ClientCore core;
    private final Consumer<NguoiDung> capNhat;
    private final Map<String,NguoiDung> hoSo=new LinkedHashMap<>(16,0.75f,true);
    private final Map<String,BufferedImage> anh=new LinkedHashMap<>(16,0.75f,true);
    private final Set<String> dangTai=ConcurrentHashMap.newKeySet();
    private final Map<String,CompletableFuture<NguoiDung>> dangHoi=new ConcurrentHashMap<>();
    private volatile long doi;
    // Ket noi API khong Socket truc tiep, giu ClientCore ngan.
    HoSoClient(ClientCore core,Consumer<NguoiDung> capNhat) { this.core=core; this.capNhat=capNhat; }
    // ===== CACHE HO SO VA ANH =====
    public synchronized NguoiDung lay(String id) { return hoSo.get(id); }
    // Chi doc RAM; UI khong cho doi ImageIO hay mang.
    public synchronized BufferedImage anh(String id) {
        NguoiDung n=hoSo.get(id); return n==null?null:anh.get(id+":"+n.anhHash);
    }
    // Xoa cache va vo hieu hoa ket qua cua luot tai thuoc phien cu.
    synchronized void xoa() { doi++; hoSo.clear(); anh.clear(); dangTai.clear(); dangHoi.clear(); }
    // Snapshot moi giu dinh danh bat bien; tai anh neu phien ban thay doi.
    void nhan(NguoiDung n) {
        synchronized (this) { hoSo.put(n.id,n); gioiHan(hoSo); }
        capNhat.accept(n); taiAnh(n);
    }
    // Chi hoi Server mot lan dong thoi cho moi user, gioi han luot cho.
    public void gap(String id) {
        NguoiDung daCo=lay(id); if (daCo!=null) { taiAnh(daCo); return; }
        if (id.isBlank() || dangHoi.size()>=32) { return; }
        long phien=doi;
        dangHoi.computeIfAbsent(id,k -> core.yeuCauHoSo(Protocol.HO_SO,id).thenApply(g -> NguoiDung.tuTruong(g,3))
            .whenCompleteAsync((n,e) -> { dangHoi.remove(id); if (e==null && phien==doi && lay(id)==null) { nhan(n); } }));
    }
    // Tai tung manh qua request co ACK/gui lai; du CRC moi giai ma PNG.
    private void taiAnh(NguoiDung n) {
        String khoa=n.id+":"+n.anhHash; long phien=doi;
        synchronized (this) { if (n.anhHash.isEmpty() || anh.containsKey(khoa) || dangTai.size()>=16 || !dangTai.add(khoa)) { return; } }
        CompletableFuture.supplyAsync(() -> {
            try { return PhanManh.docAnh(taiDuLieu(n).join(),true); }
            catch (IOException e) { throw new java.util.concurrent.CompletionException(e); }
        }).whenComplete((h,e) -> {
            dangTai.remove(khoa);
            if (e==null && phien==doi) {
                synchronized (this) { anh.put(khoa,h); gioiHan(anh); }
                NguoiDung moi=lay(n.id); if (moi!=null) { capNhat.accept(moi); }
            }
        });
    }
    // Ghep du lieu goc, kiem tra CRC truoc khi giai ma PNG.
    public CompletableFuture<byte[]> taiDuLieu(NguoiDung n) {
        return core.yeuCauAnh(Protocol.ANH_DOC,n.id,n.anhHash,"0").thenCompose(g -> {
            int so=Integer.parseInt(g[3]); PhanManh.BoGhep bo=new PhanManh.BoGhep(so,g[4]); bo.nhan(0,g[5]);
            CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
            for (int i=1;i<(so+Protocol.MANH_ANH-1)/Protocol.MANH_ANH;i++) {
                final int viTri=i;
                viec=viec.thenCompose(v -> core.yeuCauAnh(Protocol.ANH_DOC,n.id,n.anhHash,Integer.toString(viTri)).thenAccept(p -> bo.nhan(viTri,p[5])));
            }
            return viec.thenApply(v -> { try { return bo.ketQua(); } catch (IOException e) { throw new java.util.concurrent.CompletionException(e); } });
        });
    }
    // ===== LUU HO SO: CHI GUI ANH KHI BAM LUU =====
    public CompletableFuture<NguoiDung> luu(String ten,String gt,byte[] data,boolean xoa,AtomicBoolean huy,Consumer<Integer> tienDo) {
        String ma=data==null?(xoa?"DELETE":"KEEP"):UUID.randomUUID().toString();
        CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
        if (data!=null) {
            try { PhanManh.docAnh(data,true); } catch (IOException e) { return CompletableFuture.failedFuture(e); }
            viec=core.yeuCauAnh(Protocol.ANH_BAT_DAU,ma,Integer.toString(data.length),PhanManh.crc(data)).thenApply(p -> null);
            viec=viec.thenCompose(v -> guiAnh(ma,data,huy,tienDo));
        }
        return viec.thenCompose(v -> {
            if (huy.get()) { return CompletableFuture.failedFuture(new IOException("Đã hủy lưu hồ sơ.")); }
            return core.yeuCauAnh(Protocol.LUU_HO_SO,ten,gt,ma).thenApply(g -> NguoiDung.tuTruong(g,3));
        }).whenComplete((n,e) -> {
            if (e==null) { core.nhanHoSo(n); }
            else if (data!=null) { core.yeuCauAnh(Protocol.ANH_HUY,ma); }
        });
    }
    // Nhan nut Huy giua cac manh; manh dang cho ACK van ket thuc co gioi han.
    private CompletableFuture<Void> guiAnh(String ma,byte[] data,AtomicBoolean huy,Consumer<Integer> tienDo) {
        CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
        int so=(data.length+Protocol.MANH_ANH-1)/Protocol.MANH_ANH;
        for (int i=0;i<so;i++) {
            final int viTri=i;
            viec=viec.thenCompose(v -> {
                if (huy.get()) { return CompletableFuture.failedFuture(new IOException("Đã hủy tải ảnh.")); }
                return core.yeuCauAnh(Protocol.ANH_MANH,ma,Integer.toString(viTri),PhanManh.manh(data,viTri)).thenAccept(p -> tienDo.accept((viTri+1)*100/so));
            });
        }
        return viec;
    }
    // Cache LRU co cung gioi han cho snapshot va anh da giai ma.
    private static <T> void gioiHan(Map<String,T> map) {
        while (map.size()>Protocol.CACHE_ANH) { map.remove(map.keySet().iterator().next()); }
    }
}
