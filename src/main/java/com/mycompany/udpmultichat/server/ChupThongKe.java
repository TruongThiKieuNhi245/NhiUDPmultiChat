package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import java.time.*;
import java.util.*;

/** Vai tro: Chup du lieu nhat quan khong ghi dia. Ai goi: WebAdministration. Goi: QuanLyNguoiDung, QuanLyPhong, NhatKy. */
final class ChupThongKe {
    // Khong khoi tao lop tien ich.
    private ChupThongKe() { }
    // ===== SNAPSHOT BANG =====
    static AnhChupQuanTri chup(QuanLyNguoiDung users,QuanLyPhong rooms,java.util.function.BiFunction<String,String,long[]> tep,NhatKy audit) throws java.io.IOException {
        Map<String,List<List<String>>> bang=new LinkedHashMap<>(); var nguoi=users.tatCa(); var phong=rooms.tatCa();
        var tin=rooms.tinNhan().tatCa(); var online=users.danhSach().stream().map(p -> p.nguoi.id).toList();
        Map<String,Long> userCounts=new HashMap<>(),roomCounts=new HashMap<>(),memberCounts=new HashMap<>();
        for(var t:tin){userCounts.merge(t.userId,1L,Long::sum);roomCounts.merge(t.roomId,1L,Long::sum);memberCounts.merge(t.roomId+":"+t.userId,1L,Long::sum);}
        bang.put("USERS",nguoi.stream().map(n -> user(n,users,rooms,tep,userCounts,online)).toList());
        bang.put("ROOMS",phong.stream().map(p -> room(p,nguoi,rooms,tep,roomCounts)).toList());
        List<List<String>> tv=new ArrayList<>();
        for (Phong p:phong) { for (NguoiDung n:nguoi) { if (rooms.laThanhVien(n.id,p.id)) { tv.add(List.of(p.id,p.ten,n.id,n.tenDangNhap,n.tenHienThi,n.vaiTro==VaiTro.ADMIN?"ADMIN":p.chuId.equals(n.id)?"CHU_PHONG":"THANH_VIEN",Boolean.toString(rooms.thanhVien(p.id).stream().anyMatch(s -> s.nguoi.id.equals(n.id))),rooms.ngayVao(p.id,n.id),Long.toString(memberCounts.getOrDefault(p.id+":"+n.id,0L)))); } } }
        bang.put("MEMBERS",tv); bang.put("DAYS",ngay(tin,phong)); bang.put("AUDIT",audit.doc());
        long[] file=tep.apply(null,null);
        bang.put("STATS",List.of(List.of("Tổng tài khoản",""+nguoi.size()),List.of("Đang online",""+online.size()),List.of("Bị khóa",""+nguoi.stream().filter(n -> users.biKhoa(n.id)).count()),List.of("Tổng phòng",""+phong.size()),List.of("Phòng mở",""+phong.stream().filter(p -> p.dangMo).count()),List.of("Phòng đóng",""+phong.stream().filter(p -> !p.dangMo).count()),List.of("Tổng tin nhắn",""+tin.size()),List.of("Tổng tệp",""+file[0]),List.of("Dung lượng tệp",""+file[1])));
        return new AnhChupQuanTri(System.currentTimeMillis(),bang);
    }
    // User thong ke theo id bat bien, khong dua salt/hash vao snapshot.
    private static List<String> user(NguoiDung n,QuanLyNguoiDung users,QuanLyPhong rooms,java.util.function.BiFunction<String,String,long[]> tep,Map<String,Long> counts,List<String> online) {
        long[] f=tep.apply(null,n.id);
        return List.of(n.id,n.tenDangNhap,n.tenHienThi,n.vaiTro.name(),users.biKhoa(n.id)?"Bị khóa":online.contains(n.id)?"Online":"Offline",""+rooms.tatCa().stream().filter(p -> rooms.laThanhVien(n.id,p.id)).count(),""+counts.getOrDefault(n.id,0L),n.ngayTao,users.lanCuoi(n.id),""+f[0],""+f[1],n.gioiThieu);
    }
    // Phong thong ke ca thanh vien offline da tham gia.
    private static List<String> room(Phong p,List<NguoiDung> users,QuanLyPhong rooms,java.util.function.BiFunction<String,String,long[]> tep,Map<String,Long> counts) {
        long[] f=tep.apply(p.id,null); String chu=users.stream().filter(n -> n.id.equals(p.chuId)).map(n -> n.tenDangNhap).findFirst().orElse("Hệ thống");
        return List.of(p.id,p.maPhong,p.ten,p.congKhai?"Công khai":"Riêng tư",p.dangMo?"Mở":"Đóng",chu,""+rooms.soThanhVienBenVung(p.id),""+counts.getOrDefault(p.id,0L),""+f[0],""+f[1],p.ngayTao,p.moTa,p.chuId,""+p.gioiHan);
    }
    // Tat ca ngay co tin; UI loc bay ngay, bao cao giu toan bo.
    private static List<List<String>> ngay(List<TinNhan> tin,List<Phong> phong) {
        Map<String,long[]> ds=new TreeMap<>();
        for (TinNhan t:tin) {
            String ngay=Instant.ofEpochMilli(t.thoiGian).atZone(ZoneId.systemDefault()).toLocalDate().toString(); long[] n=ds.computeIfAbsent(ngay+"|"+t.roomId,k -> new long[4]);
            n[0]++; if (t.loai.equals("F")) { n[1]++; } if (t.loai.equals("S")) { n[2]++; } if (t.thuHoi) { n[3]++; }
        }
        List<List<String>> kq=new ArrayList<>();
        ds.forEach((k,n) -> { String[] p=k.split("\\|",2); String ten=phong.stream().filter(r -> r.id.equals(p[1])).map(r -> r.ten).findFirst().orElse(p[1]); kq.add(List.of(p[0],ten,""+n[0],""+n[1],""+n[2],""+n[3])); }); return kq;
    }
}
