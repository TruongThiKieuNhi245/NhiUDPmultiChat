package com.mycompany.udpmultichat.common.model;

/** Ban sao phong cong khai; khong chua salt hay hash mat khau phong. */
public final class Phong {
    /** Thanh vien cua phong, tach biet voi trang thai online. */
    public record ThanhVien(String id,String ten,String vaiTro,boolean online) { }
    public static final int SO_TRUONG = 13;
    public final String id, ten, moTa, maPhong, chuId, nhom, ngayTao;
    public final int cong, gioiHan, soThanhVien;
    public final boolean congKhai, coMatKhau, dangMo;

    public Phong(String id, String ten, String moTa, String maPhong, String chuId,
                 String nhom, int cong, int gioiHan, boolean congKhai, boolean coMatKhau,
                 boolean dangMo, int soThanhVien, String ngayTao) {
        this.id=id; this.ten=ten; this.moTa=moTa; this.maPhong=maPhong; this.chuId=chuId;
        this.nhom=nhom; this.cong=cong; this.gioiHan=gioiHan; this.congKhai=congKhai;
        this.coMatKhau=coMatKhau; this.dangMo=dangMo; this.soThanhVien=soThanhVien; this.ngayTao=ngayTao;
    }

    // Chuyen thanh truong phang, tranh tang nhieu lop escape khi gui UDP.
    public String[] cacTruong() {
        return new String[] {id, ten, moTa, maPhong, chuId, nhom, Integer.toString(cong),
            Integer.toString(gioiHan), Boolean.toString(congKhai), Boolean.toString(coMatKhau),
            Boolean.toString(dangMo), Integer.toString(soThanhVien), ngayTao};
    }

    // Doc DTO tu goi danh sach hoac phan hoi vao phong.
    public static Phong tuTruong(String[] p, int v) {
        if (p.length < v + SO_TRUONG) { throw new IllegalArgumentException("Thông tin phòng không đầy đủ"); }
        return new Phong(p[v],p[v+1],p[v+2],p[v+3],p[v+4],p[v+5],Integer.parseInt(p[v+6]),
            Integer.parseInt(p[v+7]),Boolean.parseBoolean(p[v+8]),Boolean.parseBoolean(p[v+9]),
            Boolean.parseBoolean(p[v+10]),Integer.parseInt(p[v+11]),p[v+12]);
    }

    // Tao ban sao voi so thanh vien online hien tai.
    public Phong voiSoThanhVien(int soLuong) {
        return new Phong(id,ten,moTa,maPhong,chuId,nhom,cong,gioiHan,congKhai,coMatKhau,dangMo,soLuong,ngayTao);
    }
}
