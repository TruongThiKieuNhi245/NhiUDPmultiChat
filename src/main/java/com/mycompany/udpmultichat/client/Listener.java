package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.NguoiDung;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.util.List;

/** Vai tro: Su kien luong nen; UI chuyen ve EDT. Ham default giu tuong thich SelfTest M1. Ai goi: ClientCore, ui. Goi: cac ham callback do UI cai dat. */
public interface Listener {
    // ===== VAI TRO VA CAC THAO TAC =====
    // Phat truoc moi su kien phong sau dang nhap va khi ho so phien thay doi.
    default void onNguoiDung(NguoiDung nguoi) { }
    // Snapshot ho so bat ky user chung phong, ke ca khi anh cache vua tai xong.
    default void onHoSo(NguoiDung nguoi) { }
    // Nhan tin da duoc kiem tra danh tinh va phong.
    void onTinNhan(TinNhan tin,boolean laToi);
    // Nhan thong bao he thong qua Listener.
    void onHeThong(String thongBao);
    // Nhan danh sach online theo Listener.
    void onDanhSachOnline(List<String> danhSach);
    // Nhan loi de hien thi hoac ghi nhan.
    void onLoi(String loi);
    // Nhan trang thai phien da ngat.
    void onMatKetNoi(String lyDo);

    // Thoi han mute wall-clock, 0 la da go cam.
    default void onCamChat(String room,long den) { }
    // Thong bao noi bat tu admin, khac thong bao vao/ra phong.
    default void onThongBaoQuanTri(String text) { }
    // Tien do tep phat tu worker, UI tu chuyen ve EDT.
    default void onTep(com.mycompany.udpmultichat.common.TepTienDo tienDo) { }
    // Anh xem truoc da nam trong cache RAM.
    default void onAnhTep(String room,String fileId) { }
    // Trang thai hang gui cuc bo: WAIT/SENT/ERROR/REMOVE.
    default void onTrangThaiGui(TinNhan tin,String trangThai,String lyDo) { }
    // Tombstone chi chua roomId va msgId.
    default void onThuHoi(String room,long id) { }
    // Dang go trong phong; UI tu tat sau bon giay.
    default void onDangGo(String room,String user,String ten) { }
    // Lich su khong tinh vao badge chua doc.
    default void onTinNhan(TinNhan tin,boolean laToi,boolean lichSu) { onTinNhan(tin,laToi); }
    // Nhan thong bao he thong qua Listener.
    default void onHeThong(String roomId,String thongBao) { onHeThong(thongBao); }
    // On thanh vien.
    default void onThanhVien(String roomId,List<String> danhSach) {
        if (Protocol.SANH_ID.equals(roomId)) { onDanhSachOnline(danhSach); }
    }
    // On ket noi.
    default void onKetNoi(boolean sanSang,String thongBao) { }
    // On thanh vien chi tiet.
    default void onThanhVienChiTiet(String roomId,List<Phong.ThanhVien> ds) {
        onThanhVien(roomId,ds.stream().filter(Phong.ThanhVien::online).map(Phong.ThanhVien::ten).toList());
    }
    // On danh sach phong.
    default void onDanhSachPhong(List<Phong> danhSach) { }
    // On vao phong.
    default void onVaoPhong(Phong phong) { }
    // On roi phong.
    default void onRoiPhong(String roomId,String lyDo) { }
}
