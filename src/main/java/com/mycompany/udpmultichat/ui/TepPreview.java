package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.*;
import com.mycompany.udpmultichat.common.model.*;
import com.mycompany.udpmultichat.ui.components.ThongBao;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;

/** Vai tro: Anh xem truoc cac trang thai tep dung component that. Ai goi: UiPreview. Goi: TheTep, TepGiaoDien, ChatMulticastUI. */
final class TepPreview {
    // Khong khoi tao lop tien ich.
    private TepPreview() { }
    // ===== CAC MAN HINH M4 =====
    static void xuat() throws IOException {
        UiPreview.xuatForm(TepGiaoDien.menuMau(anh -> { }),"38-menu-dinh-kem.png");
        JPanel luoi=new JPanel(new GridLayout(2,3,Theme.S16,Theme.S16)); luoi.setBorder(Theme.le(Theme.S16)); luoi.setBackground(Theme.THE);
        String[] states={"QUEUE","SEND","DONE","ERROR","DOWNLOAD","CANCEL"};
        for (int i=0;i<states.length;i++) { luoi.add(the(states[i],i%2==0,i==4?anhMau():null)); }
        UiPreview.xuat(luoi,"39-the-tep-cac-trang-thai.png",1040,650);
        UiPreview.xuat(the("DONE",false,anhMau()),"40-the-anh-xem-truoc.png",350,350);
        ChatMulticastUI f=new ChatMulticastUI();
        try {
            var n=new NguoiDung("nhi","nhikieu","Kiều Nhi",VaiTro.ADMIN,"2026-11-13");
            var room=new Phong("sanh","Sảnh chung","","SANH01","HE_THONG","239.255.42.1",9001,500,true,false,true,2,"2026-11-13").voiThongTin(true,"Hệ thống",2);
            f.duLieuMau(n,java.util.List.of(room)); long luc=System.currentTimeMillis();
            TepTin tep=new TepTin(UUID.randomUUID().toString(),"sanh","an","Bài tập chương 6.pdf",5*1024*1024,123,luc);
            f.onTinNhan(new TinNhan(1,"sanh","an","Minh An",tep.dongGoi(),luc,TinNhan.taoMa(),"F"),false);
            TepTin gui=new TepTin(UUID.randomUUID().toString(),"sanh","nhi","Báo cáo mạng.zip",5*1024*1024,124,luc);
            f.onTep(new TepTienDo(TinNhan.taoMa(),gui,"SEND",gui.kichThuoc()*45/100,3.2*1024*1024,Path.of("bao-cao.zip"),"",true));
            UiPreview.xuat(f.getRootPane(),"41-chat-the-tep.png",1240,900);
            if (UiPreview.demNhan(f.getRootPane(),tep.ten())!=1 || UiPreview.demNhan(f.getRootPane(),gui.ten())!=1) { throw new AssertionError("Preview the tep phai hien ca tin cua minh va nguoi khac"); }
            f.toast(ThongBao.Muc.LOI,"Tệp quá lớn (tối đa 10 MB).");
            UiPreview.xuat(f.getRootPane(),"42-tep-qua-lon.png",1240,900);
        } finally { f.dispose(); }
        UiPreview.xuat(TepGiaoDien.hopLuu("Bài tập có dấu.txt"),"43-hop-luu-tep.png",760,500);
        System.out.println("PASS UiPreview M4: menu +, the tep 6 trang thai, preview anh va hop luu");
    }
    // Cac the mang cung du lieu tien do nhu Listener that.
    private static JPanel the(String state,boolean toi,BufferedImage anh) {
        TepTin t=new TepTin(UUID.randomUUID().toString(),"sanh",toi?"nhi":"an","Bài tập Lập trình mạng rất dài.pdf",5*1024*1024,123,System.currentTimeMillis());
        var td=new TepTienDo(TinNhan.taoMa(),t,state,state.equals("DONE")?t.kichThuoc():t.kichThuoc()*45/100,3.2*1024*1024,Path.of("test.pdf"),"Mất kết nối. Hãy thử lại.",toi);
        TheTep the=new TheTep(t,toi,td,anh,s -> { }); the.setForeground(toi?Theme.CHINH:Theme.NEN);
        JPanel boc=new JPanel(new FlowLayout(FlowLayout.CENTER,Theme.S8,Theme.S8)); boc.setBackground(Theme.THE); boc.add(the); return boc;
    }
    // Anh mau ve bang code, khong lay tai nguyen ngoai.
    private static BufferedImage anhMau() {
        BufferedImage h=new BufferedImage(240,150,BufferedImage.TYPE_INT_RGB); Graphics2D g=h.createGraphics();
        g.setColor(Theme.NEN_TIN); g.fillRect(0,0,240,150); g.setColor(Theme.CHINH); g.fillOval(160,16,40,40); g.setColor(Theme.THANH_CONG); g.fillPolygon(new int[]{0,80,140,240,240,0},new int[]{150,50,105,60,150,150},6); g.dispose(); return h;
    }
}
