package com.mycompany.udpmultichat.web;

import com.fasterxml.jackson.databind.*;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.WebChatService;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Web1IntegrationTest {
    private static final Path DATA = Path.of(".selftest/web1-integration-" + UUID.randomUUID());
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("chat.data-dir", () -> DATA.toString());
    }
    @LocalServerPort int port;
    @Autowired UdpChatTransport udp;
    @Autowired ObjectMapper json;
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    @Test void authenticatedChatMustReceiveUdpBeforePersistenceAndWebSocket() throws Exception {
        Client a = new Client(), b = new Client();
        assertEquals(401, a.get("/api/history").statusCode());
        assertEquals(403, a.request("/api/login", "{}", false).statusCode());
        a.registerLogin("webalice"); b.registerLogin("webbob");
        assertThrows(CompletionException.class, () -> a.connect("https://foreign.invalid"));
        Peer wa = a.connect("http://127.0.0.1:" + port), wb = b.connect("http://127.0.0.1:" + port);
        try {
            String first = TinNhan.taoMa();
            wa.command(first, "sanh", "Xin chào tiếng Việt: Hà Nội | \\ 🌱");
            JsonNode saved = wb.await("message").get("message");
            assertEquals(first, saved.get("clientMsgId").asText());
            assertEquals("Xin chào tiếng Việt: Hà Nội | \\ 🌱", saved.get("text").asText());
            wa.await("confirmed");
            assertEquals(1, a.history().size());
            System.out.println("PASS Web1: hai cookie / WS doc lap, tieng Viet, room + clientMsgId + msgId");

            udp.pauseReceiver();
            wa.ws.sendText("{\"type\":\"ping\"}", true).join(); wa.await("pong");
            String paused = TinNhan.taoMa();
            wa.command(paused, "sanh", "Tin khi UDP dừng"); wa.await("error");
            assertEquals(1, a.history().size());
            assertNull(wb.events.poll(300, TimeUnit.MILLISECONDS));
            System.out.println("PASS Web1: receiver dong, WS van pong, khong luu / khong broadcast");
            udp.resumeReceiver();
            wa.command(paused, "sanh", "Tin khi UDP dừng");
            assertEquals(paused, wb.await("message").path("message").path("clientMsgId").asText());
            long originalId = wa.await("confirmed").path("message").path("msgId").asLong();
            wa.command(paused, "sanh", "Tin khi UDP dừng");
            assertEquals(originalId, wa.await("confirmed").path("message").path("msgId").asLong());
            assertEquals(2, a.history().size());
            wa.command(paused, "sanh", "Nội dung bị thay đổi"); wa.await("error");
            assertEquals(2, a.history().size());
            assertNull(wb.events.poll(300, TimeUnit.MILLISECONDS));
            System.out.println("PASS Web1: retry sau khoi phuc / replay cung ma / payload khac bi chan");

            wa.command(TinNhan.taoMa(), "0123456789abcdef01234567", "Không thuộc phòng"); wa.await("error");
            wa.command(TinNhan.taoMa(), "sanh", "á".repeat(501)); wa.await("error");
            wa.ws.sendText("{\"type\":\"send\",\"roomId\":\"sanh\",\"clientMsgId\":\"" + TinNhan.taoMa() + "\",\"text\":\"giả mạo\",\"userId\":\"admin\"}", true).join(); wa.await("error");
            assertEquals(2, a.history().size());
            System.out.println("PASS Web1: trai phong / UTF-8 qua gioi han / gia mao danh tinh bi chan");

            wb.ws.sendClose(WebSocket.NORMAL_CLOSURE, "test reconnect").join();
            String offline = TinNhan.taoMa(); wa.command(offline, "sanh", "Tin bù sau mất kết nối"); wa.await("confirmed");
            Peer back = b.connect("http://127.0.0.1:" + port);
            try { assertEquals(3, b.history().size()); }
            finally { back.ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join(); }
            assertEquals(200, a.request("/api/logout", "{}", true).statusCode());
            assertEquals(401, a.get("/api/history").statusCode());
            assertTrue(wa.closed.await(3, TimeUnit.SECONDS));
            System.out.println("PASS Web1: reconnect lay bu lich su / logout thu hoi quyen");
        } finally { udp.resumeReceiver(); wa.ws.abort(); wb.ws.abort(); }
    }

    @Test void persistentIdempotencyAndDiskFailure(@TempDir Path directory) throws Exception {
        String id = TinNhan.taoMa(); long msgId;
        try (var service = new WebChatService(directory); var transport = new UdpChatTransport(service)) {
            service.register("persist", "sample-password");
            var identity = service.login("persist", "sample-password");
            msgId = transport.send(identity.token(), "sanh", id, "Qua restart vẫn một tin").get(5, TimeUnit.SECONDS).message().msgId;
            Path history;
            try (var files = Files.walk(directory)) { history = files.filter(f -> f.getFileName().toString().equals("sanh.log")).findFirst().orElseThrow(); }
            Path backup = history.resolveSibling("sanh.saved"); Files.move(history, backup); Files.createDirectory(history);
            int[] broadcasts = {0}; transport.onCommitted(t -> broadcasts[0]++);
            assertThrows(ExecutionException.class, () -> transport.send(identity.token(), "sanh", TinNhan.taoMa(), "Lỗi đĩa").get(5, TimeUnit.SECONDS));
            assertEquals(0, broadcasts[0]); assertEquals(1, service.history(identity.token(), "sanh", 0).size());
            Files.delete(history); Files.move(backup, history);
        }
        try (var service = new WebChatService(directory); var transport = new UdpChatTransport(service)) {
            var identity = service.login("persist", "sample-password");
            var duplicate = transport.send(identity.token(), "sanh", id, "Qua restart vẫn một tin").get(5, TimeUnit.SECONDS);
            assertTrue(duplicate.duplicate()); assertEquals(msgId, duplicate.message().msgId);
            assertEquals(1, service.history(identity.token(), "sanh", 0).size());
        }
        System.out.println("PASS Web1: ghi loi khong broadcast/RAM; restart giu khoa idempotency");
    }

    @Test void webLoginUpgradesLegacyPasswordInSameStore(@TempDir Path directory) throws Exception {
        var store = new com.mycompany.udpmultichat.server.KhoLuuTru(directory, text -> { });
        var desktop = new com.mycompany.udpmultichat.server.QuanLyNguoiDung(store);
        desktop.dangKy("legacy", "sample-password");
        assertFalse(Files.readString(directory.resolve("users.csv")).contains("pbkdf2$"));
        try (var service = new WebChatService(directory)) {
            service.login("legacy", "sample-password");
            assertTrue(Files.readString(directory.resolve("users.csv")).contains("pbkdf2$210000$"));
        }
        try (var service = new WebChatService(directory)) {
            assertEquals("legacy", service.login("legacy", "sample-password").login());
        }
        System.out.println("PASS Web1: dang nhap web nang SHA-256 cu sang PBKDF2, restart dang nhap duoc");
    }

    @Test void historyPagesAreBoundedOrderedAndAuthorized(@TempDir Path directory) throws Exception {
        var disk = new com.mycompany.udpmultichat.server.KhoLuuTru(directory, text -> { });
        for (int i = 1; i <= 123; i++) {
            disk.ghiTin(new TinNhan(i, "sanh", "fixture", "Lịch sử", "Tin " + i, i, TinNhan.taoMa(), "T"));
        }
        try (var service = new WebChatService(directory)) {
            service.register("paging", "sample-password");
            var identity = service.login("paging", "sample-password");
            var latest = service.historyPage(identity.token(), "sanh", 0);
            assertEquals(50, latest.size()); assertEquals(74, latest.getFirst().msgId); assertEquals(123, latest.getLast().msgId);
            var older = service.historyPage(identity.token(), "sanh", 74);
            assertEquals(50, older.size()); assertEquals(24, older.getFirst().msgId); assertEquals(73, older.getLast().msgId);
            var first = service.historyPage(identity.token(), "sanh", 24);
            assertEquals(23, first.size()); assertEquals(1, first.getFirst().msgId);
            assertTrue(service.historyPage(identity.token(), "sanh", 1).isEmpty());
            assertEquals(123, service.historyPage(identity.token(), "sanh", Long.MAX_VALUE).getLast().msgId);
            assertThrows(IllegalArgumentException.class, () -> service.historyPage(identity.token(), "other", 0));
            service.logout(identity.token());
            assertThrows(IllegalArgumentException.class, () -> service.historyPage(identity.token(), "sanh", 0));
        }
        Client anonymous = new Client();
        assertEquals(401, anonymous.get("/api/history/page").statusCode());
    }

    private final class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3)).build();
        String csrf;
        HttpResponse<String> get(String path) throws Exception { return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> request(String path, String body, boolean withCsrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json");
            if (withCsrf) { builder.header("X-CSRF-TOKEN", csrf); }
            return http.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        void registerLogin(String login) throws Exception {
            csrf = json.readTree(get("/api/csrf").body()).get("token").asText();
            String body = json.writeValueAsString(Map.of("login", login, "password", "sample-password"));
            assertEquals(200, request("/api/register", body, true).statusCode());
            var loginResponse = request("/api/login", body, true); assertEquals(200, loginResponse.statusCode(), loginResponse.body());
            assertFalse(loginResponse.body().contains("token"));
        }
        Peer connect(String origin) {
            Peer peer = new Peer(); peer.ws = http.newWebSocketBuilder().header("Origin", origin)
                    .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"), peer).join();
            try { peer.await("ready"); } catch (Exception e) { throw new IllegalStateException(e); }
            return peer;
        }
        JsonNode history() throws Exception { return json.readTree(get("/api/history").body()); }
    }
    private final class Peer implements WebSocket.Listener {
        final BlockingQueue<JsonNode> events = new LinkedBlockingQueue<>();
        final StringBuilder buffer = new StringBuilder();
        final CountDownLatch closed = new CountDownLatch(1);
        WebSocket ws;
        @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence text, boolean last) {
            buffer.append(text);
            if (last) {
                try { events.add(json.readTree(buffer.toString())); }
                catch (java.io.IOException e) { throw new IllegalStateException(e); }
                buffer.setLength(0);
            }
            webSocket.request(1); return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket socket, int status, String reason) { closed.countDown(); return null; }
        void command(String id, String room, String text) throws Exception {
            ws.sendText(json.writeValueAsString(Map.of("type", "send", "roomId", room, "clientMsgId", id, "text", text)), true).join();
        }
        JsonNode await(String type) throws Exception {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
            while (System.nanoTime() < until) {
                JsonNode event = events.poll(100, TimeUnit.MILLISECONDS);
                if (event == null) { continue; }
                if (event.path("type").asText().equals("ping")) { continue; }
                if (event.path("type").asText().equals(type)) { return event; }
            }
            fail("Khong nhan " + type); return null;
        }
    }
}
