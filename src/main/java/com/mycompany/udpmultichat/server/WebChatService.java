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
    private final NhatKy audit;
    private final WebAdministration admin;
    private volatile java.util.function.Consumer<Event> events=event -> { };
    public record Event(String type,String roomId,Object data,String recipientId) {
        public Event(String type,String roomId,Object data){this(type,roomId,data,"");}
    }
    public record MemberNotice(String id,String userId,String targetName,String actorId,String actorName,String roomName,String kind,long until,String reason) { }
    private MemberNotice memberNotice(QuanLyNguoiDung.Phien actor,String action,Map<String,String> args,boolean administration) {
        if(!Set.of("member-moderate","room-transfer").contains(action))return null;
        String room=administration && action.equals("room-transfer")?args.getOrDefault("id",""):args.getOrDefault("roomId","");
        String user=args.getOrDefault("userId","");
        String kind=action.equals("room-transfer")?"OWNER":args.getOrDefault("kind","");
        if(!kind.equals("OWNER") && !kind.equals("KICK") && "-1".equals(args.get("seconds")))kind="UN"+kind;
        long until=kind.equals("MUTE") || kind.equals("BAN")?moderation.den(room,user,kind):0;
        String reason=kind.equals("MUTE") || kind.equals("BAN")?moderation.lyDo(room,user,kind):kind.equals("KICK")?args.getOrDefault("reason","").strip():"";
        String roomName=room.equals("*")?"toàn hệ thống":rooms.lay(room).ten;
        var notice=new MemberNotice(UUID.randomUUID().toString(),user,users.lay(user).tenHienThi+" (@"+users.lay(user).tenDangNhap+")",actor.nguoi.id,actor.nguoi.tenHienThi+" (@"+actor.nguoi.tenDangNhap+")",roomName,kind,until,reason);
        events.accept(new Event("member-notice",room,notice,user));
        if(Set.of("KICK","BAN","OWNER").contains(kind) && !room.equals("*")) {
            var target=users.lay(user);String detail=kind.equals("OWNER")?" trở thành chủ phòng.":kind.equals("BAN")?" đã bị cấm vào phòng.":" đã được đưa ra khỏi phòng.";
            events.accept(new Event("room-activity",room,Map.of("id",notice.id(),"actorId",actor.nguoi.id,"userId",user,"text",target.tenHienThi+" (@"+target.tenDangNhap+")"+detail)));
        }
        return notice;
    }
    public void onEvent(java.util.function.Consumer<Event> listener) { events=listener; }
    private void changed() { events.accept(new Event("workspace","",Map.of())); }
    private final Map<String, Session> sessions = new HashMap<>();
    private final FileChannel lockChannel;
    private final FileLock dataLock;
    private static final InetSocketAddress INTERNAL = new InetSocketAddress("127.0.0.1", 0);

    public WebChatService(Path directory) throws IOException { this(directory,""); }
    public WebChatService(Path directory,String initialAdminPassword) throws IOException {
        Files.createDirectories(directory);
        lockChannel = FileChannel.open(directory.resolve("web.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired = lockChannel.tryLock();
        if (acquired == null) { lockChannel.close(); throw new IOException("Thư mục web đang được sử dụng."); }
        dataLock = acquired;
        try {
            // Khong tao admin co mat khau mac dinh cho kho web moi.
            Path config = directory.resolve("config.properties");
            if (!Files.exists(config)) {
                if(!initialAdminPassword.isEmpty() && (initialAdminPassword.length()<12 || initialAdminPassword.length()>128))throw new IllegalArgumentException("Mật khẩu quản trị ban đầu cần 12–128 ký tự.");
                var properties=new Properties();properties.setProperty("admin.password",initialAdminPassword.isEmpty()?BaoMat.ngauNhien(32):initialAdminPassword);
                try(var out=Files.newBufferedWriter(config,StandardOpenOption.CREATE_NEW)){properties.store(out,"One-time admin initialization");}
            }
            KhoLuuTru store = new KhoLuuTru(directory, s -> { });
            users = new QuanLyNguoiDung(store, true);
            rooms = new QuanLyPhong(store, users::danhSach);
            audit=new NhatKy(directory,5_000_000);
            moderation = new KiemDuyet(store,audit);
            rooms.kiemDuyet(moderation::kiemTraGui, moderation::kiemTraVao);
            workspace=new WebWorkspace(store,users,rooms,moderation);
            files=new WebFiles(directory);cleanupFiles();
            admin=new WebAdministration(users,rooms,moderation,files,audit);
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
        if (!Set.of("T","S","F","Y").contains(kind) || replyTo<0) throw new IllegalArgumentException("Loại tin không hợp lệ.");
        if (kind.equals("S") && (replyTo!=0 || !Protocol.stickerHopLe(text))) throw new IllegalArgumentException("Sticker không hợp lệ.");
        if (kind.equals("F") && replyTo!=0)throw new IllegalArgumentException("Thẻ tệp không hỗ trợ reply trực tiếp.");
        String stored=kind.equals("T")?WebReply.encode(text,replyTo):text;
        if(kind.equals("Y")){admin.authorize(person);rooms.lay(room);if(replyTo!=0 || text.isBlank() || text.codePointCount(0,text.length())>200)throw new IllegalArgumentException("Thông báo cần 1–200 ký tự.");}
        else rooms.kiemTraGui(person, room);
        if (replyTo>0) rooms.tinNhan().theoId(room,replyTo);
        TinNhan old=rooms.tinNhan().theoMa(room,person.nguoi.id,clientMsgId);
        if(old!=null && old.thuHoi)workspace.checkRecalledRetry(old,kind,stored);
        boolean duplicate = old != null;
        if(kind.equals("F") && (old==null || !old.thuHoi))files.validate(room,person.nguoi.id,text,rooms.tinNhan().cacTep(),duplicate);
        if (!duplicate && rooms.mocTin(room) >= 10000) { throw new IllegalArgumentException("Phòng đã đạt giới hạn 10.000 tin của bộ lưu trữ hiện tại."); }
        if (!duplicate && !kind.equals("Y") && !moderation.choTin(person, System.currentTimeMillis())) {
            throw new IllegalArgumentException("Bạn gửi quá nhanh. Hãy chờ một chút.");
        }
        TinNhan saved = kind.equals("Y")?rooms.tinNhan().them(person,room,stored,clientMsgId,kind):rooms.themTin(person, room, stored, clientMsgId, kind);
        return new Accepted(saved, duplicate);
    }

    public synchronized void logout(String token) {
        Session s = sessions.remove(token);
        if (s != null) { users.dangXuat(s.person);admin.revoke(token);changed(); }
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
    public synchronized boolean administrator(String token) {
        try{var p=require(token,false);return p.nguoi.vaiTro==VaiTro.ADMIN && !p.nguoi.buocDoiMatKhau;}
        catch(IllegalArgumentException e){return false;}
    }
    public synchronized Map<String,Object> snapshot(String token) { return workspace.snapshot(require(token)); }
    public synchronized List<WebWorkspace.Member> members(String token,String room) { return workspace.members(require(token),room); }
    public synchronized List<WebWorkspace.Restriction> restrictions(String token,String room) { return workspace.restrictions(require(token),room); }
    public synchronized List<WebWorkspace.Profile> searchUsers(String token,String query) { return workspace.searchUsers(require(token),query); }
    public synchronized Object sentInvites(String token,String room) { return workspace.sent(require(token),room); }
    public synchronized byte[] avatar(String token,String id) throws IOException { return workspace.avatar(require(token),id); }
    public synchronized Object action(String token,String action,Map<String,String> args) throws IOException {
        var person=require(token);
        mutation(token);
        Object result=workspace.action(person,action,args);
        if(action.equals("room-delete"))files.removeRoom(args.get("roomId"));
        var notice=memberNotice(person,action,args,false);if(notice!=null)result=Map.of("status","ok","notice",notice);
        if(action.equals("member-moderate"))audit.ghi(person.nguoi.tenDangNhap,"WEB_ROOM_MODERATE",args.getOrDefault("roomId","")+":"+args.getOrDefault("userId",""),args.getOrDefault("kind","")+"; thời hạn giây="+args.getOrDefault("seconds","") );
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
    private QuanLyNguoiDung.Phien requireAdmin(String token) { var p=require(token);admin.authorize(p);return p; }
    public synchronized WebAdministration.Snapshot adminSnapshot(String token) throws IOException {
        requireAdmin(token);mutation(token);return admin.capture(token);
    }
    public synchronized WebAdministration.Snapshot adminReport(String token,String id) { requireAdmin(token);return admin.read(token,id); }
    public synchronized void auditExport(String token,String id,String format) throws IOException {
        var p=requireAdmin(token);mutation(token);admin.read(token,id);
        if(!Set.of("pdf","zip","html","audit").contains(format))throw new IllegalArgumentException("Định dạng báo cáo không hợp lệ.");
        audit.ghi(p.nguoi.tenDangNhap,"WEB_EXPORT_"+format.toUpperCase(Locale.ROOT),id,"Yêu cầu xuất bản dữ liệu đang xem");
    }
    public synchronized WebAdministration.Plan broadcastPlan(String token,String id,String text) throws IOException {
        var p=requireAdmin(token);mutation(token);var plan=admin.broadcast(token,id,text);
        audit.ghi(p.nguoi.tenDangNhap,"WEB_BROADCAST",id,"Yêu cầu gửi qua UDP tới "+plan.rooms().size()+" phòng");return plan;
    }
    private void revokeUser(String id) {
        var keys=sessions.entrySet().stream().filter(e->e.getValue().person.nguoi.id.equals(id)).map(Map.Entry::getKey).toList();
        for(String key:keys){logout(key);events.accept(new Event("session-revoked","",key));}
    }
    public synchronized Object adminAction(String token,String action,Map<String,String> args) throws IOException {
        var p=requireAdmin(token);mutation(token);String id=args.getOrDefault("id","");Object result;
        if(action.equals("user-delete")) {
            var user=users.lay(id);
            if(!user.tenDangNhap.equals(args.get("confirm")))throw new IllegalArgumentException("Tên tài khoản xác nhận chưa khớp.");
            users.khoa(p,id,true,"Xóa tài khoản");revokeUser(id);
            for(var room:rooms.tatCa()) {
                if(room.chuId.equals(id)) {
                    String owner=users.tatCa().stream().filter(u->!u.id.equals(id) && !users.biKhoa(u.id) && rooms.laThanhVien(u.id,room.id))
                            .min(Comparator.comparing(u->rooms.ngayVao(room.id,u.id))).map(u->u.id).orElse(p.nguoi.id);
                    rooms.ganChu(p,room.id,owner);admin.connectOwner(owner,room.id);
                }
                rooms.goThanhVien(room.id,id);
            }
            rooms.tinNhan().nguoiDaXoa(id);users.xoaQuanTri(p,id);audit.ghi(p.nguoi.tenDangNhap,"WEB_USER_DELETE",id,"Thành công");result=Map.of("status","ok");
        }else if(action.equals("room-delete")) {
            workspace.deleteAsAdmin(p,id);files.removeRoom(id);audit.ghi(p.nguoi.tenDangNhap,"WEB_ROOM_DELETE",id,"Thành công");result=Map.of("status","ok");
        }else if(action.equals("room-clear")) {
            rooms.lay(id);workspace.rememberRecalls(rooms.tinNhan().tatCa().stream().filter(t->t.roomId.equals(id) && !t.thuHoi).toList());
            rooms.tinNhan().xoaLichSu(id);files.removeRoom(id);events.accept(new Event("history-cleared",id,Map.of()));
            audit.ghi(p.nguoi.tenDangNhap,"WEB_ROOM_CLEAR",id,"Thành công");result=Map.of("status","ok");
        }else {
            String oldRole=action.equals("user-edit")?users.lay(id).vaiTro.name():"";
            result=admin.action(p,action,args);
            if(action.equals("user-reset") || (action.equals("user-lock") && "true".equals(args.get("locked"))) ||
                    (action.equals("user-edit") && !oldRole.equals(args.get("role"))))revokeUser(id);
        }
        var notice=memberNotice(p,action,args,true);if(notice!=null)result=Map.of("status","ok","notice",notice);changed();return result;
    }
    public synchronized String exportHtml(String token,String room) {
        workspace.read(require(token),room);
        return WebHistoryReport.html(rooms.lay(room).ten,require(token).nguoi.id,
                rooms.tinNhan().tatCa().stream().filter(t->t.roomId.equals(room)).map(this::message).toList());
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
