package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Vai tro: Noi dung mot tab: bong tin va dong he thong; tieu de/chuc nang phong o cua so chinh. Ai goi: ChatMulticastUI. Goi: ThanhPhan, Theme. */
@SuppressWarnings("serial")
final class KhungChatPanel extends JPanel {
    // ===== VAI TRO VA CAC THAO TAC =====
    private final JPanel ds=new JPanel(); private final JScrollPane cuon;
    private Phong phong;
    private java.util.function.Function<String,java.awt.image.BufferedImage> anh=id -> null;
    private java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo=id -> null;
    private final java.util.Map<Long,HangTin> theoId=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,HangTin> theoMa=new java.util.LinkedHashMap<>();
    private java.util.function.Function<String,java.awt.image.BufferedImage> anhTep=id -> null;
    private final java.util.Set<Long> thuHoi=new java.util.HashSet<>();
    private java.util.function.BooleanSupplier quanLy=() -> false;
    private java.util.function.BiConsumer<String,TinNhan> hanhDong=(l,t) -> { };
    // Cache duoc doc truc tiep tren EDT, khong chan mang.
    void datHoSo(java.util.function.Function<String,java.awt.image.BufferedImage> anh,java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo) { this.anh=anh; this.hoSo=hoSo; }
    // Quyen cua phong duoc doc lai moi khi menu mo.
    void datHanhDong(java.util.function.BooleanSupplier quanLy,java.util.function.BiConsumer<String,TinNhan> hanhDong) { this.quanLy=quanLy; this.hanhDong=hanhDong; }
    // Ve lai ten va avatar, giu nguyen vi tri cuon.
    void hoSoMoi(com.mycompany.udpmultichat.common.model.NguoiDung n) {
        for (var c:ds.getComponents()) { if (c instanceof HangTin h && h.tin().userId.equals(n.id)) { h.veLai(); } }
    }
    // Khoi tao lop va noi cac thanh phan can thiet.
    KhungChatPanel(Phong phong) {
        super(new BorderLayout()); this.phong=phong; ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS)); ds.setBackground(Theme.THE);
        JPanel nen=new JPanel(new BorderLayout()); nen.setBackground(Theme.THE); nen.add(ds,BorderLayout.NORTH);
        cuon=new JScrollPane(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16); add(cuon,BorderLayout.CENTER);
    }
    // Tra thong tin phong cua khung chat dang giu.
    Phong phong() { return phong; }
    // Cap nhat.
    void capNhat(Phong p) { phong=p; }
    // Ma Client chi duy nhat trong pham vi nguoi gui; nguoi khac cung ma khong ghi de hang.
    private static String khoa(TinNhan t) { return t.userId+":"+t.maClient; }
    // Loc lai tren EDT vi callback tin va thu hoi co the den khac luong.
    TinNhan locTin(TinNhan t) { return thuHoi.contains(t.msgId)?t.daThuHoi():t; }
    // Tin Server cung ma Client thay dong dang gui, khong hien hai lan.
    void themTin(TinNhan t,boolean toi) {
        if (thuHoi.contains(t.msgId)) { t=t.daThuHoi(); }
        HangTin cu=theoId.get(t.msgId);
        if (cu==null && !t.maClient.isEmpty()) { cu=theoMa.get(khoa(t)); }
        if (cu!=null) { cu.capNhat(t); theoId.put(t.msgId,cu); capNhatNhanGui(); return; }
        HangTin h=taoHang(t,toi);
        if (t.loai.equals("F")) { var tep=com.mycompany.udpmultichat.common.TepTin.doc(t.roomId,t.userId,t.noiDung,t.thoiGian); h.datTep(null,() -> anhTep.apply(tep.id())); }
        if (toi) { h.trangThai("SENT",""); } theoId.put(t.msgId,h); if (!t.maClient.isEmpty()) { theoMa.put(khoa(t),h); } them(h); capNhatNhanGui();
    }
    // Tao hang, ten hien thi lay snapshot moi nhat theo id bat bien.
    private HangTin taoHang(TinNhan t,boolean toi) {
        return new HangTin(t,toi,() -> anh.apply(t.userId),() -> {
            var n=hoSo.apply(t.userId); return n==null?t.ten:n.tenHienThi;
        },quanLy,lenh -> {
            HangTin h=theoMa.get(khoa(t)); if (h==null) { h=theoId.get(t.msgId); }
            hanhDong.accept(lenh,h==null?t:h.tin());
        });
    }
    // Trang thai ma Client la khoa; local id=0 khong dua vao map msgId.
    void trangThai(TinNhan t,String ma,String loi) {
        HangTin h=theoMa.get(khoa(t));
        if (ma.equals("REMOVE")) {
            if (h!=null) { ds.remove(h); theoMa.remove(khoa(t)); HangTin xoa=h; theoId.values().removeIf(v -> v==xoa); ds.revalidate(); ds.repaint(); capNhatNhanGui(); } return;
        }
        if (h==null) { h=taoHang(t,true); theoMa.put(khoa(t),h); them(h); }
        if (t.msgId>0) { if (thuHoi.contains(t.msgId)) { t=t.daThuHoi(); } h.capNhat(t); theoId.put(t.msgId,h); }
        h.trangThai(ma,loi); capNhatNhanGui();
    }
    // Hang sau cung theo thu tu hien thi la tin moi nhat cua minh.
    private void capNhatNhanGui() {
        HangTin cuoi=null;
        for (var c:ds.getComponents()) { if (c instanceof HangTin h && h.laToi()) { cuoi=h; } }
        for (var c:ds.getComponents()) { if (c instanceof HangTin h) { h.hienDaGui(h==cuoi); } }
    }
    // Cap nhat theo ma Client upload hoac id tep download, giu nguyen hang chat.
    void tienDoTep(com.mycompany.udpmultichat.common.TepTienDo td,String ten) {
        HangTin h=theoMa.get(td.tep().user()+":"+td.maClient());
        if (!td.gui()) { h=timTep(td.tep().id()); }
        if (h==null && td.gui()) {
            var t=td.tep(); TinNhan tin=new TinNhan(0,t.room(),t.user(),ten,t.dongGoi(),t.luc(),td.maClient(),"F");
            h=taoHang(tin,true); theoMa.put(khoa(tin),h); them(h);
        }
        if (h!=null) {
            if (td.gui() && h.tin().msgId==0) { var t=h.tin(); h.capNhat(new TinNhan(0,t.roomId,t.userId,t.ten,td.tep().dongGoi(),t.thoiGian,t.maClient,"F")); }
            h.datTep(td,() -> anhTep.apply(td.tep().id()));
        }
    }
    // Thumbnail duoc nap nen, renderer chi doc cache.
    void datAnhTep(java.util.function.Function<String,java.awt.image.BufferedImage> anh) { anhTep=anh; }
    // Cap nhat anh khi tai xong, khong lam thay doi vi tri cuon.
    void anhTepMoi(String id) { HangTin h=timTep(id); if (h!=null) { h.veLai(); } }
    // Tim the trong so hang cua phong, khong giu noi dung tep trong RAM.
    private HangTin timTep(String id) {
        for (var c:ds.getComponents()) {
            if (c instanceof HangTin h && h.tin().loai.equals("F") && com.mycompany.udpmultichat.common.TepTin.doc(phong.id,h.tin().userId,h.tin().noiDung,h.tin().thoiGian).id().equals(id)) { return h; }
        }
        return null;
    }
    // Tombstone den truoc ban tin van chan noi dung cu den cham.
    void thuHoi(long id) {
        thuHoi.add(id); HangTin h=theoId.get(id); if (h!=null) { h.capNhat(h.tin().daThuHoi()); }
    }
    // He thong.
    void heThong(String text) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(Theme.PHU); hang.add(l); them(hang);
    }
    // Them.
    private void them(JPanel hang) {
        var bar=cuon.getVerticalScrollBar(); boolean oDay=bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-36;
        ds.add(hang); ds.revalidate(); ds.repaint();
        if (oDay) { SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum())); }
    }
}
