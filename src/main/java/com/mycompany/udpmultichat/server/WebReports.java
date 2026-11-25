package com.mycompany.udpmultichat.server;

import com.mycompany.udpmultichat.common.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

/** Xuat cung mot snapshot; khong doc lai kho trong luc tao PDF/ZIP. */
public final class WebReports {
    private WebReports() { }
    public static byte[] csv(List<String> headers,List<List<String>> rows) {
        var out=new StringBuilder("\ufeff");out.append(csvLine(headers));for(var row:rows)out.append(csvLine(row));return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String csvLine(List<String> row) { return String.join(",",row.stream().map(XuatBaoCao::oCsv).toList())+"\r\n"; }
    private static List<List<String>> select(List<List<String>> rows,int... columns) { return rows.stream().map(r->Arrays.stream(columns).mapToObj(r::get).toList()).toList(); }
    public static byte[] zip(AnhChupQuanTri a) throws IOException {
        var items=new LinkedHashMap<String,byte[]>();
        items.put("nguoidung.csv",csv(List.of("Tên đăng nhập","Tên hiển thị","Vai trò","Trạng thái","Ngày tạo","Lần đăng nhập cuối","Số phòng","Số tin","Số tệp","Dung lượng"),select(a.lay("USERS"),1,2,3,4,7,8,5,6,9,10)));
        items.put("phong.csv",csv(List.of("Mã","Tên","Loại","Trạng thái","Chủ phòng","Số thành viên","Số tin","Số tệp","Dung lượng","Ngày tạo"),select(a.lay("ROOMS"),1,2,3,4,5,6,7,8,9,10)));
        items.put("thanhvien_theophong.csv",csv(List.of("Phòng","Người dùng","Vai trò","Online","Ngày vào","Số tin"),select(a.lay("MEMBERS"),1,3,5,6,7,8)));
        items.put("tin_theo_ngay.csv",csv(List.of("Ngày","Phòng","Số tin","Số tệp","Số sticker","Số tin thu hồi"),a.lay("DAYS")));
        items.put("tong_hop.html",html(a).getBytes(StandardCharsets.UTF_8));
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){for(var item:items.entrySet()){zip.putNextEntry(new ZipEntry(item.getKey()));zip.write(item.getValue());zip.closeEntry();}}return out.toByteArray();
    }
    public static String html(AnhChupQuanTri a) { return XuatBaoCao.html(a).replace("<h1>Báo cáo Quản trị Hệ thống</h1>","<h1>Báo cáo Quản trị Hệ thống</h1><p>Tệp/dung lượng chỉ tính tệp đã gửi còn lưu; không tính upload chờ. Snapshot bất biến trong 5 phút.</p>"); }
    public static byte[] pdf(WebAdministration.Snapshot snapshot) throws IOException {
        try(var document=new PDDocument();var regular=WebReports.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf");var bold=WebReports.class.getResourceAsStream("/fonts/NotoSans-Bold.ttf")) {
            if(regular==null || bold==null)throw new IOException("Không tìm thấy font báo cáo.");
            var normal=PDType0Font.load(document,regular);var strong=PDType0Font.load(document,bold);
            var a=snapshot.data();var writer=new Pdf(document,normal,strong,snapshot.id());
            try {
                writer.banner("BÁO CÁO QUẢN TRỊ","UDPmultiChat");
                writer.paragraph("Thời điểm: "+DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.ofEpochMilli(a.luc()))+" (UTC+07)");
                writer.paragraph("Phạm vi: toàn bộ tài khoản và phòng hiện có. Mã snapshot: "+snapshot.id());
                writer.cards(a.lay("STATS"));
                writer.paragraph("Số tin gồm văn bản, sticker, tệp, thông báo và tin đã thu hồi trong các phòng còn tồn tại. Thành viên gồm cả offline; online tính theo phiên nghiệp vụ.");
                writer.paragraph("Tệp và dung lượng chỉ tính tệp đã gửi còn lưu, không gồm upload chờ. Báo cáo không chứa mật khẩu, token hoặc nội dung chat.");
                var roomRows=a.lay("ROOMS").stream().map(r->List.of(r.get(2),r.get(6),r.size()>14?r.get(14):"0",r.get(7),r.get(8))).toList();
                writer.table("Số người dùng theo từng phòng",List.of("Phòng","Thành viên","Online","Số tin","Số tệp"),new float[]{211,80,64,80,80},roomRows);
                var top=a.lay("USERS").stream().sorted(Comparator.comparingLong((List<String> r)->Long.parseLong(r.get(6))).reversed()).limit(10).toList();
                writer.table("Top người chat",List.of("Tài khoản","Tên hiển thị","Số tin"),new float[]{175,260,80},select(top,1,2,6));
                var rooms=a.lay("ROOMS").stream().sorted(Comparator.comparingLong((List<String> r)->Long.parseLong(r.get(7))).reversed()).limit(10).toList();
                writer.table("Phòng hoạt động nhiều nhất",List.of("Phòng","Số tin","Tệp","Dung lượng"),new float[]{255,80,70,110},rooms.stream().map(r->List.of(r.get(2),r.get(7),r.get(8),TepTin.dungLuong(Long.parseLong(r.get(9))))).toList());
                Map<String,long[]> days=new TreeMap<>();for(var r:a.lay("DAYS")){long[] n=days.computeIfAbsent(r.get(0),k->new long[4]);for(int i=0;i<4;i++)n[i]+=Long.parseLong(r.get(i+2));}
                writer.chart(days);
                var dayRows=days.entrySet().stream().map(e->List.of(e.getKey(),""+e.getValue()[0],""+e.getValue()[1],""+e.getValue()[2],""+e.getValue()[3])).toList();
                writer.table("Tin nhắn theo ngày",List.of("Ngày","Tổng tin","Tệp","Sticker","Đã thu hồi"),new float[]{135,100,80,85,115},dayRows);
                writer.finish();
            }finally{writer.close();}
            document.getDocumentInformation().setTitle("Báo cáo UDPmultiChat");document.getDocumentInformation().setProducer("UDPmultiChat / Apache PDFBox 3.0.8");
            var out=new ByteArrayOutputStream();document.save(out);return out.toByteArray();
        }
    }
    private static final class Pdf implements AutoCloseable {
        private final PDDocument doc;private final PDType0Font normal,bold;private final String id;
        private PDPage page;private PDPageContentStream stream;private float y;private static final float LEFT=40,WIDTH=515;
        Pdf(PDDocument doc,PDType0Font normal,PDType0Font bold,String id) throws IOException {this.doc=doc;this.normal=normal;this.bold=bold;this.id=id;newPage();}
        private void newPage() throws IOException {
            if(stream!=null)stream.close();if(doc.getNumberOfPages()>=200)throw new IllegalArgumentException("PDF vượt 200 trang. Hãy tải CSV đầy đủ để phân tích.");
            page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=798;
        }
        private void need(float height) throws IOException { if(y-height<55)newPage(); }
        private String safe(String value,PDFont font) throws IOException {
            var out=new StringBuilder();String clean=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFC);
            for(int cp:clean.codePoints().toArray()) {
                if(Character.isISOControl(cp)){out.append(' ');continue;}
                String s=new String(Character.toChars(cp));try{font.encode(s);out.append(s);}catch(IllegalArgumentException e){out.append("[U+").append(Integer.toHexString(cp).toUpperCase(Locale.ROOT)).append(']');}
            }
            return out.toString();
        }
        private void text(String value,float x,float top,float size,boolean strong,int color) throws IOException {
            var font=strong?bold:normal;stream.setNonStrokingColor(new java.awt.Color(color));stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,top);stream.showText(safe(value,font));stream.endText();
        }
        private void rect(float x,float top,float width,float height,int color) throws IOException {stream.setNonStrokingColor(new java.awt.Color(color));stream.addRect(x,top-height,width,height);stream.fill();}
        private List<String> wrap(String value,float width,float size) throws IOException {
            String input=safe(value,normal);var lines=new ArrayList<String>();var current=new StringBuilder();
            for(String word:input.split(" ",-1)) {
                String candidate=current.length()==0?word:current+" "+word;
                if(normal.getStringWidth(candidate)/1000*size<=width){current.setLength(0);current.append(candidate);continue;}
                if(current.length()>0){lines.add(current.toString());current.setLength(0);}
                for(int cp:word.codePoints().toArray()) {
                    String c=new String(Character.toChars(cp));
                    if(current.length()>0 && normal.getStringWidth(current.toString()+c)/1000*size>width){lines.add(current.toString());current.setLength(0);}
                    current.append(c);
                }
            }
            if(current.length()>0 || lines.isEmpty())lines.add(current.toString());return lines;
        }
        void banner(String title,String brand) throws IOException {rect(LEFT,y,WIDTH,80,0x43378c);rect(LEFT,y,7,80,0x9769df);text(brand,LEFT+20,y-23,11,true,0xe9ddff);text(title,LEFT+20,y-53,22,true,0xffffff);y-=100;}
        void paragraph(String value) throws IOException {var lines=wrap(value,WIDTH,10);need(lines.size()*14+8);for(String line:lines){text(line,LEFT,y,10,false,0x536076);y-=14;}y-=8;}
        void heading(String title) throws IOException {need(65);text(title,LEFT,y,14,true,0x43378c);y-=25;}
        void cards(List<List<String>> stats) throws IOException {
            for(int start=0;start<stats.size();start+=3){need(64);for(int i=start;i<Math.min(start+3,stats.size());i++){
                float x=LEFT+(i-start)*175;rect(x,y,165,54,0xf0ecfa);String label=stats.get(i).get(0),value=stats.get(i).get(1);if(label.equals("Dung lượng tệp"))value=TepTin.dungLuong(Long.parseLong(value));
                text(label,x+11,y-16,9,false,0x536076);text(value,x+11,y-40,18,true,0x51409b);
            }y-=64;}y-=8;
        }
        private void tableHeader(List<String> headers,float[] widths) throws IOException {
            rect(LEFT,y,WIDTH,30,0x51409b);float x=LEFT;for(int i=0;i<headers.size();i++){text(headers.get(i),x+7,y-19,9,true,0xffffff);x+=widths[i];}y-=30;
        }
        void table(String title,List<String> headers,float[] widths,List<List<String>> rows) throws IOException {
            float first=125;
            if(rows.size()<=10){first=77;for(var row:rows){int max=1;for(int i=0;i<headers.size();i++)max=Math.max(max,wrap(row.get(i),widths[i]-14,9).size());first+=max*13+14;}}
            need(first);heading(title);tableHeader(headers,widths);int index=0;
            if(rows.isEmpty())rows=List.of(java.util.Collections.nCopies(headers.size(),"-"));
            for(var row:rows) {
                var lines=new ArrayList<List<String>>();int max=1;for(int i=0;i<headers.size();i++){var list=wrap(row.get(i),widths[i]-14,9);lines.add(list);max=Math.max(max,list.size());}
                float height=max*13+14;
                if(y-height<55){newPage();heading(title+" (tiếp)");tableHeader(headers,widths);}
                rect(LEFT,y,WIDTH,height,index++%2==0?0xf4f3f9:0xffffff);float x=LEFT;
                for(int i=0;i<lines.size();i++){float baseline=y-16;for(String line:lines.get(i)){text(line,x+7,baseline,9,false,0x263244);baseline-=13;}x+=widths[i];}
                y-=height;
            }y-=22;
        }
        void chart(Map<String,long[]> values) throws IOException {
            need(200);heading("Biểu đồ tin nhắn 14 ngày có dữ liệu gần nhất");
            var list=new ArrayList<>(values.entrySet());if(list.size()>14)list=new ArrayList<>(list.subList(list.size()-14,list.size()));
            if(list.isEmpty()){paragraph("Chưa có dữ liệu tin nhắn.");return;}
            long max=Math.max(1,list.stream().mapToLong(e->e.getValue()[0]).max().orElse(1));float step=WIDTH/list.size();float base=y-115;
            for(int i=0;i<list.size();i++){
                var e=list.get(i);float height=100f*e.getValue()[0]/max,x=LEFT+i*step+4;
                rect(x,base+height,Math.min(40,step-8),height,0x7960c6);text(""+e.getValue()[0],x,base+height+5,8,true,0x43378c);
                text(LocalDate.parse(e.getKey()).format(DateTimeFormatter.ofPattern("dd/MM")),x,base-14,8,false,0x536076);
            }y=base-38;
        }
        void finish() throws IOException {
            close();int count=doc.getNumberOfPages();
            for(int i=0;i<count;i++)try(var footer=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)) {
                footer.setNonStrokingColor(new java.awt.Color(0x6a7180));footer.beginText();footer.setFont(normal,8);footer.newLineAtOffset(40,30);footer.showText("UDPmultiChat | "+id.substring(0,8)+" | Trang "+(i+1)+"/"+count);footer.endText();
            }
        }
        @Override public void close() throws IOException {if(stream!=null){stream.close();stream=null;}}
    }
}
