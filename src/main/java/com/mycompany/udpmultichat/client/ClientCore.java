package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.ReliableUdp;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Vai tro: API client khong Swing: unicast dieu khien, kenh va dong bo rieng cho tung phong. Ai goi: ChatMulticastUI, SelfTest. Goi: ReliableUdp, KenhPhong, DongBoTinNhan, Listener. */
public final class ClientCore implements AutoCloseable {
    private final Listener listener;
    private final boolean tuDongPing;
    private final Predicate<TinNhan> giuMulticast;
    private final Map<String,CompletableFuture<String[]>> dangCho=new ConcurrentHashMap<>();
    private final Map<String,Phong> daVao=new ConcurrentHashMap<>();
    private final Map<String,KenhPhong> cacKenh=new ConcurrentHashMap<>();
    private final java.util.Set<String> daSanSang=ConcurrentHashMap.newKeySet();
    private final Map<String,DongBoTinNhan> cacDongBo=new ConcurrentHashMap<>();
    private final Map<String,BangTen> cacThanhVien=new ConcurrentHashMap<>();
    private final TreeMap<Integer,Phong> choDanhSach=new TreeMap<>();
    private volatile List<Phong> danhSach=List.of();
    private volatile ReliableUdp udp;
    private volatile InetSocketAddress server;
    private volatile String token="";
    private volatile NguoiDung toi;
    private ScheduledExecutorService dongHo;
    private volatile long pingThanhCong;
    private long phienBanPhong=-1;
    private volatile boolean choPing, dangKhoiPhuc, thuLai;
    private char[] matKhauPhien=new char[0];
    private String tenPhien="";

    // Khoi tao lop va noi cac thanh phan can thiet.
    public ClientCore(Listener listener) { this(listener,true); }
    // Khoi tao lop va noi cac thanh phan can thiet.
    public ClientCore(Listener listener,boolean tuDongPing) { this(listener,tuDongPing,tin -> true); }
    // Bo loc chi de SelfTest chu dong lam mat goi multicast; ban thuong giu moi goi.
    public ClientCore(Listener listener,boolean tuDongPing,Predicate<TinNhan> giuMulticast) {
        this.listener=listener; this.tuDongPing=tuDongPing; this.giuMulticast=giuMulticast;
    }

    // ===== TAI KHOAN VA PHIEN =====
    private synchronized void ketNoi(String ip,int cong) throws IOException {
        MangUtil.dungIPv4();
        InetSocketAddress dich=new InetSocketAddress(InetAddress.getByName(ip.trim()),cong);
        if (!token.isEmpty()) { throw new IllegalStateException("Hãy đăng xuất trước"); }
        if (udp!=null && dich.equals(server)) { return; }
        close(); server=dich;
        udp=new ReliableUdp(new DatagramSocket(),this::nhanUnicast,listener::onLoi);
    }
    // Dang ky.
    public CompletableFuture<Void> dangKy(String ip,int cong,String ten,String matKhau) {
        try { ketNoi(ip,cong); return yeuCau(Protocol.DANG_KY,ten,matKhau).thenApply(p -> null); }
        catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
    }
    // Dang nhap.
    public CompletableFuture<NguoiDung> dangNhap(String ip,int cong,String ten,String matKhau) {
        try { ketNoi(ip,cong); }
        catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
        return yeuCau(Protocol.DANG_NHAP,ten,matKhau).thenCompose(p -> {
            if (p.length!=9+Phong.SO_TRUONG) {
                return CompletableFuture.failedFuture(new IOException("Phản hồi đăng nhập sai định dạng"));
            }
            token=p[3]; toi=NguoiDung.tuTruong(p,4);
            listener.onNguoiDung(toi);
            tenPhien=ten; matKhauPhien=matKhau.toCharArray(); batHeartbeat();
            try { CauHinhClient.ghi(ten,ip); } catch (IOException e) { listener.onLoi("Không ghi được tên/Server lần trước"); }
            return moKenh(Phong.tuTruong(p,9)).thenApply(r -> {
                return toi;
            });
        }).whenComplete((v,e) -> {
            if (e!=null && !token.isEmpty()) { yeuCau(Protocol.DANG_XUAT,token).whenComplete((p,loi) -> close()); }
        });
    }
    // Dang xuat.
    public CompletableFuture<Void> dangXuat() {
        thuLai=false;
        if (token.isEmpty()) { close(); return CompletableFuture.completedFuture(null); }
        return yeuCau(Protocol.DANG_XUAT,token).handle((p,e) -> {
            close();
            if (e!=null) { listener.onHeThong("Đã đóng Client; Server sẽ dọn phiên sau 15 giây."); }
            return null;
        });
    }
    // Nguoi dung.
    public NguoiDung nguoiDung() { return toi; }
    // Danh sach phong.
    public List<Phong> danhSachPhong() { return danhSach; }
    // Phong da vao.
    public List<Phong> phongDaVao() { return List.copyOf(daVao.values()); }

