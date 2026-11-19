package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.ReliableUdp;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Vai tro: Ghep request/response, timeout va huy cac request cua phien cu. Ai goi: ClientCore. Goi: ReliableUdp. */
final class YeuCauClient {
    private final Map<String,CompletableFuture<String[]>> dangCho=new ConcurrentHashMap<>();

    // ACK chi xac nhan van chuyen; Future nghiep vu chi thanh cong khi co RESPONSE.
    CompletableFuture<String[]> gui(ReliableUdp kenh,InetSocketAddress server,String ma,String noiDung) {
        if (kenh==null) { return CompletableFuture.failedFuture(new IOException("Chưa kết nối Server")); }
        CompletableFuture<String[]> ketQua=new CompletableFuture<>(); dangCho.put(ma,ketQua);
        ketQua.orTimeout(7,TimeUnit.SECONDS).whenComplete((v,e) -> dangCho.remove(ma,ketQua));
        kenh.gui(server,noiDung).whenComplete((v,e) -> { if (e!=null) { ketQua.completeExceptionally(e); } });
        return ketQua;
    }

    // Chi doc phan hoi da duoc Core kiem tra dung endpoint.
    void nhan(String[] p) {
        if (p.length<4) { return; }
        CompletableFuture<String[]> cho=dangCho.get(p[1]);
        if (cho==null) { return; }
        if ("OK".equals(p[2])) { cho.complete(p); }
        else { cho.completeExceptionally(new IOException(p[3])); }
    }

    // Bo map truoc callback de dong phien khong cham vao request moi.
    void xoa() {
        var cu=java.util.List.copyOf(dangCho.values()); dangCho.clear();
        cu.forEach(f -> f.completeExceptionally(new IOException("Kết nối đã đóng")));
    }
}
