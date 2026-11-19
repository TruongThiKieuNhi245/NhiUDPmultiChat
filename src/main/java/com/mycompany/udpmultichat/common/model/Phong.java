package com.mycompany.udpmultichat.common.model;

/** Vai tro: Ban sao phong cong khai; khong chua salt hay hash mat khau phong. Ai goi: ClientCore, QuanLyPhong, ui. Goi: khong goi lop khac. */
public final class Phong {
    // ===== VAI TRO VA CAC THAO TAC =====
    /** Thanh vien cua phong, co biet danh rieng theo nhom va trang thai online. */
    public record ThanhVien(String id,String tenDangNhap,String ten,String vaiTro,boolean online,String bietDanh) {
        // Constructor 5 tham so de tuong thich nguon cu.
        public ThanhVien(String id,String tenDangNhap,String ten,String vaiTro,boolean online) {
            this(id,tenDangNhap,ten,vaiTro,online,"");
        }
        // Nhan dien tai khoan, khong so sanh ten hien thi co the trung nhau.
        public boolean laToi(String taiKhoan) { return !taiKhoan.isBlank() && tenDangNhap.equalsIgnoreCase(taiKhoan); }
        // Uu tien biet danh trong phong, fallback ve ten hien thi goc.
        public String tenHienThi() { return (bietDanh!=null && !bietDanh.isBlank())?bietDanh:ten; }
        // Kiem tra thanh vien co biet danh rieng trong phong khong.
        public boolean coBietDanh() { return bietDanh!=null && !bietDanh.isBlank(); }
    }
    public static final int SO_TRUONG = 13;
    public final String id, ten, moTa, maPhong, chuId, nhom, ngayTao;
    public final int cong, gioiHan, soThanhVien;
    public final boolean congKhai, coMatKhau, dangMo;
    public final boolean daThamGia;
    public final String tenChu;
    public final int tongThanhVien;

    // Tao DTO phong tu du lieu Server, khong chua mat khau.
    public Phong(String id, String ten, String moTa, String maPhong, String chuId,
                 String nhom, int cong, int gioiHan, boolean congKhai, boolean coMatKhau,
                 boolean dangMo, int soThanhVien, String ngayTao) {
        this(id,ten,moTa,maPhong,chuId,nhom,cong,gioiHan,congKhai,coMatKhau,dangMo,soThanhVien,ngayTao,false,chuId,soThanhVien);
    }

    // Co tham gia la thong tin rieng tung Client, khong doi schema rooms.csv.
    private Phong(String id,String ten,String moTa,String maPhong,String chuId,String nhom,int cong,int gioiHan,
                  boolean congKhai,boolean coMatKhau,boolean dangMo,int soThanhVien,String ngayTao,boolean daThamGia,String tenChu,int tongThanhVien) {
        this.id=id; this.ten=ten; this.moTa=moTa; this.maPhong=maPhong; this.chuId=chuId;
        this.nhom=nhom; this.cong=cong; this.gioiHan=gioiHan; this.congKhai=congKhai;
        this.coMatKhau=coMatKhau; this.dangMo=dangMo; this.soThanhVien=soThanhVien; this.ngayTao=ngayTao;
        this.daThamGia=daThamGia;
        this.tenChu=tenChu; this.tongThanhVien=tongThanhVien;
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

    // Du lieu rieng cho UI di kem snapshot, khong luu vao CSV phong.
    public Phong voiThongTin(boolean thamGia,String chu,int tong) {
        return new Phong(id,ten,moTa,maPhong,chuId,nhom,cong,gioiHan,congKhai,coMatKhau,dangMo,soThanhVien,ngayTao,thamGia,chu,tong);
    }
}