    // ===== API PHONG =====
    public CompletableFuture<Phong> taoPhong(String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) {
        return yeuCau(Protocol.TAO_PHONG,token,ten,moTa,Boolean.toString(congKhai),matKhau,Integer.toString(gioiHan))
                .thenApply(p -> Phong.tuTruong(p,3));
    }
    // Sua phong.
    public CompletableFuture<Phong> suaPhong(String id,String ten,String moTa,boolean congKhai,String matKhau,int gioiHan) {
        return yeuCau(Protocol.SUA_PHONG,token,id,ten,moTa,Boolean.toString(congKhai),matKhau==null?"KEEP":"SET",
                matKhau==null?"":matKhau,Integer.toString(gioiHan)).thenApply(p -> Phong.tuTruong(p,3));
    }
    // Xoa phong.
    public CompletableFuture<Void> xoaPhong(String id) { return yeuCau(Protocol.XOA_PHONG,token,id).thenApply(p -> null); }
    // Dong mo phong.
    public CompletableFuture<Void> dongMoPhong(String id,boolean mo) {
        return yeuCau(Protocol.TRANG_THAI_PHONG,token,id,Boolean.toString(mo)).thenApply(p -> null);
    }
    // Vao phong.
    public CompletableFuture<Phong> vaoPhong(String maHoacId,String matKhau) {
        return yeuCau(Protocol.CHUAN_BI_VAO,token,maHoacId,matKhau).thenCompose(p -> moKenh(Phong.tuTruong(p,3)));
    }
    // Mo kenh.
    private CompletableFuture<Phong> moKenh(Phong p) {
        if (daVao.containsKey(p.id)) { return CompletableFuture.completedFuture(p); }
        try {
            DongBoTinNhan dongBo=new DongBoTinNhan(p.id,(tin,lichSu) -> listener.onTinNhan(
                    tin,toi!=null && tin.userId.equals(toi.id),lichSu));
            cacDongBo.put(p.id,dongBo); daVao.put(p.id,p);
            KenhPhong kenh=new KenhPhong(p.id,p.nhom,p.cong,tin -> {
                if (giuMulticast.test(tin) && dongBo.nhan(tin) && daSanSang.contains(p.id)) { hoiDongBo(p.id); }
            },noiDung -> listener.onHeThong(p.id,noiDung),listener::onLoi);
            cacKenh.put(p.id,kenh); listener.onVaoPhong(p);
            return yeuCau(Protocol.SAN_SANG,token,p.id).thenCompose(v -> {
                        daSanSang.add(p.id); return dongBoPhong(p.id);
                    })
                    .thenApply(v -> p).whenComplete((v,e) -> {
                        if (e!=null) {
                            yeuCau(Protocol.ROI_PHONG,token,p.id);
                            yeuCau(Protocol.HUY_VAO,token,p.id);
                            dongKenh(p.id,"Không vào được phòng: "+loiDeHieu(e));
                        }
                    });
        } catch (IOException | RuntimeException e) {
            yeuCau(Protocol.HUY_VAO,token,p.id); dongKenh(p.id,"Không mở được kênh phòng");
            return CompletableFuture.failedFuture(e);
        }
    }
    // Chuyen chu.
    public CompletableFuture<Void> chuyenChu(String id,String userId) {
        return yeuCau(Protocol.CHUYEN_CHU,token,id,userId).thenApply(p -> null);
    }
    // Kiem tra server.
    public CompletableFuture<Void> kiemTraServer(String ip,int cong) {
        try { ketNoi(ip,cong); return yeuCau(Protocol.THU_SERVER).thenApply(p -> null); }
        catch (IOException | RuntimeException e) { return CompletableFuture.failedFuture(e); }
    }
    // San sang.
    public boolean sanSang() { return toi!=null && !thuLai; }
    // Roi phong.
    public CompletableFuture<Void> roiPhong(String id) {
        return yeuCau(Protocol.ROI_PHONG,token,id).thenApply(p -> { dongKenh(id,"Bạn đã rời phòng."); return null; });
    }
    // Gui tin.
    public CompletableFuture<Void> guiTin(String id,String noiDung) {
        if (thuLai) { return CompletableFuture.failedFuture(new IOException("Đang khôi phục kết nối, chưa thể gửi")); }
        if (!daVao.containsKey(id)) { return CompletableFuture.failedFuture(new IOException("Bạn chưa vào phòng")); }
        try { Goi.kiemTraTin(new TinNhan(Long.MAX_VALUE,id,toi.id,toi.tenHienThi,noiDung.strip(),Long.MAX_VALUE)); }
        catch (IllegalArgumentException e) { return CompletableFuture.failedFuture(e); }
        return yeuCau(Protocol.GUI_TIN,token,id,noiDung.trim()).thenApply(p -> null);
    }
    // Ham cu duoc giu cho SelfTest va truong hop chi chat Sanh chung.
    public CompletableFuture<Void> guiTin(String noiDung) { return guiTin(Protocol.SANH_ID,noiDung); }

