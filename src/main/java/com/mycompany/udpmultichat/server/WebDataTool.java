package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Cong cu offline: giu khoa kho, kiem checksum va chi khoi phuc vao thu muc moi. */
public final class WebDataTool {
    private static final long MAX_BYTES=1024L*1024*1024,MAX_TEXT=64L*1024*1024;
    private static final int MAX_FILES=20000,MAX_MANIFEST=8*1024*1024;
    private WebDataTool(){ }
    public record Summary(int accounts,int rooms,int messages,int files,long fileBytes) { }
    private record Item(String name,long size,String hash) { }
    public static void main(String[] args)throws IOException {
        if(args.length==2 && args[0].equals("verify")){System.out.println(verify(Path.of(args[1])));return;}
        if(args.length!=3)throw new IllegalArgumentException("Dùng: backup <data-da-dung> <zip-moi> | verify <zip> | restore <zip> <data-moi> | import-copy <ban-sao-data> <data-moi>");
        Summary result=switch(args[0]){
            case "backup"->backup(Path.of(args[1]),Path.of(args[2]));
            case "restore"->restore(Path.of(args[1]),Path.of(args[2]));
            case "import-copy"->importCopy(Path.of(args[1]),Path.of(args[2]));
            default->throw new IllegalArgumentException("Lệnh không hợp lệ.");
        };System.out.println(result);
    }
    public static Summary backup(Path source,Path destination)throws IOException {
        Path root=directory(source),out=destination.toAbsolutePath().normalize();
        if(Files.exists(out,LinkOption.NOFOLLOW_LINKS))throw new FileAlreadyExistsException(out.toString());
        Path parent=directory(out.getParent());if(parent.startsWith(root))throw new IOException("Đặt archive ngoài thư mục dữ liệu.");
        try(var channel=FileChannel.open(root.resolve("web.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=lock(channel)){
            if(!lock.isValid())throw new IOException("Không giữ được khóa dữ liệu.");
            List<Item> items=inventory(root);Summary summary=validateCopy(root,parent);
            Path temp=Files.createTempFile(parent,".udp-backup-",".tmp");
            try {
                Properties manifest=new Properties();manifest.setProperty("format","UDPmultiChat-backup-1");manifest.setProperty("created",java.time.Instant.now().toString());
                for(var item:items)manifest.setProperty("file."+item.name(),item.size()+":"+item.hash());
                try(var zip=new ZipOutputStream(Files.newOutputStream(temp))){
                    var writer=new StringWriter();manifest.store(writer,"Private backup; checksums detect corruption, not authenticity");
                    zip.putNextEntry(new ZipEntry("manifest.properties"));zip.write(writer.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
                    for(var item:items){zip.putNextEntry(new ZipEntry("data/"+item.name()));try(var input=Files.newInputStream(root.resolve(item.name()))){String hash=copy(input,zip,item.size());if(!hash.equals(item.hash()))throw new IOException("Dữ liệu thay đổi trong khi backup: "+item.name());}zip.closeEntry();}
                }
                Files.move(temp,out);return summary;
            } finally {Files.deleteIfExists(temp);}
        }
    }
    public static Summary verify(Path archive)throws IOException {
        Path parent=directory(archive.toAbsolutePath().normalize().getParent()),stage=Files.createTempDirectory(parent,".udp-verify-");
        try{extract(archive,stage);return validateCopy(stage,parent);}finally{cleanup(stage,parent);}
    }
    public static Summary restore(Path archive,Path destination)throws IOException {
        Path target=destination.toAbsolutePath().normalize(),parent=directory(target.getParent());
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))throw new FileAlreadyExistsException("Chỉ khôi phục vào thư mục chưa tồn tại: "+target);
        Path stage=Files.createTempDirectory(parent,".udp-restore-");
        try{extract(archive,stage);Summary summary=validateCopy(stage,parent);Files.move(stage,target);return summary;}finally{cleanup(stage,parent);}
    }
    public static Summary importCopy(Path sourceCopy,Path destination)throws IOException {
        Path parent=directory(destination.toAbsolutePath().normalize().getParent());
        Path archive=parent.resolve(".udp-import-"+UUID.randomUUID()+".zip");
        try{backup(sourceCopy,archive);return restore(archive,destination);}finally{Files.deleteIfExists(archive);}
    }
    private static FileLock lock(FileChannel channel)throws IOException {
        try{FileLock lock=channel.tryLock();if(lock==null)throw new IOException("Kho đang chạy. Dừng server trước khi sao lưu.");return lock;}
        catch(OverlappingFileLockException e){throw new IOException("Kho đang chạy. Dừng server trước khi sao lưu.",e);}
    }
    private static Path directory(Path path)throws IOException {
        if(path==null || Files.isSymbolicLink(path) || !Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Chọn thư mục thật đã tồn tại.");
        return path.toRealPath();
    }
    private static String safe(String name)throws IOException {
        if(name.isBlank() || name.length()>256 || name.contains("\\") || name.contains(":") || name.startsWith("/") || Arrays.stream(name.split("/",-1)).anyMatch(p->p.isEmpty() || p.equals(".") || p.equals("..")))throw new IOException("Đường dẫn archive không hợp lệ.");
        if(!name.matches("(?:config|accounts|moderation|web-preferences|web-recall-digests|members|nicknames|invites)\\.properties|users\\.csv|rooms\\.csv|rooms/(?:sanh|[a-f0-9]{24})\\.log|avatars/[a-z0-9_]{3,20}\\.png|uploads/[a-z0-9]{1,40}/[a-f0-9-]{36}\\.(?:bin|jpg|properties)|audit(?:\\.[0-9]+)?\\.log"))throw new IOException("Tệp chưa hỗ trợ trong kho: "+name);
        return name;
    }
    private static List<Item> inventory(Path root)throws IOException {
        List<Item> items=new ArrayList<>();long total=0,text=0;
        try(var paths=Files.walk(root)){
            var iterator=paths.iterator();int visited=0;
            while(iterator.hasNext()){
                Path path=iterator.next();if(++visited>MAX_FILES*3)throw new IOException("Kho có quá nhiều đường dẫn.");
                if(path.equals(root))continue;
                if(Files.isSymbolicLink(path) || !path.toRealPath().startsWith(root))throw new IOException("Không sao lưu liên kết ra ngoài kho.");
                if(Files.isDirectory(path))continue;
                if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Kho có tệp đặc biệt.");
                String name=root.relativize(path).toString().replace('\\','/');
                if(name.equals("web.lock") || name.endsWith(".tmp") || name.endsWith(".part") || name.startsWith("web-upload-tmp/"))continue;
                safe(name);long size=Files.size(path);total+=size;if(!name.endsWith(".bin") && !name.endsWith(".jpg"))text+=size;
                if(total>MAX_BYTES || text>MAX_TEXT || items.size()>=MAX_FILES)throw new IOException("Kho vượt giới hạn backup: 1 GB, 64 MB metadata/lịch sử, 20.000 tệp.");
                try(var input=Files.newInputStream(path)){items.add(new Item(name,size,copy(input,OutputStream.nullOutputStream(),size)));}
            }
        }
        if(items.stream().noneMatch(i->i.name().equals("config.properties")) || items.stream().noneMatch(i->i.name().equals("users.csv")))throw new IOException("Bản sao thiếu config.properties hoặc users.csv.");
        items.sort(Comparator.comparing(Item::name));return List.copyOf(items);
    }
    private static String copy(InputStream input,OutputStream out,long limit)throws IOException {
        MessageDigest digest;
        try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        byte[] buffer=new byte[32768];long total=0;int n;
        while((n=input.read(buffer))!=-1){total+=n;if(total>limit)throw new IOException("Tệp vượt kích thước đã khai báo.");digest.update(buffer,0,n);out.write(buffer,0,n);}
        if(total!=limit)throw new IOException("Tệp thiếu byte so với khai báo.");return HexFormat.of().formatHex(digest.digest());
    }
    private static Properties properties(Path file)throws IOException {Properties p=new Properties();try(var r=Files.newBufferedReader(file,StandardCharsets.UTF_8)){p.load(r);}return p;}
    private static void extract(Path archive,Path stage)throws IOException {
        if(!Files.isRegularFile(archive,LinkOption.NOFOLLOW_LINKS) || Files.size(archive)>MAX_BYTES+MAX_MANIFEST+10_000_000)throw new IOException("Archive không hợp lệ hoặc quá lớn.");
        try(var zip=new ZipFile(archive.toFile())){
            var manifestEntry=zip.getEntry("manifest.properties");if(manifestEntry==null || manifestEntry.getSize()<0 || manifestEntry.getSize()>MAX_MANIFEST)throw new IOException("Manifest thiếu hoặc quá lớn.");
            Properties manifest=new Properties();try(var input=zip.getInputStream(manifestEntry)){byte[] data=input.readNBytes(MAX_MANIFEST+1);if(data.length>MAX_MANIFEST)throw new IOException("Manifest quá lớn.");manifest.load(new StringReader(new String(data,StandardCharsets.UTF_8)));}
            if(!"UDPmultiChat-backup-1".equals(manifest.getProperty("format")))throw new IOException("Định dạng backup chưa hỗ trợ.");
            Map<String,Item> expected=new TreeMap<>();long total=0,text=0;
            for(String key:manifest.stringPropertyNames())if(key.startsWith("file.")){
                String name=safe(key.substring(5));String[] fields=manifest.getProperty(key).split(":",-1);
                if(fields.length!=2 || !fields[0].matches("[0-9]{1,10}") || !fields[1].matches("[a-f0-9]{64}"))throw new IOException("Checksum/kích thước trong manifest không hợp lệ.");
                long size=Long.parseLong(fields[0]);total+=size;if(!name.endsWith(".bin") && !name.endsWith(".jpg"))text+=size;
                if(total>MAX_BYTES || text>MAX_TEXT || expected.size()>=MAX_FILES)throw new IOException("Archive vượt giới hạn dữ liệu.");
                expected.put("data/"+name,new Item(name,size,fields[1]));
            }
            Set<String> seen=new HashSet<>();var entries=zip.entries();
            while(entries.hasMoreElements()){
                ZipEntry entry=entries.nextElement();if(!seen.add(entry.getName()))throw new IOException("Archive trùng entry.");
                if(entry.getName().equals("manifest.properties"))continue;
                Item item=expected.get(entry.getName());if(item==null || entry.isDirectory() || entry.getSize()!=item.size())throw new IOException("Archive có entry lạ/sai kích thước.");
                Path file=stage.resolve(item.name()).normalize();if(!file.startsWith(stage))throw new IOException("Đường dẫn vượt staging.");Files.createDirectories(file.getParent());
                try(var input=zip.getInputStream(entry);var output=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)){if(!copy(input,output,item.size()).equals(item.hash()))throw new IOException("Checksum không khớp: "+item.name());}
            }
            if(seen.size()!=expected.size()+1 || !seen.containsAll(expected.keySet()))throw new IOException("Archive thiếu tệp.");
        }catch(IllegalArgumentException e){throw new IOException("Archive/manifest bị hỏng.",e);}
    }
    private static Summary validateCopy(Path source,Path parent)throws IOException {
        List<Item> items=inventory(source);Path probe=Files.createTempDirectory(parent,".udp-validate-");
        try {
            for(var item:items){Path target=probe.resolve(item.name());Files.createDirectories(target.getParent());Files.copy(source.resolve(item.name()),target);}
            var config=properties(probe.resolve("config.properties"));
            if(config.containsKey("admin.password") || !config.containsKey("admin.passwordHash") || !config.containsKey("admin.salt"))throw new IOException("Config chưa chuyển mật khẩu một lần thành hash. Khởi động/dừng bản nguồn trước khi sao chép.");
            var disk=new KhoLuuTru(probe,ignored->{ });int accountCount=disk.docNguoiDung().size();var users=new QuanLyNguoiDung(disk,true);var rooms=new QuanLyPhong(disk,List::of);
            if(users.tongNguoiDung()!=accountCount || users.tatCa().stream().noneMatch(u->u.vaiTro==com.mycompany.udpmultichat.common.model.VaiTro.ADMIN))throw new IOException("Kho thiếu admin hợp lệ hoặc cần sửa tài khoản trước khi nhập.");
            Set<String> known=users.tatCa().stream().map(u->u.id).collect(java.util.stream.Collectors.toSet());
            for(var room:rooms.tatCa())if(!room.id.equals("sanh") && !known.contains(room.chuId))throw new IOException("Chủ phòng không tồn tại: "+room.id);
            for(var user:users.tatCa())if(!user.anhHash.isEmpty()){
                Path avatar=probe.resolve("avatars/"+user.tenDangNhap+".png");if(!Files.isRegularFile(avatar))throw new IOException("Avatar bị thiếu: "+user.tenDangNhap);
                byte[] image=Files.readAllBytes(avatar);PhanManh.docAnh(image,true);if(!PhanManh.crc(image).equals(user.anhHash))throw new IOException("Avatar không khớp hash: "+user.tenDangNhap);
            }
            if(Files.exists(probe.resolve("moderation.properties"))){var rules=properties(probe.resolve("moderation.properties"));for(String key:rules.stringPropertyNames())if(!key.endsWith(":reason"))Long.parseLong(rules.getProperty(key));}
            Set<String> liveRooms=rooms.tatCa().stream().map(r->r.id).collect(java.util.stream.Collectors.toSet());
            for(var item:items)if(item.name().startsWith("rooms/") && item.name().endsWith(".log")){
                String roomId=item.name().substring(6,item.name().length()-4);if(!liveRooms.contains(roomId))disk.docTin(roomId);
            }
            Map<String,TepTin> files=new HashMap<>();long fileBytes=0;
            for(var item:items)if(item.name().startsWith("uploads/") && item.name().endsWith(".properties")){
                Properties p=properties(probe.resolve(item.name()));
                try{
                    var metadata=new TepTin(p.getProperty("id"),p.getProperty("room"),p.getProperty("user"),p.getProperty("name"),Long.parseLong(p.getProperty("size")),Long.parseLong(p.getProperty("crc")),Long.parseLong(p.getProperty("time")));
                    String expected="uploads/"+metadata.room()+"/"+metadata.id()+".properties";
                    if(!expected.equals(item.name()) || files.putIfAbsent(metadata.id(),metadata)!=null)throw new IOException("Metadata tệp sai định danh.");
                    rooms.lay(metadata.room());Path bin=probe.resolve("uploads/"+metadata.room()+"/"+metadata.id()+".bin");
                    if(metadata.kichThuoc()<=0 || metadata.kichThuoc()>WebFiles.MAX_BYTES || !Files.isRegularFile(bin) || Files.size(bin)!=metadata.kichThuoc())throw new IOException("Tệp thiếu hoặc sai kích thước: "+item.name());
                    CRC32 crc=new CRC32();try(var input=Files.newInputStream(bin)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1)crc.update(buffer,0,n);}if(crc.getValue()!=metadata.crc())throw new IOException("CRC tệp sai: "+item.name());fileBytes+=metadata.kichThuoc();
                }catch(RuntimeException e){throw new IOException("Metadata tệp không hợp lệ: "+item.name(),e);}
            }
            for(var item:items)if(item.name().startsWith("uploads/") && !item.name().endsWith(".properties")){
                String name=Path.of(item.name()).getFileName().toString(),id=name.substring(0,name.lastIndexOf('.'));
                if(!files.containsKey(id))throw new IOException("Tệp mồ côi không có metadata: "+item.name());
            }
            for(var message:rooms.tinNhan().tatCa())if(message.loai.equals("F")){
                TepTin file=TepTin.doc(message.roomId,message.userId,message.noiDung,message.thoiGian);var stored=files.get(file.id());if(stored==null || !stored.room().equals(message.roomId) || !stored.user().equals(message.userId) || !stored.dongGoi().equals(message.noiDung))throw new IOException("Tin tệp thiếu metadata/bin tương ứng.");
            }
            return new Summary(users.tongNguoiDung(),rooms.tatCa().size(),rooms.tinNhan().tatCa().size(),files.size(),fileBytes);
        }catch(IllegalArgumentException e){throw new IOException("Bản sao không qua kiểm tra nghiệp vụ: "+e.getMessage(),e);}finally{cleanup(probe,parent);}
    }
    private static void cleanup(Path temp,Path parent)throws IOException {
        if(!temp.getParent().equals(parent) || !temp.getFileName().toString().startsWith(".udp-"))throw new IOException("Không xóa ngoài staging do công cụ tạo.");
        if(!Files.exists(temp,LinkOption.NOFOLLOW_LINKS))return;
        Files.walkFileTree(temp,new SimpleFileVisitor<>(){
            @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs)throws IOException{Files.delete(file);return FileVisitResult.CONTINUE;}
            @Override public FileVisitResult postVisitDirectory(Path dir,IOException error)throws IOException{if(error!=null)throw error;Files.delete(dir);return FileVisitResult.CONTINUE;}
        });
    }
}
