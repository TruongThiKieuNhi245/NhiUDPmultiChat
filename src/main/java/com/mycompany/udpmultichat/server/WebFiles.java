package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.WebFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** HTTP chi luu tep; tin F chi duoc chap nhan sau UDP receive. */
public final class WebFiles {
    public static final long MAX_BYTES=10L*1024*1024;
    private static final long STAGED_TTL=24L*60*60*1000;
    private static final Set<String> TYPES=Set.of("txt","csv","json","pdf","zip","docx","xlsx","pptx","odt","ods","odp","rtf","png","jpg","jpeg","bin");
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(WebFiles.class);
    private final Set<String> pendingRemoval=new LinkedHashSet<>(),pendingRooms=new LinkedHashSet<>();
    private final KhoTep files;
    private final Path temporary;
    private final Map<String,Ticket> active=new HashMap<>();
    public record Ticket(String id,String room,String user,String name,long size,Path path) { }
    public record Download(InputStream stream,String name,long size,long crc,boolean thumbnail) implements AutoCloseable {
        @Override public void close() throws IOException { stream.close(); }
    }
    WebFiles(Path directory) throws IOException {
        files=new KhoTep(directory);
        temporary=directory.toAbsolutePath().normalize().resolve("web-upload-tmp");
        Files.createDirectories(temporary);
        if(Files.isSymbolicLink(temporary))throw new IOException("Thư mục tạm không được là liên kết.");
        try(var paths=Files.list(temporary)) {
            for(var path:paths.toList())if(path.getFileName().toString().matches("upload-[a-zA-Z0-9-]+\\.part"))Files.deleteIfExists(path);
        }
    }
    synchronized Ticket begin(String room,String user,String name,long size) throws IOException {
        TepTin.kiemTra(size,MAX_BYTES);
        if(name==null || name.length()>255)throw new IllegalArgumentException("Tên tệp quá dài.");
        String safe=TepTin.tenAnToan(name),extension=extension(safe);
        if(!TYPES.contains(extension))throw new IllegalArgumentException("Loại tệp chưa hỗ trợ. Hãy chọn ảnh PNG/JPG, PDF, tài liệu Office, TXT/CSV/JSON hoặc ZIP.");
        if(active.size()>=8 || active.values().stream().filter(t->t.user().equals(user)).count()>=2)throw new IllegalArgumentException("Đang có nhiều tệp tải lên. Hãy chờ tệp hiện tại hoàn tất.");
        quota(null,null,512L*1024*1024,size);
        quota(room,null,100L*1024*1024,size);
        quota(null,user,50L*1024*1024,size);
        if(files.entries().size()+active.size()>=1024)throw new IllegalArgumentException("Kho đã đạt giới hạn số tệp.");
        String id=UUID.randomUUID().toString();
        var ticket=new Ticket(id,room,user,safe,size,Files.createTempFile(temporary,"upload-",".part"));
        active.put(id,ticket);return ticket;
    }
    private void quota(String room,String user,long limit,long size) {
        long used=files.thongKe(room,user)[1]+active.values().stream()
                .filter(t->room==null || t.room().equals(room)).filter(t->user==null || t.user().equals(user)).mapToLong(Ticket::size).sum();
        if(size>limit-used)throw new IllegalArgumentException("Kho tệp đã hết dung lượng cho tài khoản hoặc phòng. Hãy thu hồi tệp không cần dùng.");
    }
    // Stream ngoai khoa nghiep vu chat, khong nap toan bo tep vao RAM.
    public long write(Ticket ticket,InputStream source) throws IOException {
        CRC32 crc=new CRC32();long total=0;
        try(OutputStream out=Files.newOutputStream(ticket.path())) {
            byte[] buffer=new byte[32768];int n;
            while((n=source.read(buffer))!=-1) {
                total+=n;
                if(total>MAX_BYTES || total>ticket.size())throw new IllegalArgumentException("Tệp vượt 10 MB hoặc kích thước tải lên không khớp.");
                crc.update(buffer,0,n);out.write(buffer,0,n);
            }
        }
        if(total!=ticket.size())throw new IllegalArgumentException("Tệp tải lên chưa đầy đủ. Hãy thử lại.");
        return crc.getValue();
    }
    synchronized WebFile finish(Ticket ticket,long crc) throws IOException {
        if(active.get(ticket.id())!=ticket)throw new IllegalArgumentException("Lượt tải lên đã hết hạn.");
        var file=new TepTin(ticket.id(),ticket.room(),ticket.user(),ticket.name(),ticket.size(),crc,System.currentTimeMillis());
        boolean image=Set.of("png","jpg","jpeg").contains(extension(file.ten()));
        Path bin=files.duong(file.room(),file.id(),".bin");
        try {
            KhoTep.doiTen(ticket.path(),bin);
            if(image && AnhTep.tao(bin,files.duong(file.room(),file.id(),".jpg"))==null)throw new IllegalArgumentException("Ảnh bị hỏng hoặc quá nhiều điểm ảnh. Hãy chọn ảnh PNG/JPG khác.");
            files.luu(file);active.remove(ticket.id());return dto(file);
        }catch(IOException | RuntimeException e) {
            Files.deleteIfExists(bin);Files.deleteIfExists(files.duong(file.room(),file.id(),".jpg"));
            Files.deleteIfExists(files.duong(file.room(),file.id(),".part"));throw e;
        }
    }
    public synchronized void abort(Ticket ticket) throws IOException {
        active.remove(ticket.id(),ticket);Files.deleteIfExists(ticket.path());
    }
    void validate(String room,String user,String text,Set<String> live,boolean duplicate) {
        var metadata=TepTin.doc(room,user,text,0);var file=files.lay(metadata.id());
        if(!file.room().equals(room) || !file.user().equals(user) || !file.dongGoi().equals(text))throw new IllegalArgumentException("Thẻ tệp không thuộc tài khoản/phòng hoặc metadata đã thay đổi.");
        if(!duplicate && live.contains(file.id()))throw new IllegalArgumentException("Tệp đã được gửi. Hãy dùng Gửi lại trên tin đang có.");
    }
    public WebFile describe(String room,String user,String text) {
        var t=TepTin.doc(room,user,text,0);
        try { return dto(files.lay(t.id())); }
        catch(IllegalArgumentException e) { return new WebFile(t.id(),t.ten(),t.kichThuoc(),t.crc(),text,false); }
    }
    private WebFile dto(TepTin file) {
        boolean image=false;
        try { image=Files.isRegularFile(files.duong(file.room(),file.id(),".jpg")); }catch(IOException ignored) { }
        return new WebFile(file.id(),file.ten(),file.kichThuoc(),file.crc(),file.dongGoi(),image);
    }
    TepTin metadata(String id) { return files.lay(id); }
    Download open(String id,boolean thumbnail) throws IOException {
        var file=files.lay(id);Path path=files.duong(file.room(),id,thumbnail?".jpg":".bin");
        return new Download(Files.newInputStream(path),file.ten(),Files.size(path),file.crc(),thumbnail);
    }
    void discard(String user,String id,Set<String> live) throws IOException {
        var t=files.lay(id);
        if(!t.user().equals(user) || live.contains(id))throw new IllegalArgumentException("Chỉ hủy được tệp của bạn khi chưa gửi thành tin.");
        files.xoa(id);
    }
    // Trang thai thu hoi/xoa phong da commit; loi cleanup khong doi thanh ket qua nghiep vu that bai.
    void remove(String id) {
        try { files.xoa(id);pendingRemoval.remove(id); }
        catch(IOException e) { if(defer(pendingRemoval,id))LOG.warn("FILE_CLEANUP_DEFERRED fileId={}",id); }
    }
    void removeRoom(String room) {
        try { files.xoaPhong(room);pendingRooms.remove(room); }
        catch(IOException e) { if(defer(pendingRooms,room))LOG.warn("ROOM_FILE_CLEANUP_DEFERRED roomId={}",room); }
    }
    private boolean defer(Set<String> queue,String id) {
        boolean added=queue.add(id);
        if(queue.size()>1024)queue.remove(queue.iterator().next());
        return added;
    }
    // Tep da upload chua gui ton tai 24h; khoi dong lai van cho phep gui lai metadata cu.
    void cleanup(Set<String> live,Set<String> rooms) throws IOException {
        for(String id:List.copyOf(pendingRemoval))remove(id);
        for(String room:List.copyOf(pendingRooms))removeRoom(room);
        long now=System.currentTimeMillis();
        for(var file:files.entries())if(!live.contains(file.id()) && (!rooms.contains(file.room()) || now-file.luc()>STAGED_TTL))remove(file.id());
    }
    private static String extension(String name) { int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT); }
}
