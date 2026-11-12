package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.Protocol;
import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.util.List;

/** Su kien luong nen; UI chuyen ve EDT. Ham default giu tuong thich SelfTest M1. */
public interface Listener {
    void onTinNhan(TinNhan tin,boolean laToi);
    void onHeThong(String thongBao);
    void onDanhSachOnline(List<String> danhSach);
    void onLoi(String loi);
    void onMatKetNoi(String lyDo);

    // Lich su khong tinh vao badge chua doc.
    default void onTinNhan(TinNhan tin,boolean laToi,boolean lichSu) { onTinNhan(tin,laToi); }
    default void onHeThong(String roomId,String thongBao) { onHeThong(thongBao); }
    default void onThanhVien(String roomId,List<String> danhSach) {
        if (Protocol.SANH_ID.equals(roomId)) { onDanhSachOnline(danhSach); }
    }
    default void onDanhSachPhong(List<Phong> danhSach) { }
    default void onVaoPhong(Phong phong) { }
    default void onRoiPhong(String roomId,String lyDo) { }
}
