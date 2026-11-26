package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Web3AdminTest {
    static final String PASSWORD="Admin-integration-private-password";
    String admin(WebChatService s) throws IOException{return s.login("admin",PASSWORD).token();}
    String account(WebChatService s,String name) throws IOException{s.register(name,"member-private-password");return s.login(name,"member-private-password").token();}
    String room(WebChatService s,String token,String name) throws IOException{return ((Map<?,?>)s.action(token,"room-create",Map.of("name",name,"description","Fixture","publicRoom","true","password","","capacity","30"))).get("roomId").toString();}
    Map<String,String> edit(String id,String role){return Map.of("id",id,"name","Tên mới","about","Hồ sơ thử nghiệm","role",role);}
    long stat(WebAdministration.Snapshot snap,String name){return Long.parseLong(snap.data().lay("STATS").stream().filter(r->r.get(0).equals(name)).findFirst().orElseThrow().get(1));}
    @Test void guardsSnapshotOwnershipRolesAndRevocation(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=admin(s),member=account(s,"ordinary"),id=s.identity(member).id();
            assertThrows(WebAdministration.Forbidden.class,()->s.adminSnapshot(member));
            assertThrows(WebAdministration.Forbidden.class,()->s.adminAction(member,"user-edit",edit(id,"ADMIN")));
            var snapshot=s.adminSnapshot(root);s.adminAction(root,"user-edit",edit(id,"ADMIN"));assertFalse(s.active(member));
            String promoted=s.login("ordinary","member-private-password").token();
            assertThrows(IllegalArgumentException.class,()->s.adminReport(promoted,snapshot.id()));
            String rootId=s.identity(root).id();s.adminAction(promoted,"user-edit",edit(rootId,"THANH_VIEN"));assertFalse(s.active(root));
            assertThrows(IllegalArgumentException.class,()->s.adminReport(root,snapshot.id()));
            assertThrows(IllegalArgumentException.class,()->s.adminAction(promoted,"user-edit",edit(id,"THANH_VIEN")));
            assertThrows(IllegalArgumentException.class,()->s.adminAction(promoted,"user-lock",Map.of("id",id,"locked","true","reason","self")));
            assertThrows(IllegalArgumentException.class,()->s.adminAction(promoted,"user-delete",Map.of("id",id,"confirm","ordinary")));
            assertEquals(1,s.adminSnapshot(promoted).data().lay("USERS").stream().filter(r->r.get(3).equals("ADMIN")).count());
        }
    }
    @Test void resetTemporaryPasswordLockAndCrud(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=admin(s),member=account(s,"resetme"),id=s.identity(member).id();
            var reset=(Map<?,?>)s.adminAction(root,"user-reset",Map.of("id",id));String temporary=reset.get("temporaryPassword").toString();assertFalse(s.active(member));
            String fresh=s.login("resetme",temporary).token();assertTrue(((WebWorkspace.Profile)s.snapshot(fresh).get("profile")).forcePassword());
            assertThrows(IllegalArgumentException.class,()->s.beginUpload(fresh,"sanh","x.txt",1));
            s.action(fresh,"password-change",Map.of("current",temporary,"next","member-new-private-password"));
            s.adminAction(root,"user-lock",Map.of("id",id,"locked","true","reason","Fixture lock"));assertFalse(s.active(fresh));
            assertThrows(IllegalArgumentException.class,()->s.login("resetme","member-new-private-password"));
            s.adminAction(root,"user-lock",Map.of("id",id,"locked","false","reason","Fixture unlock"));
            String back=s.login("resetme","member-new-private-password").token();assertTrue(s.active(back));
            var added=(Map<?,?>)s.adminAction(root,"user-add",Map.of("login","added","name","Người mới","role","THANH_VIEN","password","Temporary-private-password"));assertNotNull(added.get("id"));
            s.adminAction(root,"user-delete",Map.of("id",id,"confirm","resetme"));assertFalse(s.active(back));
            assertThrows(IllegalArgumentException.class,()->s.login("resetme","member-new-private-password"));
        }
    }
    @Test void snapshotNumbersExportsAndHistoryEscaping(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD);var udp=new UdpChatTransport(s)){
            String root=admin(s),a=account(s,"reporta"),b=account(s,"reportb"),room=room(s,a,"Phòng tiếng Việt");s.action(b,"room-join",Map.of("key",room,"password",""));
            s.action(a,"profile-save",Map.of("name","=SUM(1,1)<script>","about","<script>","avatarMode","KEEP"));
            String first=TinNhan.taoMa();udp.send(a,room,first,"bí mật sẽ thu hồi").get(5,TimeUnit.SECONDS);
            udp.send(a,room,TinNhan.taoMa(),"<script>alert(1)</script>","T",1).get(5,TimeUnit.SECONDS);
            var ticket=s.beginUpload(a,room,"tệp.txt",3);var file=s.finishUpload(a,ticket,s.writeUpload(ticket,new ByteArrayInputStream(new byte[]{1,2,3})));s.abortUpload(ticket);
            udp.send(a,room,TinNhan.taoMa(),file.text(),"F",0).get(5,TimeUnit.SECONDS);
            var waiting=s.beginUpload(a,room,"chờ.txt",5);s.finishUpload(a,waiting,s.writeUpload(waiting,new ByteArrayInputStream(new byte[]{1,2,3,4,5})));s.abortUpload(waiting);
            s.recall(a,room,1);s.logout(b);
            var snapshot=s.adminSnapshot(root);assertEquals(3,stat(snapshot,"Tổng tài khoản"));assertEquals(2,stat(snapshot,"Đang online"));assertEquals(1,stat(snapshot,"Ngoại tuyến"));assertEquals(3,stat(snapshot,"Tổng tin nhắn"));assertEquals(1,stat(snapshot,"Tổng tệp"));assertEquals(3,stat(snapshot,"Dung lượng tệp"));
            var row=snapshot.data().lay("ROOMS").stream().filter(r->r.get(0).equals(room)).findFirst().orElseThrow();assertEquals("2",row.get(6));assertEquals("1",row.get(14));
            String html=s.exportHtml(a,room);assertFalse(html.contains("bí mật sẽ thu hồi"));assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));assertTrue(html.contains("Tin đã thu hồi"));
            udp.send(a,room,TinNhan.taoMa(),"Thêm sau snapshot").get(5,TimeUnit.SECONDS);assertEquals(3,stat(s.adminReport(root,snapshot.id()),"Tổng tin nhắn"));
            byte[] pdf=WebReports.pdf(snapshot);assertTrue(pdf.length>10000);
            try(var doc=Loader.loadPDF(pdf)){String text=new PDFTextStripper().getText(doc);assertTrue(text.contains("Số người dùng theo từng phòng"));assertTrue(text.contains("Phòng tiếng Việt"));assertFalse(text.contains("Thêm sau snapshot"));for(var page:doc.getPages()){assertEquals(595.276,page.getMediaBox().getWidth(),.1);for(var name:page.getResources().getFontNames())assertTrue(page.getResources().getFont(name).isEmbedded());}}
            for(var pair:Map.of("overview","BÁO CÁO TỔNG QUAN","users","BÁO CÁO NGƯỜI DÙNG","rooms","BÁO CÁO PHÒNG CHAT","audit","BÁO CÁO NHẬT KÝ").entrySet()) {
                try(var doc=Loader.loadPDF(WebReports.pdf(snapshot,pair.getKey()))){assertTrue(new PDFTextStripper().getText(doc).contains(pair.getValue()));}
            }
            Map<String,String> files=new HashMap<>();try(var zip=new ZipInputStream(new ByteArrayInputStream(WebReports.zip(snapshot.data())),java.nio.charset.StandardCharsets.UTF_8)){ZipEntry e;while((e=zip.getNextEntry())!=null)files.put(e.getName(),new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
            assertEquals(Set.of("nguoidung.csv","phong.csv","thanhvien_theophong.csv","tin_theo_ngay.csv","tong_hop.html"),files.keySet());
            assertTrue(files.get("nguoidung.csv").startsWith("\ufeff"));assertTrue(files.get("nguoidung.csv").contains("'=SUM(1,1)"));assertTrue(files.get("tong_hop.html").contains("&lt;script&gt;"));
            assertFalse(files.get("nguoidung.csv").contains(PASSWORD));assertFalse(files.get("nguoidung.csv").contains("pbkdf2$"));
        }
    }
    @Test void broadcastUsesUdpAndRoomClearKeepsRecallDigest(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD);var udp=new UdpChatTransport(s)){
            String root=admin(s),member=account(s,"announce"),room=room(s,member,"Phòng đóng");s.adminAction(root,"room-state",Map.of("id",room,"open","false"));
            String id=TinNhan.taoMa();var plan=s.broadcastPlan(root,id,"Thông báo kiểm thử");assertEquals(2,plan.rooms().size());
            assertThrows(WebAdministration.Forbidden.class,()->s.broadcastPlan(member,id,"Giả mạo"));
            assertThrows(ExecutionException.class,()->udp.send(member,"sanh",id,"Giả mạo","Y",0).get(5,TimeUnit.SECONDS));
            udp.pauseReceiver();assertThrows(ExecutionException.class,()->udp.send(root,room,id,plan.text(),"Y",0).get(5,TimeUnit.SECONDS));assertTrue(s.history(member,room,0).isEmpty());
            udp.resumeReceiver();var saved=udp.send(root,room,id,plan.text(),"Y",0).get(5,TimeUnit.SECONDS);assertEquals("Y",saved.message().loai);assertTrue(udp.send(root,room,id,plan.text(),"Y",0).get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(IllegalArgumentException.class,()->s.broadcastPlan(root,id,"Khác nội dung"));
            s.adminAction(root,"room-clear",Map.of("id",room));assertTrue(s.messages(member,room,0,true).getFirst().recalled());
            assertTrue(udp.send(root,room,id,plan.text(),"Y",0).get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(IllegalArgumentException.class,()->s.adminAction(root,"room-delete",Map.of("id","sanh")));
            s.adminAction(root,"room-delete",Map.of("id",room));assertThrows(IllegalArgumentException.class,()->s.history(member,room,0));
        }
    }
    @Test void paginatedVietnamesePdfForManyRooms(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD)){
            var source=s.adminSnapshot(admin(s));var tables=new LinkedHashMap<>(source.data().bang());var rows=new ArrayList<List<String>>();
            for(int i=0;i<180;i++)rows.add(List.of("room"+i,"CODE"+i,"Phòng học tiếng Việt số "+i,"Công khai","Mở","admin","12","34","2","12345","2026-11-25","","admin","30","5"));
            tables.put("ROOMS",rows);var snap=new WebAdministration.Snapshot(UUID.randomUUID().toString(),new AnhChupQuanTri(source.data().luc(),tables));
            byte[] bytes=WebReports.pdf(snap);Files.write(dir.resolve("many-rooms.pdf"),bytes);
            try(var doc=Loader.loadPDF(bytes)){assertTrue(doc.getNumberOfPages()>5);String text=new PDFTextStripper().getText(doc);assertTrue(text.contains("Phòng học tiếng Việt số 179"));assertTrue(text.contains("(tiếp)"));assertTrue(text.contains("Trang 1/"));}
        }
    }
    @Test void assignedOwnerIsOnlineAndTransferRespectsCapacityBanLock(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=admin(s),bob=account(s,"ownerbob"),carol=account(s,"ownercarol"),bid=s.identity(bob).id(),cid=s.identity(carol).id();
            var created=(Map<?,?>)s.adminAction(root,"room-create",Map.of("name","Chủ phòng online","description","Fixture","publicRoom","true","password","","capacity","2","ownerId",bid));String room=created.get("roomId").toString();
            assertTrue(s.canRead(bob,room));assertTrue(s.allowed(bob,room));assertTrue(s.canRead(root,room));
            assertThrows(IllegalArgumentException.class,()->s.adminAction(root,"room-transfer",Map.of("id",room,"userId",cid)));
            s.adminAction(root,"member-moderate",Map.of("roomId",room,"userId",bid,"kind","BAN","seconds","60","reason","Fixture ban"));
            assertFalse(s.canRead(bob,room));assertThrows(IllegalArgumentException.class,()->s.adminAction(root,"room-transfer",Map.of("id",room,"userId",bid)));
            s.adminAction(root,"user-lock",Map.of("id",cid,"locked","true","reason","Fixture lock"));assertThrows(IllegalArgumentException.class,()->s.adminAction(root,"room-transfer",Map.of("id",room,"userId",cid)));
        }
    }

}
