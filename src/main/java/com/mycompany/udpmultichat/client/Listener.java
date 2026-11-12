package com.mycompany.udpmultichat.client;

import com.mycompany.udpmultichat.common.model.TinNhan;
import java.util.List;

/** Su kien chay tu luong nen; UI phai dung SwingUtilities.invokeLater. */
public interface Listener {
    void onTinNhan(TinNhan tin, boolean laToi);
    void onHeThong(String thongBao);
    void onDanhSachOnline(List<String> danhSach);
    void onLoi(String loi);
    void onMatKetNoi(String lyDo);
}
