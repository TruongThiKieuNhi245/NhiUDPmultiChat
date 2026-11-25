package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.util.*;

/** Chay duoi khoa WebChatService; khong ghi tin chat moi tu HTTP. */
public final class WebWorkspace {
    private final KhoLuuTru disk;
    private final QuanLyNguoiDung users;
    private final QuanLyPhong rooms;
    private final KiemDuyet moderation;
    private final QuanLyLoiMoi invites;
    private final Properties preferences;
    private final Properties recallDigests;

    WebWorkspace(KhoLuuTru disk, QuanLyNguoiDung users, QuanLyPhong rooms, KiemDuyet moderation) throws IOException {
        this.disk=disk; this.users=users; this.rooms=rooms; this.moderation=moderation;
        invites=new QuanLyLoiMoi(disk); preferences=disk.docThuocTinh("web-preferences"); recallDigests=disk.docThuocTinh("web-recall-digests");
    }
    public record Profile(String id, String login, String name, String about, String avatarHash, String role, boolean forcePassword) { }
    public record Room(String id, String name, String description, String code, String ownerId, int capacity,
                       boolean publicRoom, boolean password, boolean open, boolean joined, boolean manage,
                       int memberCount, int onlineCount, long lastMsgId, long mutedUntil, boolean notificationsMuted) { }
    public record Member(String id, String login, String name, String nickname, String role, boolean online, String avatarHash) { }
    public Profile profile(NguoiDung u) {
        return new Profile(u.id,u.tenDangNhap,u.tenHienThi,u.gioiThieu,u.anhHash,u.vaiTro.name(),u.buocDoiMatKhau);
    }
    public Map<String,Object> snapshot(Phien p) {
        if (p.nguoi.buocDoiMatKhau) { return Map.of("profile",profile(p.nguoi),"rooms",List.of(),"invitations",List.of()); }
        List<Room> list=rooms.danhSachCho(p).stream().map(r -> {
            boolean joined=rooms.laThanhVien(p.nguoi.id,r.id), manage=QuanLyPhong.duocQuanLy(p.nguoi,r);
            return new Room(r.id,r.ten,r.moTa,joined||manage?r.maPhong:"",r.chuId,r.gioiHan,r.congKhai,r.coMatKhau,r.dangMo,
                    joined,manage,rooms.soThanhVienBenVung(r.id),rooms.thanhVien(r.id).size(),
                    joined?rooms.mocTin(r.id):0,moderation.mute(r.id,p.nguoi.id),
                    Boolean.parseBoolean(preferences.getProperty(r.id+"."+p.nguoi.id,"false")));
        }).toList();
        return Map.of("profile",profile(p.nguoi),"rooms",list,"invitations",invites.danhSachLoiMoiCho(p.nguoi.id,rooms,users));
    }
    void restore(Phien p) throws IOException {
        if (p.nguoi.buocDoiMatKhau) return;
        p.sanSang=true;
        rooms.datCho(p,"sanh",""); rooms.xacNhanVao(p,"sanh");
        for (var r:rooms.danhSachCho(p)) {
            if (!rooms.laThanhVien(p.nguoi.id,r.id)) continue;
            try { rooms.datCho(p,r.id,""); rooms.xacNhanVao(p,r.id); }
            catch (IllegalArgumentException ignored) { /* Ban van co hieu luc sau restart. */ }
        }
    }
    void read(Phien p,String room) {
        users.kiemTraBatBuoc(p,Protocol.GUI_TIN);
        rooms.kiemTraThanhVien(p,room); moderation.kiemTraVao(p,room);
    }
    public List<Member> members(Phien p,String room) {
        read(p,room);
        Set<String> online=new HashSet<>(); rooms.thanhVien(room).forEach(person -> online.add(person.nguoi.id));
        return users.tatCa().stream().filter(u -> rooms.laThanhVien(u.id,room)).map(u -> new Member(u.id,u.tenDangNhap,
                u.tenHienThi,rooms.layBietDanh(room,u.id),u.vaiTro.name(),online.contains(u.id),u.anhHash)).toList();
    }
    public List<Profile> searchUsers(Phien p,String query) {
        users.kiemTraBatBuoc(p,Protocol.GUI_TIN);
        if (query.length()<2 || query.length()>100) return List.of();
        return users.timKiem(query).stream().map(this::profile).toList();
    }
    public List<QuanLyLoiMoi.BanGhiLoiMoi> sent(Phien p,String room) {
        read(p,room); manage(p,room); return invites.danhSachTheoPhong(room);
    }
    private void manage(Phien p,String room) {
        if (!QuanLyPhong.duocQuanLy(p.nguoi,rooms.lay(room))) throw new IllegalArgumentException("Bạn không có quyền quản lý phòng.");
    }
    public byte[] avatar(Phien p,String id) throws IOException {
        var target=users.lay(id);
        boolean shared=p.nguoi.id.equals(id) || rooms.danhSachCho(p).stream().anyMatch(r ->
                p.phong.contains(r.id) && rooms.laThanhVien(id,r.id) && moderation.den(r.id,p.nguoi.id,"BAN")==0);
        if (!shared) throw new IllegalArgumentException("Bạn không có quyền xem ảnh này.");
        return disk.docAnh(target.tenDangNhap);
    }
    public Object action(Phien p,String action,Map<String,String> args) throws IOException {
        if (action.equals("password-change")) {
            users.doiMatKhau(p,value(args,"current"),value(args,"next")); restore(p); return profile(p.nguoi);
        }
        users.kiemTraBatBuoc(p,Protocol.GUI_TIN);
        String room=value(args,"roomId");
        switch (action) {
            case "room-create": {
                var r=rooms.tao(p,value(args,"name"),value(args,"description"),bool(args,"publicRoom"),value(args,"password"),number(args,"capacity",2,500));
                rooms.datCho(p,r.id,""); rooms.xacNhanVao(p,r.id); return Map.of("roomId",r.id);
            }
            case "room-edit":
                read(p,room);
                rooms.sua(p,room,value(args,"name"),value(args,"description"),bool(args,"publicRoom"),
                        bool(args,"keepPassword")?null:value(args,"password"),number(args,"capacity",2,500)); break;
            case "room-state": read(p,room); rooms.dongMo(p,room,bool(args,"open")); break;
            case "room-delete":
                read(p,room); manage(p,room); invites.xoaTheoPhong(room); rooms.xoa(p,room);
                for (var session:users.danhSach()) { session.phong.remove(room); session.choVao.remove(room); } break;
            case "room-join": {
                var r=rooms.datCho(p,value(args,"key"),value(args,"password")); rooms.xacNhanVao(p,r.id); return Map.of("roomId",r.id);
            }
            case "room-leave": read(p,room); rooms.roiChuDong(p,room); break;
            case "room-transfer": read(p,room); rooms.chuyenChu(p,room,value(args,"userId")); break;
            case "member-nickname":
                read(p,room); rooms.datBietDanh(p,room,value(args,"userId"),value(args,"nickname"),users); break;
            case "member-moderate": moderate(p,room,args); break;
            case "invite-send": {
                read(p,room); invites.donDepHetHan();
                String user=value(args,"userId");
                if (moderation.den(room,user,"BAN")>0) throw new IllegalArgumentException("Người này đang bị cấm vào phòng.");
                if(invites.danhSachTheoPhong(room).size()>=128)throw new IllegalArgumentException("Phòng đã đủ 128 lời mời đang chờ.");
                return invites.guiLoiMoi(p,room,user,rooms,users);
            }
            case "invite-answer": {
                String id=value(args,"inviteId"); boolean accept=bool(args,"accept");
                if (accept) {
                    var invitation=invites.danhSachLoiMoiCho(p.nguoi.id,rooms,users).stream().filter(i -> i.inviteId().equals(id))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("Lời mời không còn hiệu lực."));
                    var author=users.lay(invitation.nguoiMoiId());
                    if (users.biKhoa(author.id) || !QuanLyPhong.duocQuanLy(author,rooms.lay(invitation.roomId())))
                        throw new IllegalArgumentException("Người mời không còn quyền quản lý phòng.");
                }
                var invitation=invites.traLoiLoiMoi(p,id,accept,rooms);
                if (accept) { rooms.datCho(p,invitation.roomId(),""); rooms.xacNhanVao(p,invitation.roomId()); }
                return Map.of("roomId",invitation.roomId(),"accepted",accept);
            }
            case "invite-cancel": invites.huyLoiMoi(p,value(args,"inviteId"),rooms); break;
            case "profile-save": return saveProfile(p,args);
            case "notifications":
                read(p,room); Properties old=(Properties)preferences.clone();
                preferences.setProperty(room+"."+p.nguoi.id,Boolean.toString(bool(args,"muted")));
                try { disk.ghiThuocTinh("web-preferences",preferences); }
                catch (IOException e) { preferences.clear(); preferences.putAll(old); throw e; } break;
            default: throw new IllegalArgumentException("Thao tác không được hỗ trợ.");
        }
        return Map.of("status","ok");
    }
    private Profile saveProfile(Phien p,Map<String,String> args) throws IOException {
        String mode=value(args,"avatarMode"), hash=p.nguoi.anhHash;
        if (!Set.of("KEEP","DELETE","SET").contains(mode)) throw new IllegalArgumentException("Chọn thao tác ảnh hợp lệ.");
        byte[] image=null;
        if (mode.equals("DELETE")) image=new byte[0];
        if (mode.equals("SET")) {
            String encoded=value(args,"avatar");
            if (encoded.length()>88000) throw new IllegalArgumentException("Avatar quá lớn.");
            image=Base64.getDecoder().decode(encoded); PhanManh.docAnh(image,true);
        }
        byte[] previous=image==null?null:disk.docAnh(p.nguoi.tenDangNhap);
        if (image!=null) { disk.ghiAnh(p.nguoi.tenDangNhap,image); hash=image.length==0?"":PhanManh.crc(image); }
        try { return profile(users.luuHoSo(p,value(args,"name"),value(args,"about"),hash)); }
        catch (IOException | RuntimeException e) {
            if (previous!=null) try { disk.ghiAnh(p.nguoi.tenDangNhap,previous); } catch (IOException rollback) { e.addSuppressed(rollback); }
            throw e;
        }
    }
    private void moderate(Phien p,String room,Map<String,String> args) throws IOException {
        read(p,room); var target=users.lay(value(args,"userId")); var r=rooms.lay(room);
        moderation.quyen(p,target,r);
        String kind=value(args,"kind");
        if (kind.equals("KICK")) {
            if (room.equals("sanh")) throw new IllegalArgumentException("Không đuổi khỏi Sảnh chung.");
        } else {
            if (!Set.of("MUTE","BAN").contains(kind)) throw new IllegalArgumentException("Thao tác kiểm duyệt không hợp lệ.");
            long duration=number(args,"seconds",-1,31536000);
            if (duration==0) throw new IllegalArgumentException("Chọn thời hạn từ 1 giây hoặc gỡ lệnh.");
            moderation.dat(room,target.id,kind,duration,"Kiểm duyệt phòng trên web");
            if (duration<0 || kind.equals("MUTE")) return;
        }
        rooms.goThanhVien(room,target.id);
        for (var session:users.danhSach()) if (session.nguoi.id.equals(target.id)) { session.phong.remove(room); session.choVao.remove(room); }
    }
    private String digest(String kind,String text) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest((kind+"\n"+text).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    void rememberRecall(TinNhan t) throws IOException {
        if (t.thuHoi || t.maClient.isEmpty()) return;
        String key=t.roomId+"."+t.userId+"."+t.maClient;
        String old=recallDigests.getProperty(key);
        recallDigests.setProperty(key,digest(t.loai,t.noiDung));
        try { disk.ghiThuocTinh("web-recall-digests",recallDigests); }
        catch (IOException e) { if(old==null)recallDigests.remove(key);else recallDigests.setProperty(key,old);throw e; }
    }
    void deleteAsAdmin(Phien p,String room) throws IOException {
        if(p.nguoi.vaiTro!=VaiTro.ADMIN)throw new WebAdministration.Forbidden();
        invites.xoaTheoPhong(room);rooms.xoa(p,room);
        for(var session:users.danhSach()){session.phong.remove(room);session.choVao.remove(room);}
    }
    void rememberRecalls(List<TinNhan> list) throws IOException {
        var old=(Properties)recallDigests.clone();
        for(var t:list)recallDigests.setProperty(t.roomId+"."+t.userId+"."+t.maClient,digest(t.loai,t.noiDung));
        try{disk.ghiThuocTinh("web-recall-digests",recallDigests);}
        catch(IOException e){recallDigests.clear();recallDigests.putAll(old);throw e;}
    }
    void checkRecalledRetry(TinNhan t,String kind,String text) {
        String expected=recallDigests.getProperty(t.roomId+"."+t.userId+"."+t.maClient);
        if (expected==null || !expected.equals(digest(kind,text))) throw new IllegalArgumentException("Mã tin đã thu hồi không khớp nội dung yêu cầu.");
    }
    public boolean typing(Phien p,String room) {
        rooms.kiemTraGui(p,room); return rooms.tinNhan().choTyping(room,p.nguoi.id,System.currentTimeMillis());
    }
    static String value(Map<String,String> args,String key) { return Objects.requireNonNullElse(args.get(key),""); }
    static boolean bool(Map<String,String> args,String key) {
        String value=value(args,key);
        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Giá trị bật/tắt không hợp lệ.");
        return Boolean.parseBoolean(value);
    }
    static int number(Map<String,String> args,String key,int min,int max) {
        try { int n=Integer.parseInt(value(args,key)); if (n>=min && n<=max) return n; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Giá trị số không hợp lệ.");
    }
}
