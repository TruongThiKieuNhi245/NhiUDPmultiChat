package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.io.IOException;
import java.util.*;

/** Caller giu khoa WebChatService; snapshot bat bien gan voi phien admin. */
public final class WebAdministration {
    private final QuanLyNguoiDung users;
    private final QuanLyPhong rooms;
    private final KiemDuyet moderation;
    private final WebFiles files;
    private final NhatKy audit;
    private final Map<String,Cached> snapshots=new LinkedHashMap<>();
    private final Map<String,Plan> broadcasts=new LinkedHashMap<>();
    public record Snapshot(String id,AnhChupQuanTri data) { }
    private record Cached(String token,Snapshot snapshot,long created) { }
    public record Plan(String token,String id,String text,List<String> rooms,long created) { }
    public static final class Forbidden extends RuntimeException {
        private static final long serialVersionUID=1L;
        public Forbidden() { super("Chỉ quản trị viên hệ thống được dùng chức năng này."); }
    }
    WebAdministration(QuanLyNguoiDung users,QuanLyPhong rooms,KiemDuyet moderation,WebFiles files,NhatKy audit) {
        this.users=users;this.rooms=rooms;this.moderation=moderation;this.files=files;this.audit=audit;
    }
    void authorize(QuanLyNguoiDung.Phien p) {
        users.kiemTraBatBuoc(p,Protocol.GUI_TIN);
        if(p.nguoi.vaiTro!=VaiTro.ADMIN)throw new Forbidden();
    }
    Snapshot capture(String token) throws IOException {
        expire();
        var owned=snapshots.entrySet().stream().filter(e->e.getValue().token().equals(token)).toList();
        if(owned.size()>=2)snapshots.remove(owned.getFirst().getKey());
        if(snapshots.size()>=8)throw new IllegalArgumentException("Đang có nhiều bản báo cáo. Hãy thử lại sau.");
        Set<String> live=rooms.tinNhan().cacTep();
        var source=ChupThongKe.chup(users,rooms,(room,user)->files.statistics(room,user,live),audit);
        var tables=new LinkedHashMap<>(source.bang());
        var stats=new ArrayList<>(source.lay("STATS"));
        long online=users.danhSach().stream().map(p->p.nguoi.id).distinct().count();
        stats.add(List.of("Ngoại tuyến",Long.toString(users.tatCa().stream().filter(u->!users.biKhoa(u.id)).count()-online)));
        tables.put("STATS",stats);
        tables.put("ROOMS",source.lay("ROOMS").stream().map(row->{var copy=new ArrayList<>(row);copy.add(Integer.toString(rooms.thanhVien(row.get(0)).size()));return List.copyOf(copy);}).toList());
        if(tables.values().stream().mapToLong(List::size).sum()>20000)throw new IllegalArgumentException("Báo cáo vượt giới hạn 20.000 dòng của bản lưu trữ hiện tại.");
        var snapshot=new Snapshot(UUID.randomUUID().toString(),new AnhChupQuanTri(source.luc(),tables));
        snapshots.put(snapshot.id(),new Cached(token,snapshot,System.currentTimeMillis()));return snapshot;
    }
    Snapshot read(String token,String id) {
        expire();var cached=snapshots.get(id);
        if(cached==null || !cached.token().equals(token))throw new IllegalArgumentException("Bản báo cáo đã hết hạn. Hãy làm mới dữ liệu.");
        return cached.snapshot();
    }
    void revoke(String token) { snapshots.values().removeIf(c->c.token().equals(token));broadcasts.values().removeIf(p->p.token().equals(token)); }
    private void expire() { long now=System.currentTimeMillis();snapshots.values().removeIf(c->now-c.created()>300000);broadcasts.values().removeIf(p->now-p.created()>120000); }
    Plan broadcast(String token,String id,String text) {
        expire();if(id==null || !id.matches("[A-Za-z0-9_-]{22}") || text==null || text.isBlank() || text.codePointCount(0,text.length())>200)throw new IllegalArgumentException("Thông báo cần 1–200 ký tự và mã yêu cầu hợp lệ.");
        String key=token+":"+id;var old=broadcasts.get(key);
        if(old!=null){if(!old.text().equals(text))throw new IllegalArgumentException("Mã thông báo đã dùng với nội dung khác.");return old;}
        if(broadcasts.size()>=16)broadcasts.remove(broadcasts.keySet().iterator().next());
        var plan=new Plan(token,id,text,rooms.tatCa().stream().map(r->r.id).toList(),System.currentTimeMillis());broadcasts.put(key,plan);return plan;
    }
    Object action(QuanLyNguoiDung.Phien p,String action,Map<String,String> a) throws IOException {
        String id=value(a,"id");Object result=Map.of("status","ok");
        switch(action) {
            case "user-add":
                if(users.tongNguoiDung()>=1000)throw new IllegalArgumentException("Đã đủ 1000 tài khoản.");
                result=Map.of("id",users.taoQuanTri(p,value(a,"login"),value(a,"name"),value(a,"role"),value(a,"password")).id);break;
            case "user-edit":users.suaQuanTri(p,id,value(a,"name"),value(a,"about"),value(a,"role"));break;
            case "user-lock":users.khoa(p,id,bool(a,"locked"),value(a,"reason"));break;
            case "user-reset":result=Map.of("temporaryPassword",users.datLai(p,id));break;
            case "room-create": {
                String owner=value(a,"ownerId");if(owner.isBlank())owner=p.nguoi.id;users.lay(owner);if(users.biKhoa(owner))throw new IllegalArgumentException("Không chọn tài khoản bị khóa làm chủ phòng.");
                var room=rooms.tao(p,value(a,"name"),value(a,"description"),bool(a,"publicRoom"),value(a,"password"),number(a,"capacity",2,500));
                rooms.ganChu(p,room.id,owner);connectOwner(owner,room.id);connectOwner(p.nguoi.id,room.id);result=Map.of("roomId",room.id);break;
            }
            case "room-edit":rooms.sua(p,id,value(a,"name"),value(a,"description"),bool(a,"publicRoom"),bool(a,"keepPassword")?null:value(a,"password"),number(a,"capacity",2,500));break;
            case "room-state":rooms.dongMo(p,id,bool(a,"open"));break;
            case "room-transfer":checkOwner(value(a,"userId"),id);rooms.ganChu(p,id,value(a,"userId"));connectOwner(value(a,"userId"),id);break;
            case "member-moderate":moderate(p,a);break;
            default:throw new IllegalArgumentException("Thao tác quản trị không hợp lệ.");
        }
        audit.ghi(p.nguoi.tenDangNhap,"WEB_"+action.toUpperCase(Locale.ROOT),id,"Thành công; không ghi mật khẩu/nội dung chat");return result;
    }
    private void checkOwner(String user,String room) {
        users.lay(user);var target=rooms.lay(room);
        if(users.biKhoa(user) || moderation.den(room,user,"BAN")>0)throw new IllegalArgumentException("Không chọn tài khoản bị khóa hoặc đang bị cấm vào phòng làm chủ.");
        if(!rooms.laThanhVien(user,room) && rooms.soThanhVienBenVung(room)>=target.gioiHan)throw new IllegalArgumentException("Phòng đã đủ thành viên. Hãy chọn một thành viên hiện có.");
    }
    void connectOwner(String user,String room) throws IOException {
        for(var session:users.danhSach())if(session.nguoi.id.equals(user) && !session.nguoi.buocDoiMatKhau){rooms.datCho(session,room,"");rooms.xacNhanVao(session,room);}
    }
    void moderate(QuanLyNguoiDung.Phien p,Map<String,String> a) throws IOException {
        String scope=value(a,"roomId"),id=value(a,"userId"),kind=value(a,"kind");
        if(value(a,"reason").strip().length()>200)throw new IllegalArgumentException("Lý do tối đa 200 ký tự.");
        var target=users.lay(id);var room=scope.equals("*")?null:rooms.lay(scope);moderation.quyen(p,target,room);
        if(room==null && !kind.equals("MUTE"))throw new IllegalArgumentException("Chọn phòng cho thao tác này.");
        if((kind.equals("BAN") || kind.equals("KICK")) && scope.equals("sanh"))throw new IllegalArgumentException("Không đuổi/cấm vào Sảnh chung; hãy dùng khóa tài khoản hoặc cấm chat.");
        if(kind.equals("MUTE") || kind.equals("BAN")) {
            int duration=number(a,"seconds",-1,31536000);if(duration==0)throw new IllegalArgumentException("Chọn thời hạn ít nhất 1 giây hoặc -1 để gỡ.");
            String reason=value(a,"reason").strip();if(reason.length()>200)throw new IllegalArgumentException("Lý do tối đa 200 ký tự.");
            moderation.dat(scope,id,kind,duration,reason);if(kind.equals("MUTE") || duration<0)return;
        }else if(!kind.equals("KICK"))throw new IllegalArgumentException("Chọn cấm chat, cấm vào hoặc đuổi thành viên.");
        boolean replaceOwner=room.chuId.equals(id);
        if(replaceOwner){rooms.ganChu(p,scope,p.nguoi.id);connectOwner(p.nguoi.id,scope);}
        rooms.goThanhVien(scope,id);for(var session:users.danhSach()){if(session.nguoi.id.equals(id)){session.phong.remove(scope);session.choVao.remove(scope);}}
    }
    static String value(Map<String,String> a,String key) { return a.getOrDefault(key,""); }
    static boolean bool(Map<String,String> a,String key) { String v=value(a,key);if(!Set.of("true","false").contains(v))throw new IllegalArgumentException("Giá trị bật/tắt không hợp lệ.");return Boolean.parseBoolean(v); }
    static int number(Map<String,String> a,String key,int min,int max) { try{int n=Integer.parseInt(value(a,key));if(n<min || n>max)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("Giá trị số không hợp lệ: "+key); } }
}
