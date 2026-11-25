package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.net.SocketException;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

/** Diem chay QA tren test classpath; dieu khien UDP khong duoc dong goi vao JAR. */
public final class Web1BrowserFixture {
    private Web1BrowserFixture() { }
    public static void main(String[] args) throws java.io.IOException {
        if (java.util.Arrays.asList(args).contains("--fixture.ux=true")) { seedUx(args); }
        if (java.util.Arrays.asList(args).contains("--fixture.force-password=true")) {
            String directory=java.util.Arrays.stream(args).filter(a->a.startsWith("--chat.data-dir=")).findFirst().orElseThrow().substring("--chat.data-dir=".length());
            var path=java.nio.file.Path.of(directory);java.nio.file.Files.createDirectories(path);
            java.nio.file.Files.writeString(path.resolve("config.properties"),"admin.password=admin123\n",java.nio.file.StandardOpenOption.CREATE_NEW);
        }
        SpringApplication app = new SpringApplication(WebApplication.class, FixtureConfig.class);
        app.run(args);
    }
    // Chi tao du lieu QA trong thu muc moi, khong co endpoint ghi lich su trong JAR.
    private static void seedUx(String[] args) throws java.io.IOException {
        String directory = java.util.Arrays.stream(args).filter(a -> a.startsWith("--chat.data-dir="))
                .findFirst().orElseThrow().substring("--chat.data-dir=".length());
        var path = java.nio.file.Path.of(directory);
        java.nio.file.Files.createDirectories(path);
        java.nio.file.Files.writeString(path.resolve("config.properties"), "admin.password=fixture-private-password\n",
                java.nio.file.StandardOpenOption.CREATE_NEW);
        var store = new com.mycompany.udpmultichat.server.KhoLuuTru(path, text -> { });
        var users = new com.mycompany.udpmultichat.server.QuanLyNguoiDung(store, true);
        var ids = new java.util.ArrayList<String>();
        for (String login : java.util.List.of("ux_alice", "ux_bob")) {
            var user = users.dangKy(login, "Web1-fixture-password");
            var session = users.dangNhap(login, "Web1-fixture-password", new java.net.InetSocketAddress("127.0.0.1", 0));
            users.luuHoSo(session, "Cùng tên", "", "");
            users.dangXuat(session);
            ids.add(user.id);
        }
        long start = System.currentTimeMillis() - 172800000;
        for (int i = 1; i <= 680; i++) {
            store.ghiTin(new com.mycompany.udpmultichat.common.model.TinNhan(i, "sanh", ids.get(i % 2), "Cùng tên",
                    "Tin lịch sử " + i + " · Tiếng Việt rõ ràng 🌿", start + i * 120000L,
                    com.mycompany.udpmultichat.common.model.TinNhan.taoMa(), "T"));
        }
    }
    @TestConfiguration
    @EnableAutoConfiguration
    @Import(Control.class)
    public static class FixtureConfig { }
    @RestController
    public static class Control {
        private final UdpChatTransport udp;
        public Control(UdpChatTransport udp) { this.udp = udp; }
        @PostMapping("/api/fixture/udp")
        Map<String, Object> udp(@RequestBody Map<String, Boolean> body) throws SocketException {
            if (Boolean.TRUE.equals(body.get("running"))) { udp.resumeReceiver(); } else { udp.pauseReceiver(); }
            return Map.of("running", udp.running());
        }
    }
}
