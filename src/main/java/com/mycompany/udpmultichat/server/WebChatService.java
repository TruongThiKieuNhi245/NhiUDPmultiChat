package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;

/** Nghiep vu dung chung kho desktop; adapter web khong tu ghi tin. */
public final class WebChatService implements AutoCloseable {
    private final QuanLyNguoiDung users;
    private final QuanLyPhong rooms;
    private final KiemDuyet moderation;
    private final WebWorkspace workspace;
    private final WebFiles files;
    private volatile java.util.function.Consumer<Event> events=event -> { };
    public record Event(String type,String roomId,Object data) { }
    public void onEvent(java.util.function.Consumer<Event> listener) { events=listener; }
    private void changed() { events.accept(new Event("workspace","",Map.of())); }
    private final Map<String, Session> sessions = new HashMap<>();
    private final FileChannel lockChannel;
    private final FileLock dataLock;
    private static final InetSocketAddress INTERNAL = new InetSocketAddress("127.0.0.1", 0);

    public WebChatService(Path directory) throws IOException {
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(directory.resolve("web.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired = lockChannel.tryLock();
        if (acquired == null) { lockChannel.close(); throw new IOException("Thư mục web đang được sử dụng."); }
        dataLock = acquired;
        try {
            // Khong tao admin co mat khau mac dinh cho kho web moi.
            Path config = directory.resolve("config.properties");
            if (!Files.exists(config)) {
                Files.writeString(config, "admin.password=" + BaoMat.ngauNhien(32) + "\n", StandardOpenOption.CREATE_NEW);
            }
            KhoLuuTru store = new KhoLuuTru(directory, s -> { });
            users = new QuanLyNguoiDung(store, true);
            rooms = new QuanLyPhong(store, users::danhSach);
            moderation = new KiemDuyet(store, new NhatKy(directory, 5_000_000));
            rooms.kiemDuyet(moderation::kiemTraGui, moderation::kiemTraVao);
            workspace=new WebWorkspace(store,users,rooms,moderation);
            files=new WebFiles(directory);cleanupFiles();
        } catch (IOException | RuntimeException e) { dataLock.release(); lockChannel.close(); throw e; }
    }

    public synchronized void register(String login, String password) throws IOException {
        if (users.tongNguoiDung() >= 1000) { throw new IllegalArgumentException("Đã đủ tài khoản cho bản Web 1."); }
        users.dangKy(login, password);
    }

    public synchronized Identity login(String login, String password) throws IOException {
        expire();
        if (sessions.size() >= 128) { throw new IllegalArgumentException("Đã đủ phiên. Hãy thử lại sau."); }
        var person = users.dangNhap(login, password, INTERNAL);
        try {
            workspace.restore(person);
            String token = BaoMat.ngauNhien(24);
            sessions.put(token, new Session(person));
            changed();
            return new Identity(token, person.nguoi.id, person.nguoi.tenDangNhap, person.nguoi.tenHienThi);
        } catch (IOException | RuntimeException e) { users.dangXuat(person); throw e; }
    }

    public synchronized Identity identity(String token) {
        var person = require(token);
        return new Identity(token, person.nguoi.id, person.nguoi.tenDangNhap, person.nguoi.tenHienThi);
    }

    private QuanLyNguoiDung.Phien require(String token) { return require(token,true); }
    private QuanLyNguoiDung.Phien require(String token,boolean touch) {
        Session s = sessions.get(token);
        if (s == null || System.currentTimeMillis() - s.lastSeen > 1_800_000 || users.biKhoa(s.person.nguoi.id)) {
            logout(token); throw new IllegalArgumentException("Phiên đã hết hạn. Hãy đăng nhập lại.");
        }
        if (touch) { s.lastSeen = System.currentTimeMillis(); users.ping(s.person); }
        return s.person;
    }

    public synchronized boolean allowed(String token, String room) {
        try { var p=require(token,false); users.kiemTraBatBuoc(p,Protocol.GUI_TIN); rooms.kiemTraGui(p, room); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    public synchronized List<TinNhan> history(String token, String room, long after) {
        workspace.read(require(token), room);
        return rooms.layTin(room, false, after);
    }

    // Trang gan nhat hoac 50 tin truoc cursor, cung kiem quyen voi lich su dong bo.
    public synchronized List<TinNhan> historyPage(String token, String room, long before) {
        workspace.read(require(token), room);
        long end = before == 0 ? rooms.mocTin(room) : Math.min(before - 1, rooms.mocTin(room));
        long start = Math.max(0, end - 50);
        return rooms.layTin(room, false, start).stream().filter(t -> t.msgId <= end).toList();
    }

    // Chi UdpChatTransport.receive duoc goi diem chap nhan tin moi nay.
    public synchronized Accepted receive(String token, String room, String clientMsgId, String text) throws IOException {
        return receive(token,room,clientMsgId,text,"T",0);
    }
    public synchronized Accepted receive(String token,String room,String clientMsgId,String text,String kind,long replyTo) throws IOException {
        var person = require(token);
        users.kiemTraBatBuoc(person,Protocol.GUI_TIN);
        if (!Set.of("T","S","F").contains(kind) || replyTo<0) throw new IllegalArgumentException("Loại tin không hợp lệ.");
        if (kind.equals("S") && (replyTo!=0 || !Protocol.stickerHopLe(text))) throw new IllegalArgumentException("Sticker không hợp lệ.");
        if (kind.equals("F") && replyTo!=0)throw new IllegalArgumentException("Thẻ tệp không hỗ trợ reply trực tiếp.");
        String stored=kind.equals("T")?WebReply.encode(text,replyTo):text;
        rooms.kiemTraGui(person, room);
        if (replyTo>0) rooms.tinNhan().theoId(room,replyTo);
        TinNhan old=rooms.tinNhan().theoMa(room,person.nguoi.id,clientMsgId);
        if(old!=null && old.thuHoi)workspace.checkRecalledRetry(old,kind,stored);
        boolean duplicate = old != null;
        if(kind.equals("F") && (old==null || !old.thuHoi))files.validate(room,person.nguoi.id,text,rooms.tinNhan().cacTep(),duplicate);
        if (!duplicate && rooms.mocTin(room) >= 10000) { throw new IllegalArgumentException("Phòng đã đạt giới hạn 10.000 tin của bộ lưu trữ hiện tại."); }
        if (!duplicate && !moderation.choTin(person, System.currentTimeMillis())) {
            throw new IllegalArgumentException("Bạn gửi quá nhanh. Hãy chờ một chút.");
        }
        TinNhan saved = rooms.themTin(person, room, stored, clientMsgId, kind);
        return new Accepted(saved, duplicate);
    }

    public synchronized void logout(String token) {
        Session s = sessions.remove(token);
        if (s != null) { users.dangXuat(s.person); changed(); }
    }
    public synchronized void expire() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> {
            if (now - e.getValue().lastSeen <= 1_800_000) { return false; }
            users.dangXuat(e.getValue().person); return true;
        });
    }
    @Override public synchronized void close() throws IOException {
        sessions.values().forEach(s -> users.dangXuat(s.person)); sessions.clear();
        dataLock.release(); lockChannel.close();
    }
    public synchronized boolean active(String token) {
        try { require(token,false); return true; } catch (IllegalArgumentException e) { return false; }
    }
    public synchronized boolean canRead(String token,String room) {
        try { workspace.read(require(token,false),room); return true; } catch (IllegalArgumentException e) { return false; }
    }
    public synchronized Map<String,Object> snapshot(String token) { return workspace.snapshot(require(token)); }
    public synchronized List<WebWorkspace.Member> members(String token,String room) { return workspace.members(require(token),room); }
    public synchronized List<WebWorkspace.Profile> searchUsers(String token,String query) { return workspace.searchUsers(require(token),query); }
    public synchronized Object sentInvites(String token,String room) { return workspace.sent(require(token),room); }
    public synchronized byte[] avatar(String token,String id) throws IOException { return workspace.avatar(require(token),id); }
    public synchronized Object action(String token,String action,Map<String,String> args) throws IOException {
        var person=require(token);
        mutation(token);
        Object result=workspace.action(person,action,args);
        if(action.equals("room-delete"))files.removeRoom(args.get("roomId"));
        changed(); return result;
    }
    private void mutation(String token) {
        var attempts=sessions.get(token).actions;
        long now=System.currentTimeMillis();
        while(!attempts.isEmpty() && attempts.peekFirst()<now-10000)attempts.removeFirst();
        if(attempts.size()>=30)throw new IllegalArgumentException("Bạn thao tác quá nhanh. Hãy chờ một chút.");
        attempts.addLast(now);
    }
    public synchronized void typing(String token,String room) {
        var p=require(token);
        if (workspace.typing(p,room)) events.accept(new Event("typing",room,Map.of("userId",p.nguoi.id,"name",p.nguoi.tenHienThi,"expires",System.currentTimeMillis()+3500)));
    }
    public synchronized void deliver(TinNhan t,java.util.function.Consumer<WebMessage> delivery) { delivery.accept(message(t)); }
    public synchronized WebMessage message(TinNhan t) {
        t=rooms.tinNhan().theoId(t.roomId,t.msgId);
        long reply=t.thuHoi || !t.loai.equals("T")?0:WebReply.id(t.noiDung);
        String quote="",author="";
        if (reply>0) {
            var source=rooms.tinNhan().theoId(t.roomId,reply); author=source.ten;
            quote=source.thuHoi?"Tin đã thu hồi":source.loai.equals("S")?"[Sticker]":source.loai.equals("F")?"[Tệp] "+TepTin.doc(source.roomId,source.userId,source.noiDung,0).ten():WebReply.body(source.noiDung);
            if (quote.length()>160) quote=quote.substring(0,160)+"…";
        }
        var user=users.timTheoId(t.userId);
        return new WebMessage(t.msgId,t.roomId,t.userId,t.ten,t.thuHoi?"":t.loai.equals("T")?WebReply.body(t.noiDung):t.noiDung,
                t.thoiGian,t.maClient,t.loai,t.thuHoi,reply,author,quote,rooms.layBietDanh(t.roomId,t.userId),user==null?"":user.anhHash,
                !t.thuHoi && t.loai.equals("F")?files.describe(t.roomId,t.userId,t.noiDung):null);
    }
    public synchronized List<WebMessage> messages(String token,String room,long cursor,boolean before) {
        return (before?historyPage(token,room,cursor):history(token,room,cursor)).stream().map(this::message).toList();
    }
    public synchronized List<WebMessage> search(String token,String room,String query,long after) {
        workspace.read(require(token),room);
        if (query.length()<1 || query.length()>100 || after<0) throw new IllegalArgumentException("Từ khoá hoặc cursor không hợp lệ.");
        List<WebMessage> found=new ArrayList<>(); List<TinNhan> page;
        do {
            page=rooms.layTin(room,false,after);
            for (var t:page) {
                after=t.msgId;
                if (!t.thuHoi && t.loai.equals("T") && WebReply.body(t.noiDung).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) found.add(message(t));
                if (found.size()==50) return found;
            }
        } while (page.size()==50);
        return found;
    }
    public synchronized List<WebMessage> around(String token,String room,long id) {
        workspace.read(require(token),room); rooms.tinNhan().theoId(room,id);
        return rooms.layTin(room,false,Math.max(0,id-25)).stream().map(this::message).toList();
    }
    public synchronized Object recalls(String token,String room,long after) {
        workspace.read(require(token),room);
        if (after<0) throw new IllegalArgumentException("Cursor không hợp lệ.");
        String[] result=rooms.tinNhan().danhSachThuHoi(room,after);
        return Map.of("cursor",Long.parseLong(result[1]),"more",Boolean.parseBoolean(result[2]),
                "ids",Arrays.stream(result).skip(3).map(Long::parseLong).toList());
    }
    public synchronized void recall(String token,String room,long id) throws IOException {
        var person=require(token); mutation(token); users.kiemTraBatBuoc(person,Protocol.GUI_TIN); rooms.kiemTraGui(person,room);
        var original=rooms.tinNhan().theoId(room,id);
        if (!QuanLyTinNhan.duocThuHoi(person.nguoi,rooms.lay(room),original,System.currentTimeMillis())) throw new IllegalArgumentException("Bạn không có quyền thu hồi hoặc tin đã quá 10 phút.");
        workspace.rememberRecall(original);
        var t=rooms.tinNhan().thuHoi(person,rooms.lay(room),id);
        events.accept(new Event("recall",room,message(t)));
        if(original.loai.equals("F") && !original.thuHoi)files.remove(TepTin.doc(room,original.userId,original.noiDung,0).id());
    }
    public synchronized String export(String token,String room) {
        workspace.read(require(token),room);
        StringBuilder out=new StringBuilder("UDPmultiChat — "+rooms.lay(room).ten+"\n");
        long after=0; List<TinNhan> page;
        do {
            page=rooms.layTin(room,false,after);
            for (var t:page) {
                var m=message(t); after=t.msgId;
                out.append(java.time.Instant.ofEpochMilli(m.timestamp())).append(" | ").append(m.name()).append(" | ")
                        .append(m.recalled()?"[Tin đã thu hồi]":m.kind().equals("S")?"[Sticker "+m.text()+"]":m.file()!=null?"[Tệp] "+m.file().name()+" ("+TepTin.dungLuong(m.file().size())+")":m.text()).append('\n');
            }
        } while (page.size()==50);
        return out.toString();
    }
    public synchronized WebFiles.Ticket beginUpload(String token,String room,String name,long size) throws IOException {
        var person=require(token);mutation(token);users.kiemTraBatBuoc(person,Protocol.GUI_TIN);rooms.kiemTraGui(person,room);
        cleanupFiles();return files.begin(room,person.nguoi.id,name,size);
    }
    public long writeUpload(WebFiles.Ticket ticket,java.io.InputStream input) throws IOException { return files.write(ticket,input); }
    public synchronized WebFile finishUpload(String token,WebFiles.Ticket ticket,long crc) throws IOException {
        var person=require(token);users.kiemTraBatBuoc(person,Protocol.GUI_TIN);rooms.kiemTraGui(person,ticket.room());
        if(!person.nguoi.id.equals(ticket.user()))throw new IllegalArgumentException("Lượt tải lên thuộc tài khoản khác.");
        return files.finish(ticket,crc);
    }
    public void abortUpload(WebFiles.Ticket ticket) throws IOException { files.abort(ticket); }
    public synchronized WebFiles.Download download(String token,String id,boolean thumbnail) throws IOException {
        var person=require(token);var file=files.metadata(id);workspace.read(person,file.room());
        if(!rooms.tinNhan().cacTep().contains(id))throw new IllegalArgumentException("Tệp chưa được gửi hoặc đã thu hồi.");
        return files.open(id,thumbnail);
    }
    public synchronized void discardUpload(String token,String id) throws IOException {
        var person=require(token);mutation(token);files.discard(person.nguoi.id,id,rooms.tinNhan().cacTep());
    }
    public synchronized void cleanupFiles() throws IOException {
        files.cleanup(rooms.tinNhan().cacTep(),rooms.tatCa().stream().map(r->r.id).collect(java.util.stream.Collectors.toSet()));
    }
    public record Identity(String token, String id, String login, String name) { }
    public record Accepted(TinNhan message, boolean duplicate) { }
    private static final class Session {
        private final QuanLyNguoiDung.Phien person;
        private long lastSeen = System.currentTimeMillis();
        private final ArrayDeque<Long> actions=new ArrayDeque<>();
        Session(QuanLyNguoiDung.Phien person) { this.person = person; }
    }
}
