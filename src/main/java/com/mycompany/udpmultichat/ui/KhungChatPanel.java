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
        if (cu!=null) { cu.capNhat(t); theoId.put(t.msgId,cu); return; }
        HangTin h=taoHang(t,toi); if (toi) { h.trangThai("SENT",""); } theoId.put(t.msgId,h); if (!t.maClient.isEmpty()) { theoMa.put(khoa(t),h); } them(h);
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
            if (h!=null) { ds.remove(h); theoMa.remove(khoa(t)); HangTin xoa=h; theoId.values().removeIf(v -> v==xoa); ds.revalidate(); ds.repaint(); } return;
        }
        if (h==null) { h=taoHang(t,true); theoMa.put(khoa(t),h); them(h); }
        if (t.msgId>0) { if (thuHoi.contains(t.msgId)) { t=t.daThuHoi(); } h.capNhat(t); theoId.put(t.msgId,h); }
        h.trangThai(ma,loi);
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
