package com.mycompany.udpmultichat.client;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/** Vai tro: Thu lai giao dich avatar khi mat ACK/phan hoi. Ai goi: ClientCore. Goi: API request cung ma. */
final class YeuCauAnh {
    // Khong khoi tao lop tien ich.
    private YeuCauAnh() { }

    // Ma nghiep vu giu nguyen; ReliableUdp van chi gui lai ba lan moi dot.
    static CompletableFuture<String[]> gui(Function<String,CompletableFuture<String[]>> gui) {
        return thu(gui,UUID.randomUUID().toString(),0);
    }

    // Chi thu lai loi van chuyen, khong lap loi du lieu/quyen tu Server.
    private static CompletableFuture<String[]> thu(Function<String,CompletableFuture<String[]>> gui,String ma,int lan) {
        return gui.apply(ma).handle((k,e) -> {
            if (e==null) { return CompletableFuture.completedFuture(k); }
            Throwable loi=e;
            while (loi instanceof CompletionException && loi.getCause()!=null) { loi=loi.getCause(); }
            boolean mat=loi instanceof TimeoutException || loi instanceof IOException &&
                "Không nhận được ACK sau 4 lần gửi".equals(loi.getMessage());
            if (mat && lan<3) { return thu(gui,ma,lan+1); }
            return CompletableFuture.<String[]>failedFuture(loi);
        }).thenCompose(f -> f);
    }
}
