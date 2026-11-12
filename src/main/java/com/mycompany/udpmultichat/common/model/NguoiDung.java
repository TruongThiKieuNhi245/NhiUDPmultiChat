package com.mycompany.udpmultichat.common.model;

/** Ban sao ho so cong khai, khong chua salt, hash hay token. */
public final class NguoiDung {
    public final String id, tenDangNhap, tenHienThi, ngayTao;
    public final VaiTro vaiTro;

    public NguoiDung(String id, String tenDangNhap, String tenHienThi, VaiTro vaiTro, String ngayTao) {
        this.id = id;
        this.tenDangNhap = tenDangNhap;
        this.tenHienThi = tenHienThi;
        this.vaiTro = vaiTro;
        this.ngayTao = ngayTao;
    }

    // Chuyen ho so thanh cac truong de gui, khong gui thong tin bi mat.
    public String[] cacTruong() {
        return new String[] { id, tenDangNhap, tenHienThi, vaiTro.name(), ngayTao };
    }

    // Doc ho so tu vi tri bat dau trong goi phan hoi.
    public static NguoiDung tuTruong(String[] truong, int viTri) {
        return new NguoiDung(truong[viTri], truong[viTri + 1], truong[viTri + 2],
                VaiTro.valueOf(truong[viTri + 3]), truong[viTri + 4]);
    }
}
