package com.mycompany.udpmultichat.web;

import com.mycompany.udpmultichat.server.*;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.transport.UdpChatTransport;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WebDataToolTest {
    static final String PASSWORD="Backup-fixture-private-password";
    Map<String,byte[]> bytes(Path dir)throws IOException{
        Map<String,byte[]> result=new TreeMap<>();try(var paths=Files.walk(dir)){for(var p:paths.filter(Files::isRegularFile).toList())if(!p.getFileName().toString().equals("web.lock"))result.put(dir.relativize(p).toString(),Files.readAllBytes(p));}return result;
    }
    void same(Map<String,byte[]> expected,Path dir)throws IOException{var actual=bytes(dir);assertEquals(expected.keySet(),actual.keySet());for(var entry:expected.entrySet())assertArrayEquals(entry.getValue(),actual.get(entry.getKey()),entry.getKey());}
    @Test void backupRestoreImportKeepAllBytesAndChatFileState(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("source"),archive=dir.resolve("backup.zip");String room,user;
        byte[] data=new byte[5*1024*1024];new Random(42).nextBytes(data);
        try(var s=new WebChatService(source,PASSWORD);var udp=new UdpChatTransport(s)){
            String root=s.login("admin",PASSWORD).token();s.register("member",PASSWORD);String member=s.login("member",PASSWORD).token();user=s.identity(member).id();
            room=((Map<?,?>)s.action(root,"room-create",Map.of("name","Phòng sao lưu","description","Fixture","publicRoom","true","password","","capacity","20"))).get("roomId").toString();s.action(member,"room-join",Map.of("key",room,"password",""));
            udp.send(member,room,TinNhan.taoMa(),"Tin tiếng Việt qua UDP").get(5,TimeUnit.SECONDS);
            var ticket=s.beginUpload(member,room,"tệp 5 MB.bin",data.length);var file=s.finishUpload(member,ticket,s.writeUpload(ticket,new ByteArrayInputStream(data)));s.abortUpload(ticket);udp.send(member,room,TinNhan.taoMa(),file.text(),"F",0).get(5,TimeUnit.SECONDS);
            s.adminAction(root,"member-moderate",Map.of("roomId",room,"userId",user,"kind","MUTE","seconds","3600","reason","Giữ sau restore"));
            assertThrows(IOException.class,()->WebDataTool.backup(source,archive));assertFalse(Files.exists(archive));
        }
        var original=bytes(source);var summary=WebDataTool.backup(source,archive);assertEquals(2,summary.accounts());assertEquals(2,summary.rooms());assertEquals(2,summary.messages());assertEquals(1,summary.files());assertEquals(data.length,summary.fileBytes());same(original,source);
        assertEquals(summary,WebDataTool.verify(archive));Path restored=dir.resolve("restored");assertEquals(summary,WebDataTool.restore(archive,restored));same(original,restored);same(original,source);
        try(var s=new WebChatService(restored)){
            String member=s.login("member",PASSWORD).token();assertTrue(s.canRead(member,room));assertFalse(s.allowed(member,room));assertEquals(2,s.history(member,room,0).size());
            var file=s.messages(member,room,0,false).getLast().file();try(var download=s.download(member,file.id(),false)){assertArrayEquals(data,download.stream().readAllBytes());}
        }
        Path imported=dir.resolve("imported");assertEquals(summary,WebDataTool.importCopy(source,imported));same(original,imported);same(original,source);
    }
    @Test void restoreNeverOverwritesAndBackupNeverReplaces(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("data");try(var service=new WebChatService(source,PASSWORD)){assertNotNull(service);}
        Path archive=dir.resolve("one.zip");WebDataTool.backup(source,archive);byte[] saved=Files.readAllBytes(archive);
        assertThrows(FileAlreadyExistsException.class,()->WebDataTool.backup(source,archive));assertArrayEquals(saved,Files.readAllBytes(archive));
        Path existing=dir.resolve("existing");Files.createDirectory(existing);Files.writeString(existing.resolve("important.txt"),"Giữ nguyên");assertThrows(FileAlreadyExistsException.class,()->WebDataTool.restore(archive,existing));assertEquals("Giữ nguyên",Files.readString(existing.resolve("important.txt")));
        assertThrows(IOException.class,()->WebDataTool.backup(source,source.resolve("inside.zip")));
    }
    @Test void corruptOrTraversalArchivesLeaveNoPublishedDirectory(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("data");try(var service=new WebChatService(source,PASSWORD)){assertNotNull(service);}
        Path good=dir.resolve("good.zip");WebDataTool.backup(source,good);
        Path bad=dir.resolve("tampered.zip");try(var zip=new ZipFile(good.toFile());var out=new ZipOutputStream(Files.newOutputStream(bad))){var entries=zip.entries();while(entries.hasMoreElements()){var e=entries.nextElement();out.putNextEntry(new ZipEntry(e.getName()));byte[] data=zip.getInputStream(e).readAllBytes();if(e.getName().equals("data/users.csv"))data[0]^=1;out.write(data);out.closeEntry();}}
        Path target=dir.resolve("must-not-exist");assertThrows(IOException.class,()->WebDataTool.restore(bad,target));assertFalse(Files.exists(target));
        Path traversal=dir.resolve("traversal.zip");try(var out=new ZipOutputStream(Files.newOutputStream(traversal))){out.putNextEntry(new ZipEntry("manifest.properties"));out.write("format=UDPmultiChat-backup-1\nfile.../escape=1:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();out.putNextEntry(new ZipEntry("data/../escape"));out.write(1);out.closeEntry();}
        assertThrows(IOException.class,()->WebDataTool.restore(traversal,target));assertFalse(Files.exists(target));assertFalse(Files.exists(dir.resolve("escape")));
        try(var paths=Files.list(dir)){assertTrue(paths.noneMatch(p->p.getFileName().toString().startsWith(".udp-")));}
    }
    @Test void invalidCopyReportsItemAndPreservesOriginal(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("data");try(var service=new WebChatService(source,PASSWORD)){assertNotNull(service);}
        Files.writeString(source.resolve("users.csv"),"header\nbroken row\n");var original=bytes(source);Path archive=dir.resolve("no.zip");assertThrows(IOException.class,()->WebDataTool.backup(source,archive));assertFalse(Files.exists(archive));same(original,source);
        Files.writeString(source.resolve("unknown-private.txt"),"Không lấy tệp lạ");assertThrows(IOException.class,()->WebDataTool.backup(source,archive));assertFalse(Files.exists(archive));
    }
    @Test void legacyDefaultPasswordStillForcesChangeAfterImport(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("desktop-copy");var disk=new KhoLuuTru(source,ignored->{});new QuanLyNguoiDung(disk);new QuanLyPhong(disk,List::of);
        var original=bytes(source);Path target=dir.resolve("web-new");WebDataTool.importCopy(source,target);same(original,source);
        try(var service=new WebChatService(target)){
            String token=service.login("admin","admin123").token();assertTrue(((WebWorkspace.Profile)service.snapshot(token).get("profile")).forcePassword());assertThrows(IllegalArgumentException.class,()->service.history(token,"sanh",0));
            service.action(token,"password-change",Map.of("current","admin123","next",PASSWORD));assertTrue(service.canRead(token,"sanh"));
        }
    }
    @Test void deletedAuthorFileIsRetainedButCorruptBinaryIsRejected(@TempDir Path dir)throws Exception{
        Path source=dir.resolve("data");String fileId,room;
        try(var service=new WebChatService(source,PASSWORD);var udp=new UdpChatTransport(service)){
            String root=service.login("admin",PASSWORD).token();service.register("oldauthor",PASSWORD);String member=service.login("oldauthor",PASSWORD).token();
            room=((Map<?,?>)service.action(root,"room-create",Map.of("name","Phòng lưu tệp","description","Fixture","publicRoom","true","password","","capacity","20"))).get("roomId").toString();service.action(member,"room-join",Map.of("key",room,"password",""));
            var ticket=service.beginUpload(member,room,"giữ.bin",3);var file=service.finishUpload(member,ticket,service.writeUpload(ticket,new ByteArrayInputStream(new byte[]{1,2,3})));service.abortUpload(ticket);fileId=file.id();udp.send(member,room,TinNhan.taoMa(),file.text(),"F",0).get(5,TimeUnit.SECONDS);
            service.adminAction(root,"user-delete",Map.of("id",service.identity(member).id(),"confirm","oldauthor"));
        }
        var summary=WebDataTool.backup(source,dir.resolve("deleted-author.zip"));assertEquals(1,summary.accounts());assertEquals(1,summary.files());assertEquals(1,summary.messages());
        Path bin=source.resolve("uploads/"+room+"/"+fileId+".bin");Files.write(bin,new byte[]{9,2,3});var modified=bytes(source);Path archive=dir.resolve("bad-crc.zip");IOException error=assertThrows(IOException.class,()->WebDataTool.backup(source,archive));assertTrue(error.getMessage().contains("CRC"));assertFalse(Files.exists(archive));same(modified,source);
    }
}
