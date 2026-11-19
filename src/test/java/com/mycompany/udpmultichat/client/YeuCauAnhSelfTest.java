package com.mycompany.udpmultichat.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** Vai tro: Tai hien het ACK o tang request avatar, khong sua loss fixture. Ai goi: HoSoSelfTest. Goi: YeuCauAnh. */
public final class YeuCauAnhSelfTest {
    // Khong khoi tao lop tien ich.
    private YeuCauAnhSelfTest() { }

    // Ca het bon lan ACK phai thu lai cung ma; loi nghiep vu khong duoc lap.
    public static void kiemTra() {
        List<String> ma=new ArrayList<>();
        String[] k=YeuCauAnh.gui(id -> {
            ma.add(id);
            return ma.size()==1?CompletableFuture.failedFuture(new IOException("Không nhận được ACK sau 4 lần gửi")):
                CompletableFuture.completedFuture(new String[]{"OK"});
        }).join();
        can(k[0].equals("OK") && ma.size()==2 && ma.get(0).equals(ma.get(1)),"Retry doi ma giao dich");
        AtomicInteger so=new AtomicInteger();
        var loi=YeuCauAnh.gui(id -> { so.incrementAndGet(); return CompletableFuture.failedFuture(new IOException("Không có quyền")); });
        can(loi.isCompletedExceptionally() && so.get()==1,"Lap loi quyen");
        so.set(0);
        loi=YeuCauAnh.gui(id -> { so.incrementAndGet(); return CompletableFuture.failedFuture(new IOException("Không nhận được ACK sau 4 lần gửi")); });
        can(loi.isCompletedExceptionally() && so.get()==4,"Retry khong co gioi han");
    }
    // Bao assertion khong nuot ngoai le nghiep vu.
    private static void can(boolean dung,String loi) { if (!dung) { throw new AssertionError(loi); } }
}
