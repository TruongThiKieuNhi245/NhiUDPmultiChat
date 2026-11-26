package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class WebAdminApi {
    private final WebChatService service;private final UdpChatTransport udp;
    private final Semaphore exports=new Semaphore(2);
    public WebAdminApi(WebChatService service,UdpChatTransport udp){this.service=service;this.udp=udp;}
    @GetMapping("/snapshot")
    Object snapshot(HttpSession session) throws IOException{return service.adminSnapshot(WebSecurity.token(session));}
    @PostMapping("/actions/{action}")
    Object action(HttpSession session,@PathVariable String action,@RequestBody Map<String,String> args) throws IOException {
        if(args.size()>12 || args.values().stream().anyMatch(v->v==null || v.length()>4096))throw new IllegalArgumentException("Dữ liệu thao tác không hợp lệ.");
        return service.adminAction(WebSecurity.token(session),action,args);
    }
    @PostMapping("/broadcast")
    Object broadcast(HttpSession session,@RequestBody Broadcast body) throws IOException {
        String token=WebSecurity.token(session);var plan=service.broadcastPlan(token,body.id(),body.text());
        var futures=plan.rooms().stream().map(room->udp.send(token,room,plan.id(),plan.text(),"Y",0).handle((accepted,error)->error==null)).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();long committed=futures.stream().filter(CompletableFuture::join).count();
        return Map.of("committed",committed,"failed",futures.size()-committed,"id",plan.id());
    }
    public record Broadcast(String id,String text) { }
    @GetMapping("/reports/{id}/{format}")
    ResponseEntity<byte[]> report(HttpSession session,@PathVariable String id,@PathVariable String format) throws IOException {
        String token=WebSecurity.token(session);var snapshot=service.adminReport(token,id);
        if(!exports.tryAcquire())return ResponseEntity.status(429).build();
        try {
            service.auditExport(token,id,format);
            byte[] bytes;String mime,name;
            switch(format){
                case "pdf":bytes=WebReports.pdf(snapshot,"all");mime="application/pdf";name="bao-cao.pdf";break;
                case "pdf-overview":bytes=WebReports.pdf(snapshot,"overview");mime="application/pdf";name="bao-cao-tong-quan.pdf";break;
                case "pdf-users":bytes=WebReports.pdf(snapshot,"users");mime="application/pdf";name="bao-cao-nguoi-dung.pdf";break;
                case "pdf-rooms":bytes=WebReports.pdf(snapshot,"rooms");mime="application/pdf";name="bao-cao-phong.pdf";break;
                case "pdf-audit":bytes=WebReports.pdf(snapshot,"audit");mime="application/pdf";name="bao-cao-nhat-ky.pdf";break;
                case "zip":bytes=WebReports.zip(snapshot.data());mime="application/zip";name="bao-cao-nang-cao.zip";break;
                case "html":bytes=WebReports.html(snapshot.data()).getBytes(java.nio.charset.StandardCharsets.UTF_8);mime="text/html;charset=UTF-8";name="tong_hop.html";break;
                case "audit":bytes=WebReports.csv(List.of("Thời gian","Người thực hiện","Hành động","Đối tượng","Chi tiết"),snapshot.data().lay("AUDIT"));mime="text/csv;charset=UTF-8";name="nhatky.csv";break;
                default:throw new IllegalArgumentException("Định dạng báo cáo không hợp lệ.");
            }
            service.adminReport(token,id);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+name).body(bytes);
        }finally{exports.release();}
    }
    @Controller
    static class Page {
        @GetMapping("/admin") String admin(){return "forward:/index.html";}
    }
}
