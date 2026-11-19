package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.server.QuanLyNguoiDung.Phien;
import java.io.IOException;

/** Vai tro: Phan lenh CRUD/vao/roi phong. Ai goi: ServerCore, QuanTriServer. Goi: QuanLyPhong, ServerCore phat su kien. */
final class LenhPhongServer {
    private final ServerCore s;
    // Dung cung khoa ServerCore; khong tao socket hay luong moi.
    LenhPhongServer(ServerCore s) { this.s=s; }
    // ===== CRUD PHONG =====
    void xuLyPhong(Phien phien,String[] p) throws IOException {
        Phong r;
        switch (p[0]) {
            case Protocol.LAY_PHONG:
                duTruong(p,3); s.guiDanhSachPhong(phien,s.tangPhienBan());
                s.traLoi(phien.diaChi,p[1],true,"Đã cập nhật danh sách"); return;
            case Protocol.XOA_PHONG:
                duTruong(p,4); xoaPhong(phien,p[3]);
                s.traLoi(phien.diaChi,p[1],true,"Đã xóa phòng"); break;
            case Protocol.CHUAN_BI_VAO:
                duTruong(p,5); r=s.phong.datCho(phien,p[3],p[4]);
                s.traLoi(phien.diaChi,p[1],true,r.cacTruong()); return;
            case Protocol.HUY_VAO:
                duTruong(p,4); phien.choVao.remove(p[3]);
                s.traLoi(phien.diaChi,p[1],true,"Đã hủy đặt chỗ"); return;
            case Protocol.CHUYEN_CHU:
                duTruong(p,5); s.phong.chuyenChu(phien,p[3],p[4]);
                s.listener.onLog(phien.nguoi.tenHienThi+" chuyển chủ phòng "+s.phong.lay(p[3]).ten);
                s.capNhatThanhVien(p[3]); s.traLoi(phien.diaChi,p[1],true,"Đã chuyển quyền chủ phòng"); break;
            case Protocol.ROI_PHONG:
                duTruong(p,4); s.phong.kiemTraThanhVien(phien,p[3]); s.phong.roiChuDong(phien,p[3]);
                roiPhong(phien,p[3],"Bạn đã rời phòng.");
                s.traLoi(phien.diaChi,p[1],true,"Đã rời phòng"); break;
            case Protocol.BIET_DANH:
                xuLyBietDanh(phien,p); return;
            case Protocol.TIM_NGUOI_DUNG:
                xuLyTimNguoiDung(phien,p); return;
            case Protocol.MOI_VAO_PHONG:
                xuLyMoiVaoPhong(phien,p); return;
            case Protocol.TRA_LOI_LOI_MOI:
                xuLyTraLoiLoiMoi(phien,p); return;
            case Protocol.DS_LOI_MOI:
                xuLyDanhSachLoiMoi(phien,p); return;
            case Protocol.HUY_LOI_MOI:
                xuLyHuyLoiMoi(phien,p); return;
            default: suaTaoPhong(phien,p);
        }
        s.capNhatPhong();
    }
    // Tao/sua/trang thai duoc tach khoi vao/roi de moi ham de theo doi.
    private void suaTaoPhong(Phien phien,String[] p) throws IOException {
        Phong r;
        switch (p[0]) {
            case Protocol.TAO_PHONG:
                duTruong(p,8);
                r=s.phong.tao(phien,p[3],p[4],docBoolean(p[5]),p[6],Integer.parseInt(p[7]));
                s.traLoi(phien.diaChi,p[1],true,r.cacTruong());
                s.listener.onLog(phien.nguoi.tenHienThi+" tạo phòng "+r.ten+" — "+r.nhom+":"+r.cong); break;
            case Protocol.SUA_PHONG:
                duTruong(p,10);
                if (!p[7].equals("KEEP") && !p[7].equals("SET")) { throw new IllegalArgumentException("Chế độ mật khẩu không hợp lệ"); }
                r=s.phong.sua(phien,p[3],p[4],p[5],docBoolean(p[6]),p[7].equals("KEEP")?null:p[8],Integer.parseInt(p[9]));
                s.traLoi(phien.diaChi,p[1],true,r.cacTruong());
                s.listener.onLog(phien.nguoi.tenHienThi+" sửa phòng "+r.ten);
                s.thongBaoPhong(r.id,"Thông tin phòng đã được cập nhật."); break;
            case Protocol.TRANG_THAI_PHONG:
                duTruong(p,5); s.phong.dongMo(phien,p[3],docBoolean(p[4]));
                s.thongBaoPhong(p[3],docBoolean(p[4])?"Phòng đã mở lại.":"Phòng đã đóng; tạm dừng vào phòng và gửi tin.");
                s.listener.onLog(phien.nguoi.tenHienThi+" "+(docBoolean(p[4])?"mở":"đóng")+" phòng "+s.phong.lay(p[3]).ten);
                s.traLoi(phien.diaChi,p[1],true,"Đã cập nhật trạng thái phòng"); break;
            default: throw new IllegalArgumentException("Lệnh không được hỗ trợ");
        }
    }
    // San sang.
    void sanSang(Phien phien,String[] p) throws IOException {
        duTruong(p,4);
        boolean moi=!phien.phong.contains(p[3]);
        if (Protocol.SANH_ID.equals(p[3]) && moi) { s.phong.datCho(phien,p[3],""); }
        s.phong.xacNhanVao(phien,p[3]); phien.sanSang=true;
        if (moi) {
            s.thongBaoPhong(p[3],phien.nguoi.tenHienThi+" đã vào "+s.phong.lay(p[3]).ten+".");
            s.listener.onLog(phien.nguoi.tenHienThi+" vào "+s.phong.lay(p[3]).ten);
        }
        s.capNhatThanhVien(p[3]); s.capNhatPhong(); s.quanTri.phatMute();
        s.traLoi(phien.diaChi,p[1],true,"Đã vào phòng");
    }
    // Roi s.phong.
    void roiPhong(Phien phien,String id,String lyDo) {
        s.phong.roi(phien,id);
        s.guiSuKien(phien,Goi.ghep(Protocol.DA_ROI_PHONG,id,lyDo));
        s.thongBaoPhong(id,phien.nguoi.tenHienThi+" đã rời "+s.phong.lay(id).ten+".");
        s.capNhatThanhVien(id);
    }
    // Xoa s.phong.
    void xoaPhong(Phien chu,String id) throws IOException {
        String ten=s.phong.lay(id).ten;
        s.phong.xoa(chu,id); s.tep.xoaPhong(id);
        if (s.loiMoi!=null) { s.loiMoi.xoaTheoPhong(id); }
        for (Phien p : s.nguoiDung.danhSach()) {
            if (p.phong.contains(id)) {
                s.phong.roi(p,id); s.guiSuKien(p,Goi.ghep(Protocol.DA_ROI_PHONG,id,"Phòng «"+ten+"» đã bị xóa."));
            }
            p.choVao.remove(id);
        }
        s.listener.onLog(chu.nguoi.tenHienThi+" xóa phòng "+ten);
    }
    // ===== M3c.2: BIET DANH VA LOI MOI =====
    private void xuLyBietDanh(Phien phien,String[] p) throws IOException {
        if (p.length<5) { throw new IllegalArgumentException("Thiếu tham số đổi biệt danh."); }
        String roomId=p[3], targetUserId=p[4], nickname=p.length>=6?p[5]:"";
        String nickMoi=s.phong.datBietDanh(phien,roomId,targetUserId,nickname,s.nguoiDung);
        var target=s.nguoiDung.timTheoId(targetUserId);
        String tenTarget=target!=null?target.tenHienThi:targetUserId;
        if (nickMoi.isEmpty()) {
            s.thongBaoPhong(roomId,phien.nguoi.tenHienThi+" đã xóa biệt danh của "+tenTarget+".");
        } else {
            s.thongBaoPhong(roomId,phien.nguoi.tenHienThi+" đã đặt biệt danh cho "+tenTarget+" là «"+nickMoi+"».");
        }
        s.capNhatThanhVien(roomId);
        s.traLoi(phien.diaChi,p[1],true,nickMoi);
    }
    private void xuLyTimNguoiDung(Phien phien,String[] p) {
        String tuKhoa=p.length>=4?p[3]:"";
        var ds=s.nguoiDung.timKiem(tuKhoa);
        java.util.List<String> tra=new java.util.ArrayList<>();
        tra.add(Integer.toString(ds.size()));
        for (var u:ds) {
            boolean on=s.nguoiDung.danhSach().stream().anyMatch(sess -> sess.nguoi.id.equals(u.id));
            tra.add(u.id); tra.add(u.tenDangNhap); tra.add(u.tenHienThi); tra.add(Boolean.toString(on));
        }
        s.traLoi(phien.diaChi,p[1],true,tra.toArray(String[]::new));
    }
    private void xuLyMoiVaoPhong(Phien phien,String[] p) throws IOException {
        duTruong(p,5);
        String roomId=p[3], targetUserId=p[4];
        var bg=s.loiMoi.guiLoiMoi(phien,roomId,targetUserId,s.phong,s.nguoiDung);
        s.nguoiDung.danhSach().stream()
            .filter(sess -> sess.nguoi.id.equals(targetUserId))
            .forEach(sess -> s.guiSuKien(sess,Goi.ghep(Protocol.SU_KIEN_LOI_MOI,bg.id(),bg.roomId(),s.phong.lay(bg.roomId()).ten,phien.nguoi.tenHienThi)));
        s.traLoi(phien.diaChi,p[1],true,"Đã gửi lời mời tham gia phòng.");
    }
    private void xuLyTraLoiLoiMoi(Phien phien,String[] p) throws IOException {
        duTruong(p,5);
        String inviteId=p[3];
        boolean dongY=Boolean.parseBoolean(p[4]);
        var bg=s.loiMoi.traLoiLoiMoi(phien,inviteId,dongY,s.phong);
        if (dongY) {
            s.thongBaoPhong(bg.roomId(),phien.nguoi.tenHienThi+" đã tham gia phòng qua lời mời.");
            s.capNhatThanhVien(bg.roomId());
            s.capNhatPhong();
            s.traLoi(phien.diaChi,p[1],true,bg.roomId(),s.phong.lay(bg.roomId()).ten);
        } else {
            s.traLoi(phien.diaChi,p[1],true,"Đã từ chối lời mời.");
        }
    }
    private void xuLyDanhSachLoiMoi(Phien phien,String[] p) {
        var ds=s.loiMoi.danhSachLoiMoiCho(phien.nguoi.id,s.phong,s.nguoiDung);
        java.util.List<String> tra=new java.util.ArrayList<>();
        tra.add(Integer.toString(ds.size()));
        for (var lm:ds) {
            tra.add(lm.inviteId()); tra.add(lm.roomId()); tra.add(lm.tenPhong());
            tra.add(lm.nguoiMoiId()); tra.add(lm.tenNguoiMoi()); tra.add(Long.toString(lm.thoiGianTao()));
        }
        s.traLoi(phien.diaChi,p[1],true,tra.toArray(String[]::new));
    }
    private void xuLyHuyLoiMoi(Phien phien,String[] p) throws IOException {
        duTruong(p,4);
        s.loiMoi.huyLoiMoi(phien,p[3],s.phong);
        s.traLoi(phien.diaChi,p[1],true,"Đã hủy lời mời.");
    }
    // Kiem tra so truong truoc khi truy cap mang.
    private static void duTruong(String[] g,int n) { if (g.length!=n) { throw new IllegalArgumentException("Gói phòng không hợp lệ."); } }
    // Doc boolean nghiem ngat, khong tin gia tri la.
    private static boolean docBoolean(String b) { if (!b.equals("true") && !b.equals("false")) { throw new IllegalArgumentException("Giá trị loại phòng không hợp lệ."); } return Boolean.parseBoolean(b); }
}
