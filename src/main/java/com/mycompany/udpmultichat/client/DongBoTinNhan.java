package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.model.TinNhan;
import java.io.IOException;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** Thu tu va loai trung theo msgId cua MOT phong; ghep lich su/RESYNC voi tin truc tiep. */
public final class DongBoTinNhan implements AutoCloseable {
    private final String roomId;
    private final BiConsumer<TinNhan,Boolean> hienTin;
    private final TreeMap<Long,TinNhan> choNhan=new TreeMap<>(), banSao=new TreeMap<>();
    private long cuoi, mocServer;
    private boolean sanSang, dangDongBo, laLichSu, daDong;
    private String ma="";
    private String[] ketThuc;
    private CompletableFuture<Void> ketQua=CompletableFuture.completedFuture(null);

    public DongBoTinNhan(String roomId,BiConsumer<TinNhan,Boolean> hienTin) {
        this.roomId=roomId; this.hienTin=hienTin;
    }

    // ===== NHAN TIN TRUC TIEP =====
    public synchronized boolean nhan(TinNhan tin) {
        if (daDong || !roomId.equals(tin.roomId) || tin.msgId<=cuoi) { return false; }
        choNhan.putIfAbsent(tin.msgId,tin);
        // Neu mat goi lau, giu mot cua so nho; phan con lai lay bu tu Server.
        while (choNhan.size()>1024) { choNhan.pollLastEntry(); }
        mocServer=Math.max(mocServer,tin.msgId);
        if (sanSang) { phatLienTiep(false,0); }
        return canDongBo();
    }

    // Heartbeat phat hien ca tin CUOI bi mat, khi khong co tin sau de so sanh.
    public synchronized boolean nhanMoc(long head) {
        mocServer=Math.max(mocServer,head);
        return canDongBo();
    }
    public synchronized boolean canDongBo() {
        return !daDong && !dangDongBo && (!sanSang || cuoi<mocServer);
    }
    public synchronized boolean daSanSang() { return sanSang; }
    public synchronized long cuoi() { return cuoi; }

    // ===== BAT DAU MOT LO =====
    public synchronized String batDau(boolean lichSu) {
        if (daDong || dangDongBo) { return null; }
        ma=UUID.randomUUID().toString();
        dangDongBo=true; laLichSu=lichSu; ketThuc=null; banSao.clear();
        ketQua=new CompletableFuture<>();
        String maLanNay=ma;
        ketQua.orTimeout(12,TimeUnit.SECONDS).whenComplete((v,e) -> {
            if (e!=null) { huy(maLanNay,e); }
        });
        return ma;
    }
    public synchronized CompletableFuture<Void> ketQua() { return ketQua; }

    public synchronized void nhanMuc(String maLo,TinNhan tin) {
        if (daDong || !dangDongBo || !ma.equals(maLo) || !roomId.equals(tin.roomId)) { return; }
        if (banSao.size()<50) { banSao.putIfAbsent(tin.msgId,tin); }
        thuHoanTat();
    }
    public synchronized void nhanKetThuc(String[] p) {
        if (daDong || !dangDongBo || !ma.equals(p[2])) { return; }
        ketThuc=p.clone(); thuHoanTat();
    }

    // Chi hoan tat khi nhan DU muc, khong phu thuoc thu tu datagram den.
    private void thuHoanTat() {
        if (ketThuc==null) { return; }
        long dau=Long.parseLong(ketThuc[3]), het=Long.parseLong(ketThuc[4]);
        int so=Integer.parseInt(ketThuc[5]); long head=Long.parseLong(ketThuc[6]);
        if (so<0 || so>50 || het-dau+1!=so || head<het || dau<1) {
            huy(ma,new IOException("Lô đồng bộ không hợp lệ")); return;
        }
        if (banSao.size()!=so) { return; }
        if (so>0 && (banSao.firstKey()!=dau || banSao.lastKey()!=het)) {
            huy(ma,new IOException("Lịch sử thiếu hoặc sai thứ tự")); return;
        }
        for (long id=dau;id<=het;id++) {
            if (!banSao.containsKey(id)) { huy(ma,new IOException("Thiếu tin trong lô đồng bộ")); return; }
        }
        if (!sanSang) { cuoi=dau-1; sanSang=true; choNhan.headMap(dau).clear(); }
        banSao.forEach((id,tin) -> { if (id>cuoi) { choNhan.putIfAbsent(id,tin); } });
        mocServer=Math.max(mocServer,head);
        phatLienTiep(laLichSu,het);
        dangDongBo=false; ma=""; banSao.clear(); ketThuc=null;
        CompletableFuture<Void> xong=ketQua; xong.complete(null);
    }

    private void phatLienTiep(boolean lichSu,long hetLichSu) {
        while (choNhan.containsKey(cuoi+1)) {
            TinNhan tin=choNhan.remove(++cuoi);
            hienTin.accept(tin,lichSu && tin.msgId<=hetLichSu);
        }
        choNhan.headMap(cuoi,true).clear();
    }

    public synchronized void huy(String maLo,Throwable loi) {
        if (!dangDongBo || !ma.equals(maLo)) { return; }
        dangDongBo=false; ma=""; banSao.clear(); ketThuc=null;
        ketQua.completeExceptionally(loi);
    }

    // Huy Future va bo dem khi roi/xoa phong hoac dong Client.
    @Override public synchronized void close() {
        daDong=true;
        if (dangDongBo) { huy(ma,new IOException("Đã rời phòng")); }
        choNhan.clear(); banSao.clear();
    }
}
