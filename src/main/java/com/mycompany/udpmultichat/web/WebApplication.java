package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.WebChatService;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class WebApplication {
    public static void main(String[] args) { SpringApplication.run(WebApplication.class, args); }
    @Bean(destroyMethod = "close")
    WebChatService chatService(@Value("${chat.data-dir}") String directory,@Value("${chat.initial-admin-password:}") String password) throws IOException {
        return new WebChatService(Path.of(directory),password);
    }
    @Bean(destroyMethod = "close")
    UdpChatTransport udpTransport(WebChatService service) throws IOException { return new UdpChatTransport(service); }
}
