package com.mycompany.udpmultichat.client;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Chi ghi ten dang nhap va Server lan truoc; khong luu mat khau/token. */
public final class CauHinhClient {
    private CauHinhClient() { }
    public static Path tepCauHinh() { return Path.of(System.getProperty("udpmultichat.clientConfig","client.properties")); }
    public static Properties doc() {
        Properties p=new Properties();
        Path tep=tepCauHinh();
        if (Files.exists(tep)) {
            try (Reader r=Files.newBufferedReader(tep,StandardCharsets.UTF_8)) { p.load(r); }
            catch (IOException e) { /* Mac dinh neu file tuy chon khong doc duoc. */ }
        }
        return p;
    }
    // Ghi dong bo cho nhieu cua so tren cung JVM, doi ten file tam cung thu muc.
    public static synchronized void ghi(String ten,String server) throws IOException {
        Properties p=new Properties(); p.setProperty("username",ten); p.setProperty("server",server);
        StringWriter w=new StringWriter(); p.store(w,"Khong luu mat khau");
        Path tep=tepCauHinh().toAbsolutePath();
        Path tam=Files.createTempFile(tep.getParent(),"client-",".tmp");
        try {
            Files.writeString(tam,w.toString(),StandardCharsets.UTF_8);
            try { Files.move(tam,tep,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(tam,tep,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tam); }
    }
}
