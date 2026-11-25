package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.WebChatService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class WebSecurity {
    public static final String TOKEN = "chatIdentity";
    static String token(HttpSession session) {
        if (session == null) { return ""; }
        try { Object value = session.getAttribute(TOKEN); return value == null ? "" : value.toString(); }
        catch (IllegalStateException e) { return ""; }
    }
    @Bean
    SecurityFilterChain security(HttpSecurity http, WebChatService service) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/admin", "/index.html", "/admin-ui.js", "/app.js", "/chat-view.js", "/chat-store.js", "/workspace-ui.js", "/file-upload.js", "/app.css", "/error",
                "/api/csrf", "/api/login", "/api/register").permitAll().anyRequest().authenticated());
        http.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)));
        http.headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                "default-src 'self'; connect-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'none'")));
        http.addFilterAfter(new OncePerRequestFilter() {
            private final java.util.Map<String, java.util.ArrayDeque<Long>> attempts = new java.util.HashMap<>();
            private synchronized boolean allow(String ip) {
                long now = System.currentTimeMillis();
                attempts.entrySet().removeIf(e -> e.getValue().peekLast() < now - 60_000);
                if (!attempts.containsKey(ip) && attempts.size() >= 1024) { return false; }
                var queue = attempts.computeIfAbsent(ip, k -> new java.util.ArrayDeque<>());
                while (!queue.isEmpty() && queue.peekFirst() < now - 60_000) { queue.removeFirst(); }
                if (queue.size() >= 15) { return false; }
                queue.addLast(now); return true;
            }
            @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                boolean upload=req.getRequestURI().equals("/api/files/upload") && req.getMethod().equals("POST");
                if(upload && (token(req.getSession(false)).isEmpty() || !service.active(token(req.getSession(false))))) { res.sendError(401);return; }
                if(upload && req.getHeader("X-CSRF-TOKEN")==null) { res.sendError(403);return; }
                if (req.getContentLengthLong() > (upload?11*1024*1024:req.getRequestURI().equals("/api/actions/profile-save")?96000:8192)) { res.sendError(413); return; }
                if ("POST".equals(req.getMethod()) && java.util.Set.of("/api/login", "/api/register").contains(req.getRequestURI())
                        && !allow(req.getRemoteAddr())) { res.sendError(429); return; }
                String key = token(req.getSession(false));
                if (!key.isEmpty()) {
                    try {
                        var identity = service.identity(key);
                        var auth = UsernamePasswordAuthenticationToken.authenticated(identity.id(), null, java.util.List.of());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (IllegalArgumentException e) {
                        SecurityContextHolder.clearContext();
                        // Xoa danh tinh da thu hoi, de cung cookie co the dang nhap lai.
                        HttpSession stale=req.getSession(false);
                        if(stale!=null)try{stale.removeAttribute(TOKEN);}catch(IllegalStateException ignored){ }
                    }
                }
                chain.doFilter(upload?req:new BoundedJsonRequest(req,req.getRequestURI().equals("/api/actions/profile-save")?96000:8192),res);
            }
        }, SecurityContextHolderFilter.class);
        return http.build();
    }
    @Bean
    org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer jsonLimits() {
        return builder -> {
            builder.featuresToEnable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(
                com.fasterxml.jackson.core.StreamReadConstraints.builder().maxStringLength(88000).maxNestingDepth(10).build()));
        };
    }
    @Bean
    ServletListenerRegistrationBean<HttpSessionListener> sessionLifecycle(WebChatService service, ChatSocket socket) {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
            @Override public void sessionDestroyed(HttpSessionEvent event) {
                String token = WebSecurity.token(event.getSession());
                service.logout(token); socket.closeIdentity(token);
            }
        });
    }
}
