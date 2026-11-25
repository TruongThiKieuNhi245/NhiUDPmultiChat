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
        String previous=WebSecurity.token(session);
        if (!previous.isEmpty() && service.active(previous)) { throw new IllegalArgumentException("Hãy đăng xuất trước."); }
        session.removeAttribute(WebSecurity.TOKEN);
        var identity = service.login(body.login(), body.password());
        request.changeSessionId(); session.setAttribute(WebSecurity.TOKEN, identity.token());
        return publicIdentity(identity);
    }
    @GetMapping("/me")
    Map<String, String> me(HttpSession session) { return publicIdentity(service.identity(WebSecurity.token(session))); }
    @PostMapping("/logout")
    Map<String, String> logout(HttpSession session) { session.invalidate(); return Map.of("status", "logged-out"); }
    @GetMapping("/history")
    List<com.mycompany.udpmultichat.common.model.WebMessage> history(HttpSession session, @RequestParam(defaultValue = "sanh") String roomId,
                             @RequestParam(defaultValue = "0") long after) {
        if (after < 0) { throw new IllegalArgumentException("Cursor không hợp lệ."); }
        return service.messages(WebSecurity.token(session),roomId,after,false);
    }
    @GetMapping("/health")
    Map<String, Object> health() { return Map.of("http", "up", "udpReceiver", udp.running(), "lastUdpReceive", udp.lastReceived()); }
    @GetMapping("/history/page")
    List<com.mycompany.udpmultichat.common.model.WebMessage> historyPage(HttpSession session, @RequestParam(defaultValue = "sanh") String roomId,
                                @RequestParam(defaultValue = "0") long before) {
        if (before < 0) { throw new IllegalArgumentException("Cursor không hợp lệ."); }
        return service.messages(WebSecurity.token(session),roomId,before,true);
    }
    @GetMapping("/workspace")
    Map<String,Object> workspace(HttpSession session) { return service.snapshot(WebSecurity.token(session)); }
    @PostMapping("/actions/{action}")
    Object action(HttpSession session,@PathVariable String action,@RequestBody Map<String,String> args) throws IOException {
        if (args.size()>12 || args.values().stream().anyMatch(v -> v==null || v.length()>(action.equals("profile-save")?88000:4096))) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ.");
        }
        return service.action(WebSecurity.token(session),action,args);
    }
    @GetMapping("/members")
    Object members(HttpSession session,@RequestParam String roomId) { return service.members(WebSecurity.token(session),roomId); }
    @GetMapping("/moderation")
    Object moderation(HttpSession session,@RequestParam String roomId) { return service.restrictions(WebSecurity.token(session),roomId); }
    @GetMapping("/users/search")
    Object users(HttpSession session,@RequestParam String q) { return service.searchUsers(WebSecurity.token(session),q); }
    @GetMapping("/invitations/sent")
    Object sent(HttpSession session,@RequestParam String roomId) { return service.sentInvites(WebSecurity.token(session),roomId); }
    @GetMapping("/avatars/{id}")
    ResponseEntity<byte[]> avatar(HttpSession session,@PathVariable String id) throws IOException {
        byte[] bytes=service.avatar(WebSecurity.token(session),id);
        return bytes.length==0?ResponseEntity.notFound().build():ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore()).body(bytes);
    }
    @GetMapping("/stickers")
    Object stickers() {
        return Map.of("groups",com.mycompany.udpmultichat.common.StickerDanhMuc.NHOM,
                "items",com.mycompany.udpmultichat.common.StickerDanhMuc.CAC_MUC);
    }
    @GetMapping(value="/stickers/{code}.png",produces="image/png")
    org.springframework.core.io.Resource sticker(@PathVariable String code) {
        if (com.mycompany.udpmultichat.common.StickerDanhMuc.tim(code)==null) throw new IllegalArgumentException("Sticker không hỗ trợ.");
        return new org.springframework.core.io.ClassPathResource("stickers/"+code+".png");
    }
    @GetMapping("/search")
    Object search(HttpSession session,@RequestParam String roomId,@RequestParam String q,@RequestParam(defaultValue="0") long after) {
        return service.search(WebSecurity.token(session),roomId,q,after);
    }
    @GetMapping("/history/around")
    Object around(HttpSession session,@RequestParam String roomId,@RequestParam long msgId) {
        return service.around(WebSecurity.token(session),roomId,msgId);
    }
    @GetMapping("/recalls")
    Object recalls(HttpSession session,@RequestParam String roomId,@RequestParam(defaultValue="0") long after) {
        return service.recalls(WebSecurity.token(session),roomId,after);
    }
    @PostMapping("/recall")
    Object recall(HttpSession session,@RequestBody Recall body) throws IOException {
        service.recall(WebSecurity.token(session),body.roomId(),body.msgId()); return Map.of("status","ok");
    }
    public record Recall(String roomId,long msgId) { }
    @GetMapping("/export")
    ResponseEntity<byte[]> export(HttpSession session,@RequestParam String roomId,@RequestParam(defaultValue="txt") String format) {
        if(!Set.of("txt","html").contains(format))throw new IllegalArgumentException("Chọn HTML hoặc TXT.");
        boolean html=format.equals("html");String text=html?service.exportHtml(WebSecurity.token(session),roomId):service.export(WebSecurity.token(session),roomId);
        return ResponseEntity.ok().contentType(new MediaType("text",html?"html":"plain",java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=lich-su."+format)
                .body(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
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
        @ExceptionHandler(com.mycompany.udpmultichat.server.WebAdministration.Forbidden.class)
        ResponseEntity<Map<String,String>> forbidden(com.mycompany.udpmultichat.server.WebAdministration.Forbidden e){return ResponseEntity.status(403).body(Map.of("error",e.getMessage()));}
        @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
        ResponseEntity<Map<String,String>> uploadTooLarge() {
            return ResponseEntity.status(413).body(Map.of("error","Tệp vượt 10 MB. Hãy chọn tệp nhỏ hơn hoặc chia tệp trước khi gửi."));
        }
        @ExceptionHandler(BoundedJsonRequest.TooLarge.class)
        ResponseEntity<Map<String,String>> bodyTooLarge() {
            return ResponseEntity.status(413).body(Map.of("error","Dữ liệu yêu cầu quá lớn. Hãy rút ngắn hoặc chọn ảnh nhỏ hơn."));
        }
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
