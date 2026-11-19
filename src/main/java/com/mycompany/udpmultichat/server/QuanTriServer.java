package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Vai tro: Xac thuc lenh admin/moderation, snapshot va job bao cao. Ai goi: ServerCore. Goi: QuanLyNguoiDung, KiemDuyet, XuatBaoCao. */
final class QuanTriServer implements AutoCloseable {
    private final ServerCore s; final NhatKy audit; final KiemDuyet duyet;
    private final Map<String,BanChup> chup=new HashMap<>(); private final Map<String,CongViec> jobs=new HashMap<>();
    private final Map<String,Integer> sai=new HashMap<>();
    private final Map<String,TraLoiLenh> daXuLy=new LinkedHashMap<>();
    private final ExecutorService nen=Executors.newSingleThreadExecutor(r -> { Thread t=new Thread(r,"bao-cao"); t.setDaemon(true); return t; });
    private final Path reports;
    // Cac ham xu ly duoc Core goi tren cung khoa, chi worker ghi bao cao ra ngoai khoa.
    QuanTriServer(ServerCore s,KhoLuuTru kho,Path data) throws IOException {
        this.s=s; reports=data.toAbsolutePath().getParent().resolve("reports"); audit=new NhatKy(data,5*1024*1024); duyet=new KiemDuyet(kho,audit);
        s.phong.kiemDuyet(duyet::kiemTraGui,duyet::kiemTraVao);
    }
    // ===== QUYEN ADMIN VA REQUEST =====
    String[] xuLy(Phien actor,String[] g) throws IOException {
        donCache();
        String khoa=actor.token+":"+g[1]; TraLoiLenh cu=daXuLy.get(khoa);
        if (cu!=null) {
            if (!Arrays.equals(cu.lenh,g)) { throw new IllegalArgumentException("Mã yêu cầu đã được sử dụng."); }
            if (cu.loi==null && g[0].startsWith("AD_") && actor.nguoi.vaiTro!=VaiTro.ADMIN) {
                audit.ghi(actor.nguoi.tenDangNhap,"DENIED",g[0],"Quyền ADMIN đã thay đổi");
                throw new IllegalArgumentException("Chỉ quản trị viên hệ thống được dùng chức năng này.");
            }
            if (cu.loi!=null) { throw new IllegalArgumentException(cu.loi); }
            return cu.ketQua.clone();
        }
        try {
            String[] k=thucHien(actor,g);
            ghiKetQua(khoa,new TraLoiLenh(g.clone(),k.clone(),null,System.currentTimeMillis())); return k;
        } catch (IllegalArgumentException e) {
            ghiKetQua(khoa,new TraLoiLenh(g.clone(),null,e.getMessage(),System.currentTimeMillis())); throw e;
        }
    }
    // Chi vao day mot lan moi request/token, loi I/O khong duoc coi la da hoan tat.
    private String[] thucHien(Phien actor,String[] g) throws IOException {
        if (g[0].equals(Protocol.MOD) || g[0].equals(Protocol.MOD_LIST)) { return moderation(actor,g); }
        if (actor.nguoi.vaiTro!=VaiTro.ADMIN) {
            audit.ghi(actor.nguoi.tenDangNhap,"DENIED",g[0],"Không có quyền ADMIN"); throw new IllegalArgumentException("Chỉ quản trị viên hệ thống được dùng chức năng này.");
        }
        donCache();
        return switch (g[0]) {
            case Protocol.AD_SNAPSHOT -> snapshot(actor);
            case Protocol.AD_READ -> doc(actor,g);
            case Protocol.AD_ACTION -> thaoTac(actor,g);
            case Protocol.AD_EXPORT -> xuat(actor,g.length>3 && g[3].equals("AUDIT"));
            case Protocol.AD_JOB -> { du(g,4); yield job(actor,g[3]); }
            default -> throw new IllegalArgumentException("Lệnh quản trị không hợp lệ.");
        };
    }
    // Mot snapshot cho toan bo nam tab, khong boc salt/hash.
    private String[] snapshot(Phien p) throws IOException {
        if (chup.size()>=32) { throw new IllegalArgumentException("Đang có nhiều phiên xem quản trị. Hãy thử lại."); }
        String id=UUID.randomUUID().toString(); AnhChupQuanTri a=ChupThongKe.chup(s.nguoiDung,s.phong,s.tep,audit); chup.put(id,new BanChup(p.token,a)); return new String[]{id,Long.toString(a.luc())};
    }
    // Mot hang/response duoi MTU, snapshot gan token de khong lo du lieu giua phien.
    private String[] doc(Phien p,String[] g) {
        if (g.length!=6) { throw new IllegalArgumentException("Yêu cầu bảng không hợp lệ."); }
        BanChup a=chup.get(g[3]); if (a==null || !a.token.equals(p.token)) { throw new IllegalArgumentException("Snapshot đã hết hạn. Hãy tải lại."); }
        a.lanDoc=System.currentTimeMillis();
        List<List<String>> ds=a.a.lay(g[4]); int i=Integer.parseInt(g[5]); if (i<0 || i>ds.size()) { throw new IllegalArgumentException("Vị trí dòng không hợp lệ."); }
        return Goi.noi(new String[]{Integer.toString(ds.size())},i==ds.size()?new String[0]:ds.get(i).toArray(String[]::new));
    }
    // ===== CRUD TAI KHOAN / PHONG =====
    private String[] thaoTac(Phien p,String[] g) throws IOException {
        if (g.length<5) { throw new IllegalArgumentException("Thiếu thao tác hoặc đối tượng."); }
        String lenh=g[3],id=g[4],ketQua="Đã cập nhật.";
        switch (lenh) {
            case "USER_ADD": du(g,8); ketQua=s.nguoiDung.taoQuanTri(p,id,g[5],g[6],g[7]).id; break;
            case "USER_EDIT": du(g,8); s.nguoiDung.suaQuanTri(p,id,g[5],g[6],g[7]); phatHoSo(id); break;
            case "USER_LOCK": du(g,7); s.nguoiDung.khoa(p,id,bool(g[5]),lyDo(g[6])); if (bool(g[5])) { ngat(id,"Tài khoản đã bị khóa."); } break;
            case "USER_RESET": du(g,5); ketQua=s.nguoiDung.datLai(p,id); ngat(id,"Mật khẩu đã được đặt lại. Hãy đăng nhập bằng mật khẩu tạm."); break;
            case "USER_DELETE": du(g,6); if (!s.nguoiDung.lay(id).tenDangNhap.equals(g[5])) { throw new IllegalArgumentException("Tên xác nhận chưa khớp."); } xoaUser(p,id); break;
            case "ROOM_CREATE": du(g,10); s.nguoiDung.lay(id); Phong moi=s.phong.tao(p,g[5],g[6],bool(g[7]),g[8],Integer.parseInt(g[9])); if (!id.equals(p.nguoi.id)) { s.nguoiDung.lay(id); s.phong.ganChu(p,moi.id,id); } ketQua=moi.id; break;
            case "ROOM_EDIT": du(g,11); if (!Set.of("KEEP","SET","REMOVE").contains(g[8])) { throw new IllegalArgumentException("Chế độ mật khẩu không hợp lệ."); } s.phong.sua(p,id,g[5],g[6],bool(g[7]),g[8].equals("KEEP")?null:g[8].equals("REMOVE")?"":g[9],Integer.parseInt(g[10])); break;
            case "ROOM_STATE": du(g,6); s.phong.dongMo(p,id,bool(g[5])); break;
            case "ROOM_DELETE": du(g,5); s.lenhPhong.xoaPhong(p,id); break;
            case "ROOM_CLEAR": du(g,5); s.phong.lay(id); s.tep.xoaPhong(id); s.phong.tinNhan().xoaLichSu(id); s.thongBaoPhong(id,"Quản trị viên đã xóa nội dung lịch sử."); break;
            case "BROADCAST": du(g,5); ketQua=thongBao(p,id); break;
            default: throw new IllegalArgumentException("Thao tác quản trị không hợp lệ.");
        }
        audit.ghi(p.nguoi.tenDangNhap,lenh,id,lenh.equals("USER_RESET")?"Đã cấp mật khẩu tạm, không ghi mật khẩu vào audit":"Thành công");
        thayDoi(); return new String[]{ketQua};
    }
    // Dang xuat/timeout khac voi ngat cuong che: Client khong duoc tu reconnect.
    private void ngat(String id,String lyDo) {
        for (Phien p:List.copyOf(s.nguoiDung.danhSach())) {
            if (p.nguoi.id.equals(id)) { s.guiSuKien(p,Goi.ghep(Protocol.FORCE_OUT,lyDo)); s.nguoiDung.dangXuat(p); s.roiTatCa(p,lyDo); }
        }
    }
    // Chuyen phong cua nguoi da xoa cho thanh vien vao som nhat, du phong actor admin.
    private void xoaUser(Phien p,String id) throws IOException {
        s.nguoiDung.khoa(p,id,true,"Xóa tài khoản");
        for (Phong r:s.phong.tatCa()) {
            if (r.chuId.equals(id)) {
                String chu=s.nguoiDung.tatCa().stream().filter(n -> !n.id.equals(id) && s.phong.laThanhVien(n.id,r.id) && !s.nguoiDung.biKhoa(n.id)).min(Comparator.comparing(n -> s.phong.ngayVao(r.id,n.id))).map(n -> n.id).orElse(p.nguoi.id);
                s.phong.ganChu(p,r.id,chu);
            }
            s.phong.goThanhVien(r.id,id);
        }
        ngat(id,"Tài khoản đã bị xóa."); s.phong.tinNhan().nguoiDaXoa(id); s.nguoiDung.xoaQuanTri(p,id);
        s.nguoiDung.danhSach().forEach(n -> s.guiSuKien(n,Goi.ghep(Protocol.USER_DELETED,id)));
    }
    // Profile/role moi gui cho cac Client online, khong tai avatar trong EDT.
    private void phatHoSo(String id) {
        NguoiDung n=s.nguoiDung.lay(id); String goi=Goi.ghep(Goi.noi(new String[]{Protocol.HO_SO_MOI},n.cacTruong())); s.nguoiDung.danhSach().forEach(p -> s.guiSuKien(p,goi));
    }
    // Broadcast duoc luu o moi phong, ke ca phong dang dong/rieng tu.
    private String thongBao(Phien p,String text) throws IOException {
        if (text.isBlank() || text.codePointCount(0,text.length())>200) { throw new IllegalArgumentException("Thông báo cần 1–200 ký tự."); }
        int daLuu=0,loi=0;
        for (Phong r:s.phong.tatCa()) {
            TinNhan t;
            try { t=s.phong.tinNhan().them(p,r.id,text,TinNhan.taoMa(),"Y"); daLuu++; }
            catch (IOException e) { loi++; s.listener.onLog("Không lưu được broadcast ở "+r.ten+": "+e.getMessage()); continue; }
            try { s.phatMulticast(r.id,t.dongGoi()); }
            catch (IOException e) { s.listener.onLog("Broadcast đã lưu; Client sẽ RESYNC ở "+r.ten); }
        }
        s.nguoiDung.danhSach().forEach(n -> s.guiSuKien(n,Goi.ghep(Protocol.ANNOUNCE,text)));
        String k=loi==0?"Đã gửi thông báo.":"Đã thông báo trực tuyến; chỉ lưu được "+daLuu+" phòng, "+loi+" phòng lỗi lưu. Xem log Server.";
        if (loi>0) { audit.ghi(p.nguoi.tenDangNhap,"BROADCAST_PARTIAL","rooms",k); }
        return k;
    }
    // ===== KIEM DUYET PHONG / TOAN HE THONG =====
    private String[] moderation(Phien p,String[] g) throws IOException {
        if (g[0].equals(Protocol.MOD_LIST)) { return thanhVien(p,g); }
        du(g,8); String lenh=g[3],room=g[4],id=g[5]; NguoiDung n=s.nguoiDung.lay(id);
        Phong r=room.equals("*")?null:s.phong.lay(room);
        try { duyet.quyen(p,n,r); }
        catch (IllegalArgumentException e) { audit.ghi(p.nguoi.tenDangNhap,"DENIED","MOD:"+lenh+":"+room+":"+id,e.getMessage()); throw e; }
        if (r==null && !g[3].equals("MUTE")) { throw new IllegalArgumentException("Hãy chọn phòng."); } long giay=Long.parseLong(g[6]); String lyDo=lyDo(g[7]);
        switch (lenh) {
            case "MUTE": case "BAN": duyet.dat(room,id,lenh,giay,lyDo); if (lenh.equals("BAN") && giay>=0) { kick(p,r,id,"Bạn bị cấm vào phòng."); } break;
            case "KICK": if (r==null) { throw new IllegalArgumentException("Hãy chọn phòng."); } kick(p,r,id,"Bạn đã bị mời ra khỏi phòng."); break;
            case "TRANSFER": if (r==null) { throw new IllegalArgumentException("Hãy chọn phòng."); } s.phong.ganChu(p,room,id); break;
            default: throw new IllegalArgumentException("Thao tác kiểm duyệt không hợp lệ.");
        }
        audit.ghi(p.nguoi.tenDangNhap,lenh,id,room+" · "+lyDo); phatMute(); thayDoi(); return new String[]{"Đã cập nhật kiểm duyệt."};
    }
    // Kick nguoi offline van xoa tu cach thanh vien ben vung.
    private void kick(Phien actor,Phong r,String id,String thongBao) throws IOException {
        if (r==null) { throw new IllegalArgumentException("Hãy chọn phòng."); }
        if (r.chuId.equals(id)) { s.phong.ganChu(actor,r.id,actor.nguoi.id); }
        s.phong.goThanhVien(r.id,id);
        for (Phien n:s.nguoiDung.danhSach()) { if (n.nguoi.id.equals(id)) { s.phong.roi(n,r.id); s.guiSuKien(n,Goi.ghep(Protocol.DA_ROI_PHONG,r.id,thongBao)); } }
        s.thongBaoPhong(r.id,s.nguoiDung.lay(id).tenHienThi+" đã bị mời ra khỏi phòng.");
    }
    // Chu phong duoc doc danh sach phuc vu go ban/mute; admin duoc xem moi phong.
    private String[] thanhVien(Phien p,String[] g) throws IOException {
        du(g,5); Phong r=s.phong.lay(g[3]);
        if (!QuanLyPhong.duocQuanLy(p.nguoi,r)) {
            audit.ghi(p.nguoi.tenDangNhap,"DENIED","MOD_LIST:"+r.id,"Không có quyền xem kiểm duyệt phòng.");
            throw new IllegalArgumentException("Không có quyền xem kiểm duyệt phòng.");
        }
        int i=Integer.parseInt(g[4]); var ds=s.nguoiDung.tatCa(); if (i<0 || i>ds.size()) { throw new IllegalArgumentException("Dòng không hợp lệ."); }
        if (i==ds.size()) { return new String[]{""+ds.size()}; }
        NguoiDung n=ds.get(i); return new String[]{""+ds.size(),n.id,n.tenDangNhap,n.tenHienThi,n.vaiTro.name(),""+s.phong.laThanhVien(n.id,r.id),""+duyet.mute(r.id,n.id),""+duyet.den(r.id,n.id,"BAN")};
    }
    // State mute den Client qua unicast tin cay, heartbeat gui lai khi bi lo.
    void phatMute() { for (Phien p:s.nguoiDung.danhSach()) { for (String room:p.phong) { s.guiSuKien(p,Goi.ghep(Protocol.MUTE_STATE,room,Long.toString(duyet.mute(room,p.nguoi.id)))); } } }
    // Heartbeat chi bu trang thai cho phien vua PING, tranh phat N lan cho moi user.
    void phatMute(Phien p) {
        for (String room:p.phong) { s.guiSuKien(p,Goi.ghep(Protocol.MUTE_STATE,room,Long.toString(duyet.mute(room,p.nguoi.id)))); }
    }
    // Moi thao tac phat revision, UI admin tu tai lai, khong bat bam Lam moi.
    void thayDoi() {
        s.capNhatPhong(); for (Phong r:s.phong.tatCa()) { s.capNhatThanhVien(r.id); }
    }
    // ===== JOB BAO CAO =====
    private String[] xuat(Phien p,boolean auditOnly) throws IOException {
        if (jobs.size()>=8) { throw new IllegalArgumentException("Đang có nhiều báo cáo. Hãy thử lại sau."); }
        AnhChupQuanTri a=ChupThongKe.chup(s.nguoiDung,s.phong,s.tep,audit); String id=UUID.randomUUID().toString();
        jobs.put(id,new CongViec(p.token,CompletableFuture.supplyAsync(() -> { try { return XuatBaoCao.nen(XuatBaoCao.xuat(a,reports,auditOnly)); } catch (IOException e) { throw new CompletionException(e); } },nen)));
        audit.ghi(p.nguoi.tenDangNhap,auditOnly?"EXPORT_AUDIT":"EXPORT_REPORT","reports","Đã bắt đầu từ snapshot"); return new String[]{id};
    }
    // Poll job chi tra fileId khi hoan tat; ZIP van duoc tai bang cua so UDP M4.
    private String[] job(Phien p,String id) throws IOException {
        CongViec j=jobs.get(id); if (j==null || !j.token.equals(p.token)) { throw new IllegalArgumentException("Không tìm thấy tác vụ báo cáo."); }
        if (!j.f.isDone()) { return new String[]{"WAIT"}; }
        try { if (j.tep==null) { j.tep=s.tep.dangKyBaoCao(p,j.f.join()); } return new String[]{"DONE",j.tep.dongGoi()}; }
        catch (CompletionException e) { throw new IOException("Không tạo được báo cáo.",e.getCause()); }
    }
    // Nut Server cung dung snapshot nhu admin, khong can mo Client.
    CompletableFuture<Path> xuatTrenServer() throws IOException {
        AnhChupQuanTri a=ChupThongKe.chup(s.nguoiDung,s.phong,s.tep,audit); audit.ghi("SERVER","EXPORT_REPORT","reports","Xuất từ cửa sổ Server");
        return CompletableFuture.supplyAsync(() -> { try { return XuatBaoCao.xuat(a,reports,false); } catch (IOException e) { throw new CompletionException(e); } },nen);
    }
    // Bao mat: ghi moi dot 3 lan dang nhap sai, khong ghi mat khau.
    void dangNhapSai(String diaChi) throws IOException { int n=sai.merge(diaChi,1,Integer::sum); if (n%3==0) { audit.ghi("ANONYMOUS","LOGIN_FAILURE",diaChi,"3 lần đăng nhập không thành công"); } if (sai.size()>1000) { sai.clear(); } }
    // Quet han va phat lai mute; khong ghi audit lap lai cho cung mot lenh.
    void quet() throws IOException { duyet.quet(); donCache(); }
    // Cache snapshot 30s, job 10 phut, co gioi han so luong.
    private void donCache() {
        long now=System.currentTimeMillis(); chup.values().removeIf(c -> now-c.lanDoc>30000);
        jobs.values().removeIf(j -> now-j.luc>600000); daXuLy.values().removeIf(k -> now-k.luc>120000);
    }
    // Chi giu ket qua toi da 512 lenh co tac dung phu; doc bang khong can cache lap.
    private void ghiKetQua(String khoa,TraLoiLenh k) {
        if (!Set.of(Protocol.AD_ACTION,Protocol.MOD).contains(k.lenh[0])) { return; }
        if (daXuLy.size()>=512) { daXuLy.remove(daXuLy.keySet().iterator().next()); }
        daXuLy.put(khoa,k);
    }
    private record TraLoiLenh(String[] lenh,String[] ketQua,String loi,long luc) { }
    // Gioi han ly do de goi tin van nho hon MTU.
    private static String lyDo(String s) { if (s.codePointCount(0,s.length())>200) { throw new IllegalArgumentException("Lý do tối đa 200 ký tự."); } return s; }
    // Kiem tra truong truoc khi doc mang.
    private static void du(String[] g,int n) { if (g.length!=n) { throw new IllegalArgumentException("Thông tin thao tác không hợp lệ."); } }
    // Boolean gui tu Client phai dung chinh xac.
    private static boolean bool(String b) { if (!b.equals("true") && !b.equals("false")) { throw new IllegalArgumentException("Giá trị không hợp lệ."); } return Boolean.parseBoolean(b); }
    // Dong worker bao cao, khong de tai lieu dang ghi tiep sau khi dung Server.
    @Override public void close() { nen.shutdownNow(); chup.clear(); jobs.clear(); daXuLy.clear(); }
    private static final class BanChup {
        final String token; final AnhChupQuanTri a; long lanDoc=System.currentTimeMillis();
        // Snapshot bat bien, han tinh tu lan doc cuoi de mang cham van tai duoc bang.
        BanChup(String token,AnhChupQuanTri a) { this.token=token; this.a=a; }
    }
    private static final class CongViec {
        final String token; final CompletableFuture<Path> f; final long luc=System.currentTimeMillis(); TepTin tep;
        // Gan job voi phien da xac thuc.
        CongViec(String token,CompletableFuture<Path> f) { this.token=token; this.f=f; }
    }
}
