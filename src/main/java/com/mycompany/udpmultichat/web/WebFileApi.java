package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.common.model.WebFile;
import com.mycompany.udpmultichat.server.WebChatService;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

/** Binary dung HTTP co xac thuc; upload xong khong tu tao tin chat. */
@RestController
@RequestMapping("/api/files")
public class WebFileApi {
    private final WebChatService service;
    public WebFileApi(WebChatService service) { this.service=service; }
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    WebFile upload(HttpSession session,@RequestParam String roomId,@RequestParam("file") MultipartFile file) throws IOException {
        String token=WebSecurity.token(session);
        var ticket=service.beginUpload(token,roomId,file.getOriginalFilename(),file.getSize());
        try(InputStream input=file.getInputStream()) {
            long crc=service.writeUpload(ticket,input);
            return service.finishUpload(token,ticket,crc);
        }finally { service.abortUpload(ticket); }
    }
    @GetMapping("/{id}")
    void download(HttpSession session,@PathVariable String id,@RequestParam(defaultValue="false") boolean thumbnail,HttpServletResponse response) throws IOException {
        try(var file=service.download(WebSecurity.token(session),id,thumbnail)) {
            response.setContentType(thumbnail?"image/jpeg":"application/octet-stream");
            response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
            response.setHeader("X-Content-Type-Options","nosniff");
            response.setHeader("X-Checksum-CRC32",Long.toUnsignedString(file.crc()));
            if(!thumbnail)response.setHeader(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString());
            response.setContentLengthLong(file.size());file.stream().transferTo(response.getOutputStream());
        }
    }
    @DeleteMapping("/{id}")
    Map<String,String> cancel(HttpSession session,@PathVariable String id) throws IOException {
        service.discardUpload(WebSecurity.token(session),id);return Map.of("status","removed");
    }
    @Scheduled(fixedDelay=3600000)
    void cleanup() throws IOException { service.cleanupFiles(); }
}