    // ===== DONG BO TIN NHAN =====
    public CompletableFuture<Void> dongBoPhong(String id) {
        DongBoTinNhan db=cacDongBo.get(id);
        if (db==null) { return CompletableFuture.failedFuture(new IOException("Đã rời phòng")); }
        boolean lichSu=!db.daSanSang(); String ma=db.batDau(lichSu);
        CompletableFuture<Void> ketQua=db.ketQua();
        if (ma!=null) {
            yeuCau(lichSu?Protocol.LICH_SU:Protocol.DONG_BO,token,id,ma,Long.toString(db.cuoi()))
                    .whenComplete((p,e) -> { if (e!=null) { db.huy(ma,e); } });
            ketQua.whenComplete((v,e) -> { if (e==null && db.canDongBo()) { hoiDongBo(id); } });
        }
        return ketQua;
    }
    // Hoi dong bo.
    private void hoiDongBo(String id) {
        dongBoPhong(id).exceptionally(e -> {
            if (toi!=null && daVao.containsKey(id)) { listener.onLoi("Chưa đồng bộ được phòng; sẽ thử lại qua heartbeat."); }
            return null;
        });
    }
    // Dong kenh.
    private void dongKenh(String id,String lyDo) {
        daSanSang.remove(id);
        Phong cu=daVao.remove(id);
        KenhPhong kenh=cacKenh.remove(id); if (kenh!=null) { kenh.close(); }
        DongBoTinNhan db=cacDongBo.remove(id); if (db!=null) { db.close(); }
        cacThanhVien.remove(id);
        if (cu!=null) { listener.onRoiPhong(id,lyDo); }
    }

