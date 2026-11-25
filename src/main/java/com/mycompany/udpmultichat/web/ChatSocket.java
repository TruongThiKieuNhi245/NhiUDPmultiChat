package com.mycompany.udpmultichat.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.WebChatService;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {
    private final WebChatService service;
    private final UdpChatTransport udp;
    private final ObjectMapper json;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    public ChatSocket(WebChatService service, UdpChatTransport udp, ObjectMapper json) {
        this.service = service; this.udp = udp; this.json = json;
    }
    @jakarta.annotation.PostConstruct
    void attachEvents() { udp.onCommitted(this::broadcast);service.onEvent(this::event); }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, "/ws").addInterceptors(new HandshakeInterceptor() {
            @Override public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                                       WebSocketHandler handler, Map<String, Object> attributes) {
                if (!(req instanceof ServletServerHttpRequest servlet)) { return false; }
                URI uri = req.getURI();
                String expected = uri.getScheme() + "://" + uri.getRawAuthority();
                if (!expected.equals(req.getHeaders().getOrigin())) { return false; }
                HttpSession session = servlet.getServletRequest().getSession(false);
                String token = WebSecurity.token(session);
                if (!service.active(token)) { return false; }
                attributes.put("httpSession", session); attributes.put("token", token); return true;
            }
            @Override public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res, WebSocketHandler handler, Exception e) { }
        });
    }
    @Override public synchronized void afterConnectionEstablished(WebSocketSession raw) throws IOException {
        String token = (String) raw.getAttributes().get("token");
        if (peers.size() >= 128 || peers.values().stream().filter(p -> p.token.equals(token)).count() >= 3) {
            raw.close(CloseStatus.POLICY_VIOLATION); return;
        }
        raw.setTextMessageSizeLimit(8192);
        var session = new ConcurrentWebSocketSessionDecorator(raw, 1500, 64 * 1024);
        peers.put(raw.getId(), new Peer(token, (HttpSession) raw.getAttributes().get("httpSession"), session, new ArrayDeque<>()));
        send(session, Map.of("type", "ready"));
    }
    @Override protected void handleTextMessage(WebSocketSession raw, TextMessage incoming) throws IOException {
        Peer peer = peers.get(raw.getId());
        if (peer == null || !valid(peer) || !peer.allowFrame()) { raw.close(CloseStatus.POLICY_VIOLATION); return; }
        service.identity(peer.token);
        Command command;
        try { command = json.readValue(incoming.getPayload(), Command.class); }
        catch (IOException e) { send(peer.socket, Map.of("type", "error", "error", "Yêu cầu không hợp lệ.")); return; }
        if ("ping".equals(command.type())) { send(peer.socket, Map.of("type", "pong")); return; }
        if ("typing".equals(command.type())) {
            try { service.typing(peer.token,command.roomId()); } catch (IllegalArgumentException ignored) { }
            return;
        }
        if (!"send".equals(command.type()) || command.clientMsgId() == null || !command.clientMsgId().matches("[A-Za-z0-9_-]{22}")) {
            send(peer.socket, Map.of("type", "error", "error", "Yêu cầu không hợp lệ.")); return;
        }
        send(peer.socket, Map.of("type", "pending", "clientMsgId", command.clientMsgId()));
        udp.send(peer.token, command.roomId(), command.clientMsgId(),command.text(),command.kind()==null?"T":command.kind(),command.replyTo()).whenComplete((accepted, error) -> {
            if (error != null) {
                String reason = error instanceof IllegalArgumentException ? error.getMessage() : "Chưa nhận xác nhận lưu tin. Hãy thử lại.";
                send(peer.socket, Map.of("type", "error", "clientMsgId", command.clientMsgId(), "error", reason));
            } else {
                TinNhan t = accepted.message();
                service.deliver(t,message -> {
                    if (valid(peer) && service.canRead(peer.token,t.roomId)) send(peer.socket,Map.of("type","confirmed","message",message));
                });
            }
        });
    }
    private boolean valid(Peer peer) {
        return WebSecurity.token(peer.httpSession).equals(peer.token) && service.active(peer.token);
    }
    // Diem duy nhat phat tin moi: callback sau receive/commit UDP.
    private void broadcast(TinNhan message) {
        service.deliver(message,dto -> peers.values().forEach(peer -> {
            if (valid(peer) && service.canRead(peer.token, message.roomId) && send(peer.socket,
                    Map.of("type", "message", "message", dto))) {
                UdpChatTransport.trace("WS_DELIVER", message.roomId, message.maClient, message.msgId);
            }
        }));
        // Chi admin nhan invalidation thong ke, khong kem noi dung/ten/ma phong rieng.
        peers.values().forEach(peer->{if(valid(peer) && service.administrator(peer.token))send(peer.socket,Map.of("type","admin-updated"));});
    }
    private boolean send(WebSocketSession socket, Object value) {
        try {
            if (!socket.isOpen()) { return false; }
            socket.sendMessage(new TextMessage(json.writeValueAsString(value))); return true;
        } catch (IOException | RuntimeException e) {
            try { socket.close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) { }
            return false;
        }
    }
    @Scheduled(fixedDelay = 10_000)
    void heartbeat() {
        service.expire();
        peers.values().forEach(p -> {
            if (valid(p)) { send(p.socket, Map.of("type", "ping")); }
            else { closeIdentity(p.token); }
        });
    }
    public void closeIdentity(String token) {
        peers.values().stream().filter(p -> p.token.equals(token)).forEach(p -> {
            peers.remove(p.socket.getId());
            try { p.socket.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) { }
        });
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { peers.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        peers.remove(session.getId()); session.close(CloseStatus.SERVER_ERROR);
    }
    private void event(WebChatService.Event event) {
        if(event.type().equals("session-revoked")){closeIdentity((String)event.data());return;}
        peers.values().forEach(peer -> {
            if (valid(peer) && (!event.recipientId().isEmpty()?service.identity(peer.token).id().equals(event.recipientId()):
                    event.roomId().isEmpty() || service.canRead(peer.token,event.roomId()))) {
                send(peer.socket,Map.of("type",event.type(),"roomId",event.roomId(),"data",event.data()));
            }
        });
    }
    public record Command(String type,String roomId,String clientMsgId,String text,String kind,long replyTo) { }
    private record Peer(String token, HttpSession httpSession, WebSocketSession socket, ArrayDeque<Long> frames) {
        boolean allowFrame() {
            synchronized (frames) {
                long now = System.currentTimeMillis();
                while (!frames.isEmpty() && frames.peekFirst() <= now - 10000) { frames.removeFirst(); }
                if (frames.size() >= 40) { return false; }
                frames.addLast(now); return true;
            }
        }
    }
}
