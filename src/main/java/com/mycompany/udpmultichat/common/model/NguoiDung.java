package com.mycompany.udpmultichat.common.model;

/** Vai tro: Ho so cong khai, khong chua bi mat. Ai goi: Core, KhoLuuTru, UI. Goi: VaiTro. */
public final class NguoiDung {
    // ===== HO SO BAT BIEN =====
    public static final int SO_TRUONG=8;
    public final String id,tenDangNhap,tenHienThi,ngayTao,gioiThieu,anhHash;
    public final VaiTro vaiTro;
    public final boolean buocDoiMatKhau;
    // Giu constructor gon cho tai khoan moi va du lieu cu.
    public NguoiDung(String id,String ten,String hienThi,VaiTro vai,String ngay) {
        this(id,ten,hienThi,vai,ngay,"","",false);
    }
    // Tao snapshot; ten dang nhap va id khong doi khi sua ho so.
    public NguoiDung(String id,String ten,String hienThi,VaiTro vai,String ngay,String gt,String anh,boolean buoc) {
        this.id=id; tenDangNhap=ten; tenHienThi=hienThi; vaiTro=vai; ngayTao=ngay;
        gioiThieu=gt; anhHash=anh; buocDoiMatKhau=buoc;
    }
    // Chuyen snapshot thanh goi text UTF-8.
    public String[] cacTruong() {
        return new String[]{id,tenDangNhap,tenHienThi,vaiTro.name(),ngayTao,gioiThieu,anhHash,Boolean.toString(buocDoiMatKhau)};
    }
    // Doc snapshot tu vi tri bat dau.
    public static NguoiDung tuTruong(String[] p,int i) {
        return new NguoiDung(p[i],p[i+1],p[i+2],VaiTro.valueOf(p[i+3]),p[i+4],p[i+5],p[i+6],Boolean.parseBoolean(p[i+7]));
    }
    // Tao ho so moi, giu nguyen danh tinh va quyen.
    public NguoiDung voiHoSo(String ten,String gt,String anh,boolean buoc) {
        return new NguoiDung(id,tenDangNhap,ten,vaiTro,ngayTao,gt,anh,buoc);
    }
}
