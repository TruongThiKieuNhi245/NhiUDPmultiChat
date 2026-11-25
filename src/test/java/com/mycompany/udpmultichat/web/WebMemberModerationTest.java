package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WebMemberModerationTest {
    static final String PASSWORD="Moderation-fixture-private-password";
    String account(WebChatService s,String name)throws IOException{s.register(name,PASSWORD);return s.login(name,PASSWORD).token();}
    String room(WebChatService s,String owner)throws IOException{return ((Map<?,?>)s.action(owner,"room-create",Map.of("name","Phòng kiểm duyệt","description","Fixture","publicRoom","true","capacity","20","password",""))).get("roomId").toString();}
    Map<String,String> rule(String room,String user,String kind,String seconds){return Map.of("roomId",room,"userId",user,"kind",kind,"seconds",seconds,"reason","Lý do tiếng Việt <script>");}
    void join(WebChatService s,String token,String room)throws IOException{s.action(token,"room-join",Map.of("key",room,"password",""));}
    WebChatService.MemberNotice notice(List<WebChatService.Event> events){return (WebChatService.MemberNotice)events.stream().filter(e->e.type().equals("member-notice")).reduce((a,b)->b).orElseThrow().data();}
    @Test void targetedEventsSurviveLossOfRoomAccessAndKeepUdpHonest(@TempDir Path dir)throws Exception{
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)){
            String a=account(s,"owner"),b=account(s,"member"),id=s.identity(b).id(),r=room(s,a);join(s,b,r);
            List<WebChatService.Event> events=new ArrayList<>();s.onEvent(events::add);
            var result=(Map<?,?>)s.action(a,"member-moderate",rule(r,id,"MUTE","60"));
            var n=notice(events);assertSame(n,result.get("notice"));assertEquals(id,events.getFirst().recipientId());assertTrue(n.until()>System.currentTimeMillis());assertEquals("MUTE",n.kind());assertTrue(s.canRead(b,r));assertFalse(s.allowed(b,r));
            assertEquals(n.until(),s.members(a,r).stream().filter(m->m.id().equals(id)).findFirst().orElseThrow().mutedUntil());
            assertThrows(ExecutionException.class,()->udp.send(b,r,TinNhan.taoMa(),"Cố gửi khi cấm").get(5,TimeUnit.SECONDS));assertTrue(s.history(a,r,0).isEmpty());
            s.action(a,"member-moderate",rule(r,id,"MUTE","-1"));assertEquals("UNMUTE",notice(events).kind());assertTrue(s.allowed(b,r));
            s.action(a,"member-moderate",rule(r,id,"BAN","60"));assertEquals("BAN",notice(events).kind());assertFalse(s.canRead(b,r));assertEquals(id,events.stream().filter(e->e.type().equals("member-notice")).reduce((x,y)->y).orElseThrow().recipientId());
            assertThrows(IllegalArgumentException.class,()->join(s,b,r));assertEquals(1,s.restrictions(a,r).size());
            s.action(a,"member-moderate",rule(r,id,"BAN","-1"));assertEquals("UNBAN",notice(events).kind());assertFalse(s.canRead(b,r));join(s,b,r);
            s.action(a,"member-moderate",rule(r,id,"KICK","0"));assertEquals("KICK",notice(events).kind());assertFalse(s.canRead(b,r));join(s,b,r);assertTrue(s.canRead(b,r));
            assertTrue(events.stream().filter(e->e.type().equals("workspace") || e.type().equals("room-activity")).allMatch(e->e.recipientId().isEmpty()));
        }
    }
    @Test void permissionsInvalidAndDiskFailureProduceNoSuccessNotice(@TempDir Path dir)throws Exception{
        try(var s=new WebChatService(dir)){
            String a=account(s,"owner"),b=account(s,"member"),c=account(s,"outsider"),r=room(s,a),id=s.identity(b).id();join(s,b,r);
            List<WebChatService.Event> events=new ArrayList<>();s.onEvent(events::add);
            assertThrows(IllegalArgumentException.class,()->s.action(b,"member-moderate",rule(r,s.identity(a).id(),"MUTE","60")));
            assertThrows(IllegalArgumentException.class,()->s.action(c,"member-moderate",rule(r,id,"KICK","0")));
            assertThrows(IllegalArgumentException.class,()->s.restrictions(b,r));assertThrows(WebAdministration.Forbidden.class,()->s.restrictions(b,"*"));
            assertThrows(IllegalArgumentException.class,()->s.action(a,"member-moderate",rule(r,id,"MUTE","0")));
            assertThrows(IllegalArgumentException.class,()->s.action(a,"member-moderate",rule(r,id,"MUTE","31536001")));
            var tooLong=new HashMap<>(rule(r,id,"KICK","0"));tooLong.put("reason","x".repeat(201));assertThrows(IllegalArgumentException.class,()->s.action(a,"member-moderate",tooLong));
            Files.createDirectory(dir.resolve("moderation.properties.tmp"));assertThrows(IOException.class,()->s.action(a,"member-moderate",rule(r,id,"MUTE","60")));
            assertTrue(s.allowed(b,r));assertTrue(s.restrictions(a,r).isEmpty());assertTrue(events.isEmpty());
        }
    }
    @Test void persistedExpiryReasonAndGlobalMuteStayDistinct(@TempDir Path dir)throws Exception{
        String r,id;
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=s.login("admin",PASSWORD).token(),a=account(s,"owner"),b=account(s,"member");id=s.identity(b).id();r=room(s,a);join(s,b,r);
            s.adminAction(root,"member-moderate",rule("*",id,"MUTE","60"));s.action(a,"member-moderate",rule(r,id,"MUTE","1"));
            s.action(a,"member-moderate",rule(r,id,"MUTE","-1"));assertFalse(s.allowed(b,r));assertEquals(0,s.restrictions(a,r).size());assertEquals(1,s.restrictions(root,"*").size());
        }
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=s.login("admin",PASSWORD).token(),a=s.login("owner",PASSWORD).token(),b=s.login("member",PASSWORD).token();assertFalse(s.allowed(b,r));
            var member=s.members(a,r).stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();assertTrue(member.muteReason().contains("tiếng Việt"));
            s.adminAction(root,"member-moderate",rule("*",id,"MUTE","-1"));s.action(a,"member-moderate",rule(r,id,"MUTE","1"));Thread.sleep(1100);assertTrue(s.allowed(b,r));assertTrue(s.restrictions(a,r).isEmpty());
        }
    }
    @Test void administratorReplacingRoomOwnerGetsImmediateAccess(@TempDir Path dir)throws Exception{
        try(var s=new WebChatService(dir,PASSWORD)){
            String root=s.login("admin",PASSWORD).token(),a=account(s,"owner"),r=room(s,a);assertFalse(s.canRead(root,r));
            s.adminAction(root,"member-moderate",rule(r,s.identity(a).id(),"KICK","0"));assertTrue(s.canRead(root,r));assertTrue(s.allowed(root,r));assertFalse(s.canRead(a,r));
            assertEquals(s.identity(root).id(),s.members(root,r).stream().filter(m->m.id().equals(s.identity(root).id())).findFirst().orElseThrow().id());
        }
    }
}
