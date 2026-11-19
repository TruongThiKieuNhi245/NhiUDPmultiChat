package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.Goi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Vai tro: Audit UTF-8 moi su kien mot dong, xoay 5MB. Ai goi: QuanTriServer, KiemDuyet. Goi: Files, Goi. */
public final class NhatKy {
    private final Path tep; private final long gioiHan;
    // Gioi han rieng chi dung trong test xoay log.
    public NhatKy(Path data,long gioiHan) throws IOException { Files.createDirectories(data); tep=data.resolve("audit.log"); this.gioiHan=gioiHan; }
    // ===== GHI VA DOC AUDIT =====
    public synchronized void ghi(String actor,String lenh,String doiTuong,String chiTiet) throws IOException {
        String dong=Goi.ghep(Instant.now().toString(),actor,lenh,doiTuong,chiTiet)+"\n";
        if (Files.exists(tep) && Files.size(tep)+dong.getBytes(StandardCharsets.UTF_8).length>gioiHan) { Files.move(tep,tep.resolveSibling("audit.1.log"),StandardCopyOption.REPLACE_EXISTING); }
        Files.writeString(tep,dong,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    // Doc toi da 1000 dong gan nhat, khong nap nhat ky vo han vao UI.
    public synchronized List<List<String>> doc() throws IOException {
        if (!Files.exists(tep)) { return List.of(); }
        ArrayDeque<List<String>> ds=new ArrayDeque<>();
        try (var in=Files.newBufferedReader(tep,StandardCharsets.UTF_8)) {
            String dong; while ((dong=in.readLine())!=null) { if (ds.size()==1000) { ds.removeFirst(); } ds.addLast(List.of(Goi.tach(dong))); }
        }
        return List.copyOf(ds);
    }
}
