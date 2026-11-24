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
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WebApplication.class, FixtureConfig.class);
        app.run(args);
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
