package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Web3FileServiceTest {
    String account(WebChatService s,String login) throws IOException { s.register(login,"file-fixture-password");return s.login(login,"file-fixture-password").token(); }
    WebFile upload(WebChatService s,String token,String room,String name,byte[] bytes) throws IOException {
        var ticket=s.beginUpload(token,room,name,bytes.length);
        try { return s.finishUpload(token,ticket,s.writeUpload(ticket,new ByteArrayInputStream(bytes))); }
        finally { s.abortUpload(ticket); }
    }
    @Test void fiveMegabytesRequiresUdpCommitAndSameRetryKey(@TempDir Path dir) throws Exception {
        byte[] bytes=new byte[5*1024*1024];new Random(35).nextBytes(bytes);var crc=new CRC32();crc.update(bytes);
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"filealice"),b=account(s,"filebob");
            var f=upload(s,a,"sanh","../../../bản chụp.bin",bytes);
            assertFalse(f.name().contains("/"));assertFalse(f.name().contains(".."));assertEquals(crc.getValue(),f.crc());
            assertTrue(s.history(a,"sanh",0).isEmpty());
            assertThrows(IllegalArgumentException.class,()->s.download(a,f.id(),false));
            String id=TinNhan.taoMa();int[] broadcasts={0};udp.onCommitted(t->broadcasts[0]++);
            udp.pauseReceiver();
            assertThrows(ExecutionException.class,()->udp.send(a,"sanh",id,f.text(),"F",0).get(5,TimeUnit.SECONDS));
            assertTrue(s.history(a,"sanh",0).isEmpty());assertEquals(0,broadcasts[0]);
            udp.resumeReceiver();var saved=udp.send(a,"sanh",id,f.text(),"F",0).get(5,TimeUnit.SECONDS);
            assertEquals("F",saved.message().loai);assertEquals(f.id(),s.message(saved.message()).file().id());
            try(var data=s.download(b,f.id(),false)){assertArrayEquals(bytes,data.stream().readAllBytes());assertEquals(crc.getValue(),data.crc());}
            assertTrue(udp.send(a,"sanh",id,f.text(),"F",0).get(5,TimeUnit.SECONDS).duplicate());
            assertEquals(1,s.history(a,"sanh",0).size());assertEquals(1,broadcasts[0]);
            assertThrows(ExecutionException.class,()->udp.send(a,"sanh",id,f.text()+"x","F",0).get(5,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->udp.send(a,"sanh",TinNhan.taoMa(),f.text(),"F",0).get(5,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->udp.send(b,"sanh",TinNhan.taoMa(),f.text(),"F",0).get(5,TimeUnit.SECONDS));
            assertThrows(IllegalArgumentException.class,()->s.discardUpload(a,f.id()));
            s.logout(b);assertThrows(IllegalArgumentException.class,()->s.download(b,f.id(),false));
        }
    }
    @Test void restartStagingRecallAndCleanupArePersistent(@TempDir Path dir) throws Exception {
        WebFile staged,committed;String id=TinNhan.taoMa();byte[] bytes="Tệp tiếng Việt qua restart".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"filerestart");staged=upload(s,a,"sanh","chờ gửi.txt",bytes);committed=upload(s,a,"sanh","đã gửi.txt",bytes);
            udp.send(a,"sanh",id,committed.text(),"F",0).get(5,TimeUnit.SECONDS);
        }
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=s.login("filerestart","file-fixture-password").token();
            assertTrue(udp.send(a,"sanh",id,committed.text(),"F",0).get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(IllegalArgumentException.class,()->s.download(a,staged.id(),false));
            udp.send(a,"sanh",TinNhan.taoMa(),staged.text(),"F",0).get(5,TimeUnit.SECONDS);
            try(var data=s.download(a,committed.id(),false)){assertArrayEquals(bytes,data.stream().readAllBytes());}
            s.recall(a,"sanh",1);
            assertNull(s.messages(a,"sanh",0,true).getFirst().file());assertFalse(s.export(a,"sanh").contains(committed.name()));
            assertThrows(IllegalArgumentException.class,()->s.download(a,committed.id(),false));
            assertFalse(Files.exists(dir.resolve("uploads/sanh/"+committed.id()+".bin")));
            assertTrue(udp.send(a,"sanh",id,committed.text(),"F",0).get(5,TimeUnit.SECONDS).duplicate());
            assertThrows(ExecutionException.class,()->udp.send(a,"sanh",id,staged.text(),"F",0).get(5,TimeUnit.SECONDS));
        }
    }
    @Test void limitsReservationInvalidImagesAndCancelledStreams(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir)) {
            String a=account(s,"filelimits");
            assertThrows(IllegalArgumentException.class,()->s.beginUpload(a,"sanh","zero.txt",0));
            assertThrows(IllegalArgumentException.class,()->s.beginUpload(a,"sanh","big.txt",WebFiles.MAX_BYTES+1));
            assertThrows(IllegalArgumentException.class,()->s.beginUpload(a,"sanh","active.html",1));
            assertThrows(IllegalArgumentException.class,()->upload(s,a,"sanh","bad.png",new byte[]{1,2,3}));
            var one=s.beginUpload(a,"sanh","one.bin",100);var two=s.beginUpload(a,"sanh","two.bin",100);
            assertThrows(IllegalArgumentException.class,()->s.beginUpload(a,"sanh","three.bin",100));
            assertThrows(IllegalArgumentException.class,()->s.writeUpload(one,new ByteArrayInputStream(new byte[99])));
            assertThrows(IllegalArgumentException.class,()->s.writeUpload(two,new ByteArrayInputStream(new byte[101])));
            s.abortUpload(one);s.abortUpload(two);
            assertFalse(Files.exists(one.path()));assertFalse(Files.exists(two.path()));
            byte[] max=new byte[(int)WebFiles.MAX_BYTES];
            for(int i=0;i<5;i++)upload(s,a,"sanh","quota"+i+".bin",max);
            assertThrows(IllegalArgumentException.class,()->upload(s,a,"sanh","full.bin",new byte[]{1}));
        }
    }
    @Test void diskFailureDoesNotConfirmAndRightsRecheckedAtFinish(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"filedisk");var f=upload(s,a,"sanh","không mất khi lỗi.txt",new byte[]{1,2,3});
            Path history=dir.resolve("rooms/sanh.log");
            if(!Files.exists(history.getParent()))throw new AssertionError("history directory");
            Path old=dir.resolve("rooms/sanh.fixture");boolean existed=Files.exists(history);
            if(existed)Files.move(history,old);Files.createDirectory(history);
            int[] delivered={0};udp.onCommitted(t->delivered[0]++);String id=TinNhan.taoMa();
            assertThrows(ExecutionException.class,()->udp.send(a,"sanh",id,f.text(),"F",0).get(5,TimeUnit.SECONDS));
            assertEquals(0,delivered[0]);assertTrue(s.history(a,"sanh",0).isEmpty());
            Files.delete(history);if(existed)Files.move(old,history);
            udp.send(a,"sanh",id,f.text(),"F",0).get(5,TimeUnit.SECONDS);assertEquals(1,delivered[0]);
            var ticket=s.beginUpload(a,"sanh","logout.txt",3);long crc=s.writeUpload(ticket,new ByteArrayInputStream(new byte[]{4,5,6}));
            s.logout(a);assertThrows(IllegalArgumentException.class,()->s.finishUpload(a,ticket,crc));s.abortUpload(ticket);
            assertFalse(Files.exists(ticket.path()));
        }
    }
    @Test void expiredStagingAndDeletedRoomRemoveBinary(@TempDir Path dir) throws Exception {
        WebFile expired,live;
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"filecleanup");
            expired=upload(s,a,"sanh","expired.txt",new byte[]{1});
            var response=(Map<?,?>)s.action(a,"room-create",Map.of("name","Phòng tệp","description","Fixture","publicRoom","true","password","","capacity","3"));
            String room=response.get("roomId").toString();live=upload(s,a,room,"remove.txt",new byte[]{2});
            udp.send(a,room,TinNhan.taoMa(),live.text(),"F",0).get(5,TimeUnit.SECONDS);
            s.action(a,"room-delete",Map.of("roomId",room));assertFalse(Files.exists(dir.resolve("uploads/"+room+"/"+live.id()+".bin")));
        }
        Path metadata=dir.resolve("uploads/sanh/"+expired.id()+".properties");var properties=new Properties();
        try(var in=Files.newBufferedReader(metadata)){properties.load(in);}properties.setProperty("time","0");
        try(var out=Files.newBufferedWriter(metadata)){properties.store(out,"fixture expiry");}
        try(var s=new WebChatService(dir)) { s.cleanupFiles();assertFalse(Files.exists(dir.resolve("uploads/sanh/"+expired.id()+".bin"))); }
    }
    @Test void cleanupFailureDoesNotMisreportCommittedRecall(@TempDir Path dir) throws Exception {
        try(var s=new WebChatService(dir);var udp=new UdpChatTransport(s)) {
            String a=account(s,"filedeferred");var f=upload(s,a,"sanh","cleanup.txt",new byte[]{1});
            udp.send(a,"sanh",TinNhan.taoMa(),f.text(),"F",0).get(5,TimeUnit.SECONDS);
            Path binary=dir.resolve("uploads/sanh/"+f.id()+".bin"),metadata=dir.resolve("uploads/sanh/"+f.id()+".properties");
            Files.delete(binary);Files.createDirectory(binary);Files.writeString(binary.resolve("lock.fixture"),"fixture");
            assertDoesNotThrow(()->s.recall(a,"sanh",1));
            assertTrue(s.messages(a,"sanh",0,true).getFirst().recalled());
            assertThrows(IllegalArgumentException.class,()->s.download(a,f.id(),false));
            assertTrue(Files.exists(metadata));
            Files.delete(binary.resolve("lock.fixture"));Files.delete(binary);s.cleanupFiles();assertFalse(Files.exists(metadata));
        }
    }

}
