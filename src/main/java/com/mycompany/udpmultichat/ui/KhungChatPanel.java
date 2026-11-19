package com.mycompany.udpmultichat.ui;

import com.mycompany.udpmultichat.common.model.Phong;
import com.mycompany.udpmultichat.common.model.TinNhan;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Vai tro: Noi dung mot tab: bong tin, phan cach ngay, gop tin, tim kiem. Ai goi: ChatMulticastUI. Goi: HangTin, Theme. */
@SuppressWarnings("serial")
final class KhungChatPanel extends JPanel {
    private final JPanel ds=new JPanel(); private final JScrollPane cuon;
    private Phong phong;
    private java.util.function.Function<String,java.awt.image.BufferedImage> anh=id -> null;
    private java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo=id -> null;
    private final Map<Long,HangTin> theoId=new LinkedHashMap<>();
    private final Map<String,HangTin> theoMa=new LinkedHashMap<>();
    private java.util.function.Function<String,java.awt.image.BufferedImage> anhTep=id -> null;
    private final Set<Long> thuHoi=new HashSet<>();
    private java.util.function.BooleanSupplier quanLy=() -> false;
    private java.util.function.BiConsumer<String,TinNhan> hanhDong=(l,t) -> { };
    private LocalDate ngayCuoi=null; private TinNhan tinCuoi=null;
    private final JTextField txtTim=new JTextField();
    private final JLabel lblDemTim=Theme.nhan("",false);
    private final List<HangTin> dsKhop=new ArrayList<>();
    private int viTriTim=-1;
    private final JPanel thanhTim=taoThanhTim();

    void datHoSo(java.util.function.Function<String,java.awt.image.BufferedImage> anh,java.util.function.Function<String,com.mycompany.udpmultichat.common.model.NguoiDung> hoSo) { this.anh=anh; this.hoSo=hoSo; }
    void datHanhDong(java.util.function.BooleanSupplier quanLy,java.util.function.BiConsumer<String,TinNhan> hanhDong) { this.quanLy=quanLy; this.hanhDong=hanhDong; }
    void hoSoMoi(com.mycompany.udpmultichat.common.model.NguoiDung n) {
        for (var c:ds.getComponents()) { if (c instanceof HangTin h && h.tin().userId.equals(n.id)) { h.veLai(); } }
    }

    KhungChatPanel(Phong phong) {
        super(new BorderLayout()); this.phong=phong; ds.setLayout(new BoxLayout(ds,BoxLayout.Y_AXIS)); ds.setBackground(Theme.THE);
        JPanel nen=new JPanel(new BorderLayout()); nen.setBackground(Theme.THE); nen.add(ds,BorderLayout.NORTH);
        cuon=new JScrollPane(nen); cuon.setBorder(null); cuon.getVerticalScrollBar().setUnitIncrement(16);
        add(thanhTim,BorderLayout.NORTH); add(cuon,BorderLayout.CENTER);
        ganPhimTatTimKiem();
    }

    Phong phong() { return phong; }
    void capNhat(Phong p) { phong=p; }
    private static String khoa(TinNhan t) { return t.userId+":"+t.maClient; }
    TinNhan locTin(TinNhan t) { return thuHoi.contains(t.msgId)?t.daThuHoi():t; }

    // Them tin: tu chen vach phan cach ngay va xet gop tin lien tiep duoi 5 phut.
    void themTin(TinNhan t,boolean toi) {
        if (thuHoi.contains(t.msgId)) { t=t.daThuHoi(); }
        HangTin cu=theoId.get(t.msgId);
        if (cu==null && !t.maClient.isEmpty()) { cu=theoMa.get(khoa(t)); }
        if (cu!=null) { cu.capNhat(t); theoId.put(t.msgId,cu); capNhatNhanGui(); return; }

        LocalDate date=Instant.ofEpochMilli(t.thoiGian).atZone(ZoneId.systemDefault()).toLocalDate();
        if (ngayCuoi==null || !ngayCuoi.equals(date)) { ds.add(taoHangNgay(date)); ngayCuoi=date; tinCuoi=null; }

        boolean gop=tinCuoi!=null && tinCuoi.userId.equals(t.userId) && (t.thoiGian-tinCuoi.thoiGian<300000L)
                && !tinCuoi.thuHoi && !t.thuHoi && !t.loai.equals("Y") && !tinCuoi.loai.equals("Y");
        HangTin h=taoHang(t,toi);
        if (gop) { h.datGop(true); }
        if (t.loai.equals("F")) { var tep=com.mycompany.udpmultichat.common.TepTin.doc(t.roomId,t.userId,t.noiDung,t.thoiGian); h.datTep(null,() -> anhTep.apply(tep.id())); }
        if (toi) { h.trangThai("SENT",""); } theoId.put(t.msgId,h); if (!t.maClient.isEmpty()) { theoMa.put(khoa(t),h); }
        them(h); tinCuoi=t; capNhatNhanGui();
    }

