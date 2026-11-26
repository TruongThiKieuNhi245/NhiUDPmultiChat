package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** Vai tro: Bao cao tu snapshot, CSV BOM an toan va HTML tu chua. Ai goi: QuanTriServer, ServerMain, AdminSelfTest. Goi: Files, ZipOutputStream. */
public final class XuatBaoCao {
    // Khong khoi tao lop tien ich.
    private XuatBaoCao() { }
    // ===== XUAT KHONG GIU KHOA SERVER =====
    public static Path xuat(AnhChupQuanTri a,Path reports,boolean audit) throws IOException {
        Files.createDirectories(reports); String ten="baocao_"+DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path thu=Files.createTempDirectory(reports,".baocao-tam-");
        try {
        if (audit) { csv(thu.resolve("nhatky.csv"),List.of("Thời gian","Người thực hiện","Hành động","Đối tượng","Chi tiết"),a.lay("AUDIT")); }
        else {
            csv(thu.resolve("nguoidung.csv"),List.of("Tên đăng nhập","Tên hiển thị","Vai trò","Trạng thái","Ngày tạo","Lần đăng nhập cuối","Số phòng","Số tin","Số tệp","Dung lượng"),chon(a.lay("USERS"),1,2,3,4,7,8,5,6,9,10));
            csv(thu.resolve("phong.csv"),List.of("Mã","Tên","Loại","Trạng thái","Chủ phòng","Số thành viên","Số tin","Số tệp","Dung lượng","Ngày tạo"),chon(a.lay("ROOMS"),1,2,3,4,5,6,7,8,9,10));
            csv(thu.resolve("thanhvien_theophong.csv"),List.of("Phòng","Người dùng","Vai trò","Online","Ngày vào","Số tin"),chon(a.lay("MEMBERS"),1,3,5,6,7,8));
            csv(thu.resolve("tin_theo_ngay.csv"),List.of("Ngày","Phòng","Số tin","Số tệp","Số sticker","Số tin thu hồi"),a.lay("DAYS"));
            ghi(thu.resolve("tong_hop.html"),html(a));
        }
            int so=0;
            while (true) {
                Path dich=reports.resolve(ten+(so==0?"":"_"+so));
                try { return Files.move(thu,dich); }
                catch (FileAlreadyExistsException e) { so++; }
            }
        } catch (IOException | RuntimeException e) {
            donTam(thu,e); throw e;
        }
    }
    // Xuat tuy chon cac nhom du lieu ra thu muc theo lua chon cua admin.
    public static Path xuatTuyChon(AnhChupQuanTri a,Path reports,boolean coUsers,boolean coRooms,boolean coAudit,boolean coHtml,boolean coCsv) throws IOException {
        Files.createDirectories(reports); String ten="baocao_"+DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path thu=Files.createTempDirectory(reports,".baocao-tam-");
        try {
            if (coCsv && coUsers) { csv(thu.resolve("nguoidung.csv"),List.of("Tên đăng nhập","Tên hiển thị","Vai trò","Trạng thái","Ngày tạo","Lần đăng nhập cuối","Số phòng","Số tin","Số tệp","Dung lượng"),chon(a.lay("USERS"),1,2,3,4,7,8,5,6,9,10)); }
            if (coCsv && coRooms) {
                csv(thu.resolve("phong.csv"),List.of("Mã","Tên","Loại","Trạng thái","Chủ phòng","Số thành viên","Số tin","Số tệp","Dung lượng","Ngày tạo"),chon(a.lay("ROOMS"),1,2,3,4,5,6,7,8,9,10));
                csv(thu.resolve("thanhvien_theophong.csv"),List.of("Phòng","Người dùng","Vai trò","Online","Ngày vào","Số tin"),chon(a.lay("MEMBERS"),1,3,5,6,7,8));
            }
            if (coCsv && coAudit) { csv(thu.resolve("nhatky.csv"),List.of("Thời gian","Người thực hiện","Hành động","Đối tượng","Chi tiết"),a.lay("AUDIT")); }
            if (coCsv && (coRooms || coUsers)) { csv(thu.resolve("tin_theo_ngay.csv"),List.of("Ngày","Phòng","Số tin","Số tệp","Số sticker","Số tin thu hồi"),a.lay("DAYS")); }
            if (coHtml) { ghi(thu.resolve("tong_hop.html"),htmlTuyChon(a,coUsers,coRooms,coAudit,true)); }
            int so=0;
            while (true) {
                Path dich=reports.resolve(ten+(so==0?"":"_"+so));
                try { return Files.move(thu,dich); }
                catch (FileAlreadyExistsException e) { so++; }
            }
        } catch (IOException | RuntimeException e) { donTam(thu,e); throw e; }
    }
    // Chi don staging vua tao khi xuat loi, khong cham bao cao cu.
    private static void donTam(Path thu,Throwable loi) {
        try (var ds=Files.walk(thu)) {
            for (Path p:ds.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(p); }
        } catch (IOException e) { loi.addSuppressed(e); }
    }
    // Chon cot theo schema snapshot, khong dua id/token noi bo vao bao cao.
    private static List<List<String>> chon(List<List<String>> ds,int... cot) { return ds.stream().map(r -> Arrays.stream(cot).mapToObj(r::get).toList()).toList(); }
    // BOM UTF-8 va CRLF; moi truong duoc bao nhay kep RFC 4180.
    public static void csv(Path p,List<String> cot,List<List<String>> dong) throws IOException {
        StringBuilder s=new StringBuilder("\ufeff").append(dongCsv(cot)); for (var r:dong) { s.append(dongCsv(r)); } ghi(p,s.toString());
    }
    // Cong thuc Excel bi vo hieu hoa, ke ca co khoang trang/tab phia truoc.
    private static String dongCsv(List<String> row) { return String.join(",",row.stream().map(XuatBaoCao::oCsv).toList())+"\r\n"; }
    // Escape dau nhay kep va chen nhay don truoc cong thuc, khong lam mat tieng Viet.
    public static String oCsv(String v) {
        String n=v.stripLeading(); if (!n.isEmpty() && "=+-@".indexOf(n.charAt(0))>=0) { v="'"+v; }
        return "\""+v.replace("\"","\"\"")+"\"";
    }
    // Tat ca chuoi nguoi dung phai di qua ham nay truoc khi chen HTML.
    public static String e(String v) { return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    // Dinh dang thoi diem xuat bao cao ro rang ngay thang gio phut giay, khong timezone la.
    private static String thoiDiem(AnhChupQuanTri a) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(Instant.ofEpochMilli(a.luc()).atZone(ZoneId.systemDefault()));
    }
    // Style hien dai voi mau gradient diem nhan, ho tro in an va mo offline.
    private static String css() {
        return "<style>body{font-family:'Segoe UI',system-ui,sans-serif;color:#1e293b;background:#f8fafc;margin:24px;line-height:1.5}"
            + "main{max-width:1040px;margin:auto;background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:32px;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05)}"
            + ".header{border-bottom:2px solid #e2e8f0;padding-bottom:16px;margin-bottom:24px}"
            + ".badge-pill{display:inline-block;padding:4px 12px;border-radius:9999px;font-size:12px;font-weight:600;background:#eef2ff;color:#4f46e5;margin-bottom:8px}"
            + "h1{color:#1e293b;font-size:24px;margin:4px 0 8px 0;font-weight:700}"
            + ".meta{color:#64748b;font-size:14px;margin:0}.meta b{color:#0f172a}"
            + ".cards{display:flex;flex-wrap:wrap;gap:12px;margin:16px 0}"
            + ".card{background:#f8fafc;border:1px solid #e2e8f0;border-top:3px solid #4f46e5;border-radius:8px;padding:12px 16px;min-width:140px;flex:1}"
            + ".card b{display:block;font-size:24px;color:#4f46e5;margin-top:4px}"
            + "h2{color:#1e293b;font-size:16px;margin:24px 0 8px 0;padding-left:8px;border-left:3px solid #4f46e5}"
            + "table{border-collapse:collapse;width:100%;margin:12px 0 24px;background:#ffffff;border:1px solid #e2e8f0;border-radius:8px}"
            + "th,td{border:1px solid #e2e8f0;padding:10px 14px;text-align:left}"
            + "th{background-color:#4f46e5;color:#ffffff;font-weight:600;font-size:14px}"
            + "td{font-size:14px;color:#334155}"
            + "tr:nth-child(even){background-color:#f8fafc}"
            + "svg{background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;width:100%;height:220px;margin:12px 0}"
            + ".footer{border-top:1px solid #e2e8f0;margin-top:28px;padding-top:14px;font-size:12px;color:#94a3b8;text-align:center}"
            + "@media print{body{background:#fff;margin:0}main{border:none;box-shadow:none;padding:0;max-width:100%}h2{break-after:avoid}tr{break-inside:avoid}.card{background:#fff;border:1px solid #cbd5e1}th{background-color:#4f46e5!important;color:#fff!important;-webkit-print-color-adjust:exact;print-color-adjust:exact}}</style>";
    }
    // ===== HTML IN DUOC, KHONG TAI CSS/JS NGOAI =====
    public static String html(AnhChupQuanTri a) {
        StringBuilder s=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><title>Báo cáo UDP Chat</title>");
        s.append(css()).append("<main><div class='header'><span class='badge-pill'>HỆ THỐNG UDP CHAT</span><h1>Báo cáo Quản trị Hệ thống</h1>");
        s.append("<p class='meta'>Thời điểm xuất báo cáo: <b>").append(thoiDiem(a)).append("</b></p></div><div class='cards'>");
        for (var r:a.lay("STATS")) { String v=r.get(0).equals("Dung lượng tệp")?TepTin.dungLuong(Long.parseLong(r.get(1))):r.get(1); s.append("<div class='card'>").append(e(r.get(0))).append("<b>").append(e(v)).append("</b></div>"); } s.append("</div>");
        s.append("<p style='font-size:13px;color:#64748b;'>Số liệu thống kê tổng hợp các tin nhắn, sticker và tệp đang lưu trong các phòng hiện có.</p>");
        bang(s,"Số người dùng theo từng phòng",List.of("Mã","Phòng","Số người dùng"),chon(a.lay("ROOMS"),1,2,6));
        var top=a.lay("USERS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(6))).reversed()).limit(10).toList();
        bang(s,"Top 10 người nhắn nhiều nhất",List.of("Tài khoản","Tên hiển thị","Số tin"),chon(top,1,2,6));
        var phong=a.lay("ROOMS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(7))).reversed()).limit(10).toList();
        bang(s,"Top phòng sôi nổi",List.of("Phòng","Số tin","Số tệp"),chon(phong,2,7,8));
        s.append("<h2>Tin nhắn 7 ngày gần nhất</h2>").append(bieuDo(a));
        bang(s,"Tài khoản bị khóa",List.of("Tài khoản","Tên hiển thị","Ngày tạo"),chon(a.lay("USERS").stream().filter(r -> r.get(4).equals("Bị khóa")).toList(),1,2,7));
        return s.append("<div class='footer'>Báo cáo hệ thống UDP Chat · Dữ liệu tại thời điểm xuất</div></main></html>").toString();
    }
    // HTML tuy chon cac nhom du lieu duoc chon de in hoac xem tren trinh duyet.
    public static String htmlTuyChon(AnhChupQuanTri a,boolean coUsers,boolean coRooms,boolean coAudit,boolean coStats) {
        StringBuilder s=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><title>Báo cáo UDP Chat</title>");
        s.append(css()).append("<main><div class='header'><span class='badge-pill'>HỆ THỐNG UDP CHAT</span><h1>Báo cáo Quản trị Hệ thống</h1>");
        s.append("<p class='meta'>Thời điểm xuất báo cáo: <b>").append(thoiDiem(a)).append("</b></p></div>");
        if (coStats) {
            s.append("<div class='cards'>");
            for (var r:a.lay("STATS")) { String v=r.get(0).equals("Dung lượng tệp")?TepTin.dungLuong(Long.parseLong(r.get(1))):r.get(1); s.append("<div class='card'>").append(e(r.get(0))).append("<b>").append(e(v)).append("</b></div>"); } s.append("</div>");
            s.append("<p style='font-size:13px;color:#64748b;'>Số liệu thống kê thời gian thực từ Server UDP Chat.</p>");
        }
        if (coUsers) {
            var top=a.lay("USERS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(6))).reversed()).limit(10).toList();
            bang(s,"Top 10 người nhắn nhiều nhất",List.of("Tài khoản","Tên hiển thị","Số tin"),chon(top,1,2,6));
            bang(s,"Danh sách tài khoản",List.of("Tài khoản","Tên hiển thị","Vai trò","Trạng thái"),chon(a.lay("USERS"),1,2,3,4));
        }
        if (coRooms) {
            bang(s,"Số người dùng theo từng phòng",List.of("Mã","Phòng","Số người dùng"),chon(a.lay("ROOMS"),1,2,6));
            var phong=a.lay("ROOMS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(7))).reversed()).limit(10).toList();
            bang(s,"Top phòng sôi nổi",List.of("Phòng","Số tin","Số tệp"),chon(phong,2,7,8));
        }
        if (coStats && !a.lay("DAYS").isEmpty()) {
            s.append("<h2>Tin nhắn 7 ngày gần nhất</h2>").append(bieuDo(a));
        }
        if (coAudit && !a.lay("AUDIT").isEmpty()) {
            bang(s,"Nhật ký kiểm toán hệ thống",List.of("Thời gian","Người thực hiện","Hành động","Đối tượng","Chi tiết"),chon(a.lay("AUDIT"),0,1,2,3,4));
        }
        return s.append("<div class='footer'>Báo cáo hệ thống UDP Chat · Dữ liệu tại thời điểm xuất</div></main></html>").toString();
    }
    // Bang HTML chi chen gia tri da thoat, khong nhan HTML nguoi dung.
    private static void bang(StringBuilder s,String ten,List<String> cols,List<List<String>> rows) {
        s.append("<h2>").append(e(ten)).append("</h2><table><thead><tr>");
        cols.forEach(c -> s.append("<th>").append(e(c)).append("</th>"));
        s.append("</tr></thead><tbody>");
        for (var row:rows) {
            s.append("<tr>");
            row.forEach(c -> s.append("<td>").append(e(c)).append("</td>"));
            s.append("</tr>");
        }
        s.append("</tbody></table>");
    }
    // SVG chi dung so do Server tinh, cot co bo goc va nhan ro rang.
    private static String bieuDo(AnhChupQuanTri a) {
        Map<String,Long> ds=new LinkedHashMap<>(); LocalDate nay=Instant.ofEpochMilli(a.luc()).atZone(ZoneId.systemDefault()).toLocalDate();
        for (int i=6;i>=0;i--) { ds.put(nay.minusDays(i).toString(),0L); }
        for (var r:a.lay("DAYS")) { if (ds.containsKey(r.get(0))) { ds.merge(r.get(0),Long.parseLong(r.get(2)),Long::sum); } }
        long max=Math.max(1,ds.values().stream().mapToLong(Long::longValue).max().orElse(1));
        StringBuilder s=new StringBuilder("<svg viewBox='0 0 840 240' role='img' aria-label='Tin theo ngày'>"); int i=0;
        for (var r:ds.entrySet()) {
            int h=(int)(160*r.getValue()/max),x=20+i++*118;
            s.append("<rect fill='#4f46e5' rx='4' ry='4' x='").append(x).append("' y='").append(185-h).append("' width='65' height='").append(h).append("'/>");
            s.append("<text fill='#64748b' font-size='12' x='").append(x+8).append("' y='210'>").append(e(r.getKey().substring(5))).append("</text>");
            s.append("<text fill='#1e293b' font-weight='bold' font-size='12' x='").append(x+16).append("' y='").append(Math.max(20,175-h)).append("'>").append(r.getValue()).append("</text>");
        }
        return s.append("</svg>").toString();
    }
    // Zip mot thu muc bao cao gom cac file truc tiep, khong chen duong dan tuy y.
    public static Path nen(Path thu) throws IOException {
        Path zip=thu.resolveSibling(thu.getFileName()+".zip"),tam=zip.resolveSibling(zip.getFileName()+".tmp");
        try (ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(tam))) {
            try (var ds=Files.list(thu)) { for (Path p:ds.filter(Files::isRegularFile).toList()) { out.putNextEntry(new ZipEntry(p.getFileName().toString())); Files.copy(p,out); out.closeEntry(); } }
        }
        Files.move(tam,zip); return zip;
    }
    // Moi file ghi tam cung thu muc roi doi ten, khong ghi de bao cao cu.
    private static void ghi(Path p,String s) throws IOException { Path tam=p.resolveSibling(p.getFileName()+".tmp"); Files.writeString(tam,s,StandardCharsets.UTF_8); Files.move(tam,p); }
}