    // ===== REQUEST / RESPONSE =====
    private CompletableFuture<String[]> yeuCau(String lenh,String... thamSo) {
        ReliableUdp kenh=udp;
        if (kenh==null) { return CompletableFuture.failedFuture(new IOException("Chưa kết nối Server")); }
        String ma=UUID.randomUUID().toString();
        CompletableFuture<String[]> ketQua=new CompletableFuture<>(); dangCho.put(ma,ketQua);
        ketQua.orTimeout(7,TimeUnit.SECONDS).whenComplete((v,e) -> dangCho.remove(ma));
        kenh.gui(server,Goi.ghep(Goi.noi(new String[]{lenh,ma},thamSo))).whenComplete((v,e) -> {
            if (e!=null) { ketQua.completeExceptionally(e); }
        });
        return ketQua;
    }
    // Nhan unicast.
    private void nhanUnicast(InetSocketAddress nguon,String duLieu) {
        if (!nguon.equals(server)) { return; }
        String[] p=Goi.tach(duLieu);
        if (p.length<2) { return; }
        switch (p[0]) {
            case Protocol.TRA_LOI:
                if (p.length<4) { return; }
                CompletableFuture<String[]> cho=dangCho.get(p[1]);
                if (cho!=null) {
                    if ("OK".equals(p[2])) { cho.complete(p); }
                    else { cho.completeExceptionally(new IOException(p[3])); }
                }
                break;
            case Protocol.DANH_SACH_PHONG: nhanDanhSach(p); break;
            case Protocol.ONLINE: if (p.length==10) { nhanThanhVien(p); } break;
            case Protocol.DA_ROI_PHONG: if (p.length==3 && !thuLai) { dongKenh(p[1],p[2]); } break;
            case Protocol.MUC_DONG_BO:
                if (p.length==10 && cacDongBo.containsKey(p[1])) {
                    TinNhan tin=TinNhan.tuTruong(java.util.Arrays.copyOfRange(p,3,10));
                    DongBoTinNhan db=cacDongBo.get(p[1]); if (db!=null) { db.nhanMuc(p[2],tin); }
                }
                break;
            case Protocol.HET_DONG_BO:
                DongBoTinNhan db=cacDongBo.get(p[1]); if (p.length==7 && db!=null) { db.nhanKetThuc(p); } break;
            case Protocol.MOC_TIN:
                DongBoTinNhan dongBo=cacDongBo.get(p[1]);
                if (p.length==3 && dongBo!=null && dongBo.nhanMoc(Long.parseLong(p[2])) && daSanSang.contains(p[1])) { hoiDongBo(p[1]); }
                break;
            case Protocol.HET_PHIEN: if (p.length==2) { matKetNoi(p[1]); } break;
            default: break;
        }
    }

    // ===== SNAPSHOT DANH SACH THEO PHIEN BAN =====
    private synchronized void nhanDanhSach(String[] p) {
        if (p.length<4) { return; }
        long ban=Long.parseLong(p[1]); int so=Integer.parseInt(p[2]), viTri=Integer.parseInt(p[3]);
        if (ban<phienBanPhong || so<0 || so>254 || (so>0 && (viTri<0 || viTri>=so))) { return; }
        if (ban>phienBanPhong) { choDanhSach.clear(); phienBanPhong=ban; }
        if (so>0) {
            if (p.length!=7+Phong.SO_TRUONG) { return; }
            int them=4+Phong.SO_TRUONG;
            choDanhSach.put(viTri,Phong.tuTruong(p,4).voiThongTin(Boolean.parseBoolean(p[them]),p[them+1],Integer.parseInt(p[them+2])));
        }
        if (choDanhSach.size()==so) {
            danhSach=List.copyOf(choDanhSach.values());
            danhSach.forEach(r -> daVao.computeIfPresent(r.id,(id,cu) -> r));
            listener.onDanhSachPhong(danhSach);
        }
    }
    // Nhan thanh vien.
    private void nhanThanhVien(String[] p) {
        if (!daVao.containsKey(p[1])) { return; }
        BangTen bang=cacThanhVien.computeIfAbsent(p[1],id -> new BangTen());
        long ban=Long.parseLong(p[2]); int so=Integer.parseInt(p[3]), viTri=Integer.parseInt(p[4]);
        if (ban<bang.ban || so<1 || so>1000 || viTri<0 || viTri>=so) { return; }
        if (ban>bang.ban) { bang.ten.clear(); bang.ban=ban; }
        bang.ten.put(viTri,new Phong.ThanhVien(p[5],p[6],p[7],p[8],Boolean.parseBoolean(p[9])));
        if (bang.ten.size()==so) { listener.onThanhVienChiTiet(p[1],List.copyOf(bang.ten.values())); }
    }