    private HangTin taoHang(TinNhan t,boolean toi) {
        return new HangTin(t,toi,() -> anh.apply(t.userId),() -> {
            var n=hoSo.apply(t.userId); return n==null?t.ten:n.tenHienThi;
        },quanLy,lenh -> {
            if (lenh.startsWith("Cuộn:")) {
                try { cuonToiTin(Long.parseLong(lenh.substring(5))); } catch (NumberFormatException ignored) { }
            } else {
                HangTin h=theoMa.get(khoa(t)); if (h==null) { h=theoId.get(t.msgId); }
                hanhDong.accept(lenh,h==null?t:h.tin());
            }
        });
    }

    void trangThai(TinNhan t,String ma,String loi) {
        HangTin h=theoMa.get(khoa(t));
        if (ma.equals("REMOVE")) {
            if (h!=null) { ds.remove(h); theoMa.remove(khoa(t)); HangTin xoa=h; theoId.values().removeIf(v -> v==xoa); ds.revalidate(); ds.repaint(); capNhatNhanGui(); } return;
        }
        if (h==null) { h=taoHang(t,true); theoMa.put(khoa(t),h); them(h); }
        if (t.msgId>0) { if (thuHoi.contains(t.msgId)) { t=t.daThuHoi(); } h.capNhat(t); theoId.put(t.msgId,h); }
        h.trangThai(ma,loi); capNhatNhanGui();
    }

    private void capNhatNhanGui() {
        HangTin cuoi=null;
        for (var c:ds.getComponents()) { if (c instanceof HangTin h && h.laToi()) { cuoi=h; } }
        for (var c:ds.getComponents()) { if (c instanceof HangTin h) { h.hienDaGui(h==cuoi); } }
    }

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

    void datAnhTep(java.util.function.Function<String,java.awt.image.BufferedImage> anh) { anhTep=anh; }
    void anhTepMoi(String id) { HangTin h=timTep(id); if (h!=null) { h.veLai(); } }

    private HangTin timTep(String id) {
        for (var c:ds.getComponents()) {
            if (c instanceof HangTin h && h.tin().loai.equals("F") && com.mycompany.udpmultichat.common.TepTin.doc(phong.id,h.tin().userId,h.tin().noiDung,h.tin().thoiGian).id().equals(id)) { return h; }
        }
        return null;
    }

    void thuHoi(long id) {
        thuHoi.add(id); HangTin h=theoId.get(id); if (h!=null) { h.capNhat(h.tin().daThuHoi()); }
    }

