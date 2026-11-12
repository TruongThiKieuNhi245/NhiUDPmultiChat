package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Vai tro: Trang thai gui, retry on dinh, typing va dong bo thu hoi. Ai goi: ClientCore, UI. Goi: ClientCore API xac thuc, Listener. */
public final class TroChuyenClient {
    private final ClientCore core;
    private final Listener listener;
    private final Map<String,ChoGui> dangGui=new ConcurrentHashMap<>();
    private final Map<String,Long> typing=new ConcurrentHashMap<>(),revision=new ConcurrentHashMap<>();
    private final Map<String,Set<Long>> daThuHoi=new ConcurrentHashMap<>();
    private final Set<String> dangDongBo=ConcurrentHashMap.newKeySet();
    private volatile long doi;
    // Gan Core dang dung, khong tao them socket hay luong.
    TroChuyenClient(ClientCore core,Listener listener) { this.core=core; this.listener=listener; }
    // ===== GUI TIN VA GUI LAI =====
    public CompletableFuture<Void> gui(String room,String text,String loai) { return guiCoMa(room,text,loai,TinNhan.taoMa()); }
    // Ma khong doi cho moi lan retry; Server con kiem tra lai ma/noi dung.
    public CompletableFuture<Void> guiCoMa(String room,String text,String loai,String ma) {
        NguoiDung n=core.nguoiDung();
        if (n==null || !core.sanSang() || core.phongDaVao().stream().noneMatch(p -> p.id.equals(room))) { return CompletableFuture.failedFuture(new IOException("Bạn chưa kết nối hoặc chưa vào phòng.")); }
        TinNhan t=new TinNhan(0,room,n.id,n.tenHienThi,text.strip(),System.currentTimeMillis(),ma,loai);
        try { Goi.kiemTraTin(t); } catch (IllegalArgumentException e) { return CompletableFuture.failedFuture(e); }
        if (dangGui.size()>=256) { return CompletableFuture.failedFuture(new IOException("Có quá nhiều tin chờ. Hãy gửi lại hoặc xóa tin lỗi.")); }
        String khoa=room+":"+ma; ChoGui moi=new ChoGui(t); ChoGui cu=dangGui.putIfAbsent(khoa,moi);
        if (cu!=null) { return CompletableFuture.failedFuture(new IOException("Tin này đang chờ. Hãy dùng Gửi lại.")); }
        return thuGui(khoa,moi);
    }
    // Gui lai tu hang loi; khong tao bong tin hoac ma moi.
    public CompletableFuture<Void> guiLai(String room,String ma) {
        ChoGui c=dangGui.get(room+":"+ma);
        if (c==null) { return CompletableFuture.failedFuture(new IOException("Tin không còn trong hàng gửi.")); }
        synchronized (c) { if (!c.loi) { return CompletableFuture.failedFuture(new IOException("Tin vẫn đang gửi.")); } }
        return thuGui(room+":"+ma,c);
    }
    // Moi lan thu co so luot rieng de timeout cu khong ghi de trang thai moi.
    private CompletableFuture<Void> thuGui(String khoa,ChoGui c) {
        final long luot;
        synchronized (c) { c.loi=false; c.ack=false; luot=++c.luot; listener.onTrangThaiGui(c.tin,"WAIT",""); }
        CompletableFuture.delayedExecutor(Protocol.CHO_TIN_MS,TimeUnit.MILLISECONDS).execute(() -> {
            if (dangGui.get(khoa)==c && c.luot==luot) { thatBai(c,"Chưa nhận đủ xác nhận. Bạn có thể gửi lại."); }
        });
        String lenh=c.tin.loai.equals("S")?Protocol.STICKER:Protocol.GUI_TIN;
        return core.yeuCauHoSo(lenh,c.tin.roomId,c.tin.maClient,c.tin.noiDung).thenAccept(g -> {
            synchronized (c) { if (c.luot!=luot) { return; } c.ack=true; c.serverId=Long.parseLong(g[3]); hoanTat(khoa,c); }
        }).whenComplete((v,e) -> { if (e!=null && c.luot==luot && dangGui.get(khoa)==c) { thatBai(c,ClientCore.loiDeHieu(e)); } });
    }
    // Chi multcast that moi danh dau quay ve; HISTORY/RESYNC van co the hien tin.
    void nhanMulticast(TinNhan t) {
        ChoGui c=dangGui.get(t.roomId+":"+t.maClient);
        if (c!=null && c.tin.userId.equals(t.userId)) {
            synchronized (c) { c.multicast=true; c.tin=t; hoanTat(t.roomId+":"+t.maClient,c); }
        }
    }
    // Co ca RESPONSE va multicast moi chuyen Da gui.
    private void hoanTat(String khoa,ChoGui c) {
        if (c.ack && c.multicast && c.tin.msgId==c.serverId && dangGui.remove(khoa,c)) { listener.onTrangThaiGui(c.tin,"SENT",""); }
    }
    // Loi giu noi dung va ma de retry, khong tu dong tao tin moi.
    private void thatBai(ChoGui c,String lyDo) {
        synchronized (c) { if (!dangGui.containsValue(c)) { return; } c.loi=true; listener.onTrangThaiGui(c.tin,"ERROR",lyDo); }
    }
    // Xoa chi hang loi cuc bo; khong phai lenh thu hoi tin da luu.
    public void xoaTinLoi(String room,String ma) {
        String k=room+":"+ma; ChoGui c=dangGui.get(k);
        if (c!=null && c.loi && dangGui.remove(k,c)) { listener.onTrangThaiGui(c.tin,"REMOVE",""); }
    }
    // ===== THU HOI VA TOMBSTONE =====
    public CompletableFuture<Void> thuHoi(String room,long id) { return core.yeuCauHoSo(Protocol.THU_HOI,room,Long.toString(id)).thenApply(g -> null); }
    // Nhan tombstone truoc bat ky ban tin cu nao den cham.
    void thuHoiMoi(String room,long id) {
        daThuHoi.computeIfAbsent(room,k -> ConcurrentHashMap.newKeySet()).add(id); listener.onThuHoi(room,id);
    }
    // Khong cho noi dung cu song lai khi multicast cu den sau su kien thu hoi.
    TinNhan loc(TinNhan t) { return daThuHoi.getOrDefault(t.roomId,Set.of()).contains(t.msgId)?t.daThuHoi():t; }
    // RESYNC explicit lay lai tombstone ke ca khi msgId hien tai da bang Server.
    public CompletableFuture<Void> dongBoThuHoi(String room) { return dongBoThuHoi(room,0,doi); }
    // Revision trong HEAD phat hien thu hoi cu khi noi lai phien, du msgId khong tang.
    void nhanRevision(String room,long ban) {
        if (ban<=revision.getOrDefault(room,-1L) || !dangDongBo.add(room)) { return; }
        long phien=doi;
        dongBoThuHoi(room,0,phien).whenComplete((v,e) -> {
            dangDongBo.remove(room);
            if (e==null && doi==phien) { revision.put(room,ban); }
        });
    }
    // Lay toi da 50 ID moi request; khong chua noi dung goc.
    private CompletableFuture<Void> dongBoThuHoi(String room,long sau,long phien) {
        return core.yeuCauHoSo(Protocol.DS_THU_HOI,room,Long.toString(sau)).thenCompose(g -> {
            if (doi!=phien || core.nguoiDung()==null) { return CompletableFuture.completedFuture(null); }
            for (int i=6;i<g.length;i++) { thuHoiMoi(room,Long.parseLong(g[i])); }
            if (Boolean.parseBoolean(g[5])) { return dongBoThuHoi(room,Long.parseLong(g[4]),phien); }
            return CompletableFuture.completedFuture(null);
        });
    }
    // ===== TYPING KHONG ACK =====
    public void dangGo(String room,String text) {
        if (text.isBlank() || !core.sanSang() || !choTyping(room,System.nanoTime()/1000000L)) { return; }
        CompletableFuture.runAsync(() -> core.guiTyping(room));
    }
    // Gioi han theo tung phong o Client; Server kiem tra doc lap lan nua.
    public synchronized boolean choTyping(String room,long luc) {
        Long cu=typing.get(room); if (cu!=null && luc-cu<Protocol.TYPING_MS) { return false; }
        typing.put(room,luc); return true;
    }
    // Goi tu multicast sau khi KenhPhong da loc roomId.
    void nhanTyping(String room,String user,String ten) {
        NguoiDung n=core.nguoiDung(); if (n!=null && !n.id.equals(user)) { listener.onDangGo(room,user,ten); }
    }
    // Don rieng phong vua roi, ke ca hang gui loi va revision.
    void roiPhong(String room) {
        dangGui.entrySet().removeIf(e -> e.getValue().tin.roomId.equals(room)); daThuHoi.remove(room); revision.remove(room); typing.remove(room);
    }
    // Vo hieu hoa callback dong bo cua phien cu.
    void xoa() { doi++; dangGui.clear(); typing.clear(); revision.clear(); daThuHoi.clear(); dangDongBo.clear(); }
    /** Trang thai mot tin dang cho, bao ve bang khoa cua chinh ban ghi. */
    private static final class ChoGui {
        volatile TinNhan tin; volatile boolean loi,ack,multicast; volatile long luot,serverId;
        // Tao hang gui cuc bo truoc request.
        ChoGui(TinNhan tin) { this.tin=tin; }
    }
}
