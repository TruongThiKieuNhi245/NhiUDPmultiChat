package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Vai tro: Anh M5 tu component that, khong ket noi mang. Ai goi: UiPreview. Goi: QuanTriFrame, FormQuanTri, ChatMulticastUI. */
final class QuanTriPreview {
    // Khong khoi tao lop tien ich.
    private QuanTriPreview() { }
    // ===== NAM TAB VA CAC FORM =====
    static void xuat() throws IOException {
        QuanTriBangKiemTra.chay();
        AnhChupQuanTri a=mau(); QuanTriFrame f=new QuanTriFrame(null,true);
        try {
            f.capNhat(a); String[] ten={"44-quan-tri-tong-quan.png","45-quan-tri-nguoi-dung.png","46-quan-tri-phong.png","47-quan-tri-nhat-ky.png","48-quan-tri-bao-cao.png"};
            for (int i=0;i<ten.length;i++) { f.xemTab(i); UiPreview.xuat(f.getRootPane(),ten[i],1280,860); }
            f.trangThaiMau("Đang tải dữ liệu...",false,false); UiPreview.xuat(f.getRootPane(),"58-quan-tri-dang-tai.png",1280,860);
            f.trangThaiMau("Chưa có dữ liệu",true,false); UiPreview.xuat(f.getRootPane(),"59-quan-tri-rong.png",1280,860);
            f.trangThaiMau("Không tải được. Kiểm tra Server rồi bấm Làm mới để thử lại.",true,true); UiPreview.xuat(f.getRootPane(),"60-quan-tri-loi-thu-lai.png",1280,860);
        } finally { f.dispose(); }
        UiPreview.xuatForm(FormQuanTri.nguoiDung(true,List.of()),"49-them-nguoi-dung.png");
        UiPreview.xuatForm(FormQuanTri.nguoiDung(false,a.lay("USERS").get(1)),"50-sua-nguoi-dung.png");
        UiPreview.xuatForm(QuanTriFrame.matKhauTamPanel("9fA7bT2cQ8"),"51-mat-khau-tam.png");
        FormQuanTri xoa=new FormQuanTri("Xóa tài khoản",true).moTa("minhan","Ngắt phiên, gỡ khỏi mọi phòng và xóa avatar. Tin cũ giữ tên Người dùng đã xóa.").text("ten","Gõ lại tên đăng nhập","");
        UiPreview.xuatForm(xoa,"52-xac-nhan-xoa.png");
        FormQuanTri loi=FormQuanTri.nguoiDung(true,List.of());
        int cao=loi.getPreferredSize().height;
        loi.loiO("login","Tên đăng nhập đã tồn tại."); loi.loiO("ten","Tên hiển thị cần 2–30 ký tự.");
        if (loi.getPreferredSize().height<=cao) { throw new AssertionError("Form admin cache chieu cao cu khi hien loi"); }
        UiPreview.xuatForm(loi,"57-them-user-co-loi.png");
        UiPreview.xuatForm(KiemDuyetUI.menu(l -> { }),"53-menu-kiem-duyet.png");
        xuatChat();
        Path docs=Path.of("docs","screenshots","bao-cao-mau");
        com.mycompany.udpmultichat.server.XuatBaoCao.xuat(a,docs,false);
        System.out.println("PASS UiPreview M5: 5 tab, form user, kiem duyet, mute, kick va broadcast; HTML mau tu chua");
    }
    // Dung su kien Listener that de ve mute va thong bao.
    private static void xuatChat() throws IOException {
        ChatMulticastUI f=new ChatMulticastUI(); long now=System.currentTimeMillis();
        try {
            var n=new NguoiDung("an","minhan","Minh An",VaiTro.THANH_VIEN,"2026-11-14");
            var p=new Phong("sanh","Sảnh chung","","SANH01","HE_THONG","239.255.42.1",9001,500,true,false,true,3,"2026-11-14").voiThongTin(true,"Hệ thống",3);
            f.duLieuMau(n,List.of(p)); f.onThanhVienChiTiet("sanh",List.of(new Phong.ThanhVien("admin","admin","Kiều Nhi","ADMIN",true),new Phong.ThanhVien("an","minhan","Minh An","THANH_VIEN",true),new Phong.ThanhVien("ha","thuha","Thu Hà","THANH_VIEN",false)));
            f.onTinNhan(new TinNhan(1,"sanh","ha","Thu Hà","Hẹn mọi người buổi học tiếp theo!",now),false);
            f.onCamChat("sanh",now+3600000); UiPreview.xuat(f.getRootPane(),"54-o-soan-bi-mute.png",1240,800);
            if (UiPreview.demNhan(f.getRootPane(),"Bạn bị cấm chat đến "+java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM").format(Instant.ofEpochMilli(now+3600000).atZone(ZoneId.systemDefault())))!=1) { throw new AssertionError("Preview thieu ly do mute"); }
            f.onCamChat("sanh",0); f.toast(ThongBao.Muc.CANH_BAO,"Bạn đã bị mời ra khỏi phòng Bài tập mạng."); UiPreview.xuat(f.getRootPane(),"55-toast-kick.png",1240,800);
            f.onTinNhan(new TinNhan(2,"sanh","admin","Quản trị viên","Buổi thực hành bắt đầu lúc 14:00. Mọi người chuẩn bị bài nhé!",now,TinNhan.taoMa(),"Y"),false);
            f.onThongBaoQuanTri("Buổi thực hành bắt đầu lúc 14:00."); UiPreview.xuat(f.getRootPane(),"56-thong-bao-quan-tri.png",1240,800);
        } finally { f.dispose(); }
    }
    // Snapshot cung schema Server, khong chen token/hash vao du lieu mau.
    private static AnhChupQuanTri mau() {
        Map<String,List<List<String>>> b=new LinkedHashMap<>(); String date="2026-11-14T07:00:00Z";
        b.put("USERS",List.of(user("admin","admin","Kiều Nhi","ADMIN","Online","2","12",date),user("an","minhan","Minh An","THANH_VIEN","Online","2","18",date),user("ha","thuha","Thu Hà","THANH_VIEN","Offline","3","7",date),user("khoa","khoa","Tài khoản bị khóa","THANH_VIEN","Bị khóa","2","3",date)));
        b.put("ROOMS",List.of(room("sanh","SANH01","Sảnh chung","Công khai","Mở","Hệ thống","4","20",date),room("baitap","NET123","Bài tập mạng","Công khai","Mở","minhan","3","16",date),room("rieng","ABC789","Nhóm báo cáo","Riêng tư","Đóng","thuha","2","4",date)));
        b.put("STATS",List.of(List.of("Tổng tài khoản","4"),List.of("Đang online","2"),List.of("Bị khóa","1"),List.of("Tổng phòng","3"),List.of("Phòng mở","2"),List.of("Phòng đóng","1"),List.of("Tổng tin nhắn","40"),List.of("Tổng tệp","2"),List.of("Dung lượng tệp","6291456")));
        b.put("DAYS",List.of(List.of(LocalDate.now().minusDays(1).toString(),"Sảnh chung","12","1","2","0"),List.of(LocalDate.now().toString(),"Sảnh chung","8","0","0","0"),List.of(LocalDate.now().toString(),"Bài tập mạng","16","1","4","1"),List.of(LocalDate.now().toString(),"Nhóm báo cáo","4","0","0","0")));
        b.put("MEMBERS",List.of(member("sanh","Sảnh chung","admin","admin","Kiều Nhi","ADMIN",true,date,8),member("sanh","Sảnh chung","an","minhan","Minh An","THANH_VIEN",true,date,8),member("sanh","Sảnh chung","ha","thuha","Thu Hà","THANH_VIEN",false,date,3),member("sanh","Sảnh chung","khoa","khoa","Tài khoản bị khóa","THANH_VIEN",false,date,1),
            member("baitap","Bài tập mạng","admin","admin","Kiều Nhi","ADMIN",true,date,4),member("baitap","Bài tập mạng","an","minhan","Minh An","CHU_PHONG",true,date,10),member("baitap","Bài tập mạng","ha","thuha","Thu Hà","THANH_VIEN",false,date,2),member("rieng","Nhóm báo cáo","ha","thuha","Thu Hà","CHU_PHONG",false,date,2),member("rieng","Nhóm báo cáo","khoa","khoa","Tài khoản bị khóa","THANH_VIEN",false,date,2)));
        b.put("AUDIT",List.of(List.of(date,"admin","USER_LOCK","khoa","Kiểm tra tài khoản"),List.of(date,"admin","MUTE","an","Bài tập mạng · 5 phút"),List.of(date,"admin","BROADCAST","Toàn hệ thống","Thông báo buổi học"),List.of(date,"ANONYMOUS","LOGIN_FAILURE","192.168.1.30","3 lần đăng nhập không thành công"),List.of(date,"admin","EXPORT_REPORT","reports","Snapshot nhất quán")));
        return new AnhChupQuanTri(System.currentTimeMillis(),b);
    }
    // Mot user mau co du cot thong ke va thong tin ho so.
    private static List<String> user(String id,String login,String ten,String vai,String state,String phong,String tin,String date) {
        boolean coTep=Set.of("admin","an").contains(id);
        return List.of(id,login,ten,vai,state,phong,tin,date,date,coTep?"1":"0",coTep?"3145728":"0","Sinh viên lập trình mạng");
    }
    // Membership mau nhat quan voi so lieu user/phong, gom ca nguoi offline.
    private static List<String> member(String room,String ten,String id,String login,String hien,String vai,boolean online,String date,int tin) {
        return List.of(room,ten,id,login,hien,vai,""+online,date,""+tin);
    }
    // Mot phong mau co du thong tin chinh sua.
    private static List<String> room(String id,String code,String ten,String loai,String state,String chu,String nguoi,String tin,String date) {
        boolean coTep=!id.equals("rieng");
        return List.of(id,code,ten,loai,state,chu,nguoi,tin,coTep?"1":"0",coTep?"3145728":"0",date,"Phòng trao đổi bài tập",chu,"20");
    }
}
