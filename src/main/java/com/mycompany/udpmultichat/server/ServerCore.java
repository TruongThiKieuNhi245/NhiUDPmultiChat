package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.Goi;
import com.mycompany.udpmultichat.common.MangUtil;
import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.ReliableUdp;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import com.mycompany.udpmultichat.common.model.VaiTro;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Vai tro: Server UDP dieu phoi tai khoan/phong; luu tin truoc khi multicast dung nhom phong. Ai goi: ServerMain, SelfTest. Goi: ReliableUdp, QuanLyNguoiDung, QuanLyPhong, KhoLuuTru. */
public final class ServerCore implements AutoCloseable {
    /** Giao dien nhan su kien tren luong nen, khong rang buoc Swing. */
    public interface Listener {
        // Nhan log qua Listener, chuyen ve luong phu hop.
        void onLog(String noiDung);
        // Nhan snapshot thong ke tu ServerCore.
        void onThongKe(int tongUser,int online,long soTin);
        // Bo sung so phong, giu listener test cu khong phu thuoc giao dien.
        default void onThongKe(int tongUser,int online,int soPhong,long soTin) { onThongKe(tongUser,online,soTin); }
    }
    private final Path thuMuc;
    private final Listener listener;
    private QuanLyNguoiDung nguoiDung;
    private QuanLyPhong phong;
    private QuanLyHoSo hoSo;
    private ReliableUdp udp;
    private QuanLyTep tep;
    private double matTep; private long seedTep;
    private MulticastSocket multicast;
    private ScheduledExecutorService dongHo;
    private long phienBan;
    private volatile boolean dangChay;
    // Khoi tao lop va noi cac thanh phan can thiet.
    public ServerCore(Path thuMuc,Listener listener) { this.thuMuc=thuMuc; this.listener=listener; }
    // Mat DATA/ACK voi seed chi dung kiem thu, khong sua cau hinh nguoi dung.
    public void moPhongMatTep(double tiLe,long seed) { if (dangChay) { throw new IllegalStateException("Server đang chạy"); } matTep=tiLe; seedTep=seed; }
    // So tep/dung luong da luu theo phong va nguoi gui, danh cho Dashboard.
    public synchronized long[] thongKeTep(String room,String user) { return tep==null?new long[]{0,0}:tep.thongKe(room,user); }
    // ===== BAT DAU / DUNG =====
    public synchronized void batDau(int cong) throws IOException {
        if (dangChay) { throw new IllegalStateException("Server đang chạy"); }
        MangUtil.dungIPv4();
        try {
            NetworkInterface card=MangUtil.timGiaoDienMang();
            KhoLuuTru kho=new KhoLuuTru(thuMuc,listener::onLog);
            nguoiDung=new QuanLyNguoiDung(kho); hoSo=new QuanLyHoSo(kho,nguoiDung);
            phong=new QuanLyPhong(kho,nguoiDung::danhSach);
            tep=new QuanLyTep(thuMuc,kho,phong,(p,t,ma) -> {
                TinNhan tin=phong.themTin(p,t.room(),t.dongGoi(),ma,"F");
                try { phatMulticast(t.room(),tin.dongGoi()); } catch (IOException e) { listener.onLog("Tin tệp đã lưu; Client sẽ RESYNC."); }
                baoThongKe(); return tin;
            },matTep,seedTep);
            multicast=new MulticastSocket();
            multicast.setNetworkInterface(card); multicast.setTimeToLive(1);
            DatagramSocket socket=new DatagramSocket(null);
            try { socket.bind(new InetSocketAddress(cong)); }
            catch (IOException e) { socket.close(); throw e; }
            udp=new ReliableUdp(socket,this::xuLyGoi,listener::onLog);
            dongHo=Executors.newSingleThreadScheduledExecutor(viec -> {
                Thread t=new Thread(viec,"server-heartbeat"); t.setDaemon(true); return t;
            });
            dangChay=true;
            dongHo.scheduleAtFixedRate(this::quetPhien,1,1,TimeUnit.SECONDS);
            listener.onLog("Server UDP cổng "+udp.cong()+"; card "+card.getDisplayName());
            Phong sanh=phong.lay(Protocol.SANH_ID);
            listener.onLog("Sảnh chung: "+sanh.nhom+":"+sanh.cong);
            baoThongKe();
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }
    // Cong.
    public int cong() { return udp==null?-1:udp.cong(); }
    // Dang chay.
    public boolean dangChay() { return dangChay; }
    // Dong tai nguyen cua lop, ke ca khi co loi.
    @Override public synchronized void close() {
        dangChay=false;
        if (dongHo!=null) { dongHo.shutdownNow(); }
        if (udp!=null) { udp.close(); }
        if (tep!=null) { tep.close(); tep=null; }
        if (multicast!=null) { multicast.close(); }
        listener.onLog("Server đã dừng; Client sẽ phát hiện qua heartbeat.");
        listener.onThongKe(nguoiDung==null?0:nguoiDung.tongNguoiDung(),0,phong==null?0:phong.tongTin());
    }
    // ===== NHAN VA PHAN LENH =====
    // Cung khoa voi heartbeat; cac tac vu gui lo chi cho ACK bat dong bo.
    private synchronized void xuLyGoi(InetSocketAddress nguon,String duLieu) {
        if (!dangChay) { return; }
        String ma="";
        try {
            String[] p=Goi.tach(duLieu);
            if (p.length<2 || p[1].length()>64) { return; }
            if (Protocol.DANG_GO.equals(p[0])) { typing(nguon,p); return; }
            ma=p[1];
            if (Protocol.THU_SERVER.equals(p[0])) { duTruong(p,2); traLoi(nguon,ma,true,"Server đang hoạt động"); return; }
            if (Protocol.DANG_KY.equals(p[0]) || Protocol.DANG_NHAP.equals(p[0]) || Protocol.KET_NOI_LAI.equals(p[0])) {
                taiKhoan(nguon,p); return;
            }
            if (p.length<3) { throw new IllegalArgumentException("Thiếu token phiên"); }
            Phien phien=nguoiDung.kiemTra(p[2],nguon);
            nguoiDung.kiemTraBatBuoc(phien,p[0]);
            if (java.util.Set.of(Protocol.HO_SO,Protocol.LUU_HO_SO,Protocol.DOI_MAT_KHAU,Protocol.ANH_BAT_DAU,Protocol.ANH_MANH,Protocol.ANH_HUY,Protocol.ANH_DOC).contains(p[0])) {
                xuLyHoSo(phien,p); return;
            }
            if (p[0].startsWith("FILE_")) { traLoi(nguon,ma,true,tep.xuLy(phien,p)); return; }
            switch (p[0]) {
                case Protocol.PING:
                    duTruong(p,3); nguoiDung.ping(phien); guiMocTin(phien);
                    traLoi(nguon,ma,true,"PONG"); break;
                case Protocol.DANG_XUAT:
                    duTruong(p,3); nguoiDung.dangXuat(phien);
                    roiTatCa(phien,"đã đăng xuất."); traLoi(nguon,ma,true,"Đã đăng xuất"); break;
                case Protocol.SAN_SANG: sanSang(phien,p); break;
                case Protocol.GUI_TIN: case Protocol.STICKER: guiTin(phien,p); break;
                case Protocol.THU_HOI: thuHoi(phien,p); break;
                case Protocol.DS_THU_HOI: duTruong(p,5); phong.kiemTraThanhVien(phien,p[3]); traLoi(nguon,ma,true,phong.tinNhan().danhSachThuHoi(p[3],Long.parseLong(p[4]))); break;
                case Protocol.LICH_SU: case Protocol.DONG_BO: guiLichSu(phien,p); break;
                default: xuLyPhong(phien,p);
            }
        } catch (IllegalArgumentException e) { traLoi(nguon,ma,false,e.getMessage()); }
        catch (IOException e) {
            listener.onLog("Lỗi lưu/gửi: "+e.getMessage());
            traLoi(nguon,ma,false,"Server không lưu/gửi được dữ liệu. Hãy thử lại.");
        }
    }
    // Tai khoan.
    private void taiKhoan(InetSocketAddress nguon,String[] p) throws IOException {
        if (Protocol.KET_NOI_LAI.equals(p[0])) {
            duTruong(p,5); quetPhien(); Phien k=nguoiDung.ketNoiLai(p[2],p[3],p[4],nguon);
            traLoi(nguon,p[1],true,Goi.noi(new String[]{k.token},k.nguoi.cacTruong())); return;
        }
        duTruong(p,4);
        if (Protocol.DANG_KY.equals(p[0])) {
            nguoiDung.dangKy(p[2],p[3]); traLoi(nguon,p[1],true,"Đăng ký thành công. Hãy đăng nhập.");
            listener.onLog("Đăng ký: "+p[2]);
        } else {
            quetPhien();
            Phien moi=nguoiDung.dangNhap(p[2],p[3],nguon);
            traLoi(nguon,p[1],true,Goi.noi(new String[]{moi.token},moi.nguoi.cacTruong()));
            listener.onLog("Đăng nhập: "+moi.nguoi.tenDangNhap);
        }
        baoThongKe();
    }
    // Ho so moi duoc phat den chinh minh va cac phien chung phong.
    private void xuLyHoSo(Phien p,String[] g) throws IOException {
        String[] k=hoSo.xuLy(p,g); traLoi(p.diaChi,g[1],true,k);
        if (g[0].equals(Protocol.LUU_HO_SO) || g[0].equals(Protocol.DOI_MAT_KHAU)) {
            String goi=Goi.ghep(Goi.noi(new String[]{Protocol.HO_SO_MOI},p.nguoi.cacTruong()));
            nguoiDung.danhSach().stream().filter(n -> n==p || n.phong.stream().anyMatch(p.phong::contains)).forEach(n -> guiSuKien(n,goi));
            for (String id:p.phong) { capNhatThanhVien(id); }
            capNhatPhong(); listener.onLog(p.nguoi.tenDangNhap+(g[0].equals(Protocol.LUU_HO_SO)?" đã cập nhật hồ sơ.":" đã đổi mật khẩu."));
        }
    }
    // ===== CRUD PHONG =====
    private void xuLyPhong(Phien phien,String[] p) throws IOException {
        Phong r;
        switch (p[0]) {
            case Protocol.LAY_PHONG:
                duTruong(p,3); guiDanhSachPhong(phien,++phienBan);
                traLoi(phien.diaChi,p[1],true,"Đã cập nhật danh sách"); return;
            case Protocol.XOA_PHONG:
                duTruong(p,4); xoaPhong(phien,p[3]);
                traLoi(phien.diaChi,p[1],true,"Đã xóa phòng"); break;
            case Protocol.CHUAN_BI_VAO:
                duTruong(p,5); r=phong.datCho(phien,p[3],p[4]);
                traLoi(phien.diaChi,p[1],true,r.cacTruong()); return;
            case Protocol.HUY_VAO:
                duTruong(p,4); phien.choVao.remove(p[3]);
                traLoi(phien.diaChi,p[1],true,"Đã hủy đặt chỗ"); return;
            case Protocol.CHUYEN_CHU:
                duTruong(p,5); phong.chuyenChu(phien,p[3],p[4]);
                listener.onLog(phien.nguoi.tenHienThi+" chuyển chủ phòng "+phong.lay(p[3]).ten);
                capNhatThanhVien(p[3]); traLoi(phien.diaChi,p[1],true,"Đã chuyển quyền chủ phòng"); break;
            case Protocol.ROI_PHONG:
                duTruong(p,4); phong.kiemTraThanhVien(phien,p[3]); phong.roiChuDong(phien,p[3]);
                roiPhong(phien,p[3],"Bạn đã rời phòng.");
                traLoi(phien.diaChi,p[1],true,"Đã rời phòng"); break;
            default: suaTaoPhong(phien,p);
        }
        capNhatPhong();
    }
    // Tao/sua/trang thai duoc tach khoi vao/roi de moi ham de theo doi.
    private void suaTaoPhong(Phien phien,String[] p) throws IOException {
        Phong r;
        switch (p[0]) {
            case Protocol.TAO_PHONG:
                duTruong(p,8);
                r=phong.tao(phien,p[3],p[4],docBoolean(p[5]),p[6],Integer.parseInt(p[7]));
                traLoi(phien.diaChi,p[1],true,r.cacTruong());
                listener.onLog(phien.nguoi.tenHienThi+" tạo phòng "+r.ten+" — "+r.nhom+":"+r.cong); break;
            case Protocol.SUA_PHONG:
                duTruong(p,10);
                if (!p[7].equals("KEEP") && !p[7].equals("SET")) { throw new IllegalArgumentException("Chế độ mật khẩu không hợp lệ"); }
                r=phong.sua(phien,p[3],p[4],p[5],docBoolean(p[6]),p[7].equals("KEEP")?null:p[8],Integer.parseInt(p[9]));
                traLoi(phien.diaChi,p[1],true,r.cacTruong());
                listener.onLog(phien.nguoi.tenHienThi+" sửa phòng "+r.ten);
                thongBaoPhong(r.id,"Thông tin phòng đã được cập nhật."); break;
            case Protocol.TRANG_THAI_PHONG:
                duTruong(p,5); phong.dongMo(phien,p[3],docBoolean(p[4]));
                thongBaoPhong(p[3],docBoolean(p[4])?"Phòng đã mở lại.":"Phòng đã đóng; tạm dừng vào phòng và gửi tin.");
                listener.onLog(phien.nguoi.tenHienThi+" "+(docBoolean(p[4])?"mở":"đóng")+" phòng "+phong.lay(p[3]).ten);
                traLoi(phien.diaChi,p[1],true,"Đã cập nhật trạng thái phòng"); break;
            default: throw new IllegalArgumentException("Lệnh không được hỗ trợ");
        }
    }
    // San sang.
    private void sanSang(Phien phien,String[] p) throws IOException {
        duTruong(p,4);
        boolean moi=!phien.phong.contains(p[3]);
        if (Protocol.SANH_ID.equals(p[3]) && moi) { phong.datCho(phien,p[3],""); }
        phong.xacNhanVao(phien,p[3]); phien.sanSang=true;
        if (moi) {
            thongBaoPhong(p[3],phien.nguoi.tenHienThi+" đã vào "+phong.lay(p[3]).ten+".");
            listener.onLog(phien.nguoi.tenHienThi+" vào "+phong.lay(p[3]).ten);
        }
        capNhatThanhVien(p[3]); capNhatPhong();
        traLoi(phien.diaChi,p[1],true,"Đã vào phòng");
    }
    // Roi phong.
    private void roiPhong(Phien phien,String id,String lyDo) {
        phong.roi(phien,id);
        guiSuKien(phien,Goi.ghep(Protocol.DA_ROI_PHONG,id,lyDo));
        thongBaoPhong(id,phien.nguoi.tenHienThi+" đã rời "+phong.lay(id).ten+".");
        capNhatThanhVien(id);
    }
    // Roi tat ca.
    private void roiTatCa(Phien phien,String lyDo) {
        if (tep!=null) { tep.matPhien(phien.token); }
        for (String id : List.copyOf(phien.phong)) { roiPhong(phien,id,"Phiên đã kết thúc."); }
        phien.choVao.clear(); listener.onLog(phien.nguoi.tenHienThi+" "+lyDo); capNhatPhong();
    }
    // Xoa phong.
    private void xoaPhong(Phien chu,String id) throws IOException {
        String ten=phong.lay(id).ten;
        phong.xoa(chu,id); tep.xoaPhong(id);
        for (Phien p : nguoiDung.danhSach()) {
            if (p.phong.contains(id)) {
                phong.roi(p,id); guiSuKien(p,Goi.ghep(Protocol.DA_ROI_PHONG,id,"Phòng «"+ten+"» đã bị xóa."));
            }
            p.choVao.remove(id);
        }
        listener.onLog(chu.nguoi.tenHienThi+" xóa phòng "+ten);
    }
    // ===== TIN NHAN VA GUI BU =====
    private void guiTin(Phien phien,String[] p) throws IOException {
        duTruong(p,6); TinNhan tin=phong.themTin(phien,p[3],p[5],p[4],p[0].equals(Protocol.STICKER)?"S":"T");
        try { phatMulticast(p[3],tin.dongGoi()); }
        catch (IOException e) { listener.onLog("Tin đã lưu; multicast lỗi, Client sẽ RESYNC: "+e.getMessage()); }
        traLoi(phien.diaChi,p[1],true,Long.toString(tin.msgId)); baoThongKe();
    }
    // Thu hoi phat su kien tin cay; heartbeat revision bu khi Client da mat ket noi.
    private void thuHoi(Phien p,String[] g) throws IOException {
        duTruong(g,5); phong.kiemTraThanhVien(p,g[3]);
        TinNhan cu=phong.tinNhan().theoId(g[3],Long.parseLong(g[4]));
        TinNhan t=phong.tinNhan().thuHoi(p,phong.lay(g[3]),Long.parseLong(g[4]));
        tep.xoaTin(cu);
        phong.thanhVien(g[3]).forEach(n -> guiSuKien(n,Goi.ghep(Protocol.DA_THU_HOI,g[3],Long.toString(t.msgId))));
        traLoi(p.diaChi,g[1],true,"Đã thu hồi tin nhắn."); listener.onLog(p.nguoi.tenDangNhap+" thu hồi tin "+t.msgId+" ở "+g[3]);
    }
    // Kiem tra token va quyen truoc khi phat nhanh vao dung nhom; khong ACK/luu lich su.
    private void typing(InetSocketAddress diaChi,String[] g) throws IOException {
        duTruong(g,3); Phien p=nguoiDung.kiemTra(g[1],diaChi); nguoiDung.kiemTraBatBuoc(p,Protocol.DANG_GO);
        phong.kiemTraThanhVien(p,g[2]); if (!phong.lay(g[2]).dangMo) { return; }
        if (phong.tinNhan().choTyping(g[2],p.nguoi.id,System.nanoTime()/1000000L)) {
            phatMulticast(g[2],Goi.ghep(Protocol.DANG_GO,g[2],p.nguoi.id,p.nguoi.tenHienThi));
        }
    }
    // Gui lich su.
    private void guiLichSu(Phien phien,String[] p) {
        duTruong(p,6); phong.kiemTraThanhVien(phien,p[3]);
        if (!p[4].matches("[a-f0-9-]{36}")) { throw new IllegalArgumentException("Mã đồng bộ không hợp lệ"); }
        long sau=Long.parseLong(p[5]), head=phong.mocTin(p[3]);
        if (sau<0 || sau>head) { throw new IllegalArgumentException("Mốc đồng bộ không hợp lệ"); }
        boolean lanDau=p[0].equals(Protocol.LICH_SU);
        if (!lanDau) { listener.onLog("RESYNC "+phien.nguoi.tenDangNhap+" phòng "+phong.lay(p[3]).ten+" sau tin "+sau); }
        List<TinNhan> ds=phong.layTin(p[3],lanDau,sau);
        long dau=ds.isEmpty()?(lanDau?head+1:sau+1):ds.get(0).msgId;
        long cuoi=ds.isEmpty()?(lanDau?head:sau):ds.get(ds.size()-1).msgId;
        List<String> lo=new ArrayList<>();
        for (TinNhan tin : ds) {
            lo.add(Goi.ghep(Goi.noi(new String[]{Protocol.MUC_DONG_BO,p[3],p[4]},Goi.tach(tin.dongGoi()))));
        }
        lo.add(Goi.ghep(Protocol.HET_DONG_BO,p[3],p[4],Long.toString(dau),Long.toString(cuoi),Integer.toString(ds.size()),Long.toString(head)));
        guiLo(phien,lo); traLoi(phien.diaChi,p[1],true,p[4]);
    }
    // Socket phat dung nhom VA cong cua phong, khong dung nhom co dinh.
    private void phatMulticast(String roomId,String duLieu) throws IOException {
        Phong r=phong.lay(roomId); byte[] data=Goi.thanhByte(duLieu);
        multicast.send(new DatagramPacket(data,data.length,InetAddress.getByName(r.nhom),r.cong));
    }
    // Thong bao phong.
    private void thongBaoPhong(String id,String noiDung) {
        try { phatMulticast(id,Goi.ghep(Protocol.HE_THONG,id,noiDung)); }
        catch (IOException e) { listener.onLog("Lỗi thông báo phòng: "+e.getMessage()); }
    }
    // ===== SNAPSHOT REALTIME VA HEARTBEAT =====
    private void capNhatPhong() {
        long ban=++phienBan;
        nguoiDung.danhSach().stream().filter(p -> p.sanSang).forEach(p -> guiDanhSachPhong(p,ban));
        baoThongKe();
    }
    // Gui danh sach phong.
    private void guiDanhSachPhong(Phien p,long ban) {
        List<Phong> ds=phong.danhSachCho(p); List<String> lo=new ArrayList<>();
        if (ds.isEmpty()) { lo.add(Goi.ghep(Protocol.DANH_SACH_PHONG,Long.toString(ban),"0","-1")); }
        for (int i=0;i<ds.size();i++) {
            Phong r=ds.get(i);
            String chu=nguoiDung.tatCa().stream().filter(n -> n.id.equals(r.chuId)).map(n -> n.tenHienThi+" (@"+n.tenDangNhap+")").findFirst().orElse("Hệ thống");
            lo.add(Goi.ghep(Goi.noi(new String[]{Protocol.DANH_SACH_PHONG,Long.toString(ban),Integer.toString(ds.size()),Integer.toString(i)},ds.get(i).cacTruong(),
                new String[]{Boolean.toString(phong.laThanhVien(p.nguoi.id,r.id)),chu,Integer.toString(phong.soThanhVienBenVung(r.id))})));
        }
        guiLo(p,lo);
    }
    // Cap nhat thanh vien.
    private void capNhatThanhVien(String id) {
        List<Phien> online=phong.thanhVien(id); String ban=Long.toString(++phienBan); Phong r=phong.lay(id);
        var ds=nguoiDung.tatCa().stream().filter(n -> phong.laThanhVien(n.id,id)).toList();
        for (Phien dich : online) {
            List<String> lo=new ArrayList<>();
            for (int i=0;i<ds.size();i++) {
                var n=ds.get(i); String vai=n.vaiTro==VaiTro.ADMIN?"ADMIN":(r.chuId.equals(n.id)?"CHU_PHONG":"THANH_VIEN");
                boolean bat=online.stream().anyMatch(p -> p.nguoi.id.equals(n.id));
                lo.add(Goi.ghep(Protocol.ONLINE,id,ban,Integer.toString(ds.size()),Integer.toString(i),n.id,n.tenDangNhap,n.tenHienThi,vai,Boolean.toString(bat)));
            }
            guiLo(dich,lo);
        }
    }
    // Gui moc tin.
    private void guiMocTin(Phien p) {
        List<String> lo=p.phong.stream().map(id -> Goi.ghep(Protocol.MOC_TIN,id,Long.toString(phong.mocTin(id)),Long.toString(phong.tinNhan().phienBan(id)))).toList();
        guiLo(p,lo);
    }
    // Quet phien.
    private synchronized void quetPhien() {
        if (!dangChay) { return; }
        phong.donDatCho(); hoSo.donDep();
        for (Phien p : nguoiDung.xoaHetHan()) {
            guiSuKien(p,Goi.ghep(Protocol.HET_PHIEN,"Không có heartbeat quá 15 giây. Hãy đăng nhập lại."));
            roiTatCa(p,"đã mất kết nối (hết heartbeat).");
        }
    }
    // Bao thong ke.
    private void baoThongKe() {
        listener.onThongKe(nguoiDung.tongNguoiDung(),nguoiDung.danhSach().size(),phong.tongPhong(),phong.tongTin());
    }
    // ===== GUI UNICAST KHONG CHAN RECEIVE =====
    // Moi lo cho ACK tung goi, tranh day hang 512 goi khi lich su/danh sach dai.
    private void guiLo(Phien phien,List<String> lo) {
        ReliableUdp kenh=udp;
        CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
        for (String duLieu : lo) { viec=viec.thenCompose(v -> kenh.gui(phien.diaChi,duLieu)); }
        viec.exceptionally(e -> { if (dangChay) { listener.onLog("Không gửi đủ cập nhật cho "+phien.nguoi.tenDangNhap); } return null; });
    }
    // Gui su kien.
    private void guiSuKien(Phien phien,String duLieu) { guiLo(phien,List.of(duLieu)); }
    // Tra loi.
    private void traLoi(InetSocketAddress dich,String ma,boolean ok,String... noiDung) {
        if (ma.isEmpty() || udp==null) { return; }
        udp.gui(dich,Goi.ghep(Goi.noi(new String[]{Protocol.TRA_LOI,ma,ok?"OK":"ERR"},noiDung)))
            .exceptionally(e -> { if (dangChay) { listener.onLog("Không nhận ACK phản hồi từ "+dich); } return null; });
    }
    // Du truong.
    private static void duTruong(String[] p,int so) {
        if (p.length!=so) { throw new IllegalArgumentException("Số trường của gói không hợp lệ"); }
    }
    // Doc boolean.
    private static boolean docBoolean(String s) {
        if (!s.equals("true") && !s.equals("false")) { throw new IllegalArgumentException("Giá trị đúng/sai không hợp lệ"); }
        return Boolean.parseBoolean(s);
    }
}
