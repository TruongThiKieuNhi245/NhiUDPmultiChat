package com.mycompany.udpmultichat.web;

import com.fasterxml.jackson.databind.*;
import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Web2ServiceTest {
    final ObjectMapper json=new ObjectMapper();
    static Map<String,String> args(String... values) {
        var map=new HashMap<String,String>();for(int i=0;i<values.length;i+=2)map.put(values[i],values[i+1]);return map;
    }
    String account(WebChatService s,String login) throws IOException { s.register(login,"sample-password");return s.login(login,"sample-password").token(); }
    String create(WebChatService s,String token,String name,boolean pub,int capacity) throws IOException {
        return json.valueToTree(s.action(token,"room-create",args("name",name,"description","Phòng fixture","publicRoom",String.valueOf(pub),"password","","capacity",String.valueOf(capacity)))).path("roomId").asText();
    }
    String user(WebChatService s,String token) { return s.identity(token).id(); }
    JsonNode snapshot(WebChatService s,String token) { return json.valueToTree(s.snapshot(token)); }
    JsonNode room(WebChatService s,String token,String room) {
        for(var r:snapshot(s,token).path("rooms"))if(r.path("id").asText().equals(room))return r;
        return json.missingNode();
    }
    void join(WebChatService s,String token,String key) throws IOException { s.action(token,"room-join",args("key",key,"password","")); }
    String invite(WebChatService s,String owner,String room,String user) throws IOException {
        return json.valueToTree(s.action(owner,"invite-send",args("roomId",room,"userId",user))).path("id").asText();
    }
    @Test void roomsPermissionsCountsModerationAndNickname(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"alice"),b=account(s,"bob"),c=account(s,"carol");
            String one=create(s,a,"Phòng một",true,3),two=create(s,a,"Phòng riêng",false,3);
            assertTrue(room(s,c,two).isMissingNode());
            String secret=room(s,a,two).path("code").asText();assertFalse(snapshot(s,c).toString().contains(secret));
            join(s,b,one);join(s,c,secret);
            var accepted=udp.send(a,one,TinNhan.taoMa(),"Chỉ phòng một").get(5,TimeUnit.SECONDS);
            assertEquals(1,s.history(b,one,0).size());assertTrue(s.history(c,two,0).isEmpty());
            assertThrows(IllegalArgumentException.class,()->s.history(c,one,0));
            assertThrows(ExecutionException.class,()->udp.send(c,one,TinNhan.taoMa(),"Giả phòng").get(5,TimeUnit.SECONDS));
            assertThrows(IllegalArgumentException.class,()->s.action(b,"room-delete",args("roomId",one)));
            assertThrows(IllegalArgumentException.class,()->s.action(a,"room-delete",args("roomId","sanh")));
            s.action(a,"member-nickname",args("roomId",one,"userId",user(s,b),"nickname","Bạn cùng nhóm"));
            var msg=udp.send(b,one,TinNhan.taoMa(),"Biệt danh đúng phòng").get(5,TimeUnit.SECONDS).message();
            assertEquals("Bạn cùng nhóm",s.message(msg).nickname());assertEquals("bob",s.message(msg).name());
            assertEquals("",s.message(accepted.message()).nickname());
            assertEquals(2,room(s,a,one).path("memberCount").asInt());assertEquals(2,room(s,a,one).path("onlineCount").asInt());
            s.action(a,"member-moderate",args("roomId",one,"userId",user(s,b),"kind","MUTE","seconds","60"));
            assertTrue(s.canRead(b,one));assertFalse(s.allowed(b,one));assertTrue(s.active(b));
            assertThrows(ExecutionException.class,()->udp.send(b,one,TinNhan.taoMa(),"Bị mute").get(5,TimeUnit.SECONDS));
            s.action(a,"member-moderate",args("roomId",one,"userId",user(s,b),"kind","MUTE","seconds","-1"));
            s.action(a,"room-state",args("roomId",one,"open","false"));assertTrue(s.canRead(b,one));assertFalse(s.allowed(b,one));
            s.action(a,"room-state",args("roomId",one,"open","true"));
            String bid=user(s,b);
            s.action(a,"member-moderate",args("roomId",one,"userId",bid,"kind","BAN","seconds","60"));
            assertFalse(s.canRead(b,one));assertThrows(IllegalArgumentException.class,()->join(s,b,one));
            s.action(a,"member-moderate",args("roomId",one,"userId",bid,"kind","BAN","seconds","-1"));join(s,b,one);
            s.action(a,"room-transfer",args("roomId",one,"userId",bid));
            assertFalse(room(s,a,one).path("manage").asBoolean());assertTrue(room(s,b,one).path("manage").asBoolean());
            s.action(a,"room-leave",args("roomId",one));assertFalse(s.canRead(a,one));
            s.logout(b);assertEquals(1,room(s,c,one).path("memberCount").asInt());assertEquals(0,room(s,c,one).path("onlineCount").asInt());
        }
    }
    @Test void offlineInvitationsRecheckCapacityClosedBanAndPersist(@TempDir Path dir) throws Exception {
        String room,bid,cid,inviteB,inviteC;
        try(var s=new WebChatService(dir)) {
            String a=account(s,"invitea"),b=account(s,"inviteb"),c=account(s,"invitec");bid=user(s,b);cid=user(s,c);
            room=create(s,a,"Phòng lời mời",false,2);s.logout(b);s.logout(c);
            inviteB=invite(s,a,room,bid);inviteC=invite(s,a,room,cid);
            assertEquals(1,room(s,a,room).path("memberCount").asInt());
        }
        try(var s=new WebChatService(dir)) {
            String a=s.login("invitea","sample-password").token(),b=s.login("inviteb","sample-password").token(),c=s.login("invitec","sample-password").token();
            assertEquals(1,snapshot(s,b).path("invitations").size());
            s.action(a,"room-state",args("roomId",room,"open","false"));
            assertThrows(IllegalArgumentException.class,()->s.action(b,"invite-answer",args("inviteId",inviteB,"accept","true")));
            assertEquals(1,snapshot(s,b).path("invitations").size());
            s.action(a,"room-state",args("roomId",room,"open","true"));
            s.action(a,"member-moderate",args("roomId",room,"userId",bid,"kind","BAN","seconds","60"));
            assertThrows(IllegalArgumentException.class,()->s.action(b,"invite-answer",args("inviteId",inviteB,"accept","true")));
            s.action(a,"member-moderate",args("roomId",room,"userId",bid,"kind","BAN","seconds","-1"));
            var executor=Executors.newFixedThreadPool(2);
            try {
                List<Callable<Boolean>> calls=List.of(()->{try{s.action(b,"invite-answer",args("inviteId",inviteB,"accept","true"));return true;}catch(IllegalArgumentException e){return false;}},
                        ()->{try{s.action(c,"invite-answer",args("inviteId",inviteC,"accept","true"));return true;}catch(IllegalArgumentException e){return false;}});
                int winners=0;for(var future:executor.invokeAll(calls))if(future.get())winners++;
                assertEquals(1,winners);assertEquals(2,room(s,a,room).path("memberCount").asInt());
            }finally{executor.shutdownNow();}
            String loser=s.canRead(b,room)?c:b;
            String invitation=s.canRead(b,room)?inviteC:inviteB;
            assertEquals(1,snapshot(s,loser).path("invitations").size());
            s.action(loser,"invite-answer",args("inviteId",invitation,"accept","false"));assertEquals(0,snapshot(s,loser).path("invitations").size());
        }
    }
    @Test void stickerReplyRecallRetryAndRestartDoNotLeakQuote(@TempDir Path dir) throws Exception {
        String token,room,client=TinNhan.taoMa();
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            token=account(s,"chatuser");room=create(s,token,"Phòng reply",true,10);
            var original=udp.send(token,room,client,"Bí mật chỉ trong tin gốc").get(5,TimeUnit.SECONDS).message();
            var reply=udp.send(token,room,TinNhan.taoMa(),"Đồng ý","T",original.msgId).get(5,TimeUnit.SECONDS).message();
            assertEquals(original.msgId,s.message(reply).replyTo());assertTrue(s.message(reply).replyText().contains("Bí mật"));
            var sticker=udp.send(token,room,TinNhan.taoMa(),"1f600","S",0).get(5,TimeUnit.SECONDS).message();assertEquals("S",s.message(sticker).kind());
            assertThrows(ExecutionException.class,()->udp.send(token,room,TinNhan.taoMa(),"unknown","S",0).get(5,TimeUnit.SECONDS));
            s.recall(token,room,original.msgId);
            assertEquals("Tin đã thu hồi",s.message(reply).replyText());assertTrue(s.search(token,room,"Bí mật",0).isEmpty());
            assertFalse(s.export(token,room).contains("Bí mật"));
            assertTrue(udp.send(token,room,client,"Bí mật chỉ trong tin gốc").get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(ExecutionException.class,()->udp.send(token,room,client,"Thay nội dung sau thu hồi").get(5,TimeUnit.SECONDS));
            try(var files=Files.walk(dir)){assertFalse(Files.readString(files.filter(p->p.getFileName().toString().equals(room+".log")).findFirst().orElseThrow()).contains("Bí mật"));}
        }
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String again=s.login("chatuser","sample-password").token();
            assertTrue(s.canRead(again,room));assertEquals("Tin đã thu hồi",s.messages(again,room,0,false).get(1).replyText());
            assertTrue(udp.send(again,room,client,"Bí mật chỉ trong tin gốc").get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(ExecutionException.class,()->udp.send(again,room,client,"Thay sau restart").get(5,TimeUnit.SECONDS));
        }
    }
    @Test void profileAvatarPasswordAndForcedPassword(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir)) {
            String a=account(s,"profilea");String uid=user(s,a);
            var image=new java.awt.image.BufferedImage(96,96,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
            s.action(a,"profile-save",args("name","Tên hiển thị mới","about","Giới thiệu","avatarMode","SET","avatar",Base64.getEncoder().encodeToString(out.toByteArray())));
            assertArrayEquals(out.toByteArray(),s.avatar(a,uid));assertEquals("Tên hiển thị mới",s.identity(a).name());
            assertThrows(IllegalArgumentException.class,()->s.action(a,"password-change",args("current","wrong-password","next","new-password")));
            s.action(a,"password-change",args("current","sample-password","next","new-password"));
            s.action(a,"profile-save",args("name","Tên hiển thị mới","about","","avatarMode","DELETE"));assertEquals(0,s.avatar(a,uid).length);
            s.logout(a);assertThrows(IllegalArgumentException.class,()->s.login("profilea","sample-password"));s.login("profilea","new-password");
        }
        Path forced=dir.resolve("forced");Files.createDirectories(forced);Files.writeString(forced.resolve("config.properties"),"admin.password=admin123\n");
        try(var s=new WebChatService(forced)) {
            String token=s.login("admin","admin123").token();assertTrue(snapshot(s,token).path("profile").path("forcePassword").asBoolean());
            assertThrows(IllegalArgumentException.class,()->s.history(token,"sanh",0));
            s.action(token,"password-change",args("current","admin123","next","private-new-password"));
            assertFalse(snapshot(s,token).path("profile").path("forcePassword").asBoolean());assertTrue(s.canRead(token,"sanh"));
        }
    }
    @Test void nicknameAndInvitationDiskFailuresDoNotChangeMemory(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir)) {
            String a=account(s,"diska"),b=account(s,"diskb");String room=create(s,a,"Phòng lỗi đĩa",true,10);join(s,b,room);
            s.action(a,"member-nickname",args("roomId",room,"userId",user(s,b),"nickname","Tên cũ"));
            Files.createDirectory(dir.resolve("nicknames.properties.tmp"));
            assertThrows(IOException.class,()->s.action(a,"member-nickname",args("roomId",room,"userId",user(s,b),"nickname","Tên mới")));
            assertEquals("Tên cũ",s.members(a,room).stream().filter(m->m.id().equals(user(s,b))).findFirst().orElseThrow().nickname());
            String c=account(s,"diskc");Files.createDirectory(dir.resolve("invites.properties.tmp"));
            assertThrows(IOException.class,()->invite(s,a,room,user(s,c)));
            assertEquals(0,snapshot(s,c).path("invitations").size());
        }
    }
    @Test void searchPaginationAndLatestHistoryStayBounded(@TempDir Path dir) throws Exception {
        var disk=new KhoLuuTru(dir,text -> { });
        for(int i=1;i<=123;i++)disk.ghiTin(new TinNhan(i,"sanh","fixture","Tác giả","needle "+i,i,TinNhan.taoMa(),"T"));
        try(var service=new WebChatService(dir)) {
            String token=account(service,"searcher");
            var first=service.search(token,"sanh","needle",0);assertEquals(50,first.size());assertEquals(1,first.getFirst().msgId());
            var second=service.search(token,"sanh","needle",50);assertEquals(50,second.size());assertEquals(51,second.getFirst().msgId());
            var last=service.search(token,"sanh","needle",100);assertEquals(23,last.size());
            assertEquals(74,service.messages(token,"sanh",0,true).getFirst().msgId());
            assertEquals(50,service.around(token,"sanh",60).size());
            assertThrows(IllegalArgumentException.class,()->service.search(token,"sanh","",0));
        }
    }
    @Test void invitationsExpireAndFormerOwnerCannotGrantAccess(@TempDir Path dir) throws Exception {
        String room,expired;
        try(var service=new WebChatService(dir)) {
            String a=account(service,"ownera"),b=account(service,"invitedb"),c=account(service,"ownerc");
            room=create(service,a,"Phòng hết hạn",true,5);join(service,c,room);
            String invitation=invite(service,a,room,user(service,b));
            service.action(a,"room-transfer",args("roomId",room,"userId",user(service,c)));
            assertThrows(IllegalArgumentException.class,()->service.action(b,"invite-answer",args("inviteId",invitation,"accept","true")));
            service.action(b,"invite-answer",args("inviteId",invitation,"accept","false"));
            String other=invite(service,c,room,user(service,b));
            assertThrows(IllegalArgumentException.class,()->service.action(a,"invite-cancel",args("inviteId",other)));
            service.action(c,"invite-cancel",args("inviteId",other));
            expired=invite(service,c,room,user(service,b));
        }
        var disk=new KhoLuuTru(dir,text -> { });var records=disk.docLoiMoi();String[] values=records.getProperty(expired).split("\\|");
        records.setProperty(expired,values[0]+"|"+values[1]+"|"+values[2]+"|1");disk.ghiLoiMoi(records);
        try(var service=new WebChatService(dir)) {
            String b=service.login("invitedb","sample-password").token();assertEquals(0,snapshot(service,b).path("invitations").size());
            assertThrows(IllegalArgumentException.class,()->service.action(b,"invite-answer",args("inviteId",expired,"accept","true")));
            assertFalse(service.canRead(b,room));
        }
    }

}
