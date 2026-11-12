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

/** Server UDP dieu phoi tai khoan/phong; luu tin truoc khi multicast dung nhom phong. */
public final class ServerCore implements AutoCloseable {
    /** Giao dien nhan su kien tren luong nen, khong rang buoc Swing. */
    public interface Listener {
        void onLog(String noiDung);
        void onThongKe(int tongUser,int online,long soTin);
    }
    private final Path thuMuc;
    private final Listener listener;
    private QuanLyNguoiDung nguoiDung;
    private QuanLyPhong phong;
    private ReliableUdp udp;
    private MulticastSocket multicast;
    private ScheduledExecutorService dongHo;
    private long phienBan;
    private volatile boolean dangChay;

    public ServerCore(Path thuMuc,Listener listener) { this.thuMuc=thuMuc; this.listener=listener; }

    // ===== BAT DAU / DUNG =====
    public synchronized void batDau(int cong) throws IOException {
        if (dangChay) { throw new IllegalStateException("Server đang chạy"); }
        MangUtil.dungIPv4();
        try {
            NetworkInterface card=MangUtil.timGiaoDienMang();
            KhoLuuTru kho=new KhoLuuTru(thuMuc,listener::onLog);
            nguoiDung=new QuanLyNguoiDung(kho);
            phong=new QuanLyPhong(kho,nguoiDung::danhSach);
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
    public int cong() { return udp==null?-1:udp.cong(); }
    public boolean dangChay() { return dangChay; }

    @Override public synchronized void close() {
        dangChay=false;
        if (dongHo!=null) { dongHo.shutdownNow(); }
        if (udp!=null) { udp.close(); }
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
            ma=p[1];
            if (Protocol.DANG_KY.equals(p[0]) || Protocol.DANG_NHAP.equals(p[0])) {
                taiKhoan(nguon,p); return;
            }
            if (p.length<3) { throw new IllegalArgumentException("Thiếu token phiên"); }
            Phien phien=nguoiDung.kiemTra(p[2],nguon);
            switch (p[0]) {
                case Protocol.PING:
                    duTruong(p,3); nguoiDung.ping(phien); guiMocTin(phien);
                    traLoi(nguon,ma,true,"PONG"); break;
                case Protocol.DANG_XUAT:
                    duTruong(p,3); nguoiDung.dangXuat(phien);
                    roiTatCa(phien,"đã đăng xuất."); traLoi(nguon,ma,true,"Đã đăng xuất"); break;
                case Protocol.SAN_SANG: sanSang(phien,p); break;
                case Protocol.GUI_TIN: guiTin(phien,p); break;
                case Protocol.LICH_SU: case Protocol.DONG_BO: guiLichSu(phien,p); break;
                default: xuLyPhong(phien,p);
            }
        } catch (IllegalArgumentException e) { traLoi(nguon,ma,false,e.getMessage()); }
        catch (IOException e) {
            listener.onLog("Lỗi lưu/gửi: "+e.getMessage());
            traLoi(nguon,ma,false,"Server không lưu/gửi được dữ liệu. Hãy thử lại.");
        }
    }

    private void taiKhoan(InetSocketAddress nguon,String[] p) throws IOException {
        duTruong(p,4);
        if (Protocol.DANG_KY.equals(p[0])) {
            nguoiDung.dangKy(p[2],p[3]); traLoi(nguon,p[1],true,"Đăng ký thành công. Hãy đăng nhập.");
            listener.onLog("Đăng ký: "+p[2]);
        } else {
            quetPhien();
            Phien moi=nguoiDung.dangNhap(p[2],p[3],nguon);
            traLoi(nguon,p[1],true,Goi.noi(new String[]{moi.token},moi.nguoi.cacTruong(),
                    phong.lay(Protocol.SANH_ID).cacTruong()));
            listener.onLog("Đăng nhập: "+moi.nguoi.tenDangNhap);
        }
        baoThongKe();
    }

    // ===== CRUD PHONG =====
    private void xuLyPhong(Phien phien,String[] p) throws IOException {
        Phong r;
        switch (p[0]) {
            case Protocol.LAY_PHONG:
                duTruong(p,3); guiDanhSachPhong(phien,++phienBan);
                traLoi(phien.diaChi,p[1],true,"Đã cập nhật danh sách"); return;
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
                thongBaoPhong(r.id,"Thông tin phòng đã được cập nhật."); break;
            case Protocol.TRANG_THAI_PHONG:
                duTruong(p,5); phong.dongMo(phien,p[3],docBoolean(p[4]));
                thongBaoPhong(p[3],docBoolean(p[4])?"Phòng đã mở lại.":"Phòng đã đóng; tạm dừng vào phòng và gửi tin.");
                traLoi(phien.diaChi,p[1],true,"Đã cập nhật trạng thái phòng"); break;
            case Protocol.XOA_PHONG:
                duTruong(p,4); xoaPhong(phien,p[3]);
                traLoi(phien.diaChi,p[1],true,"Đã xóa phòng"); break;
            case Protocol.CHUAN_BI_VAO:
                duTruong(p,5); r=phong.datCho(phien,p[3],p[4]);
                traLoi(phien.diaChi,p[1],true,r.cacTruong()); return;
            case Protocol.HUY_VAO:
                duTruong(p,4); phien.choVao.remove(p[3]);
                traLoi(phien.diaChi,p[1],true,"Đã hủy đặt chỗ"); return;
            case Protocol.ROI_PHONG:
                duTruong(p,4); phong.kiemTraThanhVien(phien,p[3]);
                roiPhong(phien,p[3],"Bạn đã rời phòng.");
                traLoi(phien.diaChi,p[1],true,"Đã rời phòng"); break;
            default: throw new IllegalArgumentException("Lệnh không được hỗ trợ");
        }
        capNhatPhong();
    }

    private void sanSang(Phien phien,String[] p) {
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

    private void roiPhong(Phien phien,String id,String lyDo) {
        phong.roi(phien,id);
        guiSuKien(phien,Goi.ghep(Protocol.DA_ROI_PHONG,id,lyDo));
        thongBaoPhong(id,phien.nguoi.tenHienThi+" đã rời "+phong.lay(id).ten+".");
        capNhatThanhVien(id);
    }

    private void roiTatCa(Phien phien,String lyDo) {
        for (String id : List.copyOf(phien.phong)) { roiPhong(phien,id,"Phiên đã kết thúc."); }
        phien.choVao.clear(); listener.onLog(phien.nguoi.tenHienThi+" "+lyDo); capNhatPhong();
    }

    private void xoaPhong(Phien chu,String id) throws IOException {
        String ten=phong.lay(id).ten;
        phong.xoa(chu,id);
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
        duTruong(p,5); TinNhan tin=phong.themTin(phien,p[3],p[4]);
        try { phatMulticast(p[3],tin.dongGoi()); }
        catch (IOException e) { listener.onLog("Tin đã lưu; multicast lỗi, Client sẽ RESYNC: "+e.getMessage()); }
        traLoi(phien.diaChi,p[1],true,Long.toString(tin.msgId)); baoThongKe();
    }

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
    private void guiDanhSachPhong(Phien p,long ban) {
        List<Phong> ds=phong.danhSachCho(p); List<String> lo=new ArrayList<>();
        if (ds.isEmpty()) { lo.add(Goi.ghep(Protocol.DANH_SACH_PHONG,Long.toString(ban),"0","-1")); }
        for (int i=0;i<ds.size();i++) {
            lo.add(Goi.ghep(Goi.noi(new String[]{Protocol.DANH_SACH_PHONG,Long.toString(ban),Integer.toString(ds.size()),Integer.toString(i)},ds.get(i).cacTruong())));
        }
        guiLo(p,lo);
    }
    private void capNhatThanhVien(String id) {
        List<Phien> ds=phong.thanhVien(id); String ban=Long.toString(++phienBan);
        Phong r=phong.lay(id);
        for (Phien dich : ds) {
            List<String> lo=new ArrayList<>();
            for (int i=0;i<ds.size();i++) {
                Phien p=ds.get(i);
                String vai=p.nguoi.vaiTro==VaiTro.ADMIN?"ADMIN":(r.chuId.equals(p.nguoi.id)?"Chủ phòng":"Thành viên");
                lo.add(Goi.ghep(Protocol.ONLINE,id,ban,Integer.toString(ds.size()),Integer.toString(i),p.nguoi.id,p.nguoi.tenHienThi+" ("+vai+")"));
            }
            guiLo(dich,lo);
        }
    }
    private void guiMocTin(Phien p) {
        List<String> lo=p.phong.stream().map(id -> Goi.ghep(Protocol.MOC_TIN,id,Long.toString(phong.mocTin(id)))).toList();
        guiLo(p,lo);
    }
    private synchronized void quetPhien() {
        if (!dangChay) { return; }
        phong.donDatCho();
        for (Phien p : nguoiDung.xoaHetHan()) {
            guiSuKien(p,Goi.ghep(Protocol.HET_PHIEN,"Không có heartbeat quá 15 giây. Hãy đăng nhập lại."));
            roiTatCa(p,"đã mất kết nối (hết heartbeat).");
        }
    }
    private void baoThongKe() {
        listener.onThongKe(nguoiDung.tongNguoiDung(),nguoiDung.danhSach().size(),phong.tongTin());
    }

    // ===== GUI UNICAST KHONG CHAN RECEIVE =====
    // Moi lo cho ACK tung goi, tranh day hang 512 goi khi lich su/danh sach dai.
    private void guiLo(Phien phien,List<String> lo) {
        ReliableUdp kenh=udp;
        CompletableFuture<Void> viec=CompletableFuture.completedFuture(null);
        for (String duLieu : lo) { viec=viec.thenCompose(v -> kenh.gui(phien.diaChi,duLieu)); }
        viec.exceptionally(e -> { if (dangChay) { listener.onLog("Không gửi đủ cập nhật cho "+phien.nguoi.tenDangNhap); } return null; });
    }
    private void guiSuKien(Phien phien,String duLieu) { guiLo(phien,List.of(duLieu)); }
    private void traLoi(InetSocketAddress dich,String ma,boolean ok,String... noiDung) {
        if (ma.isEmpty() || udp==null) { return; }
        udp.gui(dich,Goi.ghep(Goi.noi(new String[]{Protocol.TRA_LOI,ma,ok?"OK":"ERR"},noiDung)))
            .exceptionally(e -> { if (dangChay) { listener.onLog("Không nhận ACK phản hồi từ "+dich); } return null; });
    }
    private static void duTruong(String[] p,int so) {
        if (p.length!=so) { throw new IllegalArgumentException("Số trường của gói không hợp lệ"); }
    }
    private static boolean docBoolean(String s) {
        if (!s.equals("true") && !s.equals("false")) { throw new IllegalArgumentException("Giá trị đúng/sai không hợp lệ"); }
        return Boolean.parseBoolean(s);
    }
}