    // ===== HEARTBEAT VA DONG =====
    private synchronized void batHeartbeat() {
        pingThanhCong=System.nanoTime();
        if (!tuDongPing || token.isEmpty()) { return; }
        dongHo=Executors.newSingleThreadScheduledExecutor(viec -> {
            Thread t=new Thread(viec,"client-heartbeat"); t.setDaemon(true); return t;
        });
        dongHo.scheduleAtFixedRate(this::ping,Protocol.PING_MS,Protocol.PING_MS,TimeUnit.MILLISECONDS);
    }
    // Ping.
    private void ping() {
        if (token.isEmpty() || choPing || dangKhoiPhuc) { return; }
        if (thuLai) { khoiPhuc(); return; }
        choPing=true;
        yeuCau(Protocol.PING,token).whenComplete((p,e) -> {
            choPing=false;
            if (e==null) { pingThanhCong=System.nanoTime(); }
            else if (System.nanoTime()-pingThanhCong>Protocol.HET_PHIEN_MS*1_000_000L) {
                matKetNoi("Không liên lạc được Server. Kiểm tra IP, Server và Windows Firewall.");
            }
        });
    }
    // Mat ket noi.
    private void matKetNoi(String lyDo) {
        if (toi==null) { return; }
        if (!tuDongPing) { close(); listener.onMatKetNoi(lyDo); return; }
        thuLai=true; listener.onKetNoi(false,"Mất kết nối, đang thử kết nối lại…");
    }
    // Dang nhap lai cung endpoint, giu bo dem msgId de RESYNC thay vi nhan trung lich su.
    private void khoiPhuc() {
        if (matKhauPhien.length==0 || toi==null) { return; }
        dangKhoiPhuc=true; String cu=token;
        yeuCau(Protocol.KET_NOI_LAI,tenPhien,new String(matKhauPhien),cu).thenCompose(p -> {
            if (toi==null) { return CompletableFuture.failedFuture(new IOException("Đã đăng xuất")); }
            token=p[3]; toi=NguoiDung.tuTruong(p,4);
            listener.onNguoiDung(toi);
            synchronized (this) { phienBanPhong=-1; choDanhSach.clear(); cacThanhVien.clear(); }
            CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
            for (Phong r : phongDaVao()) {
                viec=viec.thenCompose(v -> khoiPhucPhong(r));
            }
            return viec.thenCompose(v -> yeuCau(Protocol.PING,token)).thenApply(k -> (Void)null);
        }).whenComplete((v,e) -> {
            dangKhoiPhuc=false;
            if (e==null && toi!=null) { thuLai=false; pingThanhCong=System.nanoTime(); listener.onKetNoi(true,"Đã kết nối lại và đồng bộ tin nhắn."); }
        });
    }
    // Khoi phuc tung phong tuan tu; chi bo phong khi Server xac nhan khong con quyen.
    private CompletableFuture<Void> khoiPhucPhong(Phong r) {
        return yeuCau(Protocol.CHUAN_BI_VAO,token,r.maPhong,"")
            .thenCompose(k -> yeuCau(Protocol.SAN_SANG,token,r.id)).thenCompose(k -> dongBoPhong(r.id))
            .exceptionallyCompose(e -> {
                String lyDo=loiDeHieu(e);
                if (lyDo.contains("không tồn tại") || lyDo.contains("Không tìm thấy") || lyDo.contains("bị cấm")) {
                    dongKenh(r.id,"Không khôi phục được phòng: "+lyDo); return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.failedFuture(e);
            });
    }
    // Loi de hieu.
    public static String loiDeHieu(Throwable loi) {
        while (loi.getCause()!=null) { loi=loi.getCause(); }
        return loi instanceof java.util.concurrent.TimeoutException?"Server chưa phản hồi":String.valueOf(loi.getMessage());
    }
    // Dong tai nguyen cua lop, ke ca khi co loi.
    @Override public void close() {
        ReliableUdp kenhCu;
        synchronized (this) {
            toi=null; token=""; kenhCu=udp; udp=null; thuLai=false;
            java.util.Arrays.fill(matKhauPhien,'\0'); matKhauPhien=new char[0]; tenPhien="";
            if (dongHo!=null) { dongHo.shutdownNow(); dongHo=null; }
            choDanhSach.clear(); danhSach=List.of(); phienBanPhong=-1; choPing=false;
        }
        // Khong giu khoa Client khi dong Future cua tung phong.
        for (String id : List.copyOf(daVao.keySet())) { dongKenh(id,"Đã đóng kết nối."); }
        if (kenhCu!=null) { kenhCu.close(); }
        dangCho.values().forEach(f -> f.completeExceptionally(new IOException("Kết nối đã đóng")));
        dangCho.clear(); cacThanhVien.clear();
    }
    /** Bo dem ghep danh sach thanh vien rieng tung phong. */
    private static final class BangTen {
        long ban=-1;
        final TreeMap<Integer,Phong.ThanhVien> ten=new TreeMap<>();
    }
}
