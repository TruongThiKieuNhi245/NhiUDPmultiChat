package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.WebChatService;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WebApi {
    private final WebChatService service;
    private final UdpChatTransport udp;
    public WebApi(WebChatService service, UdpChatTransport udp) { this.service = service; this.udp = udp; }
    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "header", token.getHeaderName()); }
    @PostMapping("/register")
    Map<String, String> register(@RequestBody Credentials body) throws IOException {
        validate(body); service.register(body.login(), body.password()); return Map.of("status", "created");
    }
    @PostMapping("/login")
    Map<String, String> login(@RequestBody Credentials body, HttpServletRequest request) throws IOException {
        validate(body);
        HttpSession session = request.getSession();
        if (!WebSecurity.token(session).isEmpty()) { throw new IllegalArgumentException("Hãy đăng xuất trước."); }
        var identity = service.login(body.login(), body.password());
        request.changeSessionId(); session.setAttribute(WebSecurity.TOKEN, identity.token());
        return publicIdentity(identity);
    }
    @GetMapping("/me")
    Map<String, String> me(HttpSession session) { return publicIdentity(service.identity(WebSecurity.token(session))); }
    @PostMapping("/logout")
    Map<String, String> logout(HttpSession session) { session.invalidate(); return Map.of("status", "logged-out"); }
    @GetMapping("/history")
    List<ChatMessage> history(HttpSession session, @RequestParam(defaultValue = "sanh") String roomId,
                             @RequestParam(defaultValue = "0") long after) {
        if (after < 0) { throw new IllegalArgumentException("Cursor không hợp lệ."); }
        return service.history(WebSecurity.token(session), roomId, after).stream().map(ChatMessage::from).toList();
    }
    @GetMapping("/health")
    Map<String, Object> health() { return Map.of("http", "up", "udpReceiver", udp.running(), "lastUdpReceive", udp.lastReceived()); }
    private static void validate(Credentials body) {
        if (body.login() == null || body.password() == null || body.login().length() > 20 || body.password().length() > 128) {
            throw new IllegalArgumentException("Kiểm tra tên đăng nhập và mật khẩu.");
        }
    }
    static Map<String, String> publicIdentity(WebChatService.Identity id) {
        return Map.of("id", id.id(), "login", id.login(), "name", id.name());
    }
    public record Credentials(String login, String password) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }
    @RestControllerAdvice
    static class Errors {
        @ExceptionHandler(IllegalArgumentException.class)
        ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        @ExceptionHandler(IOException.class)
        ResponseEntity<Map<String, String>> storage(IOException e) {
            return ResponseEntity.status(503).body(Map.of("error", "Không lưu được dữ liệu. Hãy thử lại."));
        }
    }
}
