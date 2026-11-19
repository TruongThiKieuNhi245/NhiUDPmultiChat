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
    // ===== HTML IN DUOC, KHONG TAI CSS/JS NGOAI =====
    public static String html(AnhChupQuanTri a) {
        StringBuilder s=new StringBuilder("<!doctype html><html lang='vi'><meta charset='utf-8'><title>Báo cáo UDP Chat</title><style>");
        s.append("body{font:15px system-ui;color:#202635;background:#f5f6fa;margin:32px}main{max-width:1100px;margin:auto}h1{color:#4f6bed}.cards{display:flex;flex-wrap:wrap;gap:12px}.card{background:white;border:1px solid #c9ced9;border-radius:8px;padding:16px;min-width:145px}.card b{display:block;font-size:26px}table{border-collapse:collapse;width:100%;background:white;margin:16px 0 28px}th,td{border:1px solid #c9ced9;padding:10px;text-align:left}th{background:#e9edff}tr:nth-child(even){background:#fafbff}svg{background:white;width:100%;height:240px}@media print{body{margin:0;background:white}h2{break-after:avoid}tr{break-inside:avoid}.card{min-width:110px}}");
        s.append("</style><main><h1>Báo cáo UDP Chat</h1><p>Thời điểm chụp dữ liệu: ").append(e(Instant.ofEpochMilli(a.luc()).atZone(ZoneId.systemDefault()).toString())).append("</p><div class='cards'>");
        for (var r:a.lay("STATS")) { s.append("<div class='card'>").append(e(r.get(0))).append("<b>").append(e(r.get(1))).append("</b></div>"); } s.append("</div>");
        s.append("<p>Số tin gồm văn bản, sticker, tệp, tin hệ thống và tombstone thu hồi trong các phòng còn tồn tại. Số tệp/sticker theo ngày chỉ tính tin còn nội dung; tin thu hồi được đếm riêng. Tệp và dung lượng là dữ liệu hiện còn lưu, không phải tổng đã truyền.</p>");
        bang(s,"Số người dùng theo từng phòng",List.of("Mã","Phòng","Số người dùng"),chon(a.lay("ROOMS"),1,2,6));
        var top=a.lay("USERS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(6))).reversed()).limit(10).toList();
        bang(s,"Top 10 người nhắn nhiều nhất",List.of("Tài khoản","Tên hiển thị","Số tin"),chon(top,1,2,6));
        var phong=a.lay("ROOMS").stream().sorted(Comparator.comparingLong((List<String> r) -> Long.parseLong(r.get(7))).reversed()).limit(10).toList();
        bang(s,"Top phòng sôi nổi",List.of("Phòng","Số tin","Số tệp"),chon(phong,2,7,8));
        s.append("<h2>Tin nhắn 7 ngày gần nhất</h2>").append(bieuDo(a));
        bang(s,"Tài khoản bị khóa",List.of("Tài khoản","Tên hiển thị","Ngày tạo"),chon(a.lay("USERS").stream().filter(r -> r.get(4).equals("Bị khóa")).toList(),1,2,7));
        return s.append("<p>Báo cáo tự chứa · UDP Chat · dữ liệu tại một thời điểm</p></main></html>").toString();
    }
    // Bang HTML chi chen gia tri da thoat, khong nhan HTML nguoi dung.
    private static void bang(StringBuilder s,String ten,List<String> cols,List<List<String>> rows) {
        s.append("<h2>").append(e(ten)).append("</h2><table><thead><tr>"); cols.forEach(c -> s.append("<th>").append(e(c)).append("</th>")); s.append("</tr></thead><tbody>");
        for (var row:rows) { s.append("<tr>"); row.forEach(c -> s.append("<td>").append(e(c)).append("</td>")); s.append("</tr>"); } s.append("</tbody></table>");
    }
    // SVG chi dung so do Server tinh, nhan ngay cung duoc thoat HTML.
    private static String bieuDo(AnhChupQuanTri a) {
        Map<String,Long> ds=new LinkedHashMap<>(); LocalDate nay=Instant.ofEpochMilli(a.luc()).atZone(ZoneId.systemDefault()).toLocalDate();
        for (int i=6;i>=0;i--) { ds.put(nay.minusDays(i).toString(),0L); }
        for (var r:a.lay("DAYS")) { if (ds.containsKey(r.get(0))) { ds.merge(r.get(0),Long.parseLong(r.get(2)),Long::sum); } }
        long max=Math.max(1,ds.values().stream().mapToLong(Long::longValue).max().orElse(1)); StringBuilder s=new StringBuilder("<svg viewBox='0 0 840 240' role='img' aria-label='Tin theo ngày'>"); int i=0;
        for (var r:ds.entrySet()) { int h=(int)(160*r.getValue()/max),x=20+i++*118; s.append("<rect fill='#4f6bed' x='").append(x).append("' y='").append(185-h).append("' width='65' height='").append(h).append("'/><text x='").append(x).append("' y='210'>").append(e(r.getKey().substring(5))).append("</text><text x='").append(x).append("' y='").append(175-h).append("'>").append(r.getValue()).append("</text>"); }
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