    void heThong(String text) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER)); hang.setOpaque(false);
        JLabel l=Theme.nhan(text,false); l.setFont(Theme.NHO); l.setForeground(Theme.PHU); hang.add(l); them(hang);
    }

    private void them(JPanel hang) {
        var bar=cuon.getVerticalScrollBar(); boolean oDay=bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-36;
        ds.add(hang); ds.revalidate(); ds.repaint();
        if (oDay) { SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum())); }
    }

    // Vach phan cach ngay bo tron can giua khung chat.
    private JPanel taoHangNgay(LocalDate date) {
        JPanel hang=new JPanel(new FlowLayout(FlowLayout.CENTER,0,Theme.S8)); hang.setOpaque(false);
        LocalDate nay=LocalDate.now();
        String chu=date.equals(nay)?"Hôm nay":date.equals(nay.minusDays(1))?"Hôm qua":
                date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel l=new JLabel(chu) {
            @Override protected void paintComponent(Graphics g) {
                Theme.boTron(g,getWidth(),getHeight(),Theme.CHON,10); super.paintComponent(g);
            }
        };
        l.setFont(Theme.NHO.deriveFont(Font.BOLD,11f)); l.setForeground(Theme.PHU);
        l.setBorder(BorderFactory.createEmptyBorder(2,12,2,12)); hang.add(l); return hang;
    }

    // Cuon viewport den tin nhan goc va nhay sang de nhan biet.
    void cuonToiTin(long msgId) {
        HangTin h=theoId.get(msgId);
        if (h!=null) {
            h.scrollRectToVisible(new Rectangle(0,0,h.getWidth(),h.getHeight()));
            Timer t=new Timer(200,new ActionListener() {
                int count=0;
                public void actionPerformed(ActionEvent e) {
                    count++;
                    h.setBorder(count%2==1?BorderFactory.createLineBorder(Theme.CHINH,2):null);
                    if (count>=4) { ((Timer)e.getSource()).stop(); h.setBorder(null); }
                }
            });
            t.start();
        }
    }

    // Lay toan bo danh sach tin trong phong de xuat lich su .txt.
    List<TinNhan> danhSachTin() {
        List<TinNhan> list=new ArrayList<>();
        for (var c:ds.getComponents()) { if (c instanceof HangTin h) { list.add(h.tin()); } }
        return list;
    }

    // Thanh tim kiem tin nhan phong (Ctrl + F).
    private JPanel taoThanhTim() {
        JPanel bar=new JPanel(new BorderLayout(Theme.S8,0));
        bar.setBackground(Theme.NEN); bar.setBorder(Theme.le(Theme.S8));
        JPanel trai=new JPanel(new FlowLayout(FlowLayout.LEFT,Theme.S4,0)); trai.setOpaque(false);
        JLabel icon=Theme.nhan("🔍",false); txtTim.setPreferredSize(new Dimension(200,28));
        txtTim.putClientProperty("JTextField.placeholderText","Tìm trong cuộc trò chuyện…");
        txtTim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { timKiem(); }
            public void removeUpdate(DocumentEvent e) { timKiem(); }
            public void changedUpdate(DocumentEvent e) { timKiem(); }
        });
        txtTim.addActionListener(e -> chuyenTim(1));
        trai.add(icon); trai.add(txtTim); trai.add(lblDemTim); bar.add(trai,BorderLayout.CENTER);

        JPanel nut=new JPanel(new FlowLayout(FlowLayout.RIGHT,Theme.S4,0)); nut.setOpaque(false);
        JButton len=Theme.nutNhe("▲",""),xuong=Theme.nutNhe("▼",""),dong=new JButton("✕");
        len.addActionListener(e -> chuyenTim(-1)); xuong.addActionListener(e -> chuyenTim(1));
        dong.setContentAreaFilled(false); dong.setBorderPainted(false); dong.setFont(Theme.NHO);
        dong.addActionListener(e -> tatTimKiem());
        nut.add(len); nut.add(xuong); nut.add(dong); bar.add(nut,BorderLayout.EAST);
        bar.setVisible(false); return bar;
    }

    void batTimKiem() { thanhTim.setVisible(true); txtTim.requestFocusInWindow(); txtTim.selectAll(); }
    void tatTimKiem() { thanhTim.setVisible(false); viTriTim=-1; dsKhop.clear(); lblDemTim.setText(""); }

    private void timKiem() {
        dsKhop.clear(); viTriTim=-1;
        String tuKhoa=txtTim.getText().strip().toLowerCase(Locale.ROOT);
        if (tuKhoa.isEmpty()) { lblDemTim.setText(""); return; }
        for (var c:ds.getComponents()) {
            if (c instanceof HangTin h && !h.tin().thuHoi) {
                String nd=TrichDanUtil.layNoiDungThat(h.tin().noiDung).toLowerCase(Locale.ROOT);
                if (nd.contains(tuKhoa) || h.tin().ten.toLowerCase(Locale.ROOT).contains(tuKhoa)) {
                    dsKhop.add(h);
                }
            }
        }
        lblDemTim.setText(dsKhop.isEmpty()?"0/0":"1/"+dsKhop.size());
        if (!dsKhop.isEmpty()) { viTriTim=0; cuonToiHang(dsKhop.get(0)); }
    }

    private void chuyenTim(int huong) {
        if (dsKhop.isEmpty()) return;
        viTriTim=(viTriTim+huong+dsKhop.size())%dsKhop.size();
        lblDemTim.setText((viTriTim+1)+"/"+dsKhop.size());
        cuonToiHang(dsKhop.get(viTriTim));
    }

    private void cuonToiHang(HangTin h) {
        h.scrollRectToVisible(new Rectangle(0,0,h.getWidth(),h.getHeight()));
        h.setBorder(BorderFactory.createLineBorder(Theme.CHINH,2));
        Timer t=new Timer(600,e -> { h.setBorder(null); ((Timer)e.getSource()).stop(); });
        t.setRepeats(false); t.start();
    }

    private void ganPhimTatTimKiem() {
        InputMap im=getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am=getActionMap();
        im.put(KeyStroke.getKeyStroke("control F"),"tim");
        am.put("tim",new AbstractAction() { public void actionPerformed(ActionEvent e) { batTimKiem(); } });
        im.put(KeyStroke.getKeyStroke("ESCAPE"),"dongTim");
        am.put("dongTim",new AbstractAction() { public void actionPerformed(ActionEvent e) { tatTimKiem(); } });
    }
}
